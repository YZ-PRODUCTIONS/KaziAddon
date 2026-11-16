// One source of truth for the editable Blockbench cuboids and Java ModelRenderer rig.
// Usage: node tools/build_mountain_goat.cjs
// Regenerates only the Mountain Goat assets; keep hand edits in this source.
'use strict';
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require('C:/Users/th3wyz/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..');
const material={fur:'e4e4db',light:'f3f1e6',shade:'c3c6bf',horn:'353d41',ridge:'596269',hoof:'34383b',eye:'cfa962',pupil:'1b2328',nose:'5a6264',ear:'bdafa6'};
const names=Object.keys(material),joints=[],cubes=[];
const uid=s=>crypto.createHash('md5').update('mountain-goat/'+s).digest('hex').replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/,'$1-$2-$3-$4-$5');
const add=(a,b)=>a.map((v,i)=>v+b[i]),sub=(a,b)=>a.map((v,i)=>v-b[i]),mul=(a,v)=>a.map(n=>n*v);
const dot=(a,b)=>a.reduce((v,n,i)=>v+n*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const norm=a=>mul(a,1/Math.hypot(...a));
const round=v=>+v.toFixed(5),vec=a=>a.map(round);
function write(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);}
function joint(name,pos,parent='root'){const j={name,pos,parent};joints.push(j);return j;}
joint('root',[0,24,0],null);joint('body',[0,-13,0]);joint('head',[0,-18,-9]);
joint('frontLeftLeg',[3.55,-10,-6]);joint('frontRightLeg',[-3.55,-10,-6]);
joint('hindLeftLeg',[3.55,-10,6]);joint('hindRightLeg',[-3.55,-10,6]);joint('tail',[0,-14,8]);
function cube(name,parent,center,size,mat='fur',rotation=[0,0,0]){
 assert(names.includes(mat),mat);assert(size.every(v=>v>0),name);
 cubes.push({name,parent,center,size,mat,rotation});
}
// Compact quadruped silhouette, raised shoulders, layered shag rather than a sheep's wool ball.
cube('Ribcage','body',[0,-.6,0],[10,8.8,16.5]);
cube('Withers','body',[0,-3.25,-4.4],[9.6,5.1,8.3],'light',[-7,0,0]);
cube('Hindquarters','body',[0,-.25,5.7],[9.2,8.1,6]);
cube('Shoulder mane','body',[0,.5,-7.3],[10.6,9.3,5.2]);
cube('Brisket','body',[0,3.5,-7.8],[7.1,7,4.2],'light',[-12,0,0]);
cube('Back ridge','body',[0,-4.8,1.2],[7.5,1.4,12.5],'light');
cube('Underbelly','body',[0,4.1,.3],[7.5,2.8,12.2],'shade');
for(const s of [-1,1]){
 cube('Shoulder fringe '+s,'body',[s*4.7,3.7,-6.3],[2.2,4.8,3.4],'light',[0,0,s*13]);
 cube('Side coat forward '+s,'body',[s*4.5,2.9,-1.8],[2.1,4.2,5.6],'fur',[0,0,s*7]);
 cube('Side coat rear '+s,'body',[s*4.35,3.2,3.3],[2.2,3.7,4.7],'light',[0,0,s*10]);
 cube('Haunch tuft '+s,'body',[s*4.2,3.6,7],[2.2,3.8,3.1],'fur',[12,0,s*12]);
}
// Legs have compact shaggy upper sections and hard split hooves.
for(const name of ['frontLeftLeg','frontRightLeg','hindLeftLeg','hindRightLeg']){
 const hind=name.startsWith('hind');
 cube(name+' upper',name,[0,1.5,0],[3.7,5.3,4.1],'fur',[hind?-8:3,0,0]);
 cube(name+' knee',name,[0,4.55,hind?.5:0],[2.7,2.5,2.9],'light');
 cube(name+' cannon',name,[0,6.5,hind?.25:0],[2.1,3.2,2.25],'shade');
 cube(name+' fetlock',name,[0,7.45,-.12],[2.7,1.7,2.9],'fur');
 for(const s of [-1,1])cube(name+' cloven toe '+s,name,[s*.72,9,-.35],[1.25,2,3.2],'hoof');
}
cube('Tail base','tail',[0,-.6,1.6],[3.3,3.1,4],'fur',[-28,0,0]);
cube('Tail tuft','tail',[0,-1.8,3.3],[2.7,3,3],'light',[-32,0,0]);
// High carriage, slim long muzzle and the mountain goat's characteristic chin beard.
cube('Neck ruff','head',[0,2,1.5],[7.6,8.4,6.3],'light',[-15,0,0]);
cube('Cranium','head',[0,-.45,-1.1],[5.8,6.5,6.4],'fur',[-5,0,0]);
cube('Forehead','head',[0,-2.2,-3],[5.2,3.4,3.1],'light',[-12,0,0]);
cube('Nasal bridge','head',[0,.65,-4.6],[4.2,3.7,4],'fur',[-15,0,0]);
cube('Muzzle','head',[0,1.9,-6.2],[4,2.4,2.8],'shade',[-8,0,0]);
cube('Nose','head',[0,1.25,-7.58],[3.35,1.2,.32],'nose',[-8,0,0]);
cube('Lower jaw','head',[0,2.9,-4.35],[3.9,1.25,4.1],'light',[-7,0,0]);
cube('Beard root','head',[0,3.5,-3.3],[3.2,2.7,2.2],'light',[-16,0,0]);
cube('Beard middle','head',[0,5.15,-2.8],[2.5,2.4,1.75],'fur',[-16,0,0]);
cube('Beard tip','head',[0,6.55,-2.3],[1.35,1.7,1.25],'light',[-20,0,0]);
for(const s of [-1,1]){
 cube('Cheek '+s,'head',[s*2.55,1.05,-1.9],[1.3,3.3,3.3],'light',[0,0,s*10]);
 cube('Ear '+s,'head',[s*4,-1.15,.4],[3.3,1.8,2.2],'fur',[0,s*12,s*-16]);
 cube('Ear inset '+s,'head',[s*4.05,-2.01,.25],[2.25,.15,1.25],'ear',[0,s*12,s*-16]);
 cube('Eye socket '+s,'head',[s*2.91,-.7,-3.05],[.26,1.45,1.7],'nose');
 cube('Amber iris '+s,'head',[s*3.06,-.7,-3.08],[.15,1.08,1.4],'eye');
 cube('Horizontal pupil '+s,'head',[s*3.15,-.7,-3.1],[.08,.31,1.16],'pupil');
 cube('Eye glint '+s,'head',[s*3.2,-.99,-3.44],[.06,.19,.21],'light');
 cube('Brow '+s,'head',[s*2.8,-1.7,-3],[.7,.7,2.2],'light',[0,0,s*8]);
 cube('Nostril '+s,'head',[s*1.12,1.1,-7.8],[.63,.38,.1],'pupil');
 // Four tapering segments stand up and sweep backward; no coiled ram horns.
 const horn=[[[s*1.95,-4,.1],[1.65,3.2,1.8],-7],[[s*2.12,-6.65,.65],[1.4,2.8,1.5],-16],[[s*2.25,-8.9,1.65],[1.1,2.4,1.2],-30],[[s*2.23,-10.5,3.05],[.72,2.1,.84],-49],[[s*2.13,-11.25,4.25],[.38,1.3,.47],-65]];
 horn.forEach(([p,size,angle],i)=>cube('Tapered horn '+s+' '+i,'head',p,size,'horn',[angle,0,s*-3]));
 for(let i=0;i<3;i++)cube('Horn growth ridge '+s+' '+i,'head',[s*(2+i*.06),-4.55-i*1.4,.14+i*.3],[1.65-i*.17,.22,1.79-i*.13],'ridge',[-8-i*5,0,s*-3]);
}

const absJoint=new Map();for(const j of joints)absJoint.set(j.name,j.parent?add(absJoint.get(j.parent),j.pos):j.pos);
// Reflect the Java Y-down rig into Blockbench's Y-up editor coordinates.
const toBB=p=>[p[0],24-p[1],p[2]];
const atlasSize=256,tileSize=64;
const atlas=Buffer.alloc(atlasSize*atlasSize*4);
for(let y=0;y<atlasSize;y++)for(let x=0;x<atlasSize;x++){
 const mi=Math.floor(y/tileSize)*4+Math.floor(x/tileSize),hex=material[names[mi]]||'e4e4db';
 const c=hex.match(/../g).map(v=>parseInt(v,16)),p=(y*atlasSize+x)*4;
 c.forEach((v,i)=>atlas[p+i]=v);atlas[p+3]=255;
}
function texOffset(c){const i=names.indexOf(c.mat);return[i%4*64,Math.floor(i/4)*64];}
function faces(c){const[u,v]=texOffset(c);return Object.fromEntries(['north','south','east','west','up','down'].map(side=>[side,{uv:[u+2,v+2,u+8,v+8],texture:0}]));}
function group(j){return {name:j.name,origin:toBB(absJoint.get(j.name)),rotation:[0,0,0],uuid:uid('joint/'+j.name),visibility:true,export:true,children:[]};}
const groups=joints.map(group),byGroup=new Map(groups.map(g=>[g.name,g]));
for(const j of joints)if(j.parent)byGroup.get(j.parent).children.push(byGroup.get(j.name));
const elements=cubes.map(c=>{
 const center=add(absJoint.get(c.parent),c.center),lo=sub(center,mul(c.size,.5)),hi=add(center,mul(c.size,.5));
 const e={name:c.name,uuid:uid('cube/'+c.name),type:'cube',box_uv:false,autouv:0,from:[lo[0],24-hi[1],lo[2]],to:[hi[0],24-lo[1],hi[2]],origin:toBB(center),rotation:[-c.rotation[0],c.rotation[1],-c.rotation[2]],faces:faces(c),visibility:true,export:true,shade:true};
 byGroup.get(c.parent).children.push(e.uuid);return e;
});
const animations=[];
function clip(name,length,loop,tracks){const animators={};for(const[bone,fn]of Object.entries(tracks)){
 const keyframes=[];for(let i=0;i<=16;i++){const p=i/16,rot=fn(p);keyframes.push({channel:'rotation',time:p*length,data_points:[{x:String(-rot[0]),y:String(rot[1]),z:String(-rot[2])}],interpolation:'linear',uuid:uid(name+bone+i)});}
 animators[uid('joint/'+bone)]={name:bone,type:'bone',keyframes};
 }animations.push({name:'animation.mountain_goat.'+name,uuid:uid('animation/'+name),length,loop:loop?'loop':'once',animators});}
const wave=p=>Math.sin(p*Math.PI*2);
clip('idle',4,true,{head:p=>[1.2*wave(p),1.8*wave(p+.2),0],tail:p=>[-2*wave(p),5*wave(p),0]});
clip('walk',1,true,{frontLeftLeg:p=>[24*wave(p),0,0],frontRightLeg:p=>[-24*wave(p),0,0],hindLeftLeg:p=>[-24*wave(p),0,0],hindRightLeg:p=>[24*wave(p),0,0],head:p=>[2*wave(p*2),0,0],tail:p=>[0,7*wave(p),0]});
clip('ram',.8,false,{head:p=>[34*Math.sin(p*Math.PI),0,0],body:p=>[3*Math.sin(p*Math.PI),0,0],frontLeftLeg:p=>[-30*Math.sin(p*Math.PI),0,0],frontRightLeg:p=>[-30*Math.sin(p*Math.PI),0,0],hindLeftLeg:p=>[21*Math.sin(p*Math.PI),0,0],hindRightLeg:p=>[21*Math.sin(p*Math.PI),0,0]});
clip('leap',1,false,{frontLeftLeg:p=>[-42*Math.sin(p*Math.PI),0,0],frontRightLeg:p=>[-42*Math.sin(p*Math.PI),0,0],hindLeftLeg:p=>[32*Math.sin(p*Math.PI),0,0],hindRightLeg:p=>[32*Math.sin(p*Math.PI),0,0],head:p=>[-9*Math.sin(p*Math.PI),0,0],tail:p=>[-12*Math.sin(p*Math.PI),0,0]});
const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Mountain Goat — Yagi Yagi no Mi',model_identifier:'mountain_goat',credit:'Kazi Additions • Mountain Goat full Zoan form',visible_box:[4,4,0],resolution:{width:atlasSize,height:atlasSize},elements,groups:groups.map(g=>({...g,children:undefined})),outliner:[byGroup.get('root')],textures:[],animations};

function f(v){return Number.isInteger(v)?v+'.0F':round(v)+'F';}
let java='package net.kazi.kazimod.client.models.morphs;\n\nimport net.minecraft.client.renderer.model.Model;\nimport net.minecraft.client.renderer.model.ModelRenderer;\n\n/** Generated with the editable Blockbench source by tools/build_mountain_goat.cjs. */\npublic final class MountainGoatGeometry {\n';
for(const j of joints)java+='    public final ModelRenderer '+j.name+';\n';
java+='\n    public MountainGoatGeometry(Model owner) {\n        owner.texWidth = 256;\n        owner.texHeight = 256;\n';
for(const j of joints){java+='        '+j.name+' = new ModelRenderer(owner);\n';if(j.parent)java+='        '+j.parent+'.addChild('+j.name+');\n';}
for(let i=0;i<cubes.length;i++){
 const c=cubes[i],[u,v]=texOffset(c),pos=sub(c.center,mul(c.size,.5));
 if(c.rotation.some(v=>v!==0)){
  java+='        ModelRenderer detail'+i+' = new ModelRenderer(owner);\n        '+c.parent+'.addChild(detail'+i+');\n        detail'+i+'.setPos('+c.center.map(f).join(', ')+');\n';
  for(const[a,j]of[['x',0],['y',1],['z',2]])if(c.rotation[j])java+='        detail'+i+'.'+a+'Rot = '+f(c.rotation[j]*Math.PI/180)+';\n';
  java+='        detail'+i+'.texOffs('+u+', '+v+').addBox('+[...mul(c.size,-.5),...c.size,0].map(f).join(', ')+', false);\n';
 }else java+='        '+c.parent+'.texOffs('+u+', '+v+').addBox('+[...pos,...c.size,0].map(f).join(', ')+', false);\n';
}
java+='        resetPose();\n    }\n\n    public void resetPose() {\n';
for(const j of joints)java+='        '+j.name+'.setPos('+j.pos.map(f).join(', ')+');\n        '+j.name+'.xRot = '+j.name+'.yRot = '+j.name+'.zRot = 0.0F;\n';
java+='    }\n}\n';
// Render the exact cuboid geometry to a depth-buffered orthographic review image.
function rotate(p,r){let[x,y,z]=p;const[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
const quads=[];
for(const c of cubes){const h=mul(c.size,.5),pts=[[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]].map(p=>toBB(add(add(absJoint.get(c.parent),c.center),rotate(p.map((n,i)=>n*h[i]),c.rotation))));
 const indices=[[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[0,1,5,4],[3,7,6,2]];
 for(const ids of indices){let vs=ids.map(i=>pts[i]);let n=norm(cross(sub(vs[1],vs[0]),sub(vs[2],vs[0])));n=mul(n,-1);quads.push({vs,n,mat:c.mat,parent:c.parent});}
}
function render(width,height,camera,headOnly=false){
 const data=Buffer.alloc(width*height*4),depth=new Float32Array(width*height).fill(-Infinity);
 const cam=norm(camera),right=norm(cross([0,1,0],cam)),up=norm(cross(cam,right));
 const filtered=headOnly?quads.filter(q=>q.parent==='head'):quads;
 const coords=filtered.flatMap(q=>q.vs.map(p=>[dot(p,right),dot(p,up),dot(p,cam)]));
 const lo=[Math.min(...coords.map(p=>p[0])),Math.min(...coords.map(p=>p[1]))],hi=[Math.max(...coords.map(p=>p[0])),Math.max(...coords.map(p=>p[1]))];
 const scale=Math.min(width/(hi[0]-lo[0]),height/(hi[1]-lo[1]))*.88,cx=(lo[0]+hi[0])/2,cy=(lo[1]+hi[1])/2;
 const project=p=>[(dot(p,right)-cx)*scale+width/2,height/2-(dot(p,up)-cy)*scale,dot(p,cam)];
 const light=norm([-.3,.85,-.65]);
 for(const q of filtered){if(dot(q.n,cam)<=0)continue;const ps=q.vs.map(project),base=material[q.mat].match(/../g).map(v=>parseInt(v,16)),shade=.58+.42*Math.max(0,dot(q.n,light)),rgb=base.map(v=>Math.round(v*shade));
  for(const ids of [[0,1,2],[0,2,3]]){
   const[a,b,c]=ids.map(i=>ps[i]),den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-7)continue;
   const minX=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))),maxX=Math.min(width-1,Math.ceil(Math.max(a[0],b[0],c[0]))),minY=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))),maxY=Math.min(height-1,Math.ceil(Math.max(a[1],b[1],c[1])));
   for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){
    const u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,w=1-u-v;if(u<0||v<0||w<0)continue;
    const z=u*a[2]+v*b[2]+w*c[2],index=y*width+x;if(z<=depth[index])continue;depth[index]=z;rgb.forEach((n,i)=>data[index*4+i]=n);data[index*4+3]=255;
   }
  }
 }
 return sharp(data,{raw:{width,height,channels:4}});
}
async function main(){
 const png=await sharp(atlas,{raw:{width:atlasSize,height:atlasSize,channels:4}}).png().toBuffer();
 model.textures.push({name:'mountain_goat.png',id:'0',uuid:uid('texture'),path:'mountain_goat.png',relative_path:'mountain_goat.png',source:'data:image/png;base64,'+png.toString('base64'),width:atlasSize,height:atlasSize,uv_width:atlasSize,uv_height:atlasSize,mode:'bitmap',render_mode:'default',visible:true});
 write('src/main/resources/assets/kazimod/textures/models/mountain_goat.png',png);
 write('deliverables/mountain_goat/mountain_goat.png',png);
 write('deliverables/mountain_goat/mountain_goat.bbmodel',JSON.stringify(model,null,2));
 write('src/main/java/net/kazi/kazimod/client/models/morphs/MountainGoatGeometry.java',java);
 for(const [name,cam]of[['preview',[.8,.45,-1]],['side',[1,.17,.08]],['rear',[.8,.35,1]]]){
  write('deliverables/mountain_goat/'+name+'.png',await render(1024,1024,cam).png().toBuffer());
 }
 write('src/main/resources/assets/kazimod/textures/abilities/mountain_goat_transformation.png',await render(512,512,[.9,.22,-1],true).resize(128,128).png().toBuffer());
 assert(cubes.length<=140);assert.equal(new Set(elements.map(e=>e.uuid)).size,elements.length);
 for(const c of cubes){const[u,v]=texOffset(c),[dx,dy,dz]=c.size;assert(u+2*(dx+dz)<u+64,c.name+' UV width');assert(v+dy+dz<v+64,c.name+' UV height');}
 write('deliverables/mountain_goat/manifest.json',JSON.stringify({cuboids:cubes.length,joints:joints.map(j=>j.name),texture:[256,256],coordinates:'Java Y down, feet y=24; forward -Z. Blockbench Y up, feet y=0.',animations:animations.map(a=>a.name),java:'src/main/java/net/kazi/kazimod/client/models/morphs/MountainGoatGeometry.java',source:'tools/build_mountain_goat.cjs',preview:'Software rendered from the same cuboids, with a depth buffer; not an in-game screenshot.'},null,2));
 console.log('Generated '+cubes.length+' cuboids, '+joints.length+' joints, '+animations.length+' editable animation clips, matching Java geometry, texture, form icon and three previews.');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
