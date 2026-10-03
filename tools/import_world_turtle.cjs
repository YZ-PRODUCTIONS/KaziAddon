const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const root=path.resolve(__dirname,'..');
const source=process.argv[2]||'C:/Users/User/Downloads/KaziAddon/KaziAddon/deliverables/world_turtle/world_turtle.bbmodel';
const original=fs.readFileSync(source),model=JSON.parse(original);
const groups=new Map(model.groups.map(g=>[g.uuid,g])),elements=new Map(model.elements.map(e=>[e.uuid,e]));
const uuid=s=>{const h=crypto.createHash('md5').update('world-turtle:'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const front=['Front flipper swimming','Front_flipper_swimming2'],rear=['Rear flipper left','Rear flipper right'];
const clips=[];
function clip(name,length,loop,channels){
 const animators={};
 for(const [bone,data]of Object.entries(channels)){
  const g=model.groups.find(g=>g.name===bone);assert(g,`Missing bone ${bone}`);
  const keys=[];for(const [channel,fn]of Object.entries(data))for(let i=0;i<=64;i++){
   const time=length*i/64,v=fn(i/64);keys.push({channel,time,data_points:[{x:String(v[0]),y:String(v[1]),z:String(v[2])}],uuid:uuid(name+bone+channel+i),interpolation:'linear',color:-1});
  }animators[g.uuid]={name:bone,type:'bone',keyframes:keys};
 }clips.push({name:'animation.world_turtle.'+name,uuid:uuid(name),length,loop:loop?'loop':'once',animators});
}
const sin=p=>Math.sin(p*Math.PI*2);
clip('idle',6,true,{Neck:{rotation:p=>[1.2*sin(p),0,0]},Head:{rotation:p=>[-.6*sin(p),1.5*sin(p),0]},Tail:{rotation:p=>[0,4*sin(p),0]}});
for(const [name,length,amplitude]of [['fly',3.6,23],['fast_fly',2.4,32],['walk',3,7]]){
 const tracks={};const walking=name==='walk';
 front.forEach((bone,i)=>{const sign=i===0?1:-1;tracks[bone]={rotation:p=>[5*sin(p+(walking?i*.5:0)),sign*7*sin(p+.25+(walking?i*.5:0)),sign*amplitude*sin(p+(walking?i*.5:0))]};});
 rear.forEach((bone,i)=>{const sign=i===0?1:-1;tracks[bone]={rotation:p=>[4*sin(p-.12),sign*5*sin(p+.2),sign*(walking?5:11)*sin(p-.12+(walking?i*.5:0))]};});
 tracks['World Turtle']={position:p=>[0,walking?.35*(1-Math.cos(p*4*Math.PI)):1.2*sin(p-.12),0],rotation:p=>[walking?0:1.1*sin(p-.12),0,walking?.5*sin(p):0]};
 tracks.Neck={rotation:p=>[-2*sin(p-.12),0,0]};tracks.Tail={rotation:p=>[2*sin(p),6*sin(p-.2),0]};
 clip(name,length,true,tracks);
}
const takeoff={},landing={};
front.forEach((b,i)=>{let sign=i===0?1:-1;takeoff[b]={rotation:p=>[-6*Math.sin(Math.PI*p),0,-sign*22*Math.sin(Math.PI*p)]};landing[b]={rotation:p=>[4*Math.sin(Math.PI*p),0,sign*14*Math.sin(Math.PI*p)]};});
takeoff.Neck={rotation:p=>[8*Math.sin(Math.PI*p),0,0]};landing.Neck={rotation:p=>[-5*Math.sin(Math.PI*p),0,0]};
clip('takeoff',1.4,false,takeoff);clip('land',1,false,landing);
clip('bite',.6,false,{Neck:{rotation:p=>[-9*Math.sin(Math.PI*p),0,0],position:p=>[0,0,-2.5*Math.sin(Math.PI*p)]},Head:{rotation:p=>[13*Math.sin(Math.PI*p),0,0]}});
model.animations=clips;
function rotate(p,r){let[x,y,z]=p;const[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
const bake=nodes=>nodes.map(n=>{
 if(typeof n!=='string'){const g=groups.get(n.uuid)||n;return{name:g.name,origin:g.origin,rotation:g.rotation||[0,0,0],visible:g.visibility!==false,children:bake(n.children)};}
 const e=elements.get(n);assert.equal(e.type,'cube');const[x,y,z]=e.from,[X,Y,Z]=e.to,o=e.origin||[0,0,0];
 const faces={north:[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],south:[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],east:[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],west:[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],up:[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],down:[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]},quads=[];
 for(const[side,p]of Object.entries(faces)){const f=e.faces[side];if(f.texture==null)continue;assert.equal(f.texture,0);const[u,v,U,V]=f.uv,uv=[[U,V],[u,V],[u,v],[U,v]],shift=(f.rotation||0)/90;
  const vertices=p.map((p,i)=>[...rotate(p.map((v,j)=>v-o[j]),e.rotation||[0,0,0]).map((v,j)=>v+o[j]),uv[(i-shift+4)%4][0]/model.resolution.width,uv[(i-shift+4)%4][1]/model.resolution.height]);
  const[a,b,c]=vertices,ab=b.slice(0,3).map((v,i)=>v-a[i]),ac=c.slice(0,3).map((v,i)=>v-a[i]);let normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],len=Math.hypot(...normal);if(len<1e-8)continue;
  quads.push({vertices,normal:normal.map(v=>v/len)});
 }return{name:e.name,visible:e.visibility!==false,quads};
});
const animations=clips.map(c=>({name:c.name.split('.').pop(),length:c.length,loop:c.loop==='loop',tracks:Object.values(c.animators).map(a=>({bone:a.name,keys:a.keyframes.map(k=>({channel:k.channel,time:k.time,value:['x','y','z'].map(d=>Number(k.data_points[0][d]))}))}))}));
function write(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);}
// Remove completely hidden cuboid faces offline, preserving every exposed UV and the editable model.
function prune(nodes) {
 let removed=0;
 for(const n of nodes)if(n.children){removed+=prune(n.children);
  const solids=n.children.filter(c=>c.quads&&c.quads.length===6&&c.quads.every(q=>q.normal.filter(v=>Math.abs(v)>.001).length===1)).map(c=>{
   const pts=c.quads.flatMap(q=>q.vertices);return {node:c,lo:[0,1,2].map(i=>Math.min(...pts.map(v=>v[i]))),hi:[0,1,2].map(i=>Math.max(...pts.map(v=>v[i])))};
  });
  for(const c of n.children)if(c.quads)c.quads=c.quads.filter(q=>{
   const axis=q.normal.findIndex(v=>Math.abs(v)>.999);if(axis<0)return true;
   const axes=[0,1,2].filter(i=>i!==axis),plane=q.vertices[0][axis]+q.normal[axis]*.001;
   const lo=axes.map(i=>Math.min(...q.vertices.map(v=>v[i]))),hi=axes.map(i=>Math.max(...q.vertices.map(v=>v[i])));
   let rects=[[lo[0],lo[1],hi[0],hi[1]]];
   for(const b of solids){if(b.node===c||plane<b.lo[axis]||plane>b.hi[axis])continue;
    const [u,v]=axes,clip=[b.lo[u],b.lo[v],b.hi[u],b.hi[v]],next=[];
    for(const r of rects){const x=Math.max(r[0],clip[0]),y=Math.max(r[1],clip[1]),X=Math.min(r[2],clip[2]),Y=Math.min(r[3],clip[3]);
     if(x>=X||y>=Y){next.push(r);continue;}
     if(x>r[0]+.0001)next.push([r[0],r[1],x,r[3]]);if(X<r[2]-.0001)next.push([X,r[1],r[2],r[3]]);
     if(y>r[1]+.0001)next.push([x,r[1],X,y]);if(Y<r[3]-.0001)next.push([x,Y,X,r[3]]);
    }rects=next;if(!rects.length){removed++;return false;}
   }return true;
  });
 }return removed;
}
const nodes=bake(model.outliner),removed=prune(nodes);
write('src/main/resources/assets/kazimod/models/zoan/world_turtle.json',JSON.stringify({sourceHash:crypto.createHash('sha256').update(original).digest('hex'),nodes,animations}));
console.log(`Removed ${removed} fully occluded faces (${removed*4} vertices).`);
write('src/main/resources/assets/kazimod/textures/models/zoan/world_turtle.png',Buffer.from(model.textures[0].source.split(',')[1],'base64'));
write('deliverables/world_turtle/world_turtle_animated.bbmodel',JSON.stringify(model));
assert.deepEqual(model.elements,JSON.parse(original).elements);assert.deepEqual(model.textures,JSON.parse(original).textures);
console.log(`Preserved ${model.elements.length} cubes and original texture; exported ${clips.length} smooth animation clips.`);
