// Reproducible, low-poly Blockbench assets and the matching runtime rigs.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=process.argv[2]||path.resolve(__dirname,'..');
const uid=s=>{const h=crypto.createHash('md5').update(s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
const clamp=x=>Math.max(0,Math.min(1,x)),smooth=x=>(x=clamp(x))*x*(3-2*x),sin=p=>Math.sin(p*Math.PI*2);
const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,d);};
const materials=['fur','white','skin','belly','horn','dark','eye','mouth','gold','symbol','tongue','hoof','nose','mane','teeth','mark'];
const colors={totality:['26303b','c3d0d2','677681','b6c5cc','d8d4b5','111821','d9bb3d','4c1629','9a343b','262a36','9b425a','353c48','141e27','18222e','ede9d2','a33343'],elephant:['ca91a4','edbdc0','c88c9e','e2a7b5','e6dcc3','7b3d59','c0aa58','6c2c43','b49a38','cd9caa','9c5269','323646','804257','a9607e','eee4cc','b59a3f'],toad:['4c6531','bdc1a2','587838','d9d8af','e7e8d4','223225','d7c26b','79394e','91963d','d9d8af','8a568f','314728','45552b','3f5628','e1d9b4','222629'],ox:['262326','a2a19c','41373c','51434b','d4c5a3','18171b','c3c7b4','4e2530','8a7250','e2d9c9','865467','373238','625457','1b191e','d9cfb8','211e23']};
function atlas(kind){
 const size=256,tile=64,data=Buffer.alloc(size*size*4);let seed=1234;
 const rand=()=>((seed=Math.imul(seed,1664525)+1013904223>>>0)/4294967296);
 const pixel=(x,y,c)=>{if(x<0||x>=size||y<0||y>=size)return;const p=(y*size+x)*4;data[p]=c[0];data[p+1]=c[1];data[p+2]=c[2];data[p+3]=255;};
 const rgb=h=>h.match(/../g).map(c=>parseInt(c,16));
 for(let m=0;m<16;m++){
  const base=rgb(colors[kind][m]),ox=m%4*tile,oy=(m>>2)*tile;
  for(let y=0;y<64;y++)for(let x=0;x<64;x++){
   const coarse=(Math.sin(Math.floor(x/3)*7+Math.floor(y/4)*13)+Math.cos(Math.floor(x/2)*11-Math.floor(y/3)*5))*2.1;
   const strand=(m===0||m===13)?Math.sin(Math.floor(x/3)*2.7+y*.19)*4:Math.sin(y*.35+x*.08)*2;
   const v=coarse+strand+(rand()-.5)*5+Math.cos((x-25)/43)*3;
   pixel(ox+x,oy+y,base.map(c=>Math.max(0,Math.min(255,Math.round(c+v)))));
  }
  const dot=(x,y,c,r=1)=>{c=typeof c==='string'?rgb(c):c;for(let a=-r;a<=r;a++)for(let b=-r;b<=r;b++)pixel(ox+x+a,oy+y+b,c);};
  const line=(x,y,X,Y,c,w=1)=>{let n=Math.max(Math.abs(X-x),Math.abs(Y-y));for(let i=0;i<=n;i++)dot(Math.round(x+(X-x)*i/n),Math.round(y+(Y-y)*i/n),c,w);};
  const ring=(cx,cy,r,c,w=1)=>{for(let a=0;a<360;a++){let t=a*Math.PI/180;dot(Math.round(cx+Math.cos(t)*r),Math.round(cy+Math.sin(t)*r),c,w);}};
  if(m===0||m===13||m===1){
   for(let i=0;i<80;i++){let x=3+Math.floor(rand()*56),y=3+Math.floor(rand()*52);let c=base.map(v=>Math.max(0,v-12));line(x,y,x+(i%3-1)*2,y+4+Math.floor(rand()*6),c,0);}
  }
  if(m===2&&(kind==='elephant'||kind==='ox'))for(let i=0;i<8;i++){let y=5+i*7;line(4,y,20,y+2,base.map(v=>v-15),0);line(22,y+2,45,y,base.map(v=>v-12),0);}
  if(m===6){ring(32,32,24,'161a20',3);ring(32,32,17,kind==='totality'?'eccb4b':kind==='toad'?'cedfdc':'b7b99b',3);if(kind==='toad')line(19,32,45,32,'11161d',4);else line(32,20,32,44,'11161d',4);dot(24,20,'f5f2dc',3);}
  if(m===7){line(5,18,58,18,'1a1019',4);line(5,47,58,47,'27151f',4);}
  if(m===4||m===14){for(let i=0;i<5;i++)line(3,10+i*11,60,7+i*11,base.map(v=>v-12),0);}
  if(m===9||m===15){
   if(kind==='elephant'){
    ring(32,21,17,'806d28',2);ring(32,21,13,'d2bc64',2);line(32,4,32,54,'b49b3b',2);
    line(19,44,45,44,'a28a32',2);line(19,44,12,54,'a28a32',2);line(45,44,52,54,'a28a32',2);
    for(let x=6;x<64;x+=12){dot(x,4,'b79f42',2);dot(x,60,'b79f42',2);}
   }else if(kind==='toad'){
    ring(32,32,27,'272c2b',2);line(16,29,48,29,'272c2b',1);line(32,15,32,29,'272c2b',1);
    for(const[x,y]of[[16,29],[48,29],[32,15],[23,43],[41,43],[32,53]])dot(x,y,'242a29',3);
   }else if(kind==='totality'){
    for(const[x,y]of[[32,10],[24,24],[40,24],[32,39],[20,51],[44,51]]){dot(x,y,'111923',4);dot(x,y,'b64451',2);}
   }else{
    for(const[x,y]of[[17,15],[47,15],[17,47],[47,47]]){line(32,32,x,y,'29252b',1);ring(x,y,3,'29252b',1);}ring(32,32,6,'29252b',1);
   }
  }
 }
 return sharp(data,{raw:{width:size,height:size,channels:4}}).png().toBuffer();
}
function create(id,kind){
 let serial=0;const elements=[],groups=[],outliner=[],animations=[],clips=[];
 const model={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:id,model_identifier:id,visible_box:[8,8,0],resolution:{width:256,height:256},elements,groups,outliner,textures:[],animations};
 const group=(name,origin=[0,0,0],parent=null)=>{const g={name,origin,rotation:kind==='totality'&&/^Hand (-1|1)$/.test(name)?[0,180,0]:[0,0,0],uuid:uid(id+'/bone/'+name),export:true,visibility:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const cube=(name,c,s,mat,parent,rotation=[0,0,0],origin=c,overrides={})=>{
  const index=materials.indexOf(mat);assert(index>=0,mat);let faces={};
  for(const side of ['north','south','east','west','up','down']){
   let m=materials.indexOf(overrides[side]||mat),u=m%4*64,v=(m>>2)*64;
   const full=[6,9,15].includes(m),w=side==='east'||side==='west'?s[2]:s[0],h=side==='up'||side==='down'?s[2]:s[1];
   const uw=full?62:Math.min(62,Math.max(3,w*2.5)),vh=full?62:Math.min(62,Math.max(3,h*2.5));
   const offset=serial*17+side.charCodeAt(0)*7;
   u+=1+(full?0:offset%Math.max(1,Math.floor(63-uw)));v+=1+(full?0:(offset*3)%Math.max(1,Math.floor(63-vh)));
   faces[side]={uv:[u,v,u+uw,v+vh],texture:0};
  }
  const e={name,uuid:uid(id+'/cube/'+serial++),from:c.map((v,i)=>v-s[i]/2),to:c.map((v,i)=>v+s[i]/2),origin,rotation,faces,box_uv:false,visibility:true,export:true,autouv:0};elements.push(e);parent.children.push(e.uuid);return e;
 };
 const beam=(name,a,b,width,depth,mat,parent)=>{const d=b.map((v,i)=>v-a[i]),c=a.map((v,i)=>(v+b[i])/2),l=Math.hypot(...d);return cube(name,c,[width,l,depth],mat,parent,[Math.atan2(d[2],Math.hypot(d[0],d[1]))*180/Math.PI,0,-Math.atan2(d[0],d[1])*180/Math.PI]);};
 const chain=(name,points,widths,depths,mat,parent)=>{for(let i=1;i<points.length;i++)beam(name+' '+i,points[i-1],points[i],widths[i-1],depths[i-1],mat,parent);};
 const tuft=(name,c,s,mat,parent,r=[0,0,0])=>{cube(name,c,s,mat,parent,r);const tip=c.map((v,i)=>v+(i===1?-s[1]*.54:0));cube(name+' tip',tip,[s[0]*.55,s[1]*.45,s[2]*.65],mat,parent,r);};
 // Three intersecting boxes bevel the silhouette without a dense mesh.
 const rounded=(name,c,s,bevel,mat,parent,r=[0,0,0])=>{
  cube(name+' crown',c,[s[0]-bevel*2,s[1],s[2]-bevel*2],mat,parent,r);
  cube(name+' sides',c,[s[0],s[1]-bevel*2,s[2]-bevel*2],mat,parent,r);
  cube(name+' ends',c,[s[0]-bevel*2,s[1]-bevel*2,s[2]],mat,parent,r);
 };
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 function clip(name,length,loop,tracks){
  const animators={},runtime=[];
  for(const[bone,track]of Object.entries(tracks)){
   const g=groups.find(g=>g.name===bone);if(!g)continue;
   const list=Array.isArray(track)?track:[track],keys=[],keyframes=[];
   for(const t of list)for(let i=0;i<=40;i++){
    const time=length*i/40,value=t.fn(i/40).map(v=>+v.toFixed(4));assert(value.every(Number.isFinite));keys.push({channel:t.channel,time,value});
    keyframes.push({uuid:uid(id+'/'+name+'/'+bone+'/'+t.channel+'/'+i),channel:t.channel,time,interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((k,j)=>[k,''+value[j]]))]});
   }
   animators[g.uuid]={name:bone,type:'bone',keyframes};runtime.push({bone,keys});
  }
  animations.push({uuid:uid(id+'/anim/'+name),name:'animation.'+id+'.'+name,length,loop:loop?'loop':'once',override:false,animators});clips.push({name,length,loop,tracks:runtime});
 }
 const rootBone=group('Root'),body=group('Body',[0,20,0],rootBone);let head,legs=[];
 if(kind==='totality'){
  cube('Lean abdomen',[0,25,3],[9,13,8],'fur',body,[-14,0,0]);rounded('Hunched ribcage',[0,33,0],[17,15,15],2,'fur',body,[-19,0,0]);
  cube('White chest blaze',[0,31,-7.4],[8,12,1.6],'white',body,[-19,0,0]);
  for(const s of [-1,1]){tuft('Chest ruff '+s,[s*4.3,32,-7.2],[3.3,8,2.5],'white',body,[0,0,s*-23]);tuft('Shoulder mane '+s,[s*8.1,36.7,2.5],[5,9,7],'mane',body,[-18,0,s*27]);tuft('Scapula fur '+s,[s*6.7,35,8],[4,8,3.5],'fur',body,[-30,0,s*22]);}
  head=group('Head',[0,38,-6],body);rounded('Narrow wolf skull',[0,41,-8],[10,10,12],1.5,'fur',head,[-8,0,0]);
  for(const s of [-1,1]){
   tuft('Cheek mane '+s,[s*5.1,38.8,-6.5],[3.8,9,5],'white',head,[-12,0,s*-30]);
   tuft('Outer cheek fur '+s,[s*6,40,-3],[3,7,4],'fur',head,[-20,0,s*-38]);
   const ear=group('Ear '+s,[s*3.3,44,-5],head);
   cube('Ear base '+s,[s*3.7,46.5,-4.2],[3.6,4.5,2.5],'mane',ear,[12,0,s*-14]);
   cube('Ear tip '+s,[s*4.2,49,-3.7],[1.4,2.2,1.7],'mane',ear,[12,0,s*-14]);
   cube('Ear inner '+s,[s*3.8,46.8,-5.6],[1.5,2.6,.2],'skin',ear,[12,0,s*-14]);
   cube('Slanted golden eye '+s,[s*3.3,41.5,-14.55],[2.6,1.3,.35],'eye',head,[0,0,s*14]);
   cube('Eye brow '+s,[s*3.4,42.6,-14.3],[3.5,1.6,1.6],'fur',head,[0,0,s*18]);
   cube('White muzzle root '+s,[s*2.8,39.5,-14],[3.4,3.5,4],'white',head,[0,s*-13,0]);
  }
  cube('Six red forehead marks',[0,43.6,-14.9],[3.2,5,.2],'fur',head,[-8,0,0],[0,43.6,-14.9],{north:'symbol'});
  cube('Tapered muzzle',[0,39,-17],[4.7,2.8,5.5],'white',head,[-7,0,0]);
  cube('Nose',[0,39.1,-20],[3.4,1.9,1.8],'nose',head,[-7,0,0]);cube('Open mouth',[0,36.8,-15.7],[4.5,2.7,7.2],'mouth',head);
  const jaw=group('Jaw',[0,38,-10],head);cube('Lower jaw',[0,35.5,-15.4],[4.2,1.5,8],'white',jaw,[-9,0,0]);
  for(let x of [-1.8,-.7,.7,1.8]){const outer=Math.abs(x)>1;cube('Upper fang',[x,outer?37.2:37.8,-20.1],[.55,outer?1.8:.6,.7],'teeth',head,[0,0,x*4]);cube('Lower fang',[x,outer?36:35.8,-19.8],[.5,outer?.9:.5,.6],'teeth',jaw);}
  for(const s of [-1,1]){
   const arm=group('Arm '+s,[s*9,34,-1],body),fore=group('Forearm '+s,[s*12,23,-3],arm),hand=group('Hand '+s,[s*13,12,-5],fore);
   beam('Upper arm '+s,[s*9,34,-1],[s*12,23,-3],5.8,6,'fur',arm);beam('Forearm '+s,[s*12,23,-3],[s*13,12,-5],4.3,4.5,'white',fore);tuft('Elbow fur '+s,[s*14,24,0],[2.8,6,3.5],'fur',fore,[0,0,s*25]);
   cube('Clawed palm '+s,[s*13,10.8,-5.1],[5.7,3.6,2.8],'white',hand);
   for(let f=0;f<4;f++){let x=s*13+(f-1.5)*1.35,y=6.3-(f===1||f===2?.8:0);beam('Finger '+f,[x,10.2,-5.2],[x,y,-6.2],1.05,1.1,'white',hand);beam('Claw '+f,[x,y+.2,-6.2],[x,y-1.8,-7.5],.55,.65,'dark',hand);}
   beam('Thumb '+s,[s*15.2,11.4,-5.7],[s*16.8,8.9,-6.8],1.3,1.3,'white',hand);
   const leg=group('Leg '+s,[s*4.6,21,2],rootBone),shin=group('Shin '+s,[s*6,12,4],leg),foot=group('Foot '+s,[s*6,4,1],shin);legs.push(leg);
   beam('Haunch '+s,[s*4.6,21,2],[s*6,12,4],6,7,'fur',leg);beam('Hock '+s,[s*6,12,4],[s*6,4,1],3.6,3.8,'white',shin);cube('Wolf foot '+s,[s*6,2,-1],[5.3,4,7],'white',foot);
   for(let f=0;f<3;f++)cube('Toe claw '+s+f,[s*6+(f-1)*1.8,1,-5],[.8,1.5,2.2],'dark',foot);
  }
  const tail=group('Tail',[0,24,6],body);chain('Bushy tail',[[0,24,6],[0,25,14],[0,29,22],[0,36,27],[0,43,25],[0,47,20]],[5,7,7,4,1.7],[5,7,7,4,1.7],'mane',tail);
  for(const s of [-1,1])tuft('Tail plume '+s,[s*3,32,23],[3.8,8,4],'fur',tail,[-30,0,s*25]);
 }else if(kind==='elephant'){
  rounded('Massive rounded barrel',[0,39,6],[44,37,49],4.5,'white',body);
  rounded('High shoulders',[0,43,-8],[39,35,30],4,'white',body);
  cube('Soft belly',[0,24,7],[32,8,38],'belly',body);
  head=group('Head',[0,47,-18],body);rounded('Domed elephant forehead',[0,47,-25],[27,28,24],3.3,'white',head,[-4,0,0]);
  cube('Forehead gold seal',[0,51,-37.2],[13.5,17,.3],'white',head,[-4,0,0],[0,51,-37.2],{north:'symbol'});
  for(const s of [-1,1]){
   const ear=group('Ear '+s,[s*12,47,-23],head);ear.rotation=[0,s*18,s*9];groups.find(g=>g.uuid===ear.uuid).rotation=ear.rotation;
   cube('Ear fan center '+s,[s*24,45,-22],[21,25,4.5],'white',ear);
   cube('Ear fan crown '+s,[s*23,59,-22],[17,3,4.5],'white',ear);
   cube('Ear lower lobe '+s,[s*23,30,-22],[15,5,4.5],'belly',ear);
   beam('Crimson outer rim '+s,[s*34.5,55,-22.2],[s*34.5,35,-22.2],3.2,4.8,'mane',ear);
   beam('Crimson lower rim '+s,[s*34.5,35,-22.2],[s*29,28,-22.2],3.2,4.8,'mane',ear);
   beam('Crimson crown rim '+s,[s*29,60,-22.2],[s*34.5,55,-22.2],3,4.8,'mane',ear);
   cube('Eye crease '+s,[s*10.4,45,-36.1],[4.4,2.8,1],'skin',head,[0,0,s*14]);cube('Eye '+s,[s*10.4,44.7,-36.7],[2.1,1.8,.3],'eye',head);
   chain('Short curved ivory tusk '+s,[[s*10,36,-32],[s*13,32,-38],[s*14,33,-43],[s*13,37,-46]],[4.8,3.2,1.3],[4.8,3.2,1.3],'horn',head);
   cube('Cheek gold ring '+s,[s*10.7,37,-36],[6.5,7,.25],'white',head,[0,s*8,0],[s*10.7,37,-36],{north:'mark'});
  }
  let parent=head;const points=[[0,42,-35],[0,31,-40],[0,20,-43],[0,11,-47],[0,11,-54],[0,17,-57]];
  for(let i=0;i<5;i++){
   const g=group('Trunk '+i,points[i],parent),width=11.5-i*1.6,depth=10-i*1.4;
   const segment=beam('Tapered trunk '+i,points[i],points[i+1],width,depth,'skin',g),normal=rotate([0,0,-depth*.51],segment.rotation);
   if(i<3)for(const s of [-1,1]){const a=points[i].map((v,j)=>v+normal[j]+(j===0?s*width*.31:0)),b=points[i+1].map((v,j)=>v+normal[j]+(j===0?s*width*.31:0));beam('Trunk gold stripe '+s+' '+i,a,b,.6,.2,'gold',g);}
   parent=g;
  }
  for(const s of [-1,1])cube('Trunk nostril '+s,[s*1.1,17,-59],[1.1,1.6,.25],'dark',parent);
  for(const s of [-1,1])for(const z of [-13,22]){
   const leg=group('Leg '+s+' '+z,[s*15,30,z],rootBone);legs.push(leg);beam('Pillar leg',[s*15,30,z],[s*16,6,z-1],11.5,12.5,'white',leg);rounded('Rounded foot',[s*16,4.2,z-1],[13.5,8.4,14],1.2,'white',leg);
   if(z<0)cube('Leg gold ring',[s*16,12.5,z-7.9],[8.5,9,.25],'white',leg,[0,0,0],[s*16,12.5,z-7.9],{north:'mark'});
   for(let i=-1;i<=1;i++)cube('Toenail',[s*16+i*3.6,2,z-8.2],[2.8,3.5,1.3],'hoof',leg);
  }
  const tail=group('Tail',[0,43,29],body);chain('Tail',[[0,43,29],[0,28,34],[0,16,36]],[2,1.5],[2,1.5],'skin',tail);cube('Tail tassel',[0,15,36],[3.5,6,3.5],'hoof',tail);
  cube('Back ceremonial ring',[0,57.65,13],[23,.2,23],'white',body,[0,0,0],[0,57.65,13],{up:'mark'});
 }else if(kind==='toad'){
  rounded('Broad squat body',[0,10.8,3],[28,20,27],3.8,'skin',body);
  rounded('Pale rounded throat',[0,9,-10.2],[23.5,15.5,6],2,'belly',body);
  cube('Belly crest',[0,7.3,-13.35],[9.2,9.2,.25],'belly',body,[0,0,0],[0,7.3,-13.35],{north:'symbol'});head=group('Head',[0,15,-5],body);
  rounded('Broad upper muzzle',[0,15,-10],[25,7,11],1.6,'skin',head);
  cube('Wide mouth crease',[0,12.2,-15.6],[21.5,.65,.2],'mouth',head);
  const jaw=group('Jaw',[0,12,-6],head);cube('Lower lip',[0,11.3,-12],[22,1.6,7.8],'belly',jaw);
  for(const s of [-1,1]){
   rounded('Raised eye mound '+s,[s*9,21,-6.3],[7.6,7.4,7.6],1.4,'skin',head);
   const eye=cube('Large pale eye '+s,[s*9,21.8,-10.2],[6,5.5,.3],'skin',head,[0,0,s*7]);eye.faces.north={uv:[137,72,183,120],texture:0};
   const arm=group('Arm '+s,[s*11,11,-5],rootBone);legs.push(arm);beam('Front upper arm',[s*11,11,-5],[s*18,5,-9],3.8,4,'skin',arm);beam('Forearm',[s*18,5,-9],[s*17,1.7,-17],2.7,3,'skin',arm);
   cube('Front webbed palm',[s*17,1.3,-18],[4.6,1.3,3],'skin',arm);
   for(let f=0;f<4;f++)beam('Front splayed digit',[s*17,1.3,-18],[s*(13.7+f*2),.6,-22.5-(f===1||f===2?1:0)],.8,.9,'skin',arm);
   const leg=group('Leg '+s,[s*10,8,8],rootBone);legs.push(leg);rounded('Powerful folded haunch',[s*17,7.4,11],[12.5,13,15],2,'skin',leg,[0,s*-27,0]);beam('Folded shin',[s*21,6,14],[s*15,2,21],3.4,3.8,'skin',leg);
   for(let f=0;f<3;f++)beam('Hind splayed toe',[s*15,1.3,20],[s*(14+f*2.5),.6,25+f%2],1,1.1,'skin',leg);
  }
 }else{
  rounded('Heavy low barrel',[0,24,6],[29,27,38],3.5,'fur',body);
  rounded('Massive arched shoulders',[0,34,-8],[33,28,28],4.5,'mane',body);
  cube('Sloping back',[0,35,12],[23,7,26],'fur',body,[12,0,0]);
  head=group('Head',[0,28,-19],body);rounded('Low broad bovine skull',[0,24,-26],[23,23,21],2.8,'fur',head,[-13,0,0]);
  rounded('Dark broad muzzle',[0,16.5,-36],[19,10,10],1.5,'nose',head);
  cube('Snarling mouth',[0,13.8,-41.15],[16.5,3.4,.25],'mouth',head);
  const jaw=group('Jaw',[0,16,-30],head);cube('Lower jaw',[0,11.5,-35.8],[16.5,3,10],'fur',jaw);
  for(let i=-3;i<=3;i++)cube('Broad exposed tooth',[i*2.1,14.1,-41.4],[1.8,2.8,.4],'teeth',head,[0,0,i*2]);
  cube('White forehead diamond',[0,30.5,-38.8],[8.3,8.3,.25],'fur',head,[-13,0,45],[0,30.5,-38.8],{north:'symbol'});
  for(const s of [-1,1]){
   cube('Nostril '+s,[s*5.4,18.5,-41.2],[3,1.6,.25],'dark',head,[0,0,s*-15]);cube('Pale eye '+s,[s*8.3,26.9,-37.9],[2.8,1.7,.35],'eye',head,[0,0,s*22]);cube('Heavy brow '+s,[s*8.2,28.4,-37.6],[5.6,2.5,2],'mane',head,[0,0,s*22]);
   chain('Forward swept ivory horn '+s,[[s*10,34,-24],[s*17,36,-26],[s*22,41,-31],[s*22.5,47,-36],[s*19,51,-40]],[5.4,4,2.5,1],[5.4,4,2.5,1],'horn',head);
   cube('Small ear '+s,[s*14,28,-23],[8,3.5,4],'fur',head,[0,0,s*20]);tuft('Short cheek beard '+s,[s*9,15.5,-28],[4.8,8,5],'mane',head,[0,0,s*-15]);
   tuft('Shoulder shag '+s,[s*14,28,-10],[4.8,10,8],'fur',body,[0,0,s*17]);
   for(const z of [-11,18]){const leg=group('Leg '+s+' '+z,[s*10,24,z],rootBone);legs.push(leg);beam('Stocky upper leg',[s*10,24,z],[s*11,12,z+2],8,9,'fur',leg);beam('Lower leg',[s*11,12,z+2],[s*11,4,z],5.7,6.2,'fur',leg);cube('Cloven hoof',[s*11,2.5,z-1],[7.2,5,8.5],'hoof',leg);cube('Hoof cleft',[s*11,2,z-5.3],[.5,3,.2],'dark',leg);}
  }
  const tail=group('Tail',[0,29,24],body);chain('Whip tail',[[0,29,24],[0,32,34],[0,39,41],[0,44,47]],[1.5,1.1,.8],[1.5,1.1,.8],'fur',tail);cube('Small tail tuft',[0,44,47],[2.5,4,2.5],'mane',tail,[-30,0,0]);
 }
 const idle={Body:[R(p=>[.6*sin(p),0,.3*sin(p)]),P(p=>[0,.25*sin(p),0])],Head:R(p=>[.7*sin(p-.15),1.2*sin(p*.5),0]),Tail:R(p=>[2*sin(p),5*sin(p-.1),0]),Jaw:R(p=>[kind==='totality'?2+1.5*sin(p):.5*sin(p),0,0])};
 for(const s of [-1,1])idle['Ear '+s]=R(p=>[0,s*1.8*sin(p-.2),.5*sin(p)]);
 clip('idle',3.6,true,idle);
 for(const fast of [false,true]){
  const tracks={Body:[R(p=>[(fast?3:1)*sin(p*2),0,(fast?2:1)*sin(p)]),P(p=>[0,(fast?1.5:.5)*(1-Math.cos(p*4*Math.PI)),0])],Head:R(p=>[-(fast?3:1)*sin(p*2),0,0]),Tail:R(p=>[3*sin(p-.2),6*sin(p-.1),0])};
  legs.forEach((g,i)=>tracks[g.name]=R(p=>[(fast?34:18)*sin(p+(i%2)*.5+(i>1?.5:0)),0,0]));
  if(kind==='totality')for(const s of [-1,1]){
   tracks['Leg '+s]=R(p=>[45*(fast?1:.5)*sin(p+(s===1?.5:0)),0,0]);
   tracks['Shin '+s]=R(p=>[-(fast?38:20)*Math.max(0,sin(p+(s===1?.5:0)+.15)),0,0]);
   tracks['Arm '+s]=R(p=>[-35*(fast?1:.5)*sin(p+(s===1?.5:0)),0,s*5]);
   tracks['Forearm '+s]=R(p=>[fast?35:15,0,0]);
  }
  if(kind==='toad'){tracks.Root=P(p=>[0,(fast?4:2)*Math.pow(Math.max(0,sin(p)),2),0]);tracks.Body=R(p=>[5*sin(p),0,0]);}
  for(let i=0;i<5;i++)tracks['Trunk '+i]=R(p=>[2*sin(p-i*.12),1.8*sin(p-i*.15),0]);
  clip(fast?'run':'walk',fast?.65:1.2,true,tracks);
 }
 const rise=p=>1-smooth((p-.2)/.8);
 clip('summon',kind==='elephant'?1.2:3.5,false,{Body:R(p=>[-14*rise(p),0,0]),Head:R(p=>[12*rise(p),0,0]),'Arm -1':R(p=>[30*rise(p),0,-16*rise(p)]),'Arm 1':R(p=>[22*rise(p),0,16*rise(p)]),Jaw:R(p=>[9*Math.sin(p*Math.PI),0,0]),Tail:R(p=>[-12*rise(p),0,0])});
 clip('dismiss',2.5,false,{Body:R(p=>[-14*smooth(p),0,0]),Head:R(p=>[12*smooth(p),0,0]),'Arm -1':R(p=>[25*smooth(p),0,-12*smooth(p)]),'Arm 1':R(p=>[25*smooth(p),0,12*smooth(p)]),Tail:R(p=>[-12*smooth(p),0,0])});
 const curve=(p,keys)=>{for(let i=1;i<keys.length;i++)if(p<=keys[i][0]){let a=keys[i-1],b=keys[i];return a[1]+(b[1]-a[1])*smooth((p-a[0])/(b[0]-a[0]));}return keys.at(-1)[1];};
 for(const s of [-1,1]){
  const sw=p=>curve(p,[[0,0],[.28,-.65],[.48,1],[.65,.85],[1,0]]),lift=p=>smooth(p/.25)*(1-smooth((p-.65)/.35));
  clip(s<0?'slash_left':'slash_right',1.2,false,{Body:R(p=>[-8*lift(p),s*25*sw(p),-s*6*sw(p)]),Head:R(p=>[6*lift(p),-s*14*sw(p),0]),Root:P(p=>[0,-1.2*lift(p),-3*Math.max(0,sw(p))]),['Arm '+s]:R(p=>[75*lift(p)+12*sw(p),s*60*sw(p),s*28*lift(p)]),['Forearm '+s]:R(p=>[30*lift(p)-20*Math.max(0,sw(p)),0,0]),['Hand '+s]:R(p=>[0,s*-25*sw(p),0]),['Arm '+(-s)]:R(p=>[22*lift(p),0,-s*9*lift(p)]),Jaw:R(p=>[15*lift(p),0,0])});
 }
 const stomp=p=>curve(p,[[0,0],[.35,1],[.43,1],[.48,-.3],[.65,0],[1,0]]);
 clip('stomp',1.9,false,{Body:R(p=>[7*stomp(p),0,0]),Root:P(p=>[0,1.8*stomp(p),0]),Head:R(p=>[-12*stomp(p),0,0]),'Leg -1 -13':R(p=>[42*stomp(p),0,0]),'Leg 1 -13':R(p=>[42*stomp(p),0,0]),'Trunk 0':R(p=>[-20*stomp(p),0,0])});
 clip('tongue',2,false,{Jaw:R(p=>[-32*smooth(p/.18)*(1-smooth((p-.8)/.2)),0,0]),Head:R(p=>[5*smooth(p/.2),0,0]),Body:P(p=>[0,-1.5*smooth(p/.2)*(1-smooth((p-.8)/.2)),0])});
 clip('drop',1,true,{Body:R(p=>[-4,0,0]),Head:R(p=>[5,0,0]),'Trunk 0':R(p=>[-32,0,0]),'Ear -1':R(p=>[0,-18,-10]),'Ear 1':R(p=>[0,18,10])});
 clip('land',1.75,false,{Root:P(p=>[0,-5*Math.sin(Math.PI*smooth(p)),0]),Body:R(p=>[-5*Math.sin(Math.PI*p),0,0]),Head:R(p=>[8*Math.sin(Math.PI*p),0,0]),'Trunk 0':R(p=>[18*Math.sin(Math.PI*p),0,0])});
 clip('airborne',1,true,{Body:R(p=>[-5,0,0]),Head:R(p=>[5,0,0]),'Leg -1':R(p=>[24,0,0]),'Leg 1':R(p=>[-15,0,0]),'Arm -1':R(p=>[30,0,-8]),'Arm 1':R(p=>[30,0,8])});
 return {model,clips};
}
function rotate(p,r){let[x,y,z]=p,[a,b,c]=r.map(v=>v*Math.PI/180);[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z];}
function bake(model){const elements=new Map(model.elements.map(e=>[e.uuid,e])),groups=new Map(model.groups.map(g=>[g.uuid,g]));return function walk(nodes){return nodes.map(n=>{
 if(typeof n!=='string'){const g=groups.get(n.uuid)||n;return {name:g.name,origin:g.origin||[0,0,0],rotation:g.rotation||[0,0,0],visible:g.visibility!==false&&g.export!==false,children:walk(n.children)};}
 const e=elements.get(n),[x,y,z]=e.from,[X,Y,Z]=e.to,o=e.origin,quads=[];
 const faces={north:[[X,y,z],[x,y,z],[x,Y,z],[X,Y,z]],south:[[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],east:[[X,y,Z],[X,y,z],[X,Y,z],[X,Y,Z]],west:[[x,y,z],[x,y,Z],[x,Y,Z],[x,Y,z]],up:[[x,Y,Z],[X,Y,Z],[X,Y,z],[x,Y,z]],down:[[x,y,z],[X,y,z],[X,y,Z],[x,y,Z]]};
 for(const[side,points]of Object.entries(faces)){const face=e.faces[side];if(!face||face.texture===null)continue;let[u,v,U,V]=face.uv,uv=[[U,V],[u,V],[u,v],[U,v]];for(let i=0;i<(face.rotation||0)/90;i++)uv.unshift(uv.pop());const vertices=points.map((p,i)=>[...rotate(p.map((v,j)=>v-o[j]),e.rotation||[0,0,0]).map((v,j)=>v+o[j]),uv[i][0]/model.resolution.width,uv[i][1]/model.resolution.height]);let[a,b,c]=vertices,ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],l=Math.hypot(...normal);if(l>1e-9)quads.push({vertices,normal:normal.map(v=>v/l)});}
 return {name:e.name,visible:e.visibility!==false&&e.export!==false,quads};
 });}(model.outliner);}
function clipsFrom(model){
 const modern=parseFloat(model.meta.format_version)>=5;
 return model.animations.map(a=>({name:a.name.split('.').at(-1),length:a.length,loop:a.loop==='loop',tracks:Object.entries(a.animators).map(([uuid,animator])=>({
  bone:model.groups.find(g=>g.uuid===uuid)?.name||animator.name,
  keys:animator.keyframes.filter(k=>['rotation','position'].includes(k.channel)).map(k=>{
   const value=['x','y','z'].map(axis=>Number(k.data_points[0][axis]));
   assert(value.every(Number.isFinite),'Animation expressions are unsupported');
   // Blockbench 5 changed keyframe signs; the runtime rig retains the legacy convention.
   // https://blockbench.net/wiki/docs/bbmodel/#5.0
   if(modern)for(let i=0;i<(k.channel==='rotation'?2:1);i++)if(value[i]!==0)value[i]=-value[i];
   return {channel:k.channel,time:k.time,value};
  })
 }))}));
}
module.exports={bake,clipsFrom};
if(require.main===module)(async()=>{for(const[id,kind]of [['divine_dog_totality','totality'],['max_elephant','elephant'],['toad','toad'],['piercing_ox','ox']]){
 // Existing Blockbench files are authoritative, including manually edited geometry and poses.
 const saved=path.join(root,'deliverables/shikigami',id+'.bbmodel');
 const model=fs.existsSync(saved)?JSON.parse(fs.readFileSync(saved)):create(id,kind).model,clips=clipsFrom(model),authored=path.join(root,'tools/assets/shikigami',id+'.png');
 const png=fs.existsSync(authored)?fs.readFileSync(authored):model.textures[0]?.source?Buffer.from(model.textures[0].source.split(',')[1],'base64'):await atlas(kind),{width,height}=await sharp(png).metadata();
 assert(width===height&&width%256===0,'Atlas must be square and a multiple of 256');
 // Authored material atlases preserve the original normalized UV layout.
 for(const e of model.elements)for(const f of Object.values(e.faces))f.uv=f.uv.map((v,i)=>v*(i%2?height/model.resolution.height:width/model.resolution.width));
 model.resolution={width,height};
 model.textures=[{...model.textures[0],uuid:model.textures[0]?.uuid||uid(id+'/texture'),name:id+'.png',id:'0',width,height,uv_width:width,uv_height:height,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 assert(model.elements.length<180);assert(new Set(model.groups.map(g=>g.name)).size===model.groups.length,'Duplicate bone name');
 write('deliverables/shikigami/'+id+'.bbmodel',JSON.stringify(model));write('src/main/resources/assets/kazimod/textures/entities/'+id+'.png',png);
 write('src/main/resources/assets/kazimod/models/entities/'+id+'.json',JSON.stringify({sourceHash:crypto.createHash('sha256').update(JSON.stringify(model)).digest('hex'),nodes:bake(model),animations:clips}));
 console.log(id+': '+model.elements.length+' cubes, '+model.groups.length+' bones, '+clips.length+' animations');
}})().catch(e=>{console.error(e);process.exit(1);});
