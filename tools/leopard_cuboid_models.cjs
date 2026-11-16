const crypto = require('crypto');
const assert = require('assert/strict');
const uid = s => { const h=crypto.createHash('md5').update('cuboid-leopard/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`; };
const clamp = (v,a,b) => Math.max(a,Math.min(b,v));
const smooth = t => (t=clamp(t,0,1))*t*t*(t*(t*6-15)+10);
const wave = t => Math.sin(t*Math.PI*2);
const curve = (t,points) => {for(let i=1;i<points.length;i++)if(t<=points[i][0]){const [a,x]=points[i-1],[b,y]=points[i];return x+(y-x)*smooth((t-a)/(b-a));}return points[points.length-1][1];};
const R = fn => ({channel:'rotation',fn});
const P = fn => ({channel:'position',fn});

function create(kind) {
 const id='leopard_'+kind,full=kind==='full',awake=kind==='awakened';
 const elements=[],groups=[],outliner=[],animations=[],limbs=[];
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Cuboid Leopard '+kind,resolution:{width:256,height:256},elements,groups,outliner,textures:[],animations};
 function group(name,origin,parent){const g={name,uuid:uid(id+'/'+name),origin,rotation:[0,0,0],visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;}
 function cube(name,center,size,mat,parent,front=null,rotation=[0,0,0],origin=center){
  assert(size.every(v=>v>0));
  const from=center.map((v,i)=>v-size[i]/2),to=center.map((v,i)=>v+size[i]/2),faces={};
  for(const side of['north','south','east','west','up','down']){
   const dims=['north','south'].includes(side)?[size[0],size[1]]:['east','west'].includes(side)?[size[2],size[1]]:[size[0],size[2]];
   const tile=side==='north'&&front!==null?(typeof front==='number'?front:front.mat):mat,u=tile%4*64,v=(tile>>2)*64;
   let uv=[u+1,v+1,u+1+Math.min(62,dims[0]*4),v+1+Math.min(62,dims[1]*4)];
   if(side==='north'&&front!==null){
    if(typeof front==='number')uv=[u+1,v+1,u+63,v+63];
    else uv=[u+1+(from[0]-front.left)/(front.right-front.left)*62,v+1+(front.hi-to[1])/(front.hi-front.lo)*62,u+1+(to[0]-front.left)/(front.right-front.left)*62,v+1+(front.hi-from[1])/(front.hi-front.lo)*62];
   }
   faces[side]={uv,texture:0};
  }
  const e={name,type:'cube',uuid:uid(id+'/cube/'+name),from,to,origin,rotation,box_uv:false,faces,visibility:true,export:true};elements.push(e);parent.children.push(e.uuid);return e;
 }
 function beam(name,a,b,width,depth,mat,parent){
  const d=b.map((v,i)=>v-a[i]),length=Math.hypot(...d),rx=Math.atan2(d[2],Math.hypot(d[0],d[1]))*180/Math.PI,rz=-Math.atan2(d[0],d[1])*180/Math.PI;
  return cube(name,[a[0],a[1]+length/2,a[2]],[width,length+.65,depth],mat,parent,null,[rx,0,rz],a);
 }
 const hip=full?14.3:awake?23.5:20.3,root=group('Root',[0,0,0]),body=group('Body',[0,hip,0],root),chest=group('Chest',[0,full?15:hip+3,full?-7:0],body);
 function head(parent,y,z,w,h){
  const g=group('Head',[0,y-h*.4,z+1],parent),jaw=group('Jaw',[0,y-h*.32,z+.4],g);
  cube('Square feline head',[0,y,z],[w,h,w*.82],0,g,2);
  cube('White muzzle',[0,y-h*.25,z-w*.49],[w*.73,h*.29,w*.38],3,g,3);
  cube('Lower jaw',[0,y-h*.5,z-w*.36],[w*.66,h*.18,w*.53],1,jaw);
  cube('Recessed mouth',[0,y-h*.37,z-w*.43],[w*.61,h*.11,w*.37],6,jaw);
  cube('Black nose',[0,y-h*.13,z-w*.7],[w*.23,h*.09,.55],12,g);
  for(const s of[-1,1]){
   const ear=group('Ear '+s,[s*w*.38,y+h*.4,z+.3],g);
   cube('Square ear '+s,[s*w*.39,y+h*(full?.55:.58),z+.35],[w*.27,h*.28,w*.21],0,ear,5);
   cube('Upper fang '+s,[s*w*.25,y-h*(full?.38:.43),z-w*.58],[.6,full?.65:1.25,.65],13,g);
   if(!full)for(let i=0;i<3;i++)cube('Incisor '+s+' '+i,[s*(.28+i*.48),y-h*.37,z-w*.65],[.34,.55,.4],13,g);
  }
 }
 function leg(s,front){
  const upper=(front?'Arm ':'Leg ')+s,lower=(front?'Forearm ':'Shin ')+s,foot=(front?'Hand ':'Foot ')+s;
  const x=s*(full?4.7:3.7),z=full?(front?-9:10):.7,ankle=full?1.6:1.7;
  const a=full?6.6:awake?11.7:10,b=hip-ankle-a;
  const top=group(upper,[x,hip,z],front?chest:body),mid=group(lower,[x,hip-a,z],top),paw=group(foot,[x,ankle,z],mid);
  cube('Upper '+upper,[x,hip-a/2,z],[full?3.7:awake?5.7:6.7,a+1.3,full?4.6:6.3],0,top);
  cube('Lower '+upper,[x,hip-a-b/2,z],[full?2.8:4.3,b+1.1,full?3.2:4.7],9,mid);
  cube('Square paw '+upper,[x,1.2,z-(full?1.4:2.1)],[full?4.2:5.9,2.4,full?5.4:7.4],0,paw,1);
  for(let i=0;i<3;i++)cube('Paw claw '+upper+' '+i,[x+(i-1)*(full?1.15:1.65),.65,z-(full?4.1:5.8)],[full?.65:.8,.8,1.35],full?13:4,paw);
  limbs.push({upper,lower,foot,a,b,hip,ankle,front,s});
 }
 if(full){
  cube('Long square torso',[0,14.6,1],[11.6,9.5,25.5],0,body);
  cube('Shoulder block',[0,15.2,-8],[12.2,10.4,8.8],0,chest);
  cube('Haunch block',[0,14.6,10],[12,9.8,7],0,body);
  cube('White belly',[0,10.4,1],[8.5,2.4,18.5],1,body);
  cube('Neck',[0,16.2,-13],[8.4,8.3,7],0,chest);
  head(chest,18.9,-17.1,8.6,7.4);
  for(const s of[-1,1]){leg(s,true);leg(s,false);}
 }else{
  const shoulder=awake?36.5:33.8,width=awake?19.7:23;
  cube('Square hips',[0,hip+.8,0],[11.9,5.5,8.5],0,body,1);
  const chart={mat:8,left:-width/2,right:width/2,lo:hip+1.5,hi:shoulder+3.5};
  cube('Abdomen',[0,(hip+1.5+shoulder-2)/2,0],[awake?10.3:12.3,shoulder-2-(hip+1.5),8.4],0,chest,chart);
  cube('Broad square chest',[0,shoulder,0],[width,7,10.4],0,chest,chart);
  cube('Trapezius',[0,shoulder+3.1,.8],[width*.66,2.4,8.4],0,chest);
  cube('Neck',[0,shoulder+5,.1],[6.5,5.5,6.8],0,chest);
  head(chest,shoulder+8,-.75,awake?8.1:8.8,7.8);
  for(const s of[-1,1]){
   const x=s*(width/2+1.9),top=group('Arm '+s,[x,shoulder+1,0],chest),elbow=group('Forearm '+s,[x,shoulder-8.5,0],top),hand=group('Hand '+s,[x,shoulder-17.4,0],elbow);
   hand.rotation[1]=180;
   cube('Upper arm '+s,[x,shoulder-3,0],[awake?5.8:6.8,10.7,awake?6.1:7.1],0,top);
   cube('Forearm '+s,[x,shoulder-12.8,0],[awake?4.9:5.6,10,5.5],9,elbow);
   cube('Square palm '+s,[x,shoulder-19,-.2],[5.1,4,3.7],0,hand,1);
   for(let f=0;f<4;f++){
    const X=x+(f-1.5)*1.2,Y=shoulder-20.5,first=group('Finger '+s+' '+f,[X,Y,-.3],hand),second=group('Finger tip '+s+' '+f,[X,Y-2.1,-.3],first);
    cube('First finger segment '+s+' '+f,[X,Y-1,-.3],[1.05,2.5,1.3],10,first);
    cube('Second finger segment '+s+' '+f,[X,Y-2.8,-.3],[.95,1.8,1.15],10,second);
    cube('Squared finger claw '+s+' '+f,[X,Y-3.9,-.55],[.65,1.25,.7],4,second);
   }
   const thumb=group('Thumb '+s,[x+s*2.3,shoulder-18.1,0],hand);
   cube('Thumb base '+s,[x+s*2.8,shoulder-18.9,0],[1.6,2.7,1.7],0,thumb,null,[0,0,s*28],[x+s*2.3,shoulder-18.1,0]);
   cube('Square thumb claw '+s,[x+s*3.4,shoulder-20.5,-.25],[.8,1.3,.8],4,thumb);
   leg(s,false);
  }
 }
 const tailPoints=full?[[0,15,13],[0,14,19],[1,12.6,25],[3.5,12.7,31],[5.5,14.7,36],[5,18,39],[2.5,20,40]]:[[0,hip+1.5,5],[1,hip,11],[3.5,hip-3.5,16],[8,hip-5,20],[13,hip-3.5,20.5],[17,hip,18.5],[17,hip+4,15.5],[14,hip+6,14.5]];
 let tailParent=body;
 for(let i=0;i<tailPoints.length-1;i++){
  const g=group('Tail '+i,tailPoints[i],tailParent),thickness=(full?2.3:2.7)-i*.14;
  beam('Square tail segment '+i,tailPoints[i],tailPoints[i+1],thickness,thickness,i===tailPoints.length-2?4:9,g);tailParent=g;
 }
 function clip(name,length,loop,tracks){
  const animators={},samples=loop?48:Math.max(32,Math.ceil(length*40));
  for(const [bone,list]of Object.entries(tracks)){
   const g=groups.find(g=>g.name===bone);assert(g,'Unknown bone '+bone);const keyframes=[];
   for(const track of Array.isArray(list)?list:[list])for(let i=0;i<=samples;i++){
    const value=track.fn(i/samples);assert(value.every(Number.isFinite));keyframes.push({uuid:uid(id+'/'+name+'/'+bone+'/'+track.channel+'/'+i),channel:track.channel,time:+(length*i/samples).toFixed(6),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((axis,j)=>[axis,String(+value[j].toFixed(5))]))]});
   }
   animators[g.uuid]={name:bone,type:'bone',keyframes};
  }
  animations.push({uuid:uid(id+'/'+name),name:'animation.'+id+'.'+name,length,loop:loop?'loop':'once',override:false,animators});
 }
 function solve(limb,forward,lift,bodyY){
  const down=limb.hip+bodyY-limb.ankle-lift,a=limb.a,b=limb.b;
  const distance=clamp(Math.hypot(forward,down),Math.abs(a-b)+.01,a+b-.025),phi=(full&&limb.front?1:-1)*Math.acos(clamp((distance*distance-a*a-b*b)/(2*a*b),-1,1));
  const theta=Math.atan2(forward,down)-Math.atan2(b*Math.sin(phi),a+b*Math.cos(phi));
  return [theta*180/Math.PI,phi*180/Math.PI,-(theta+phi)*180/Math.PI];
 }
 function stance(tracks,bodyY,footFor=()=>[0,0]){
  tracks.Body=P(t=>[0,bodyY(t),0]);
  for(const limb of limbs){const angles=t=>{const [f,h]=footFor(limb,t);return solve(limb,f,h,bodyY(t));};
   tracks[limb.upper]=R(t=>[angles(t)[0],0,0]);tracks[limb.lower]=R(t=>[angles(t)[1],0,0]);tracks[limb.foot]=R(t=>[angles(t)[2],0,0]);
  }
 }
 function follow(tracks,strength=1){
  for(let i=0;i<tailPoints.length-1;i++)tracks['Tail '+i]=R(t=>[strength*1.6*wave(t-i*.1),strength*(3.4-i*.15)*wave(t-i*.1),strength*.5*wave(t-i*.1)]);
 }
 function hands(tracks,curl){if(full)return;for(const s of[-1,1]){for(let i=0;i<4;i++){
  tracks['Finger '+s+' '+i]=R(t=>[curl(t)*(1-i*.05),0,0]);tracks['Finger tip '+s+' '+i]=R(t=>[curl(t)*1.5,0,0]);
 }tracks['Thumb '+s]=R(t=>[curl(t)*.25,-s*curl(t)*.25,0]);}}
 const idle={Chest:R(t=>[.55*wave(t),0,0]),Head:R(t=>[-.4*wave(t-.12),.8*wave(t),0]),Jaw:R(t=>[-.3-.35*wave(t-.12),0,0])};
 stance(idle,t=>-.7+.09*wave(t));follow(idle);hands(idle,t=>12+1.4*wave(t-.1));
 for(const s of[-1,1]){idle['Ear '+s]=R(t=>[1.1*wave(t-.2),s*.6*wave(t),0]);if(!full){idle['Arm '+s]=R(t=>[2+wave(t-.08*s),0,s*3]);idle['Forearm '+s]=R(t=>[6-wave(t-.15*s),0,0]);}}
 clip('idle',3.8,true,idle);
 function gait(name,running){
  const tracks={},bodyY=t=>full?(running?-1.5+.8*wave(t):- .75+.12*Math.cos(t*4*Math.PI)):(running?-1.5+.55*Math.cos(t*4*Math.PI):- .8+.15*Math.cos(t*4*Math.PI));
  stance(tracks,bodyY,(limb,t)=>{
   const phase=full?(running?(limb.front?.46:.02)+(limb.s>0?.09:0):(limb.front?.25:0)+(limb.s>0?.5:0)):(limb.s>0?.5:0);
   const p=((t+phase)%1+1)%1,duty=running?(full?.38:.43):.64,stride=running?(full?7:8.8):(full?4.6:4.8),height=running?(full?3.2:4.6):1.3;
   if(p<duty)return [stride*(.5-p/duty),0];
   const q=(p-duty)/(1-duty);return [stride*(-.5+smooth(q)),height*Math.pow(Math.sin(q*Math.PI),2)];
  });
  tracks.Chest=R(t=>[full?(running?3*wave(t):.6*wave(t)):running?-9:-2,full?.6*wave(t):2.6*wave(t),full?0:1.1*wave(t)]);
  tracks.Head=R(t=>[full?(running?-2*wave(t):0):running?6:1,full?-1.2*wave(t):-1.7*wave(t),0]);
  if(!full)for(const s of[-1,1]){
   const phase=s<0?.5:0;tracks['Arm '+s]=R(t=>[(running?40:18)*wave(t+phase),s*(running?3:1),s*(running?7:4)]);
   tracks['Forearm '+s]=R(t=>[running?45+22*wave(t+phase-.15):13+8*wave(t+phase-.15),0,0]);
   tracks['Hand '+s]=R(t=>[-8-4*wave(t+phase-.25),0,0]);
  }
  follow(tracks,running?1.25:.8);hands(tracks,t=>running?38:16);
  clip(name,running?.66:1.15,true,tracks);
 }
 gait('walk',false);gait('run',true);
 const crouch={Chest:R(t=>[full?4:-9,0,0]),Head:R(t=>[full?-4:8,0,0])};stance(crouch,t=>full?-2.7:-3.3);follow(crouch,.6);hands(crouch,t=>18);
 if(!full)for(const s of[-1,1]){crouch['Arm '+s]=R(t=>[14,0,s*7]);crouch['Forearm '+s]=R(t=>[21,0,0]);}
 clip('crouch',1,true,crouch);
 const jump={Body:[P(t=>[0,0,0]),R(t=>[full?-7:-4,0,0])],Chest:R(t=>[full?-3:-12,0,0]),Head:R(t=>[full?7:12,0,0]),Jaw:R(t=>[-6,0,0])};
 for(const limb of limbs){jump[limb.upper]=R(t=>[full?(limb.front?46:-28):35,0,0]);jump[limb.lower]=R(t=>[full?(limb.front?18:-38):-55,0,0]);jump[limb.foot]=R(t=>[full?-10:20,0,0]);}
 if(!full)for(const s of[-1,1]){jump['Arm '+s]=R(t=>[55,s*8,s*13]);jump['Forearm '+s]=R(t=>[25,0,0]);}
 follow(jump,.8);hands(jump,t=>24);clip('jump',1,true,jump);
 const fall={Body:R(t=>[full?4:0,0,0]),Chest:R(t=>[full?0:-4,0,0]),Head:R(t=>[-3,0,0])};
 for(const limb of limbs){fall[limb.upper]=R(t=>[full?(limb.front?12:-12):10,0,0]);fall[limb.lower]=R(t=>[full?(limb.front?8:-18):-17,0,0]);fall[limb.foot]=R(t=>[5,0,0]);}
 if(!full)for(const s of[-1,1]){fall['Arm '+s]=R(t=>[20,s*10,s*22]);fall['Forearm '+s]=R(t=>[17,0,0]);}
 follow(fall,.6);hands(fall,t=>9);clip('fall',1,true,fall);
 const pounce={Body:R(t=>[curve(t,[[0,0],[.15,-10],[.5,-13],[1,-8]]),0,0]),Chest:R(t=>[full?-2:-8,0,0]),Head:R(t=>[full?9:15,0,0]),Jaw:R(t=>[curve(t,[[0,-4],[.2,-21],[.75,-15],[1,-9]]),0,0])};
 for(const limb of limbs){pounce[limb.upper]=R(t=>[full?(limb.front?64:-42):35,0,0]);pounce[limb.lower]=R(t=>[full?(limb.front?5:-35):-65,0,0]);pounce[limb.foot]=R(t=>[0,0,0]);}
 if(!full)for(const s of[-1,1]){pounce['Arm '+s]=R(t=>[curve(t,[[0,38],[.22,80],[1,70]]),s*10,s*9]);pounce['Forearm '+s]=R(t=>[curve(t,[[0,45],[.3,7],[1,12]]),0,0]);}
 follow(pounce,.8);hands(pounce,t=>4);clip('pounce',.6,false,pounce);
 const land={Chest:R(t=>[(full?5:-7)*Math.sin(Math.PI*t),0,0]),Head:R(t=>[(full?-4:6)*Math.sin(Math.PI*t),0,0])};
 stance(land,t=>-.7-(full?2:3.1)*Math.pow(Math.sin(Math.PI*t),2));hands(land,t=>8+13*Math.sin(Math.PI*t));
 if(!full)for(const s of[-1,1]){land['Arm '+s]=R(t=>[12+18*Math.sin(Math.PI*t),0,s*8]);land['Forearm '+s]=R(t=>[22,0,0]);}
 clip('land',.42,false,land);
 for(const side of[-1,1]){
  const e=t=>smooth(t/.12)*(1-smooth((t-.62)/.38));
  const strike={Chest:R(t=>[curve(t,[[0,0],[.25,5],[.48,-8],[.7,-3],[1,0]]),side*curve(t,[[0,0],[.25,-20],[.5,25],[.7,14],[1,0]]),-side*5*e(t)]),Head:R(t=>[-3*e(t),-side*7*e(t),0]),Jaw:R(t=>[-17*e(t),0,0])};
  strike['Arm '+side]=R(t=>[curve(t,[[0,0],[.23,-22],[.45,85],[.68,42],[1,0]]),side*curve(t,[[0,0],[.24,-24],[.52,34],[1,0]]),side*curve(t,[[0,0],[.22,24],[.53,5],[1,0]])]);
  strike['Forearm '+side]=R(t=>[curve(t,[[0,0],[.25,60],[.47,7],[.7,28],[1,0]]),0,0]);
  strike['Hand '+side]=R(t=>[-10*e(t),side*12*e(t),0]);
  if(!full){strike['Arm '+(-side)]=R(t=>[22*e(t),-side*15*e(t),-side*9*e(t)]);strike['Forearm '+(-side)]=R(t=>[48*e(t),0,0]);}
  hands(strike,t=>3+18*(1-e(t)));clip(side<0?'slash_right':'slash_left',.55,false,strike);
 }
 const guard={Chest:R(t=>[full?3:4,0,0]),Head:R(t=>[3,0,0])};
 for(const s of[-1,1]){guard['Arm '+s]=R(t=>[full?30:65,s*33,s*13]);guard['Forearm '+s]=R(t=>[full?12:65,0,0]);guard['Hand '+s]=R(t=>[-12,0,0]);}
 hands(guard,t=>41);clip('block',1,true,guard);
 const ready={Jaw:R(t=>[-4,0,0])};if(!full)for(const s of[-1,1]){ready['Arm '+s]=R(t=>[12,0,s*8]);ready['Forearm '+s]=R(t=>[30,0,0]);}hands(ready,t=>5);clip('claw_ready',1,true,ready);
 const swim={Body:R(t=>[full?0:65,0,0]),Chest:R(t=>[full?0:10,0,0]),Head:R(t=>[full?0:-60,0,0])};
 for(const limb of limbs){swim[limb.upper]=R(t=>[20*wave(t+(limb.s>0?.5:0)),0,0]);swim[limb.lower]=R(t=>[-15,0,0]);}
 if(!full)for(const s of[-1,1]){swim['Arm '+s]=R(t=>[55+25*wave(t+(s>0?.5:0)),s*10,s*12]);swim['Forearm '+s]=R(t=>[25,0,0]);}follow(swim,1.2);hands(swim,t=>9);clip('swim',1.4,true,swim);
 const transform={Chest:R(t=>[curve(t,[[0,0],[.23,9],[.6,-12],[.8,-5],[1,0]]),0,0]),Head:R(t=>[curve(t,[[0,0],[.25,-8],[.62,13],[1,0]]),0,0]),Jaw:R(t=>[curve(t,[[0,0],[.38,-8],[.65,-29],[.85,-18],[1,0]]),0,0])};
 stance(transform,t=>-.7-2.1*Math.pow(Math.sin(Math.PI*t),2));
 if(!full)for(const s of[-1,1]){transform['Arm '+s]=R(t=>[curve(t,[[0,0],[.3,35],[.65,-12],[1,0]]),s*8*Math.sin(Math.PI*t),s*18*Math.sin(Math.PI*t)]);transform['Forearm '+s]=R(t=>[30*Math.sin(Math.PI*t),0,0]);}
 hands(transform,t=>curve(t,[[0,12],[.35,44],[.65,0],[1,12]]));follow(transform,.65);clip('transform',1.5,false,transform);
 return model;
}
module.exports={create};
