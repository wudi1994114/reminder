import test from 'node:test';
import assert from 'node:assert/strict';
import { assessImage } from '../src/utils/beads/assessment.mjs';

function image(width, height, pixel) {
  const data = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) data.set(pixel(x, y), (y * width + x) * 4);
  return { width, height, data };
}
const choices = result => [result.columns, result.maxColors];

test('simple transparent icon gets a small low-color pattern without changing the input', () => {
  const input = image(256, 256, (x, y) => x > 64 && x < 192 && y > 64 && y < 192 ? [200, 32, 48, 255] : [70, 40, 10, 0]);
  const before = input.data.slice();
  const result = assessImage(input);
  assert.deepEqual(choices(result), [32, 8]);
  assert.equal(result.mode, 'illustration');
  assert.equal(result.background, 'keep');
  assert.ok([0, 1, 2].includes(result.cleanup));
  assert.ok(result.reason.length > 8);
  assert.deepEqual(input.data, before);
  assert.deepEqual(assessImage(input), result);
});

test('hidden transparent RGB cannot change recommendations or diagnostics', () => {
  const make = hidden => image(128, 128, (x, y) => x > 40 && x < 90 && y > 40 && y < 90 ? [30, 100, 200, 255] : [...hidden(x, y), 0]);
  assert.deepEqual(assessImage(make(() => [0, 0, 0])), assessImage(make((x, y) => [x * y % 256, x * 3 % 256, y * 7 % 256])));
});

test('dense high contrast detail calls for more cells even with only two colors', () => {
  const detailed = image(256, 256, (x, y) => (Math.floor(x / 3) + Math.floor(y / 3)) % 2 ? [0, 0, 0, 255] : [255, 255, 255, 255]);
  assert.equal(assessImage(detailed).columns, 96);
});

test('broad color gradients do not count as fine structural detail', () => {
  const gradient = image(256, 256, (x, y) => [x, y, (x + y) / 2, 255]);
  assert.deepEqual(choices(assessImage(gradient)), [64, 16]);
});

test('natural color variation defaults to a moderate grid and smoothing', () => {
  const photo = image(256, 256, (x, y) => [80 + x / 3 + 8 * Math.sin(x * 2 + y), 50 + y / 3 + 8 * Math.sin(y * 3), 70 + (x + y) / 6, 255]);
  const result = assessImage(photo);
  assert.deepEqual(choices(result), [64, 16]);
  assert.ok(result.cleanup >= 1);
});

test('small detailed source is not needlessly enlarged to 96 columns', () => {
  const tiny = image(24, 24, (x, y) => [x * 41 % 256, y * 43 % 256, x * y * 17 % 256, 255]);
  assert.equal(assessImage(tiny).columns, 32);
});

test('fully transparent source has a safe small recommendation', () => {
  assert.deepEqual(choices(assessImage(image(20, 20, () => [240, 20, 180, 0]))), [32, 8]);
});

test('rejects malformed or non-finite RGBA image inputs', () => {
  for (const value of [null, {}, { width: 0, height: 1, data: [] }, { width: 1.5, height: 1, data: [0, 0, 0, 255] }, { width: 2, height: 2, data: [] }, { width: Infinity, height: 1, data: [] }, { width: 1, height: 1, data: [NaN, 0, 0, 255] }]) {
    assert.throws(() => assessImage(value), /image|RGBA/i);
  }
});

test('sampling does not erase an alternating one-pixel checkerboard', () => {
  const detailed = image(256, 256, (x, y) => (x + y) % 2 ? [0, 0, 0, 255] : [255, 255, 255, 255]);
  assert.equal(assessImage(detailed).columns, 96);
});

test('assessment work stays bounded for large source dimensions', () => {
  let reads = 0;
  const data = new Proxy({ length: 4096 * 4096 * 4 }, { get(target, key) {
    if (key === 'length') return target.length;
    reads++;
    return Number(key) % 4 === 3 ? 255 : 80;
  } });
  assert.equal(assessImage({ width: 4096, height: 4096, data }).columns, 32);
  assert.ok(reads <= 128 * 128 * 4);
});
