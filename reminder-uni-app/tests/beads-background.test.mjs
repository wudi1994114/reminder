import test from 'node:test';
import assert from 'node:assert/strict';
import { removeBackground } from '../src/utils/beads/background.mjs';
function image(width,height,pixel) {
  const data=new Uint8ClampedArray(width*height*4);
  for(let y=0;y<height;y++)for(let x=0;x<width;x++)data.set(pixel(x,y),(y*width+x)*4);
  return {width,height,data};
}
test('local background removal keeps a centered subject and removes connected background lines',()=>{
  const input=image(64,80,(x,y)=>((x-32)/19)**2+((y-42)/30)**2<1 ? [190,125,55,255] : Math.abs(y-x-23)<1?[40,40,40,255]:[225,224,218,255]);
  const original=Array.from(input.data),p=removeBackground(input);
  assert.equal(p.width,64);assert.equal(p.height,80);
  assert.equal(p.data[(42*64+32)*4+3],255);
  for(const [x,y] of [[0,0],[0,79],[63,79],[7,30],[60,20]])assert.equal(p.data[(y*64+x)*4+3],0,`${x},${y}`);
  assert.equal(p.data[(17*64+32)*4+3],255,'subject top');
  assert.deepEqual(Array.from(input.data),original);
  assert.deepEqual(removeBackground(input),p);
});
test('already transparent art preserves alpha, hidden RGB and separate details without inventing background',()=>{
  const input=image(32,32,(x,y)=>x>8&&x<24&&y>5&&y<28?[240,110,25,255]:[0,255,0,0]);
  assert.deepEqual(removeBackground(input),input);
  assert.notEqual(removeBackground(input).data,input.data);
});
test('background-colored highlights enclosed by a subject are preserved',()=>{
  const input=image(64,80,(x,y)=>((x-32)/19)**2+((y-42)/30)**2<1 ? (x>24&&x<29&&y>30&&y<35?[225,224,218,255]:[190,125,55,255]):[225,224,218,255]);
  const p=removeBackground(input);
  assert.equal(p.data[(32*64+26)*4+3],255);
  assert.equal(p.data[3],0);
});
test('a uniform background cannot create an imaginary central subject',()=>{
  const input=image(32,40,()=>[225,224,218,255]);
  const p=removeBackground(input);
  assert.ok(Array.from(p.data).filter((_,i)=>i%4===3).every(v=>v===0));
});
test('near-blank noise and gentle gradients do not manufacture a central object',()=>{
  let seed=13;const random=()=>{seed=(seed*1664525+1013904223)>>>0;return seed/2**32;};
  for (const input of [image(64,64,()=>{const v=220+Math.floor(random()*6);return [v,v,v,255];}),image(64,64,x=>{const v=220+Math.floor(x/64*10);return [v,v,v,255];})]) {
    assert.ok(Array.from(removeBackground(input).data).filter((_,i)=>i%4===3).every(v=>v===0));
  }
});
test('a tiny centered subject does not force a large opaque selection ellipse',()=>{
  const input=image(64,64,(x,y)=>(x-32)**2+(y-32)**2<9?[170,90,30,255]:[225,225,225,255]);
  const output=removeBackground(input);const retained=Array.from(output.data).filter((v,i)=>i%4===3&&v>0).length;
  assert.ok(retained>=20 && retained<=40,`unexpected size ${retained}`);
});
test('a subject with a background-colored chest is found outside the central search patch',()=>{
  const input=image(64,64,(x,y)=>((x-32)/19)**2+((y-33)/27)**2<1 && !(x>23&&x<41&&y>20&&y<46)?[190,100,50,255]:[225,225,225,255]);
  const p=removeBackground(input);const retained=Array.from(p.data).filter((v,i)=>i%4===3&&v>0).length;
  assert.ok(retained>1000 && retained<2000,`subject retained ${retained}`);
});
