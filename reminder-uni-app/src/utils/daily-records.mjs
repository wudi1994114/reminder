// Calendar records keep local dates and drafts independent of the device timezone offset.
export function dateKey(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) throw new Error('日期无效');
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export const draftKey = (userId, date) => `daily-record-draft:v1:${userId}:${date}`;
const blankForm = () => ({ weightKg: '', heightCm: '', diary: '' });
const recordForm = record => ({ weightKg: record?.weightKg == null ? '' : String(record.weightKg), heightCm: record?.heightCm == null ? '' : String(record.heightCm), diary: record?.diary || '' });
const sameForm = (left, right) => ['weightKg', 'heightCm', 'diary'].every(key => String(left?.[key] ?? '') === String(right?.[key] ?? ''));
const sameBase = (draft, recordId, version) => !!draft && (draft.recordId ?? null) === recordId && draft.version === version;
let writerSequence = 0;

function measurement(value, label, maximum) {
  const text = String(value ?? '').trim();
  if (!text) return null;
  const number = Number(text);
  if (!/^\d{1,3}(?:\.\d?)?$/.test(text) || !Number.isFinite(number) || number < 0.1 || number > maximum) {
    throw new Error(`${label}请填写 0.1–${maximum}，最多一位小数`);
  }
  return number;
}

export function validateRecordForm(form) {
  const weightKg = measurement(form.weightKg, '体重', 999.9);
  const heightCm = measurement(form.heightCm, '身高', 300);
  const diary = String(form.diary ?? '').trim() || null;
  if (diary && diary.length > 10000) throw new Error('备注最多 10000 字');
  if (weightKg === null && heightCm === null && diary === null) throw new Error('至少记录一项内容，清空整条记录请使用删除');
  return { weightKg, heightCm, diary };
}

export function createDailyRecordState() {
  return { userId: null, date: '', record: null, latestHeight: null, form: blankForm(), formVersion: null, formRecordId: null,
    loaded: false, loading: false, editing: false, saving: false, error: '', conflict: false,
    draftRestored: false, storageError: '', heightError: '', recordDates: [], monthLoading: false, monthError: '' };
}

export function createDailyRecordSession({ state, api, storage }) {
  let daySequence = 0, monthSequence = 0, monthUser = null, month = '';
  const writerId = `${Date.now()}:${++writerSequence}:${Math.random().toString(36).slice(2)}`;
  const ownsDraft = (draft, recordId, version) => sameBase(draft, recordId, version) && draft.writerId === writerId;
  const readDraft = (userId, date) => {
    try {
      const value = storage.get(draftKey(userId, date));
      if (!value || !value.form || !['weightKg', 'heightCm', 'diary'].every(key => typeof value.form[key] === 'string')) return null;
      if (value.version !== null && (!Number.isInteger(value.version) || value.version < 0)) return null;
      if (value.recordId != null && (!Number.isSafeInteger(value.recordId) || value.recordId < 1)) return null;
      return value;
    } catch {
      state.storageError = '本机草稿暂时不可用，请及时保存记录';
      return null;
    }
  };
  const removeDraft = (userId, date) => {
    try { storage.remove(draftKey(userId, date)); }
    catch { state.storageError = '本机草稿暂时无法清理，请稍后重试'; }
  };
  const writeDraft = (userId, date, form, version, recordId) => {
    try { storage.set(draftKey(userId, date), { form: { ...form }, version, recordId, writerId }); }
    catch { state.storageError = '本机草稿暂时无法保存，请及时保存记录'; }
  };
  const persistDraft = () => {
    if (!state.userId || !state.date || !state.loaded) return;
    if (sameForm(state.form, recordForm(state.record))) {
      if (ownsDraft(readDraft(state.userId, state.date), state.formRecordId, state.formVersion)) removeDraft(state.userId, state.date);
    }
    else writeDraft(state.userId, state.date, state.form, state.formVersion, state.formRecordId);
  };
  const failure = (error, action) => {
    if (error?.statusCode === 409) {
      state.conflict = true;
      return '这天的记录已在其他地方更新，当前输入已保留，请先载入最新记录';
    }
    if (error?.statusCode === 401 || error?.statusCode === 403) return '登录已失效，请重新登录后重试';
    return error?.data?.message || `${action}失败，请重试。当前输入已保留`;
  };
  async function loadDay(userId, date, { discardDraft = false } = {}) {
    persistDraft();
    const sequence = ++daySequence;
    state.loaded = false;
    Object.assign(state, { userId, date, record: null, latestHeight: null, form: blankForm(), formVersion: null, formRecordId: null,
      loaded: false, loading: !!userId, editing: false, saving: false, error: '', conflict: false, draftRestored: false, heightError: '' });
    if (!userId) return;
    try {
      const result = await api.getDay(date);
      if (sequence !== daySequence) return;
      state.record = result.record || null;
      state.latestHeight = result.latestHeight || null;
      state.form = recordForm(state.record);
      state.formVersion = state.record?.version ?? null;
      state.formRecordId = state.record?.id ?? null;
      state.loaded = true;
      if (discardDraft) removeDraft(userId, date);
      const draft = discardDraft ? null : readDraft(userId, date);
      if (draft && !sameForm(draft.form, state.form)) {
        state.formVersion = draft.version;
        state.formRecordId = draft.recordId ?? null;
        state.form = { ...draft.form };
        state.editing = true;
        state.draftRestored = true;
      }
    } catch (error) {
      if (sequence === daySequence) state.error = failure(error, '加载记录');
    } finally {
      if (sequence === daySequence) state.loading = false;
    }
  }
  async function loadMonth(userId, monthKey) {
    const sequence = ++monthSequence;
    if (userId !== monthUser || month !== monthKey) state.recordDates = [];
    monthUser = userId; month = monthKey;
    state.monthLoading = !!userId;
    state.monthError = '';
    if (!userId) { state.recordDates = []; return; }
    try {
      const records = await api.getMonth(monthKey);
      if (sequence === monthSequence) state.recordDates = records.map(record => record.recordDate);
    } catch {
      if (sequence === monthSequence) state.monthError = '日历记录标记加载失败';
    } finally {
      if (sequence === monthSequence) state.monthLoading = false;
    }
  }
  async function refreshHeight(sequence, userId, date, version) {
    try {
      const result = await api.getDay(date);
      if (sequence === daySequence && state.userId === userId && (state.record?.version ?? null) === version) {
        state.latestHeight = result.latestHeight || null;
        state.heightError = '';
      }
    } catch {
      if (sequence === daySequence) {
        state.latestHeight = null;
        state.heightError = '最近身高暂时无法读取';
      }
    }
  }
  async function save() {
    if (!state.loaded || !state.userId || state.saving) return false;
    state.error = ''; state.conflict = false;
    let payload;
    try { payload = { ...validateRecordForm(state.form), version: state.formVersion, recordId: state.formRecordId }; }
    catch (error) { state.error = error.message; return false; }
    persistDraft();
    const sequence = daySequence, userId = state.userId, date = state.date, submittedForm = { ...state.form };
    state.saving = true;
    try {
      const saved = await api.saveDay(date, payload);
      const draft = readDraft(userId, date);
      if (ownsDraft(draft, payload.recordId, payload.version)) {
        if (sameForm(draft.form, submittedForm)) removeDraft(userId, date);
        else writeDraft(userId, date, draft.form, saved.version, saved.id);
      }
      if (sequence !== daySequence) return true;
      const unchanged = sameForm(state.form, submittedForm);
      state.record = saved;
      state.formVersion = saved.version;
      state.formRecordId = saved.id;
      state.draftRestored = false;
      if (unchanged) { state.form = recordForm(saved); state.editing = false; }
      else { state.editing = true; persistDraft(); }
      state.saving = false;
      await refreshHeight(sequence, userId, date, saved.version);
      return true;
    } catch (error) {
      if (sequence === daySequence) { state.error = failure(error, '保存记录'); persistDraft(); }
      return false;
    } finally {
      if (sequence === daySequence) state.saving = false;
    }
  }
  async function remove() {
    if (!state.record || state.saving) return false;
    if (state.formRecordId !== state.record.id || state.formVersion !== state.record.version) {
      state.error = failure({ statusCode: 409 }, '删除记录');
      return false;
    }
    const sequence = daySequence, userId = state.userId, date = state.date, version = state.record.version, recordId = state.record.id;
    const submittedForm = { ...state.form };
    state.saving = true; state.error = ''; state.conflict = false;
    try {
      await api.deleteDay(date, version, recordId);
      const draft = readDraft(userId, date);
      if (ownsDraft(draft, recordId, version)) {
        if (!sameForm(draft.form, submittedForm)) writeDraft(userId, date, draft.form, null, null);
        else removeDraft(userId, date);
      }
      if (state.userId !== userId || state.date !== date || !state.loaded || state.record?.id !== recordId || state.record?.version !== version) return true;
      const hasNewInput = !sameForm(state.form, submittedForm);
      state.loaded = false;
      Object.assign(state, { record: null, latestHeight: null, formVersion: null, formRecordId: null,
        editing: hasNewInput, draftRestored: hasNewInput, saving: false });
      if (!hasNewInput) state.form = blankForm();
      state.loaded = true;
      if (hasNewInput) persistDraft();
      await refreshHeight(daySequence, userId, date, null);
      return true;
    } catch (error) {
      if (sequence === daySequence) state.error = failure(error, '删除记录');
      return false;
    } finally {
      if (sequence === daySequence) state.saving = false;
    }
  }
  return { loadDay, loadMonth, save, remove, persistDraft,
    beginEdit() { if (state.loaded && !state.saving) state.editing = true; },
    closeEditor() { persistDraft(); state.editing = false; },
    dispose() { persistDraft(); ++daySequence; ++monthSequence; },
  };
}
