import { lab, toLinear, distance } from './color.mjs';

/** Bounded local bilateral smoothing before quantization; never fills transparent cells. */
export function smoothPoints(points, width, height, strength) {
  if (!strength) return points;
  const range = strength === 1 ? 12 : 18, edge = 24 ** 2;
  let source = points;
  for (let pass = 0; pass < strength; pass++) {
    const output = source.map((point, i) => {
      if (!point) return null;
      const x = i % width, y = Math.floor(i / width);
      const adjacent = [x ? i - 1 : -1, x < width - 1 ? i + 1 : -1, y ? i - width : -1, y < height - 1 ? i + width : -1];
      // Protect both sides of strong edges, including single-cell pupils/nose/strokes.
      if (adjacent.some(n => n >= 0 && source[n] && distance(point, source[n]) > edge)) return point;
      let weight = 1, sum = [...point];
      for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
        if ((!dx && !dy) || x + dx < 0 || x + dx >= width || y + dy < 0 || y + dy >= height) continue;
        const neighbor = source[(y + dy) * width + x + dx];
        if (!neighbor) continue;
        const d = distance(point, neighbor);
        if (d >= range ** 2) continue;
        const w = (dx && dy ? 0.5 : 1) * (1 - d / range ** 2) ** 2;
        weight += w;
        for (let j = 0; j < 3; j++) sum[j] += neighbor[j] * w;
      }
      return sum.map(v => v / weight);
    });
    source = output;
  }
  return source;
}

/** Cluster similar source colors before voting, so antialiasing does not split a flat color's vote. */
function dominant(buckets, alpha, mean) {
  const groups = [...buckets.values()].map(b => ({
    weight: b[0], point: lab(b[1] / b[0], b[2] / b[0], b[3] / b[0]),
  })).sort((a, b) => b.weight - a.weight);
  // Bounded candidate count keeps high-detail photos usable even in illustration mode.
  const candidates = groups.slice(0, 24);
  const dark = groups.filter(b => b.point[0] < 35 && Math.hypot(b.point[1], b.point[2]) < 30);
  if (dark.length && !candidates.includes(dark[0])) candidates.push(dark[0]);
  let best = null, bestWeight = -1, darkBest = null, darkWeight = 0;
  for (const seed of candidates) {
    let weight = 0, l = 0, a = 0, b = 0;
    for (const group of groups) if (distance(seed.point, group.point) <= 100) {
      weight += group.weight;
      l += group.point[0] * group.weight;
      a += group.point[1] * group.weight;
      b += group.point[2] * group.weight;
    }
    const point = [l / weight, a / weight, b / weight];
    if (weight > bestWeight) { bestWeight = weight; best = point; }
    if (point[0] < 35 && Math.hypot(point[1], point[2]) < 30 && weight > darkWeight) {
      darkBest = point; darkWeight = weight;
    }
  }
  // A supported dark stroke should survive against a much lighter fill. Tiny dark specks do not.
  if (darkBest && darkWeight / alpha >= 0.23 && best[0] - darkBest[0] > 32) return darkBest;
  return best || mean;
}

/** Exact area coverage, alpha premultiplication and linear-light means; transparent cells remain empty. */
export function sampleCells(image, width, height, mode) {
  const { width: sw, height: sh, data } = image, result = [];
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const x0 = x * sw / width, x1 = (x + 1) * sw / width;
    const y0 = y * sh / height, y1 = (y + 1) * sh / height;
    let alpha = 0, r = 0, g = 0, b = 0;
    const buckets = mode === 'illustration' ? new Map() : null;
    for (let sy = Math.floor(y0); sy < Math.ceil(y1); sy++) for (let sx = Math.floor(x0); sx < Math.ceil(x1); sx++) {
      if (sx >= sw || sy >= sh) continue;
      const weight = (Math.min(sx + 1, x1) - Math.max(sx, x0)) * (Math.min(sy + 1, y1) - Math.max(sy, y0));
      const offset = (sy * sw + sx) * 4, a = data[offset + 3] / 255 * weight;
      if (!a) continue;
      const lr = toLinear(data[offset]) * a, lg = toLinear(data[offset + 1]) * a, lb = toLinear(data[offset + 2]) * a;
      alpha += a; r += lr; g += lg; b += lb;
      if (buckets) {
        const key = (data[offset] >> 4) * 256 + (data[offset + 1] >> 4) * 16 + (data[offset + 2] >> 4);
        const bin = buckets.get(key) || [0, 0, 0, 0];
        bin[0] += a; bin[1] += lr; bin[2] += lg; bin[3] += lb; buckets.set(key, bin);
      }
    }
    if (alpha / ((x1 - x0) * (y1 - y0)) < 0.25) { result.push(null); continue; }
    const mean = lab(r / alpha, g / alpha, b / alpha);
    result.push(buckets ? dominant(buckets, alpha, mean) : mean);
  }
  return result;
}
