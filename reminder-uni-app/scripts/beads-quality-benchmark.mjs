/** Usage: node scripts/beads-quality-benchmark.mjs input.rgba.json output.json [baseline-engine.mjs]
 * Input: {width,height,data:[r,g,b,a,...]} decoded with the same Canvas path as the app.
 * This reports speed and isolated cells, not a subjective image-quality score.
 */
import { readFile, writeFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { performance } from 'node:perf_hooks';
import { convertToPattern } from '../src/utils/beads/engine.mjs';
import { BEAD_PALETTES } from '../src/utils/beads/palettes.mjs';
const [input, output, reference] = process.argv.slice(2);
if (!input || !output) throw new Error('Expected input.rgba.json output.json [baseline-engine.mjs]');
const image = JSON.parse(await readFile(input, 'utf8'));
image.data = new Uint8ClampedArray(image.data);
const baseline = reference ? (await import(pathToFileURL(resolve(reference)).href)).convertToPattern : null;
const results = [];
for (const [columns, maxColors] of [[48, 8], [64, 16], [96, 24], [128, 32]]) {
  for (const mode of [...(baseline ? ['baseline'] : []), 'illustration', 'photo']) {
    const fn = mode === 'baseline' ? baseline : convertToPattern;
    const options = { columns, maxColors, palette: BEAD_PALETTES[0].colors, mode: mode === 'baseline' ? 'photo' : mode, cleanup: mode === 'baseline' ? 0 : 1 };
    fn(image, options);
    const times = [];
    let pattern;
    for (let iteration = 0; iteration < 5; iteration++) {
      const start = performance.now(); pattern = fn(image, options); times.push(performance.now() - start);
    }
    const { cells, width } = pattern;
    const isolatedCells = cells.reduce((sum, color, i) => sum + Number(color >= 0 &&
      [i % width ? i - 1 : -1, (i + 1) % width ? i + 1 : -1, i - width, i + width]
        .every(n => n < 0 || n >= cells.length || cells[n] !== color)), 0);
    results.push({ columns, maxColors, mode, medianMs: times.sort((a, b) => a - b)[2], isolatedCells, pattern });
  }
}
await writeFile(output, JSON.stringify({ input, palette: BEAD_PALETTES[0].id, results }, null, 2));
console.log(JSON.stringify(results.map(({ pattern, ...result }) => ({ ...result, usedColors: pattern.palette.length })), null, 2));
