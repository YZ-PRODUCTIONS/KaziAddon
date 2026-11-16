// One editable Blockbench source supplies the in-game geometry and animation clips.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),assert=require('assert');
const sharp=require(process.env.SHARP_MODULE||'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bake,clipsFrom}=require(process.env.GENGAR_BAKE_MODULE||'./build_shikigami.cjs');
const root=process.argv[2]||path.resolve(__dirname,'..');
const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,d);};
const uid=s=>{const h=crypto.createHash('md5').update('gengar/'+s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
function canvas(size){const data=Buffer.alloc(size*size*4),rgb=c=>c.match(/../g).map(v=>parseInt(v,16));
 const dot=(x,y,c,a=255)=>{x=Math.round(x);y=Math.round(y);if(x<0||y<0||x>=size||y>=size)return;let i=(y*size+x)*4,v=Array.isArray(c)?c:rgb(c);for(let n=0;n<3;n++)data[i+n]=Math.max(0,Math.min(255,v[n]));data[i+3]=a;};
 const rect=(x,y,w,h,c)=>{for(let Y=Math.floor(y);Y<y+h;Y++)for(let X=Math.floor(x);X<x+w;X++)dot(X,Y,c);};
 const line=(x,y,X,Y,w,c)=>{let n=Math.max(Math.abs(X-x),Math.abs(Y-y),1);for(let i=0;i<=n;i++)rect(Math.round(x+(X-x)*i/n),Math.round(y+(Y-y)*i/n),w,w,c);};
 const poly=(p,c)=>{for(let y=Math.floor(Math.min(...p.map(v=>v[1])));y<=Math.max(...p.map(v=>v[1]));y++)for(let x=Math.floor(Math.min(...p.map(v=>v[0])));x<=Math.max(...p.map(v=>v[0]));x++){let inside=false;for(let i=0,j=p.length-1;i<p.length;j=i++){let a=p[i],b=p[j];if((a[1]>y)!==(b[1]>y)&&x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;}if(inside)dot(x,y,c);}};
 const ellipse=(cx,cy,rx,ry,c)=>{for(let y=cy-ry;y<=cy+ry;y++)for(let x=cx-rx;x<=cx+rx;x++)if(((x-cx)/rx)**2+((y-cy)/ry)**2<=1)dot(x,y,c);};
 return{data,dot,rect,line,poly,ellipse};
}
async function atlas(){
 const c=canvas(256),rgb=h=>h.match(/../g).map(v=>parseInt(v,16));
 const palettes=[['9984bb','735789'],['8470aa','59426f'],['8c70ac','a75c93'],['8f73af','bd66a5'],['9680b6','a45b94'],['8d76af','6e538b'],['9572aa','b05d98'],['9b77b7','c675b0'],['3b244e','261731'],['a18abd','765a91'],['d2c76a','ad9547'],['9173af','624b81'],['a28dc1','6c538b'],['796393','554264'],['8e75ab','a86092'],['9d7eb8','ad5d98']];
 // Broad, stepped color clusters preserve Minecraft texel density without noisy grain.
 for(let m=0;m<16;m++){const ox=m%4*64,oy=(m>>2)*64,top=rgb(palettes[m][0]),bottom=rgb(palettes[m][1]);
  for(let y=0;y<64;y++)for(let x=0;x<64;x++){
   const t=Math.floor(y/5)*5/63,patch=Math.sin(Math.floor(x/7)*1.6+Math.floor(y/6)*2.1)>0?2:-2;
   const shade=Math.round((6*Math.cos((x-24)/26)+patch)/3)*3;
   c.dot(ox+x,oy+y,top.map((v,i)=>v*(1-t)+bottom[i]*t+shade));
  }
  if(![4,5,6,8,10].includes(m)){
   c.poly([[ox+8,oy+7],[ox+24,oy+5],[ox+22,oy+11],[ox+14,oy+11],[ox+14,oy+21],[ox+8,oy+23]],top.map(v=>v+9));
   c.poly([[ox+49,oy+29],[ox+55,oy+25],[ox+57,oy+47],[ox+43,oy+54],[ox+43,oy+49],[ox+50,oy+44]],bottom.map(v=>v-5));
  }
 }
 // The forehead eye is painted into the face, not raised on a separate block.
 let y=64;c.poly([[27,y+5],[36,y+5],[40,y+10],[40,y+17],[36,y+21],[27,y+21],[23,y+17],[23,y+10]],'6a567c');
 c.poly([[28,y+6],[35,y+6],[38,y+10],[38,y+17],[35,y+20],[28,y+20],[25,y+17],[25,y+10]],'d4c569');
 c.rect(29,y+8,5,2,'e5d77d');c.rect(30,y+11,3,6,'4a375b');c.rect(31,y+12,2,4,'33233f');
 for(const side of[-1,1]){
  const map=p=>p.map(([x,v])=>[side<0?x:63-x,y+v]);
  c.poly(map([[4,24],[9,24],[27,32],[26,39],[20,43],[11,41],[6,36]]),'513447');
  c.poly(map([[6,27],[11,28],[25,33],[24,38],[19,41],[12,39],[8,35]]),'bc433d');
  c.poly(map([[10,30],[15,31],[23,34],[22,38],[18,39],[12,37]]),'ed7946');
  c.poly(map([[12,34],[17,35],[19,38],[15,38],[12,36]]),'f9a562');
  c.poly(map([[17,32],[21,34],[20,39],[17,38]]),'3a253d');
  c.poly(map([[4,23],[9,23],[27,31],[27,33],[9,27],[4,26]]),'60477c');
  c.poly(map([[4,20],[12,23],[15,24],[15,26],[7,23],[4,23]]),'aa90c7');
 }
 c.poly([[3,y+43],[10,y+46],[22,y+49],[42,y+49],[54,y+46],[61,y+43],[57,y+57],[47,y+62],[16,y+62],[6,y+57]],'38243f');
 // Separate jaw UVs let the grin open without moving the eyes or breaking the skull.
 const ox=128,oy=64;
 c.poly([[ox+1,oy+3],[ox+11,oy+11],[ox+24,oy+15],[ox+40,oy+15],[ox+53,oy+11],[ox+62,oy+3],[ox+58,oy+39],[ox+49,oy+51],[ox+15,oy+51],[ox+6,oy+39]],'493448');
 c.poly([[ox+4,oy+9],[ox+13,oy+17],[ox+25,oy+20],[ox+39,oy+20],[ox+51,oy+17],[ox+59,oy+9],[ox+55,oy+36],[ox+47,oy+44],[ox+17,oy+44],[ox+9,oy+36]],'d8ddd4');
 c.poly([[ox+9,oy+32],[ox+17,oy+39],[ox+47,oy+39],[ox+55,oy+32],[ox+52,oy+39],[ox+47,oy+44],[ox+17,oy+44],[ox+12,oy+39]],'aab2bc');
 for(const X of[11,18,25,32,39,46,53]){let top=17-Math.abs(X-32)*.25,bottom=43-Math.max(0,Math.abs(X-32)-14)*.8;c.line(ox+X,oy+top,ox+X,oy+bottom,1,'7c798e');}
 c.line(ox+17,oy+47,ox+46,oy+47,2,'c17aaf');
 return sharp(c.data,{raw:{width:256,height:256,channels:4}}).png().toBuffer();
}
function build(){const elements=[],groups=[],outliner=[],animations=[];
 const m={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Mega Gengar',model_identifier:'mega_gengar',resolution:{width:256,height:256},visible_box:[6,5,0],elements,groups,outliner,textures:[],animations};
 const group=(name,o=[0,0,0],parent=null,rotation=[0,0,0])=>{const g={name,uuid:uid(name),origin:o,rotation,visibility:true,export:true,children:[]};groups.push({...g,children:undefined});(parent?parent.children:outliner).push(g);return g;};
 const mesh=(name,parent)=>{const e={type:'mesh',name,uuid:uid('mesh/'+name),origin:[0,0,0],rotation:[0,0,0],vertices:{},faces:{},visibility:true,export:true};elements.push(e);parent.children.push(e.uuid);return e;};
 const face=(e,points,mat,uv)=>{const keys=points.map(p=>{const value=p.map(v=>+v.toFixed(5)),existing=Object.keys(e.vertices).find(k=>e.vertices[k].every((v,i)=>v===value[i]));if(existing)return existing;const key='v'+Object.keys(e.vertices).length;e.vertices[key]=value;return key;});
  const u=mat%4*64,v=(mat>>2)*64,coords=uv||[[u+1,v+63],[u+63,v+63],[u+63,v+1],[u+1,v+1]];
  e.faces['f'+Object.keys(e.faces).length]={vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,coords[i]])),texture:0};};
 const ring=(y,w,d,z=0)=>[[-w*.36,y,z-d/2],[w*.36,y,z-d/2],[w/2,y,z-d*.32],[w/2,y,z+d*.32],[w*.36,y,z+d/2],[-w*.36,y,z+d/2],[-w/2,y,z+d*.32],[-w/2,y,z-d*.32]];
 const loft=(name,rings,mat,parent,mapper)=>{const e=mesh(name,parent);for(let i=1;i<rings.length;i++)for(let j=0;j<8;j++){
   const next=(j+1)%8,points=[rings[i-1][next],rings[i-1][j],rings[i][j],rings[i][next]],mapped=mapper&&mapper(points,j,i);
   face(e,points,mapped?mapped.mat:mat,mapped&&mapped.uv);
  }
  for(const edge of[0,rings.length-1])for(const ids of[[0,1,2,7],[7,2,3,6],[6,3,4,5]]){
   const points=ids.map(i=>rings[edge][i]);face(e,edge===0?points:points.reverse(),mat);}
  return e;};
 const skinUV=(points,mat,lo,hi)=>({mat,uv:points.map(p=>[mat%4*64+1+Math.max(0,Math.min(1,(p[0]+16)/32))*62,(mat>>2)*64+1+Math.max(0,Math.min(1,(hi-p[1])/(hi-lo)))*62])});
 const spike=(name,a,b,width,depth,mat,parent,bend=[0,0,0])=>{
  const d=b.map((v,i)=>v-a[i]),len=Math.hypot(...d),n=d.map(v=>v/len),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=v=>{let l=Math.hypot(...v);return v.map(x=>x/l);};
  const u=unit(cross(Math.abs(n[1])<.95?[0,1,0]:[0,0,1],n)),v=cross(n,u),e=mesh(name,parent);
  const section=(t,scale)=>[[-1,-1],[1,-1],[1,1],[-1,1]].map(([x,y])=>a.map((k,i)=>k+d[i]*t+bend[i]*Math.sin(Math.PI*t)+u[i]*x*width*.5*scale+v[i]*y*depth*.5*scale));
  const rr=[section(0,1),section(.52,.61)];for(let j=0;j<4;j++){
   face(e,[rr[0][(j+1)%4],rr[0][j],rr[1][j],rr[1][(j+1)%4]],mat);
   face(e,[rr[1][(j+1)%4],rr[1][j],b],mat,[[mat%4*64+63,(mat>>2)*64+34],[mat%4*64+1,(mat>>2)*64+34],[mat%4*64+32,(mat>>2)*64+1]]);
  }face(e,[rr[0][3],rr[0][2],rr[0][1],rr[0][0]],mat);return e;
 };
 const beam=(name,a,b,w,d,mat,parent)=>{
  const delta=b.map((v,i)=>v-a[i]),len=Math.hypot(...delta),rx=Math.atan2(delta[2],Math.hypot(delta[0],delta[1])),rz=-Math.atan2(delta[0],delta[1]);
  const transform=p=>{let[x,y,z]=p;[y,z]=[y*Math.cos(rx)-z*Math.sin(rx),y*Math.sin(rx)+z*Math.cos(rx)];return[x*Math.cos(rz)-y*Math.sin(rz)+(a[0]+b[0])/2,x*Math.sin(rz)+y*Math.cos(rz)+(a[1]+b[1])/2,z+(a[2]+b[2])/2];};
  return loft(name,[ring(-len*.55,w*.8,d*.8),ring(-len*.35,w,d),ring(len*.35,w,d),ring(len*.55,w*.8,d*.8)].map(r=>r.map(transform)),mat,parent);
 };
 const r=group('Root'),body=group('Body',[0,8,2],r),head=group('Head',[0,13,2],body),jaw=group('Jaw',[0,12,-2],head);
 loft('Submerged magenta body',[ring(-2,20,14,2),ring(3,27,20,2),ring(8,28,21,2)],2,body,(p)=>skinUV(p,2,-2,18));
 loft('Beveled continuous head',[ring(2,24,18,1),ring(6,29,24,1),ring(16,32,26,1),ring(27,29,24,1),ring(31,20,18,2)],0,head,(p,j)=>{
  if(j===0||j===1||j===7)return skinUV(p,4,3,31);
  const mat=j===4||j===3||j===5?1:0;return skinUV(p,mat,1,32);
 });
 // A recessed, textured jaw opens as one piece rather than exposing stacks of cubes.
 loft('Grinning lower jaw',[ring(3.6,20,3,-9.6),ring(6,26,4,-10),ring(12.5,27.5,3,-10.5)],6,jaw,(p,j)=>{
  if(j===0||j===1||j===7)return{mat:6,uv:p.map(v=>[129+(v[0]+13.75)/27.5*62,65+(12.5-v[1])/8.9*62])};return skinUV(p,2,2,16);
 });
 group('Third Eye',[0,25,-11],head);
 for(const s of[-1,1]){
  const ear=group('Ear '+s,[s*10,27,0],head);
  spike('Long swept ear '+s,[s*10,27,0],[s*19,46,3.5],9,8,0,ear,[s*1,0,-1]);
  spike('Outer cheek point '+s,[s*13,17,3],[s*19,21,8],6,6,1,head);
  const arm=group('Arm '+s,[s*13,17,2],body),fore=group('Forearm '+s,[s*19,9,0],arm),hand=group('Hand '+s,[s*23,4,-6],fore,[0,0,180]);
  beam('Low shoulder '+s,[s*13,17,2],[s*19,9,0],10,11,0,arm);
  beam('Heavy tapered forearm '+s,[s*19,9,0],[s*23,4,-6],11,12,2,fore);
  loft('Broad grounded palm '+s,[ring(1,10,10),ring(3,13,13),ring(7,11,11)].map(rr=>rr.map(p=>[p[0]+s*23,p[1],p[2]-7])),3,hand);
  for(let i=0;i<3;i++){const X=s*23+(i-1)*4,g=group('Claw '+s+' '+i,[X,4,-11],hand);
   spike('Broad pointed digit '+s+' '+i,[X,3.5,-10],[X+(i-1)*.7,7.5,-20+(i===1?-1:0)],4.8,5.5,7,g,[0,-1,0]);
  }
  spike('Elbow barb '+s,[s*19,11,3],[s*24,19,6],6,6,0,arm,[s*.6,0,0]);
  spike('Outer wrist barb '+s,[s*27,4,-3],[s*32,10,-2],5,5,2,fore);
 }
 const crown=group('Crown',[0,28,6],head);
 spike('Central crown spine',[0,28,5],[0,40,10],7,8,0,crown);
 for(const s of[-1,1])spike('Crown side spine '+s,[s*6,27,8],[s*9,36,15],7,7,0,crown);
 for(let row=0;row<2;row++)for(let i=-1;i<=1;i++){
  const x=i*8,y=23-row*9,z=11+row;
  spike('Back silhouette spine '+row+' '+i,[x,y,z],[x*1.3,y+5,z+9],7,7,1,head,[0,1,0]);
 }
 const smooth=t=>(t=Math.max(0,Math.min(1,t)))*t*t*(t*(t*6-15)+10),wave=t=>Math.sin(t*Math.PI*2),env=t=>smooth(t/.24)*(1-smooth((t-.68)/.32));
 const curve=(t,points)=>{for(let i=1;i<points.length;i++)if(t<=points[i][0]){const[a,x]=points[i-1],[b,y]=points[i];return x+(y-x)*smooth((t-a)/(b-a));}return points[points.length-1][1];};
 const R=fn=>({channel:'rotation',fn}),P=fn=>({channel:'position',fn});
 const clip=(name,length,loop,tracks)=>{const animators={},samples=Math.max(48,Math.ceil(length*24));for(const[bone,values]of Object.entries(tracks)){const g=groups.find(g=>g.name===bone);assert(g,'Unknown bone '+bone);const keyframes=[];
  for(const track of Array.isArray(values)?values:[values])for(let i=0;i<=samples;i++){const value=track.fn(i/samples);assert(value.every(Number.isFinite));keyframes.push({uuid:uid(name+'/'+bone+'/'+track.channel+'/'+i),channel:track.channel,time:+(length*i/samples).toFixed(6),interpolation:'linear',data_points:[Object.fromEntries(['x','y','z'].map((a,j)=>[a,''+(+value[j].toFixed(5))]))]});}
  animators[g.uuid]={name:bone,type:'bone',keyframes};}animations.push({uuid:uid('clip/'+name),name:'animation.mega_gengar.'+name,length,loop:loop?'loop':'once',override:false,animators});};
 const idle={Body:[P(t=>[0,.28*wave(t),0]),R(t=>[.6*wave(t),.8*wave(t),0])],Head:R(t=>[-.5*wave(t-.1),.6*wave(t-.12),0]),Jaw:R(t=>[-.45*(1+wave(t-.1)),0,0]),Crown:R(t=>[1.2*wave(t-.2),0,0])};
 const floating={Body:[R(t=>[-6+1.3*wave(t),0,1.2*wave(t)]),P(t=>[0,.5*wave(t),0])],Head:R(t=>[4,0,-.6*wave(t)]),Crown:R(t=>[2+2*wave(t-.17),0,0])};
 for(const s of[-1,1]){
  idle['Arm '+s]=R(t=>[1.2*wave(t-.08*s),0,s*.8*wave(t)]);idle['Forearm '+s]=R(t=>[-.8*wave(t-.16*s),0,0]);idle['Hand '+s]=R(t=>[.7*wave(t-.22*s),s*.6*wave(t),0]);
  idle['Ear '+s]=R(t=>[1.3*wave(t-.19),0,s*.8*wave(t-.1)]);
  floating['Arm '+s]=R(t=>[-7+2*wave(t-.08*s),s*3,s*7]);floating['Forearm '+s]=R(t=>[8+3*wave(t-.2*s),0,0]);floating['Hand '+s]=R(t=>[-5+2*wave(t-.28*s),s*3,0]);floating['Ear '+s]=R(t=>[3+2*wave(t-.15),0,s*1.4*wave(t)]);
  for(let i=0;i<3;i++)idle['Claw '+s+' '+i]=R(t=>[1.5*wave(t-.09*i-.12*s),0,0]);
 }
 clip('idle',3.8,true,idle);clip('float',2.2,true,floating);
 const transform={Root:P(t=>[0,curve(t,[[0,-48],[.18,-38],[.72,1],[1,0]]),0]),Body:R(t=>[curve(t,[[0,-14],[.45,-10],[.8,3],[1,0]]),curve(t,[[0,-8],[.7,2],[1,0]]),0]),Head:R(t=>[6*(1-smooth(t)),0,0]),Jaw:R(t=>[-7*env(t),0,0]),Crown:R(t=>[curve(t,[[0,-6],[.65,5],[1,0]]),0,0])};
 const charge={Body:R(t=>[3*smooth(t),0,0]),Head:R(t=>[-2*smooth(t),0,0]),Jaw:R(t=>[-2*smooth(t),0,0])};
 const release={Body:R(t=>[curve(t,[[0,3],[.3,-8],[.65,2],[1,0]]),0,0]),Head:R(t=>[curve(t,[[0,-2],[.35,4],[1,0]]),0,0]),Jaw:R(t=>[-7*env(t),0,0])};
 const passage={Root:P(t=>[0,-48*smooth(t/.26),0]),Body:R(t=>[-14*smooth(t/.25),0,0]),Head:R(t=>[5*smooth(t/.25),0,0])};
 const emerge={Root:P(t=>[0,curve(t,[[0,-48],[.55,1.6],[1,0]]),0]),Body:R(t=>[-8*env(t),18*env(t),0]),Jaw:R(t=>[-10*env(t),0,0])};
 const haunt={Body:R(t=>[-2,2.4*wave(t),0]),Head:R(t=>[1,-1.8*wave(t-.08),0]),Jaw:R(t=>[-3-2*wave(t-.1),0,0])};
 const dream={Body:[R(t=>[curve(t,[[0,0],[.28,5],[.55,-11],[.72,-7],[1,0]]),0,0]),P(t=>[0,0,-1.5*env(t)])],Head:R(t=>[-4*env(t),0,0]),Jaw:R(t=>[curve(t,[[0,0],[.36,-22],[.57,-2],[.68,-5],[1,0]]),0,0])};
 const grasp={Body:R(t=>[curve(t,[[0,0],[.25,-5],[.67,4],[.85,-9],[1,0]]),0,0]),Head:R(t=>[2*env(t),0,0])};
 const tag={Head:R(t=>[0,5*env(t),0]),Body:R(t=>[0,-2*env(t),0])};
 const perish={Body:[P(t=>[0,1.1*env(t),0]),R(t=>[2*env(t),0,0])],Head:R(t=>[-4*env(t),0,0]),Jaw:R(t=>[-8*env(t),0,0]),Crown:R(t=>[3*env(t),0,0])};
 for(const s of[-1,1]){
  transform['Arm '+s]=R(t=>[curve(t,[[0,42],[.35,32],[.7,-4],[1,0]]),s*5*env(t),s*14*env(t)]);transform['Forearm '+s]=R(t=>[-12*env(t),0,0]);transform['Hand '+s]=R(t=>[7*env(t),0,-s*5*env(t)]);transform['Ear '+s]=R(t=>[curve(t,[[0,-8],[.65,6],[1,0]]),0,s*4*env(t)]);
  charge['Arm '+s]=R(t=>[42*smooth(t/.8),s*18*smooth(t/.85),s*6*smooth(t)]);charge['Forearm '+s]=R(t=>[10*smooth((t-.08)/.75),s*3*smooth(t),0]);charge['Hand '+s]=R(t=>[-12*smooth((t-.16)/.75),s*6*smooth(t),0]);
  release['Arm '+s]=R(t=>[curve(t,[[0,42],[.27,62],[.68,18],[1,0]]),s*curve(t,[[0,18],[.3,4],[1,0]]),s*6*(1-smooth(t))]);release['Forearm '+s]=R(t=>[curve(t,[[0,10],[.28,-8],[.7,4],[1,0]]),s*3*(1-smooth(t)),0]);release['Hand '+s]=R(t=>[curve(t,[[0,-12],[.3,4],[1,0]]),s*6*(1-smooth(t)),0]);
  passage['Arm '+s]=R(t=>[28*smooth(t/.2),0,s*12*smooth(t/.2)]);emerge['Arm '+s]=R(t=>[(s<0?65:35)*env(t),s*20*env(t),s*18*env(t)]);emerge['Forearm '+s]=R(t=>[-8*env(t),0,0]);
  haunt['Arm '+s]=R(t=>[30+7*wave(t-.15*s),s*8,s*18]);haunt['Forearm '+s]=R(t=>[5+4*wave(t-.22*s),0,0]);haunt['Hand '+s]=R(t=>[-8+3*wave(t-.3*s),s*8*wave(t-.2),s*5]);
  dream['Arm '+s]=R(t=>[38*env(t),s*12*env(t),s*8*env(t)]);dream['Forearm '+s]=R(t=>[-7*env(t),0,0]);
  grasp['Arm '+s]=R(t=>[curve(t,[[0,0],[.3,52],[.65,34],[.85,62],[1,0]]),s*14*env(t),s*5*env(t)]);grasp['Forearm '+s]=R(t=>[curve(t,[[0,0],[.3,5],[.65,24],[.85,-8],[1,0]]),0,0]);grasp['Hand '+s]=R(t=>[-10*env(t),s*8*env(t),0]);
  perish['Arm '+s]=R(t=>[55*env(t),s*8*env(t),s*35*env(t)]);perish['Forearm '+s]=R(t=>[10*env(t),0,0]);perish['Hand '+s]=R(t=>[-10*env(t),0,s*6*env(t)]);
  for(const move of[charge,release,dream,grasp,perish])for(let i=0;i<3;i++){const phase=i*.025;move['Claw '+s+' '+i]=R(t=>[(move===charge?10*smooth(t):move===release?10*(1-smooth(t)):14*env(Math.max(0,t-phase))),0,0]);}
  for(const move of[haunt,perish])move['Ear '+s]=R(t=>[2*wave(t-.12)* (move===perish?env(t):1),0,s*1.5*wave(t-.2)]);
 }
 tag['Arm -1']=R(t=>[38*env(t),-10*env(t),-8*env(t)]);tag['Forearm -1']=R(t=>[8*env(t),0,0]);tag['Hand -1']=R(t=>[-7*env(t),-8*env(t),0]);
 clip('transform',1.6,false,transform);clip('charge',2,false,charge);clip('release',.6,false,release);clip('passage',2,false,passage);clip('emerge',.7,false,emerge);clip('haunt',1.8,true,haunt);clip('dream',1.2,false,dream);clip('grasp',1.5,false,grasp);clip('tag',5,false,tag);clip('perish',6,false,perish);
 clip('melee',.55,false,{Body:R(t=>[-3*env(t),curve(t,[[0,0],[.25,-8],[.55,18],[1,0]]),0]),'Arm -1':R(t=>[curve(t,[[0,0],[.25,-12],[.52,55],[1,0]]),-22*env(t),-8*env(t)]),'Forearm -1':R(t=>[curve(t,[[0,0],[.3,12],[.6,-5],[1,0]]),0,0]),'Hand -1':R(t=>[-6*env(t),0,0])});
 return m;
}
async function icons(){const ids=['mega_phantom_form','shadow_ball','phantom_passage','nightmare_haunting','dream_eater','spectral_grasp','shadow_tag','perish_curse'];
 for(let k=0;k<ids.length;k++){let c=canvas(64);c.ellipse(32,32,29,29,'21122f');c.ellipse(32,32,25,25,'67429a');
  if(k===0||k===3){c.poly([[12,38],[12,20],[19,25],[24,10],[29,23],[41,16],[48,31],[50,49],[42,44],[32,50],[21,44]],'ae6ed0');c.poly([[18,28],[28,33],[20,35]],'fff0b7');c.poly([[43,28],[33,33],[41,35]],'fff0b7');c.line(22,40,40,40,3,'fff0dd');}
  if(k===1){c.ellipse(32,32,18,18,'a263ee');c.ellipse(30,31,13,13,'211132');c.line(10,46,18,36,3,'ed84e4');c.line(44,17,53,9,2,'ed84e4');}
  if(k===2){c.ellipse(32,44,23,8,'180e24');c.line(15,35,40,22,4,'ed84e4');c.poly([[35,15],[51,18],[44,33]],'e8c4ff');}
  if(k===4){c.poly([[10,21],[32,29],[54,21],[47,46],[32,52],[17,46]],'170e21');for(let i=0;i<6;i++)c.poly([[14+i*6,26],[19+i*6,28],[17+i*6,37]],'fff1cb');c.ellipse(32,43,8,4,'de63c1');}
  if(k===5){for(let s of[-1,1]){c.rect(28+s*15,29,9,20,'d997f2');for(let f=0;f<3;f++)c.line(28+s*15+f*3,30,28+s*15+f*3-s*3,16+f,2,'f0ccff');}}
  if(k===6){c.ellipse(32,32,23,13,'e4b754');c.ellipse(32,32,8,11,'35204c');c.line(32,7,32,16,2,'eecbff');c.line(32,48,32,56,2,'eecbff');}
  if(k===7){c.ellipse(32,39,22,13,'f19ae8');c.ellipse(32,39,19,10,'21122f');for(let i=0;i<6;i++){let a=i*Math.PI/3;c.line(32+Math.cos(a)*19,39+Math.sin(a)*10,32+Math.cos(a)*24,39+Math.sin(a)*14,2,'f6d5ff');}c.ellipse(32,22,11,7,'f3d982');c.ellipse(32,22,3,6,'312042');}
  write('src/main/resources/assets/kazimod/textures/abilities/gengar/'+ids[k]+'.png',await sharp(c.data,{raw:{width:64,height:64,channels:4}}).png().toBuffer());
 }
}
function bakeModel(model){
 const meshes=new Map(model.elements.filter(e=>e.type==='mesh').map(e=>[e.name,e]));
 const nodes=bake({...model,elements:model.elements.map(e=>e.type==='mesh'?{...e,from:[0,0,0],to:[0,0,0],faces:{}}:e)});
 const walk=list=>{for(const n of list){if(n.children){walk(n.children);continue;}const e=meshes.get(n.name);if(!e)continue;n.quads=[];
  for(const f of Object.values(e.faces)){const vv=f.vertices.map(key=>[...e.vertices[key],f.uv[key][0]/256,f.uv[key][1]/256]);
   const[a,b,c]=vv,ab=b.map((v,i)=>v-a[i]),ac=c.map((v,i)=>v-a[i]),normal=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]],length=Math.hypot(...normal);
   assert(length>1e-7,'Degenerate mesh face: '+e.name);assert(vv.length===3||vv.length===4);if(vv.length===3)vv.push(vv[2]);n.quads.push({vertices:vv,normal:normal.map(v=>v/length)});
  }
 }};walk(nodes);return nodes;
}
(async()=>{let model=build(),png=await atlas();model.textures=[{uuid:uid('texture'),name:'mega_gengar.png',id:'0',width:256,height:256,uv_width:256,uv_height:256,source:'data:image/png;base64,'+png.toString('base64'),mode:'bitmap',internal:true}];
 let rig={sourceHash:crypto.createHash('sha256').update(JSON.stringify(model)).digest('hex'),nodes:bakeModel(model),animations:clipsFrom(model)};
 assert(model.elements.length<60);assert(rig.animations.length===13);write('deliverables/gengar/mega_gengar.bbmodel',JSON.stringify(model));write('src/main/resources/assets/kazimod/models/entities/mega_gengar.json',JSON.stringify(rig));write('src/main/resources/assets/kazimod/textures/entities/mega_gengar.png',png);if(!process.argv.includes('--model-only'))await icons();console.log('Mega Gengar: '+model.elements.length+' low-poly parts, '+model.groups.length+' bones, '+rig.animations.length+' clips');
})().catch(e=>{console.error(e);process.exit(1);});
