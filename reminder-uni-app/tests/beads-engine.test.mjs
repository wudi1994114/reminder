import test from 'node:test';
import assert from 'node:assert/strict';
import { convertToPattern, countMaterials, replaceColor, paintCell } from '../src/utils/beads/engine.mjs';
const palette = [
  {code:'W',name:'White',hex:'#FFFFFF',rgb:[255,255,255]},
  {code:'K',name:'Black',hex:'#000000',rgb:[0,0,0]},
  {code:'R',name:'Red',hex:'#FF0000',rgb:[255,0,0]},
  {code:'G',name:'Grey',hex:'#BCBCBC',rgb:[188,188,188]},
];
function image(width,height,pixel) {
  const data = new Uint8ClampedArray(width*height*4);
  for(let y=0;y<height;y++) for(let x=0;x<width;x++) data.set(pixel(x,y),(y*width+x)*4);
  return {width,height,data};
}
const convert = (img,options={}) => convertToPattern(img,{columns:16,maxColors:4,palette,...options});
test('white and black stay beads while transparent pixels stay empty',()=>{
  const input=image(16,16,(x)=>x<5?[255,255,255,255]:x<10?[0,0,0,255]:[255,0,0,0]);
  const before=Array.from(input.data), p=convert(input);
  assert.equal(p.width,16); assert.equal(p.height,16); assert.ok(Array.isArray(p.cells));
  assert.equal(p.palette[p.cells[0]].code,'W'); assert.equal(p.palette[p.cells[5]].code,'K'); assert.equal(p.cells[10],-1);
  assert.deepEqual(Array.from(input.data),before);
});
test('columns bound width while both dimensions preserve aspect ratio within 128',()=>{
  assert.equal(convert(image(20,10,()=>[0,0,0,255]),{columns:32}).height,16);
  const tall=convert(image(1,20,()=>[0,0,0,255]),{columns:32});
  assert.equal(tall.width,6); assert.equal(tall.height,128);
  const portrait=convert(image(20,40,()=>[0,0,0,255]),{columns:128});
  assert.equal(portrait.width,64); assert.equal(portrait.height,128);
  assert.equal(convert(image(1,1000,()=>[0,0,0,255]),{columns:16}).width,1);
});
test('area sampling includes all covered pixels in linear light',()=>{
  const p=convert(image(32,16,x=>x%2?[255,255,255,255]:[0,0,0,255]));
  assert.ok(p.cells.every(i=>p.palette[i].code==='G'));
});
test('alpha-weighted sampling ignores hidden RGB and preserves empty areas',()=>{
  const p=convert(image(32,16,x=>x%2?[255,0,0,255]:[0,0,255,0]));
  assert.ok(p.cells.every(i=>p.palette[i].code==='R'));
  assert.ok(convert(image(16,16,()=>[255,255,255,0])).cells.every(i=>i===-1));
});
test('limits used colors and excludes unavailable bead codes',()=>{
  const img=image(16,16,x=>x<8?[255,255,255,255]:x<12?[0,0,0,255]:[255,0,0,255]);
  const limited=convert(img,{maxColors:2});
  assert.ok(new Set(limited.cells).size<=2); assert.ok(limited.palette.length<=2);
  const excluded=convert(img,{excludedCodes:['W','R']});
  assert.ok(excluded.cells.every(i=>!['W','R'].includes(excluded.palette[i].code)));
  assert.throws(()=>convert(img,{excludedCodes:palette.map(c=>c.code)}),/palette|color/i);
});
test('counts, replace and paint remain immutable and validate indices',()=>{
  const p={width:2,height:2,cells:[0,1,0,-1],palette:structuredClone(palette)};
  const before=structuredClone(p);
  assert.deepEqual(countMaterials(p).map(c=>[c.code,c.index,c.count]),[['W',0,2],['K',1,1]]);
  assert.deepEqual(replaceColor(p,0,1).cells,[1,1,1,-1]);
  assert.deepEqual(paintCell(p,1,1,2).cells,[0,1,0,2]);
  assert.deepEqual(paintCell(p,0,0,-1).cells,[-1,1,0,-1]);
  assert.throws(()=>paintCell(p,2,0,0),RangeError); assert.throws(()=>paintCell(p,.5,0,0),RangeError);
  assert.throws(()=>paintCell(p,0,0,99),RangeError); assert.throws(()=>replaceColor(p,0,99),RangeError);
  assert.deepEqual(p,before);
});
test('rejects malformed images and invalid conversion settings',()=>{
  assert.throws(()=>convert({width:2,height:2,data:[]}));
  for (const columns of [0,15,129,NaN,16.5]) assert.throws(()=>convert(image(1,1,()=>[0,0,0,255]),{columns}));
  for (const maxColors of [0,NaN,1.5]) assert.throws(()=>convert(image(1,1,()=>[0,0,0,255]),{maxColors}));
});
test('bundled palettes have provenance and normalized colors',async()=>{
  const {BEAD_PALETTES}=await import('../src/utils/beads/palettes.mjs');
  assert.ok(BEAD_PALETTES.some(p=>p.id==='mard-221')); assert.ok(BEAD_PALETTES.some(p=>p.id==='perler-midi'));
  for (const p of BEAD_PALETTES) {
    assert.ok(p.credit.includes('MakeBead')); assert.ok(p.colors.length>90);
    assert.equal(new Set(p.colors.map(c=>c.code)).size,p.colors.length);
    for(const c of p.colors) {assert.match(c.hex,/^#[A-Fa-f0-9]{6}$/); assert.equal(c.rgb.length,3);}
  }
});
test('maximum grid counts exactly and editing output does not alias source palette',()=>{
  const input=image(128,128,(x,y)=>[x*2,y*2,(x+y)%256,255]);
  const p=convert(input,{columns:128,maxColors:3});
  assert.equal(p.cells.length,16384);
  assert.equal(countMaterials(p).reduce((n,c)=>n+c.count,0),16384);
  assert.ok(p.palette.length<=3);
  const original=structuredClone(p);
  const edited=paintCell(p,0,0,-1);
  edited.palette[0].rgb[0]=123;
  assert.deepEqual(p,original);
  const allEmpty=convert(image(16,16,()=>[0,0,0,0]));
  assert.deepEqual(allEmpty.palette,[]); assert.deepEqual(countMaterials(allEmpty),[]);
});
