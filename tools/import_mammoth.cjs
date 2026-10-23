const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const root=path.resolve(__dirname,'..');
const source=process.argv[2]||'C:/Users/User/Downloads/KaziAddon/deliverables/mammoth';
const uid=s=>crypto.createHash('md5').update('mammoth-animated:'+s).digest('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/,'$1-$2-$3-$4-$5');
const clamp=(v,a=0,b=1)=>Math.max(a,Math.min(b,v)),smooth=t=>{t=clamp(t);return t*t*(3-2*t);};
const sin=t=>Math.sin(t*Math.PI*2),deg=r=>r*180/Math.PI;
function curve(p,keys){let i=1;while(i<keys.length-1&&keys[i][0]<p)i++;const a=keys[i-1],b=keys[i],t=smooth((p-a[0])/(b[0]-a[0]));return a[1]+(b[1]-a[1])*t;}
function write(file,data){const p=path.join(root,file);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);}
function rotate(p,r){let[x,y,z]=p;const[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
function exportModel(hybrid){
 const id=hybrid?'mammoth_hybrid':'mammoth_full',original=fs.readFileSync(path.join(source,id+'.bbmodel')),model=JSON.parse(original);
 const groups=new Map(model.groups.map(g=>[g.uuid,g])),elements=new Map(model.elements.map(e=>[e.uuid,e]));
 function find(nodes,name){for(const n of nodes)if(typeof n==='object'){if(groups.get(n.uuid).name===name)return n;const found=find(n.children,name);if(found)return found;}}
 function addGroup(name,origin,parent,predicate){const g={name,origin,rotation:[0,0,0],uuid:uid(id+name),visibility:true,export:true};groups.set(g.uuid,g);model.groups.push(g);const node={uuid:g.uuid,children:parent.children.filter(n=>typeof n==='string'&&predicate(elements.get(n).name))};parent.children=parent.children.filter(n=>!node.children.includes(n));parent.children.push(node);return node;}
 if(!hybrid)for(const front of ['front','hind'])for(const side of ['right','left']){
  const sign=side==='right'?-1:1,z=front==='front'?-15:20;
  addGroup(front+'_'+side+'_foot',[sign*11.7,3,z],find(model.outliner,front+'_'+side+'_shin'),n=>n.startsWith('foot_')||n.startsWith('toenail_'));
 }
 const clips=[];
 function clip(name,length,loop,tracks){const animators={},samples=Array.from({length:49},(_,i)=>i/48);
 if(!hybrid&&(name==='walk'||name==='run'))for(const phase of [0,.24,.5,.74]){samples.push((1-phase)%1,((name==='run'?.62:.76)-phase+1)%1);}
 const times=[...new Set(samples)].sort((a,b)=>a-b);
 for(const[bone,channels]of Object.entries(tracks)){
  const g=model.groups.find(g=>g.name===bone);assert(g,`${id}: missing ${bone}`);const keyframes=[];
  for(const[channel,fn]of Object.entries(channels))for(let i=0;i<times.length;i++){const time=length*times[i],v=fn(times[i]);assert(v.every(Number.isFinite));keyframes.push({uuid:uid(id+name+bone+channel+i),channel,time,interpolation:'linear',data_points:[{x:v[0],y:v[1],z:v[2]}]});}
  animators[g.uuid]={name:bone,type:'bone',keyframes};
 }clips.push({uuid:uid(id+name),name:'animation.'+id+'.'+name,length,loop:loop?'loop':'once',snapping:20,animators});}
 const r=(x=0,y=0,z=0)=>({rotation:()=>[x,y,z]});
 clip('idle',5,true,{body:{rotation:p=>[.4*sin(p),0,.35*sin(p+.2)]},head:{rotation:p=>[-.6*sin(p),.7*sin(p+.15),0]},trunk_base:{rotation:p=>[1.2*sin(p+.1),.9*sin(p),0]},trunk_middle:{rotation:p=>[1.7*sin(p-.12),0,1.2*sin(p)]},trunk_tip:{rotation:p=>[2.4*sin(p-.22),0,1.2*sin(p-.2)]},left_ear:{rotation:p=>[0,3*sin(p+.13),1*sin(p)]},right_ear:{rotation:p=>[0,-3*sin(p),-1*sin(p)]},tail:{rotation:p=>[1.5*sin(p),5*sin(p-.3),2*sin(p)]}});
 for(const run of [false,true]){
  const tracks={},length=run?1.05:2.15;
  if(!hybrid){
   for(const[leg,phase]of [['hind_left',0],['front_left',.24],['hind_right',.5],['front_right',.74]]){
    const front=leg.startsWith('front'),duty=run?.62:.76,step=run?8:5;
    const ik=p=>{p=(p+phase)%1;const stance=p<duty,t=stance?p/duty:(p-duty)/(1-duty),z=stance?-step+2*step*t:step-2*step*smooth(t),lift=stance?0:(run?4.7:3.2)*Math.sin(t*Math.PI);const dz=front?1:-1,L1=Math.hypot(16,dz),L2=Math.hypot(9,dz),dy=23-lift,d=Math.min(L1+L2-.001,Math.hypot(dy,z)),bend=front?1:-1;const a=Math.atan2(-z,dy)-bend*Math.acos(clamp((L1*L1+d*d-L2*L2)/(2*L1*d),-1,1)),b=bend*Math.acos(clamp((d*d-L1*L1-L2*L2)/(2*L1*L2),-1,1));const restA=deg(Math.atan2(-dz,16)),restB=deg(Math.atan2(dz,9))-restA;return[deg(a)-restA,deg(b)-restB];};
    tracks[leg+'_leg']={position:()=>[0,-2,0],rotation:p=>[ik(p)[0],0,0]};
    tracks[leg+'_shin']={rotation:p=>[ik(p)[1],0,0]};
    tracks[leg+'_foot']={rotation:p=>[-ik(p)[0]-ik(p)[1],0,0]};
   }
   tracks.body={position:p=>[0,-1.6+(run?.45:.2)*sin(p*2),0],rotation:p=>[(run?1.1:.5)*sin(p*2),0,(run?1.1:.6)*sin(p)]};
   tracks.head={rotation:p=>[-(run?1.8:.7)*sin(p*2),0,-.4*sin(p)]};
  }else{
   for(const[side,phase]of [['left',0],['right',.5]]){
    tracks[side+'_leg']={rotation:p=>[(run?31:19)*sin(p+phase),0,0],position:p=>[0,Math.max(0,sin(p+phase+.25))*(run?.65:.3),0]};
    tracks[side+'_arm']={rotation:p=>[-(run?24:14)*sin(p+phase)- (run?12:0),0,(side==='left'?-1:1)*(run?5:2)]};
   }
   tracks.body={rotation:p=>[run?-5:-1,(run?3:1.5)*sin(p),0]};
   tracks.root={position:p=>[0,(run?.3:.12)*sin(p*2),0]};
  }
  tracks.trunk_base={rotation:p=>[(run?8:2)+2*sin(p+.2),0,(run?2:1)*sin(p)]};
  tracks.trunk_middle={rotation:p=>[(run?7:3)*sin(p-.15),0,2*sin(p-.1)]};
  tracks.trunk_tip={rotation:p=>[(run?8:3)*sin(p-.3),0,2*sin(p-.2)]};
  tracks.tail={rotation:p=>[run?7:2,7*sin(p-.2),3*sin(p)]};
  tracks.left_ear={rotation:p=>[0,4*sin(p-.13),run?-5:0]};tracks.right_ear={rotation:p=>[0,-4*sin(p-.13),run?5:0]};
  clip(run?'run':'walk',length,true,tracks);
 }
 clip('sweep_charge',1,false,{body:{rotation:p=>[0,-(hybrid?8:3)*smooth(p),0]},head:{rotation:p=>[7*smooth(p),-22*smooth(p),0]},trunk_base:{rotation:p=>[52*smooth(p),-36*smooth(p),0]},trunk_middle:{rotation:p=>[22*smooth(p),-14*smooth(p),0]},trunk_tip:{rotation:p=>[-28*smooth(p),-8*smooth(p),0]},left_ear:{rotation:p=>[0,-15*smooth(p),0]},right_ear:{rotation:p=>[0,15*smooth(p),0]}});
 const sw=p=>curve(p,[[0,-1],[.23,.95],[.4,1],[1,0]]),relax=p=>1-smooth((p-.38)/.62);
 clip('sweep',.85,false,{body:{rotation:p=>[0,(hybrid?8:3)*sw(p),0]},head:{rotation:p=>[7*relax(p),22*sw(p),-3*Math.sin(p*Math.PI)]},trunk_base:{rotation:p=>[52*relax(p),36*sw(p),0]},trunk_middle:{rotation:p=>[22*relax(p),20*sw(clamp(p-.045)),0]},trunk_tip:{rotation:p=>[-28*relax(p),16*sw(clamp(p-.09)),0]},tail:{rotation:p=>[0,-12*sw(p),0]}});
 clip('trunk_ready',1,false,{head:{rotation:p=>[5*smooth(p),0,0]},trunk_base:{rotation:p=>[50*smooth(p),0,0]},trunk_middle:{rotation:p=>[28*smooth(p),0,0]},trunk_tip:{rotation:p=>[-42*smooth(p),0,0]}});
 const shot=p=>curve(p,[[0,0],[.12,1],[.3,1],[.55,.55],[1,0]]);
 clip('trunk_shot',.72,false,{body:{rotation:p=>[-(hybrid?5:2)*shot(p),0,0]},head:{rotation:p=>[-6*shot(p),0,0],position:p=>[0,0,-(hybrid?.6:1.3)*shot(p)]},trunk_base:{rotation:p=>[50+20*shot(p),0,0]},trunk_middle:{rotation:p=>[28-25*shot(p),0,0]},trunk_tip:{rotation:p=>[-42+70*shot(p),0,0]}});
 const stomp={body:{position:p=>[0,curve(p,[[0,-1.7],[.16,-.8],[.62,.6],[.86,.45],[1,-1.7]]),0],rotation:p=>[-2+3*Math.sin(p*Math.PI),0,0]},head:{rotation:p=>[4*Math.sin(p*Math.PI),0,0]},trunk_base:{rotation:p=>[18+4*sin(p),0,0]},trunk_middle:{rotation:p=>[7+6*sin(p-.12),0,0]},trunk_tip:{rotation:p=>[-5+4*sin(p-.25),0,0]}};
 if(!hybrid)for(const side of ['left','right']){stomp['front_'+side+'_leg']={rotation:p=>[-28*Math.pow(Math.sin(p*Math.PI),2),0,0],position:p=>[0,2.5*Math.sin(p*Math.PI),0]};stomp['front_'+side+'_shin']={rotation:p=>[32*Math.pow(Math.sin(p*Math.PI),2),0,0]};stomp['front_'+side+'_foot']={rotation:p=>[-4*Math.pow(Math.sin(p*Math.PI),2),0,0]};}
 else{stomp.left_leg={rotation:p=>[-20*Math.sin(p*Math.PI),0,0]};stomp.right_leg=r(2);}
 clip('stomp',.4,true,stomp);
 for(const[left,name]of [[false,'attack_right'],[true,'attack_left']]){
  const sign=left?1:-1,strike=p=>curve(p,[[0,0],[.16,-.25],[.4,1],[.58,.8],[1,0]]),tracks={body:{rotation:p=>[-2*strike(p),sign*4*strike(p),0]},head:{rotation:p=>[-6*strike(p),sign*9*strike(p),0]},trunk_base:{rotation:p=>[28*strike(p),sign*8*strike(p),0]},trunk_middle:{rotation:p=>[16*strike(p),0,0]},trunk_tip:{rotation:p=>[-18*strike(p),0,0]}};
  if(hybrid)tracks[left?'left_arm':'right_arm']={rotation:p=>[95*strike(p),sign*12*strike(p),sign*8*strike(p)]};
  clip(name,.48,false,tracks);
 }
 const crouch={body:{position:()=>[0,hybrid?-1.1:-2.4,0],rotation:()=>[hybrid?-12:-3,0,0]},head:r(hybrid?10:3),trunk_base:r(hybrid?12:5)};
 if(hybrid){crouch.left_leg=r(-8);crouch.right_leg=r(-8);crouch.left_arm=r(8,0,-5);crouch.right_arm=r(8,0,5);}else for(const side of ['left','right'])for(const end of ['front','hind']){crouch[end+'_'+side+'_leg']=r(-8);crouch[end+'_'+side+'_shin']=r(14);}
 clip('crouch',1,false,crouch);
 const air={body:r(-3),head:r(7),trunk_base:r(24),trunk_middle:r(12),trunk_tip:r(-18),tail:r(15)};
 if(hybrid){air.left_arm=r(-18,0,-14);air.right_arm=r(-18,0,14);air.left_leg=r(18);air.right_leg=r(-12);}else for(const side of ['left','right']){air['front_'+side+'_leg']=r(-24);air['front_'+side+'_shin']=r(38);air['front_'+side+'_foot']=r(-14);air['hind_'+side+'_leg']=r(18);air['hind_'+side+'_shin']=r(-30);air['hind_'+side+'_foot']=r(12);}
 clip('airborne',1,false,air);
 const land={body:{position:p=>[0,-(hybrid?1.4:2.4)*Math.sin(p*Math.PI),0],rotation:p=>[2*Math.sin(p*Math.PI),0,0]},head:{rotation:p=>[-4*Math.sin(p*Math.PI),0,0]},trunk_base:{rotation:p=>[-4*Math.sin(p*Math.PI),0,0]},trunk_middle:{rotation:p=>[-6*Math.sin(p*Math.PI),0,0]}};
 clip('land',.45,false,land);
 const swim={body:r(5),head:r(15),trunk_base:r(60),trunk_middle:r(18),trunk_tip:r(20),tail:{rotation:p=>[0,8*sin(p),0]}};
 for(const side of ['left','right']){const phase=side==='left'?0:.5;if(hybrid){swim[side+'_arm']={rotation:p=>[20+25*sin(p+phase),0,(side==='left'?-1:1)*15]};swim[side+'_leg']={rotation:p=>[14*sin(p+phase),0,0]};}else for(const end of ['front','hind']){swim[end+'_'+side+'_leg']={rotation:p=>[20*sin(p+phase+(end==='hind'?.25:0)),0,0]};swim[end+'_'+side+'_shin']={rotation:p=>[10*sin(p+phase+.15),0,0]};}}
 clip('swim',2.2,true,swim);
 clip('rideable',1,false,{body:r(-1),head:r(2),trunk_base:r(5),left_ear:r(0,-4),right_ear:r(0,4)});
 const unfurl=p=>Math.pow(1-smooth(p),2);
 const transform={body:{position:p=>[0,-(hybrid?2:4)*unfurl(p),0]},head:{rotation:p=>[-10*unfurl(p),0,0]},trunk_base:{rotation:p=>[34*unfurl(p),0,0]},trunk_middle:{rotation:p=>[26*unfurl(p),0,0]},trunk_tip:{rotation:p=>[-35*unfurl(p),0,0]},left_ear:{rotation:p=>[0,-22*unfurl(p),0]},right_ear:{rotation:p=>[0,22*unfurl(p),0]}};
 if(hybrid){transform.left_arm={rotation:p=>[0,0,-14*unfurl(p)]};transform.right_arm={rotation:p=>[0,0,14*unfurl(p)]};}
 clip('transform',1.1,false,transform);
 clip('stampede_charge',1,false,{body:{rotation:p=>[-4*smooth(p),0,0]},head:{rotation:p=>[12*Math.sin(p*Math.PI)-10*smooth(p),0,0]},trunk_base:{rotation:p=>[65*Math.sin(p*Math.PI)+40*smooth(p),0,0]},trunk_middle:{rotation:p=>[30*smooth(p),0,0]},trunk_tip:{rotation:p=>[-25*smooth(p),0,0]},left_ear:{rotation:p=>[0,-18*smooth(p),0]},right_ear:{rotation:p=>[0,18*smooth(p),0]}});
 clip('stampede_run',1,true,{body:r(-3),head:r(-8),trunk_base:{rotation:p=>[35+3*sin(p),0,0]},trunk_middle:{rotation:p=>[30+4*sin(p-.1),0,0]},trunk_tip:r(-25),left_ear:r(0,-12),right_ear:r(0,12),tail:r(12)});
 clip('vacuum',1.4,true,{head:r(0),trunk_base:{rotation:p=>[65+1*sin(p),0,0]},trunk_middle:{rotation:p=>[20+1.2*sin(p-.15),0,0]},trunk_tip:{rotation:p=>[-35+2*sin(p-.25),0,0]},left_ear:{rotation:p=>[0,-8+2*sin(p),0]},right_ear:{rotation:p=>[0,8-2*sin(p),0]}});
 const settle=p=>1-smooth(p);
 clip('stomp_finish',.8,false,{body:{position:p=>[0,-3*settle(p),0],rotation:p=>[-3*settle(p),0,0]},head:{rotation:p=>[9*settle(p),0,0]},trunk_base:{rotation:p=>[38*settle(p),0,0]},trunk_middle:{rotation:p=>[16*settle(p),0,0]},trunk_tip:{rotation:p=>[-18*settle(p),0,0]}});
 model.animations=clips;
 function bake(nodes){return nodes.map(n=>{if(typeof n!=='string'){const g=groups.get(n.uuid);return{name:g.name,origin:g.origin,rotation:g.rotation||[0,0,0],visible:g.visibility!==false,children:bake(n.children)};}
 const e=elements.get(n),[x,y,z]=e.from,[X,Y,Z]=e.to,o=e.origin||[0,0,0],quads=[];
 const faces={north:[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],south:[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],east:[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],west:[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],up:[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],down:[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]};
 for(const[side,points]of Object.entries(faces)){const f=e.faces[side];if(f.texture==null)continue;const[u,v,U,V]=f.uv,uv=[[U,V],[u,V],[u,v],[U,v]],shift=(f.rotation||0)/90;
 const vertices=points.map((p,i)=>[...rotate(p.map((v,j)=>v-o[j]),e.rotation||[0,0,0]).map((v,j)=>v+o[j]),uv[(i-shift+4)%4][0]/model.resolution.width,uv[(i-shift+4)%4][1]/model.resolution.height]);
 const[a,b,c]=vertices,ab=b.slice(0,3).map((v,i)=>v-a[i]),ac=c.slice(0,3).map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...normal);assert(len>0);quads.push({vertices,normal:normal.map(v=>v/len)});}
 return{name:e.name,visible:e.visibility!==false,quads};});}
 const animations=clips.map(c=>({name:c.name.split('.').pop(),length:c.length,loop:c.loop==='loop',tracks:Object.values(c.animators).map(a=>({bone:a.name,keys:a.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(d=>Number(k.data_points[0][d]))}))}))}));
 assert.deepEqual(model.elements,JSON.parse(original).elements);assert.deepEqual(model.textures,JSON.parse(original).textures);
 write('src/main/resources/assets/kazimod/models/zoan/'+id+'.json',JSON.stringify({sourceHash:crypto.createHash('sha256').update(original).digest('hex'),nodes:bake(model.outliner),animations}));
 write('src/main/resources/assets/kazimod/textures/models/zoan/'+id+'.png',Buffer.from(model.textures[0].source.split(',')[1],'base64'));
 write('deliverables/mammoth/'+id+'_animated.bbmodel',JSON.stringify(model,null,2));
 console.log(`${id}: preserved ${model.elements.length} cubes and textures; ${clips.length} animated clips, ${model.groups.length} joints.`);
}
exportModel(false);exportModel(true);
