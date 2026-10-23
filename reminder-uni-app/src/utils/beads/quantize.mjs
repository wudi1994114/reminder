import { distance, nearest } from './color.mjs';

/** Minimize aggregate source-color error over a subset of actual, available bead colors. */
export function selectColors(points, labs, maxColors, mode, width) {
  const histogram = new Map();
  points.forEach((point, cell) => {
    if (!point) return;
    const i = nearest(point, labs);
    let importance = 1;
    if (mode === 'illustration') {
      // Give high-contrast small features extra influence without counting absent colors.
      const adjacent = [cell % width ? cell - 1 : -1, (cell + 1) % width ? cell + 1 : -1, cell - width, cell + width];
      const contrast = adjacent.reduce((m, n) => points[n] ? Math.max(m, distance(point, points[n])) : m, 0);
      importance += Math.min(2, contrast / 1200);
    }
    const bin = histogram.get(i) || { index: i, weight: 0, sum: [0, 0, 0] };
    bin.weight += importance;
    point.forEach((v, j) => { bin.sum[j] += v * importance; });
    histogram.set(i, bin);
  });
  const bins = [...histogram.values()].map(b => {
    const point = b.sum.map(v => v / b.weight);
    // Diminishing area weight stops large muted backgrounds from spending every color slot.
    // Saturated accents carry identity even when they occupy only a small part of the image.
    const chroma = Math.hypot(point[1], point[2]);
    return { ...b, point, mass: b.weight, weight: b.weight ** 0.8 * (1 + Math.min(3, chroma ** 2 / 900)) };
  });
  if (bins.length <= maxColors) return bins.map(b => b.index);
  const candidates = bins.map(b => b.index), selected = [], locked = new Set();
  // Reserve representatives for substantial vivid hue families before optimizing background tones.
  // They come only from source colors, and their total reservation stays below the color budget.
  if (maxColors >= 4) {
    const families = new Map();
    for (const b of bins) {
      const chroma = Math.hypot(b.point[1], b.point[2]);
      if (chroma < 28) continue;
      const hue = (Math.atan2(b.point[2], b.point[1]) * 180 / Math.PI + 360) % 360;
      const key = Math.floor(((hue + 30) % 360) / 60), group = families.get(key) || [];
      group.push(b); families.set(key, group);
    }
    const groups = [...families.values()].map(group => ({
      group, mass: group.reduce((s, b) => s + b.mass, 0),
      score: group.reduce((s, b) => s + b.weight * Math.hypot(b.point[1], b.point[2]), 0),
    })).filter(g => g.mass >= Math.max(1, Math.floor(points.length * 0.001))).sort((a, b) => b.score - a.score);
    for (const { group } of groups.slice(0, Math.min(4, Math.ceil(maxColors / 2)))) {
      const weight = group.reduce((s, b) => s + b.weight, 0);
      const center = [0, 1, 2].map(j => group.reduce((s, b) => s + b.point[j] * b.weight, 0) / weight);
      const index = group.reduce((best, b) => distance(center, labs[b.index]) < distance(center, labs[best]) ? b.index : best, group[0].index);
      selected.push(index); locked.add(index);
    }
  }
  if (mode === 'illustration' && maxColors >= 3) {
    for (const predicate of [p => p[0] < 25 && Math.hypot(p[1], p[2]) < 25, p => p[0] > 92 && Math.hypot(p[1], p[2]) < 12]) {
      const bin = bins.filter(b => predicate(b.point)).sort((a, b) => b.weight - a.weight)[0];
      if (bin && !locked.has(bin.index) && selected.length < maxColors) { selected.push(bin.index); locked.add(bin.index); }
    }
  }
  if (!selected.length) {
    let best = candidates[0], bestLoss = Infinity;
    for (const i of candidates) {
      const loss = bins.reduce((s, b) => s + b.weight * distance(b.point, labs[i]), 0);
      if (loss < bestLoss) { best = i; bestLoss = loss; }
    }
    selected.push(best);
  }
  let errors = bins.map(b => Math.min(...selected.map(i => distance(b.point, labs[i]))));
  while (selected.length < maxColors) {
    let best = -1, bestGain = -1;
    for (const i of candidates) {
      if (selected.includes(i)) continue;
      const gain = bins.reduce((s, b, j) => s + b.weight * Math.max(0, errors[j] - distance(b.point, labs[i])), 0);
      if (gain > bestGain) { bestGain = gain; best = i; }
    }
    if (best < 0) break;
    selected.push(best);
    errors = bins.map((b, j) => Math.min(errors[j], distance(b.point, labs[best])));
  }
  // Deterministic constrained medoid refinement: each change can only lower its cluster's error.
  for (let iteration = 0; iteration < 3; iteration++) {
    let changed = false;
    const chosen = selected.map(i => labs[i]);
    const clusters = selected.map(() => []);
    for (const b of bins) clusters[nearest(b.point, chosen)].push(b);
    for (let j = 0; j < selected.length; j++) {
      if (locked.has(selected[j]) || !clusters[j].length) continue;
      const loss = i => clusters[j].reduce((s, b) => s + b.weight * distance(b.point, labs[i]), 0);
      let best = selected[j], bestLoss = loss(best);
      for (let i = 0; i < labs.length; i++) {
        if (selected.includes(i)) continue;
        const next = loss(i);
        if (next < bestLoss - 1e-8) { best = i; bestLoss = next; }
      }
      if (best !== selected[j]) { selected[j] = best; changed = true; }
    }
    if (!changed) break;
  }
  return selected;
}

/** Remove only small, low-contrast islands surrounded by a consistent nontransparent region. */
export function cleanIslands(cells, points, labs, width, height, strength) {
  if (!strength) return cells;
  const output = [...cells], visited = new Uint8Array(cells.length), threshold = strength === 1 ? 12 : 18;
  const neighbors = i => [i % width ? i - 1 : -1, (i + 1) % width ? i + 1 : -1, i >= width ? i - width : -1, i < width * (height - 1) ? i + width : -1];
  for (let seed = 0; seed < cells.length; seed++) {
    if (visited[seed] || cells[seed] < 0) continue;
    const component = [seed], border = new Map(); visited[seed] = 1;
    let open = false;
    for (let cursor = 0; cursor < component.length; cursor++) for (const n of neighbors(component[cursor])) {
      if (n < 0 || cells[n] < 0) { open = true; continue; }
      if (cells[n] === cells[seed]) {
        if (!visited[n]) { visited[n] = 1; component.push(n); }
      } else border.set(cells[n], (border.get(cells[n]) || 0) + 1);
    }
    if (open || component.length > strength || !border.size) continue;
    const [replacement, count] = [...border].sort((a, b) => b[1] - a[1] || a[0] - b[0])[0];
    const total = [...border.values()].reduce((s, v) => s + v, 0);
    if (count / total < 0.75 || distance(labs[cells[seed]], labs[replacement]) > threshold ** 2) continue;
    // Quantization can collapse a strong source edge into two very similar available beads.
    if (component.some(i => neighbors(i).some(n => n >= 0 && points[n] && distance(points[i], points[n]) > (threshold * 1.5) ** 2))) continue;
    if (component.some(i => distance(points[i], labs[replacement]) - distance(points[i], labs[cells[i]]) > threshold ** 2)) continue;
    for (const i of component) output[i] = replacement;
  }
  return output;
}
