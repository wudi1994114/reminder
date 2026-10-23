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

test('illustration sampling keeps flat colors and supported dark strokes instead of invented grey',()=>{
  const flat=convert(image(64,64,x=>x%4<3?[255,0,0,255]:[255,255,255,255]),{mode:'illustration'});
  assert.ok(flat.cells.every(i=>flat.palette[i].code==='R'));
  const line=convert(image(64,64,x=>x%4===1?[0,0,0,255]:[255,255,255,255]),{mode:'illustration'});
  assert.ok(line.cells.every(i=>line.palette[i].code==='K'));
  const photo=convert(image(32,16,x=>x%2?[255,255,255,255]:[0,0,0,255]),{mode:'photo'});
  assert.ok(photo.cells.every(i=>photo.palette[i].code==='G'));
});
test('light cleanup removes low-contrast singleton noise but retains eyes and transparency',()=>{
  const colors=[...palette,{code:'N',name:'Near white',hex:'#E6E6E6',rgb:[230,230,230]}];
  const input=image(16,16,(x,y)=>x===8&&y===8?[230,230,230,255]:x===3&&y===3?[0,0,0,255]:x===0&&y===0?[0,0,255,0]:[255,255,255,255]);
  const raw=convert(input,{palette:colors,cleanup:0}), clean=convert(input,{palette:colors,cleanup:1});
  assert.equal(raw.palette[raw.cells[8*16+8]].code,'N');
  assert.equal(clean.palette[clean.cells[8*16+8]].code,'W');
  assert.equal(clean.palette[clean.cells[3*16+3]].code,'K');
  assert.equal(clean.cells[0],-1);
  assert.equal(countMaterials(clean).reduce((s,c)=>s+c.count,0),255);
});
test('limited illustration palettes retain rare actual high-contrast neutral details',()=>{
  const colors=[...palette,{code:'Y',hex:'#FFFF00',rgb:[255,255,0]}];
  const p=convert(image(16,16,(x,y)=>x===5&&y===5?[0,0,0,255]:x<8?[255,255,0,255]:[255,255,255,255]),{palette:colors,maxColors:3,mode:'illustration'});
  assert.equal(p.palette[p.cells[5*16+5]].code,'K');
  assert.deepEqual(p,convert(image(16,16,(x,y)=>x===5&&y===5?[0,0,0,255]:x<8?[255,255,0,255]:[255,255,255,255]),{palette:colors,maxColors:3,mode:'illustration'}));
});
test('new processing options reject invalid modes and strengths',()=>{
  const input=image(16,16,()=>[255,255,255,255]);
  assert.throws(()=>convert(input,{mode:'unknown'}),/mode/i);
  for(const cleanup of [-1,3,0.5,NaN]) assert.throws(()=>convert(input,{cleanup}),/cleanup/i);
});
test('a large muted background cannot consume the palette budget of vivid subject accents',()=>{
  const neutrals=Array.from({length:10},(_,i)=>({code:`N${i}`,hex:'#000000',rgb:[50+i*15,48+i*15,45+i*15]}));
  const colors=[...neutrals,...palette.filter(c=>['R'].includes(c.code)),{code:'B',rgb:[20,100,200]},{code:'Y',rgb:[210,180,20]}];
  const input=image(32,32,(x,y)=>x===31&&y<3?[...colors[10+y].rgb,255]:[...neutrals[Math.floor(y/32*10)].rgb,255]);
  for (const mode of ['photo','illustration']) {
    const p=convert(input,{columns:32,palette:colors,maxColors:5,mode});
    for (const code of ['R','B','Y']) assert.ok(p.palette.some(c=>c.code===code),`${mode} lost ${code}`);
  }
});
test('cleanup protects source edges even when the remaining bead colors are similar',()=>{
  const colors=[{code:'D',rgb:[120,120,120]},{code:'L',rgb:[140,140,140]}];
  const input=image(16,16,(x,y)=>x===8&&y===8?[240,0,0,255]:[0,160,160,255]);
  const raw=convert(input,{palette:colors,cleanup:0});
  assert.notEqual(raw.cells[8*16+8],raw.cells[0]);
  for (const cleanup of [1,2]) assert.deepEqual(convert(input,{palette:colors,cleanup}).cells,raw.cells);
});
