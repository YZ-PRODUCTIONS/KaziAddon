const fs = require('fs'), path = require('path'), crypto = require('crypto'), assert = require('assert');
const root = path.resolve(__dirname, '..');
const source = process.argv[2] || 'C:/blockbench/mahoraga.bbmodel';
const original = fs.readFileSync(source), model = JSON.parse(original);
const hash = b => crypto.createHash('sha256').update(b).digest('hex');
const uid = s => crypto.createHash('md5').update('mahoraga-awakening:' + s).digest('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/, '$1-$2-$3-$4-$5');
const clamp = v => Math.max(0, Math.min(1, v)), smooth = t => { t = clamp(t); return t*t*(3-2*t); };
const sin = p => Math.sin(p * Math.PI * 2);
function curve(p, keys) {
  let i=1; while(i<keys.length-1 && keys[i][0]<p)i++;
  const a=keys[i-1],b=keys[i],h=b[0]-a[0],t=clamp((p-a[0])/h);
  // Monotone Hermite slopes carry momentum through a motion instead of stopping at every key.
  const slope=j=>{
    if(j===0 || j===keys.length-1)return 0;
    const h0=keys[j][0]-keys[j-1][0],h1=keys[j+1][0]-keys[j][0];
    const d0=(keys[j][1]-keys[j-1][1])/h0,d1=(keys[j+1][1]-keys[j][1])/h1;
    if(d0*d1<=0)return 0;
    return 3*(h0+h1)/((2*h1+h0)/d0+(h1+2*h0)/d1);
  };
  return (2*t*t*t-3*t*t+1)*a[1]+(t*t*t-2*t*t+t)*h*slope(i-1)
      +(-2*t*t*t+3*t*t)*b[1]+(t*t*t-t*t)*h*slope(i);
}
function write(file, data) { const p=path.join(root,file); fs.mkdirSync(path.dirname(p),{recursive:true}); fs.writeFileSync(p,data); }
const groups = new Map(model.groups.map(g=>[g.uuid,g])), elements = new Map(model.elements.map(e=>[e.uuid,e]));
function find(nodes,name) { for(const n of nodes)if(typeof n==='object'){ if(groups.get(n.uuid).name===name)return n; const child=find(n.children,name); if(child)return child; } }
function joint(name, origin, parentName, select) {
  const parent=find(model.outliner,parentName), g={name,origin,rotation:[0,0,0],uuid:uid(name),visibility:true,export:true};
  groups.set(g.uuid,g); model.groups.push(g);
  const n={uuid:g.uuid,children:parent.children.filter(c=>typeof c==='string' && select(elements.get(c).name))};
  assert(n.children.length); parent.children=parent.children.filter(c=>!n.children.includes(c));parent.children.push(n);
}
joint('20 Breath abdomen',[0,73,0],'02 Torso',n=>n.startsWith('abdomen') || n.startsWith('waist '));
joint('21 Breath cheeks',[0,110,0],'05 Head',n=>n.startsWith('cranium'));
for(const side of ['Right','Left']){
  for(let i=0;i<4;i++)model.groups.find(g=>g.name===`15 ${side} finger ${i}`).rotation=[100,0,0];
  joint(`22 ${side} thumb`,[side==='Right'?-33.5:33.5,48.7,-2.5],`14 ${side} hand`,n=>n.toLowerCase().includes('thumb'));
  model.groups.find(g=>g.name===`22 ${side} thumb`).rotation=[35,0,side==='Right'?48:-48];
}
const B={root:'Mahoraga',pelvis:'01 Pelvis',torso:'02 Torso',neck:'03 Neck',head:'05 Head',crest:'08 Rear crest',wheel:'09 Eight handled wheel',sash:'11 Hanging sash',ra:'12 Right upper arm',rf:'13 Right forearm',rh:'14 Right hand',la:'12 Left upper arm',lf:'13 Left forearm',lh:'14 Left hand',rt:'16 Right thigh',rs:'17 Right shin',rfoot:'18 Right foot',lt:'16 Left thigh',ls:'17 Left shin',lfoot:'18 Left foot'};
const R = fn => ({rotation:fn}), P = fn => ({position:fn}), constant=(x=0,y=0,z=0)=>R(()=>[x,y,z]);
const clips=[];
function clip(name,length,loop,tracks) {
  for(const side of ['Right','Left']){
    const opening=p=>name==='throw_block' && side==='Left'?curve(p,[[0,0],[.17,.9],[.3,.15],[.62,.15],[.72,1],[.9,0],[1,0]])
      :name==='summon'?(1-smooth((p-.42)/.45))*.7:0;
    if(name==='throw_block' || name==='summon'){
      for(let i=0;i<4;i++)tracks[`15 ${side} finger ${i}`]=R(p=>[-85*opening(p),0,0]);
      tracks[`22 ${side} thumb`]=R(p=>[-30*opening(p),0,(side==='Right'?-42:42)*opening(p)]);
    }
  }
  const animators={};
  for(const [alias,channels] of Object.entries(tracks)){
    const bone=B[alias]||alias,g=model.groups.find(g=>g.name===bone);assert(g,'Missing '+bone);const keyframes=[];
    for(const [channel,fn]of Object.entries(channels))for(let i=0;i<=64;i++){
      const value=fn(i/64);assert(value.length===3 && value.every(Number.isFinite));
      keyframes.push({uuid:uid(name+bone+channel+i),channel,time:length*i/64,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((c,j)=>[c,+value[j].toFixed(5)]))]});
    }
    animators[g.uuid]={name:bone,type:'bone',keyframes};
  }
  clips.push({uuid:uid(name),name:'animation.mahoraga.'+name,length,loop:loop?'loop':'once',snapping:20,animators});
}
clip('idle',4.8,true,{torso:R(p=>[-1+.7*sin(p),0,.35*sin(p+.2)]),neck:R(p=>[.6*sin(p+.1),.9*sin(p/2),0]),head:R(p=>[.4*sin(p-.1),.7*sin(p+.2),0]),ra:R(p=>[2*sin(p-.1),0,1.3]),la:R(p=>[1.7*sin(p+.1),0,-1.3]),rf:R(p=>[3+1.2*sin(p),0,0]),lf:R(p=>[4+sin(p-.2),0,0]),crest:R(p=>[.7*sin(p-.2),0,.5*sin(p)]),sash:R(p=>[1.2*sin(p-.15),0,.7*sin(p)]),'20 Breath abdomen':{scale:p=>[1+.012*sin(p),1,1+.018*sin(p)]}});
{
  const tracks={};
  for(const [side,phase]of [['Right',0],['Left',.5]]){
    const stride=14,duty=.66;
    // Two-link leg solution keeps feet level during stance and clears the ground on recovery.
    const ik=p=>{p=(p+phase)%1;const stance=p<duty,t=stance?p/duty:(p-duty)/(1-duty),z=stance?-stride+2*stride*t:stride-2*stride*smooth(t),lift=stance?0:8*Math.sin(t*Math.PI),L1=26,L2=27,dy=49-lift,d=Math.min(52.99,Math.hypot(dy,z));const a=Math.atan2(-z,dy)+Math.acos(Math.max(-1,Math.min(1,(L1*L1+d*d-L2*L2)/(2*L1*d)))),b=-Math.acos(Math.max(-1,Math.min(1,(d*d-L1*L1-L2*L2)/(2*L1*L2))));return[a*180/Math.PI,b*180/Math.PI];};
    tracks['16 '+side+' thigh']=R(p=>[ik(p)[0],0,side==='Right'?1:-1]);
    tracks['17 '+side+' shin']=R(p=>[ik(p)[1],0,0]);
    tracks['18 '+side+' foot']=R(p=>[-ik(p)[0]-ik(p)[1],0,0]);
    tracks['12 '+side+' upper arm']=R(p=>[-21*sin(p+phase)+4,0,(side==='Right'?1:-1)*5]);
    tracks['13 '+side+' forearm']=R(p=>[12+9*sin(p+phase+.2),0,0]);
  }
  tracks.root=P(p=>[0,-3,0]);
  tracks.torso=R(p=>[-3,3*sin(p),1.7*sin(p)]);
  tracks.pelvis=R(p=>[0,-2*sin(p),0]);
  tracks.head=R(p=>[2,-2*sin(p),-sin(p)]);
  tracks.sash=R(p=>[-7+4*sin(p*2-.15),0,3*sin(p-.1)]);
  tracks.crest=R(p=>[1,2*sin(p-.2),0]);
  tracks.wheel=P(p=>[0,1.2*sin(p*2),0]);
  clip('walk',1.25,true,tracks);
}
const running={};
const runHip=p=>curve(p,[[0,23],[.18,3],[.36,-20],[.5,-25],[.66,28],[.78,48],[.9,37],[1,23]]);
const runKnee=p=>curve(p,[[0,-10],[.16,-12],[.34,-8],[.45,-48],[.57,-102],[.71,-80],[.86,-30],[1,-10]]);
const runToe=p=>curve(p,[[0,0],[.18,0],[.36,-22],[.5,-35],[.7,-15],[.86,0],[1,0]]);
for(const [side,offset]of [['Right',0],['Left',.5]]){
  const phase=p=>(p+offset)%1,sign=side==='Right'?1:-1;
  // A nearly straight support leg alternates with a folded heel-recovery leg.
  running[`16 ${side} thigh`]=R(p=>[runHip(phase(p)),0,sign*1.5]);
  running[`17 ${side} shin`]=R(p=>[runKnee(phase(p)),0,0]);
  running[`18 ${side} foot`]=R(p=>[runToe(phase(p))-runHip(phase(p))-runKnee(phase(p)),0,0]);
  running[`12 ${side} upper arm`]=R(p=>[-.85*runHip(phase(p))+8,sign*3,sign*6]);
  running[`13 ${side} forearm`]=R(p=>[62+12*sin(phase(p)+.1),0,0]);
}
running.root=P(p=>[0,-1.4-1.4*Math.cos(p*4*Math.PI),0]);
running.torso=R(p=>[-10-1.2*sin(p*2),5*sin(p),1.5*sin(p)]);
running.pelvis=R(p=>[0,-3*sin(p),1.2*sin(p)]);
running.head=R(p=>[7+sin(p*2),-3*sin(p),-sin(p)]);
running.sash=R(p=>[-16+5*sin(p*2-.15),0,4*sin(p-.1)]);
running.crest=R(p=>[4+sin(p*2-.15),2*sin(p-.2),0]);
clip('run',.72,true,running);
const rise=p=>1-smooth((p-.45)/.55);
clip('summon',4,false,{root:{position:p=>[0,-9*rise(p),0],rotation:p=>[0,0,0]},torso:R(p=>[-32*rise(p),-12*rise(p),-7*rise(p)]),pelvis:R(p=>[0,8*rise(p),0]),head:R(p=>[18*rise(p),12*rise(p),0]),ra:R(p=>[-32*rise(p),-22*rise(p),-64*rise(p)]),rf:R(p=>[23*rise(p),0,0]),la:R(p=>[48*rise(p),14*rise(p),46*rise(p)]),lf:R(p=>[18*rise(p),0,0]),rt:R(p=>[52*rise(p),-9*rise(p),-6*rise(p)]),rs:R(p=>[-77*rise(p),0,0]),rfoot:R(p=>[25*rise(p),0,0]),lt:R(p=>[32*rise(p),8*rise(p),7*rise(p)]),ls:R(p=>[-48*rise(p),0,0]),lfoot:R(p=>[16*rise(p),0,0]),crest:R(p=>[12*rise(p),0,0]),wheel:R(p=>[-12*rise(p),45*smooth(p),-7*rise(p)]),sash:R(p=>[-12*rise(p),0,-9*rise(p)])});
for(const right of [true,false]){
 const sign=right?1:-1,wind=p=>curve(p,[[0,0],[.25,-1],[.34,-.75],[.46,.78],[.58,1.15],[.73,.65],[1,0]]),lift=p=>curve(p,[[0,0],[.19,.7],[.31,1],[.56,.9],[.76,.55],[1,0]]);
 const body=p=>wind(Math.min(1,p+.055)),drag=p=>wind(Math.max(0,p-.065));
 clip(right?'slash_right':'slash_left',1.2,false,{
   torso:R(p=>[-13*lift(p)-5*Math.max(0,body(p)),sign*32*body(p),sign*9*body(p)]),
   pelvis:R(p=>[-3*lift(p),sign*12*wind(Math.min(1,p+.10)),-sign*3*body(p)]),
   ra:R(p=>[88*lift(p)+12*drag(p),sign*64*wind(p),sign*28*drag(p)]),
   rf:R(p=>[curve(p,[[0,0],[.27,55],[.43,12],[.56,5],[.73,27],[1,0]]),sign*16*drag(p),0]),
   rh:R(p=>[5*drag(p),0,-sign*20*drag(p)]),la:R(p=>[32*lift(p),-sign*24*body(p),-18*lift(p)]),
   lf:R(p=>[55*lift(p),0,0]),head:R(p=>[10*lift(p),-sign*18*body(p),-sign*4*drag(p)]),
   crest:R(p=>[3*lift(p),-sign*5*drag(p),sign*4*drag(p)]),
   sash:R(p=>[-8*lift(p),-sign*14*drag(p),sign*10*drag(p)]),
   lt:R(p=>[22*lift(p),0,4*lift(p)]),ls:R(p=>[-34*lift(p),0,0]),lfoot:R(p=>[12*lift(p),0,0]),
   rt:R(p=>[8*lift(p),0,-4*lift(p)]),rs:R(p=>[-14*lift(p),0,0]),rfoot:R(p=>[6*lift(p),0,0]),
   root:P(p=>[sign*1.5*body(p),-4*lift(p),-5*Math.max(0,wind(p))])});
}
const slamRaise=p=>curve(p,[[0,0],[.18,.5],[.38,1],[.48,1],[.6,.65],[.76,.45],[1,0]]);
const slamDrive=p=>curve(p,[[0,0],[.46,0],[.6,1],[.67,1],[.82,.5],[1,0]]);
const bladeArm=p=>curve(p,[[0,0],[.18,85],[.38,170],[.48,175],[.6,66],[.68,62],[.82,40],[1,0]]);
clip('overhead_slam',2,false,{
 ra:R(p=>[bladeArm(p),-6*slamRaise(p),-8*slamRaise(p)]),
 rf:R(p=>[8*slamRaise(p)-11*slamDrive(p),0,0]),rh:R(p=>[0,0,0]),
 la:R(p=>[-12*slamRaise(p)-10*slamDrive(p),0,8*slamRaise(p)]),
 lf:R(p=>[12*slamRaise(p),0,0]),
 torso:R(p=>[8*slamRaise(p)-45*slamDrive(p),8*slamRaise(p)-16*slamDrive(p),-4*slamDrive(p)]),
 head:R(p=>[-5*slamRaise(p)+20*slamDrive(p),-4*slamRaise(p),0]),
 root:P(p=>[0,-4*slamDrive(p),-4*slamDrive(p)]),
 rt:R(p=>[23*slamDrive(p),0,-3*slamDrive(p)]),rs:R(p=>[-32*slamDrive(p),0,0]),rfoot:R(p=>[9*slamDrive(p),0,0]),
 lt:R(p=>[9*slamDrive(p),0,3*slamDrive(p)]),ls:R(p=>[-14*slamDrive(p),0,0]),lfoot:R(p=>[5*slamDrive(p),0,0]),
 sash:R(p=>[-20*slamDrive(Math.max(0,p-.04)),0,5*slamDrive(p)]),crest:R(p=>[8*slamDrive(Math.max(0,p-.03)),0,0])});
const guard=p=>smooth(p/.12)*(1-smooth((p-.82)/.18));
const punches=p=>{const t=p*42;return Math.cos((t-10)*Math.PI/4)*smooth((t-5)/4)*(1-smooth((t-31)/7));};
clip('punch_barrage',2.1,false,{
 torso:R(p=>[(-16-3*Math.abs(punches(p)))*guard(p),19*punches(p+.015),4*punches(p)]),
 pelvis:R(p=>[-3*guard(p),7*punches(p+.04),-2*punches(p)]),
 head:R(p=>[10*guard(p),-10*punches(p),-3*punches(p)]),
 ra:R(p=>[(76+23*punches(p))*guard(p),-12*guard(p),-10*guard(p)]),
 rf:R(p=>[(49-44*punches(p))*guard(p),0,0]),rh:R(p=>[-7*punches(p),0,0]),
 la:R(p=>[(76-23*punches(p))*guard(p),12*guard(p),10*guard(p)]),
 lf:R(p=>[(49+44*punches(p))*guard(p),0,0]),lh:R(p=>[7*punches(p),0,0]),
 rt:R(p=>[23*guard(p)+3*punches(p),0,-3*guard(p)]),rs:R(p=>[-35*guard(p)-3*punches(p),0,0]),rfoot:R(p=>[12*guard(p),0,0]),
 lt:R(p=>[13*guard(p)-3*punches(p),0,3*guard(p)]),ls:R(p=>[-23*guard(p)+3*punches(p),0,0]),lfoot:R(p=>[10*guard(p),0,0]),
 sash:R(p=>[-10*guard(p),6*punches(p-.04),5*punches(p-.025)]),crest:R(p=>[3*guard(p),-4*punches(p-.02),0]),
 root:P(p=>[1.2*punches(p),-4*guard(p)-.7*Math.abs(punches(p)),-4*guard(p)])});
clip('block',1.2,false,{torso:R(p=>[-5*guard(p),-8*guard(p),0]),head:R(p=>[-5*guard(p),0,0]),ra:R(p=>[55*guard(p),-48*guard(p),-18*guard(p)]),rf:R(p=>[112*guard(p),-18*guard(p),-15*guard(p)]),la:R(p=>[66*guard(p),48*guard(p),18*guard(p)]),lf:R(p=>[98*guard(p),18*guard(p),15*guard(p)]),rt:R(p=>[18*guard(p),0,0]),rs:R(p=>[-30*guard(p),0,0]),rfoot:R(p=>[12*guard(p),0,0]),lt:R(p=>[18*guard(p),0,0]),ls:R(p=>[-30*guard(p),0,0]),lfoot:R(p=>[12*guard(p),0,0]),root:P(p=>[0,-4*guard(p),0])});
clip('dodge',.7,false,{torso:R(p=>[-12*guard(p),0,14*guard(p)]),head:R(p=>[8*guard(p),0,-10*guard(p)]),rt:R(p=>[24*guard(p),0,-18*guard(p)]),rs:R(p=>[-48*guard(p),0,0]),rfoot:R(p=>[24*guard(p),0,0]),lt:R(p=>[-10*guard(p),0,-15*guard(p)]),ls:R(p=>[-20*guard(p),0,0]),lfoot:R(p=>[30*guard(p),0,0]),ra:R(p=>[12*guard(p),0,-24*guard(p)]),la:R(p=>[30*guard(p),0,15*guard(p)]),rf:R(p=>[55*guard(p),0,0]),lf:R(p=>[35*guard(p),0,0]),root:P(p=>[0,-6*guard(p),0]),sash:R(p=>[-15*guard(p),0,-18*guard(p)])});
const squat=p=>curve(p,[[0,0],[.35,1],[.49,1],[.60,0],[1,0]]),air=p=>smooth((p-.48)/.15);
clip('air_jump',1,false,{root:P(p=>[0,-12*squat(p),0]),torso:R(p=>[-24*squat(p)-5*air(p),0,0]),head:R(p=>[20*squat(p)+9*air(p),0,0]),rt:R(p=>[58*squat(p)+30*air(p),0,0]),rs:R(p=>[-100*squat(p)-65*air(p),0,0]),rfoot:R(p=>[42*squat(p)+25*air(p),0,0]),lt:R(p=>[58*squat(p)-12*air(p),0,0]),ls:R(p=>[-100*squat(p)-25*air(p),0,0]),lfoot:R(p=>[42*squat(p)+20*air(p),0,0]),ra:R(p=>[-26*squat(p)+70*air(p),0,-12*air(p)]),la:R(p=>[-26*squat(p)+92*air(p),0,12*air(p)]),rf:R(p=>[15*squat(p)+40*air(p),0,0]),lf:R(p=>[15*squat(p)+30*air(p),0,0]),sash:R(p=>[-35*air(p),0,0]),crest:R(p=>[8*air(p),0,0])});
const smash=p=>curve(p,[[0,0],[.24,1],[.34,.33],[.6,.33],[1,0]]);
clip('air_takedown',1.5,false,{torso:R(p=>[-25*guard(p),0,0]),ra:R(p=>[135*smash(p),0,-8*guard(p)]),la:R(p=>[125*smash(p),0,8*guard(p)]),rf:R(p=>[25*guard(p),0,0]),lf:R(p=>[25*guard(p),0,0]),rt:R(p=>[30*guard(p),0,0]),rs:R(p=>[-55*guard(p),0,0]),lt:R(p=>[10*guard(p),0,0]),ls:R(p=>[-25*guard(p),0,0]),sash:R(p=>[-40*guard(p),0,0]),head:R(p=>[15*guard(p),0,0])});
const reach=p=>curve(p,[[0,0],[.25,1],[.4,1],[.54,0],[1,0]]),throwing=p=>curve(p,[[0,0],[.35,0],[.58,-1],[.70,1],[.83,.65],[1,0]]);
clip('throw_block',1.8,false,{root:P(p=>[0,-9*reach(p),0]),torso:R(p=>[-50*reach(p),-20*throwing(p),0]),head:R(p=>[32*reach(p),12*throwing(p),0]),la:R(p=>[28*reach(p)+95*Math.abs(throwing(p)),60*throwing(p),5*reach(p)]),lf:R(p=>[10*reach(p)+40*Math.max(0,-throwing(p)),0,0]),ra:R(p=>[15*reach(p),-15*throwing(p),-14*reach(p)]),rt:R(p=>[45*reach(p),0,0]),rs:R(p=>[-70*reach(p),0,0]),rfoot:R(p=>[25*reach(p),0,0]),lt:R(p=>[45*reach(p),0,0]),ls:R(p=>[-70*reach(p),0,0]),lfoot:R(p=>[25*reach(p),0,0]),sash:R(p=>[-15*reach(p),10*throwing(p),0])});
const inhale=p=>curve(p,[[0,0],[.14,0],[.62,1],[.68,1],[.73,0],[1,0]]),recoil=p=>curve(p,[[0,0],[.66,0],[.71,1],[.84,.25],[1,0]]);
clip('air_blast',2.2,false,{torso:R(p=>[8*inhale(p)-18*recoil(p),0,0]),head:R(p=>[10*inhale(p)-12*recoil(p),0,0]),ra:R(p=>[-12*inhale(p)+24*recoil(p),0,-14*inhale(p)]),la:R(p=>[-12*inhale(p)+24*recoil(p),0,14*inhale(p)]),rf:R(p=>[22*inhale(p),0,0]),lf:R(p=>[22*inhale(p),0,0]),root:P(p=>[0,0,3*recoil(p)]),'20 Breath abdomen':{scale:p=>[1+.28*inhale(p),1+.05*inhale(p),1+.5*inhale(p)]},'21 Breath cheeks':{scale:p=>[1+.23*inhale(p),1,1+.08*inhale(p)]},'06 Jaw':R(p=>[-12*recoil(p),0,0]),sash:R(p=>[-12*recoil(p),0,0])});
clip('airborne',1,true,{torso:constant(-5),head:constant(6),rt:constant(25),rs:constant(-55),rfoot:constant(24),lt:constant(-10),ls:constant(-22),lfoot:constant(25),ra:constant(15,0,-14),la:constant(28,0,14),rf:constant(28),lf:constant(30),sash:constant(-25)});
clip('wheel_turn',1,false,{wheel:R(p=>[0,45*smooth(p),0])});
const summonClip=clips.find(c=>c.name==='animation.mahoraga.summon');
const dismissClip=JSON.parse(JSON.stringify(summonClip));
dismissClip.uuid=uid('dismiss');dismissClip.name='animation.mahoraga.dismiss';dismissClip.length=3;
for(const animator of Object.values(dismissClip.animators)){
  for(const key of animator.keyframes){
    key.uuid=uid('dismiss:'+key.uuid);
    key.time=3*(1-key.time/summonClip.length);
    // Adaptation owns the persistent wheel rotation; dismissal must not unwind it.
    if(animator.name===B.wheel && key.channel==='rotation')key.data_points[0].y=0;
  }
  animator.keyframes.sort((a,b)=>a.time-b.time);
}
clips.push(dismissClip);
model.animations=clips;
function rotate(p,r){let[x,y,z]=p;const[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
function bake(nodes){return nodes.map(n=>{if(typeof n!=='string'){const g=groups.get(n.uuid);return{name:g.name,origin:g.origin,rotation:g.rotation||[0,0,0],visible:g.visibility!==false,children:bake(n.children)};}
 const e=elements.get(n),[x,y,z]=e.from,[X,Y,Z]=e.to,o=e.origin||[0,0,0],quads=[];
 const faces={north:[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],south:[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],east:[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],west:[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],up:[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],down:[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]};
 for(const[side,points]of Object.entries(faces)){const f=e.faces[side];if(f.texture==null)continue;const[u,v,U,V]=f.uv,uv=[[U,V],[u,V],[u,v],[U,v]],shift=(f.rotation||0)/90;
 const vertices=points.map((p,i)=>[...rotate(p.map((v,j)=>v-o[j]),e.rotation||[0,0,0]).map((v,j)=>v+o[j]),uv[(i-shift+4)%4][0]/model.resolution.width,uv[(i-shift+4)%4][1]/model.resolution.height]);
 const[a,b,c]=vertices,ab=b.slice(0,3).map((v,i)=>v-a[i]),ac=c.slice(0,3).map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...normal);if(len>0)quads.push({vertices,normal:normal.map(v=>v/len)});}
 return{name:e.name,visible:e.visibility!==false,quads};});}
const animations=clips.map(c=>({name:c.name.split('.').pop(),length:c.length,loop:c.loop==='loop',tracks:Object.values(c.animators).map(a=>({bone:a.name,keys:a.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(d=>Number(k.data_points[0][d]))}))}))}));
assert.deepStrictEqual(model.elements,JSON.parse(original).elements);
assert.deepStrictEqual(model.textures,JSON.parse(original).textures);
for(const group of JSON.parse(original).groups){
  const updated=model.groups.find(g=>g.uuid===group.uuid);
  assert.deepStrictEqual({...updated,rotation:group.rotation},group);
  if(!group.name.startsWith('15 '))assert.deepStrictEqual(updated.rotation,group.rotation);
}
write('src/main/resources/assets/kazimod/models/entities/mahoraga.json',JSON.stringify({sourceHash:hash(original),nodes:bake(model.outliner),animations}));
write('src/main/resources/assets/kazimod/textures/entities/mahoraga.png',Buffer.from(model.textures[0].source.split(',')[1],'base64'));
write('deliverables/mahoraga/mahoraga_animated.bbmodel',JSON.stringify(model));
console.log(`${model.elements.length} unchanged cubes and texture; closed resting hands, preserved sword/head-wing transforms; ${clips.length} clips exported.`);
