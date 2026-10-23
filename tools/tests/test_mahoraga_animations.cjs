const fs=require('fs'),path=require('path'),assert=require('assert');
const root=path.resolve(__dirname,'../..');
const model=JSON.parse(fs.readFileSync(path.join(root,'src/main/resources/assets/kazimod/models/entities/mahoraga.json')));
const run=model.animations.find(a=>a.name==='run'),slam=model.animations.find(a=>a.name==='overhead_slam');
function keys(clip,bone,channel='rotation'){
  return clip.tracks.find(t=>t.bone===bone).keys.filter(k=>k.channel===channel);
}
let checks=0;
function check(value,message){checks++;assert(value,message);}
const right=keys(run,'17 Right shin'),left=keys(run,'17 Left shin'),height=keys(run,'Mahoraga','position');
for(let i=0;i<right.length;i++){
  check(Math.min(Math.abs(right[i].value[0]),Math.abs(left[i].value[0]))<32,'Both legs crouched at sample '+i);
  check(height[i].value[1]>=-2.81,'Running hips are too low');
}
check(Math.min(...right.map(k=>k.value[0]))<-95,'Recovery leg must fold behind the body');
for(const track of run.tracks){
  for(const channel of ['rotation','position']){
    const k=track.keys.filter(k=>k.channel===channel);if(!k.length)continue;
    check(k[0].value.every((v,i)=>Math.abs(v-k[k.length-1].value[i])<.001),'Run loop discontinuity: '+track.bone);
  }
}
check(Math.max(...keys(slam,'12 Right upper arm').map(k=>k.value[0]))>165,'Blade arm must extend overhead');
check(keys(slam,'13 Right forearm').every(k=>Math.abs(k.value[0])<12),'Blade elbow should stay extended');
check(keys(slam,'12 Left upper arm').every(k=>k.value[0]<=0),'Off-hand must not perform the overhead strike');
check(keys(slam,'13 Left forearm').every(k=>Math.abs(k.value[0])<=12),'Off-hand remains relaxed');
const impact=keys(slam,'12 Right upper arm').reduce((a,b)=>Math.abs(a.time-1.2)<Math.abs(b.time-1.2)?a:b);
check(impact.value[0]<80,'Downstroke must align with the tick-24 damage event');
const summon=model.animations.find(a=>a.name==='summon'),dismiss=model.animations.find(a=>a.name==='dismiss');
check(dismiss.length===3 && !dismiss.loop,'Dismissal is a three-second one-shot');
for(const track of dismiss.tracks){
  const original=summon.tracks.find(t=>t.bone===track.bone);
  for(const channel of ['rotation','position']){
    const forward=original.keys.filter(k=>k.channel===channel),reverse=track.keys.filter(k=>k.channel===channel);
    check(forward.length===reverse.length,'Reverse summon retains every key');
    for(let i=0;i<reverse.length;i++){
      const expected=forward[forward.length-1-i];
      check(Math.abs(reverse[i].time-3*(1-expected.time/4))<.00001,'Dismissal key time is reversed');
      check(reverse[i].value.every((v,j)=>Math.abs(v-(track.bone==='09 Eight handled wheel' && channel==='rotation' && j===1?0:expected.value[j]))<.00001),'Dismissal pose reverses summon without unwinding wheel');
    }
  }
}
console.log('Mahoraga animation checks: '+checks+' passed.');
