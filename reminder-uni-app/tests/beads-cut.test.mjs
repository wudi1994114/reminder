import test from 'node:test';
import assert from 'node:assert/strict';
import { binaryCut } from '../src/utils/beads/cut.mjs';
test('binary mincut matches exhaustive global optimum including competing boundaries',()=>{
  let seed=91;const random=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/2**32;};
  for(let trial=0;trial<30;trial++){
    const bg=Array.from({length:5},()=>random()*4),fg=Array.from({length:5},()=>random()*4);
    const edges=[[0,1,random()*5],[1,2,random()*5],[2,3,random()*5],[3,4,random()*5],[0,4,random()*5]];
    const energy=mask=>bg.reduce((sum,v,i)=>sum+(mask[i]?fg[i]:v),0)+edges.reduce((s,[a,b,w])=>s+(mask[a]!==mask[b]?w:0),0);
    let expected=Infinity;for(let m=0;m<32;m++)expected=Math.min(expected,energy(Array.from({length:5},(_,i)=>(m>>i)&1)));
    const actual=binaryCut(bg,fg,edges);assert.ok(Math.abs(energy(actual)-expected)<1e-5);
  }
});
