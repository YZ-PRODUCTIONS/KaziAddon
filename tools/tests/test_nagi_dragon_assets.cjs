/* Validates editable Blockbench dragons against their exact in-game geometry.
 * node tools/tests/test_nagi_dragon_assets.cjs [--preview]
 * The optional preview is a real textured, depth-buffered render of the runtime rigs.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const assert = require('assert');
const root = path.resolve(__dirname, '../..');
const kinds = ['first', 'blind', 'cobra', 'four_eyed', 'corpse_flame', 'ice_crystal', 'spearhead', 'angel'];
const assets = path.join(root, 'src/main/resources/assets/kazimod');
const EPS = 0.0003;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
let checks = 0;
function check(ok, message) { checks++; assert(ok, message); }
function near(a, b, epsilon = EPS) { return Math.abs(a - b) <= epsilon; }
function vector(v, length) { return Array.isArray(v) && v.length === length && v.every(Number.isFinite); }
const sub = (a, b) => a.map((v, i) => v - b[i]);
const cross = (a, b) => [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]];
const dot = (a, b) => a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
const unit = v => { const n = Math.hypot(...v); return v.map(x => x/n); };
function rotate(point, rotation) {
    let [x,y,z] = point;
    for (let i = 0; i < 3; i++) {
        const a = rotation[i] * Math.PI/180, c = Math.cos(a), s = Math.sin(a);
        if (i === 0) [y,z] = [y*c-z*s,y*s+z*c];
        if (i === 1) [x,z] = [x*c+z*s,-x*s+z*c];
        if (i === 2) [x,y] = [x*c-y*s,x*s+y*c];
    }
    return [x,y,z];
}
function pngDimensions(buffer) {
    check(buffer.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])), 'PNG signature');
    return [buffer.readUInt32BE(16), buffer.readUInt32BE(20)];
}
function runtimeNodes(nodes, result = []) {
    for (const node of nodes) {
        result.push(node);
        if (node.children) runtimeNodes(node.children, result);
    }
    return result;
}
function validate(id) {
    const prefix = id + ': ';
    const filename = path.join(root, 'deliverables/nagi_dragons', id + '.bbmodel');
    const runtimeFile = path.join(assets, 'models/nagi/dragons', id + '.json');
    const textureFile = path.join(assets, 'textures/models/nagi/dragons', id + '.png');
    for (const p of [filename, runtimeFile, textureFile]) check(fs.existsSync(p), prefix + 'missing ' + p);
    const source = JSON.parse(fs.readFileSync(filename, 'utf8'));
    const rig = JSON.parse(fs.readFileSync(runtimeFile, 'utf8'));
    const texture = fs.readFileSync(textureFile);
    check(source.meta.model_format === 'free', prefix + 'editable mesh project format');
    check(source.resolution.width === 256 && source.resolution.height === 256, prefix + '256px UV atlas');
    check(pngDimensions(texture).every(n => n === 256), prefix + '256px runtime texture');
    check(source.textures.length === 1, prefix + 'single texture atlas');
    const embedded = source.textures[0].source;
    check(typeof embedded === 'string' && embedded.startsWith('data:image/png;base64,'), prefix + 'embedded texture');
    check(Buffer.from(embedded.split(',')[1], 'base64').equals(texture), prefix + 'editable/runtime textures match exactly');
    check(rig.sourceHash === crypto.createHash('sha256').update(JSON.stringify(source)).digest('hex'), prefix + 'source hash matches editable model');
    check(Array.isArray(source.elements) && source.elements.length > 20, prefix + 'substantial authored mesh');
    const ids = new Set(), names = new Set(), elementMap = new Map();
    for (const element of source.elements) {
        check(UUID.test(element.uuid) && !ids.has(element.uuid), prefix + 'unique valid mesh UUID');
        check(typeof element.name === 'string' && !names.has(element.name), prefix + 'unique mesh name ' + element.name);
        ids.add(element.uuid); names.add(element.name); elementMap.set(element.uuid, element);
        check(element.type === 'mesh', prefix + 'mesh-only source (no blocky cube fallback)');
        check(vector(element.origin,3) && vector(element.rotation || [0,0,0],3), prefix + 'finite mesh transform');
    }
    const groupNames = new Set(), groupIds = new Set();
    for (const group of source.groups) {
        check(UUID.test(group.uuid) && !ids.has(group.uuid) && !groupIds.has(group.uuid), prefix + 'unique valid group UUID');
        check(!groupNames.has(group.name), prefix + 'unique bone name ' + group.name);
        check(vector(group.origin,3) && vector(group.rotation || [0,0,0],3), prefix + 'finite bone transform');
        groupNames.add(group.name); groupIds.add(group.uuid);
    }
    check(groupNames.has('Root') || groupNames.has('root'), prefix + 'root bone');
    for (const bone of ['Head','Jaw', ...Array.from({length:12},(_,i)=>'body_'+i)]) check(groupNames.has(bone), prefix + 'bone ' + bone);
    const included = new Set(), includedGroups = new Set();
    function visitOutliner(nodes) {
        for (const node of nodes) {
            if (typeof node === 'string') {
                check(elementMap.has(node) && !included.has(node), prefix + 'outliner has unique valid mesh references');
                included.add(node);
            } else {
                check(groupIds.has(node.uuid) && !includedGroups.has(node.uuid), prefix + 'outliner has unique valid group references');
                includedGroups.add(node.uuid); visitOutliner(node.children);
            }
        }
    }
    visitOutliner(source.outliner);
    check(included.size === source.elements.length && includedGroups.size === source.groups.length, prefix + 'no orphan meshes or bones');
    const nodes = runtimeNodes(rig.nodes), leaves = nodes.filter(n => n.quads), groups = nodes.filter(n => n.children);
    check(leaves.length === source.elements.length && groups.length === source.groups.length, prefix + 'source/runtime node parity');
    const byName = new Map(nodes.map(n => [n.name,n]));
    check(byName.size === nodes.length, prefix + 'unique runtime node names');
    for (const group of source.groups) {
        const runtime = byName.get(group.name);
        check(runtime && runtime.children && vector(runtime.origin,3) && vector(runtime.rotation,3), prefix + 'runtime bone ' + group.name);
        check(runtime.origin.every((x,i)=>near(x,group.origin[i])) && runtime.rotation.every((x,i)=>near(x,(group.rotation||[0,0,0])[i])), prefix + 'bone rest-transform parity');
    }
    let totalQuads = 0, closedMeshes = 0;
    const bounds = [[Infinity,Infinity,Infinity],[-Infinity,-Infinity,-Infinity]];
    for (const element of source.elements) {
        const runtime = byName.get(element.name);
        check(runtime && Array.isArray(runtime.quads), prefix + 'runtime mesh ' + element.name);
        const faces = Object.values(element.faces).filter(f => f.texture !== null && f.vertices.length >= 3);
        check(faces.length > 0 && runtime.quads.length === faces.length, prefix + 'face-count parity ' + element.name);
        const edges = new Map();
        let volume = 0;
        for (let f = 0; f < faces.length; f++) {
            const face = faces[f], quad = runtime.quads[f];
            check(face.vertices.length >= 3 && face.vertices.length <= 4, prefix + 'triangles/quads only');
            check(new Set(face.vertices).size === face.vertices.length, prefix + 'non-repeated face corners');
            check(face.texture === 0, prefix + 'face uses atlas 0');
            const positions = face.vertices.map(k => element.vertices[k]);
            check(positions.every(p => vector(p,3)), prefix + 'face references finite vertices');
            const normal = cross(sub(positions[1], positions[0]), sub(positions[2], positions[0]));
            check(Math.hypot(...normal) > 1e-7, prefix + 'nondegenerate face ' + element.name);
            check(vector(quad.normal,3) && near(Math.hypot(...quad.normal),1), prefix + 'finite unit normal');
            check(quad.vertices.length === 4 && quad.vertices.every(v => vector(v,5)), prefix + 'complete runtime quad');
            const expectedNormal = rotate(unit(normal), element.rotation || [0,0,0]);
            check(dot(expectedNormal,quad.normal) > .9999, prefix + 'normal orientation matches source winding');
            for (let j = 0; j < 4; j++) {
                const k = face.vertices[Math.min(j,face.vertices.length-1)], uv = face.uv[k];
                check(vector(uv,2) && uv.every(v => v >= 0 && v <= 256), prefix + 'atlas UV in bounds');
                const expected = rotate(element.vertices[k],element.rotation||[0,0,0]).map((v,i)=>v+element.origin[i]);
                expected.push(uv[0]/256,uv[1]/256);
                check(expected.every((v,i)=>near(v,quad.vertices[j][i])), prefix + 'exact source/runtime vertex and UV parity');
                for (let a = 0; a < 3; a++) {
                    bounds[0][a] = Math.min(bounds[0][a],expected[a]);
                    bounds[1][a] = Math.max(bounds[1][a],expected[a]);
                }
            }
            for (let j = 0; j < face.vertices.length; j++) {
                const a = face.vertices[j], b = face.vertices[(j+1)%face.vertices.length];
                const key = [a,b].sort().join('|');
                if (!edges.has(key)) edges.set(key,[]);
                edges.get(key).push(a+'>'+b);
            }
            for (let j = 1; j < positions.length-1; j++) volume += dot(positions[0],cross(positions[j],positions[j+1]))/6;
        }
        const closed = [...edges.values()].every(v => v.length === 2);
        if (closed) {
            check([...edges.values()].every(v => v[0] !== v[1]), prefix + 'consistent closed mesh winding ' + element.name);
            // Signed volume is reliable for closed solids, unlike centroid-based tests on bent necks.
            check(volume > 1e-7, prefix + 'outward closed mesh normals ' + element.name);
            closedMeshes++;
        }
        totalQuads += runtime.quads.length;
    }
    check(totalQuads > 200 && totalQuads < 10000, prefix + 'bounded detailed geometry: ' + totalQuads + ' quads');
    check(closedMeshes > 0, prefix + 'watertight outward-facing primitive meshes');
    check(bounds.flat().every(Number.isFinite) && bounds[1].every((v,i)=>v > bounds[0][i]), prefix + 'finite 3D bounds');
    const clips = new Map(rig.animations.map(c => [c.name,c]));
    check(clips.has('swim') && clips.get('swim').loop, prefix + 'looping swim animation');
    check(clips.has('bite') && !clips.get('bite').loop, prefix + 'one-shot bite animation');
    for (const clip of rig.animations) {
        check(clip.length > 0 && Number.isFinite(clip.length), prefix + 'finite clip duration');
        const authored = source.animations.find(a => a.name.split('.').pop() === clip.name);
        check(authored && near(authored.length,clip.length) && (authored.loop==='loop') === clip.loop, prefix + 'clip metadata parity');
        check(clip.tracks.length > 0, prefix + 'nonempty animation');
        for (const track of clip.tracks) {
            check(groupNames.has(track.bone), prefix + 'animation references existing bone');
            for (const key of track.keys) {
                check(['rotation','position'].includes(key.channel) && vector(key.value,3), prefix + 'supported finite animation key');
                check(Number.isFinite(key.time) && key.time >= 0 && key.time <= clip.length+EPS, prefix + 'key time within clip');
            }
            for (const channel of ['rotation','position']) {
                const keys = track.keys.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);
                if (!keys.length) continue;
                check(keys.length >= 2 && near(keys[0].time,0) && near(keys[keys.length-1].time,clip.length), prefix + 'complete clip endpoints');
                if (clip.loop) check(keys[0].value.every((v,i)=>near(v,keys[keys.length-1].value[i])), prefix + 'seamless loop ' + track.bone + ' ' + channel);
            }
        }
    }
    const motion = validateMotion(id,source,rig);
    console.log(id.padEnd(15) + source.elements.length + ' meshes; ' + totalQuads + ' quads; ' + source.groups.length + ' bones; ' + closedMeshes + ' closed solids');
    console.log('  Articulation: ' + motion.maxBend.toFixed(2) + 'deg max joint bend; ' + motion.minMargin.toFixed(3) + ' model-unit minimum skin overlap margin');
    return {id,source,rig,texture,textureFile,totalQuads};
}

// Column-major transforms follow the production renderer: parent * pivot * Rz * Ry * Rx * -pivot.
const identity = () => [1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1];
function multiply(a,b) {
    const result = new Array(16).fill(0);
    for(let col=0;col<4;col++)for(let row=0;row<4;row++)for(let k=0;k<4;k++)result[col*4+row]+=a[k*4+row]*b[col*4+k];
    return result;
}
function translate(x,y,z) { const m=identity();m[12]=x;m[13]=y;m[14]=z;return m; }
function rotation(r) {
    const m=identity();
    for(let i=0;i<3;i++){const a=rotate([+(i===0),+(i===1),+(i===2)],r);for(let j=0;j<3;j++)m[i*4+j]=a[j];}
    return m;
}
function transform(m,v,normal=false) { return [0,1,2].map(i=>m[i]*v[0]+m[4+i]*v[1]+m[8+i]*v[2]+(normal?0:m[12+i])); }
function sample(rig,clipName,time) {
    const clip=rig.animations.find(c=>c.name===clipName),pose=new Map();
    if(!clip)return pose;
    time=clip.loop?time%clip.length:Math.max(0,Math.min(time,clip.length));
    for(const track of clip.tracks){
        const p={position:[0,0,0],rotation:[0,0,0]};
        for(const channel of ['rotation','position']){
            const keys=track.keys.filter(k=>k.channel===channel).sort((a,b)=>a.time-b.time);if(!keys.length)continue;
            let before=keys[0],after=keys[keys.length-1];
            for(const key of keys){if(key.time<=time)before=key;if(key.time>=time){after=key;break;}}
            const f=before.time===after.time?0:(time-before.time)/(after.time-before.time);
            p[channel]=before.value.map((v,i)=>v+(after.value[i]-v)*f);
        }
        pose.set(track.bone,p);
    }
    return pose;
}
function blendPoses(...poses) {
    const result=new Map();
    for(const pose of poses)for(const[name,value]of pose){
        if(!result.has(name))result.set(name,{position:[0,0,0],rotation:[0,0,0]});
        const target=result.get(name);
        for(const channel of ['rotation','position'])for(let axis=0;axis<3;axis++)target[channel][axis]+=value[channel][axis];
    }
    return result;
}
function groupTransform(matrix,node,pose){
    const p=pose.get(node.name)||{position:[0,0,0],rotation:[0,0,0]},o=node.origin,r=node.rotation.map((v,i)=>v+p.rotation[i]);
    let m=multiply(matrix,translate(...p.position));
    m=multiply(m,translate(...o));m=multiply(m,rotation(r));return multiply(m,translate(...o.map(v=>-v)));
}
function boneMatrices(rig,pose){
    const result=new Map();
    function walk(nodes,matrix){for(const node of nodes)if(node.children){const m=groupTransform(matrix,node,pose);result.set(node.name,m);walk(node.children,m);}}
    walk(rig.nodes,identity());return result;
}
function capExtent(element,end){
    const vertices=Object.values(element.vertices),z=end==='front'?Math.max(...vertices.map(v=>v[2])):Math.min(...vertices.map(v=>v[2]));
    const ring=vertices.filter(v=>near(v[2],z,.00001)),center=[0,1].map(i=>ring.reduce((n,v)=>n+v[i],0)/ring.length);
    return {z,radius:Math.max(...ring.map(v=>Math.hypot(v[0]-center[0],v[1]-center[1])))};
}
function validateMotion(id,source,rig){
    const prefix=id+': ',swim=rig.animations.find(c=>c.name==='swim'),bite=rig.animations.find(c=>c.name==='bite');
    const bones=new Map(source.groups.map(g=>[g.name,g])),segments=[];
    for(let i=0;i<12;i++){
        const element=source.elements.find(e=>e.name.startsWith('Segment '+i+' '));
        check(element,prefix+'continuous skin segment '+i);
        segments.push({front:capExtent(element,'front'),back:capExtent(element,'back')});
    }
    let maxBend=0,minMargin=Infinity;
    for(let frame=0;frame<=56;frame++){
        const time=swim.length*frame/56,matrices=boneMatrices(rig,sample(rig,'swim',time));
        for(const matrix of matrices.values())check(matrix.every(Number.isFinite),prefix+'finite animated bone matrix');
        for(let i=1;i<12;i++){
            const a=matrices.get('body_'+(i-1)),b=matrices.get('body_'+i),pivot=bones.get('body_'+i).origin;
            check(Math.hypot(...sub(transform(a,pivot),transform(b,pivot)))<.001,prefix+'body hinge remains connected');
            const directionA=unit(transform(a,[0,0,1],true)),directionB=unit(transform(b,[0,0,1],true));
            const bend=Math.acos(Math.max(-1,Math.min(1,dot(directionA,directionB))));
            const parent=segments[i-1].back,child=segments[i].front;
            // Adjacent rigid skins need real overlap: caps sharing only a plane split into
            // a visible wedge as the child turns. The conservative radial bound covers
            // the largest edge displacement over the full sampled animation cycle.
            const overlap=child.z-parent.z;
            const required=Math.max(parent.radius,child.radius)*Math.sin(bend);
            const margin=overlap-required;
            maxBend=Math.max(maxBend,bend*180/Math.PI);minMargin=Math.min(minMargin,margin);
            check(overlap>0&&margin>=-.001,prefix+'articulated skin overlap at joint '+i+' frame '+frame+' (margin '+margin.toFixed(3)+')');
        }
    }
    // Bite overlays swim in production. It must return to precisely the same idle pose,
    // maintain the jaw hinge, and open the jaw in the middle rather than snap at the ends.
    const idle=sample(rig,'swim',.713),start=boneMatrices(rig,blendPoses(idle,sample(rig,'bite',0)));
    const finish=boneMatrices(rig,blendPoses(idle,sample(rig,'bite',bite.length)));
    for(const[name,matrix]of start)check(matrix.every((v,i)=>near(v,finish.get(name)[i],.0001)),prefix+'bite returns to its original pose '+name);
    let jawMotion=0;
    for(let frame=0;frame<=24;frame++){
        const pose=blendPoses(idle,sample(rig,'bite',bite.length*frame/24)),m=boneMatrices(rig,pose);
        const pivot=bones.get('Jaw').origin;
        check(Math.hypot(...sub(transform(m.get('Head'),pivot),transform(m.get('Jaw'),pivot)))<.001,prefix+'jaw hinge remains connected');
        for(const matrix of m.values())check(matrix.every(Number.isFinite),prefix+'finite bite pose');
        jawMotion=Math.max(jawMotion,Math.abs(pose.get('Jaw').rotation[0]-idle.get('Jaw').rotation[0]));
    }
    check(jawMotion>5,prefix+'distinct animated jaw opening');
    return{maxBend,minMargin};
}
function transformedFaces(rig,clipName,time) {
    const faces=[],pose=sample(rig,clipName,time);
    function walk(nodes,matrix){
        for(const node of nodes){
            if(!node.visible)continue;
            if(node.children){
                walk(node.children,groupTransform(matrix,node,pose));
            }else for(const quad of node.quads||[])faces.push({name:node.name,vertices:quad.vertices.map(v=>[...transform(matrix,v),v[3],v[4]]),normal:unit(transform(matrix,quad.normal,true))});
        }
    }
    walk(rig.nodes,identity());return faces;
}
function renderPanel(faces,texture,width,height,angle,elevation=.19,framingFaces=faces) {
    const image=Buffer.alloc(width*height*4),depth=new Float32Array(width*height).fill(-Infinity);
    for(let y=0;y<height;y++)for(let x=0;x<width;x++){
        const f=1-Math.min(1,Math.hypot((x-width*.5)/(width*.8),(y-height*.5)/(height*.9)));
        const i=(y*width+x)*4;image[i]=Math.round(19+f*9);image[i+1]=Math.round(24+f*12);image[i+2]=Math.round(34+f*17);image[i+3]=255;
    }
    const sy=Math.sin(angle),cy=Math.cos(angle),se=Math.sin(elevation),ce=Math.cos(elevation);
    const project=v=>[v[0]*cy-v[2]*sy,v[1]*ce-(v[0]*sy+v[2]*cy)*se,v[1]*se+(v[0]*sy+v[2]*cy)*ce];
    const lo=[Infinity,Infinity,Infinity],hi=[-Infinity,-Infinity,-Infinity];
    for(const face of framingFaces)for(const vertex of face.vertices){const p=project(vertex);for(let i=0;i<3;i++){lo[i]=Math.min(lo[i],p[i]);hi[i]=Math.max(hi[i],p[i]);}}
    const scale=Math.min((width-60)/(hi[0]-lo[0]),(height-65)/(hi[1]-lo[1]));
    const cx=(lo[0]+hi[0])*.5,cy2=(lo[1]+hi[1])*.5;
    const light=unit([-.4,.8,.5]),fill=unit([.8,.3,-.4]);
    for(const face of faces){
        // The runtime translucent material is two-sided. These geometry inspection
        // previews stay opaque so hidden surfaces do not obscure UV and seam defects.
        const shade=Math.min(1.25,.43+Math.max(0,dot(face.normal,light))*.64+Math.max(0,dot(face.normal,fill))*.12);
        const points=face.vertices.map(v=>{const p=project(v);return[(p[0]-cx)*scale+width/2,height/2-(p[1]-cy2)*scale,p[2],v[3],v[4]];});
        for(const indices of [[0,1,2],[0,2,3]]){
            const [a,b,c]=indices.map(i=>points[i]);
            const area=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);if(Math.abs(area)<1e-9)continue;
            const minX=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))),maxX=Math.min(width-1,Math.ceil(Math.max(a[0],b[0],c[0])));
            const minY=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))),maxY=Math.min(height-1,Math.ceil(Math.max(a[1],b[1],c[1])));
            for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){
                const px=x+.5,py=y+.5;
                const w0=((b[0]-px)*(c[1]-py)-(b[1]-py)*(c[0]-px))/area;
                const w1=((c[0]-px)*(a[1]-py)-(c[1]-py)*(a[0]-px))/area,w2=1-w0-w1;
                if(w0<-.00001||w1<-.00001||w2<-.00001)continue;
                const z=w0*a[2]+w1*b[2]+w2*c[2],pixel=y*width+x;if(z<depth[pixel])continue;
                const u=Math.max(0,Math.min(255,Math.floor((w0*a[3]+w1*b[3]+w2*c[3])*256)));
                const v=Math.max(0,Math.min(255,Math.floor((w0*a[4]+w1*b[4]+w2*c[4])*256)));
                const tex=(v*256+u)*4,alpha=texture[tex+3]/255;if(alpha<.05)continue;
                const p=pixel*4;
                for(let ch=0;ch<3;ch++)image[p+ch]=Math.round(Math.min(255,texture[tex+ch]*shade)*alpha+image[p+ch]*(1-alpha));
                depth[pixel]=z;
            }
        }
    }
    return image;
}
async function previews(models) {
    const sharp=require(process.env.SHARP_MODULE||'C:/Users/th3wyz/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
    const out=path.join(root,'build/nagi-dragon-previews');fs.mkdirSync(out,{recursive:true});
    const width=768,height=550,composites=[],headComposites=[];
    for(let index=0;index<models.length;index++){
        const model=models[index],texture=await sharp(model.texture).ensureAlpha().raw().toBuffer();
        const faces=transformedFaces(model.rig,'swim',.45);
        const headRoot=runtimeNodes(model.rig.nodes).find(n=>n.name==='Head');
        const headNames=new Set(runtimeNodes([headRoot]).filter(n=>n.quads).map(n=>n.name));
        const headFaces=faces.filter(f=>headNames.has(f.name));
        const headRaw=renderPanel(headFaces,texture,width,height,.78,.14);
        const headLabel=Buffer.from('<svg width="'+width+'" height="'+height+'"><text x="24" y="34" fill="#e6e9ef" font-family="Segoe UI,Arial" font-size="19" font-weight="600">'+model.id.replace(/_/g,' ').toUpperCase()+' | HEAD DETAIL</text></svg>');
        const headPng=await sharp(headRaw,{raw:{width,height,channels:4}}).composite([{input:headLabel}]).png().toBuffer();
        headComposites.push({input:headPng,left:index%2*width,top:Math.floor(index/2)*height});
        await sharp(headPng).toFile(path.join(out,model.id+'-head.png'));
        const angles=[.92,Math.PI/2,Math.PI-.6],individual=[];
        for(let view=0;view<angles.length;view++){
            const raw=renderPanel(faces,texture,width,height,angles[view],view===1?.08:.20);
            const label=model.id.replace(/_/g,' ').toUpperCase()+' | '+['THREE QUARTER','PROFILE','REAR THREE QUARTER'][view];
            const svg=Buffer.from('<svg width="'+width+'" height="'+height+'"><text x="24" y="34" fill="#e6e9ef" font-family="Segoe UI,Arial" font-size="19" font-weight="600">'+label+'</text><text x="24" y="'+(height-18)+'" fill="#90a0b4" font-family="Segoe UI,Arial" font-size="13">Runtime geometry / 256px texture / '+model.totalQuads+' quads</text></svg>');
            const png=await sharp(raw,{raw:{width,height,channels:4}}).composite([{input:svg}]).png().toBuffer();
            individual.push({input:png,left:view*width,top:0});
            if(view===0)composites.push({input:png,left:index%2*width,top:Math.floor(index/2)*height});
        }
        await sharp({create:{width:width*3,height,channels:4,background:'#151b25'}}).composite(individual).png().toFile(path.join(out,model.id+'.png'));
    }
    const sheet=path.join(out,'all-dragons.png');
    await sharp({create:{width:width*2,height:height*4,channels:4,background:'#151b25'}}).composite(composites).png().toFile(sheet);
    await sharp({create:{width:width*2,height:height*4,channels:4,background:'#151b25'}}).composite(headComposites).png().toFile(path.join(out,'head-details.png'));
    const first=models[0],texture=await sharp(first.texture).ensureAlpha().raw().toBuffer(),swim=first.rig.animations.find(c=>c.name==='swim');
    const phases=Array.from({length:8},(_,i)=>transformedFaces(first.rig,'swim',swim.length*i/8)),framing=phases.flat(),phaseImages=[];
    for(let phase=0;phase<8;phase++){
        const raw=renderPanel(phases[phase],texture,640,440,.84,.43,framing);
        const svg=Buffer.from('<svg width="640" height="440"><text x="24" y="32" fill="#e6e9ef" font-family="Segoe UI,Arial" font-size="18">SWIM PHASE '+phase+'/8</text></svg>');
        phaseImages.push({input:await sharp(raw,{raw:{width:640,height:440,channels:4}}).composite([{input:svg}]).png().toBuffer(),left:phase%4*640,top:Math.floor(phase/4)*440});
    }
    await sharp({create:{width:2560,height:880,channels:4,background:'#151b25'}}).composite(phaseImages).png().toFile(path.join(out,'first-swim-phases.png'));
    console.log('Textured previews: '+sheet);
}
if(require.main===module)(async()=>{
    const models=kinds.map(validate);
    console.log('Nagi dragon asset checks: '+checks+' passed.');
    if(process.argv.includes('--preview'))await previews(models);
})().catch(error=>{console.error(error);process.exitCode=1;});
module.exports={validate,transformedFaces,renderPanel};
