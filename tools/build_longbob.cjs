// Editable, low-poly costume model with a pixel-painted atlas and sampled animation curves.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bake,clipsFrom}=require(process.env.LONGBOb_BAKE_MODULE||'./build_shikigami.cjs');
const root=process.argv[2]||path.resolve(__dirname,'..');
const write=(p,data)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);};
const uid=s=>{let h=crypto.createHash('md5').update('longbob/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const rgb=c=>typeof c==='string'?c.match(/../g).map(v=>parseInt(v,16)):c;
async function texture(){
 const data=Buffer.alloc(256*256*4),pal=[['d0a387','ac7a66'],['c7977c','986f5a'],['624735','302923'],['292a31','17191f'],['282b32','15181f'],['303039','171b24'],['44444c','292d37'],['75747c','343743'],['2c3038','171d29'],['2a2c34','151820'],['323846','141b29'],['222831','10131a'],['b9b5a9','7d7e80'],['1b2029','080d15'],['c4c7ce','375681'],['222830','151b22']];
 const dot=(m,x,y,c)=>{x=Math.round(x);y=Math.round(y);if(x<0||y<0||x>=64||y>=64)return;let i=(((m>>2)*64+y)*256+(m%4*64+x))*4;rgb(c).forEach((v,j)=>data[i+j]=Math.max(0,Math.min(255,Math.round(v))));data[i+3]=255;};
 const rect=(m,x,y,w,h,c)=>{for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)dot(m,i,j,c);};
 const ellipse=(m,x,y,rx,ry,c)=>{for(let j=Math.floor(y-ry);j<=y+ry;j++)for(let i=Math.floor(x-rx);i<=x+rx;i++)if(((i-x)/rx)**2+((j-y)/ry)**2<=1)dot(m,i,j,c);};
 const poly=(m,p,c)=>{for(let y=Math.floor(Math.min(...p.map(v=>v[1])));y<=Math.max(...p.map(v=>v[1]));y++)for(let x=Math.floor(Math.min(...p.map(v=>v[0])));x<=Math.max(...p.map(v=>v[0]));x++){let inside=false;for(let i=0,j=p.length-1;i<p.length;j=i++)if((p[i][1]>y)!==(p[j][1]>y)&&x<(p[j][0]-p[i][0])*(y-p[i][1])/(p[j][1]-p[i][1])+p[i][0])inside=!inside;if(inside)dot(m,x,y,c);}};
 const line=(m,x,y,X,Y,w,c)=>{let n=Math.max(Math.abs(X-x),Math.abs(Y-y));for(let i=0;i<=n;i++)rect(m,Math.round(x+(X-x)*i/(n||1)),Math.round(y+(Y-y)*i/(n||1)),w,w,c);};
 const paw=(m,x,y,scale,edge,fill)=>{ellipse(m,x,y+8*scale,12*scale,10*scale,edge);ellipse(m,x,y+8*scale,10*scale,8*scale,fill);for(let [dx,dy]of[[-13,-7],[-5,-15],[6,-15],[15,-6]]){ellipse(m,x+dx*scale,y+dy*scale,5.3*scale,7*scale,edge);ellipse(m,x+dx*scale,y+dy*scale,4*scale,5.5*scale,fill);}};
 for(let m=0;m<16;m++){let a=rgb(pal[m][0]),b=rgb(pal[m][1]);for(let y=0;y<64;y++)for(let x=0;x<64;x++){
  let t=Math.floor(y/4)*4/64,n=Math.sin(Math.floor(x/2)*17.1+Math.floor(y/2)*13.7+m*7.7)*2;
  dot(m,x,y,a.map((v,j)=>v*(1-t)+b[j]*t+n));
 }}
 // Calm face, warm skin, side-parted brown hair, light stubble and closed lips.
 poly(0,[[0,0],[18,0],[14,26],[7,35],[0,40]],'5c4333');
 poly(0,[[3,35],[9,33],[13,46],[20,58],[14,64],[0,64]],'b2856d');
 poly(0,[[61,31],[53,34],[51,47],[43,59],[51,64],[64,64]],'b78770');
 ellipse(0,32,42,17,16,'c6957b');ellipse(0,31,34,9,12,'d2a184');
 for(let x of[18,45]){line(0,x-7,22,x+4,20,2,'49372e');line(0,x-5,28,x+5,27,2,'766052');rect(0,x-3,29,7,2,'9b9180');rect(0,x,28,3,3,'46463f');line(0,x-5,32,x+6,31,1,'a27967');}
 line(0,30,31,28,40,2,'b1846b');line(0,28,42,31,44,1,'996e59');line(0,35,42,38,41,1,'986a57');
 ellipse(0,32,49,9,3,'ae766d');line(0,23,49,40,49,1,'845c53');line(0,27,51,37,51,1,'c49183');
 for(let y=53;y<64;y++)for(let x=12;x<54;x++)if(Math.sin(x*19+y*37)>.72)dot(0,x,y,'957969');
 for(let y=33;y<43;y++)for(let x=8;x<57;x++)if(Math.sin(x*31+y*53)>.96)dot(0,x,y,'b3836b');
 for(let x=0;x<64;x+=5){line(2,x,0,x-8,63,2,x%2?'79593d':'493a2c');line(2,x+2,0,x-6,60,1,'87664a');}
 // Jacket trim and paw badge match the reference, rather than generic armor.
 poly(3,[[24,0],[40,0],[44,24],[32,39],[19,25]],'11151c');
 line(3,20,0,12,63,3,'2a5697');line(3,44,0,52,63,3,'2a5697');
 line(3,18,3,10,63,1,'4d73ac');line(3,46,3,54,63,1,'5579aa');
 line(3,12,38,8,62,1,'687582');paw(3,21,45,.45,'2a4c79','4774ad');
 line(4,4,20,15,63,2,'242b39');line(4,61,20,50,63,2,'394255');
 for(let m of[5,7,8])for(let i=0;i<95;i++){let x=(i*19)%64,y=(i*37)%64;line(m,x,y,x+(i%3-1)*3,y+4,1,m===8?'373b48':i%3?'6e6d78':'96929a');}
 poly(6,[[5,62],[24,5],[38,1],[59,62]],'999ba4');poly(6,[[12,62],[28,10],[35,8],[51,62]],'d5d8dc');poly(6,[[25,61],[32,14],[37,26],[47,61]],'346bad');
 for(let y=14;y<62;y+=4){line(6,25-(y-14)*.13,y,29,y-2,1,'eff0ee');line(6,49,y,45,y-3,1,'788495');}
 paw(8,30,39,1,'244572','356aab');line(8,20,45,35,41,2,'477fbe');
 line(9,8,0,13,63,2,'3a3c47');line(9,48,0,43,63,2,'0f131c');line(9,10,35,25,38,1,'41434d');
 line(10,3,42,61,42,3,'4773a4');line(10,3,57,61,57,4,'939ba4');
 for(let x=4;x<64;x+=13)rect(11,x,15,7,27,'3568a0');line(11,0,14,63,14,1,'535a65');line(11,0,43,63,43,1,'4c5159');
 for(let y=0;y<64;y++){let c=y<22?'d7d9da':y<44?'527db0':'293b55';for(let x=0;x<64;x++)dot(14,x,y,c);}
 for(let i=0;i<120;i++){let x=i*17%64,y=i*11%64;line(14,x,y,x-3,y+4,1,y<22?'a7adb9':y<44?'7297c3':'3c5069');}
 paw(15,32,33,1.3,'d8d4c6','191e27');
 return sharp(data,{raw:{width:256,height:256,channels:4}}).png().toBuffer();
}
function build(){
 const elements=[],groups=[],outliner=[],animations=[];
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'longbobthegod',model_identifier:'longbobthegod',resolution:{width:256,height:256},visible_box:[5,6,0],elements,groups,outliner,textures:[],animations};
 const group=(name,origin,parent)=>{let g={name,uuid:uid(name),origin,rotation:[0,0,0],visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const mesh=(name,parent)=>{let e={type:'mesh',name,uuid:uid('mesh/'+name),origin:[0,0,0],rotation:[0,0,0],vertices:{},faces:{},visibility:true,export:true};elements.push(e);parent.children.push(e.uuid);return e;};
 const face=(e,points,mat,uv)=>{let keys=points.map(p=>{p=p.map(v=>+v.toFixed(5));let k=Object.keys(e.vertices).find(k=>e.vertices[k].every((v,i)=>v===p[i]));if(k)return k;k='v'+Object.keys(e.vertices).length;e.vertices[k]=p;return k;});let u=mat%4*64,v=(mat>>2)*64;uv=uv||[[u+1,v+63],[u+63,v+63],[u+63,v+1],[u+1,v+1]];e.faces['f'+Object.keys(e.faces).length]={vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,uv[i]])),texture:0};};
 const uv=(p,m,x,w,lo,hi)=>p.map(v=>[m%4*64+1+Math.max(0,Math.min(1,(v[0]-x)/w+.5))*62,(m>>2)*64+1+Math.max(0,Math.min(1,(hi-v[1])/(hi-lo)))*62]);
 const ring=(x,y,z,w,d)=>[[-.36,-.5],[.36,-.5],[.5,-.3],[.5,.3],[.36,.5],[-.36,.5],[-.5,.3],[-.5,-.3]].map(([X,Z])=>[x+X*w,y,z+Z*d]);
 const loft=(name,sections,mat,parent,front)=>{let e=mesh(name,parent),rr=sections.map(p=>ring(...p)),lo=sections[0][1],hi=sections[sections.length-1][1];
  for(let n=1;n<rr.length;n++)for(let j=0;j<8;j++){let p=[rr[n-1][(j+1)%8],rr[n-1][j],rr[n][j],rr[n][(j+1)%8]],isFront=(j<2||j===7)&&front,m=isFront?front[0]:mat;let coords=isFront?uv(p,m,...front.slice(1)):
   p.map((v,i)=>[m%4*64+1+(j+(i===0||i===3?1:0))*7.7,(m>>2)*64+1+(hi-v[1])/(hi-lo)*62]);face(e,p,m,coords);}
  for(const end of[0,rr.length-1])for(const ids of[[0,1,2,7],[7,2,3,6],[6,3,4,5]])face(e,(end===0?ids:ids.slice().reverse()).map(i=>rr[end][i]),mat);return e;
 };
 const prism=(name,outline,z,depth,mat,parent)=>{let e=mesh(name,parent),a=outline.map(([x,y])=>[x,y,z]),b=outline.map(([x,y])=>[x,y,z+depth]);let x0=Math.min(...outline.map(v=>v[0])),x1=Math.max(...outline.map(v=>v[0])),y0=Math.min(...outline.map(v=>v[1])),y1=Math.max(...outline.map(v=>v[1]));
  for(let i=1;i<a.length-1;i++){let f=[a[0],a[i+1],a[i]];face(e,f,mat,uv(f,mat,(x0+x1)/2,x1-x0,y0,y1));face(e,[b[0],b[i],b[i+1]],7);}
  for(let i=0;i<a.length;i++)face(e,[a[i],a[(i+1)%a.length],b[(i+1)%a.length],b[i]],7);return e;};
 const loop=(name,x,y,z,rx,ry,width,parent,mat=12)=>{let e=mesh(name,parent),N=16;for(let i=0;i<N;i++){let a=i*2*Math.PI/N,b=(i+1)*2*Math.PI/N;let pts=[[Math.cos(a)*rx+x,Math.sin(a)*ry+y,z],[Math.cos(b)*rx+x,Math.sin(b)*ry+y,z],[Math.cos(b)*(rx-width)+x,Math.sin(b)*(ry-width)+y,z],[Math.cos(a)*(rx-width)+x,Math.sin(a)*(ry-width)+y,z]];face(e,pts,mat);}return e;};
 const root=group('Root',[0,0,0]),hips=group('Hips',[0,15,0],root),body=group('Body',[0,19,0],hips),head=group('Head',[0,30,0],body);
 loft('Dark trousers waist',[[0,14,0,15,10],[0,18,0,16,11]],9,hips);
 loft('Black and blue paw jacket',[[0,16,0,16,10],[0,21,-.6,20,14],[0,27,-.1,21,13],[0,31,0,15,10]],4,body,[3,0,21,16,31]);
 loft('Neck',[[0,29,0,8,7],[0,33,0,8.2,7]],1,head);
 loft('Black collar with blue tabs',[[0,30.2,-.1,9,7.8],[0,31.9,-.1,9,7.8]],11,head,[11,0,9,30.2,31.9]);
 loop('Silver collar ring',0,29.8,-4.15,.85,.9,.16,head);
 const pendant=mesh('Silver paw pendant',body);face(pendant,[[-1.35,26.5,-6.65],[1.35,26.5,-6.65],[1.35,29.2,-6.65],[-1.35,29.2,-6.65]],15);
 loft('Rounded face and jaw',[[0,31.7,-.25,7.5,7.8],[0,33.2,-.3,10.2,9.2],[0,36.6,0,11.3,10.3],[0,40,0,10.7,9.6],[0,41.5,.1,9,8.7]],1,head,[0,0,11.3,31.7,41.5]);
 loft('Brown hair crown',[[0,39.8,.5,11.3,9.5],[0,42,.5,10.5,9],[.8,42.8,.7,7,6.8]],2,head);
 loft('Long side-swept fringe',[[4.2,34,-4.1,1.8,2.4],[4.5,37.5,-4.1,3.2,2.7],[2.5,40,-3.6,5.2,3],[.8,42,-2,4.5,3.4]],2,head);
 loft('Hair behind left cheek',[[5,32.5,1.2,1.9,5],[5.4,36,1.4,2.2,6.6],[4.8,40,1,3,6.7]],2,head);
 loft('Hair behind right cheek',[[-4.7,33.5,1.7,1.8,5],[-5.1,37,1.6,2,6.3],[-4.6,40.5,1,2.7,6.5]],2,head);
 loop('Left round glasses',2.5,36.6,-5.3,2.35,2.05,.15,head);loop('Right round glasses',-2.5,36.6,-5.3,2.35,2.05,.15,head);
 const bridge=mesh('Glasses bridge and arms',head);face(bridge,[[-.4,36.8,-5.32],[.4,36.8,-5.32],[.4,37,-5.32],[-.4,37,-5.32]],12);
 for(let s of[-1,1])face(bridge,[[s*4.75,36.7,-5.25],[s*5.7,37.1,-1.8],[s*5.7,37.3,-1.8],[s*4.75,36.9,-5.25]],12);
 for(let s of[-1,1]){
  const ear=group('Ear '+s,[s*4.7,41,1],head);
  prism('Blue white costume ear '+s,[[s*2.8,41],[s*4.6,46],[s*7.4,51.7],[s*8.5,47.8],[s*8.8,43.3],[s*7.2,40.2]],.3,1.6,6,ear);
  if(s<0){loop('Ear silver hoop',s*8.5,45.4,.05,.45,.65,.12,ear);const charm=mesh('Ear paw charm',ear);face(charm,[[-9,42.6,-.05],[-7.6,42.6,-.05],[-7.6,44.2,-.05],[-9,44.2,-.05]],15);}
  const arm=group('Arm '+s,[s*9,28,0],body),fore=group('Forearm '+s,[s*12,22,0],arm),hand=group('Hand '+s,[s*13,16,-.4],fore);
  loft('Jacket sleeve '+s,[[s*12,20.6,0,6,6.5],[s*11.5,25,0,7,7.5],[s*9,29,0,7.5,8]],5,arm);
  loft('Gray furry gauntlet '+s,[[s*13,15.3,-.4,6.7,6.5],[s*12.7,18,-.1,7.3,7],[s*12,22,0,6.5,6.8]],7,fore);
  loft('Blue padded paw '+s,[[s*13,10.5,-.8,5.3,4],[s*13,12,-1,7,4.5],[s*13,15,-.6,7.2,4.8],[s*13,16.7,-.4,5.4,4.8]],7,hand,[8,s*13,7.2,10.5,16.7]);
  loft('Paw thumb '+s,[[s*16.3,11.7,-1.1,1.7,2.8],[s*16.9,13.3,-1,2.1,3],[s*16.1,14.8,-.6,2.3,3]],8,hand);
  for(let i=0;i<3;i++)prism('Paw claw '+s+' '+i,[[s*13-2+i*2,11.1],[s*13-1.65+i*2,9.8],[s*13-1.1+i*2,11.1]],-1.1,1.3,13,hand);
  const leg=group('Leg '+s,[s*4.3,16,0],hips),shin=group('Shin '+s,[s*4.3,8,0],leg),foot=group('Foot '+s,[s*4.3,2,0],shin);
  loft('Trouser thigh '+s,[[s*4.3,7.5,0,6.6,7],[s*4.4,11.5,0,7.7,8],[s*4.1,16,0,8.5,9]],9,leg);
  loft('Trouser shin '+s,[[s*4.3,2,0,6,6.5],[s*4.3,8.5,0,6.8,7]],9,shin);
  loft('Blue trimmed shoe '+s,[[s*4.3,.1,-1.4,6.7,9],[s*4.3,1.3,-1.5,7,9.7],[s*4.3,3,-.5,6.3,7]],10,foot,[10,s*4.3,7,0,3]);
 }
 const tail=group('Tail',[0,17,4],hips),tip=group('Tail Tip',[4,22,8],tail);
 loft('Fluffy tail base',[[0,16.5,4,4,4],[2.5,20,7,5.8,5.8],[4.2,23,8.2,6.8,6.3]],7,tail);
 loft('Blue white fluffy tail tip',[[4.1,21.8,8.1,6.7,6.2],[5.5,25.8,9,7.8,7],[6.4,29.5,8.7,6,6.4],[6.1,32,8.3,2,2.7]],14,tip);
 const smooth=t=>(t=Math.max(0,Math.min(1,t)))*t*(3-2*t),wave=t=>Math.sin(t*Math.PI*2),curve=(t,p)=>{for(let i=1;i<p.length;i++)if(t<=p[i][0])return p[i-1][1]+(p[i][1]-p[i-1][1])*smooth((t-p[i-1][0])/(p[i][0]-p[i-1][0]));return p[p.length-1][1];};
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 const clip=(name,length,loop,tracks)=>{let animators={},N=Math.max(40,Math.ceil(length*24));for(let [bone,values]of Object.entries(tracks)){let g=groups.find(g=>g.name===bone);assert(g,bone);let keyframes=[];for(let tr of Array.isArray(values)?values:[values])for(let i=0;i<=N;i++){let v=tr.fn(i/N);assert(v.every(Number.isFinite));keyframes.push({uuid:uid(name+bone+tr.channel+i),channel:tr.channel,time:+(length*i/N).toFixed(6),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((a,j)=>[a,''+(+v[j].toFixed(4))]))]});}animators[g.uuid]={name:bone,type:'bone',keyframes};}animations.push({uuid:uid('clip'+name),name:'animation.longbobthegod.'+name,length,loop:loop?'loop':'once',override:false,animators});};
 const idle={Body:R(t=>[.8*wave(t),0,0]),Head:R(t=>[-.5*wave(t),0,0]),Tail:R(t=>[2*wave(t),5*wave(t+.2),0]),'Tail Tip':R(t=>[3*wave(t-.1),8*wave(t+.1),0])};
 const run={Hips:[P(t=>[0,.23*Math.cos(t*Math.PI*4),0]),R(t=>[-5,3*wave(t),0])],Body:R(t=>[-3,-5*wave(t),2*wave(t)]),Head:R(t=>[5,2*wave(t),0]),Tail:R(t=>[12+4*wave(t),10*wave(t+.2),0]),'Tail Tip':R(t=>[-6,14*wave(t),0])};
 for(let s of[-1,1]){idle['Arm '+s]=R(t=>[3+.8*wave(t),s*4,-s*3]);idle['Forearm '+s]=R(t=>[8+.7*wave(t),0,0]);idle['Ear '+s]=R(t=>[wave(t+.15),0,s*1.1*wave(t)]);
  run['Arm '+s]=R(t=>[s*27*wave(t),0,-s*5]);run['Forearm '+s]=R(t=>[20+14*Math.max(0,-s*wave(t)),0,0]);run['Leg '+s]=R(t=>[-s*34*wave(t),0,0]);run['Shin '+s]=R(t=>[-35*Math.max(0,s*wave(t-.13)),0,0]);run['Ear '+s]=R(t=>[2*Math.cos(t*Math.PI*4),0,s*3*wave(t)]);}
 clip('idle',3.4,true,idle);clip('run',.8,true,run);
 const arrival={Body:R(t=>[0,4*Math.sin(Math.PI*t),0]),Head:R(t=>[2*Math.sin(Math.PI*t),-8*Math.sin(Math.PI*t),0]),'Arm 1':R(t=>[curve(t,[[0,0],[.25,95],[.7,95],[1,0]]),0,-10*Math.sin(Math.PI*t)]),'Forearm 1':R(t=>[35*Math.sin(Math.PI*t),0,0]),'Hand 1':R(t=>[0,0,12*Math.sin(t*Math.PI*6)*Math.sin(Math.PI*t)])};
 clip('arrival',2.75,false,arrival);
 const hit=(t,s)=>smooth(Math.max(0,1-Math.abs(t-(s<0?.375:.6875))/.16));
 const melee={Body:R(t=>[-3,13*(hit(t,1)-hit(t,-1)),2*(hit(t,1)-hit(t,-1))]),Head:R(t=>[2,-4*(hit(t,1)-hit(t,-1)),0])};
 const hydra={Body:R(t=>[-3*Math.sin(Math.PI*t),8*Math.sin(t*Math.PI*2),0]),Head:R(t=>[4*Math.sin(Math.PI*t),0,0])};
 const chloro={Body:R(t=>[-5*Math.sin(Math.PI*t),-10*Math.sin(Math.PI*t),0]),Head:R(t=>[-5*Math.sin(Math.PI*t),0,0])};
 const gumo={Body:R(t=>[curve(t,[[0,0],[.12,6],[.2,-3],[.9,-3],[1,0]]),0,0]),Head:R(t=>[-7*Math.sin(Math.PI*t),0,0])};
 const fugu={Body:R(t=>[-3*Math.sin(Math.PI*t),0,0]),Head:R(t=>[5*Math.sin(Math.PI*t),0,0])};
 const road={Root:P(t=>[0,2*Math.sin(Math.PI*t),0]),Body:R(t=>[-24*Math.sin(Math.PI*t),0,0]),Head:R(t=>[18*Math.sin(Math.PI*t),0,0]),Tail:R(t=>[18*Math.sin(Math.PI*t),6*wave(t*2),0]),'Tail Tip':R(t=>[-8,10*wave(t*2-.2),0])};
 for(let s of[-1,1]){
  melee['Arm '+s]=R(t=>[12+80*hit(t,s),s*-10,s*-7]);melee['Forearm '+s]=R(t=>[46*(1-hit(t,s)),0,0]);
  hydra['Arm '+s]=R(t=>[curve(t,[[0,0],[.2,65],[.75,80],[1,0]]),s*-18*Math.sin(Math.PI*t),s*-8]);hydra['Forearm '+s]=R(t=>[24*Math.sin(Math.PI*t),0,0]);hydra['Hand '+s]=R(t=>[-12*Math.sin(Math.PI*t),0,0]);
  chloro['Arm '+s]=R(t=>[curve(t,[[0,0],[.3,50],[.45,95],[.7,95],[1,0]]),s*-14*Math.sin(Math.PI*t),0]);chloro['Forearm '+s]=R(t=>[curve(t,[[0,0],[.3,70],[.45,0],[1,0]]),0,0]);
  gumo['Arm '+s]=R(t=>[10*Math.sin(Math.PI*t),s*8,-s*24*Math.sin(Math.PI*t)]);gumo['Forearm '+s]=R(t=>[18*Math.sin(Math.PI*t),0,0]);
  fugu['Arm '+s]=R(t=>[22*Math.sin(Math.PI*t),s*-20,-s*10*Math.sin(Math.PI*t)]);fugu['Forearm '+s]=R(t=>[50*Math.sin(Math.PI*t),0,0]);
  road['Arm '+s]=R(t=>[-25*Math.sin(Math.PI*t),s*6,-s*13]);road['Forearm '+s]=R(t=>[14,0,0]);road['Leg '+s]=R(t=>[-15*Math.sin(Math.PI*t),0,0]);road['Shin '+s]=R(t=>[-20*Math.sin(Math.PI*t),0,0]);
 }
 clip('melee',1.6,false,melee);clip('hydra',3.6,false,hydra);clip('chloro',2.4,false,chloro);clip('gumo',8,false,gumo);clip('fugu',2.2,false,fugu);clip('road',5,false,road);
 return model;
}
function bakeModel(model){
 const meshes=new Map(model.elements.map(e=>[e.name,e])),nodes=bake({...model,elements:model.elements.map(e=>({...e,from:[0,0,0],to:[0,0,0],faces:{}}))});
 const walk=nodes=>{for(let n of nodes){if(n.children){walk(n.children);continue;}let e=meshes.get(n.name);n.quads=[];for(let f of Object.values(e.faces)){let v=f.vertices.map(k=>[...e.vertices[k],f.uv[k][0]/256,f.uv[k][1]/256]),[a,b,c]=v,ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],length=Math.hypot(...normal);assert(length>1e-8,e.name);if(v.length===3)v.push(v[2]);n.quads.push({vertices:v,normal:normal.map(x=>x/length)});}}};walk(nodes);return nodes;
}
(async()=>{const model=build(),png=await texture();model.textures=[{uuid:uid('texture'),name:'longbobthegod.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 const rig={sourceHash:crypto.createHash('sha256').update(JSON.stringify(model)).digest('hex'),nodes:bakeModel(model),animations:clipsFrom(model)};
 assert(model.elements.length<60);assert(rig.animations.length===9);
 write('deliverables/longbobthegod/longbobthegod.bbmodel',JSON.stringify(model));write('src/main/resources/assets/kazimod/models/entities/longbobthegod.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/longbobthegod.png',png);
 console.log(`longbobthegod: ${model.elements.length} low-poly parts, ${model.groups.length} bones, ${rig.animations.length} animations.`);
})().catch(e=>{console.error(e);process.exit(1);});
