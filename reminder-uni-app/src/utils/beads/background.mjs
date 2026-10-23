import { lab, toLinear, distance } from './color.mjs';
import { binaryCut } from './cut.mjs';

/** Fit a bounded color/texture Gaussian mixture, with deterministic farthest-point initialization. */
const featureDistance = (a, b) => a.reduce((sum, v, i) => sum + (v - b[i]) ** 2, 0);
function mixture(points, labels, wanted, k) {
  const samples = [];
  for (let i = 0; i < points.length; i++) if (labels[i] === wanted) samples.push(points[i]);
  if (!samples.length) return null;
  const means = [samples[Math.floor(samples.length / 2)].slice()];
  while (means.length < k) {
    let best = samples[0], error = -1;
    for (let i = 0; i < samples.length; i += 2) {
      const d = Math.min(...means.map(m => featureDistance(samples[i], m)));
      if (d > error) { error = d; best = samples[i]; }
    }
    if (error < 1) break;
    means.push(best.slice());
  }
  let counts, sums, squares;
  for (let iteration = 0; iteration < 6; iteration++) {
    counts = means.map(() => 0); sums = means.map(() => Array(means[0].length).fill(0)); squares = means.map(() => Array(means[0].length).fill(0));
    for (const point of samples) {
      let best = 0, error = Infinity;
      for (let j = 0; j < means.length; j++) { const d = featureDistance(point, means[j]); if (d < error) { best = j; error = d; } }
      counts[best]++;
      for (let j = 0; j < point.length; j++) { sums[best][j] += point[j]; squares[best][j] += point[j] ** 2; }
    }
    for (let i = 0; i < means.length; i++) if (counts[i]) means[i] = sums[i].map(v => v / counts[i]);
  }
  return means.map((mean, i) => ({ mean, variance: mean.map((v, j) => Math.max(6, squares[i][j] / Math.max(1, counts[i]) - v ** 2)), weight: counts[i] / samples.length })).filter(m => m.weight > 0);
}
function cost(point, model) {
  let best = Infinity;
  for (const { mean, variance, weight } of model) {
    let value = -Math.log(weight);
    for (let j = 0; j < point.length; j++) value += 0.5 * ((point[j] - mean[j]) ** 2 / variance[j] + Math.log(variance[j]));
    best = Math.min(best, value);
  }
  return best;
}

/** Local center-subject extraction. Color models and image edges, no semantic model or network. */
export function removeBackground(image) {
  const output = { width: image.width, height: image.height, data: new Uint8ClampedArray(image.data) };
  let empty = 0;
  for (let i = 3; i < image.data.length; i += 4) if (image.data[i] < 128) empty++;
  // Existing cutouts already encode their intended silhouette and disconnected details.
  if (empty > image.width * image.height * 0.05) return output;
  const scale = Math.min(1, 192 / Math.max(image.width, image.height));
  const width = Math.max(1, Math.round(image.width * scale)), height = Math.max(1, Math.round(image.height * scale));
  if (width < 8 || height < 8) return output;
  const points = [], seeds = new Int8Array(width * height).fill(-1);
  let labels = new Uint8Array(width * height);
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const sx = Math.min(image.width - 1, Math.floor((x + 0.5) * image.width / width)), sy = Math.min(image.height - 1, Math.floor((y + 0.5) * image.height / height));
    const offset = (sy * image.width + sx) * 4, i = y * width + x;
    const point = lab(toLinear(image.data[offset]), toLinear(image.data[offset + 1]), toLinear(image.data[offset + 2]));
    const centerLuminance = image.data[offset] * 0.2126 + image.data[offset + 1] * 0.7152 + image.data[offset + 2] * 0.0722;
    let luminance = 0, square = 0, samples = 0;
    for (let dy = -2; dy <= 2; dy++) for (let dx = -2; dx <= 2; dx++) {
      const px = Math.max(0, Math.min(image.width - 1, sx + dx)), py = Math.max(0, Math.min(image.height - 1, sy + dy));
      const p = (py * image.width + px) * 4;
      const value = image.data[p] * 0.2126 + image.data[p + 1] * 0.7152 + image.data[p + 2] * 0.0722;
      if (Math.abs(value - centerLuminance) > 24) continue;
      luminance += value; square += value * value; samples++;
    }
    point.push(Math.min(40, Math.sqrt(Math.max(0, square / samples - (luminance / samples) ** 2)) * 2));
    points.push(point);
    const nx = (x + 0.5) / width, ny = (y + 0.5) / height;
    labels[i] = Number(nx > 0.10 && nx < 0.94 && ny > 0.08 && ny < 0.97);
    if (x === 0 || y === 0 || x === width - 1 || y === height - 1) seeds[i] = 0;
    if (((nx - 0.5) / 0.10) ** 2 + ((ny - 0.52) / 0.15) ** 2 < 1) seeds[i] = 2;
  }
  if (points.every(point => distance(point, points[0]) < 1)) {
    for (let i = 3; i < output.data.length; i += 4) output.data[i] = 0;
    return output;
  }
  // The center is only a search area. Hard foreground requires evidence against the border model.
  const borderLabels = new Uint8Array(points.length).fill(1);
  for (let i = 0; i < seeds.length; i++) if (seeds[i] === 0) borderLabels[i] = 0;
  const borderModel = mixture(points, borderLabels, 0, 4);
  const borderCosts = points.filter((_, i) => seeds[i] === 0).map(p => cost(p, borderModel)).sort((a, b) => a - b);
  const confidence = borderCosts[Math.floor((borderCosts.length - 1) * 0.9)] + 4;
  let foregroundSeeds = 0;
  const hasEvidence = i => Math.min(...borderModel.map(m => distance(points[i], m.mean))) > 9 ** 2 && cost(points[i], borderModel) > confidence;
  for (let i = 0; i < seeds.length; i++) if (seeds[i] === 2) {
    seeds[i] = hasEvidence(i) ? 1 : -1;
    if (seeds[i] === 1) foregroundSeeds++;
  }
  // A pale chest or face can match the floor; search the wider interior before declaring it empty.
  if (!foregroundSeeds) for (let i = 0; i < seeds.length; i++) {
    const nx = (i % width + 0.5) / width, ny = (Math.floor(i / width) + 0.5) / height;
    if (seeds[i] < 0 && ((nx - 0.5) / 0.35) ** 2 + ((ny - 0.52) / 0.4) ** 2 < 1 && hasEvidence(i)) {
      seeds[i] = 1; foregroundSeeds++;
    }
  }
  if (!foregroundSeeds) {
    for (let i = 3; i < output.data.length; i += 4) output.data[i] = 0;
    return output;
  }
  const pairs = []; let variation = 0;
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const i = y * width + x;
    for (const [dx, dy] of [[1, 0], [0, 1]]) if (x + dx < width && y + dy < height) {
      const n = (y + dy) * width + x + dx, d = distance(points[i], points[n]);
      pairs.push([i, n, d]); variation += d;
    }
  }
  const beta = pairs.length / Math.max(1, 2 * variation);
  for (const pair of pairs) pair[2] = 18 * Math.exp(-beta * pair[2]);
  for (let iteration = 0; iteration < 4; iteration++) {
    const foreground = mixture(points, labels, 1, 5), background = mixture(points, labels, 0, 4);
    if (!foreground || !background) return output;
    const fg = new Float64Array(points.length), bg = new Float64Array(points.length);
    for (let i = 0; i < points.length; i++) {
      if (seeds[i] >= 0) { bg[i] = seeds[i] ? 10000 : 0; fg[i] = seeds[i] ? 0 : 10000; continue; }
      const f = cost(points[i], foreground), b = cost(points[i], background), base = Math.min(f, b);
      const nx = (i % width + 0.5) / width, ny = (Math.floor(i / width) + 0.5) / height;
      const outside = ((nx - 0.5) / 0.42) ** 2 + ((ny - 0.54) / 0.48) ** 2;
      fg[i] = f - base + 0.7 + Math.max(0, outside - 0.8) * 3; bg[i] = b - base;
    }
    labels = binaryCut(bg, fg, pairs);
  }
  // Keep only foreground connected to the central seed, rejecting detached floor lines and specks.
  const keep = new Uint8Array(labels.length), queue = new Int32Array(labels.length);
  let read = 0, write = 0;
  for (let i = 0; i < labels.length; i++) if (seeds[i] === 1 && labels[i]) { keep[i] = 1; queue[write++] = i; }
  while (read < write) {
    const i = queue[read++], x = i % width;
    for (const n of [x ? i - 1 : -1, x + 1 < width ? i + 1 : -1, i - width, i + width]) if (n >= 0 && n < labels.length && labels[n] && !keep[n]) { keep[n] = 1; queue[write++] = n; }
  }
  // Holes inside a subject (eyes, pale muzzle, highlights) are not exterior background.
  const exterior = new Uint8Array(labels.length); read = 0; write = 0;
  for (let i = 0; i < labels.length; i++) if (!keep[i] && (i < width || i >= width * (height - 1) || i % width === 0 || i % width === width - 1)) {
    exterior[i] = 1; queue[write++] = i;
  }
  while (read < write) {
    const i = queue[read++], x = i % width;
    for (const n of [x ? i - 1 : -1, x + 1 < width ? i + 1 : -1, i - width, i + width]) if (n >= 0 && n < labels.length && !keep[n] && !exterior[n]) { exterior[n] = 1; queue[write++] = n; }
  }
  for (let i = 0; i < keep.length; i++) if (!exterior[i]) keep[i] = 1;
  for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
    const i = Math.min(height - 1, Math.floor(y * height / image.height)) * width + Math.min(width - 1, Math.floor(x * width / image.width));
    if (!keep[i]) output.data[(y * image.width + x) * 4 + 3] = 0;
  }
  return output;
}
