/** Canvas-only rendering; no per-cell view nodes. Sizes are in physical pixels. */
export async function getCanvasSurface(id, scope) {
  // #ifdef H5
  const host = document.getElementById(id);
  if (host) {
    let element = host.tagName === 'CANVAS' ? host : host.querySelector('canvas');
    if (!element) { element = document.createElement('canvas'); host.appendChild(element); }
    if (element !== host) { element.style.width = '100%'; element.style.height = '100%'; }
    return { canvas: element, ctx: element.getContext('2d') };
  }
  // #endif
  return new Promise((resolve, reject) => {
    uni.createSelectorQuery().in(scope).select(`#${id}`).fields({ node: true, size: true }).exec(result => {
      const canvas = result[0] && result[0].node;
      if (!canvas) return reject(new Error('画布尚未准备好，请稍后重试'));
      resolve({ canvas, ctx: canvas.getContext('2d') });
    });
  });
}
export function resizeSurface(surface, width, height) {
  surface.canvas.width = Math.ceil(width);
  surface.canvas.height = Math.ceil(height);
}
export async function decodePhoto(surface, path, isCurrent = () => true) {
  const image = surface.canvas.createImage ? surface.canvas.createImage() : new Image();
  await new Promise((resolve, reject) => {
    image.onload = resolve;
    image.onerror = () => reject(new Error('无法读取这张图片，请重新选择'));
    image.src = path;
  });
  if (!isCurrent()) return null;
  const ratio = Math.min(1, 768 / Math.max(image.width, image.height));
  const width = Math.max(1, Math.round(image.width * ratio));
  const height = Math.max(1, Math.round(image.height * ratio));
  resizeSurface(surface, width, height);
  surface.ctx.clearRect(0, 0, width, height);
  surface.ctx.drawImage(image, 0, 0, width, height);
  return { width, height, data: surface.ctx.getImageData(0, 0, width, height).data };
}
function codeLabels(ctx, palette, cellSize, fontLimit = 12) {
  return palette.map(color => {
    const rgb = color.rgb || [1, 3, 5].map(start => parseInt(color.hex.slice(start, start + 2), 16));
    const linear = rgb.map(value => { const v = value / 255; return v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4; });
    const luminance = linear[0] * .2126 + linear[1] * .7152 + linear[2] * .0722;
    let size = Math.min(fontLimit, cellSize * .46);
    ctx.font = `${size}px sans-serif`;
    const width = ctx.measureText(color.code).width;
    if (width > cellSize * .86) size *= cellSize * .86 / width;
    return { text: color.code, font: `${size}px sans-serif`, fill: luminance > .179 ? '#000' : '#fff' };
  });
}
function drawGrid(ctx, width, height, cell, left = 0, top = 0, startX = 0, startY = 0, guides = true) {
  for (let axis = 0; axis < 2; axis++) {
    const count = axis ? height : width, start = axis ? startY : startX;
    for (let index = 0; index <= count; index++) {
      const position = start + index, major = guides && position % 10 === 0, minor = guides && position % 5 === 0;
      ctx.beginPath(); ctx.strokeStyle = major || minor ? '#c65b51' : 'rgba(44,35,20,.25)';
      ctx.lineWidth = major ? 1.2 : .5; ctx.setLineDash(minor && !major ? [3, 2] : []);
      if (axis) { ctx.moveTo(left, top + index * cell); ctx.lineTo(left + width * cell, top + index * cell); }
      else { ctx.moveTo(left + index * cell, top); ctx.lineTo(left + index * cell, top + height * cell); }
      ctx.stroke();
    }
  }
  ctx.setLineDash([]);
}
function drawCoordinates(ctx, width, height, cell, left, top, startX = 0, startY = 0) {
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.font = `${Math.min(12, cell * .36)}px sans-serif`;
  ctx.fillStyle = '#514737';
  for (let x = 0; x < width; x++) {
    const cx = left + (x + .5) * cell;
    ctx.fillText(String(startX + x + 1), cx, top - cell / 2);
    ctx.fillText(String(startX + x + 1), cx, top + (height + .5) * cell);
  }
  for (let y = 0; y < height; y++) {
    const cy = top + (y + .5) * cell;
    ctx.fillText(String(startY + y + 1), left - cell / 2, cy);
    ctx.fillText(String(startY + y + 1), left + (width + .5) * cell, cy);
  }
}
export function drawPattern(surface, pattern, cellSize, showCodes = false, labelVisibility) {
  const { ctx } = surface;
  const border = showCodes ? 1 : 0;
  // Bound bitmap memory independently of the enlarged, scrollable reading view.
  cellSize = Math.min(cellSize, 2048 / Math.max(pattern.width + border * 2, pattern.height + border * 2));
  const inset = border * cellSize;
  resizeSurface(surface, (pattern.width + border * 2) * cellSize, (pattern.height + border * 2) * cellSize);
  ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, surface.canvas.width, surface.canvas.height);
  const showLabels = showCodes && (labelVisibility ?? cellSize >= 12);
  const labels = showLabels ? codeLabels(ctx, pattern.palette, cellSize) : [];
  if (showCodes) {
    ctx.fillStyle = '#f1f1ef'; ctx.fillRect(0, 0, surface.canvas.width, surface.canvas.height);
    ctx.fillStyle = '#fff'; ctx.fillRect(inset, inset, pattern.width * cellSize, pattern.height * cellSize);
    drawCoordinates(ctx, pattern.width, pattern.height, cellSize, inset, inset);
  }
  pattern.cells.forEach((index, offset) => {
    if (index < 0 || !pattern.palette[index]) return;
    const x = inset + (offset % pattern.width) * cellSize, y = inset + Math.floor(offset / pattern.width) * cellSize;
    ctx.fillStyle = pattern.palette[index].hex;
    ctx.fillRect(x, y, cellSize, cellSize);
    if (showLabels) {
      const label = labels[index]; ctx.fillStyle = label.fill; ctx.font = label.font;
      ctx.fillText(label.text, x + cellSize / 2, y + cellSize / 2);
    }
  });
  drawGrid(ctx, pattern.width, pattern.height, cellSize, inset, inset, 0, 0, showCodes);
}
export function exportTiles(pattern, tileSize = 48) {
  const tiles = [];
  for (let y = 0; y < pattern.height; y += tileSize) for (let x = 0; x < pattern.width; x += tileSize) {
    tiles.push({ x, y, width: Math.min(tileSize, pattern.width - x), height: Math.min(tileSize, pattern.height - y) });
  }
  return tiles;
}
function exportLayout(tile, materials, credit, quality) {
  const left = 58, top = 110, legendColumns = materials.length > 64 ? 8 : 4;
  // Reduce cell pitch for large whole charts while retaining every row and column.
  for (let cell = quality === 'high' ? 64 : 32; cell >= 1; cell--) {
    const width = Math.max(720, legendColumns * 180 + 48, left + tile.width * cell + 58);
    const legendTop = top + tile.height * cell + 56;
    const maxCharacters = Math.floor((width - 48) / 7), creditLines = [];
    for (const word of String(credit).split(/\s+/).filter(Boolean)) {
      const last = creditLines.length - 1;
      if (last < 0 || creditLines[last].length + word.length + 1 > maxCharacters) creditLines.push(word);
      else creditLines[last] += ` ${word}`;
    }
    const height = legendTop + 64 + Math.ceil(materials.length / legendColumns) * 28 + 38 + creditLines.length * 18;
    if (width <= 4096 && height <= 4096) return {cell,left,top,width,height,legendTop,legendColumns,creditLines};
  }
  throw new Error('图纸内容过大，请选择分块导出');
}
export function drawExport(surface, pattern, materials, tile, part, total, brand, credit = '', quality = 'standard') {
  const {cell,left,top,width,height,legendTop,legendColumns,creditLines} = exportLayout(tile, materials, credit, quality);
  resizeSurface(surface, width, height);
  const { ctx } = surface;
  ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, width, height);
  ctx.fillStyle = '#241e14'; ctx.font = 'bold 25px sans-serif'; ctx.fillText('备忘鸡 · 拼豆图纸', 24, 38);
  ctx.font = '17px sans-serif';
  ctx.fillText(`${brand} · ${pattern.width} 列 × ${pattern.height} 行 · ${total === 1 ? '完整图' : `第 ${part}/${total} 张`}`, 24, 69);
  const labels = codeLabels(ctx, pattern.palette, cell, quality === 'high' ? 24 : 12);
  drawCoordinates(ctx, tile.width, tile.height, cell, left, top, tile.x, tile.y);
  for (let y = 0; y < tile.height; y++) {
    for (let x = 0; x < tile.width; x++) {
      const index = pattern.cells[(tile.y + y) * pattern.width + tile.x + x];
      const color = pattern.palette[index]; const px = left + x * cell, py = top + y * cell;
      ctx.fillStyle = color ? color.hex : '#fff'; ctx.fillRect(px, py, cell, cell);
      if (color) {
        const label = labels[index]; ctx.fillStyle = label.fill; ctx.font = label.font;
        ctx.fillText(label.text, px + cell / 2, py + cell / 2);
      }
    }
  }
  drawGrid(ctx, tile.width, tile.height, cell, left, top, tile.x, tile.y);
  ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic'; ctx.fillStyle = '#241e14'; ctx.font = 'bold 19px sans-serif';
  ctx.fillText(`整幅材料表 · 共 ${materials.reduce((sum, color) => sum + color.count, 0)} 颗`, 24, legendTop + 14);
  ctx.font = '15px sans-serif';
  materials.forEach((color, index) => {
    const x = 24 + (index % legendColumns) * ((width - 48) / legendColumns), y = legendTop + 40 + Math.floor(index / legendColumns) * 28;
    ctx.fillStyle = color.hex; ctx.fillRect(x, y - 15, 20, 20); ctx.strokeStyle = '#aaa'; ctx.strokeRect(x, y - 15, 20, 20);
    ctx.fillStyle = '#241e14'; ctx.fillText(`${color.code}   ${color.count} 颗`, x + 29, y);
  });
  ctx.fillStyle = '#746958'; ctx.font = '13px sans-serif';
  const footerY = height - creditLines.length * 18 - 17;
  ctx.fillText('屏幕颜色仅供参考，请以实物色卡为准。透明格留空。', 24, footerY);
  ctx.font = '12px sans-serif';
  creditLines.forEach((line, index) => ctx.fillText(line, 24, footerY + 18 * (index + 1)));
}
export async function surfaceFile(surface, scope) {
  // #ifdef H5
  return Promise.resolve(surface.canvas.toDataURL('image/png'));
  // #endif
  // #ifndef H5
  // Canvas 2D commands can reach the native renderer after the JS call returns.
  await new Promise(resolve => {
    const timer = setTimeout(resolve, 100);
    if (surface.canvas.requestAnimationFrame) surface.canvas.requestAnimationFrame(() => { clearTimeout(timer); resolve(); });
  });
  const { width, height } = surface.canvas;
  return new Promise((resolve, reject) => uni.canvasToTempFilePath({
    canvas: surface.canvas, x: 0, y: 0, width, height, destWidth: width, destHeight: height, fileType: 'png',
    success: result => result.tempFilePath ? resolve(result.tempFilePath) : reject(new Error('未能生成图纸图片，请重试')),
    fail: reject,
  }, scope));
  // #endif
}
export async function saveExport(path, name) {
  // #ifdef H5
  const link = document.createElement('a'); link.href = path; link.download = name; document.body.appendChild(link); link.click(); link.remove(); return;
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => uni.saveImageToPhotosAlbum({ filePath: path, success: resolve, fail: reject }));
  // #endif
}
