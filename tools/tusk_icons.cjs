const fs = require('fs');
const path = require('path');
const sharp = require('C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const ids = ['tusk','act_1','act_2','act_3','act_4','nail_shot','golden_nail','nail_shotgun','spin_glide','pursuing_hole','spiral_burst','wormhole_slip','wormhole_shot','dimensional_dodge','stand_barrage','golden_beatdown','infinite_rotation'];
const P = '#ee91c6', L = '#ffd6ee', D = '#572b56', G = '#f9ce62', W = '#fff8d7', B = '#93d4ee', N = '#18182f';
const shape = (d,fill=P,stroke=N,width=2) => `<path d="${d}" fill="${fill}" stroke="${stroke}" stroke-width="${width}" stroke-linejoin="round" stroke-linecap="round"/>`;
const line = (d,color=W,width=2) => shape(d,'none',color,width);
const ellipse = (x,y,rx,ry,fill,stroke=N,width=2) => `<ellipse cx="${x}" cy="${y}" rx="${rx}" ry="${ry}" fill="${fill}" stroke="${stroke}" stroke-width="${width}"/>`;
const group = (transform,art) => `<g transform="${transform}">${art}</g>`;
function star(x,y,r=4){return shape(`M${x} ${y-r}L${x+r*.3} ${y-r*.3}L${x+r} ${y}L${x+r*.3} ${y+r*.3}L${x} ${y+r}L${x-r*.3} ${y+r*.3}L${x-r} ${y}L${x-r*.3} ${y-r*.3}Z`,G,N,1);}
function portal(x,y,rx,ry){return ellipse(x,y,rx+2,ry+2,D,N,2)+ellipse(x,y,rx,ry,N,P,2)+ellipse(x,y,rx*.7,ry*.7,'#0c0d1b',B,1)+line(`M${x-rx*.7} ${y-ry*.6}Q${x} ${y-ry*1.2} ${x+rx*.6} ${y-ry*.65}`,L,1.5);}
function nail(x,y,angle=0,golden=true,scale=1){return group(`translate(${x} ${y}) rotate(${angle}) scale(${scale})`,shape('M-4 9L-4 -5Q0 -12 4 -5L4 9L0 15Z',golden?G:'#d9e8f2')+shape('M-1 -7L2 -5L2 8L0 12Z',golden?W:'#ffffff','none')+line('M-7 -2Q0 -8 7 -2M-7 4Q0 -2 7 4',golden?'#d98e32':B,1.5));}
function fist(x,y,scale=1,angle=0){return group(`translate(${x} ${y}) rotate(${angle}) scale(${scale})`,shape('M-10 11L-11 -5L-7 -9L-3 -8L1 -10L5 -8L9 -8L12 -4L11 8L7 12Z',P)+shape('M-10 1L-5 -2L-1 2L0 8L-7 10Z',D)+shape('M-8 -5L-6 -7L-3 -6L0 -8L3 -6L6 -6L9 -3L8 0L-6 0Z',L,'none')+line('M-3 -6L-3 -1M2 -6L2 -1M7 -5L7 -1',D,1)+shape('M-8 11L7 11L7 18L-7 18Z','#698ca3')+star(5,5,3));}
function body(x,y,scale=1,color=B){return group(`translate(${x} ${y}) scale(${scale})`,ellipse(0,-15,4.5,5,color)+shape('M-5 -8L5 -8L8 5L3 8L5 21L0 21L-2 10L-5 20L-10 19L-6 4Z',color)+line('M-5 -5L-12 3M5 -5L11 0',color,5));}
function portrait(act){
 let art='';
 if(act===1){art=shape('M15 19L5 10L7 5L23 12M41 12L57 5L59 10L49 19',P)+shape('M16 13L23 8L41 8L48 13L47 43L40 51L24 51L17 43Z',P)+shape('M17 23L7 32L17 36M47 23L57 32L47 36',L)+shape('M25 27L34 25L39 30L29 39Z',G)+ellipse(23,26,3,4,N)+ellipse(41,26,3,4,N)+star(32,16,5)+line('M24 43L28 46L36 46L40 43',D,2);}
 if(act===2){art=shape('M14 16L20 8L44 8L51 17L47 38L37 47L23 45L15 35', '#34455e')+portal(10,19,7,8)+portal(54,19,7,8)+shape('M17 15L26 10L42 13L46 21L43 42L22 42L16 30Z',P)+line('M24 29L24 39M30 29L30 40M36 29L36 40M42 29L42 37',D,3)+ellipse(23,25,1.5,2,N)+ellipse(39,25,1.5,2,N)+star(31,18,4)+shape('M26 44L38 44L38 51L26 51Z',B);}
 if(act===3){art=shape('M12 21L18 12L27 8L37 8L47 13L53 24L49 46L39 51L23 51L14 44Z',P)+shape('M30 10L35 10L37 47L31 47Z',D)+shape('M15 22L21 17L24 40L17 43Z',L,'none')+star(23,20,5)+star(43,20,5)+ellipse(21,41,3.5,3.5,'#9cae96')+ellipse(43,41,3.5,3.5,'#9cae96')+line('M18 30L21 30M43 30L46 30',N,2);}
 if(act===4){art=shape('M7 14L14 7L26 10L38 10L50 7L57 14L53 34L43 39L39 52L25 52L21 39L11 34Z',D)+shape('M17 13L28 8L36 8L47 13L44 34L38 41L26 41L20 34Z',P)+shape('M23 16L28 13L28 24L32 28L36 24L36 13L41 16L39 31L33 36L26 31Z',L)+shape('M29 14L35 14L35 23L32 26L29 23Z','#779899')+shape('M25 39L32 43L39 39L38 51L32 56L26 51Z',P)+line('M11 14L17 16L16 28M53 14L47 16L48 28',L,2)+star(15,17,3)+star(49,17,3)+star(32,34,2.5);}
 return art+group('translate(0 1)',shape('M21 52L43 52L43 62L21 62Z',N,'#a876ab',1)+`<text x="32" y="60" text-anchor="middle" fill="${W}" font-family="serif" font-weight="bold" font-size="9">${['','I','II','III','IV'][act]}</text>`);
}
function art(id){
 if(id==='tusk')return portrait(4)+star(8,45,4)+star(55,45,4);
 if(id.startsWith('act_'))return portrait(+id.slice(-1));
 switch(id){
 case 'nail_shot':case 'golden_nail':return line('M9 43L26 26M7 35L18 24',id==='golden_nail'?G:B,3)+shape('M8 57L7 45L15 36L22 38L28 31L32 33L27 42L30 48L23 58Z','#deb3a4')+shape('M15 48L22 41L25 43L20 51Z','#fff0db','none')+nail(42,20,222,id==='golden_nail',1.25)+star(54,8,3);
 case 'nail_shotgun':return [ [-38,14,25],[-19,22,18],[0,32,15],[19,42,18],[38,50,25] ].map(([a,x,y])=>nail(x,y,180+a,false,.65)).join('')+shape('M17 59L19 46L24 41L40 41L46 47L47 59Z','#d1a392')+line('M23 52L23 45M29 50L29 43M35 50L35 43M41 52L41 46','#fff0db',3)+line('M25 35L23 30M32 33L32 28M39 35L41 30',G,2);
 case 'spin_glide':return line('M3 30L22 30M4 39L17 39M6 49L23 49',B,3)+group('rotate(18 34 28)',body(35,24,.9,P))+ellipse(35,53,17,5,'none',G,3)+line('M22 56L14 55M47 49L55 51',W,2);
 case 'pursuing_hole':return shape('M3 53L24 30L61 38L38 61Z','#3f465b')+line('M9 52Q23 48 25 40Q25 35 39 35',G,3)+portal(22,47,10,5)+body(48,20,.55,'#bdd3e5')+line('M36 36L43 33L40 40',W,2);
 case 'spiral_burst':return portal(12,49,9,5)+portal(51,49,9,5)+portal(32,55,10,5)+shape('M11 31L24 32L19 12L30 25L34 4L39 26L55 15L47 33L61 35L45 42L47 51L32 45L20 51L21 40Z',G)+shape('M25 32L31 33L34 19L38 34L45 35L37 39L32 44L29 38L23 37Z',W,'none');
 case 'wormhole_slip':return line('M15 18Q34 4 50 17',G,2)+line('M46 12L51 17L45 19',G,2)+body(15,30,.6,P)+portal(16,47,13,7)+body(49,26,.6,B)+portal(49,43,12,6)+shape('M3 49L29 49L26 60L6 60Z',N,'none');
 case 'wormhole_shot':return portal(18,32,11,23)+nail(32,30,-90,true,1.1)+line('M38 30L49 30M43 25L50 25M43 35L50 35',G,2)+body(55,31,.55,'#bed1e8')+star(49,28,4);
 case 'dimensional_dodge':return `<g opacity=".45">${body(16,31,.85,B)}</g>`+portal(42,32,12,25)+group('rotate(18 43 29)',body(43,30,.85,P))+line('M6 29L25 29',G,4)+shape('M23 24L31 29L23 34Z',W)+line('M23 45L31 43M24 51L31 49',B,2);
 case 'stand_barrage':return line('M3 14L14 10M2 31L12 28M4 51L15 47',G,3)+fist(42,15,.7,25)+fist(23,29,.8,25)+fist(44,44,.85,25)+star(60,30,3);
 case 'golden_beatdown':return body(37,42,.7,'#9aadc4')+shape('M9 46L23 40L20 27L32 36L47 29L43 41L60 45L44 50L47 59L32 53L21 60L22 50Z',G)+fist(29,18,1.2,-18)+line('M8 6L5 18M53 6L57 20',W,3);
 case 'infinite_rotation':{let spiral='';for(let i=0;i<=100;i++){let a=i*.19,r=2+i*.245;spiral+=(i?'L':'M')+(32+Math.cos(a)*r).toFixed(2)+' '+(32+Math.sin(a)*r).toFixed(2);}return line(spiral,D,7)+line(spiral,G,4)+line(spiral,W,1)+nail(34,32,40,true,1.3)+star(10,11,4)+star(54,53,4);}
 default:throw new Error('Unknown Tusk icon '+id);
 }
}
async function icon(id){let svg=`<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">${art(id)}</svg>`;return sharp(Buffer.from(svg)).resize(64,64).png().toBuffer();}
async function build(root){for(const id of ids){const dest=path.join(root,'src/main/resources/assets/kazimod/textures/abilities/tusk',id+'.png');fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,await icon(id));}}
module.exports={icon,build,ids};
if(require.main===module)build(process.argv[2]||path.resolve(__dirname,'..')).catch(e=>{console.error(e);process.exit(1);});
