// Editable low-poly Blockbench model, baked geometry, texture atlas, and animation clips.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bake,clipsFrom}=require(process.env.JEROME_BAKE_MODULE||'./build_shikigami.cjs');
const root=process.argv[2]||path.resolve(__dirname,'..');
const write=(p,data)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);};
const uid=s=>{const h=crypto.createHash('md5').update('jerome/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
async function texture(){
 const size=256,data=Buffer.alloc(size*size*4),rgb=c=>c.match(/../g).map(x=>parseInt(x,16));
 const dot=(x,y,c)=>{x=Math.round(x);y=Math.round(y);if(x<0||x>=size||y<0||y>=size)return;const i=(y*size+x)*4,v=Array.isArray(c)?c:rgb(c);for(let a=0;a<3;a++)data[i+a]=Math.max(0,Math.min(255,v[a]));data[i+3]=255;};
 const poly=(m,p,c)=>{const ox=m%4*64,oy=(m>>2)*64;for(let y=Math.floor(Math.min(...p.map(v=>v[1])));y<=Math.max(...p.map(v=>v[1]));y++)for(let x=Math.floor(Math.min(...p.map(v=>v[0])));x<=Math.max(...p.map(v=>v[0]));x++){let inside=false;for(let i=0,j=p.length-1;i<p.length;j=i++)if((p[i][1]>y)!==(p[j][1]>y)&&x<(p[j][0]-p[i][0])*(y-p[i][1])/(p[j][1]-p[i][1])+p[i][0])inside=!inside;if(inside)dot(ox+x,oy+y,c);}};
 const line=(m,x,y,X,Y,w,c)=>{const n=Math.max(Math.abs(x-X),Math.abs(y-Y),1);for(let i=0;i<=n;i++)for(let a=0;a<w;a++)for(let b=0;b<w;b++)dot(m%4*64+x+(X-x)*i/n+a,(m>>2)*64+y+(Y-y)*i/n+b,c);};
 const ellipse=(m,x,y,rx,ry,c)=>{for(let Y=Math.floor(y-ry);Y<=y+ry;Y++)for(let X=Math.floor(x-rx);X<=x+rx;X++)if(((X-x)/rx)**2+((Y-y)/ry)**2<=1)dot(m%4*64+X,(m>>2)*64+Y,c);};
 for(let m=0;m<16;m++){const base=rgb(m===6?'30384a':'6f5140');for(let y=0;y<64;y++)for(let x=0;x<64;x++){const shade=Math.round((6*Math.cos((x-24)/26)-y*.1+2*Math.sin(Math.floor(x/4)*2+Math.floor(y/4)*3))/2)*2;dot(m%4*64+x,(m>>2)*64+y,base.map(v=>v+shade));}}
 // Face, including the broad eyebrows and round eyes visible in the references.
 poly(0,[[0,0],[14,0],[11,17],[14,40],[23,57],[30,64],[0,64]],'44352e');
 poly(0,[[53,0],[64,0],[64,64],[40,64],[49,48],[54,29]],'3e3029');
 ellipse(0,25,10,10,5,'8c6c54');ellipse(0,19,8,4,2,'a98c70');
 poly(0,[[10,24],[13,20],[25,21],[27,27],[21,25]],'292724');
 poly(0,[[37,27],[38,21],[50,20],[54,25],[46,25]],'292724');
 ellipse(0,20,33,7,8,'352b27');ellipse(0,44,33,7,8,'352b27');
 ellipse(0,20,32,5.6,6.4,'e6e8df');ellipse(0,44,32,5.6,6.4,'e6e8df');
 ellipse(0,21,32,1.9,2.6,'272929');ellipse(0,43,32,1.9,2.6,'272929');
 dot(21,31,'ffffff');dot(43,31,'ffffff');
 poly(0,[[29,32],[26,43],[29,46],[37,46],[40,43],[35,36]],'49372e');
 poly(0,[[30,34],[28,40],[33,41],[34,36]],'947159');line(0,28,44,31,44,1,'2d2823');line(0,35,44,38,44,1,'2d2823');
 ellipse(0,33,54,6,5,'a27659');ellipse(0,33,54,4,2.5,'4a3028');line(0,30,56,35,56,1,'bd8f71');
 poly(0,[[10,40],[14,39],[17,44],[14,47],[10,45]],'9e7b60');
 line(0,20,58,25,61,1,'49382f');line(0,44,58,39,61,1,'49382f');
 // The muscle definition is painted across continuous surfaces, not piled-on cubes.
 for(let s of[-1,1]){
  const mirror=p=>p.map(([x,y])=>[s<0?x:64-x,y]);
  poly(2,mirror([[1,12],[13,9],[29,15],[31,29],[27,43],[15,48],[2,40]]),'3b2e27');
  poly(2,mirror([[3,13],[14,11],[28,17],[29,29],[24,40],[14,44],[3,37]]),s<0?'785a45':'694c3a');
  poly(2,mirror([[5,15],[14,12],[23,17],[21,21],[11,20],[5,24]]),'9d7c60');
  poly(2,mirror([[0,45],[12,49],[27,44],[31,46],[25,56],[10,60],[0,55]]),'49352b');
  poly(2,mirror([[15,2],[24,7],[30,11],[29,16],[22,10]]),'44342b');
  for(let n=0;n<3;n++){
   const y=4+n*18;
   poly(3,mirror([[5,y+3],[14,y],[29,y+3],[30,y+13],[22,y+17],[10,y+15]]),'3b2d25');
   poly(3,mirror([[7,y+4],[16,y+2],[27,y+4],[27,y+11],[21,y+14],[10,y+12]]),n%2?'795b43':'826148');
   poly(3,mirror([[10,y+4],[19,y+3],[24,y+5],[19,y+6],[11,y+7]]),'a17d5a');
  }
  poly(7,mirror([[3,9],[17,6],[29,13],[27,33],[14,46],[5,40]]),'3e3028');
  poly(7,mirror([[6,11],[17,9],[26,15],[24,28],[14,39],[8,35]]),'866448');
 }
 line(2,31,17,31,48,1,'302823');line(3,31,2,31,58,1,'332922');
 for(const m of[4,5,8,10,14]){
  poly(m,[[4,10],[13,3],[32,2],[50,12],[58,30],[48,48],[30,60],[12,54],[4,35]],'49362b');
  poly(m,[[9,12],[17,7],[32,6],[46,15],[51,30],[44,43],[29,54],[16,49],[9,33]],'7e5d44');
  poly(m,[[14,13],[24,8],[35,11],[37,16],[24,17],[17,27],[12,28]],'ad8964');
  poly(m,[[30,22],[42,19],[48,30],[39,43],[29,50],[24,44],[32,34]],'62452f');
  line(m,9,52,19,59,1,'342923');line(m,45,48,53,38,1,'342923');
 }
 // Dark posing trunks; bare feet and curled fists use skin-tone atlases.
 poly(6,[[0,8],[13,12],[32,16],[51,12],[64,8],[60,29],[44,51],[32,62],[20,51],[4,29]],'222b3a');
 poly(6,[[4,12],[14,16],[29,20],[30,42],[21,36],[10,23]],'5e7185');
 line(6,5,10,31,17,2,'8b9aaa');line(6,31,17,58,10,2,'6b7c8f');
 for(let i=0;i<4;i++){line(12,13+i*10,19,13+i*10,49,1,'3d2d24');ellipse(12,18+i*10,22,3,2,'ad8563');}
 line(12,12,49,51,49,2,'443025');line(12,12,51,50,51,1,'896547');
 for(let i=0;i<5;i++){line(5,9+i*10,46,9+i*10,61,1,'3c2d25');ellipse(5,13+i*10,55,3,3,'9b785b');}
 ellipse(9,31,32,10,20,'3d3029');ellipse(9,34,31,6,14,'8a674e');
 return sharp(data,{raw:{width:size,height:size,channels:4}}).png().toBuffer();
}
function build(){
 const elements=[],groups=[],outliner=[],animations=[];
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Jerome',model_identifier:'jerome',resolution:{width:256,height:256},visible_box:[6,6,0],elements,groups,outliner,textures:[],animations};
 const group=(name,origin,parent)=>{let g={name,uuid:uid(name),origin,rotation:[0,0,0],visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const face=(e,pts,mat,uv)=>{let keys=pts.map(p=>{p=p.map(v=>+v.toFixed(5));let k=Object.keys(e.vertices).find(k=>e.vertices[k].every((v,i)=>v===p[i]));if(k)return k;k='v'+Object.keys(e.vertices).length;e.vertices[k]=p;return k;});let u=mat%4*64,v=(mat>>2)*64;uv=uv||[[u+1,v+63],[u+63,v+63],[u+63,v+1],[u+1,v+1]];e.faces['f'+Object.keys(e.faces).length]={vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,uv[i]])),texture:0};};
 const ring=(x,y,z,w,d)=>[[-.36,-.5],[.36,-.5],[.5,-.3],[.5,.3],[.36,.5],[-.36,.5],[-.5,.3],[-.5,-.3]].map(([X,Z])=>[x+X*w,y,z+Z*d]);
 const loft=(name,sections,mat,parent,map)=>{let e={type:'mesh',name,uuid:uid('mesh/'+name),origin:[0,0,0],rotation:[0,0,0],vertices:{},faces:{},visibility:true,export:true};elements.push(e);parent.children.push(e.uuid);let rr=sections.map(p=>ring(...p));
  const low=sections[0][1],high=sections[sections.length-1][1],ox=mat%4*64,oy=(mat>>2)*64;
  for(let n=1;n<rr.length;n++)for(let j=0;j<8;j++){let p=[rr[n-1][(j+1)%8],rr[n-1][j],rr[n][j],rr[n][(j+1)%8]],m=map&&map(p,j);
   const wrapped=p.map((v,i)=>[ox+1+(j+(i===0||i===3?1:0))*7.7,oy+1+(high-v[1])/(high-low)*62]);face(e,p,m?m.mat:mat,m?m.uv:wrapped);}
  for(const end of[0,rr.length-1])for(const ids of[[0,1,2,7],[7,2,3,6],[6,3,4,5]])face(e,(end===0?ids:ids.slice().reverse()).map(i=>rr[end][i]),mat,[[ox+26,oy+15],[ox+32,oy+15],[ox+32,oy+9],[ox+26,oy+9]]);return e;
 };
 const uv=(p,mat,x,width,low,high)=>({mat,uv:p.map(v=>[mat%4*64+1+Math.max(0,Math.min(1,(v[0]-x)/width+.5))*62,(mat>>2)*64+1+Math.max(0,Math.min(1,(high-v[1])/(high-low)))*62])});
 const root=group('Root',[0,0,0]),hips=group('Hips',[0,25,0],root),body=group('Body',[0,28,0],hips),head=group('Head',[0,42,0],body);
 loft('Athletic pelvis',[[0,24,0,16,9],[0,28,0,17,10],[0,30,0,16,9]],6,hips);
 loft('Defined abdomen and obliques',[[0,27,0,16,9],[0,32,-.2,20,11],[0,35,0,24,12]],3,body,(p,j)=>j<2||j===7?uv(p,3,0,24,27,35):uv(p,7,0,24,27,35));
 loft('Massive pectorals and trapezius',[[0,33,0,23,11],[0,37,-.5,32,15],[0,41,0,29,13],[0,43,0,17,9]],2,body,(p,j)=>j<2||j===7?uv(p,2,0,32,33,43):uv(p,7,0,32,33,43));
 loft('Thick neck',[[0,41,0,10,8],[0,45,0,8,7]],1,body);
 loft('Bald rounded head',[[0,43,-.5,6.2,6],[0,45,-.4,8.8,7.8],[0,50,0,9.6,8.7],[0,52.5,.3,8.1,7.8],[0,54,.4,4.4,4.8]],1,head,(p,j)=>j<2||j===7?uv(p,0,0,9.6,43,54):uv(p,1,0,9.6,43,54));
 for(const side of[-1,1]){
  loft('Ear '+side,[[side*4.1,45.6,.2,1,1.5],[side*4.7,46.7,.1,1.2,2],[side*4.4,48.2,.2,.7,1.2]],9,head);
  const arm=group('Arm '+side,[side*13,40,0],body),fore=group('Forearm '+side,[side*17,31,0],arm),hand=group('Hand '+side,[side*18.5,23,-.5],fore);
  loft('Massive deltoid '+side,[[side*14.2,35,0,10,11],[side*14.8,39,0,13,13],[side*12,42,0,10,11]],8,arm,(p,j)=>j<2||j===7?uv(p,8,side*14,13,35,42):null);
  loft('Biceps and triceps '+side,[[side*17,30,-.1,7.2,7.4],[side*17,33.5,-.6,10.2,10.5],[side*15,37,0,9,10]],10,arm,(p,j)=>j<2||j===7?uv(p,10,side*16,11,30,37):null);
  loft('Bare muscular forearm '+side,[[side*18.6,22.8,-.5,5.7,5.8],[side*18.3,27,-.3,8.4,8.7],[side*17,31,0,8,8]],4,fore,(p,j)=>j<2||j===7?uv(p,4,side*18,9,23,31):null);
  loft('Closed fist '+side,[[side*18.5,18.6,-.7,5,5.5],[side*18.7,20,-1,6.4,6.4],[side*18.5,23.3,-.5,5.6,5.7]],12,hand,(p,j)=>j<2||j===7?uv(p,12,side*18.6,6.5,18.6,23.3):null);
  loft('Folded thumb '+side,[[side*16.2,20.3,-2,1.7,2.2],[side*16.1,22,-1.7,2,2.5]],4,hand);
  const leg=group('Leg '+side,[side*5,26,0],hips),shin=group('Shin '+side,[side*5,14,0],leg),foot=group('Foot '+side,[side*5,3,0],shin);
  loft('Powerful bare thigh '+side,[[side*5,13.5,0,6.8,7],[side*5.2,18.5,-.3,10,10.5],[side*4.7,25.5,0,10,10]],8,leg,(p,j)=>j<2||j===7?uv(p,8,side*5,10.5,13,26):null);
  loft('Calf and shin '+side,[[side*5,2.5,-.4,4.7,5],[side*5,7,.6,7,7.5],[side*5,11.5,.5,8,8],[side*5,15,0,6.8,6.5]],10,shin,(p,j)=>j<2||j===7?uv(p,10,side*5,8,2,15):null);
  loft('Bare foot and toes '+side,[[side*5,.2,-1.8,6.8,10.5],[side*5,1.5,-2,7,10.5],[side*5,3.5,-1,5.5,7.5]],5,foot,(p,j)=>j<2||j===7?uv(p,5,side*5,7,.2,3.5):null);
 }
 const smooth=t=>(t=Math.max(0,Math.min(1,t)))*t*(3-2*t),wave=t=>Math.sin(t*Math.PI*2),curve=(t,points)=>{for(let i=1;i<points.length;i++)if(t<=points[i][0]){let[a,x]=points[i-1],[b,y]=points[i];return x+(y-x)*smooth((t-a)/(b-a));}return points[points.length-1][1];};
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 const clip=(name,length,loop,tracks)=>{let animators={},samples=Math.max(30,Math.ceil(length*24));for(let[bone,values]of Object.entries(tracks)){let g=groups.find(g=>g.name===bone);assert(g);let keyframes=[];for(let track of Array.isArray(values)?values:[values])for(let i=0;i<=samples;i++){let value=track.fn(i/samples);assert(value.every(Number.isFinite));keyframes.push({uuid:uid(name+'/'+bone+'/'+track.channel+'/'+i),channel:track.channel,time:+(length*i/samples).toFixed(6),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((v,j)=>[v,''+(+value[j].toFixed(4))]))]});}animators[g.uuid]={name:bone,type:'bone',keyframes};}animations.push({uuid:uid('clip/'+name),name:'animation.jerome.'+name,length,loop:loop?'loop':'once',override:false,animators});};
 const idle={Body:R(t=>[.6*wave(t),0,0]),Hips:P(t=>[0,.12*wave(t),0]),Head:R(t=>[-.5*wave(t),0,0])},run={Hips:[P(t=>[0,.35*Math.cos(t*Math.PI*4),0]),R(t=>[-7,4*wave(t),0])],Body:R(t=>[-3,-7*wave(t),3*wave(t)]),Head:R(t=>[5,2*wave(t),0])};
 for(let s of[-1,1]){
  idle['Arm '+s]=R(t=>[2+.7*wave(t),s*2,s*-1]);idle['Forearm '+s]=R(t=>[9+.7*wave(t-.1),0,0]);
  run['Arm '+s]=R(t=>[s*34*wave(t),0,-s*6]);run['Forearm '+s]=R(t=>[30+15*Math.max(0,-s*wave(t)),0,0]);
  run['Leg '+s]=R(t=>[-s*38*wave(t),0,0]);run['Shin '+s]=R(t=>[-45*Math.max(0,s*wave(t-.12)),0,0]);run['Foot '+s]=R(t=>[10*wave(t+.2*s),0,0]);
 }
 clip('idle',3.4,true,idle);clip('run',.7,true,run);
 const punch=(t,s)=>smooth(Math.max(0,1-Math.abs(t-(s<0?.3:.5))/.14,s<0?1-Math.abs(t-.725)/.14:0));
 const combo={Body:R(t=>[-4,20*(punch(t,1)-punch(t,-1)),4*(punch(t,1)-punch(t,-1))]),Hips:R(t=>[0,9*(punch(t,1)-punch(t,-1)),0]),Head:R(t=>[3,-8*(punch(t,1)-punch(t,-1)),0])};
 const arrival={Root:P(t=>[0,curve(t,[[0,10],[.25,7],[.5,-1],[.7,0],[1,0]]),0]),Body:R(t=>[curve(t,[[0,-8],[.48,-16],[.75,0],[1,0]]),0,0]),Head:R(t=>[8*(1-smooth(t/.7)),0,0])};
 const rush={Body:R(t=>[-22*smooth(t/.25)*(1-smooth((t-.68)/.32)),18*Math.sin(Math.PI*t),-5*Math.sin(Math.PI*t)]),Head:R(t=>[16*smooth(t/.2),-8*Math.sin(Math.PI*t),0])};
 const slam={Body:R(t=>[curve(t,[[0,0],[.3,8],[.48,12],[.55,-35],[.73,-22],[1,0]]),0,0]),Hips:P(t=>[0,curve(t,[[0,0],[.3,1],[.5,1],[.55,-2],[1,0]]),0]),Head:R(t=>[curve(t,[[0,0],[.4,-10],[.55,20],[1,0]]),0,0])};
 const sphere={Body:R(t=>[0,curve(t,[[0,0],[.3,-20],[.5,17],[.6,-14],[.7,17],[1,0]]),0]),Head:R(t=>[0,7*Math.sin(Math.PI*t),0])};
 const beamPose={Body:R(t=>[curve(t,[[0,0],[.28,5],[.4,-8],[.85,-8],[1,0]]),0,0]),Head:R(t=>[-4*Math.sin(Math.PI*t),0,0]),Hips:P(t=>[0,-.5*Math.sin(Math.PI*t),0])};
 const surge={Body:R(t=>[curve(t,[[0,0],[.2,-8],[.56,5],[1,0]]),0,0]),Head:R(t=>[-12*Math.sin(Math.PI*t),0,0]),Hips:P(t=>[0,-.5*Math.sin(Math.PI*t),0])};
 for(let s of[-1,1]){
  arrival['Arm '+s]=R(t=>[40*(1-smooth(t/.7)),s*-12*(1-smooth(t/.7)),0]);arrival['Forearm '+s]=R(t=>[65*(1-smooth(t/.7)),0,0]);
  combo['Arm '+s]=R(t=>[18+77*punch(t,s),-s*10,-s*8]);combo['Forearm '+s]=R(t=>[65*(1-punch(t,s)),s*8,0]);
  rush['Arm '+s]=R(t=>[s<0?45*Math.sin(Math.PI*t):-28*Math.sin(Math.PI*t),s*-20*Math.sin(Math.PI*t),-s*12*Math.sin(Math.PI*t)]);
  rush['Forearm '+s]=R(t=>[75*Math.sin(Math.PI*t),0,0]);rush['Leg '+s]=R(t=>[s*32*Math.sin(Math.PI*t),0,0]);rush['Shin '+s]=R(t=>[-28*Math.sin(Math.PI*t),0,0]);
  slam['Arm '+s]=R(t=>[curve(t,[[0,0],[.3,160],[.48,170],[.55,35],[.7,25],[1,0]]),s*-12,-s*8]);
  slam['Forearm '+s]=R(t=>[curve(t,[[0,0],[.3,38],[.48,45],[.55,4],[1,0]]),0,0]);
  const thrust=t=>curve(t,[[0,0],[.3,55],[.46,65],[.5,100],[.58,60],[.68,65],[.7,100],[.85,75],[1,0]]);
  sphere['Arm '+s]=R(t=>[s<0?thrust(t):20*Math.sin(Math.PI*t),s<0?-8:0,-s*8*Math.sin(Math.PI*t)]);
  sphere['Forearm '+s]=R(t=>[s<0?Math.max(0,65-thrust(t)*.65):35*Math.sin(Math.PI*t),0,0]);
  beamPose['Arm '+s]=R(t=>[curve(t,[[0,0],[.2,60],[.36,65],[.4,95],[.85,95],[1,0]]),-s*19,-s*4]);
  beamPose['Forearm '+s]=R(t=>[curve(t,[[0,0],[.2,70],[.36,65],[.4,5],[.85,5],[1,0]]),s*12,0]);
  surge['Arm '+s]=R(t=>[25*Math.sin(Math.PI*t),s*8,-s*42*Math.sin(Math.PI*t)]);
  surge['Forearm '+s]=R(t=>[85*Math.sin(Math.PI*t),0,0]);
 }
 clip('arrival',3,false,arrival);clip('combo',2,false,combo);clip('rush',2.3,false,rush);clip('slam',2.4,false,slam);
 clip('sphere',3,false,sphere);clip('beam',3.9,false,beamPose);clip('surge',2.8,false,surge);
 // Preserve the heavy build without letting the shoulder span dwarf his height.
 for(const e of elements)for(const v of Object.values(e.vertices))v[0]*=.87;
 const narrow=g=>{g.origin[0]*=.87;for(const child of g.children||[])if(typeof child==='object')narrow(child);};outliner.forEach(narrow);
 for(const g of groups)g.origin=[...g.origin];
 return model;
}
function bakeModel(model){
 const meshes=new Map(model.elements.map(e=>[e.name,e])),nodes=bake({...model,elements:model.elements.map(e=>({...e,from:[0,0,0],to:[0,0,0],faces:{}}))});
 const walk=nodes=>{for(let n of nodes){if(n.children){walk(n.children);continue;}let e=meshes.get(n.name);n.quads=[];
  for(let f of Object.values(e.faces)){let v=f.vertices.map(k=>[...e.vertices[k],f.uv[k][0]/256,f.uv[k][1]/256]),[a,b,c]=v,ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],length=Math.hypot(...normal);assert(length>1e-8);if(v.length===3)v.push(v[2]);n.quads.push({vertices:v,normal:normal.map(x=>x/length)});}
 }};walk(nodes);return nodes;
}
(async()=>{const model=build(),png=await texture();model.textures=[{uuid:uid('texture'),name:'jerome.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 const rig={sourceHash:crypto.createHash('sha256').update(JSON.stringify(model)).digest('hex'),nodes:bakeModel(model),animations:clipsFrom(model)};
 assert(model.elements.length<=30);assert(rig.animations.length===9);
 write('deliverables/jerome/jerome.bbmodel',JSON.stringify(model));write('src/main/resources/assets/kazimod/models/entities/jerome.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/jerome.png',png);
 console.log(`Jerome: ${model.elements.length} low-poly parts, ${model.groups.length} bones, ${rig.animations.length} clips.`);
})().catch(e=>{console.error(e);process.exit(1);});

