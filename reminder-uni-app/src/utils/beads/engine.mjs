/** Local-only bead conversion. Transparent cells are -1; palette entries are owned copies. */
const LINEAR = Array.from({ length: 256 }, (_, v) => {
  v /= 255;
  return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
});
const toLinear = (v) => LINEAR[Math.max(0, Math.min(255, Math.round(v)))];
function lab(r, g, b) {
  const f = (v) => (v > 216 / 24389 ? Math.cbrt(v) : ((24389 / 27) * v + 16) / 116);
  const x = f((r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047);
  const y = f(r * 0.2126729 + g * 0.7151522 + b * 0.072175);
  const z = f((r * 0.0193339 + g * 0.119192 + b * 0.9503041) / 1.08883);
  return [116 * y - 16, 500 * (x - y), 200 * (y - z)];
}
const distance = (a, b) => (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2;
const copyColor = (c) => ({ ...c, rgb: [...c.rgb] });
function nearest(point, colors) {
  let best = 0,
    d = Infinity;
  for (let i = 0; i < colors.length; i++) {
    const next = distance(point, colors[i]);
    if (next < d) {
      d = next;
      best = i;
    }
  }
  return best;
}
/** Box area resampling, premultiplied by alpha, in linear sRGB; alpha below 1/4 becomes an empty bead. */
function sample(image, width, height) {
  const { width: sw, height: sh, data } = image,
    result = [];
  for (let y = 0; y < height; y++)
    for (let x = 0; x < width; x++) {
      const x0 = (x * sw) / width,
        x1 = ((x + 1) * sw) / width,
        y0 = (y * sh) / height,
        y1 = ((y + 1) * sh) / height;
      let alpha = 0,
        r = 0,
        g = 0,
        b = 0;
      for (let sy = Math.floor(y0); sy < Math.ceil(y1); sy++)
        for (let sx = Math.floor(x0); sx < Math.ceil(x1); sx++) {
          if (sx >= sw || sy >= sh) continue;
          const weight =
            (Math.min(sx + 1, x1) - Math.max(sx, x0)) * (Math.min(sy + 1, y1) - Math.max(sy, y0));
          const offset = (sy * sw + sx) * 4,
            a = (data[offset + 3] / 255) * weight;
          alpha += a;
          r += toLinear(data[offset]) * a;
          g += toLinear(data[offset + 1]) * a;
          b += toLinear(data[offset + 2]) * a;
        }
      result.push(
        alpha / ((x1 - x0) * (y1 - y0)) < 0.25 ? null : lab(r / alpha, g / alpha, b / alpha),
      );
    }
  return result;
}
/** columns is the target width (16..128); fit both dimensions within 128, preserving aspect ratio. */
export function convertToPattern(
  image,
  { columns = 48, maxColors = 24, palette, excludedCodes = [] } = {},
) {
  if (
    !image ||
    !Number.isInteger(image.width) ||
    !Number.isInteger(image.height) ||
    image.width < 1 ||
    image.height < 1 ||
    !image.data ||
    image.data.length !== image.width * image.height * 4
  )
    throw new TypeError('Invalid RGBA image');
  if (!Number.isInteger(columns) || columns < 16 || columns > 128)
    throw new RangeError('columns must be 16..128');
  if (!Number.isInteger(maxColors) || maxColors < 1)
    throw new RangeError('maxColors must be a positive integer');
  if (!Array.isArray(palette) || !palette.length)
    throw new TypeError('A color palette is required');
  const excluded = new Set(excludedCodes),
    available = palette.filter((c) => !excluded.has(c.code));
  if (!available.length) throw new RangeError('No available palette colors');
  for (const c of available)
    if (
      !Array.isArray(c.rgb) ||
      c.rgb.length !== 3 ||
      c.rgb.some((v) => !Number.isFinite(v) || v < 0 || v > 255)
    )
      throw new TypeError('Invalid palette RGB');
  let width = columns;
  let height = Math.max(1, Math.round((columns * image.height) / image.width));
  if (height > 128) {
    height = 128;
    width = Math.max(1, Math.round((128 * image.width) / image.height));
  }
  const points = sample(image, width, height),
    labs = available.map((c) => lab(...c.rgb.map(toLinear)));
  // Histogram of closest palette colors. Select a weighted, perceptually diverse subset.
  const histogram = new Map();
  for (const point of points)
    if (point) {
      const i = nearest(point, labs);
      histogram.set(i, (histogram.get(i) || 0) + 1);
    }
  const entries = [...histogram].sort((a, b) => b[1] - a[1] || a[0] - b[0]);
  const selected = [];
  if (entries.length <= maxColors) selected.push(...entries.map((e) => e[0]));
  else {
    selected.push(entries[0][0]);
    while (selected.length < maxColors) {
      let best = -1,
        score = -1;
      for (const [i, count] of entries) {
        if (selected.includes(i)) continue;
        const next = count * Math.min(...selected.map((j) => distance(labs[i], labs[j])));
        if (next > score) {
          best = i;
          score = next;
        }
      }
      selected.push(best);
    }
  }
  const chosen = selected.map((i) => labs[i]);
  const cells = points.map((p) => (p ? nearest(p, chosen) : -1));
  // Drop any selected entry unused after rematching, so materials and palette stay consistent.
  const used = [...new Set(cells.filter((i) => i >= 0))].sort((a, b) => a - b),
    remap = new Map(used.map((i, j) => [i, j]));
  return {
    width,
    height,
    cells: cells.map((i) => (i < 0 ? -1 : remap.get(i))),
    palette: used.map((i) => copyColor(available[selected[i]])),
  };
}
function validatePattern(pattern) {
  if (
    !pattern ||
    !Number.isInteger(pattern.width) ||
    !Number.isInteger(pattern.height) ||
    pattern.width < 1 ||
    pattern.height < 1 ||
    !Array.isArray(pattern.cells) ||
    pattern.cells.length !== pattern.width * pattern.height ||
    !Array.isArray(pattern.palette)
  )
    throw new TypeError('Invalid pattern');
}
function validateIndex(pattern, index) {
  if (!Number.isInteger(index) || index < -1 || index >= pattern.palette.length)
    throw new RangeError('Invalid palette index');
}
export function countMaterials(pattern) {
  validatePattern(pattern);
  const counts = new Map();
  for (const i of pattern.cells) {
    validateIndex(pattern, i);
    if (i >= 0) counts.set(i, (counts.get(i) || 0) + 1);
  }
  return [...counts]
    .sort((a, b) => b[1] - a[1] || a[0] - b[0])
    .map(([index, count]) => ({ ...copyColor(pattern.palette[index]), index, count }));
}
function copyPattern(pattern, cells) {
  return { ...pattern, cells, palette: pattern.palette.map(copyColor) };
}
export function replaceColor(pattern, fromIndex, toIndex) {
  validatePattern(pattern);
  validateIndex(pattern, fromIndex);
  validateIndex(pattern, toIndex);
  return copyPattern(
    pattern,
    pattern.cells.map((i) => (i === fromIndex ? toIndex : i)),
  );
}
export function paintCell(pattern, x, y, index) {
  validatePattern(pattern);
  validateIndex(pattern, index);
  if (
    !Number.isInteger(x) ||
    !Number.isInteger(y) ||
    x < 0 ||
    y < 0 ||
    x >= pattern.width ||
    y >= pattern.height
  )
    throw new RangeError('Cell out of bounds');
  const cells = [...pattern.cells];
  cells[y * pattern.width + x] = index;
  return copyPattern(pattern, cells);
}
