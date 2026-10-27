import test from 'node:test';
import assert from 'node:assert/strict';
import { trendWindow, measurementPoints, trendSummary, trendLayout, nearestTrendPoint, trendPointerX, createTrendState, createTrendLoader } from '../src/utils/daily-record-trend.mjs';

const point = (recordDate, value) => ({ recordDate, value });
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };

test('inclusive windows cross month and leap-day boundaries without local timezone shifts', () => {
  assert.deepEqual(trendWindow('2026-10-27', 30), { start: '2026-09-28', end: '2026-10-27', days: 30 });
  assert.equal(trendWindow('2028-03-01', 30).start, '2028-02-01');
  assert.equal(trendWindow('0001-01-02', 365).start, '0001-01-01');
  for (const end of ['2026-02-30', '2026-2-01', '0000-01-01', 'bad']) assert.throws(() => trendWindow(end, 30), /日期/);
  for (const days of [0, 7, 366, '30']) assert.throws(() => trendWindow('2026-10-27', days), /范围/);
});

test('only real, valid points inside the window are plotted; missing days stay missing', () => {
  const window = trendWindow('2026-10-27', 30);
  const points = measurementPoints([point('2026-10-27', 62.5), point('2026-10-01', 63), point('2026-09-27', 90), point('2026-10-28', 10), point('2026-10-02', null), point('2026-10-03', 0), point('2026-10-04', Infinity), point('2026-02-30', 60)], window);
  assert.deepEqual(points, [point('2026-10-01', 63), point('2026-10-27', 62.5)]);
  assert.deepEqual(measurementPoints([], window), []);
});

test('summaries use first and last genuine measurements with decimal-safe changes', () => {
  assert.equal(trendSummary([]), null);
  assert.equal(trendSummary([point('2026-10-01', 62.3), point('2026-10-27', 62.5)]).change, 0.2);
  const one = trendSummary([point('2026-10-01', 175)]);
  assert.equal(one.count, 1); assert.equal(one.change, 0); assert.equal(one.latest.value, 175);
});

test('selecting an older point compares that selected value with the first measurement', () => {
  const summary = trendSummary([point('2026-10-01', 63.5), point('2026-10-13', 63.1), point('2026-10-27', 62.5)], 1);
  assert.equal(summary.selected.recordDate, '2026-10-13');
  assert.equal(summary.selectedChange, -0.4);
  assert.equal(summary.change, -1);
});

test('chart coordinates reflect uneven date intervals, stay bounded, and do not force a zero baseline', () => {
  const window = trendWindow('2026-10-27', 30);
  const layout = trendLayout([point('2026-10-01', 63), point('2026-10-02', 62.9), point('2026-10-27', 62.5)], window, 270, 176);
  const [a, b, c] = layout.points;
  assert.ok(c.x - b.x > 20 * (b.x - a.x));
  assert.ok(layout.min > 60 && layout.max < 65);
  assert.ok(layout.points.every(p => p.x >= layout.left && p.x <= layout.right && p.y >= layout.top && p.y <= layout.bottom));
  assert.equal(nearestTrendPoint(layout, b.x + 1), 1);
  assert.equal(nearestTrendPoint(layout, -10), 0);
  assert.equal(nearestTrendPoint(layout, 9999), 2);
});

test('single and flat series yield finite coordinates and readable nonzero axes', () => {
  for (const points of [[point('2026-10-27', 175)], [point('2026-10-01', 175), point('2026-10-27', 175)]]) {
    const layout = trendLayout(points, trendWindow('2026-10-27', 30), 240, 176);
    assert.ok(layout.max > layout.min);
    assert.ok(layout.points.every(p => Number.isFinite(p.x) && Number.isFinite(p.y)));
    assert.equal(layout.ticks.length, 3);
  }
  assert.equal(nearestTrendPoint(trendLayout([], trendWindow('2026-10-27', 30), 240), 20), -1);
});

test('canvas-local touches and viewport click coordinates select the same interior measurement', () => {
  const range = trendWindow('2026-10-27', 30), rect = { left: 26 };
  const points = Array.from({ length: 30 }, (_, index) => point(new Date(Date.parse(range.start+'T00:00:00Z')+index*86400000).toISOString().slice(0,10), 63-index/10));
  const layout = trendLayout(points, range, 270), selected = layout.points[12];
  assert.equal(selected.recordDate, '2026-10-10');
  for (const event of [{ detail: { x: selected.x + rect.left } }, { clientX: selected.x + rect.left }, { changedTouches: [{ x: selected.x }] }, { changedTouches: [{ clientX: selected.x + rect.left }] }]) {
    assert.equal(nearestTrendPoint(layout, trendPointerX(event, rect)), 12);
  }
  assert.equal(trendPointerX({ detail: { x: selected.x } }, rect, true), selected.x);
});

test('loading passes only metric/end/days and keeps the freshest metric result', async () => {
  const first = deferred(), state = createTrendState(), calls = [];
  const loader = createTrendLoader({ state, getTrend: args => { calls.push(args); return args.metric === 'weightKg' ? first.promise : Promise.resolve([point('2026-10-27', 175)]); } });
  const old = loader.load({ userId: 7, metric: 'weightKg', end: '2026-10-27', days: 30 });
  await loader.load({ userId: 7, metric: 'heightCm', end: '2026-10-27', days: 90 });
  first.resolve([point('2026-10-27', 62.5)]); await old;
  assert.deepEqual(calls[1], { metric: 'heightCm', end: '2026-10-27', days: 90 });
  assert.equal(state.points[0].value, 175); assert.equal(state.selectedIndex, 0); assert.equal(state.loading, false);
});

test('account switches and disposal clear private chart points and ignore old responses', async () => {
  const pending = deferred(), state = createTrendState();
  const loader = createTrendLoader({ state, getTrend: () => pending.promise });
  const old = loader.load({ userId: 7, metric: 'weightKg', end: '2026-10-27', days: 30 });
  await loader.load({ userId: null, metric: 'heightCm', end: '2026-10-27', days: 30 });
  assert.deepEqual(state.points, []);
  pending.resolve([point('2026-10-27', 62.5)]); await old;
  assert.deepEqual(state.points, []);
  const late = deferred();
  const second = createTrendLoader({ state, getTrend: () => late.promise });
  const request = second.load({ userId: 8, metric: 'heightCm', end: '2026-10-27', days: 30 });
  second.dispose(); late.resolve([point('2026-10-27', 180)]); await request;
  assert.deepEqual(state.points, []); assert.equal(state.loading, false);
});

test('errors do not look like empty history, and retry can recover', async () => {
  let fail = true; const state = createTrendState();
  const loader = createTrendLoader({ state, getTrend: async () => { if (fail) throw { statusCode: 503 }; return []; } });
  const args = { userId: 7, metric: 'weightKg', end: '2026-10-27', days: 30 };
  assert.equal(await loader.load(args), false); assert.match(state.error, /读取/); assert.equal(state.loading, false);
  fail = false; assert.equal(await loader.load(args), true); assert.equal(state.error, ''); assert.deepEqual(state.points, []);
  assert.equal(await loader.load({ ...args, metric: 'diary' }), false); assert.match(state.error, /指标/);
});
