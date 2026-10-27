const DAY = 86400000;
const FIRST_DAY = Date.parse('0001-01-01T00:00:00Z');
export const MEASUREMENTS = { weightKg: { label: '体重', unit: 'kg' }, heightCm: { label: '身高', unit: 'cm' } };

function dateTime(date) {
  if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('日期无效');
  const time = Date.parse(`${date}T00:00:00Z`);
  if (!Number.isFinite(time) || time < FIRST_DAY || new Date(time).toISOString().slice(0, 10) !== date) throw new Error('日期无效');
  return time;
}
const isoDate = time => new Date(time).toISOString().slice(0, 10);

export function trendWindow(end, days) {
  if (![30, 90, 365].includes(days)) throw new Error('时间范围无效');
  const endTime = dateTime(end);
  return { start: isoDate(Math.max(FIRST_DAY, endTime - (days - 1) * DAY)), end, days };
}

export function measurementPoints(rows, range) {
  if (!Array.isArray(rows)) throw new Error('读取记录失败，请重试');
  const byDate = new Map();
  for (const row of rows) {
    try { dateTime(row.recordDate); } catch { continue; }
    if (row.recordDate < range.start || row.recordDate > range.end || row.value == null || !['number', 'string'].includes(typeof row.value) || String(row.value).trim() === '') continue;
    const value = Number(row.value);
    if (Number.isFinite(value) && value > 0) byDate.set(row.recordDate, { recordDate: row.recordDate, value });
  }
  return [...byDate.values()].sort((a, b) => a.recordDate.localeCompare(b.recordDate));
}

export function trendSummary(points, selectedIndex = points.length - 1) {
  if (!points.length) return null;
  const first = points[0], latest = points[points.length - 1];
  const selected = points[selectedIndex] || latest;
  return { first, latest, selected, count: points.length, change: Math.round((latest.value - first.value) * 10) / 10, selectedChange: Math.round((selected.value - first.value) * 10) / 10 };
}

export function trendLayout(points, range, width, height = 176) {
  const left = 42, right = Math.max(180, width) - 10, top = 14, bottom = height - 28;
  const values = points.map(point => point.value);
  const low = values.length ? Math.min(...values) : 0, high = values.length ? Math.max(...values) : 1;
  const pad = Math.max(.5, (high - low) * .15);
  const min = Math.max(0, Math.floor((low - pad) * 10) / 10), max = Math.ceil((high + pad) * 10) / 10;
  const start = dateTime(range.start), end = dateTime(range.end), span = Math.max(DAY, end - start);
  return { width, height, left, right, top, bottom, min, max,
    points: points.map(point => ({ ...point, x: left + (dateTime(point.recordDate) - start) / span * (right - left), y: bottom - (point.value - min) / (max - min) * (bottom - top) })),
    ticks: [min, (min + max) / 2, max].map(value => ({ value, y: bottom - (value - min) / (max - min) * (bottom - top) })),
    dates: [start, start + Math.floor((end - start) / DAY / 2) * DAY, end].map(time => ({ date: isoDate(time), x: left + (time - start) / span * (right - left) })),
  };
}

export function nearestTrendPoint(layout, x) {
  if (!Number.isFinite(x) || !layout.points.length) return -1;
  return layout.points.reduce((nearest, point, index) => Math.abs(point.x - x) < Math.abs(layout.points[nearest].x - x) ? index : nearest, 0);
}

export function trendPointerX(event, rect, detailIsLocal = false) {
  const touch = event.changedTouches?.[0] || event.touches?.[0];
  if (Number.isFinite(touch?.x)) return touch.x;
  const clientX = touch?.clientX ?? event.clientX ?? event.detail?.clientX;
  if (Number.isFinite(clientX)) return clientX - rect.left;
  if (Number.isFinite(event.detail?.x)) return detailIsLocal ? event.detail.x : event.detail.x - rect.left;
  return NaN;
}

export function createTrendState() {
  return { points: [], loading: false, error: '', selectedIndex: -1, range: null };
}

export function createTrendLoader({ state, getTrend }) {
  let sequence = 0;
  return {
    async load({ userId, metric, end, days }) {
      const current = ++sequence;
      Object.assign(state, { points: [], selectedIndex: -1, error: '', loading: !!userId, range: null });
      if (!userId) return false;
      try {
        if (!Object.prototype.hasOwnProperty.call(MEASUREMENTS, metric)) throw new Error('测量指标无效');
        const range = trendWindow(end, days);
        const rows = await getTrend({ metric, end, days });
        if (current !== sequence) return false;
        state.points = measurementPoints(rows, range);
        state.selectedIndex = state.points.length - 1;
        state.range = range;
        return true;
      } catch (error) {
        if (current === sequence) state.error = [401, 403].includes(error?.statusCode) ? '请重新登录后查看变化' : error?.data?.message || error?.message || '读取记录失败，请重试';
        return false;
      } finally {
        if (current === sequence) state.loading = false;
      }
    },
    dispose() { ++sequence; Object.assign(state, { points: [], selectedIndex: -1, range: null, loading: false }); },
  };
}

export function drawTrend(context, layout, selectedIndex) {
  context.setFillStyle('#fffdf8'); context.fillRect(0, 0, layout.width, layout.height);
  context.setFontSize(11); context.setTextAlign('right'); context.setTextBaseline('middle');
  for (const tick of layout.ticks) {
    context.setStrokeStyle('#ebe2d2'); context.setLineWidth(1); context.beginPath(); context.moveTo(layout.left, tick.y); context.lineTo(layout.right, tick.y); context.stroke();
    context.setFillStyle('#6d604c'); context.fillText(tick.value.toFixed(1), layout.left - 7, tick.y);
  }
  context.setTextBaseline('top');
  layout.dates.forEach((tick, index) => {
    context.setTextAlign(index === 0 ? 'left' : index === layout.dates.length - 1 ? 'right' : 'center');
    context.fillText(`${Number(tick.date.slice(5, 7))}/${Number(tick.date.slice(8, 10))}`, tick.x, layout.bottom + 9);
  });
  const selected = layout.points[selectedIndex];
  if (selected) {
    context.setStrokeStyle('#cbb991'); context.setLineWidth(1); context.beginPath(); context.moveTo(selected.x, layout.top); context.lineTo(selected.x, layout.bottom); context.stroke();
  }
  if (layout.points.length > 1) {
    context.setStrokeStyle('#996619'); context.setLineWidth(2); context.beginPath();
    layout.points.forEach((point, index) => index ? context.lineTo(point.x, point.y) : context.moveTo(point.x, point.y));
    context.stroke();
  }
  layout.points.forEach((point, index) => {
    if (layout.points.length > 60 && index !== selectedIndex && index !== 0 && index !== layout.points.length - 1) return;
    context.setFillStyle(index === selectedIndex ? '#78551c' : '#c99435');
    context.beginPath(); context.arc(point.x, point.y, index === selectedIndex ? 4 : 2.5, 0, 2 * Math.PI); context.fill();
  });
  context.draw();
}
