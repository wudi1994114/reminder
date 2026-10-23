/** Local-only bead conversion. Transparent cells are -1; palette entries are owned copies. */
import { lab, toLinear, nearestColor } from './color.mjs';
import { sampleCells, smoothPoints } from './sampling.mjs';
import { selectColors, cleanIslands } from './quantize.mjs';
import { removeBackground } from './background.mjs';
const copyColor = (c) => ({ ...c, rgb: [...c.rgb] });
/** columns is the target width (16..128); fit both dimensions within 128, preserving aspect ratio. */
export function convertToPattern(
  image,
  { columns = 48, maxColors = 24, palette, excludedCodes = [], mode = 'photo', cleanup = 0, background = 'keep' } = {},
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
  if (!['photo', 'illustration'].includes(mode)) throw new RangeError('Invalid processing mode');
  if (![0, 1, 2].includes(cleanup)) throw new RangeError('cleanup must be 0, 1 or 2');
  if (!['keep', 'remove'].includes(background)) throw new RangeError('Invalid background option');
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
  const source = background === 'remove' ? removeBackground(image) : image;
  const points = smoothPoints(sampleCells(source, width, height, mode), width, height, cleanup),
    labs = available.map((c) => lab(...c.rgb.map(toLinear)));
  const selected = selectColors(points, labs, maxColors, mode, width);
  const chosen = selected.map((i) => labs[i]);
  const mapped = points.map((p) => (p ? nearestColor(p, chosen) : -1));
  const cells = cleanIslands(mapped, points, chosen, width, height, cleanup);
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
