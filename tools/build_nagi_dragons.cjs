// Editable Blockbench mesh sources are authoritative; runtime rigs are baked from the same files.
// Usage: node tools/build_nagi_dragons.cjs [--regenerate] [--only=first]
// Default preserves manual edits to an existing .bbmodel, then re-exports it.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const {materials,atlas}=require('./nagi_dragon_materials.cjs');
const root=path.resolve(__dirname,'..'),ids=['first','blind','cobra','four_eyed','corpse_flame','ice_crystal','spearhead','angel'];
const uid=s=>crypto.createHash('md5').update('nagi-dragons-v2/'+s).digest('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/,'$1-$2-$3-$4-$5');
const add=(a,b)=>a.map((n,i)=>n+b[i]),sub=(a,b)=>a.map((n,i)=>n-b[i]),mul=(a,t)=>a.map(n=>n*t);
const dot=(a,b)=>a.reduce((s,n,i)=>s+n*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const norm=a=>mul(a,1/(Math.hypot(...a)||1)),lerp=(a,b,t)=>a.map((v,i)=>v+(b[i]-v)*t);
const round=a=>a.map(v=>+v.toFixed(5));
const tile=mat=>{let i=materials.indexOf(mat);assert(i>=0,mat);return[i%4*64,Math.floor(i/4)*64];};
function write(file,data){const full=path.join(root,file);fs.mkdirSync(path.dirname(full),{recursive:true});fs.writeFileSync(full,data);}

function build(id){
 let serial=0;const elements=[],groups=[],outliner=[],animations=[],byName=new Map();
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Nagi • '+id.replaceAll('_',' '),model_identifier:'nagi_dragon_'+id,
  credit:'Kazi Additions — articulated dragon summon model',visible_box:[20,12,0],resolution:{width:256,height:256},elements,groups,outliner,textures:[],animations};
 function group(name,origin,parent){const g={name,origin,rotation:[0,0,0],uuid:uid(id+'/group/'+name),visibility:true,export:true,children:[]};
  assert(!byName.has(name),name);byName.set(name,g);groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;}
 // Geometry is authored +Y up, +Z forward. Mesh-local origin is zero; bone pivots are absolute.
 function mesh(name,verts,polygons,parent){
  const vertices=Object.fromEntries(verts.map((v,i)=>['v'+i,round(v)])),faces={};
  for(let i=0;i<polygons.length;i++){
   const p=polygons[i],keys=p.ids.map(v=>'v'+v),[u,v]=tile(p.mat),uv=p.uv||[[.08,.92],[.92,.92],[.92,.08],[.08,.08]];
   assert(keys.length===3||keys.length===4,name);const n=cross(sub(verts[p.ids[1]],verts[p.ids[0]]),sub(verts[p.ids[2]],verts[p.ids[0]]));
   if(Math.hypot(...n)<1e-7)continue;
   faces['f'+i]={vertices:keys,uv:Object.fromEntries(keys.map((k,j)=>[k,[u+1+uv[j][0]*62,v+1+uv[j][1]*62]])),texture:0};
  }
  if(!Object.keys(faces).length)return;
  const e={name,uuid:uid(id+'/mesh/'+serial+++'/'+name),type:'mesh',origin:[0,0,0],rotation:[0,0,0],vertices,faces,
   visibility:true,export:true,color:Math.floor(serial%8),autouv:0};elements.push(e);parent.children.push(e.uuid);return e;
 }
 // Loft elliptical rings along Z; face winding is made explicitly outward.
 function loft(name,rings,sides,mat,parent,phase=0,caps=true){
  const verts=[],polys=[];
  for(const r of rings)for(let s=0;s<sides;s++){let a=s/sides*Math.PI*2+phase;verts.push([r[0]+Math.cos(a)*r[3],r[1]+Math.sin(a)*r[4],r[2]]);}
  for(let k=0;k<rings.length-1;k++)for(let s=0;s<sides;s++){
   let next=(s+1)%sides,idx=[k*sides+s,k*sides+next,(k+1)*sides+next,(k+1)*sides+s];
   const coords=[[s/sides,k/(rings.length-1)],[(s+1)/sides,k/(rings.length-1)],[(s+1)/sides,(k+1)/(rings.length-1)],[s/sides,(k+1)/(rings.length-1)]],uvByVertex=new Map(idx.map((v,i)=>[v,coords[i]]));
   let avg=mul(idx.map(i=>verts[i]).reduce(add,[0,0,0]),.25),center=mul(add(rings[k].slice(0,3),rings[k+1].slice(0,3)),.5);
   if(dot(cross(sub(verts[idx[1]],verts[idx[0]]),sub(verts[idx[2]],verts[idx[0]])),sub(avg,center))<0)idx.reverse();
   const material=typeof mat==='function'?mat(s,k,(s+.5)/sides*Math.PI*2+phase):mat;
   polys.push({ids:idx,mat:material,uv:idx.map(v=>uvByVertex.get(v))});
  }
  for(const end of caps?[0,rings.length-1]:[]){
   const ci=verts.length;verts.push(rings[end].slice(0,3));
   for(let s=0;s<sides;s++){
    let idx=[ci,end*sides+s,end*sides+(s+1)%sides],direction=(end===0?rings[0][2]-rings[1][2]:rings[end][2]-rings[end-1][2]);
    if(cross(sub(verts[idx[1]],verts[idx[0]]),sub(verts[idx[2]],verts[idx[0]]))[2]*direction<0)idx.reverse();
    polys.push({ids:idx,mat:typeof mat==='function'?mat(s,end,0):mat,uv:idx.map(v=>v===ci?[.5,.5]:[.5+.45*Math.cos((v%sides)/sides*Math.PI*2+phase),.5+.45*Math.sin((v%sides)/sides*Math.PI*2+phase)])});
   }
  }
  return mesh(name,verts,polys,parent);
 }
 // Continuous tapered curved horns, feathers and tusks with closed pointed tips.
 function tendril(name,points,widths,mat,parent,sides=6,flat=1){
  let verts=[],polys=[];
  for(let k=0;k<points.length;k++){
   const tangent=norm(sub(points[Math.min(k+1,points.length-1)],points[Math.max(0,k-1)]));
   const u=norm(cross(tangent,Math.abs(tangent[1])>.85?[0,0,1]:[0,1,0])),v=norm(cross(tangent,u));
   for(let s=0;s<sides;s++){const a=s*Math.PI*2/sides;verts.push(add(points[k],add(mul(u,Math.cos(a)*widths[k]),mul(v,Math.sin(a)*widths[k]*flat))));}
  }
  for(let k=0;k<points.length-1;k++)for(let s=0;s<sides;s++){
   const idx=[k*sides+s,k*sides+(s+1)%sides,(k+1)*sides+(s+1)%sides,(k+1)*sides+s],center=lerp(points[k],points[k+1],.5),avg=mul(idx.map(i=>verts[i]).reduce(add,[0,0,0]),.25);
   const coords=[[.05,k/(points.length-1)],[.95,k/(points.length-1)],[.95,(k+1)/(points.length-1)],[.05,(k+1)/(points.length-1)]],uvByVertex=new Map(idx.map((v,i)=>[v,coords[i]]));
   if(dot(cross(sub(verts[idx[1]],verts[idx[0]]),sub(verts[idx[2]],verts[idx[0]])),sub(avg,center))<0)idx.reverse();
   polys.push({ids:idx,mat,uv:idx.map(v=>uvByVertex.get(v))});
  }
  for(const end of [0,points.length-1]){let c=verts.length;verts.push(points[end]);for(let s=0;s<sides;s++){
   const idx=[c,end*sides+s,end*sides+(s+1)%sides],out=norm(sub(points[end],points[end===0?1:end-1]));
   if(dot(cross(sub(verts[idx[1]],verts[idx[0]]),sub(verts[idx[2]],verts[idx[0]])),out)<0)idx.reverse();
   polys.push({ids:idx,mat,uv:idx.map(v=>v===c?[.5,.5]:[.5+.45*Math.cos((v%sides)/sides*Math.PI*2),.5+.45*Math.sin((v%sides)/sides*Math.PI*2)])});}}
  return mesh(name,verts,polys,parent);
 }
 // A raised kite-shaped armor scale, not a floating rectangular decal.
 function plate(name,c,w,l,normal,mat,parent,thickness=.4,conform){
  const n=norm(normal),long=norm(sub([0,0,1],mul(n,n[2]))),wide=norm(cross(long,n));
  const ps=[[-w*.5,0,-l*.34],[0,0,-l*.60],[w*.5,0,-l*.34],[w*.44,0,l*.20],[0,0,l*.5],[-w*.44,0,l*.20],[0,thickness,0]];
  const vs=ps.map(p=>{const v=add(c,add(mul(wide,p[0]),add(mul(n,p[1]),mul(long,p[2]))));return conform?conform(v,p[1]):v;}),polys=[];
  for(let i=0;i<6;i++){let idx=[6,i,(i+1)%6];if(dot(cross(sub(vs[idx[1]],vs[idx[0]]),sub(vs[idx[2]],vs[idx[0]])),n)<0)idx.reverse();
   polys.push({ids:idx,mat,uv:idx.map(k=>[ps[k][0]/w+.5,(ps[k][2]/l+.6)/1.1])});}
  return mesh(name,vs,polys,parent);
 }
 function ellipsoid(name,c,r,mat,parent,sides=10,rings=5){
  // Keep tiny nonzero pole rings to avoid degenerate faces and singular normals.
  const sections=Array.from({length:rings+1},(_,i)=>{let a=-Math.PI/2+.015+(Math.PI-.03)*i/rings;return[c[0],c[1],c[2]+Math.sin(a)*r[2],Math.cos(a)*r[0],Math.cos(a)*r[1]];});
  return loft(name,sections,sides,mat,parent);
 }
 const Root=group('root',[0,0,0]),Head=group('Head',[0,9,-5],Root),Jaw=group('Jaw',[0,6,-4],Head);
 const feathered=id==='angel',ice=id==='ice_crystal',flame=id==='corpse_flame',spear=id==='spearhead',cobra=id==='cobra',blind=id==='blind';
 const length=feathered?104:flame?92:108;
 function center(t){return[Math.sin(t*Math.PI*2)*12,8.5+Math.sin(t*Math.PI*1.65)*6.0,-8-t*length];}
 function radius(t){return (4.9+Math.sin(t*Math.PI)*2.1)*(1-Math.pow(t,1.65))+.22;}
 let previous=Root;
 for(let i=0;i<12;i++){
  const start=i/12,end=(i+1)/12,g=group('body_'+i,center(start),previous);previous=g;
  // Overlapping tapered sockets hide the wedge opened by a rotating child segment.
  const overlap=Math.min(1.35,radius(end)*.42)/length;
  const segmentStart=i===0?start:start-overlap,segmentEnd=i===11?end:end+overlap;
  const sections=Array.from({length:5},(_,j)=>{let t=segmentStart+(segmentEnd-segmentStart)*j/4,c=center(t),r=radius(t);
   // A buried neck collar covers Head's independent nod and forward bite lunge.
   if(i===0&&j===0)c=[0,cobra?11.5:c[1],-2.5];
   return[...c,r,r*(cobra?.94:.86)];});
  loft('Segment '+i+' • scaled hide',sections,12,(s,k,a)=>Math.sin(a)<-.40?'belly':blind||spear?'armor':'scales',g);
  const fine=group('detail_scales_'+i,center(start),g);
  if(i<10)for(let row=0;row<2;row++)for(let side=0;side<5;side++){
   let t=start+(end-start)*(.28+row*.48),a=(side/4)*Math.PI+(row%2?.12:-.06),c=center(t),r=radius(t);
   const n=[Math.cos(a),Math.sin(a),0],p=add(c,[n[0]*(r+.07),n[1]*(r*.86+.07),0]);
   plate('detail_overlap scale '+i+'/'+row+'/'+side,p,Math.max(.7,r*.68),Math.max(1,(end-start)*length*.62),n,side===2?'armor':'scales',fine,.25);
  }
  if(i<11){
   const t=start+(end-start)*.55,c=center(t),r=radius(t),size=(ice?5:flame?6:blind?4.5:spear?3.5:feathered?4:2.9)*(1-t*.6);
   const crest=group('crest_'+i,add(c,[0,r*.83,0]),g);
   tendril('Dorsal '+(ice?'crystal':feathered?'plume':flame?'flame':'spine')+' '+i,[add(c,[0,r*.80,0]),add(c,[0,r*.85+size*.65,-1.1]),add(c,[.25,r*.85+size,-3.8])],[Math.max(.3,r*.25),Math.max(.15,r*.17),.045],feathered?'plume':flame?'glow':'crest',crest,5,.50);
   if(ice||flame||blind||feathered)for(const s of [-1,1]){
    const b=add(c,[s*r*.80,r*.25,0]);
    tendril('Lateral '+i+' '+s,[b,add(b,[s*size*.75,size*.55,-1.6]),add(b,[s*size,size*.40,-4.7])],[r*.20,r*.13,.035],flame?'crest':feathered?'plume':'horn',g,5,.60);
   }
  }
 }

 const hy=cobra?15:flame?11:9;
 if(!flame){
  let profile=spear?[[0,hy,-8,5.6,3.7],[0,hy+1,-3,7,4],[0,hy+1,3,5.2,3.4],[0,hy+.5,12,2.6,1.8],[0,hy,24,.25,.35]]:
   cobra?[[0,hy,-5,4.9,4.3],[0,hy+1,0,6.1,5],[0,hy,6,4.6,3.3],[0,hy-1,13,3.7,1.9]]:
   [[0,hy,-8,5.7,4.8],[0,hy+1,-3,blind?8.2:7.3,5.8],[0,hy+1,3,6.7,4.4],[0,hy,9,5,2.7],[0,hy-.4,15,3.9,1.65]];
  if(!spear){const last=profile[profile.length-1];profile.push([0,last[1]-.12,last[2]+1.1,last[3]*.76,last[4]*.8],[0,last[1]-.2,last[2]+1.7,last[3]*.22,last[4]*.40]);}
  loft('Sculpted cranium and tapered muzzle',profile,12,(s,k,a)=>Math.sin(a)<-.45?'joint':blind||spear?'armor':'scales',Head,Math.PI/12);
  const nose=spear?21:cobra?12.7:14.4;
  loft('Nasal ridge',[[0,hy+1.3,5,1.1,1.2],[0,hy+2,9,1.3,1.2],[0,hy+.3,nose,1,.6]],6,'armor',Head);
  loft('Hinged mandible',[[0,hy-3,-5,4.6,1.4],[0,hy-3.8,3,5.3,1.35],[0,hy-4,10,4.3,1.1],[0,hy-3.6,spear?20:14,spear?1.6:3.3,.75],[0,hy-3.6,spear?22:15.5,.5,.35]],10,'belly',Jaw);
  loft('Dark palate',[[0,hy-2.35,-3,3.6,.4],[0,hy-2.35,6,4.1,.28],[0,hy-2.1,spear?16:13,3.1,.16]],8,'mouth',Head);
  loft('Lower mouth lining',[[0,hy-2.45,-2,3.6,.14],[0,hy-2.5,6,4.4,.18],[0,hy-2.8,spear?18:13,3,.10]],8,'mouth',Jaw);
  loft('Tongue',[[0,hy-2.25,0,1.4,.22],[0,hy-2.2,6,1.7,.25],[0,hy-2.3,11,.9,.12]],8,'tongue',Jaw);
  for(const s of [-1,1]){
   ellipsoid('Nostril shadow '+s,[s*(spear?1.4:2.45),hy+.25,nose+.15],[.55,.3,.26],'joint',Head,8,3);
   for(let t=0;t<7;t++){
    let z=1+t*(spear?2.5:1.75),x=s*(4.9-(z/15)*1.6),top=hy-1.9,len=t===1||t===4?2.15:1.3;
    tendril('Upper serrated fang '+s+'/'+t,[[x,top,z],[x*.96,top-len*.7,z+.1],[x*.91,top-len,z+.55]],[.58,.32,.03],'teeth',Head,5);
    tendril('Lower interlocking fang '+s+'/'+t,[[x*.96,hy-2.6,z+.7],[x*.91,hy-1.8,z+.85],[x*.88,hy-1.4,z+.65]],[.43,.22,.025],'teeth',Jaw,5);
   }
   // Eyeless dragon uses armored sensory plates; cobra has a single frontal eye.
   if(!blind&&!cobra){
    for(let e=0;e<(id==='four_eyed'?2:1);e++){
     const z=1.5-e*4.1,y=hy+2+e*.6,x=s*(spear?6.0:6.5);
     ellipsoid('Recessed eye socket '+s+'/'+e,[x,y,z],[.6,1.45,2.25],'joint',Head,8,4);
     ellipsoid('Eye sclera '+s+'/'+e,[x+s*.32,y,z+.1],[.36,.95,1.6],'eye',Head,8,4);
     ellipsoid('glow_iris '+s+'/'+e,[x+s*.6,y,z+.32],[.17,.78,.75],'iris',Head,8,4);
     ellipsoid('Vertical pupil '+s+'/'+e,[x+s*.74,y,z+.35],[.06,.62,.18],'pupil',Head,6,4);
     ellipsoid('glow_eye catchlight '+s+'/'+e,[x+s*.79,y+.3,z+.55],[.05,.17,.18],'glow',Head,6,3);
     tendril('Heavy angled brow '+s+'/'+e,[[s*4.5,y+1.4,z+1.5],[s*6.1,y+1.2,z+.3],[s*7.4,y+.8,z-2.6]],[1.15,.95,.16],'armor',Head,6,.8);
    }
   }
   if(!spear&&!cobra&&id!=='four_eyed'){
    const h=ice?13:blind?10:feathered?8:9;
    tendril('Swept crown horn '+s,[[s*4.7,hy+4,-3],[s*6.2,hy+7,-5],[s*7.4,hy+h,-9],[s*7.0,hy+h+2,-13]],[1.8,1.4,.8,.035],feathered?'trim':'horn',Head,7);
   }
   if(!cobra)for(let j=0;j<3;j++){
    const p=[s*(5.4+j*.3),hy-1+j*1.7,-4-j*.7];
    tendril('Swept cheek '+s+'/'+j,[p,add(p,[s*(2.5+j*.7),1.4,-3]),add(p,[s*(3.4+j),2.2,-7-j])],[1.45,1.0,.035],feathered?'plume':ice?'crest':'armor',Head,5,.5);
   }
  }
  const facial=group('detail_facial_plates',[0,hy,0],Head);
  // Fit every scale vertex to the actual polygonal cranium, rather than floating
  // flat plates above differently shaped cobra/spearhead skulls.
  function skullSurface(v,raised){
   const z=Math.max(profile[0][2]+.1,Math.min(profile[profile.length-1][2]-.1,v[2]));
   let k=0;while(k<profile.length-2&&profile[k+1][2]<z)k++;
   const a=profile[k],b=profile[k+1],t=(z-a[2])/(b[2]-a[2]),r=lerp(a,b,t);
   const x=Math.max(-r[3]*.9,Math.min(r[3]*.9,v[0])),ring=Array.from({length:12},(_,i)=>{let p=i*Math.PI/6+Math.PI/12;return[Math.cos(p)*r[3],r[1]+Math.sin(p)*r[4]];});
   let y=-Infinity;
   for(let i=0;i<12;i++){const p=ring[i],q=ring[(i+1)%12];if(Math.abs(q[0]-p[0])>1e-7&&x>=Math.min(p[0],q[0])-1e-7&&x<=Math.max(p[0],q[0])+1e-7)y=Math.max(y,p[1]+(q[1]-p[1])*(x-p[0])/(q[0]-p[0]));}
   assert(Number.isFinite(y),'Cranium surface');return[x,y+.055+raised,z];
  }
  for(let row=0;row<3;row++)for(let s=-1;s<=1;s++){
   const z=(cobra?-2.2:-4)+row*4.2,y=hy+5.6-row*.8,x=s*(3.6-row*.4);
   plate('detail_brow plate '+row+'/'+s,[x,y,z],3.6,5.2,[s*.18,1,.12],s===0?'armor':'scales',facial,.35,skullSurface);
  }
  if(blind){
   for(let i=0;i<4;i++){
    let z=6-i*3.9;loft('Blind armored brow band '+i,[[0,hy+2,z-1,5.5,2.8],[0,hy+2.2,z,5.8,3.1],[0,hy+2,z+1,5.4,2.7]],8,'armor',Head);
   }
   tendril('Central blind crown blade',[[0,hy+4,-4],[0,hy+9,-9],[0,hy+11,-19]],[2.0,1.7,.03],'crest',Head,6,.45);
  }
  if(spear){
   tendril('Black spear dorsal blade',[[0,hy+3,-1],[0,hy+10,-8],[0,hy+11,-20]],[2.5,2.2,.03],'armor',Head,5,.45);
   for(const s of [-1,1])tendril('Spearhead swept edge '+s,[[s*4,hy+3,-3],[s*9,hy+5,-10],[s*12,hy+4,-20]],[1.6,1.5,.03],'crest',Head,5,.4);
  }
  if(ice){
   for(let i=0;i<9;i++){let a=i/8*Math.PI,x=Math.cos(a)*5.5,y=hy+Math.sin(a)*4;
    tendril('Glacial mane shard '+i,[[x,y,-7],[x*1.7,y+3,-13],[x*2.1,y+5,-21]],[1.8,1.25,.03],'crest',Head,5,.6);}
  }
  if(id==='first')for(let i=0;i<3;i++){
   const z=3-i*4.2; tendril('Ivory forehead crown '+i,[[0,hy+4.7,z],[0,hy+7.5+i*.9,z-2],[0,hy+9+i,z-5]],[1.05,.8,.03],'horn',Head,6,.6);
  }
  if(id==='four_eyed')for(const s of [-1,1])for(let i=0;i<3;i++){
   let p=[s*(4.2+i*.8),hy+3.5-i*.3,-4-i];tendril('Four-eyed frilled crown '+s+'/'+i,[p,add(p,[s*2.8,3.2,-3]),add(p,[s*4,3.8,-7])],[1.05,.85,.035],'crest',Head,5,.5);
  }
 }

 if(cobra){
  // Broad ribbed hood: sculpted mantle with an inset contrasting membrane and raised radial ribs.
  const hood=group('Hood',[0,10,-8],Root),vs=[[0,12,-6.8]],faces=[];
  const outline=[[0,27,-8],[6,27,-7.5],[11,24,-7],[14.5,20,-6.5],[14.8,14,-6.8],[12,8,-7.5],[7,3,-8.5],[2.8,.5,-9.5],[0,0,-10],[-2.8,.5,-9.5],[-7,3,-8.5],[-12,8,-7.5],[-14.8,14,-6.8],[-14.5,20,-6.5],[-11,24,-7],[-6,27,-7.5]];
  const N=outline.length;vs.push(...outline);
  for(let i=0;i<N;i++){let idx=[0,i+1,(i+1)%N+1];if(cross(sub(vs[idx[1]],vs[0]),sub(vs[idx[2]],vs[0]))[2]<0)idx.reverse();faces.push({ids:idx,mat:'belly',uv:idx.map(k=>[vs[k][0]/30+.5,.5-(vs[k][1]-11)/34])});}
  mesh('Hood front fan',vs,faces,hood);
  const rear=vs.map(v=>add(v,[0,0,-.6]));mesh('Hood scaled back',rear,faces.map(f=>({...f,ids:f.ids.slice().reverse(),mat:'scales'})),hood);
  for(let i=0;i<N;i++){
   let a=vs[i+1],b=vs[(i+1)%N+1];tendril('Hood rim '+i,[a,lerp(a,b,.5),b],[.65,.65,.65],'trim',hood,5);
  }
  for(const s of [-1,1])for(let i=0;i<5;i++){
   let edge=outline[s>0?i+1:15-i];tendril('Hood raised rib '+s+'/'+i,[[s*1.5,3.5,-8.6],[s*(4+i*.8),12,-6.5],edge],[.22,.27,.12],'vein',hood,5);
  }
  ellipsoid('Cyclops dark socket',[0,hy+3.8,5],[2.8,2.9,1.0],'joint',Head,12,6);
  ellipsoid('Cyclops eye',[0,hy+3.8,5.7],[2.2,2.3,.7],'eye',Head,12,6);
  ellipsoid('glow_cyclops iris',[0,hy+3.8,6.32],[1.25,1.4,.22],'iris',Head,12,5);
  ellipsoid('Cyclops vertical pupil',[0,hy+3.8,6.53],[.3,1.1,.10],'pupil',Head,8,5);
  ellipsoid('glow_cyclops highlight',[-.55,hy+4.45,6.65],[.33,.35,.06],'glow',Head,8,4);
  for(const s of [-1,1])tendril('Venom fang '+s,[[s*2.8,hy-2,10],[s*3,hy-5.8,11],[s*2.5,hy-6.8,12]],[.9,.55,.03],'teeth',Head,7);
 }

 if(flame){
  loft('Flame throat',[[0,hy,-9,4.7,5.2],[0,hy,-4,5.2,6.5],[0,hy,3,6.6,7.5]],12,'scales',Head,0,false);
  // Hollow circular maw, including visible inner walls, rather than a sphere or flat disk.
  const vs=[],ps=[],N=16;
  for(const [rx,ry,z] of [[6.6,7.7,4],[7.4,8.5,6],[5.3,6.2,6.4],[3.5,4.2,-1]])for(let i=0;i<N;i++){let a=i*Math.PI*2/N;vs.push([Math.cos(a)*rx,hy+Math.sin(a)*ry,z]);}
  for(let k=0;k<3;k++)for(let i=0;i<N;i++){
   let idx=[k*N+i,k*N+(i+1)%N,(k+1)*N+(i+1)%N,(k+1)*N+i];if(k===0)idx.reverse();
   ps.push({ids:idx,mat:k===0?'crest':k===1?'trim':'mouth',uv:[[i/N,0],[(i+1)/N,0],[(i+1)/N,1],[i/N,1]]});
  }
  mesh('Open fiery maw rim and interior',vs,ps,Head);
  ellipsoid('Black throat depth',[0,hy,-1.3],[3.6,4.3,.5],'mouth',Head,12,5);
  for(let i=0;i<N;i++){
   let a=i/N*Math.PI*2,p=[Math.cos(a)*5.6,hy+Math.sin(a)*6.3,6.3],q=[Math.cos(a)*3.9,hy+Math.sin(a)*4.5,6.9];
   tendril('Maw fang '+i,[p,lerp(p,q,.6),q],[.64,.36,.03],'teeth',Math.sin(a)<-.4?Jaw:Head,5);
  }
  for(let i=0;i<11;i++){
   const a=i/10*Math.PI,p=[Math.cos(a)*5.5,hy+Math.sin(a)*5.8,-1],g=group('flame_'+i,p,Head);
   tendril('Flame outer tongue '+i,[p,add(p,[Math.cos(a)*3,4,-3]),add(p,[Math.cos(a)*5,10+(i%3)*2,-6]),add(p,[Math.cos(a)*3.5,14+(i%3)*2,-9])],[1.8,2.2,1.2,.03],'crest',g,6,.65);
   tendril('glow_flame hot core '+i,[add(p,[0,0,.15]),add(p,[Math.cos(a)*2,4,-2.9]),add(p,[Math.cos(a)*3.5,10,-5.8])],[.65,.9,.03],'glow',g,5,.45);
  }
 }

 if(feathered){
  // Individually layered flight feathers; wings and neck mane remain rigged, solid meshes.
  for(const s of [-1,1]){
   const wing=group(s<0?'wing_left':'wing_right',[s*4,10,-12],Root);
   tendril('Feathered wing leading edge '+s,[[s*4,10,-12],[s*17,15,-19],[s*31,17,-29]],[2.5,2,.06],'plume',wing,7,.5);
   for(let row=0;row<3;row++)for(let f=0;f<8;f++){
    let x=s*(8+f*2.8),y=11+f*.65-row*.65,z=-13-f*1.9-row*2;
    tendril('Wing layered feather '+s+'/'+row+'/'+f,[[x,y,z],[x+s*(3.2+row),y-.3,z-6],[x+s*(4+row),y-1,z-12-row*3]],[1.5-row*.18,1.7-row*.14,.035],row===0?'plume':'belly',wing,6,.17);
    if(row===0)tendril('detail_feather rachis '+s+'/'+f,[[x,y+.22,z],[x+s*3,y-.07,z-6],[x+s*4,y-.85,z-11]],[.12,.1,.025],'trim',wing,5,.6);
   }
   for(let j=0;j<7;j++){
    let p=[s*(4.5+j*.25),hy+1.5-j*.7,-4-j*.9];
    tendril('Cascading neck feather '+s+'/'+j,[p,add(p,[s*(5+j*.25),2,-7]),add(p,[s*(7+j*.6),.5,-14])],[1.8,2,.035],'plume',Head,6,.16);
   }
  }
 }

 function clip(name,length,loop,tracks){
  const animators={};
  for(const [bone,channels] of Object.entries(tracks)){
   const g=byName.get(bone);assert(g,bone);const keyframes=[];
   for(const [channel,fn] of Object.entries(channels))for(let i=0;i<=24;i++){
    const time=length*i/24,value=fn(i/24).map(v=>Math.abs(v)<1e-7?0:+v.toFixed(5));
    keyframes.push({uuid:uid(id+'/anim/'+name+'/'+bone+'/'+channel+'/'+i),channel,time,
     interpolation:'linear',data_points:[{x:String(value[0]),y:String(value[1]),z:String(value[2])}]});
   }
   animators[g.uuid]={name:bone,type:'bone',keyframes};
  }
  animations.push({uuid:uid(id+'/animation/'+name),name:'animation.nagi_dragon_'+id+'.'+name,length,loop:loop?'loop':'once',override:false,animators});
 }
 const swim={Head:{rotation:p=>[1.3*Math.sin(p*Math.PI*2),1.3*Math.cos(p*Math.PI*2),0]},Jaw:{rotation:p=>[4+1.4*Math.sin(p*Math.PI*2),0,0]}};
 for(let i=0;i<12;i++)swim['body_'+i]={rotation:p=>[3.1*Math.sin(p*Math.PI*2-i*.52),(5.6+i*.17)*Math.sin(p*Math.PI*2-i*.60),.8*Math.sin(p*Math.PI*2-i*.45)]};
 for(let i=0;i<11;i++)swim['crest_'+i]={rotation:p=>[(flame?6:2)*Math.sin(p*Math.PI*2-i*.7),0,0]};
 if(feathered){swim.wing_left={rotation:p=>[2*Math.sin(p*Math.PI*2),0,7*Math.sin(p*Math.PI*2)]};swim.wing_right={rotation:p=>[2*Math.sin(p*Math.PI*2),0,-7*Math.sin(p*Math.PI*2)]};}
 if(cobra)swim.Hood={rotation:p=>[0,1.8*Math.sin(p*Math.PI*2),1.3*Math.cos(p*Math.PI*2)]};
 if(flame)for(let i=0;i<11;i++)swim['flame_'+i]={rotation:p=>[5*Math.sin(p*Math.PI*2+i),3*Math.cos(p*Math.PI*2+i),0]};
 clip('swim',2.8,true,swim);
 const pulse=p=>{let t=p<.24?p/.24:p<.42?1:1-(p-.42)/.58;return Math.max(0,t)*Math.max(0,t)*(3-2*Math.max(0,t));};
 clip('bite',.6,false,{Head:{rotation:p=>[-7*pulse(p),0,0],position:p=>[0,.5*pulse(p),2.6*pulse(p)]},Jaw:{rotation:p=>[(flame?12:26)*pulse(p),0,0]}});
 return model;
}

function rotate(p,r){let[x,y,z]=p;for(let i=0;i<3;i++){let a=(r[i]||0)*Math.PI/180,c=Math.cos(a),s=Math.sin(a);if(i===0)[y,z]=[y*c-z*s,y*s+z*c];if(i===1)[x,z]=[x*c+z*s,-x*s+z*c];if(i===2)[x,y]=[x*c-y*s,x*s+y*c];}return[x,y,z];}
function bake(model){
 const elements=new Map(model.elements.map(e=>[e.uuid,e])),groups=new Map(model.groups.map(g=>[g.uuid,g]));
 return function walk(nodes){return nodes.map(item=>{
  if(typeof item!=='string'){let g=groups.get(item.uuid)||item;return{name:g.name,origin:g.origin,rotation:g.rotation,visible:g.visibility!==false,children:walk(item.children)};}
  const e=elements.get(item);assert(e&&e.type==='mesh','Mesh-only export expected');let quads=[];
  for(const face of Object.values(e.faces)){
   if(face.texture===null)continue;
   const vertices=face.vertices.map(k=>[...round(add(rotate(e.vertices[k],e.rotation),e.origin)),face.uv[k][0]/256,face.uv[k][1]/256]);
   const normal=norm(cross(sub(vertices[1].slice(0,3),vertices[0].slice(0,3)),sub(vertices[2].slice(0,3),vertices[0].slice(0,3))));
   if(vertices.length===3)vertices.push([...vertices[2]]);
   assert(vertices.length===4,'Faces must be tris/quads');quads.push({vertices,normal});
  }
  return{name:e.name,visible:e.visibility!==false,quads};
 });}(model.outliner);
}
function clips(model){
 const modern=parseFloat(model.meta.format_version)>=5;
 return model.animations.map(a=>({name:a.name.split('.').pop(),length:a.length,loop:a.loop==='loop',tracks:Object.entries(a.animators).map(([uuid,anim])=>({
  bone:model.groups.find(g=>g.uuid===uuid)?.name||anim.name,keys:anim.keyframes.filter(k=>['rotation','position'].includes(k.channel)).map(k=>{
   const value=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));assert(value.every(Number.isFinite),'Only numeric animation keys supported');
   if(modern)for(let i=0;i<(k.channel==='rotation'?2:1);i++)value[i]=-value[i];
   return{channel:k.channel,time:k.time,value};
  })
 }))}));
}
async function main(){
 const only=process.argv.find(a=>a.startsWith('--only='))?.slice(7),regenerate=process.argv.includes('--regenerate');
 for(const id of ids.filter(id=>!only||id===only)){
  const relative='deliverables/nagi_dragons/'+id+'.bbmodel',source=path.join(root,relative);
  const model=!regenerate&&fs.existsSync(source)?JSON.parse(fs.readFileSync(source,'utf8')):build(id);
  const png=!regenerate&&model.textures[0]?.source?Buffer.from(model.textures[0].source.split(',')[1],'base64'):await atlas(id);
  model.textures=[{uuid:uid(id+'/texture'),name:id+'.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,
   source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true,saved:true,visible:true}];
  const text=JSON.stringify(model),rig={sourceHash:crypto.createHash('sha256').update(text).digest('hex'),nodes:bake(model),animations:clips(model)};
  write(relative,text);write('deliverables/nagi_dragons/'+id+'.png',png);
  write('src/main/resources/assets/kazimod/models/nagi/dragons/'+id+'.json',JSON.stringify(rig));
  write('src/main/resources/assets/kazimod/textures/models/nagi/dragons/'+id+'.png',png);
  console.log(id+': '+model.elements.length+' mesh parts, '+model.groups.length+' bones, '+model.elements.reduce((n,e)=>n+Object.keys(e.faces).length,0)+' faces');
 }
}
module.exports={build,bake,clips,ids};
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});
