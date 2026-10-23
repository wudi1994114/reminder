/** Shared perceptual arithmetic. Inputs to lab() are linear-light RGB. */
const LINEAR = Array.from({ length: 256 }, (_, v) => {
  v /= 255;
  return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
});
export const toLinear = (v) => LINEAR[Math.max(0, Math.min(255, Math.round(v)))];
export function lab(r, g, b) {
  const f = (v) => (v > 216 / 24389 ? Math.cbrt(v) : ((24389 / 27) * v + 16) / 116);
  const x = f((r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047);
  const y = f(r * 0.2126729 + g * 0.7151522 + b * 0.072175);
  const z = f((r * 0.0193339 + g * 0.119192 + b * 0.9503041) / 1.08883);
  return [116 * y - 16, 500 * (x - y), 200 * (y - z)];
}
export const distance = (a, b) => (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2;
export function nearest(point, colors) {
  let best = 0, d = Infinity;
  for (let i = 0; i < colors.length; i++) {
    const next = distance(point, colors[i]);
    if (next < d) { d = next; best = i; }
  }
  return best;
}
