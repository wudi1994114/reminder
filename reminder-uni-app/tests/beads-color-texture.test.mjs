import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import * as color from '../src/utils/beads/color.mjs';
import * as sampling from '../src/utils/beads/sampling.mjs';
import { selectColors, cleanIslands } from '../src/utils/beads/quantize.mjs';
const palette = JSON.parse(fs.readFileSync(new URL('../src/utils/beads/palette-data/mard-221.json', import.meta.url))).colors;
const labs = palette.map(c => color.lab(...c.rgb.map(color.toLinear)));
const warmShadow = [23.14330677616104, 4.3178011575174535, 15.949481147224532];

test('sampled warm cat shadow does not acquire green with the full bead palette', () => {
  assert.equal(typeof color.nearestColor, 'function');
  const matched = labs[color.nearestColor(warmShadow, labs)];
  assert.ok(matched[1] >= 0, `warm source mapped to a*=${matched[1]}`);
});
test('limited palette optimization uses the same hue protection as final matching', () => {
  const selected = selectColors(Array.from({length: 20}, () => warmShadow), labs, 1, 'photo', 5);
  assert.ok(labs[selected[0]][1] >= 0, `selected ${palette[selected[0]].code}`);
});
test('matching retains genuine green, blue and all exact bead colors', () => {
  assert.equal(typeof color.nearestColor, 'function');
  for (let i = 0; i < labs.length; i++) assert.equal(color.distance(labs[i], labs[color.nearestColor(labs[i], labs)]), 0);
  const green = palette.findIndex(c => c.code === 'B23');
  assert.equal(color.nearestColor([24, -10, 15], labs), green);
});
test('cleanup smooths low contrast texture before palette selection without mutating input', () => {
  assert.equal(typeof sampling.smoothPoints, 'function');
  const points = Array.from({length: 81}, (_,i) => [40 + (i % 2 ? 3 : -3), 8, 15]);
  const snapshot = structuredClone(points);
  const out = sampling.smoothPoints(points, 9, 9, 2);
  const variance = ps => ps.reduce((s,p) => s + (p[0] - 40) ** 2, 0) / ps.length;
  assert.ok(variance(out) < variance(points) * 0.25);
  assert.deepEqual(points, snapshot);
  assert.deepEqual(sampling.smoothPoints(points, 9, 9, 0), points);
});
test('cleanup preserves isolated dark facial features, green accents and transparency', () => {
  assert.equal(typeof sampling.smoothPoints, 'function');
  const points = Array.from({length: 49}, () => [45, 10, 18]);
  points[24] = [8, 1, 2];
  points[10] = [42, -20, 18];
  points[0] = null;
  for (const strength of [1,2]) {
    const out = sampling.smoothPoints(points, 7, 7, strength);
    assert.deepEqual(out[24], points[24]);
    assert.deepEqual(out[10], points[10]);
    assert.equal(out[0], null);
  }
});
test('cleanup does not blend across a high contrast boundary', () => {
  assert.equal(typeof sampling.smoothPoints, 'function');
  const points = Array.from({length: 49}, (_, i) => i % 7 < 3 ? [15, 3, 4] : [80, 8, 18]);
  assert.deepEqual(sampling.smoothPoints(points, 7, 7, 2), points);
});
test('island cleanup cannot reintroduce a warm-to-green hue shift', () => {
  const chosen = [[40, 5, 8], [40, -5, 8]];
  const cells = Array.from({length:9}, (_,i) => i === 4 ? 0 : 1);
  const points = cells.map(i => chosen[i]);
  assert.equal(cleanIslands(cells, points, chosen, 3, 3, 2)[4], 0);
});
