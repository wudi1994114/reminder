/** Bounded, deterministic local image statistics; no semantic subject recognition. */
const SAMPLE_SIDE = 128;
const distance = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]) / Math.sqrt(3);
const round = value => Math.round(value * 10000) / 10000;

/** Recommend bead settings from RGBA without changing or retaining the source. */
export function assessImage(image) {
  if (!image || !Number.isSafeInteger(image.width) || !Number.isSafeInteger(image.height) ||
      image.width < 1 || image.height < 1 || !Number.isSafeInteger(image.width * image.height * 4) ||
      !image.data || image.data.length !== image.width * image.height * 4) {
    throw new TypeError('Invalid RGBA image');
  }
  const { width, height, data } = image;
  const scale = Math.min(1, SAMPLE_SIDE / Math.max(width, height));
  const sw = Math.max(1, Math.round(width * scale)), sh = Math.max(1, Math.round(height * scale));
  const pixels = new Array(sw * sh), histogram = new Map();
  let visible = 0;
  // Stagger sample positions to avoid erasing alternating source pixels.
  for (let y = 0; y < sh; y++) for (let x = 0; x < sw; x++) {
    const sx = Math.min(width - 1, Math.floor((x + 0.25 + (x % 2) * 0.5) * width / sw));
    const sy = Math.min(height - 1, Math.floor((y + 0.25 + (y % 2) * 0.5) * height / sh));
    const i = (sy * width + sx) * 4, alpha = data[i + 3];
    if (!Number.isFinite(alpha) || alpha < 0 || alpha > 255) throw new TypeError('Invalid RGBA image values');
    // RGB under fully transparent pixels never contributes, including diagnostics.
    if (alpha === 0) continue;
    const rgb = [data[i], data[i + 1], data[i + 2]];
    if (rgb.some(v => !Number.isFinite(v) || v < 0 || v > 255)) throw new TypeError('Invalid RGBA image values');
    pixels[y * sw + x] = [...rgb, alpha / 255];
    const key = (rgb[0] >> 5) * 64 + (rgb[1] >> 5) * 8 + (rgb[2] >> 5);
    histogram.set(key, (histogram.get(key) || 0) + alpha / 255);
    visible += alpha / 255;
  }
  // Blurring first separates persistent contours from fine texture. Alpha-weighting
  // avoids counting arbitrary hidden RGB as texture at transparent boundaries.
  const blurred = pixels.map((pixel, i) => {
    if (!pixel) return null;
    const x = i % sw, y = Math.floor(i / sw), sum = [0, 0, 0];
    let weight = 0;
    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
      const nx = x + dx, ny = y + dy;
      if (nx < 0 || nx >= sw || ny < 0 || ny >= sh) continue;
      const other = pixels[ny * sw + nx];
      if (!other) continue;
      weight += other[3];
      for (let c = 0; c < 3; c++) sum[c] += other[c] * other[3];
    }
    return sum.map(v => v / weight);
  });
  let comparisons = 0, fineEdges = 0, structuralEdges = 0, texture = 0, pixelWeight = 0;
  for (let y = 0; y < sh; y++) for (let x = 0; x < sw; x++) {
    const i = y * sw + x, p = pixels[i];
    if (!p) continue;
    pixelWeight += p[3];
    if (distance(p, blurred[i]) > 18) texture += p[3];
    for (const j of [x + 1 < sw ? i + 1 : -1, y + 1 < sh ? i + sw : -1]) {
      const q = pixels[j];
      if (!q) continue;
      const weight = Math.min(p[3], q[3]);
      comparisons += weight;
      if (distance(p, q) > 40) fineEdges += weight;
      if (distance(blurred[i], blurred[j]) > 24) structuralEdges += weight;
    }
  }
  const colorBins = [...histogram.values()].filter(count => count >= Math.max(1, visible * 0.002)).length;
  const edgeDensity = fineEdges / (comparisons || 1), structureDensity = structuralEdges / (comparisons || 1);
  const textureDensity = texture / (pixelWeight || 1);
  const simple = colorBins <= 8 && edgeDensity < 0.08 && textureDensity < 0.08;
  const detailed = edgeDensity > 0.3 || structureDensity > 0.08;
  let columns = simple || visible === 0 ? 32 : detailed ? 96 : 64;
  // More output columns cannot create source information, particularly pixel icons.
  if (width <= 40) columns = 32;
  else if (width < 96) columns = Math.min(columns, 64);
  const maxColors = visible === 0 || (colorBins <= 8 && columns === 32) ? 8 : columns === 96 ? 24 : 16;
  const cleanup = simple || visible === 0 ? 0 : textureDensity > 0.25 && structureDensity < 0.05 ? 2 : 1;
  const reason = visible === 0 ? '图片为透明内容，先使用32格、8色。' :
    columns === 32 ? '图片色块较简洁或原图较小，推荐32格，减少豆子数量。' :
    columns === 96 ? '图片包含较多密集边缘，推荐96格、24色保留细节。' :
    '图片有一定颜色层次，推荐64格、16色平衡细节和豆子数量。';
  return {
    columns, maxColors, cleanup, mode: 'illustration', background: 'keep', reason,
    diagnostics: { sampledPixels: sw * sh, visibleFraction: round(visible / (sw * sh)), colorBins,
      edgeDensity: round(edgeDensity), structureDensity: round(structureDensity), textureDensity: round(textureDensity) },
  };
}
