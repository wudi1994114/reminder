import test from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { createDailyRecordState, createDailyRecordSession, dateKey, validateRecordForm, draftKey } from '../src/utils/daily-records.mjs';

const row = (recordDate = '2026-10-27', extra = {}) => ({ id: 1, recordDate, weightKg: 62.5, heightCm: null, diary: '今天散步了', version: 0, ...extra });
const { reactive, watch } = createRequire(import.meta.url)('vue');
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
function fixture(overrides = {}) {
  const state = createDailyRecordState(), data = new Map();
  const storage = { get: key => data.get(key), set: (key, value) => data.set(key, structuredClone(value)), remove: key => data.delete(key) };
  const calls = [];
  const api = {
    getDay: async date => ({ record: row(date), latestHeight: { recordDate: '2026-10-01', heightCm: 175 } }),
    getMonth: async () => [row()],
    saveDay: async (date, value) => { calls.push(['save', date, value]); return row(date, { ...value, version: (value.version ?? -1) + 1 }); },
    deleteDay: async (date, version, recordId) => { calls.push(['delete', date, version, recordId]); },
    ...overrides,
  };
  const session = createDailyRecordSession({ state, api, storage });
  return { state, session, data, calls };
}

test('local calendar dates never shift through UTC and invalid dates are rejected', () => {
  assert.equal(dateKey(new Date(2026, 9, 27, 0, 10)), '2026-10-27');
  assert.throws(() => dateKey(new Date('invalid')));
});

test('fields are optional, numeric values are bounded, and blank records cannot be saved', () => {
  assert.deepEqual(validateRecordForm({ weightKg: ' 62.5 ', heightCm: '', diary: '' }), { weightKg: 62.5, heightCm: null, diary: null });
  assert.deepEqual(validateRecordForm({ weightKg: '', heightCm: '', diary: '  写一句话\n就好  ' }), { weightKg: null, heightCm: null, diary: '写一句话\n就好' });
  for (const value of ['0', '-1', '1000', '62.55', '1e2', 'Infinity', 'abc']) {
    assert.throws(() => validateRecordForm({ weightKg: value, diary: '' }), /体重/);
  }
  assert.throws(() => validateRecordForm({ heightCm: '300.1', diary: '' }), /身高/);
  assert.throws(() => validateRecordForm({ diary: 'x'.repeat(10001) }), /10000/);
  assert.throws(() => validateRecordForm({ diary: ' \n ' }), /至少/);
});

test('historical height stays a hint and never becomes a new measurement on a diary-only save', async () => {
  const { state, session, calls } = fixture({ getDay: async () => ({ record: null, latestHeight: { heightCm: 175, recordDate: '2026-10-01' } }) });
  await session.loadDay(7, '2026-10-27');
  session.beginEdit(); state.form.diary = '只写日记';
  assert.equal(state.form.heightCm, '');
  assert.equal(await session.save(), true);
  assert.deepEqual(calls[0], ['save', '2026-10-27', { weightKg: null, heightCm: null, diary: '只写日记', version: null, recordId: null }]);
});

test('drafts survive closing and date changes, and remain isolated by account and date', async () => {
  const { state, session, data } = fixture();
  await session.loadDay(7, '2026-10-27');
  session.beginEdit(); state.form.diary = '未完成的日记'; session.closeEditor();
  assert.equal(data.get(draftKey(7, '2026-10-27')).form.diary, '未完成的日记');
  await session.loadDay(8, '2026-10-27');
  assert.equal(state.form.diary, '今天散步了'); assert.equal(state.draftRestored, false);
  await session.loadDay(7, '2026-10-26');
  assert.equal(state.form.diary, '今天散步了');
  await session.loadDay(7, '2026-10-27');
  assert.equal(state.form.diary, '未完成的日记'); assert.equal(state.draftRestored, true);
  assert.equal(state.editing, true);
});

test('unchanged forms do not create drafts and reverting edits removes a draft', async () => {
  const { state, session, data } = fixture();
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); session.persistDraft();
  assert.equal(data.size, 0);
  state.form.diary = '改了'; session.persistDraft(); assert.equal(data.size, 1);
  state.form.diary = '今天散步了'; session.persistDraft(); assert.equal(data.size, 0);
});

test('late day requests cannot show another date or account and logout clears in-memory private data', async () => {
  const first = deferred();
  const { state, session } = fixture({ getDay: date => date === '2026-10-27' ? first.promise : Promise.resolve({ record: row(date), latestHeight: null }) });
  const pending = session.loadDay(7, '2026-10-27');
  await session.loadDay(8, '2026-10-26');
  first.resolve({ record: row('2026-10-27', { diary: '账号7的隐私' }), latestHeight: null }); await pending;
  assert.equal(state.date, '2026-10-26'); assert.equal(state.userId, 8);
  assert.notEqual(state.form.diary, '账号7的隐私');
  await session.loadDay(null, '2026-10-26');
  assert.equal(state.record, null); assert.equal(state.form.diary, ''); assert.equal(state.latestHeight, null);
});

test('save errors retain all input, preserve draft and support explicit retry', async () => {
  let fail = true;
  const { state, session, data } = fixture({ saveDay: async (date, payload) => { if (fail) throw { statusCode: 503 }; return row(date, { ...payload, version: 1 }); } });
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); state.form.diary = '不能丢';
  assert.equal(await session.save(), false); assert.match(state.error, /保存/);
  assert.equal(state.form.diary, '不能丢'); assert.equal(data.get(draftKey(7, state.date)).form.diary, '不能丢');
  fail = false; assert.equal(await session.save(), true); assert.equal(data.size, 0); assert.equal(state.editing, false);
});

test('a stale restored draft keeps its version and conflicts cannot silently overwrite newer data', async () => {
  const { state, session, data, calls } = fixture({ saveDay: async (date, payload) => { calls.push(['save', payload]); throw { statusCode: 409 }; } });
  data.set(draftKey(7, '2026-10-27'), { form: { weightKg: '61', heightCm: '', diary: '旧草稿' }, version: 0 });
  await session.loadDay(7, '2026-10-27');
  assert.equal(await session.save(), false); assert.equal(calls[0][1].version, 0);
  assert.equal(state.conflict, true); assert.equal(state.form.diary, '旧草稿');
  await session.loadDay(7, '2026-10-27', { discardDraft: true });
  assert.equal(state.form.diary, '今天散步了'); assert.equal(data.size, 0);
});

test('edits made while a save is pending retain a draft with the new server version', async () => {
  const pending = deferred();
  const { state, session, data } = fixture({ saveDay: () => pending.promise });
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); state.form.diary = '第一版';
  const saving = session.save(); state.form.diary = '继续写的第二版'; session.persistDraft();
  pending.resolve(row('2026-10-27', { diary: '第一版', version: 1 })); await saving;
  assert.equal(state.form.diary, '继续写的第二版'); assert.equal(state.editing, true);
  assert.equal(data.get(draftKey(7, state.date)).version, 1);
});

test('late save after switching days cannot overwrite the new date or erase its draft', async () => {
  const pending = deferred(); const { state, session, data } = fixture({ saveDay: () => pending.promise });
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); state.form.diary = '27日内容';
  const saving = session.save(); await session.loadDay(7, '2026-10-26');
  session.beginEdit(); state.form.diary = '26日内容'; session.persistDraft();
  pending.resolve(row('2026-10-27', { diary: '27日内容', version: 1 })); await saving;
  assert.equal(state.date, '2026-10-26'); assert.equal(state.form.diary, '26日内容');
  assert.equal(data.get(draftKey(7, '2026-10-26')).form.diary, '26日内容');
});

test('month markers are cleared on account switches and late responses cannot reintroduce old data', async () => {
  const pending = deferred(); let first = true;
  const { state, session } = fixture({ getMonth: () => { if (first) { first = false; return pending.promise; } return Promise.resolve([]); } });
  const loading = session.loadMonth(7, '2026-10'); await session.loadMonth(8, '2026-10');
  pending.resolve([row()]); await loading;
  assert.deepEqual(state.recordDates, []);
});

test('delete only targets the displayed date and version; failure retains the record', async () => {
  const { state, session, calls } = fixture();
  await session.loadDay(7, '2026-10-27');
  assert.equal(await session.remove(), true); assert.deepEqual(calls[0], ['delete', '2026-10-27', 0, 1]);
  assert.equal(state.record, null); assert.equal(state.form.diary, '');
  const failed = fixture({ deleteDay: async () => { throw { statusCode: 503 }; } });
  await failed.session.loadDay(7, '2026-10-27');
  assert.equal(await failed.session.remove(), false); assert.equal(failed.state.record.diary, '今天散步了');
});

test('local storage failure is visible and does not block an account save', async () => {
  const state = createDailyRecordState();
  const session = createDailyRecordSession({ state, api: { getDay: async () => ({ record: null, latestHeight: null }), saveDay: async (date, p) => row(date, p) }, storage: { get() { throw Error('blocked'); }, set() { throw Error('full'); }, remove() { throw Error('blocked'); } } });
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); state.form.diary = '仍然可以保存'; session.persistDraft();
  assert.match(state.storageError, /草稿/); assert.equal(await session.save(), true);
});

test('reactive autosave never writes a previous account form into the next date during reset', async () => {
  const state = reactive(createDailyRecordState()), data = new Map();
  const session = createDailyRecordSession({ state, api: { getDay: async date => ({ record: row(date), latestHeight: null }) }, storage: {
    get: key => data.get(key), set: (key, value) => data.set(key, JSON.parse(JSON.stringify(value))), remove: key => data.delete(key),
  } });
  const stop = watch(() => state.form, () => session.persistDraft(), { deep: true, flush: 'sync' });
  await session.loadDay(7, '2026-10-27'); session.beginEdit(); state.form.diary = '账号7的草稿';
  await session.loadDay(8, '2026-10-26');
  assert.equal(state.form.diary, '今天散步了'); assert.equal(data.has(draftKey(8, '2026-10-26')), false);
  await session.loadDay(7, '2026-10-27');
  assert.equal(state.form.diary, '账号7的草稿'); assert.equal(state.formVersion, 0);
  stop();
});

test('restored drafts keep the original record identity after another device deletes and recreates the day', async () => {
  const { state, session, data, calls } = fixture({ getDay: async date => ({ record: row(date, { id: 2 }), latestHeight: null }), saveDay: async (date, payload) => { calls.push(payload); throw { statusCode: 409 }; } });
  data.set(draftKey(7, '2026-10-27'), { form: { weightKg: '61', heightCm: '', diary: '原记录的草稿' }, version: 0, recordId: 1 });
  await session.loadDay(7, '2026-10-27');
  await session.save();
  assert.equal(calls[0].recordId, 1); assert.equal(state.conflict, true);
  assert.equal(data.get(draftKey(7, '2026-10-27')).recordId, 1);
});

test('late deletion retains and rebases a newly written draft after revisiting the same day', async () => {
  const state = reactive(createDailyRecordState()), data = new Map(), pending = deferred();
  const session = createDailyRecordSession({ state, api: { getDay: async date => ({ record: row(date), latestHeight: null }), deleteDay: () => pending.promise }, storage: {
    get: key => data.get(key), set: (key, value) => data.set(key, JSON.parse(JSON.stringify(value))), remove: key => data.delete(key),
  } });
  const stop = watch(() => state.form, () => session.persistDraft(), { deep: true, flush: 'sync' });
  await session.loadDay(7, '2026-10-27');
  const deleting = session.remove();
  await session.loadDay(7, '2026-10-26'); await session.loadDay(7, '2026-10-27');
  session.beginEdit(); state.form.diary = '删除期间又写的新日记';
  pending.resolve(); await deleting;
  const draft = data.get(draftKey(7, '2026-10-27'));
  assert.ok(draft, 'new draft must survive the old delete response');
  assert.equal(draft.form.diary, '删除期间又写的新日记');
  assert.equal(draft.version, null); assert.equal(draft.recordId, null);
  assert.equal(state.formVersion, null); assert.equal(state.formRecordId, null);
  stop();
});

test('a late create response cannot adopt another session draft from a deleted record', async () => {
  const data = new Map(), pending = deferred();
  const storage = { get: key => data.get(key), set: (key, value) => data.set(key, JSON.parse(JSON.stringify(value))), remove: key => data.delete(key) };
  const oldState = reactive(createDailyRecordState()), newState = reactive(createDailyRecordState());
  const oldSession = createDailyRecordSession({ state: oldState, storage, api: { getDay: async () => ({ record: row(), latestHeight: null }) } });
  const newSession = createDailyRecordSession({ state: newState, storage, api: { getDay: async () => ({ record: null, latestHeight: null }), saveDay: () => pending.promise } });
  const stops = [watch(() => oldState.form, () => oldSession.persistDraft(), { deep: true, flush: 'sync' }), watch(() => newState.form, () => newSession.persistDraft(), { deep: true, flush: 'sync' })];
  await oldSession.loadDay(7, '2026-10-27');
  await newSession.loadDay(7, '2026-10-27'); newSession.beginEdit(); newState.form.diary = '重建后的记录';
  const saving = newSession.save(); await newSession.loadDay(7, '2026-10-26');
  oldSession.beginEdit(); oldState.form.diary = '仍基于旧记录的另一份草稿';
  pending.resolve(row('2026-10-27', { id: 2, version: 0, diary: '重建后的记录' })); await saving;
  assert.equal(data.get(draftKey(7, '2026-10-27')).recordId, 1);
  assert.equal(data.get(draftKey(7, '2026-10-27')).version, 0);
  stops.forEach(stop => stop());
});

test('deleting an old record never clears or rebases a replacement record draft from another session', async () => {
  const data = new Map(), pending = deferred(), state = reactive(createDailyRecordState());
  const storage = { get: key => data.get(key), set: (key, value) => data.set(key, JSON.parse(JSON.stringify(value))), remove: key => data.delete(key) };
  const session = createDailyRecordSession({ state, storage, api: { getDay: async date => ({ record: row(date), latestHeight: null }), deleteDay: () => pending.promise } });
  const stop = watch(() => state.form, () => session.persistDraft(), { deep: true, flush: 'sync' });
  await session.loadDay(7, '2026-10-27'); const deleting = session.remove();
  data.set(draftKey(7, '2026-10-27'), { form: { weightKg: '', heightCm: '', diary: '重建记录的草稿' }, recordId: 2, version: 0 });
  pending.resolve(); await deleting;
  assert.equal(data.get(draftKey(7, '2026-10-27')).recordId, 2);
  assert.equal(data.get(draftKey(7, '2026-10-27')).version, 0);
  stop();
});

test('deleting from a restored stale editor cannot delete the replacement record', async () => {
  const { state, session, data, calls } = fixture({ getDay: async date => ({ record: row(date, { id: 2 }), latestHeight: null }) });
  data.set(draftKey(7, '2026-10-27'), { form: { weightKg: '', heightCm: '', diary: '旧记录草稿' }, recordId: 1, version: 0 });
  await session.loadDay(7, '2026-10-27');
  assert.equal(await session.remove(), false); assert.equal(state.conflict, true); assert.equal(calls.length, 0);
});

test('a successful save only rebases its own editor draft, never another tab with the same original version', async () => {
  const data = new Map(), pending = deferred();
  const storage = { get: key => data.get(key), set: (key, value) => data.set(key, JSON.parse(JSON.stringify(value))), remove: key => data.delete(key) };
  const stateA = createDailyRecordState(), stateB = createDailyRecordState();
  const api = { getDay: async () => ({ record: row(), latestHeight: null }), saveDay: () => pending.promise };
  const sessionA = createDailyRecordSession({ state: stateA, storage, api }), sessionB = createDailyRecordSession({ state: stateB, storage, api });
  await sessionA.loadDay(7, '2026-10-27'); await sessionB.loadDay(7, '2026-10-27');
  sessionA.beginEdit(); stateA.form.diary = 'A标签保存的日记'; const saving = sessionA.save();
  sessionB.beginEdit(); stateB.form.diary = 'B标签独立编写的日记'; sessionB.persistDraft();
  pending.resolve(row('2026-10-27', { diary: 'A标签保存的日记', version: 1 })); await saving;
  assert.equal(data.get(draftKey(7, '2026-10-27')).form.diary, 'B标签独立编写的日记');
  assert.equal(data.get(draftKey(7, '2026-10-27')).version, 0);
});
