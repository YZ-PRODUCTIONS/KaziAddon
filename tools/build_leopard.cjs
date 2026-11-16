const fs = require('fs'), path = require('path'), crypto = require('crypto'), assert = require('assert');
const sharp = require(process.env.SHARP_MODULE || 'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const { clipsFrom } = require(process.env.LEOPARD_BAKE_MODULE || './build_shikigami.cjs');
const root = process.argv[2] || path.resolve(__dirname, '..');
const write = (p, data) => { p = path.join(root, p); fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, data); };
const uid = s => { const h = crypto.createHash('md5').update('leopard/' + s).digest('hex'); return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`; };
const rgb = h => h.match(/../g).map(v => parseInt(v, 16));
function canvas(size) {
  const data = Buffer.alloc(size * size * 4);
  const dot = (x,y,c) => { x=Math.round(x);y=Math.round(y);if(x<0||x>=size||y<0||y>=size)return;const k=(y*size+x)*4;const col=Array.isArray(c)?c:rgb(c);for(let a=0;a<3;a++)data[k+a]=Math.max(0,Math.min(255,col[a]));data[k+3]=255; };
  const rect = (x,y,w,h,c) => {for(let Y=y;Y<y+h;Y++)for(let X=x;X<x+w;X++)dot(X,Y,c);};
  const line = (x,y,X,Y,w,c) => {const n=Math.max(Math.abs(X-x),Math.abs(Y-y),1);for(let i=0;i<=n;i++)rect(Math.round(x+(X-x)*i/n),Math.round(y+(Y-y)*i/n),w,w,c);};
  const ellipse = (x,y,rx,ry,c) => {for(let Y=Math.floor(y-ry);Y<=y+ry;Y++)for(let X=Math.floor(x-rx);X<=x+rx;X++)if(((X-x)/rx)**2+((Y-y)/ry)**2<=1)dot(X,Y,c);};
  const poly = (pts,c) => {for(let y=Math.floor(Math.min(...pts.map(p=>p[1])));y<=Math.max(...pts.map(p=>p[1]));y++)for(let x=Math.floor(Math.min(...pts.map(p=>p[0])));x<=Math.max(...pts.map(p=>p[0]));x++){let on=false;for(let i=0,j=pts.length-1;i<pts.length;j=i++){const a=pts[i],b=pts[j];if((a[1]>y)!==(b[1]>y)&&x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])on=!on;}if(on)dot(x,y,c);}};
  return {data,dot,rect,line,ellipse,poly};
}
async function texture(kind) {
  const c=canvas(256), awake=kind==='awakened', gold=awake?'eabb42':'d3a34a';
  const colors=[gold,'f1e9cf',gold,'eee3c6','33303a','b88b54','251a20','181725','f1e9cf',gold,'d1a257','292332','39303a','f5e7cb','69504c','be8b36'];
  for(let tile=0;tile<16;tile++){
    const ox=tile%4*64,oy=(tile>>2)*64,base=rgb(colors[tile]);
    for(let y=0;y<64;y++)for(let x=0;x<64;x++){
      const cluster=Math.sin(Math.floor(x/3)*2.35+Math.floor(y/4)*1.47),edge=Math.abs(x-31.5)/32;
      const shade=Math.round((5-12*edge+5*Math.cos(y/17)+cluster*2)/3)*3;
      c.dot(ox+x,oy+y,base.map(v=>v+shade));
    }
    if([0,9,10,15].includes(tile)){
      // Broken rosettes, rather than solid dots, are the defining leopard marking.
      for(let row=0;row<7;row++)for(let col=0;col<7;col++){
        const x=ox+4+col*9+(row%2)*3,y=oy+5+row*9+Math.sin(col*2+row)*1.2;
        if(x+4>=ox+64||y+4>=oy+64)continue;
        const rx=2.15+((row+col)%3)*.25,ry=1.9+((row*2+col)%3)*.35;
        c.ellipse(x,y,rx+.7,ry+.7,'57402b');c.ellipse(x,y,rx,ry,'302a24');c.ellipse(x,y,rx-1,ry-1,'b7873e');
        const a=(row*2.1+col*1.9);c.rect(x+Math.cos(a)*rx-1,y+Math.sin(a)*ry-1,2,2,gold);
        if((row+col)%3===0)c.dot(x-rx-1,y+ry,'302a24');
      }
    }
    if([1,3,8].includes(tile)){
      for(let y=8;y<61;y+=8)for(let x=5+(y%3);x<61;x+=9)c.line(ox+x,oy+y,ox+x-1,oy+y+2,1,'d2c8aa');
    }
    if([7,11].includes(tile)){
      for(let k=0;k<3;k++){
        const cx=ox+12+k*18,cy=oy+13+(k%2)*27;
        let prev=null;for(let t=0;t<30;t++){const a=t*.26,r=8*(1-t/33),p=[cx+Math.cos(a)*r,cy+Math.sin(a)*r];if(prev)c.line(...prev,...p,1,tile===7?'41384d':'51415c');prev=p;}
      }
    }
  }
  // One complete face chart is projected across the beveled front of the skull.
  const ox=128,oy=0,point=p=>[ox+p[0],oy+p[1]];
  c.poly([[17,4],[23,8],[26,23],[22,30],[19,23],[21,15]].map(point),'312b24');
  c.poly([[46,4],[40,8],[37,23],[41,30],[44,23],[42,15]].map(point),'312b24');
  c.poly([[29,0],[34,0],[33,10],[35,17],[32,23],[29,17],[31,10]].map(point),'3e3125');
  for(const side of[-1,1]){
    const mirror=pts=>pts.map(([x,y])=>point([side<0?x:63-x,y]));
    c.poly(mirror([[3,25],[11,22],[27,29],[26,35],[18,39],[7,35]]),'2a2825');
    c.poly(mirror([[6,28],[11,27],[25,30],[22,35],[15,36],[8,33]]),'eee3b7');
    c.ellipse(ox+(side<0?17:46),31,3,4,awake?'d84d39':'b1af5a');
    c.rect(ox+(side<0?17:46),28,1,7,'191b1b');
    c.dot(ox+(side<0?16:45),29,'fff6d1');
    c.poly(mirror([[6,37],[10,39],[15,40],[16,43],[12,46],[6,44]]),'393028');
    c.poly(mirror([[3,13],[9,11],[13,15],[9,18],[4,17]]),'443427');
    c.poly(mirror([[2,47],[8,47],[13,51],[10,53],[4,52]]),'322d25');
    c.poly(mirror([[21,37],[26,36],[29,48],[26,49],[24,43]]),'9e7533');
  }
  c.poly([[25,50],[38,50],[34,57],[29,57]].map(point),'3b2a2a');
  // Muzzle whisker roots, dark mouth seam, and an ivory chin.
  for(let s of[-1,1])for(let k=0;k<5;k++)c.ellipse(192+32+s*(8+(k%3)*6),20+Math.floor(k/3)*12,1.1,1.1,'493c31');
  c.line(192+31,29,192+31,43,2,'5d4535');c.line(192+31,43,192+9,49,1,'5d4535');c.line(192+32,43,192+54,49,1,'5d4535');
  c.poly([[66,66],[124,66],[119,104],[96,122],[73,104]],'dfb4a4');
  c.poly([[73,74],[117,74],[111,98],[96,111],[79,98]],'80655a');
  // Chest muscles are shading on continuous fur, not floating abdominal cubes.
  for(let s of[-1,1]){
    const X=32+s*15,Y=128;c.line(X-11,Y+14,X+10,Y+18,2,'b2aa90');
    for(let i=0;i<3;i++)c.line(32+s*3,Y+29+i*9,32+s*15,Y+32+i*9,1,'c1b99d');
  }
  if(awake){for(let s of[-1,1])for(let i=0;i<5;i++){const X=s<0?2:59;c.poly([[X,131+i*11],[X+s*5,135+i*11],[X-s*7,142+i*11],[X,140+i*11]],'34302d');}}
  return sharp(c.data,{raw:{width:256,height:256,channels:4}}).png().toBuffer();
}
const {create} = require('./leopard_cuboid_models.cjs');
const {bake} = require(process.env.LEOPARD_BAKE_MODULE || './build_shikigami.cjs');
async function main(){
  for(const kind of['full','hybrid','awakened']){
    const id='leopard_'+kind,saved=path.join(root,'deliverables/leopard',id+'.bbmodel');
    // Existing edited Blockbench sources stay authoritative unless recreation is explicitly requested.
    const model=fs.existsSync(saved)&&!process.argv.includes('--recreate')?JSON.parse(fs.readFileSync(saved)):create(kind);
    assert(model.elements.every(e=>e.type==='cube'), 'Leopard models must contain only Blockbench cuboids');
    const authored=path.join(root,'src/main/resources/assets/kazimod/textures/zoan/'+id+'.png');
    const png=model.textures.length?Buffer.from(model.textures[0].source.split(',')[1],'base64'):fs.existsSync(authored)?fs.readFileSync(authored):await texture(kind);
    model.textures=[{uuid:uid(id+'/texture'),id:'0',name:id+'.png',width:256,height:256,source:'data:image/png;base64,'+png.toString('base64'),internal:true}];
    const source=JSON.stringify(model);write('deliverables/leopard/'+id+'.bbmodel',source);
    write('src/main/resources/assets/kazimod/textures/zoan/'+id+'.png',png);
    write('src/main/resources/assets/kazimod/models/zoan/'+id+'.json',JSON.stringify({sourceHash:crypto.createHash('sha256').update(source).digest('hex'),nodes:bake(model),animations:clipsFrom(model)}));
    console.log(id,model.elements.length+' parts',model.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0)+' faces',model.animations.length+' clips');
  }
  const c=canvas(64);c.ellipse(32,32,29,29,'171520');c.ellipse(32,32,26,26,'413549');
  c.poly([[16,16],[23,20],[32,15],[41,20],[48,16],[46,39],[38,49],[26,49],[18,39]],'deb14f');
  c.poly([[19,28],[28,31],[25,35],[20,33]],'f5e8bc');c.poly([[45,28],[36,31],[39,35],[44,33]],'f5e8bc');
  c.rect(24,30,2,4,'cc4438');c.rect(38,30,2,4,'cc4438');c.poly([[27,37],[37,37],[32,42]],'2b252b');c.line(25,43,39,43,2,'eee3c6');
  for(const s of[-1,1]){c.line(32+s*18,42,32+s*25,27,4,'191723');c.line(32+s*25,27,32+s*20,11,4,'191723');c.line(32+s*20,11,32+s*10,8,4,'191723');c.line(32+s*10,8,32+s*7,13,3,'191723');}
  c.line(31,18,31,26,2,'3d3028');c.line(23,21,26,26,2,'3d3028');c.line(40,21,37,26,2,'3d3028');
  write('src/main/resources/assets/kazimod/textures/abilities/leopard/awakened_leopard_form.png',await sharp(c.data,{raw:{width:64,height:64,channels:4}}).png().toBuffer());
}
module.exports={create,bake,texture};
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});
