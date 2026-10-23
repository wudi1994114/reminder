/** Canvas-only rendering; no per-cell view nodes. Sizes are in physical pixels. */
export async function getCanvasSurface(id, scope) {
  // #ifdef H5
  const host = document.getElementById(id);
  const element = host && (host.tagName === 'CANVAS' ? host : host.querySelector('canvas'));
  if (element) return { canvas: element, ctx: element.getContext('2d') };
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
export function drawPattern(surface, pattern, cellSize) {
  const { ctx } = surface;
  resizeSurface(surface, pattern.width * cellSize, pattern.height * cellSize);
  ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, surface.canvas.width, surface.canvas.height);
  pattern.cells.forEach((index, offset) => {
    if (index < 0 || !pattern.palette[index]) return;
    ctx.fillStyle = pattern.palette[index].hex;
    ctx.fillRect((offset % pattern.width) * cellSize, Math.floor(offset / pattern.width) * cellSize, cellSize, cellSize);
  });
  ctx.beginPath(); ctx.strokeStyle = 'rgba(44,35,20,.25)'; ctx.lineWidth = .5;
  for (let x = 0; x <= pattern.width; x++) { ctx.moveTo(x * cellSize, 0); ctx.lineTo(x * cellSize, pattern.height * cellSize); }
  for (let y = 0; y <= pattern.height; y++) { ctx.moveTo(0, y * cellSize); ctx.lineTo(pattern.width * cellSize, y * cellSize); }
  ctx.stroke();
}
export function exportTiles(pattern, tileSize = 48) {
  const tiles = [];
  for (let y = 0; y < pattern.height; y += tileSize) for (let x = 0; x < pattern.width; x += tileSize) {
    tiles.push({ x, y, width: Math.min(tileSize, pattern.width - x), height: Math.min(tileSize, pattern.height - y) });
  }
  return tiles;
}
export function drawExport(surface, pattern, materials, tile, part, total, brand, credit = '') {
  const cell = 32, left = 58, top = 110;
  const width = Math.max(720, left + tile.width * cell + 24);
  const legendTop = top + tile.height * cell + 42;
  const maxCharacters = Math.floor((width - 48) / 7);
  const creditLines = [];
  for (const word of String(credit).split(/\s+/).filter(Boolean)) {
    const last = creditLines.length - 1;
    if (last < 0 || creditLines[last].length + word.length + 1 > maxCharacters) creditLines.push(word);
    else creditLines[last] += ` ${word}`;
  }
  const height = legendTop + 64 + Math.ceil(materials.length / 4) * 28 + 38 + creditLines.length * 18;
  resizeSurface(surface, width, height);
  const { ctx } = surface;
  ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, width, height);
  ctx.fillStyle = '#241e14'; ctx.font = 'bold 25px sans-serif'; ctx.fillText('备忘鸡 · 拼豆图纸', 24, 38);
  ctx.font = '17px sans-serif';
  ctx.fillText(`${brand} · ${pattern.width} 列 × ${pattern.height} 行 · 第 ${part}/${total} 张`, 24, 69);
  ctx.font = '12px sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
  for (let x = 0; x < tile.width; x++) { ctx.fillStyle = '#514737'; ctx.fillText(String(tile.x + x + 1), left + (x + .5) * cell, top - 16); }
  for (let y = 0; y < tile.height; y++) {
    ctx.fillStyle = '#514737'; ctx.fillText(String(tile.y + y + 1), left - 22, top + (y + .5) * cell);
    for (let x = 0; x < tile.width; x++) {
      const index = pattern.cells[(tile.y + y) * pattern.width + tile.x + x];
      const color = pattern.palette[index]; const px = left + x * cell, py = top + y * cell;
      ctx.fillStyle = color ? color.hex : '#fff'; ctx.fillRect(px, py, cell, cell);
      ctx.strokeStyle = '#b9b4ab'; ctx.lineWidth = .5; ctx.strokeRect(px, py, cell, cell);
      if (color) {
        const rgb = color.rgb || [parseInt(color.hex.slice(1, 3), 16), parseInt(color.hex.slice(3, 5), 16), parseInt(color.hex.slice(5, 7), 16)];
        ctx.fillStyle = rgb[0] * .299 + rgb[1] * .587 + rgb[2] * .114 > 145 ? '#171717' : '#fff';
        ctx.font = `${color.code.length > 4 ? 8 : 10}px sans-serif`; ctx.fillText(color.code, px + cell / 2, py + cell / 2);
      }
    }
  }
  ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic'; ctx.fillStyle = '#241e14'; ctx.font = 'bold 19px sans-serif';
  ctx.fillText(`整幅材料表 · 共 ${materials.reduce((sum, color) => sum + color.count, 0)} 颗`, 24, legendTop + 14);
  ctx.font = '15px sans-serif';
  materials.forEach((color, index) => {
    const x = 24 + (index % 4) * ((width - 48) / 4), y = legendTop + 40 + Math.floor(index / 4) * 28;
    ctx.fillStyle = color.hex; ctx.fillRect(x, y - 15, 20, 20); ctx.strokeStyle = '#aaa'; ctx.strokeRect(x, y - 15, 20, 20);
    ctx.fillStyle = '#241e14'; ctx.fillText(`${color.code}   ${color.count} 颗`, x + 29, y);
  });
  ctx.fillStyle = '#746958'; ctx.font = '13px sans-serif';
  const footerY = height - creditLines.length * 18 - 17;
  ctx.fillText('屏幕颜色仅供参考，请以实物色卡为准。透明格留空。', 24, footerY);
  ctx.font = '12px sans-serif';
  creditLines.forEach((line, index) => ctx.fillText(line, 24, footerY + 18 * (index + 1)));
}
export function surfaceFile(surface, scope) {
  // #ifdef H5
  return Promise.resolve(surface.canvas.toDataURL('image/png'));
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => uni.canvasToTempFilePath({ canvas: surface.canvas, fileType: 'png', success: result => resolve(result.tempFilePath), fail: reject }, scope));
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
