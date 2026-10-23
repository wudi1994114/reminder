/** Deterministic binary graph cut; label 1 is source/foreground. No recursive paths. */
export function binaryCut(backgroundCost, foregroundCost, pairs) {
  const count = backgroundCost.length, source = count, sink = count + 1, size = count + 2;
  const edgeCount = count * 4 + pairs.length * 2;
  const head = new Int32Array(size).fill(-1), next = new Int32Array(edgeCount), to = new Int32Array(edgeCount), capacity = new Float64Array(edgeCount);
  let used = 0;
  function link(a, b, ab, ba = 0) {
    to[used] = b; capacity[used] = ab; next[used] = head[a]; head[a] = used++;
    to[used] = a; capacity[used] = ba; next[used] = head[b]; head[b] = used++;
  }
  for (let i = 0; i < count; i++) { link(source, i, backgroundCost[i]); link(i, sink, foregroundCost[i]); }
  for (const [a, b, weight] of pairs) link(a, b, weight, weight);
  const levels = new Int32Array(size), queue = new Int32Array(size), current = new Int32Array(size), nodes = new Int32Array(size), path = new Int32Array(size);
  function bfs() {
    levels.fill(-1); levels[source] = 0;
    let read = 0, write = 1; queue[0] = source;
    while (read < write) {
      const a = queue[read++];
      for (let edge = head[a]; edge >= 0; edge = next[edge]) if (capacity[edge] > 1e-8 && levels[to[edge]] < 0) {
        levels[to[edge]] = levels[a] + 1; queue[write++] = to[edge];
      }
    }
    return levels[sink] >= 0;
  }
  while (bfs()) {
    current.set(head); let depth = 0; nodes[0] = source;
    while (depth >= 0) {
      const a = nodes[depth];
      if (a === sink) {
        let flow = Infinity;
        for (let i = 0; i < depth; i++) flow = Math.min(flow, capacity[path[i]]);
        let first = depth;
        for (let i = 0; i < depth; i++) {
          const edge = path[i]; capacity[edge] -= flow; capacity[edge ^ 1] += flow;
          if (capacity[edge] <= 1e-8 && first === depth) first = i;
        }
        depth = first; current[nodes[depth]] = next[path[depth]];
        continue;
      }
      let edge = current[a];
      while (edge >= 0 && (capacity[edge] <= 1e-8 || levels[to[edge]] !== levels[a] + 1)) edge = next[edge];
      current[a] = edge;
      if (edge >= 0) { path[depth] = edge; nodes[++depth] = to[edge]; }
      else {
        levels[a] = -1;
        if (--depth >= 0) current[nodes[depth]] = next[path[depth]];
      }
    }
  }
  const result = new Uint8Array(count);
  // Last unsuccessful BFS marks exactly the residual source side.
  for (let i = 0; i < count; i++) result[i] = Number(levels[i] >= 0);
  return result;
}
