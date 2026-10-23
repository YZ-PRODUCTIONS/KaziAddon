const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const root=path.resolve(__dirname,'..');
const source=process.argv[2]||'C:/blockbench/merged_beast_agito.bbmodel';
const original=fs.readFileSync(source),model=JSON.parse(original),before=JSON.parse(original);
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const uid=s=>crypto.createHash('md5').update('agito-summon:'+s).digest('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/,'$1-$2-$3-$4-$5');
const groups=new Map(model.groups.map(g=>[g.uuid,g])),elements=new Map(model.elements.map(e=>[e.uuid,e]));
const B={root:'Agito',pelvis:'Pelvis',torso:'Torso',head:'Head',neck:'Neck',mane:'Shoulder mane',ra:'Right upper arm',rf:'Right forearm',rh:'Right hand',la:'Left upper arm',lf:'Left forearm',lh:'Left hand',rt:'Right thigh',rs:'Right lower leg',rfoot:'Right foot',lt:'Left thigh',ls:'Left lower leg',lfoot:'Left foot'};
const clamp=p=>Math.max(0,Math.min(1,p)),smooth=p=>{p=clamp(p);return p*p*(3-2*p);},sin=p=>Math.sin(p*2*Math.PI);
const R=fn=>({rotation:fn}),P=fn=>({position:fn}),constant=(x=0,y=0,z=0)=>R(()=>[x,y,z]);
function curve(p,keys){
  let i=1;while(i<keys.length-1 && keys[i][0]<p)i++;
  const a=keys[i-1],b=keys[i],h=b[0]-a[0],t=clamp((p-a[0])/h);
  const slope=j=>{if(j===0||j===keys.length-1)return 0;const h0=keys[j][0]-keys[j-1][0],h1=keys[j+1][0]-keys[j][0],d0=(keys[j][1]-keys[j-1][1])/h0,d1=(keys[j+1][1]-keys[j][1])/h1;return d0*d1<=0?0:3*(h0+h1)/((2*h1+h0)/d0+(h1+2*h0)/d1);};
  return(2*t*t*t-3*t*t+1)*a[1]+(t*t*t-2*t*t+t)*h*slope(i-1)+(-2*t*t*t+3*t*t)*b[1]+(t*t*t-t*t)*h*slope(i);
}
const clips=[...(model.animations||[])];
function clip(name,length,loop,tracks){
  const animators={};
  for(const [alias,channels]of Object.entries(tracks)){
    const bone=B[alias]||alias,g=model.groups.find(g=>g.name===bone);assert(g,'Missing bone '+bone);
    const keyframes=[];
    for(const [channel,fn]of Object.entries(channels))for(let i=0;i<=80;i++){
      const value=fn(i/80);assert(value.length===3 && value.every(Number.isFinite));
      keyframes.push({uuid:uid(name+bone+channel+i),channel,time:length*i/80,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((d,j)=>[d,+value[j].toFixed(5)]))]});
    }
    animators[g.uuid]={name:bone,type:'bone',keyframes};
  }
  const index=clips.findIndex(c=>c.name==='animation.agito.'+name);if(index>=0)clips.splice(index,1);
  clips.push({uuid:uid(name),name:'animation.agito.'+name,length,loop:loop?'loop':'once',snapping:20,animators});
}
const run={};
const hip=p=>curve(p,[[0,23],[.18,0],[.38,-25],[.5,-18],[.68,40],[.82,46],[1,23]]);
const knee=p=>curve(p,[[0,-9],[.2,-12],[.38,-8],[.53,-88],[.68,-98],[.85,-30],[1,-9]]);
for(const [side,phase]of [['Right',0],['Left',.5]]){
 const q=p=>(p+phase)%1,s=side==='Right'?-1:1;
 run[side+' thigh']=R(p=>[hip(q(p)),0,-s*2]);
 run[side+' lower leg']=R(p=>[knee(q(p)),0,0]);
 run[side+' foot']=R(p=>[-hip(q(p))-knee(q(p))-14*Math.max(0,sin(q(p)-.25)),0,0]);
 run[side+' upper arm']=R(p=>[-.8*hip(q(p))+10,s*5,s*7]);
 run[side+' forearm']=R(p=>[48+12*sin(q(p)+.1),0,0]);
}
run.root=P(p=>[0,-1.2-1.2*Math.cos(p*4*Math.PI),0]);
run.torso=R(p=>[-11-1.2*sin(p*2),7*sin(p),2*sin(p)]);
run.pelvis=R(p=>[0,-4*sin(p),1.5*sin(p)]);
run.head=R(p=>[8+sin(p*2),-4*sin(p),-sin(p)]);
run.mane=R(p=>[1.2*sin(p*2-.1),0,sin(p-.1)]);
for(let i=1;i<=7;i++)run['Serpent tail '+i]=R(p=>[1.2*sin(p*2-i*.08),2*sin(p-i*.08),0]);
clip('run',.76,true,run);
const rise=p=>1-smooth((p-.36)/.64);
clip('summon',4,false,{root:P(p=>[0,-8*rise(p),0]),torso:R(p=>[-26*rise(p),-10*rise(p),0]),head:R(p=>[18*rise(p),10*rise(p),0]),ra:R(p=>[34*rise(p),-15*rise(p),-38*rise(p)]),rf:R(p=>[42*rise(p),0,0]),la:R(p=>[-15*rise(p),12*rise(p),35*rise(p)]),lf:R(p=>[30*rise(p),0,0]),rt:R(p=>[40*rise(p),0,-6*rise(p)]),rs:R(p=>[-65*rise(p),0,0]),rfoot:R(p=>[25*rise(p),0,0]),lt:R(p=>[28*rise(p),0,6*rise(p)]),ls:R(p=>[-48*rise(p),0,0]),lfoot:R(p=>[20*rise(p),0,0])});
const dismiss=JSON.parse(JSON.stringify(clips.find(c=>c.name==='animation.agito.summon')));
dismiss.name='animation.agito.dismiss';dismiss.uuid=uid('dismiss');dismiss.length=3;
for(const a of Object.values(dismiss.animators)){
 for(const k of a.keyframes){k.uuid=uid('dismiss'+k.uuid);k.time=3*(1-k.time/4);}a.keyframes.sort((a,b)=>a.time-b.time);
}clips.push(dismiss);
const guard=p=>smooth(p/.13)*(1-smooth((p-.72)/.28));
for(const right of [true,false]){
 const sign=right?-1:1,arm=right?'ra':'la',forearm=right?'rf':'lf',other=right?'la':'ra',otherForearm=right?'lf':'rf';
 const punch=p=>curve(p,[[0,0],[.25,-.6],[.36,-.3],[.46,1],[.57,.85],[.75,.25],[1,0]]);
 const lift=p=>curve(p,[[0,0],[.23,.7],[.38,1],[.58,1],[.78,.65],[1,0]]);
 const tracks={torso:R(p=>[-12*guard(p)-8*Math.max(0,punch(p)),sign*27*punch(p),-sign*5*punch(p)]),pelvis:R(p=>[-3*guard(p),sign*9*punch(Math.min(1,p+.05)),0]),head:R(p=>[12*guard(p),-sign*15*punch(p),0]),root:P(p=>[0,-3*guard(p),-4*Math.max(0,punch(p))]),rt:R(p=>[15*guard(p),0,0]),rs:R(p=>[-23*guard(p),0,0]),rfoot:R(p=>[8*guard(p),0,0]),lt:R(p=>[10*guard(p),0,0]),ls:R(p=>[-18*guard(p),0,0]),lfoot:R(p=>[8*guard(p),0,0])};
 tracks[arm]=R(p=>[78*lift(p)+18*punch(p),sign*18*lift(p),sign*8*lift(p)]);
 tracks[forearm]=R(p=>[50*lift(p)-45*Math.max(0,punch(p)),0,0]);
 tracks[other]=R(p=>[30*guard(p),-sign*12*guard(p),-sign*8*guard(p)]);
 tracks[otherForearm]=R(p=>[65*guard(p),0,0]);
 tracks['Serpent tail 3']=R(p=>[0,-sign*5*punch(Math.max(0,p-.07)),0]);
 clip(right?'punch_right':'punch_left',1.3,false,tracks);
}
const punches=p=>Math.cos((p*44-10)*Math.PI/6)*smooth((p*44-5)/4)*(1-smooth((p*44-34)/8));
clip('punch_barrage',2.2,false,{torso:R(p=>[-17*guard(p),18*punches(p+.01),4*punches(p)]),pelvis:R(p=>[-3*guard(p),6*punches(p+.035),0]),head:R(p=>[12*guard(p),-10*punches(p),0]),ra:R(p=>[(78+20*punches(p))*guard(p),-12*guard(p),-7*guard(p)]),rf:R(p=>[(48-43*punches(p))*guard(p),0,0]),la:R(p=>[(78-20*punches(p))*guard(p),12*guard(p),7*guard(p)]),lf:R(p=>[(48+43*punches(p))*guard(p),0,0]),root:P(p=>[0,-3*guard(p),-3*guard(p)]),rt:R(p=>[15*guard(p),0,0]),rs:R(p=>[-23*guard(p),0,0]),rfoot:R(p=>[8*guard(p),0,0]),lt:R(p=>[10*guard(p),0,0]),ls:R(p=>[-18*guard(p),0,0]),lfoot:R(p=>[8*guard(p),0,0])});
const squat=p=>curve(p,[[0,0],[.28,1],[.39,1],[.53,0],[1,0]]),air=p=>smooth((p-.4)/.15);
clip('jump',1,false,{root:P(p=>[0,-10*squat(p),0]),torso:R(p=>[-25*squat(p)-7*air(p),0,0]),head:R(p=>[20*squat(p)+6*air(p),0,0]),rt:R(p=>[50*squat(p)+26*air(p),0,0]),rs:R(p=>[-86*squat(p)-54*air(p),0,0]),rfoot:R(p=>[36*squat(p)+28*air(p),0,0]),lt:R(p=>[50*squat(p)-10*air(p),0,0]),ls:R(p=>[-86*squat(p)-26*air(p),0,0]),lfoot:R(p=>[36*squat(p)+28*air(p),0,0]),ra:R(p=>[-24*squat(p)+45*air(p),0,-12*air(p)]),la:R(p=>[-24*squat(p)+60*air(p),0,12*air(p)]),rf:R(p=>[25*air(p),0,0]),lf:R(p=>[32*air(p),0,0])});
clip('airborne',1,true,{torso:constant(-7),head:constant(6),rt:constant(26),rs:constant(-54),rfoot:constant(28),lt:constant(-10),ls:constant(-26),lfoot:constant(28),ra:constant(45,0,-12),la:constant(60,0,12),rf:constant(25),lf:constant(32)});
const hug=p=>smooth(p/.7);
clip('embrace',1,false,{root:P(p=>[0,-5*hug(p),0]),torso:R(p=>[-24*hug(p),0,0]),head:R(p=>[-2*hug(p),0,0]),ra:R(p=>[45*hug(p),-14*hug(p),22*hug(p)]),rf:R(p=>[38*hug(p),-28*hug(p),0]),rh:R(p=>[0,0,12*hug(p)]),la:R(p=>[45*hug(p),14*hug(p),-22*hug(p)]),lf:R(p=>[38*hug(p),28*hug(p),0]),lh:R(p=>[0,0,-12*hug(p)]),rt:R(p=>[20*hug(p),0,-2*hug(p)]),rs:R(p=>[-32*hug(p),0,0]),rfoot:R(p=>[12*hug(p),0,0]),lt:R(p=>[20*hug(p),0,2*hug(p)]),ls:R(p=>[-32*hug(p),0,0]),lfoot:R(p=>[12*hug(p),0,0])});
clip('healing_breath',3.6,true,{torso:R(p=>[.7*sin(p),0,0]),head:R(p=>[-.5*sin(p),0,0]),ra:R(p=>[.4*sin(p),0,.4*sin(p)]),la:R(p=>[.4*sin(p),0,-.4*sin(p)]),'Serpent tail 4':R(p=>[0,1.5*sin(p-.2),0])});
model.animations=clips;
function rotate(p,r){let[x,y,z]=p;const[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
function bake(nodes){return nodes.map(n=>{
 if(typeof n!=='string'){const g=groups.get(n.uuid);return{name:g.name,origin:g.origin,rotation:g.rotation||[0,0,0],visible:g.visibility!==false&&g.export!==false,children:bake(n.children)};}
 const e=elements.get(n),[x,y,z]=e.from,[X,Y,Z]=e.to,o=e.origin||[0,0,0],quads=[];
 const faces={north:[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],south:[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],east:[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],west:[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],up:[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],down:[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]};
 for(const[side,points]of Object.entries(faces)){const f=e.faces[side];if(f.texture==null)continue;const[u,v,U,V]=f.uv,uv=[[U,V],[u,V],[u,v],[U,v]],shift=(f.rotation||0)/90;
 const vertices=points.map((p,i)=>[...rotate(p.map((v,j)=>v-o[j]),e.rotation||[0,0,0]).map((v,j)=>v+o[j]),uv[(i-shift+4)%4][0]/model.resolution.width,uv[(i-shift+4)%4][1]/model.resolution.height]);
 const[a,b,c]=vertices,ab=b.slice(0,3).map((v,i)=>v-a[i]),ac=c.slice(0,3).map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...normal);
 if(len>0)quads.push({vertices,normal:normal.map(v=>v/len)});
 }return{name:e.name,visible:e.visibility!==false&&e.export!==false,quads};
});}
function sample(keys,time,axis){
 let i=0;while(i<keys.length-2&&keys[i+1].time<time)i++;
 const a=keys[i],b=keys[Math.min(i+1,keys.length-1)],t=a.time===b.time?0:clamp((time-a.time)/(b.time-a.time));
 const val=k=>+k.data_points[0][axis],v0=val(keys[Math.max(0,i-1)]),v1=val(a),v2=val(b),v3=val(keys[Math.min(keys.length-1,i+2)]);
 return b.interpolation==='catmullrom'?.5*((2*v1)+(-v0+v2)*t+(2*v0-5*v1+4*v2-v3)*t*t+(-v0+3*v1-3*v2+v3)*t*t*t):v1+(v2-v1)*t;
}
const animations=clips.map(c=>({name:c.name.split('.').pop(),length:c.length,loop:c.loop==='loop',tracks:Object.values(c.animators).map(a=>{
 const keys=[];for(const channel of ['position','rotation']){const list=a.keyframes.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);if(!list.length)continue;
 for(let i=0;i<=80;i++)keys.push({channel,time:c.length*i/80,value:['x','y','z'].map(d=>+sample(list,c.length*i/80,d).toFixed(5))});}
 return{bone:a.name,keys};
})}));
for(const key of ['elements','groups','outliner','textures'])assert.deepStrictEqual(model[key],before[key],key+' was altered');
assert.strictEqual(hash(fs.readFileSync(source)),hash(original),'Source changed during export');
function write(file,data){const p=path.join(root,file);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);}
write('src/main/resources/assets/kazimod/models/entities/agito.json',JSON.stringify({sourceHash:hash(original),nodes:bake(model.outliner),animations}));
write('src/main/resources/assets/kazimod/textures/entities/agito.png',Buffer.from(model.textures[0].source.split(',')[1],'base64'));
write('deliverables/agito/agito_animated.bbmodel',JSON.stringify(model));
console.log(JSON.stringify({sourceHash:hash(original),cubes:model.elements.length,bones:model.groups.length,animations:animations.map(c=>c.name),geometryPreserved:true}));
