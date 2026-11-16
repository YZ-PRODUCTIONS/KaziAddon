// Editable low-poly Blockbench model, baked geometry, texture atlas, and animation clips.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bake,clipsFrom}=require(process.env.HERMANO_BAKE_MODULE||'./build_shikigami.cjs');
const root=process.argv[2]||path.resolve(__dirname,'..');
const write=(p,data)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);};
const uid=s=>{const h=crypto.createHash('md5').update('hermano/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
async function texture(){
 const size=256,data=Buffer.alloc(size*size*4),rgb=c=>typeof c==='string'?c.match(/../g).map(x=>parseInt(x,16)):c;
 const dot=(x,y,c)=>{x=Math.round(x);y=Math.round(y);if(x<0||x>=size||y<0||y>=size)return;let i=(y*size+x)*4,v=rgb(c);for(let a=0;a<3;a++)data[i+a]=Math.max(0,Math.min(255,v[a]));data[i+3]=255;};
 const poly=(m,p,c)=>{const ox=m%4*64,oy=(m>>2)*64;for(let y=Math.floor(Math.min(...p.map(v=>v[1])));y<=Math.max(...p.map(v=>v[1]));y++)for(let x=Math.floor(Math.min(...p.map(v=>v[0])));x<=Math.max(...p.map(v=>v[0]));x++){let inside=false;for(let i=0,j=p.length-1;i<p.length;j=i++)if((p[i][1]>y)!==(p[j][1]>y)&&x<(p[j][0]-p[i][0])*(y-p[i][1])/(p[j][1]-p[i][1])+p[i][0])inside=!inside;if(inside)dot(ox+x,oy+y,c);}};
 const line=(m,x,y,X,Y,w,c)=>{const n=Math.max(Math.abs(x-X),Math.abs(y-Y),1);for(let i=0;i<=n;i++)for(let a=0;a<w;a++)for(let b=0;b<w;b++)dot(m%4*64+x+(X-x)*i/n+a,(m>>2)*64+y+(Y-y)*i/n+b,c);};
 const colors=[['e9e6a5','c1bf7c'],['dfdda0','aaa96b'],['17191e','24252d'],['343f87','20275d'],['e7e8ee','a9acbd'],['e6e8ec','9c9eaf'],['202127','101116'],['252731','101117'],['3e488b','252c62'],['d3d198','9a9c67'],['3c438b','25285e'],['b1b17a','90945d'],['c2c6d2','8c90a1'],['343741','181a22'],['464a93','2e336f'],['ebe8ac','b7b57b']];
 for(let m=0;m<16;m++){let a=rgb(colors[m][0]),b=rgb(colors[m][1]);for(let y=0;y<64;y++)for(let x=0;x<64;x++){
  let t=Math.floor(y/4)*4/63,shade=Math.round((Math.cos((x-22)/35)*4+(Math.sin(Math.floor(x/5)*1.8+Math.floor(y/5)*2.7)>0?1.5:-1.5))/2)*2;
  dot(m%4*64+x,(m>>2)*64+y,a.map((v,i)=>v*(1-t)+b[i]*t+shade));
 }}
 // Tall, unarmored face: angular eyes, brow, cheek planes, and a closed stern mouth.
 poly(0,[[0,0],[27,0],[24,25],[19,38],[10,45],[5,62],[0,64]],'b2b578');
 poly(0,[[30,2],[44,4],[52,12],[55,30],[49,37],[39,34],[30,26]],'f2efb3');
 poly(0,[[7,39],[15,42],[27,46],[26,51],[19,53],[11,50]],'31332f');
 poly(0,[[57,39],[49,42],[37,46],[38,51],[45,53],[53,50]],'31332f');
 poly(0,[[9,41],[17,44],[25,47],[24,49],[19,50],[13,48]],'10121a');
 poly(0,[[55,41],[47,44],[39,47],[40,49],[45,50],[51,48]],'10121a');
 poly(0,[[21,46],[24,47],[23,49],[21,48]],'f5f0e0');poly(0,[[43,46],[40,47],[41,49],[43,48]],'f5f0e0');
 line(0,16,37,25,43,1,'90975d');line(0,48,37,39,43,1,'90975d');
 poly(0,[[29,46],[26,54],[30,55],[34,55],[38,53],[33,51]],'929864');
 poly(0,[[31,48],[30,52],[34,53],[33,50]],'e1dda0');
 line(0,25,58,31,57,1,'4e5944');line(0,31,57,38,58,1,'4e5944');line(0,29,60,35,60,1,'ebdea1');
 line(0,11,54,18,56,1,'8c945e');line(0,53,54,46,56,1,'8c945e');
 // The chest yoke and pectorals are texture details on a continuous torso.
 poly(2,[[0,23],[13,29],[21,43],[32,50],[43,43],[51,29],[64,23],[64,64],[0,64]],'303a7d');
 poly(2,[[0,32],[12,36],[21,49],[30,52],[25,57],[12,54],[0,47]],'48518f');
 poly(2,[[64,32],[52,36],[43,49],[34,52],[39,57],[52,54],[64,47]],'3b4587');
 poly(2,[[2,30],[12,34],[24,49],[31,51],[31,54],[21,52],[9,40]],'11141e');
 poly(2,[[62,30],[52,34],[40,49],[33,51],[33,54],[43,52],[55,40]],'11141e');
 poly(2,[[24,4],[31,15],[39,4],[36,20],[31,29],[27,20]],'30313c');
 line(2,8,20,21,23,2,'34343f');line(2,56,20,43,23,2,'34343f');
 // Abdomen suit follows the reference's broad blue chevron, not armor plating.
 poly(3,[[0,0],[15,6],[32,14],[49,6],[64,0],[62,36],[48,51],[32,63],[16,51],[2,36]],'3a4387');
 poly(3,[[4,12],[15,18],[25,26],[26,32],[15,29],[7,23]],'59629e');
 poly(3,[[60,12],[49,18],[39,26],[38,32],[49,29],[57,23]],'465195');
 poly(3,[[0,38],[13,45],[25,60],[22,64],[0,64]],'13161e');poly(3,[[64,38],[51,45],[39,60],[42,64],[64,64]],'13161e');
 line(3,31,25,31,56,1,'2a326f');
 for(const m of[8,10,14]){
  poly(m,[[7,10],[18,4],[38,6],[50,17],[47,25],[39,22],[28,12],[17,16],[11,27]],'56609d');
  poly(m,[[4,29],[15,24],[23,28],[30,43],[39,46],[49,38],[58,35],[57,49],[44,60],[22,62],[8,50]],'222a62');
  poly(m,[[18,28],[27,24],[36,30],[42,42],[35,49],[25,45]],'475293');
 }
 for(const m of[4,5,12]){
  poly(m,[[3,0],[14,2],[19,29],[12,49],[9,64],[0,64]],'8e93a7');
  poly(m,[[24,0],[37,0],[41,31],[35,49],[28,44]],'f5f4f2');
  line(m,9,7,52,7,1,'b9becd');line(m,7,10,51,10,1,'eceef3');
 }
 for(const m of[6,7,13]){
  poly(m,[[9,5],[21,3],[30,14],[28,32],[20,41],[15,34]],'363941');
  poly(m,[[48,14],[54,12],[56,44],[43,60],[34,61],[43,45]],'090c12');
 }
 // Fist atlas: curled finger bands and knuckles, no floating digit cubes.
 for(let i=0;i<4;i++){line(12,14+i*10,22,14+i*10,51,1,'858b9b');line(12,15+i*10,24,20+i*10,24,2,'f4f1ed');}
 line(12,12,48,52,48,1,'9ca0ae');
 return sharp(data,{raw:{width:size,height:size,channels:4}}).png().toBuffer();
}
function build(){
 const elements=[],groups=[],outliner=[],animations=[];
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'El Hermano',model_identifier:'el_hermano',resolution:{width:256,height:256},visible_box:[6,6,0],elements,groups,outliner,textures:[],animations};
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
 loft('Blue chevron abdomen',[[0,27,0,15,8],[0,32,0,18,10],[0,35,0,22,11]],3,body,(p,j)=>j<2||j===7?uv(p,3,0,22,27,35):null);
 loft('Broad chest and black yoke',[[0,33,0,21,10],[0,37,-.3,28,13],[0,41,0,26,11],[0,43,0,16,8]],2,body,(p,j)=>j<2||j===7?uv(p,2,0,28,33,43):uv(p,7,0,28,33,43));
 loft('High black collar',[[0,41,0,9,7],[0,44,0,7,6]],7,body);
 loft('Tall tapered head',[[0,43,-.5,5.7,5.3],[0,45,-.4,8,7],[0,49,0,9,8],[0,54,.4,7.8,7],[0,57,.6,4.6,5],[0,58,.7,2.4,2.9]],1,head,(p,j)=>j<2||j===7?uv(p,0,0,9,43,58):uv(p,1,0,9,43,58));
 for(const side of[-1,1]){
  loft('Ear '+side,[[side*4.1,45.6,.2,1,1.5],[side*4.7,46.7,.1,1.2,2],[side*4.4,48.2,.2,.7,1.2]],9,head);
  const arm=group('Arm '+side,[side*13,40,0],body),fore=group('Forearm '+side,[side*17,31,0],arm),hand=group('Hand '+side,[side*18.5,23,-.5],fore);
  loft('Deltoid '+side,[[side*14.2,35,0,9,10],[side*14.5,39,0,11,12],[side*12,42,0,9,10]],8,arm,(p,j)=>j<2||j===7?uv(p,8,side*14,11,35,42):null);
  loft('Muscular upper arm '+side,[[side*17,30,-.1,6.5,6.7],[side*17,33.5,-.4,8.5,9],[side*15,37,0,8,9]],10,arm,(p,j)=>j<2||j===7?uv(p,10,side*16,10,30,37):null);
  loft('Long white glove '+side,[[side*18.6,22.8,-.5,5.3,5.3],[side*18.3,27,-.2,6.8,7.2],[side*17,31,0,7.5,7.5]],4,fore,(p,j)=>j<2||j===7?uv(p,4,side*18,8,23,31):null);
  loft('Closed fist '+side,[[side*18.5,18.6,-.7,5,5.5],[side*18.7,20,-1,6.4,6.4],[side*18.5,23.3,-.5,5.6,5.7]],12,hand,(p,j)=>j<2||j===7?uv(p,12,side*18.6,6.5,18.6,23.3):null);
  loft('Folded thumb '+side,[[side*16.2,20.3,-2,1.7,2.2],[side*16.1,22,-1.7,2,2.5]],4,hand);
  const leg=group('Leg '+side,[side*5,26,0],hips),shin=group('Shin '+side,[side*5,14,0],leg),foot=group('Foot '+side,[side*5,3,0],shin);
  loft('Powerful thigh '+side,[[side*5,13.5,0,6,6],[side*5.2,18.5,0,8.4,8.5],[side*4.7,25.5,0,9.2,9]],6,leg,(p,j)=>j<2||j===7?uv(p,6,side*5,9.5,13,26):null);
  loft('Calf above boot '+side,[[side*5,9,0,6.3,6],[side*5.1,12,0,7.2,6.8],[side*5,15,0,6.5,6]],6,shin);
  loft('White tapered boot '+side,[[side*5,2.5,-.4,5.5,6],[side*5,7,0,6.5,6.7],[side*5,11.5,0,7.5,7]],5,shin,(p,j)=>j<2||j===7?uv(p,5,side*5,8,2,12):null);
  loft('Boot toe '+side,[[side*5,.2,-1.8,6.5,10],[side*5,1.5,-2,7,10.5],[side*5,3.5,-1,5.8,7.5]],5,foot);
 }
 const smooth=t=>(t=Math.max(0,Math.min(1,t)))*t*(3-2*t),wave=t=>Math.sin(t*Math.PI*2),curve=(t,points)=>{for(let i=1;i<points.length;i++)if(t<=points[i][0]){let[a,x]=points[i-1],[b,y]=points[i];return x+(y-x)*smooth((t-a)/(b-a));}return points[points.length-1][1];};
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 const clip=(name,length,loop,tracks)=>{let animators={},samples=Math.max(30,Math.ceil(length*24));for(let[bone,values]of Object.entries(tracks)){let g=groups.find(g=>g.name===bone);assert(g);let keyframes=[];for(let track of Array.isArray(values)?values:[values])for(let i=0;i<=samples;i++){let value=track.fn(i/samples);assert(value.every(Number.isFinite));keyframes.push({uuid:uid(name+'/'+bone+'/'+track.channel+'/'+i),channel:track.channel,time:+(length*i/samples).toFixed(6),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((v,j)=>[v,''+(+value[j].toFixed(4))]))]});}animators[g.uuid]={name:bone,type:'bone',keyframes};}animations.push({uuid:uid('clip/'+name),name:'animation.el_hermano.'+name,length,loop:loop?'loop':'once',override:false,animators});};
 const idle={Body:R(t=>[.6*wave(t),0,0]),Hips:P(t=>[0,.12*wave(t),0]),Head:R(t=>[-.5*wave(t),0,0])},run={Hips:[P(t=>[0,.35*Math.cos(t*Math.PI*4),0]),R(t=>[-7,4*wave(t),0])],Body:R(t=>[-3,-7*wave(t),3*wave(t)]),Head:R(t=>[5,2*wave(t),0])};
 for(let s of[-1,1]){
  idle['Arm '+s]=R(t=>[2+.7*wave(t),s*2,s*-1]);idle['Forearm '+s]=R(t=>[9+.7*wave(t-.1),0,0]);
  run['Arm '+s]=R(t=>[s*34*wave(t),0,-s*6]);run['Forearm '+s]=R(t=>[30+15*Math.max(0,-s*wave(t)),0,0]);
  run['Leg '+s]=R(t=>[-s*38*wave(t),0,0]);run['Shin '+s]=R(t=>[-45*Math.max(0,s*wave(t-.12)),0,0]);run['Foot '+s]=R(t=>[10*wave(t+.2*s),0,0]);
 }
 clip('idle',3.4,true,idle);clip('run',.7,true,run);
 const arrival={Root:P(t=>[0,curve(t,[[0,14],[.25,10],[.42,-1],[.65,0],[1,0]]),0]),Body:R(t=>[curve(t,[[0,-8],[.4,-14],[.72,0],[1,0]]),0,0]),Head:R(t=>[curve(t,[[0,16],[.65,3],[1,0]]),0,0])};
 const impact={Body:R(t=>[0,curve(t,[[0,0],[.2,-14],[.6,-20],[.65,18],[.83,8],[1,0]]),0]),Head:R(t=>[0,8*smooth(t/.6)*(1-smooth((t-.67)/.33)),0])};
 const counter={Body:R(t=>[2,0,0]),Head:R(t=>[-3,0,0])},full={Body:R(t=>[curve(t,[[0,0],[.2,-7],[.7,3],[1,0]]),0,0]),Head:R(t=>[curve(t,[[0,0],[.2,-10],[.72,4],[1,0]]),0,0]),Hips:P(t=>[0,-.4*Math.sin(Math.PI*t),0])};
 const rush={Body:R(t=>[-18*smooth(t/.25)*(1-smooth((t-.68)/.32)),12*Math.sin(Math.PI*t),0]),Head:R(t=>[12*smooth(t/.2),-8*Math.sin(Math.PI*t),0]),'Arm -1':R(t=>[curve(t,[[0,0],[.26,-40],[.34,94],[.64,94],[1,0]]),-8,4]),'Forearm -1':R(t=>[curve(t,[[0,0],[.26,66],[.34,0],[.64,0],[1,0]]),0,0]),'Arm 1':R(t=>[-35*Math.sin(Math.PI*t),0,-8]),'Forearm 1':R(t=>[55*Math.sin(Math.PI*t),0,0])};
 const glare={Head:R(t=>[curve(t,[[0,0],[.38,-6],[.48,2],[1,0]]),0,0]),Body:R(t=>[0,2*Math.sin(Math.PI*t),0])};
 const punch=(t,side)=>{let p=side<0?Math.max(0,1-Math.abs(t-.3)/.14,1-Math.abs(t-.725)/.14):Math.max(0,1-Math.abs(t-.5)/.14);return smooth(p);};
 const combo={Body:R(t=>[-3,18*(punch(t,1)-punch(t,-1)),3*(punch(t,1)-punch(t,-1))]),Hips:R(t=>[0,8*(punch(t,1)-punch(t,-1)),0]),Head:R(t=>[2,-7*(punch(t,1)-punch(t,-1)),0])};
 for(let s of[-1,1]){
  arrival['Arm '+s]=R(t=>[curve(t,[[0,55],[.25,55],[.65,0],[1,0]]),s*-25*(1-smooth((t-.3)/.4)),0]);arrival['Forearm '+s]=R(t=>[65*(1-smooth((t-.3)/.4)),s*35*(1-smooth((t-.3)/.4)),s*12*(1-smooth((t-.3)/.4))]);
  impact['Arm '+s]=R(t=>[s<0?curve(t,[[0,0],[.22,68],[.6,78],[.65,98],[.84,70],[1,0]]):16*Math.sin(Math.PI*t),s<0?-12*smooth(t/.25):0,s<0?8*smooth(t/.25):-8*Math.sin(Math.PI*t)]);
  impact['Forearm '+s]=R(t=>[s<0?curve(t,[[0,0],[.22,25],[.6,18],[.65,0],[1,0]]):25*Math.sin(Math.PI*t),0,0]);
  counter['Arm '+s]=R(t=>[52*smooth(t/.15),s*-36*smooth(t/.15),-s*10*smooth(t/.15)]);counter['Forearm '+s]=R(t=>[70*smooth(t/.15),s*35*smooth(t/.15),s*12*smooth(t/.15)]);
  full['Arm '+s]=R(t=>[-12*Math.sin(Math.PI*t),s*8,-s*18*Math.sin(Math.PI*t)]);full['Forearm '+s]=R(t=>[32*Math.sin(Math.PI*t),0,0]);
  combo['Arm '+s]=R(t=>[15+79*punch(t,s),-s*10,-s*8]);combo['Forearm '+s]=R(t=>[60*(1-punch(t,s)),s*8,0]);
  rush['Leg '+s]=R(t=>[s*30*Math.sin(Math.PI*t),0,0]);rush['Shin '+s]=R(t=>[-22*Math.sin(Math.PI*t),0,0]);
 }
 clip('arrival',3,false,arrival);clip('combo',2,false,combo);clip('rush',2.3,false,rush);clip('glare',1.9,false,glare);clip('impact',3.2,false,impact);clip('counter',1.6,false,counter);clip('full_power',3.5,false,full);
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
(async()=>{const model=build(),png=await texture();model.textures=[{uuid:uid('texture'),name:'el_hermano.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 const rig={sourceHash:crypto.createHash('sha256').update(JSON.stringify(model)).digest('hex'),nodes:bakeModel(model),animations:clipsFrom(model)};
 assert(model.elements.length<=30);assert(rig.animations.length===9);
 write('deliverables/el_hermano/el_hermano.bbmodel',JSON.stringify(model));write('src/main/resources/assets/kazimod/models/entities/el_hermano.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/el_hermano.png',png);
 console.log(`El Hermano: ${model.elements.length} low-poly parts, ${model.groups.length} bones, ${rig.animations.length} clips.`);
})().catch(e=>{console.error(e);process.exit(1);});
