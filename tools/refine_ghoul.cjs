const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require('C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=process.argv[2]||path.resolve(__dirname,'..'),names=['ukaku','koukaku','rinkaku','bikaku'];
const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,d);};
const uid=s=>{let h=crypto.createHash('md5').update('ghoul-v2/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const add=(a,b)=>a.map((x,i)=>x+b[i]),sub=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,s)=>a.map(x=>x*s),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],norm=a=>mul(a,1/(Math.hypot(...a)||1));
const smooth=t=>(t=Math.max(0,Math.min(1,t)))*t*(3-2*t),pulse=(t,s,w,r)=>t<=s?smooth((t-s+w)/w):1-smooth((t-s)/r);
function rest(type,l,t){if(type===3)return[Math.sin(t*4.3)*13,12-9*Math.sin(t*2.7)+t*t*9,3+43*t];let s=l%2?1:-1,u=l<2?1:-1;return[s*(2+t*26+Math.sin(t*Math.PI)*5),14+u*Math.sin(t*Math.PI*.85)*15+t*5,3+22*t+Math.sin(t*Math.PI)*8];}
function make(type){let id=names[type],elements=[],groups=[],outliner=[],animations=[],deform=[],serial=0;
 const m={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Ghoul '+id+' refined',model_identifier:'ghoul_'+id,resolution:{width:256,height:256},elements,groups,outliner,textures:[],animations};
 const group=(name,origin,parent)=>{let g={name,uuid:uid(id+name),origin,rotation:[0,0,0],visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const rootBone=group('Root',[0,14,3]);
 function tube(name,points,radii,depth,parent,limb=-1){let e={type:'mesh',uuid:uid(id+'mesh'+serial++),name,origin:[0,0,0],rotation:[0,0,0],visibility:true,export:true,vertices:{},faces:{}},params={};
  for(let j=0;j<points.length;j++){let t=j/(points.length-1),tangent=norm(sub(points[Math.min(j+1,points.length-1)],points[Math.max(0,j-1)])),u=norm(cross(tangent,Math.abs(tangent[1])>.95?[1,0,0]:[0,1,0])),v=norm(cross(tangent,u));
   for(let k=0;k<8;k++){let a=k*Math.PI/4,offset=add(mul(u,Math.cos(a)*radii[j]),mul(v,Math.sin(a)*radii[j]*depth)),key=`r${j}_${k}`;e.vertices[key]=add(points[j],offset);params[key]=[t,Math.cos(a)*radii[j],Math.sin(a)*radii[j]*depth];}
  }
  for(let j=0;j<points.length-1;j++)for(let k=0;k<8;k++){let keys=[`r${j}_${k}`,`r${j}_${(k+1)%8}`,`r${j+1}_${(k+1)%8}`,`r${j+1}_${k}`],uv=[[1+k*31.5,1+j/(points.length-1)*254],[1+(k+1)*31.5,1+j/(points.length-1)*254],[1+(k+1)*31.5,1+(j+1)/(points.length-1)*254],[1+k*31.5,1+(j+1)/(points.length-1)*254]];e.faces['f'+j+'_'+k]={vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,uv[i]])),texture:0};}
  for(let end of [0,points.length-1])for(let k=1;k<7;k++){let keys=[`r${end}_0`,`r${end}_${k}`,`r${end}_${k+1}`];if(end===0)keys.reverse();e.faces['cap'+end+'_'+k]={vertices:keys,uv:Object.fromEntries(keys.map(k=>[k,[128,128]])),texture:0};}
  elements.push(e);parent.children.push(e.uuid);if(limb>=0)deform.push({name,limb,params});return e;
 }
 if(type===0)for(let side of [-1,1]){
  let size=side<0?1:.76,wing=group('Wing '+side,[side*3,22,3],rootBone);
  tube('Living wing stem '+side,[[side*3,22,3],[side*8,26,5],[side*17,29,7],[side*27,31,8]].map((p,i)=>[p[0]*size,p[1],p[2]]),[2.8,4,3,.1],.55,wing);
  for(let i=0;i<11;i++){let a=[side*(5+i*.8)*size,22+i*.36,4+i*.4],end=[side*(22+12*Math.sin(i*.25))*size,19+(10-i)*2.8,8+i*.9],mid=add(mul(a,.42),mul(end,.58)),g=group('Feather '+side+' '+i,a,wing);tube('Faceted feather '+side+' '+i,[a,add(mul(a,.75),mul(end,.25)),mid,add(mul(a,.12),mul(end,.88)),end],[1.2,3.1,2.7,1.1,.035],.22,g);}
  for(let i=0;i<4;i++)tube('Lower hooked feather '+side+' '+i,[[side*5,22,4],[side*(12+i*3)*size,20-i*2,7],[side*(18+i*3)*size,14-i*2,11]],[1.6,2.3,.035],.25,wing);
 }
 if(type===1){let arm=group('Armor',[-5,22,0],rootBone),blade=group('Blade',[-6,14,0],arm);
  tube('Shoulder carapace',[[-5,23,0],[-8,22,0],[-9,19,1]],[2.6,4.5,3],.8,arm);
  let pts=[];for(let i=0;i<=20;i++){let t=i/20,a=t*Math.PI*3.6;pts.push([-5+Math.cos(a)*3.9,21-t*10,Math.sin(a)*3.8]);}tube('Continuous spiral armor',pts,pts.map((_,i)=>1.9-i*.035),.75,arm);
  tube('Crescent blade',[[-6,13,0],[-10,11,-3],[-14,13,-8],[-15,19,-12],[-12,26,-14],[-8,31,-12]],[2.2,4.6,5.2,4.1,2.3,.03],.23,blade);
  tube('Blade dorsal spur',[[-9,16,-1],[-13,20,2],[-12,25,3]],[2.3,2,.03],.35,blade);
  let shield=group('Shield',[-5,22,0],rootBone);
  for(let i=-2;i<=2;i++){let x=i*4.4; tube('Shield shell '+i,[[x,6,-11],[x*1.1,13,-14],[x,22,-14],[x*.6,28,-11]],[.4,3.7,3.8,.3],.35,shield);}
 }
 if(type>=2)for(let limb=0;limb<(type===2?4:1);limb++){
  let g=group('Tendril '+limb,rest(type,limb,0),rootBone),pts=Array.from({length:25},(_,j)=>rest(type,limb,j/24)),radii=pts.map((_,j)=>(type===2?2.4:3.1)*Math.pow(1-j/24,.68)+.04);
  tube('Organic limb '+limb,pts,radii,type===2?.88:.82,g,limb);
 }
 const clip=(name,length,loop,fn)=>{let animators={};for(let g of groups){let keys=[];for(let k=0;k<=24;k++){let v=fn(g.name,k/24);if(!v)continue;keys.push({uuid:uid(id+name+g.name+k),channel:'rotation',time:length*k/24,interpolation:'linear',data_points:[{x:''+v[0],y:''+v[1],z:''+v[2]}]});}if(keys.length)animators[g.uuid]={name:g.name,type:'bone',keyframes:keys};}animations.push({uuid:uid(id+name),name:'animation.ghoul.'+name,length,loop:loop?'loop':'once',animators});};
 clip('idle',3,true,(n,t)=>n.startsWith('Wing')?[Math.sin(t*Math.PI*2)*2,Math.sin(t*Math.PI*2)*2,0]:n==='Armor'?[0,0,0]:n.startsWith('Feather')?[Math.sin(t*Math.PI*2+Number(n.split(' ').at(-1))*.3)*2,0,0]:[0,0,0]);
 clip('run',.8,true,(n,t)=>n.startsWith('Wing')?[-10+Math.sin(t*Math.PI*2)*3,0,n.includes('-1')?-8:8]:[0,0,0]);
 for(let a=1;a<=4;a++)clip('attack'+a,1,false,(n,t)=>{
  let f=pulse(t,a===4?.73:a===2?.62:.55,.45,.3),s=n.includes('-1')?-1:1;
  if(n.startsWith('Wing'))return [a===2?-55*f:12*f,s*(a===3?-24:16)*f,s*(a===4?25:-9)*f];
  if(n.startsWith('Feather'))return [f*(a===3?-12:8),s*f*5,0];
  if(n==='Armor')return a===1?[60*f,18*f,-25*f]:a===2?[80*f,-85*f,20*f]:a===3?[75*f,15*f,-25*f]:[-135*f,10*f,-10*f];
  if(n==='Blade')return a===4?[110*pulse(t,.73,.08,.23),0,0]:[0,0,0];return[0,0,0];
 });
 clip('guard',1,true,n=>n==='Armor'?[60,18,-25]:[0,0,0]);
 function bakeNodes(nodes){return nodes.map(n=>{if(typeof n!=='string')return{name:n.name,origin:n.origin,rotation:n.rotation,visible:true,children:bakeNodes(n.children)};let e=elements.find(e=>e.uuid===n),quads=[];for(let f of Object.values(e.faces)){let v=f.vertices.map(k=>[...e.vertices[k],f.uv[k][0]/256,f.uv[k][1]/256]),normal=norm(cross(sub(v[1],v[0]),sub(v[2],v[0])));if(v.length===3)v.push(v[2]);quads.push({vertices:v,normal});}return{name:e.name,visible:true,quads};});}
 let nodes=bakeNodes(outliner),tubes=deform.map(d=>{let e=elements.find(e=>e.name===d.name);return{limb:d.limb,quads:Object.values(e.faces).map(f=>{let v=f.vertices.map(k=>[...d.params[k],f.uv[k][0]/256,f.uv[k][1]/256]);if(v.length===3)v.push(v[2]);return v;})};});
 let clips=animations.map(a=>({name:a.name.split('.').at(-1),length:a.length,loop:a.loop==='loop',tracks:Object.values(a.animators).map(v=>({bone:v.name,keys:v.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(a=>Number(k.data_points[0][a]))}))}))}));
 return{m,rig:{nodes,animations:clips,tubes}};
}
async function icons(){
 const shapes=[
 '<path d="M7 29Q32 4 57 29Q32 52 7 29Z"/><circle cx="32" cy="29" r="9" fill="#ff476c"/>',
 '<path d="M9 45L25 12L22 39ZM26 49L43 9L41 38ZM43 48L56 22L54 43Z"/>',
 '<path d="M5 33L38 20L32 7L59 29L28 55L35 39Z"/>',
 '<path d="M8 47L28 28L51 6L57 13L34 39Z"/><path d="M11 12L17 24M30 4L31 15M49 39L60 41" fill="none"/>',
 '<path d="M9 6L18 34L24 22L20 49L9 38L14 34ZM28 6L36 35L42 23L39 55L28 43L32 35ZM46 9L53 31L58 22L56 48L46 38L49 31Z"/>',
 '<path d="M12 10L32 4L52 10L49 36L32 58L15 36Z"/><path d="M32 11L32 47M21 17L43 17" fill="none" stroke="#efb2ff"/>',
 '<path d="M10 48Q25 12 55 9Q37 28 39 47L30 54L23 40Z"/>',
 '<path d="M25 10L46 15L43 39L29 51L20 35Z"/><path d="M4 19L20 19M2 29L17 29M5 39L18 39" fill="none"/>',
 '<path d="M27 4L39 4L37 35L49 28L32 50L16 28L27 35Z"/><path d="M5 56L20 48L31 58L42 48L59 56" fill="none"/>',
 '<path d="M5 55Q15 22 30 8L24 32ZM19 58Q29 29 44 13L38 39ZM36 59Q43 32 58 20L53 45Z"/>',
 '<path d="M8 55Q5 15 32 11Q58 9 53 31Q47 43 35 35Q26 29 35 23" fill="none" stroke-width="7"/>',
 '<path d="M8 55Q4 30 27 29L22 36L32 10L47 36L38 29Q59 35 56 55" fill="none" stroke-width="6"/>',
 '<path d="M49 11Q13 0 9 31Q6 57 35 55Q57 52 54 30Q51 16 33 21Q20 25 29 37" fill="none" stroke-width="7"/>',
 '<path d="M5 49Q25 41 30 29L52 6L57 11L36 37Q21 52 5 55Z"/>',
 '<path d="M5 18Q8 55 42 48L38 39L59 44L46 59L47 51Q7 65 5 18Z"/>',
 '<path d="M14 48Q1 22 23 9Q49 -1 55 26Q58 44 41 47L44 34L30 46L44 59L42 50" fill="none" stroke-width="6"/>',
 '<path d="M6 14L23 20L12 29L28 35L17 46L53 27L39 23L50 10L30 19Z"/>'
 ];
 for(let i=0;i<17;i++){let type=i===0?0:Math.floor((i-1)/4),c=['#ff536e','#cc7ae6','#ef334d','#49d8db'][type],svg=`<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect x="1" y="1" width="62" height="62" rx="6" fill="#14151e" stroke="${c}" stroke-width="2"/><g fill="${c}" stroke="${c}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${shapes[i]}</g></svg>`;write('src/main/resources/assets/kazimod/textures/abilities/ghoul/'+i+'.png',await sharp(Buffer.from(svg)).png().toBuffer());}
}
(async()=>{let source=path.join(__dirname,'assets/ghoul/material-atlas.png'),meta=await sharp(source).metadata(),w=Math.floor(meta.width/2),h=Math.floor(meta.height/2);for(let type=0;type<4;type++){
 let small=await sharp(source).extract({left:(type%2)*w,top:Math.floor(type/2)*h,width:w,height:h}).resize(64,64,{kernel:'lanczos3'}).modulate({brightness:1.18}).png().toBuffer();
 let png=await sharp(small).resize(256,256,{kernel:'nearest'}).png().toBuffer(),{m,rig}=make(type);
 m.textures=[{uuid:uid(names[type]+'texture'),name:names[type]+'.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 rig.sourceHash=crypto.createHash('sha256').update(JSON.stringify(m)).digest('hex');write('deliverables/ghoul/'+names[type]+'.bbmodel',JSON.stringify(m));write('src/main/resources/assets/kazimod/models/entities/ghoul_'+names[type]+'.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/ghoul_'+names[type]+'.png',png);console.log(names[type],m.elements.length,'mesh parts',m.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0),'faces');
 }await icons();})().catch(e=>{console.error(e);process.exit(1);});
