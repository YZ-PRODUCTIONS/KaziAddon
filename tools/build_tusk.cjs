// Editable Blockbench sources and runtime rigs share exactly the same geometry and keyframes.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bake,clipsFrom}=require(process.env.TUSK_BAKE_MODULE||'./build_shikigami.cjs');
const refinement=require('./tusk_model_refinement.cjs');
const root=process.argv[2]||path.resolve(__dirname,'..');
const uid=s=>{const h=crypto.createHash('md5').update(s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,d);};
const smooth=p=>(p=Math.max(0,Math.min(1,p)))*p*(3-2*p),sin=p=>Math.sin(p*Math.PI*2);
const curve=(p,k)=>{for(let i=1;i<k.length;i++)if(p<=k[i][0])return k[i-1][1]+(k[i][1]-k[i-1][1])*smooth((p-k[i-1][0])/(k[i][0]-k[i-1][0]));return k.at(-1)[1];};
const materials=['pink','light','shadow','blue','metal','gold','dark','star','blue_star','mask2','mask3','mask4','disc','eyes','beak','heart'];
function canvas(size){const data=Buffer.alloc(size*size*4);const rgb=c=>typeof c==='string'?c.match(/../g).map(v=>parseInt(v,16)):c;
 const dot=(x,y,c,a=255)=>{x=Math.round(x);y=Math.round(y);if(x<0||x>=size||y<0||y>=size)return;const i=(y*size+x)*4;[data[i],data[i+1],data[i+2]]=rgb(c);data[i+3]=a;};
 const rect=(x,y,w,h,c,a=255)=>{for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)dot(i,j,c,a);};
 const line=(x,y,X,Y,w,c)=>{const n=Math.max(1,Math.abs(X-x),Math.abs(Y-y));for(let i=0;i<=n;i++)rect(Math.round(x+(X-x)*i/n)-w/2,Math.round(y+(Y-y)*i/n)-w/2,w,w,c);};
 const poly=(points,c)=>{for(let y=Math.floor(Math.min(...points.map(p=>p[1])));y<=Math.ceil(Math.max(...points.map(p=>p[1])));y++)for(let x=Math.floor(Math.min(...points.map(p=>p[0])));x<=Math.ceil(Math.max(...points.map(p=>p[0])));x++){
  let inside=false;for(let i=0,j=points.length-1;i<points.length;j=i++){const a=points[i],b=points[j];if((a[1]>y)!==(b[1]>y)&&x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;}if(inside)dot(x,y,c);
 }};
 const star=(x,y,r,c)=>poly(Array.from({length:10},(_,i)=>{let a=-Math.PI/2+i*Math.PI/5,R=i%2?r*.43:r;return[x+Math.cos(a)*R,y+Math.sin(a)*R];}),c);
 return{data,dot,rect,line,poly,star};
}
async function atlas(act){const s=256,c=canvas(s),colors=['c56a9c','efb4d3','843e70','586ba5','b9c6ba','e6ca73','252332','c56a9c','586ba5','e7a4c8','d889b8','d98bb5','dc8db9','da8eb8','e5c267','bd4f8d'];
 for(let m=0;m<16;m++){let ox=m%4*64,oy=Math.floor(m/4)*64,base=colors[m].match(/../g).map(v=>parseInt(v,16));
  for(let y=0;y<64;y++)for(let x=0;x<64;x++){const noise=((Math.floor(x/2)*19+Math.floor(y/2)*31+m*7)%11-5)*.65;
   const edge=Math.min(x,y,63-x,63-y),shine=8*Math.cos((x-23)/34)+4*Math.sin(y*.07)-(edge<3?12:0);
   c.dot(ox+x,oy+y,base.map(v=>Math.max(0,Math.min(255,Math.round(v+noise+shine)))));
  }
  const rect=(x,y,w,h,col)=>c.rect(ox+x,oy+y,w,h,col),line=(x,y,X,Y,w,col)=>c.line(ox+x,oy+y,ox+X,oy+Y,w,col),star=(x,y,r,col)=>c.star(ox+x,oy+y,r,col);
  if([0,1,2,3,4,5].includes(m)) {line(5,6,58,6,1,base.map(v=>Math.min(255,v+25)));line(4,57,59,57,2,base.map(v=>Math.max(0,v-28)));for(let i=0;i<4;i++)line(8+i*14,10,10+i*14,26,1,base.map(v=>Math.max(0,v-10)));}
  if(m===7||m===8){star(32,32,24,'6c3b5b');star(32,30,21,m===8?'e4b85a':'f2d87c');star(30,27,11,'ffe9a5');}
  if(m===9){for(let i=0;i<5;i++)rect(12+i*9,43,3,i%2?12:16,'a65386');rect(16,29,4,5,'242233');rect(43,29,4,5,'242233');star(22,13,6,'f4dc95');star(40,14,6,'f4dc95');}
  if(m===10){line(31,2,31,48,4,'843a70');line(34,2,34,48,2,'ffd5e7');for(const x of[14,46]){rect(x,38,3,7,'292538');rect(x,41,3,1,'9f507d');star(x+1,19,7,'edc982');}line(6,54,17,58,2,'8a3f6a');line(47,58,58,54,2,'8a3f6a');}
  if(m===11){line(19,7,19,24,4,'302f41');line(44,7,44,24,4,'302f41');line(19,24,25,29,4,'302f41');line(25,29,38,29,4,'302f41');line(38,29,44,24,4,'302f41');line(20,7,20,22,1,'dae4cf');line(43,7,43,22,1,'dae4cf');for(const x of[19,44]){star(x,40,10,'302637');star(x,39,4,'e7bbd4');}for(let x=12;x<60;x+=12)rect(x,55,4,8,'602a52');}
  if(m===12){for(let y=0;y<64;y++)for(let x=0;x<64;x++){let X=(x-31.5)/27,Y=(y-31.5)/28;if(X*X+Y*Y>1)c.dot(ox+x,oy+y,[0,0,0],0);else{const v=Math.round(19*Math.cos(Y*3)-12*X+((x+y)%3));c.dot(ox+x,oy+y,base.map(b=>Math.max(0,Math.min(255,b+v))));}}line(30,1,30,6,3,'b2b4a2');line(32,58,32,63,3,'b2b4a2');line(13,20,13,43,1,'fbc9e0');}
  if(m===13){for(const x of[13,42]){rect(x-3,20,17,16,'643553');rect(x,22,11,11,'fff0c7');rect(x+4,22,4,11,'2c2636');}line(13,49,49,49,2,'7c3b66');}
  if(m===14){line(32,2,32,59,2,'9d793d');line(27,4,29,53,2,'ffe8a0');}
  if(m===15){c.rect(ox,oy,64,64,'000000',0);c.poly([[ox+6,oy+14],[ox+17,oy+5],[ox+31,oy+14],[ox+45,oy+5],[ox+58,oy+14],[ox+58,oy+31],[ox+32,oy+59],[ox+6,oy+31]],'c15b99');line(12,15,22,11,3,'eeabd0');}
 }
 return sharp(c.data,{raw:{width:s,height:s,channels:4}}).png().toBuffer();
}
function model(act){const id='tusk_act_'+act,elements=[],groups=[],outliner=[],animations=[];let serial=0;
 const m={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Tusk Act '+act,model_identifier:id,resolution:{width:256,height:256},visible_box:[5,5,0],elements,groups,outliner,textures:[],animations};
 const group=(name,o=[0,0,0],parent=null)=>{let g={name,uuid:uid(id+'/bone/'+name),origin:o,rotation:[0,0,0],visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const cube=(name,c,s,mat,parent,rotation=[0,0,0],origin=c,front=null)=>{let faces={};for(const side of['north','south','west','east','up','down']){let index=materials.indexOf(side==='north'&&front?front:mat),u=index%4*64,v=Math.floor(index/4)*64;faces[side]={uv:[u+1,v+1,u+63,v+63],texture:0};}let e={name,uuid:uid(id+'/cube/'+serial++),from:c.map((v,i)=>v-s[i]/2),to:c.map((v,i)=>v+s[i]/2),origin,rotation,faces,box_uv:false,visibility:true,export:true,autouv:0};elements.push(e);parent.children.push(e.uuid);return e;};
 const bevel=(name,c,s,mat,parent,b=.7,front=null)=>{cube(name,c,[s[0]-b*2,s[1],s[2]],mat,parent,[0,0,0],c,front);cube(name+' side bevel',c,[s[0],s[1]-b*2,s[2]-b*2],mat,parent);};
 const beam=(name,a,b,w,d,mat,parent)=>{let v=b.map((n,i)=>n-a[i]),c=a.map((n,i)=>(n+b[i])/2),l=Math.hypot(...v);return cube(name,c,[w,l,d],mat,parent,[Math.atan2(v[2],Math.hypot(v[0],v[1]))*180/Math.PI,0,-Math.atan2(v[0],v[1])*180/Math.PI]);};
 const spike=(name,a,b,w,mat,parent)=>{const mid=a.map((v,i)=>v+(b[i]-v)*.65);beam(name,a,mid,w,w,mat,parent);beam(name+' point',mid,b,w*.38,w*.38,mat,parent);};
 const rootBone=group('Root'),body=group('Body',[0,act===4?29:act===3?20:12,0],rootBone),head=group('Head',[0,act===4?37:act===3?31:17,0],body);
 if(act===1){
  bevel('Star-shaped head',[0,16,0],[13,12,7],'pink',head,.9,'eyes');
  cube('Forehead gold star',[0,21,-3.6],[4,4,.15],'star',head);
  spike('Golden beak',[0,15,-3.6],[0,13,-8],2.8,'beak',head);
  for(const s of[-1,1]){
   let ear=group('Ear '+s,[s*5,20,0],head);beam('Wide swept ear',[s*5,20,0],[s*11,23,0],3.8,1.8,'star',ear);spike('Ear tip',[s*11,23,0],[s*14,22,-.2],2.4,'pink',ear);
   let arm=group('Arm '+s,[s*5,14,0],body),fore=group('Forearm '+s,[s*8,11,0],arm);
   beam('Star side limb',[s*5,14,0],[s*8,11,0],3,3,'star',arm);spike('Side limb point',[s*8,11,0],[s*11,10,-.4],2,'pink',fore);
   for(let i=0;i<2;i++){let name='Ribbon '+s+' '+i,g=group(name,[s*(3+i*2),11-i,1],body);beam('Trailing ribbon',[s*(3+i*2),11-i,1],[s*(5+i*2),6-i*2,2],.8,.8,'pink',g);cube('Heart tassel',[s*(5+i*2),4-i*2,2],[3,3.5,.5],'heart',g);}
  }
  bevel('Small floating body',[0,10,1],[7,6,5],'star',body,.6);
 } else if(act===2){
  bevel('Round floating helmet',[0,23,-.5],[13,13,10],'pink',head,1.5,'mask2');
  for(const s of[-1,1]){
   cube('Helmet dark rim',[s*5.8,23,0],[1.2,9.5,7.4],'blue',head,[0,0,s*-12]);
   let ear=group('Ear '+s,[s*5,28,0],head);beam('Antenna stalk',[s*5,28,0],[s*10,29,0],1,1,'pink',ear);
   bevel('Round antenna end',[s*12,28,0],[5,5,2],'blue',ear,.9);cube('Antenna dark aperture',[s*12,28,-1.05],[2.4,2.4,.2],'dark',ear);
   let arm=group('Arm '+s,[s*6,17,0],body),fore=group('Forearm '+s,[s*10,13,-1],arm);
   bevel('Shoulder pivot',[s*6,17,0],[3,3,3],'metal',arm,.5);beam('Slim upper arm',[s*6,17,0],[s*10,13,-1],2.2,2.5,'pink',arm);
   beam('Open pincer forearm',[s*10,13,-1],[s*10,10,-4],3.3,3.8,'light',fore);
   for(const x of[-1,1])spike('Pincer '+s+' '+x,[s*10+x*1.2,10,-4],[s*10+x,8,-6],1.1,'pink',fore);
  }
  cube('Neck bearing',[0,16,0],[3,3,3],'metal',body);cube('Flexible waist',[0,12,1],[5,8,5],'blue',body,[0,0,-8]);
  bevel('Floating lower pod',[0,7,2],[13,10,10],'pink',body,1.5,'blue_star');
  for(const s of[-1,1]){let g=group('Leg '+s,[s*4,5,2],body);spike('Pod spike '+s,[s*4,5,2],[s*8,1,3],1.6,'pink',g);}
  spike('Pod bottom spike',[0,3,2],[0,0,2],1.6,'metal',body);
 } else {
  const four=act===4,shoulder=four?15:10,sy=four?40:29,elbow=four?30:21,wrist=four?20:12;
  if(four){
   bevel('Broad chest armor',[0,37,1],[25,15,13],'pink',body,2);
   bevel('Compact helmet',[0,38,-8],[9,12,7],'pink',head,1,'mask4');
   cube('Left breastplate',[-7.5,37,-6],[6.5,10,1.2],'light',body,[0,0,-7]);cube('Right breastplate',[7.5,37,-6],[6.5,10,1.2],'light',body,[0,0,7]);
   cube('Dark abdominal core',[0,25,1],[16,14,10],'shadow',body);
   for(let strip=0;strip<12;strip++){const a=strip*Math.PI/6,x=Math.cos(a)*11,z=Math.sin(a)*7.7+1;let parent=body;
    for(let row=0;row<4;row++){let y=30-row*5,name='Skirt '+strip+' '+row,g=group(name,[x,y+1,z],parent);cube('Linked armor disc',[x,y-1.2,z],[4.1,5.5,.65],'disc',g,[0,90-strip*30,0]);parent=g;}
   }
  }else{
   bevel('Large tapered hood',[0,30,1],[17,18,13],'pink',head,2,'mask3');
   cube('Tall central crest',[0,39,1],[1.2,7,7.5],'blue',head,[-15,0,0]);
   for(const s of[-1,1]){let g=group('Ear '+s,[s*6,35,-1],head);beam('Curved feeler',[s*6,35,-1],[s*10,33,-3],.8,.8,'pink',g);bevel('Feeler ball',[s*10,33,-3],[1.8,1.8,1.8],'metal',g,.3);}
   bevel('Narrow abdominal plate',[0,19,1],[6,10,6],'blue',body,1.2);
   for(const s of[-1,1]){beam('Waist piston '+s,[s*6,25,1],[s*4,14,1],1.2,1.2,'metal',body);bevel('Hip bearing '+s,[s*4,14,1],[3.5,3.5,3.5],'pink',body,.6);}
  }
  for(const s of[-1,1]){
   let arm=group('Arm '+s,[s*shoulder,sy,1],body),fore=group('Forearm '+s,[s*(shoulder+2),elbow,0],arm),hand=group('Hand '+s,[s*(shoulder+2),wrist,-1],fore);
   bevel('Shoulder shell '+s,[s*shoulder,sy,1],four?[10,10,11]:[7,7,7],four?'star':'blue_star',arm,1.3);
   beam('Upper arm armor '+s,[s*shoulder,sy-2,1],[s*(shoulder+2),elbow,0],four?5.3:3.2,four?6:3.2,'pink',arm);
   bevel('Elbow bearing '+s,[s*(shoulder+2),elbow,0],four?[4.8,4.8,4.8]:[3,3,3],'metal',fore,.5);
   beam('Forearm armor '+s,[s*(shoulder+2),elbow-1,0],[s*(shoulder+2),wrist,-1],four?6:3.4,four?6.4:3.8,'pink',fore);
   if(four){cube('Closed blue fist '+s,[s*(shoulder+2),wrist-2,-1.4],[5.4,4.5,4.6],'blue_star',hand);cube('Thumb '+s,[s*(shoulder-.7),wrist-1.8,-2.1],[1.4,2.5,2],'blue',hand);
    for(let f=0;f<3;f++)cube('Knuckle '+s+' '+f,[s*(shoulder+2)+(f-1)*1.6,wrist-2,-3.8],[1.3,2.3,.6],'blue',hand);
   }else {cube('Flat claw palm '+s,[s*(shoulder+2),wrist-1,-1.3],[4,2,4],'blue',hand);for(let f=-1;f<=1;f++)spike('Finger needle '+s+f,[s*(shoulder+2)+f*1.4,wrist-1,-2],[s*(shoulder+2)+f*1.4,wrist-6,-2.5],.5,'metal',hand);}
   let hip=four?7:4,hy=four?24:15,knee=four?13:8,leg=group('Leg '+s,[s*hip,hy,1],rootBone),shin=group('Shin '+s,[s*(hip+1),knee,1],leg);
   beam('Upper leg '+s,[s*hip,hy,1],[s*(hip+1),knee,1],four?6:3.6,four?7:3.7,'pink',leg);
   bevel('Knee pivot '+s,[s*(hip+1),knee,1],four?[6,5,6]:[3.6,3.6,3.6],'metal',shin,.7);
   beam('Lower leg '+s,[s*(hip+1),knee-1,1],[s*(hip+1),3,0],four?6:3.6,four?6:3.5,'star',shin);
   cube('Foot '+s,[s*(hip+1),1.7,-1.7],four?[6.5,3.4,9]:[5,2.4,5],'blue',shin);
   if(!four)for(let f=-1;f<=1;f++)spike('Toe needle '+s+f,[s*(hip+1)+f*1.5,1,-3],[s*(hip+1)+f*1.5,-1,-5],.5,'metal',shin);
  }
 }
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 function clip(name,length,loop,tracks){const animators={};for(const[bone,track]of Object.entries(tracks)){const g=groups.find(g=>g.name===bone);if(!g)continue;const keyframes=[];
  for(const t of Array.isArray(track)?track:[track])for(let i=0;i<=48;i++){const value=t.fn(i/48);keyframes.push({uuid:uid(id+'/'+name+'/'+bone+'/'+t.channel+'/'+i),channel:t.channel,time:+(length*i/48).toFixed(5),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((k,j)=>[k,''+(+value[j].toFixed(4))]))]});}
  animators[g.uuid]={name:bone,type:'bone',keyframes};}animations.push({uuid:uid(id+'/animation/'+name),name:'animation.'+id+'.'+name,length,loop:loop?'loop':'once',override:false,animators});
 }
 const idle={Root:P(p=>[0,.55*sin(p),0]),Body:R(p=>[1.1*sin(p),0,.6*sin(p-.15)]),Head:R(p=>[-.8*sin(p),2*sin(p*.5),0])};
 for(const s of[-1,1]){idle['Arm '+s]=R(p=>[act===4?8+2*sin(p):2*sin(p),0,s*(act===4?8:3)]);idle['Forearm '+s]=R(p=>[act===4?12:4,0,0]);idle['Ear '+s]=R(p=>[2*sin(p-.2),s*3*sin(p),0]);for(let j=0;j<2;j++)idle['Ribbon '+s+' '+j]=R(p=>[3*sin(p-j*.15),0,s*4*sin(p-j*.1)]);}
 for(let s=0;s<12;s++)for(let r=0;r<4;r++)idle['Skirt '+s+' '+r]=R(p=>[1.2*sin(p-r*.08+s*.07),0,.8*sin(p-r*.12)]);
 clip('idle',3.6,true,idle);
 const follow={Body:R(p=>[-9+1.5*sin(p),0,0]),Head:R(p=>[7,0,0]),'Arm -1':R(p=>[-16,0,-10]),'Arm 1':R(p=>[-16,0,10]),'Leg -1':R(p=>[12+8*sin(p),0,0]),'Leg 1':R(p=>[-8-8*sin(p),0,0]),'Shin -1':R(p=>[-16,0,0]),'Shin 1':R(p=>[-16,0,0])};
 for(let s=0;s<12;s++)for(let r=0;r<4;r++)follow['Skirt '+s+' '+r]=R(p=>[-2-r*.6+2*sin(p-r*.12),0,0]);clip('follow',1.1,true,follow);
 const envelope=p=>smooth(p/.2)*(1-smooth((p-.68)/.32));
 clip('summon',1.6,false,{Root:P(p=>[0,-5*(1-smooth(p)),3*(1-smooth(p))]),Body:R(p=>[-18*(1-smooth(p)),12*(1-smooth(p)),0]),Head:R(p=>[15*(1-smooth(p)),0,0]),'Arm -1':R(p=>[42*(1-smooth(p)),0,-20*envelope(p)]),'Arm 1':R(p=>[42*(1-smooth(p)),0,20*envelope(p)])});
 clip('dismiss',1.2,false,{Root:P(p=>[0,-6*smooth(p),3*smooth(p)]),Body:R(p=>[-20*smooth(p),0,0]),'Arm -1':R(p=>[35*smooth(p),0,-8]),'Arm 1':R(p=>[35*smooth(p),0,8])});
 for(const name of['shoot','shotgun','hole','wormhole']){const snap=p=>curve(p,[[0,0],[.22,1],[.36,.75],[.6,.9],[1,0]]);clip(name,name==='shoot'?.5:name==='wormhole'?.9:.7,false,{Body:R(p=>[-4*snap(p),-14*snap(p),0]),Head:R(p=>[2*snap(p),12*snap(p),0]),'Arm -1':R(p=>[85*snap(p),-8*snap(p),-7*snap(p)]),'Forearm -1':R(p=>[12*snap(p),0,0]),'Arm 1':R(p=>[30*snap(p),0,15*snap(p)]),Root:P(p=>[0,0,.65*Math.sin(Math.PI*p)])});}
 clip('glide',1.5,true,{Body:R(p=>[-20,0,0]),Head:R(p=>[15,0,0]),Root:P(p=>[0,-2,0]),'Arm -1':R(p=>[32,0,-18]),'Arm 1':R(p=>[32,0,18]),'Leg -1':R(p=>[30,0,0]),'Leg 1':R(p=>[-15,0,0])});
 clip('burst',1,false,{Body:R(p=>[8*envelope(p),0,0]),'Arm -1':R(p=>[60*envelope(p),0,-55*envelope(p)]),'Arm 1':R(p=>[60*envelope(p),0,55*envelope(p)]),Root:P(p=>[0,1.4*envelope(p),0])});
 clip('slip',1,false,{Body:R(p=>[-25*envelope(p),40*envelope(p),0]),Root:P(p=>[0,-6*envelope(p),0]),'Arm -1':R(p=>[35*envelope(p),0,-18*envelope(p)]),'Arm 1':R(p=>[35*envelope(p),0,18*envelope(p)])});
 clip('dodge',.6,false,{Body:R(p=>[-12*envelope(p),-35*envelope(p),-14*envelope(p)]),Root:P(p=>[-5*envelope(p),-1*envelope(p),0]),'Arm -1':R(p=>[60*envelope(p),0,-15*envelope(p)]),'Arm 1':R(p=>[65*envelope(p),0,15*envelope(p)])});
 const barrage={Body:R(p=>[-8*envelope(p),9*sin(p*8)*envelope(p),0]),Root:P(p=>[0,0,-2*envelope(p)]),Head:R(p=>[4,0,0])};
 for(const s of[-1,1]){barrage['Arm '+s]=R(p=>[(70+24*sin(p*8+(s===1?.5:0)))*envelope(p),s*8,s*7]);barrage['Forearm '+s]=R(p=>[(22-20*sin(p*8+(s===1?.5:0)))*envelope(p),0,0]);barrage['Hand '+s]=P(p=>[0,0,-2*Math.max(0,sin(p*8+(s===1?.5:0)))*envelope(p)]);}
 clip('barrage',2,false,barrage);
 const hit=p=>curve(p,[[0,0],[.15,.3],[.24,1],[.29,.25],[.4,1],[.45,.25],[.56,1],[.61,.25],[.72,1],[.79,-.7],[.88,1.5],[1,0]]);
 clip('beatdown',2.5,false,{Body:R(p=>[-8*envelope(p),22*hit(p),-4*hit(p)]),Root:P(p=>[0,-1.1*envelope(p),-2*Math.max(0,hit(p))]),Head:R(p=>[4,-12*hit(p),0]),'Arm -1':R(p=>[60*envelope(p)+34*hit(p),-15*hit(p),-10]),'Forearm -1':R(p=>[35*envelope(p)-25*Math.max(0,hit(p)),0,0]),'Arm 1':R(p=>[70*envelope(p)-25*hit(p),20*hit(p),12]),'Forearm 1':R(p=>[25*envelope(p)+20*hit(p),0,0])});
 clip('infinite',2.5,false,{Body:R(p=>[-5-10*smooth(p),-20*smooth(p),0]),Head:R(p=>[5,16*smooth(p),0]),Root:P(p=>[0,1.5*smooth(p),0]),'Arm -1':R(p=>[35+55*smooth(p),-8,0]),'Forearm -1':R(p=>[50*(1-smooth(p)),0,0]),'Arm 1':R(p=>[50,10,20]),'Forearm 1':R(p=>[35,0,0])});
 for(const element of elements)if(['Heart tassel','Linked armor disc'].includes(element.name))
  for(const side of ['east','west','up','down'])element.faces[side].texture=null;
 return m;
}
async function icon(id,act=4){const c=canvas(32),pink='e995c4',gold='ffe9a0',dark='673c70',white='fff4d7',blue='95b4e6';
 const ring=(r,w,col,cx=16,cy=16)=>{for(let y=0;y<32;y++)for(let x=0;x<32;x++){const d=Math.hypot(x-cx,y-cy);if(d>=r-w&&d<=r)c.dot(x,y,col);}};
 if(id.startsWith('act_')||id==='tusk'){c.poly([[7,8],[10,3],[22,3],[25,8],[24,25],[16,29],[8,25]],dark);c.rect(9,7,14,17,pink);c.star(16,10,5,gold);c.rect(11,18,3,3,white);c.rect(19,18,3,3,white);for(let i=0;i<act;i++)c.rect(8+i*5,28,3,2,gold);}
 else if(id==='nail_shot'||id==='nail_shotgun'||id==='pursuing_hole'){for(let j=0;j<(id==='nail_shotgun'?3:1);j++){let o=j*6;c.poly([[4+o,26],[9+o,11],[17+o,4],[15+o,17]],gold);c.line(7+o,24,15+o,7,2,white);}if(id==='pursuing_hole'){ring(11,2,pink);ring(7,1,gold);}}
 else if(id==='stand_barrage'||id==='golden_beatdown'){c.rect(10,8,15,14,pink);c.rect(6,14,5,8,blue);c.rect(10,6,3,4,white);c.rect(15,6,3,4,white);c.rect(20,6,3,4,white);c.star(17,17,5,gold);for(let i=0;i<3;i++)c.line(2,10+i*6,7,8+i*6,2,gold);}
 else {ring(12,2,pink);ring(8,1,gold);for(let i=0;i<36;i++){let a=i*.29,r=2+i*.23;c.dot(16+Math.cos(a)*r,16+Math.sin(a)*r,white);}if(id==='infinite_rotation')c.star(16,16,6,gold);else if(id==='spin_glide')c.line(2,27,27,5,3,blue);else if(id==='wormhole_shot')c.line(7,24,25,8,3,gold);else if(id==='dimensional_dodge')c.poly([[9,10],[16,7],[19,21],[14,27],[10,20]],blue);else if(id==='spiral_burst')for(let i=0;i<8;i++){let a=i*Math.PI/4;c.line(16+Math.cos(a)*11,16+Math.sin(a)*11,16+Math.cos(a)*15,16+Math.sin(a)*15,2,gold);}}
 return sharp(c.data,{raw:{width:32,height:32,channels:4}}).png().toBuffer();
}
(async()=>{for(let act=1;act<=4;act++){const id='tusk_act_'+act,file=path.join(root,'deliverables/tusk/'+id+'.bbmodel');
 const existing=fs.existsSync(file),rebuild=!existing||process.argv.includes('--refine');
 let m=existing?JSON.parse(fs.readFileSync(file)):model(act);if(rebuild)m=refinement.refine(m,act);
 const png=rebuild?await refinement.atlas():m.textures[0]?.source?Buffer.from(m.textures[0].source.split(',')[1],'base64'):await atlas(act);
 const {width,height}=await sharp(png).metadata();
 m.textures=[{uuid:uid(id+'/texture'),name:id+'.png',id:'0',width,height,uv_width:width,uv_height:height,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 assert(m.elements.length<180);const rig={sourceHash:crypto.createHash('sha256').update(JSON.stringify(m)).digest('hex'),nodes:refinement.bake(m,bake),animations:clipsFrom(m)};
 write('deliverables/tusk/'+id+'.bbmodel',JSON.stringify(m));write('src/main/resources/assets/kazimod/models/entities/'+id+'.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/'+id+'.png',png);
 console.log(id+': '+m.elements.length+' low-poly parts, '+m.groups.length+' bones, '+m.animations.length+' animations');
 write('src/main/resources/assets/kazimod/textures/abilities/tusk/act_'+act+'.png',await icon('act_'+act,act));
 }
 for(const id of['tusk','nail_shot','nail_shotgun','spin_glide','pursuing_hole','spiral_burst','wormhole_slip','wormhole_shot','dimensional_dodge','stand_barrage','golden_beatdown','infinite_rotation'])write('src/main/resources/assets/kazimod/textures/abilities/tusk/'+id+'.png',await icon(id));
})().catch(error=>{console.error(error);process.exit(1);});
