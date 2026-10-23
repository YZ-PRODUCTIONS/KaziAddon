// Faceted Blockbench meshes and hand-authored pixel materials for the four Tusk Acts.
const crypto = require('crypto');
const sharp = require(process.env.SHARP_MODULE || 'C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const MATERIALS = ['pink','pale','rose','navy','steel','gold','dark','armor','stars','ear','face1','face2','face3','face4','hood','pod','spine','chest','shoulder','arm','shin','glove','disc','discBack','heart','beak','port','trim','blueStar','joint','pincer','sole'];
const SIZE = 512, TILE = 64;
const uid = s => { const h = crypto.createHash('md5').update(s).digest('hex'); return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`; };
const rgb = hex => hex.match(/../g).map(v => parseInt(v,16));
const blend = (a,b,t) => a.map((v,i) => Math.round(v+(b[i]-v)*t));

async function atlas() {
    const pixels = Buffer.alloc(SIZE*SIZE*4);
    const palettes = {
        pink:['a5487a','df85b4','f4c1da'], pale:['b96d98','edabc9','ffe1e9'], rose:['682b59','ac4c83','d882ad'],
        navy:['24273f','425c91','94b2da'], steel:['455755','90a99c','dae9d0'], gold:['856028','d5af50','fff0aa'],
        dark:['171629','242238','41354d'], armor:['873964','dc7eae','f7c2dc']
    };
    const bases = {stars:'pink',ear:'rose',face1:'pale',face2:'pink',face3:'pink',face4:'pink',hood:'pink',pod:'pink',spine:'navy',chest:'pink',shoulder:'pink',arm:'pink',shin:'pink',glove:'navy',disc:'pink',discBack:'rose',heart:'rose',beak:'gold',port:'navy',trim:'navy',blueStar:'navy',joint:'steel',pincer:'pale',sole:'navy'};
    for (let index=0;index<MATERIALS.length;index++) {
        const name=MATERIALS[index],ox=index%8*TILE,oy=Math.floor(index/8)*TILE;
        const [lo,mid,hi]=(palettes[name]||palettes[bases[name]]).map(rgb);
        // Each painted pixel is a 2x2 cluster, giving a clean, consistent texel scale.
        const dot=(x,y,c,a=255)=>{x=Math.floor(x);y=Math.floor(y);if(x<0||x>=32||y<0||y>=32)return;
            const color=Array.isArray(c)?c:rgb(c);for(let yy=0;yy<2;yy++)for(let xx=0;xx<2;xx++){
                const p=((oy+y*2+yy)*SIZE+ox+x*2+xx)*4;pixels[p]=color[0];pixels[p+1]=color[1];pixels[p+2]=color[2];pixels[p+3]=a;
            }};
        const rect=(x,y,w,h,c)=>{for(let Y=y;Y<y+h;Y++)for(let X=x;X<x+w;X++)dot(X,Y,c);};
        const line=(x,y,X,Y,c,w=1)=>{const n=Math.max(Math.abs(X-x),Math.abs(Y-y),1);for(let i=0;i<=n;i++)rect(Math.round(x+(X-x)*i/n),Math.round(y+(Y-y)*i/n),w,w,c);};
        const poly=(ps,c)=>{for(let y=0;y<32;y++)for(let x=0;x<32;x++){let inside=false;for(let i=0,j=ps.length-1;i<ps.length;j=i++){
            const a=ps[i],b=ps[j];if((a[1]>y)!==(b[1]>y)&&x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])inside=!inside;
        }if(inside)dot(x,y,c);}};
        const oval=(x,y,rx,ry,c)=>{for(let Y=y-ry;Y<=y+ry;Y++)for(let X=x-rx;X<=x+rx;X++)if((X-x)**2/rx**2+(Y-y)**2/ry**2<=1)dot(X,Y,c);};
        const star=(x,y,r,c)=>poly(Array.from({length:10},(_,i)=>{let a=-Math.PI/2+i*Math.PI/5,R=i%2?r*.43:r;return[x+Math.cos(a)*R,y+Math.sin(a)*R];}),c);
        const badge=(x,y,r=3)=>{star(x,y,r+1,'593047');star(x,y,r,'efd478');dot(x-1,y-1,'fff0ad');};
        const rivet=(x,y)=>{oval(x,y,1.5,1.5,'65364f');dot(x,y,'f5d4da');};
        for(let y=0;y<32;y++)for(let x=0;x<32;x++){
            const edge=Math.min(x,31-x),shade=Math.max(0,1-edge/8)*.45 + y/31*.12;
            const highlight=Math.max(0,1-Math.abs(x-11)/12)*Math.max(0,1-Math.abs(y-9)/22)*.36;
            let color=blend(blend(mid,lo,Math.round(shade*8)/8),hi,Math.round(highlight*7)/7);
            if((x===4||x===5)&&y>4&&y<23)color=blend(color,hi,.25);
            dot(x,y,color);
        }
        if(['armor','arm','shin','pincer','shoulder'].includes(name)) {
            line(3,27,9,30,'713653');line(9,30,24,30,'713653');line(24,30,29,26,'713653');
            line(4,25,10,28,'f2b2d0');line(10,28,24,28,'f2b2d0');
        }
        if(name==='stars'||name==='hood'){for(const [x,y,r] of [[7,8,3],[24,14,3],[12,26,2]])badge(x,y,r);}
        if(name==='ear'){badge(12,10,4);badge(22,24,2);line(5,4,3,25,'f1acd0');}
        if(name==='face1'){
            badge(16,5,4);
            for(const x of [8,23]){oval(x,19,4,3,'794268');oval(x,18,3,3,'fff0b8');line(x-3,17,x+3,19,'33263b');line(x,15,x,21,'33263b');dot(x-2,16,'fff9d3');}
            line(3,14,6,13,'b76792');line(25,13,29,15,'b76792');
            oval(16,28,5,2,'713653');line(10,25,8,23,'a1557d');line(22,25,24,23,'a1557d');
        }
        if(name==='face2'){
            for(const [x,y] of [[6,8],[12,4],[20,4],[26,8]])badge(x,y,2);
            oval(10,18,1.2,1.5,'282237');oval(22,18,1.2,1.5,'282237');
            for(let i=0;i<5;i++){const x=6+i*5,y=23+(i===0||i===4?-2:0);rect(x,y,2,31-y,'763451');line(x-1,y,x-1,30,'f7bfd8');}
        }
        if(name==='face3'){
            line(15,0,15,27,'462a49',2);line(14,0,14,24,'f5c9e0');
            for(const x of [7,24]){badge(x,8,3);oval(x,20,1,2,'272237');line(x-1,20,x+1,20,'e3a4c7');}
            const wave=[[2,26],[5,26],[7,29],[10,29],[11,25],[14,25],[16,29],[19,29],[21,25],[24,25],[26,29],[29,26]];
            for(let i=1;i<wave.length;i++)line(...wave[i-1],...wave[i],'6a3259');
            line(2,23,3,13,'c16c9a');line(29,23,28,13,'c16c9a');
        }
        if(name==='face4'){
            line(9,3,9,11,'38263e',3);line(10,12,13,15,'38263e',3);line(13,15,19,15,'38263e',3);line(20,14,23,11,'38263e',3);line(23,3,23,11,'38263e',3);
            line(10,3,10,10,'c5d8c4');line(11,12,14,15,'c5d8c4');line(14,15,20,15,'c5d8c4');line(22,12,24,10,'c5d8c4');line(24,3,24,10,'c5d8c4');
            for(const x of [10,23]){star(x,22,4,'3e263e');dot(x,22,'fae6d6');}
            for(const x of [6,13,21,28]){line(x,30,x-1,27,'743454');line(x-1,27,x-2,29,'743454');}
        }
        if(name==='pod'){
            for(const [x,y] of [[7,11],[24,10],[12,22],[25,25]]){oval(x,y,2.4,2.8,'743954');oval(x,y-1,1.5,1.8,'32344e');dot(x-1,y-2,'aeb6d5');}
            line(1,2,30,2,'f7c1d8');line(3,29,28,29,'783c60');
        }
        if(name==='spine'){for(let y=3;y<32;y+=6){poly([[0,y],[31,y-3],[31,y],[0,y+3]],'b8c5ca');line(2,y+4,28,y+1,'20283d');}}
        if(name==='chest'){
            poly([[0,2],[12,4],[10,7],[0,6]],'432d47');poly([[31,2],[20,4],[22,7],[31,6]],'432d47');
            poly([[0,12],[8,19],[6,23],[0,17]],'4d2e48');poly([[31,12],[24,18],[25,23],[31,18]],'4d2e48');
            for(const [x,y] of [[7,10],[6,16],[10,24],[24,10],[27,16],[22,24]])rivet(x,y);
            line(2,26,9,30,'71314f');line(29,26,23,30,'71314f');
        }
        if(name==='shoulder'){
            poly([[0,14],[7,16],[15,24],[25,15],[31,12],[31,18],[24,21],[15,30],[6,22],[0,20]],'763451');
            line(1,13,8,15,'f6c5da');line(8,15,15,23,'f6c5da');line(15,23,26,14,'f6c5da');
            badge(16,9,3);badge(7,26,2);badge(27,24,2);
        }
        if(name==='arm'){badge(22,11,3);badge(10,21,2);}
        if(name==='shin'){badge(10,9,3);badge(24,20,3);line(3,4,28,4,'743954');}
        if(name==='glove'){
            for(const x of [7,13,19,25]){line(x,4,x,28,'223049');line(x+1,5,x+1,26,'87accb');}
            for(const y of [11,19,26])line(3,y,28,y,'293c58');badge(17,8,5);
        }
        if(name==='disc'||name==='discBack'){
            for(let y=0;y<32;y++)for(let x=0;x<32;x++){
                const radius=Math.min(x,31-x),rim=radius<3||y<2||y>29;
                let c=rim?rgb('71314f'):blend(rgb('b95f96'),rgb('f7bfda'),Math.round((Math.max(0,1-Math.abs(x-13)/15)*Math.max(0,1-Math.abs(y-19)/24))*5)/5);
                if(name==='discBack')c=blend(c,rgb('7f3d6c'),.35);
                if(y<9&&x>4&&x<28&&x%3===0)c=blend(c,rgb('6f355a'),.5*(1-y/10));
                dot(x,y,c);
            }
            line(4,9,3,23,'f7cce0');line(3,23,8,29,'f7cce0');
        }
        if(name==='heart'){line(4,9,10,5,'efacd0',2);line(5,22,14,29,'a64680');}
        if(name==='beak'){line(10,1,10,30,'fff0b1',2);line(24,1,24,30,'987237');for(let x=2;x<32;x+=5)line(x,2,x,4,'9a7537');}
        if(name==='port'){oval(16,16,13,13,'b15d91');oval(16,16,11,11,'25253d');oval(16,16,8,8,'111321');line(8,7,13,4,'a6bce0');}
        if(name==='trim'){poly([[0,6],[13,11],[22,24],[31,17],[31,21],[21,29],[12,15],[0,10]],'e8c574');}
        if(name==='blueStar'){star(16,16,13,'e4be65');star(16,16,10,'bb668d');line(3,29,29,29,'bccde2');}
        if(name==='joint'){oval(16,16,12,12,'526b63');oval(16,15,9,9,'c0d1bc');oval(14,12,4,4,'e8edce');line(16,4,16,26,'718a7c');}
        if(name==='sole'){line(2,25,29,25,'22344e');line(3,22,29,22,'85a9c8');}
    }
    return sharp(pixels,{raw:{width:SIZE,height:SIZE,channels:4}}).png().toBuffer();
}

function refine(source,act) {
    const m=JSON.parse(JSON.stringify(source));m.elements=[];m.textures=[];m.resolution={width:SIZE,height:SIZE};
    const nodes=new Map();
    const clean=items=>{for(const n of items)if(typeof n!=='string'){nodes.set(n.name,n);n.children=n.children.filter(x=>typeof x!=='string');clean(n.children);}};
    clean(m.outliner);
    const groups=new Map(m.groups.map(g=>[g.name,g]));
    function pivot(name,at){nodes.get(name).origin=at.slice();groups.get(name).origin=at.slice();}
    function tile(name){const i=MATERIALS.indexOf(name);if(i<0)throw Error('Unknown material '+name);return[i%8*64,Math.floor(i/8)*64];}
    let serial=0;
    const part=(name,center,vertices,faces,parent,rotation=[0,0,0])=>{
        const e={name,uuid:uid(`tusk_refined_${act}/${serial++}/${name}`),type:'mesh',origin:center,rotation,vertices,faces,visibility:true,export:true,shading:'flat'};
        m.elements.push(e);nodes.get(parent).children.push(e.uuid);return e;
    };
    function prism(name,c,size,material,parent,cut=.22,front=material,back=material,rotation=[0,0,0],taper=1){
        const [w,h,d]=size,a=Math.min(w,h)*cut,poly=[[-w/2+a,h/2],[w/2-a,h/2],[w/2,h/2-a],[w/2,-h/2+a],[w/2-a,-h/2],[-w/2+a,-h/2],[-w/2,-h/2+a],[-w/2,h/2-a]];
        const vertices={},faces={};
        for(let i=0;i<8;i++)for(let z=0;z<2;z++){let[x,y]=poly[i],factor=1+(taper-1)*(y/h+.5);vertices[`${z}_${i}`]=[x*factor,y,(z?1:-1)*d/2*factor];}
        const face=(key,keys,mat,uv)=>{const[u,v]=tile(mat);faces[key]={vertices:keys,uv:Object.fromEntries(keys.map((k,i)=>[k,[u+1+uv[i][0]*62,v+1+uv[i][1]*62]])),texture:0};};
        for(let j=0;j<3;j++){
            const ids=[0,1+j*2,2+j*2,3+j*2];
            face('front'+j,ids.map(i=>'0_'+i),front,ids.map(i=>[(poly[i][0]/w+.5),.5-poly[i][1]/h]));
            const reversed=ids.slice().reverse();face('back'+j,reversed.map(i=>'1_'+i),back,reversed.map(i=>[.5-poly[i][0]/w,.5-poly[i][1]/h]));
        }
        for(let i=0;i<8;i++)face('edge'+i,['0_'+i,'1_'+i,'1_'+((i+1)%8),'0_'+((i+1)%8)],material,[[0,0],[1,0],[1,1],[0,1]]);
        return part(name,c,vertices,faces,parent,rotation);
    }
    function box(name,c,s,mat,parent,rotation=[0,0,0],front=mat){
        const faces={};for(const side of ['north','south','west','east','up','down']){const[u,v]=tile(side==='north'?front:mat);faces[side]={uv:[u+1,v+1,u+63,v+63],texture:0};}
        const e={name,uuid:uid(`tusk_refined_${act}/${serial++}/${name}`),type:'cube',from:c.map((v,i)=>v-s[i]/2),to:c.map((v,i)=>v+s[i]/2),origin:c,rotation,faces,box_uv:false,visibility:true,export:true};
        m.elements.push(e);nodes.get(parent).children.push(e.uuid);return e;
    }
    function beam(name,a,b,w,d,mat,parent,taper=1){
        const v=b.map((n,i)=>n-a[i]),c=a.map((n,i)=>(n+b[i])/2),l=Math.hypot(...v);
        return prism(name,c,[w,l,d],mat,parent,.15,mat,mat,[Math.atan2(v[2],Math.hypot(v[0],v[1]))*180/Math.PI,0,-Math.atan2(v[0],v[1])*180/Math.PI],taper);
    }
    function heart(name,c,parent){
        // A solid, low-poly heart instead of a transparent square on a cuboid.
        const p=[[-1.7,.8],[-1,.8+1.1],[0,1.1],[1,1.9],[1.7,.8],[1.5,-.3],[0,-1.9],[-1.5,-.3]],vertices={},faces={};
        for(let i=0;i<8;i++)for(let z=0;z<2;z++)vertices[`${z}_${i}`]=[p[i][0],p[i][1],z?.28:-.28];
        for(let z=0;z<2;z++){
            vertices['c'+z]=[0,0,z?.28:-.28];
            for(let i=0;i<8;i++){const keys=['c'+z,`${z}_${i}`,`${z}_${(i+1)%8}`];if(z)keys.reverse();const[u,v]=tile('heart');
                faces[`f${z}_${i}`]={vertices:keys,uv:Object.fromEntries(keys.map(k=>[k,[u+32+vertices[k][0]*16,v+32-vertices[k][1]*14]])),texture:0};}
        }
        for(let i=0;i<8;i++){const keys=['0_'+i,'1_'+i,'1_'+((i+1)%8),'0_'+((i+1)%8)],[u,v]=tile('rose');faces['s'+i]={vertices:keys,uv:Object.fromEntries(keys.map((k,j)=>[k,[u+(j%2?58:4),v+(j<2?4:58)]])),texture:0};}
        return part(name,c,vertices,faces,parent);
    }

    if(act===1){
        prism('Tapered star face',[0,17.5,0],[11.5,14.5,5.2],'pink','Head',.25,'face1','stars');
        beam('Forehead ridge',[0,18,-2.7],[0,24.3,-2.1],.3,.22,'gold','Head',.6);
        beam('Golden tusk',[0,15.7,-2.7],[0,13.6,-7.5],2.4,2.4,'beak','Head',.08);
        prism('Lower star body',[0,10.4,.4],[7.5,5.6,4.5],'pink','Body',.3,'stars');
        for(const s of [-1,1]){
            beam('Swept ear base '+s,[s*4,22,0],[s*9.4,25,1],3.2,1.6,'ear','Ear '+s,.7);
            beam('Swept ear tip '+s,[s*9.4,25,1],[s*13.5,22.2,2],2.2,1.3,'pink','Ear '+s,.1);
            beam('Side star point '+s,[s*4.4,14,.2],[s*9.5,12.8,.1],3.5,2.6,'stars','Arm '+s,.18);
            beam('Lower star point '+s,[s*3,10,.4],[s*5.8,7,.3],2.4,2,'pink','Forearm '+s,.06);
            for(let j=0;j<2;j++){
                const b='Ribbon '+s+' '+j,x=s*(3+j*1.7),y=10-j,z=1;
                beam('Curved tendril upper '+s+j,[x,y,z],[x+s*(1+j*.2),6-j*1.1,1.3],.5,.5,'pink',b,.8);
                beam('Curved tendril lower '+s+j,[x+s*(1+j*.2),6-j*1.1,1.3],[x+s*(.3+j*.8),4-j*1.2,1.1],.4,.4,'pink',b,.7);
                heart('Heart tassel '+s+j,[x+s*(.3+j*.8),2.8-j*1.2,1.1],b);
            }
        }
    } else if(act===2){
        prism('Faceted round helmet',[0,23,-.3],[13.5,13,10.5],'navy','Head',.28,'face2','trim');
        for(const s of [-1,1]){
            beam('Bent antenna stalk '+s,[s*4.6,28,0],[s*9.2,28.8,-.3],1.15,1.15,'pink','Ear '+s);
            beam('Bent antenna joint '+s,[s*9.2,28.8,-.3],[s*11.5,26.4,-.5],1.15,1.15,'navy','Ear '+s);
            prism('Hollow antenna cup '+s,[s*12,25.4,-.5],[5,5,2.6],'navy','Ear '+s,.29,'port','navy');
            prism('Shoulder disc '+s,[s*6.3,16.4,-.4],[3.3,3.3,2.6],'steel','Arm '+s,.28,'joint');
            beam('Tapered upper arm '+s,[s*6.5,16.5,0],[s*10,13,-.5],2,2.6,'pink','Arm '+s,.65);
            beam('Pincer cuff '+s,[s*10,13,-.5],[s*10,10.5,-3.4],3.7,3.6,'pincer','Forearm '+s,.85);
            prism('Pincer dark opening '+s,[s*10,10.1,-3.8],[2.5,2.8,.15],'dark','Forearm '+s,.3);
            for(const f of [-1,1])beam('Pincer blade '+s+f,[s*10+f*1.4,10.6,-3.4],[s*10+f*.7,7.9,-5.5],1.5,2,'pale','Forearm '+s,.07);
        }
        box('Shoulder crossbrace',[0,16.2,0],[11,1.5,2],'steel','Body');
        prism('Ribbed center brace',[0,15.8,-1.8],[4.4,3.3,1],'rose','Body',.13,'face2');
        prism('Segmented spine',[0,11.6,.6],[3.7,7,3.7],'spine','Body',.13,'spine','spine',[0,0,-8]);
        prism('Floating pelvic shell',[0,6.4,1.2],[11.5,9.5,10],'trim','Body',.28,'pod','trim');
        for(const s of [-1,1])beam('Pod spur '+s,[s*4,4.5,2],[s*7.8,1,3.2],1.4,1.4,'pink','Leg '+s,.08);
        beam('Pod keel',[0,2,1.3],[0,-1,1.3],1.4,1.4,'steel','Body',.1);
    } else {
        const four=act===4,shoulder=four?15:10,sy=four?40:29,elbow=four?30:21,wrist=four?20:12;
        if(four){
            prism('Tapered chest shell',[0,36.5,1],[26,17,14],'armor','Body',.24,'chest','armor');
            prism('Small projecting helmet',[0,37,-7],[8.6,11,6.5],'pink','Head',.26,'face4','pink');
            prism('Abdominal core',[0,24,1],[15,13,10],'rose','Body',.2);
            for(let strip=0;strip<12;strip++){
                const a=strip*Math.PI/6;
                for(let row=0;row<4;row++){
                    const spread=1+row*.08,x=Math.cos(a)*11.5*spread,z=Math.sin(a)*8.1*spread+1,y=29-row*5.7,bone='Skirt '+strip+' '+row;
                    pivot(bone,[x,y+1.5,z]);
                    prism('Linked spoon plate '+strip+' '+row,[x,y-1.4,z],[4.4,6,.65],'rose',bone,.32,'disc','discBack',[0,90-strip*30,0]);
                    if(row===3)beam('Hanging plate tip '+strip,[x,y-4.4,z],[x*1.05,y-6,z*1.05],1.8,.65,'pink',bone,.15);
                }
            }
        }else{
            prism('Rounded heavy hood',[0,30,.4],[18,18.5,14],'hood','Head',.27,'face3','hood');
            beam('Swept central crest',[0,31,-6.8],[0,42.5,-3.3],.8,2.8,'navy','Head',.18);
            for(const s of [-1,1]){
                beam('Curved feeler base '+s,[s*5.8,35,-4.5],[s*9,34,-5.4],.65,.65,'pink','Ear '+s);
                beam('Curved feeler tip '+s,[s*9,34,-5.4],[s*10.5,31.8,-6],.6,.6,'pink','Ear '+s);
                prism('Feeler bead '+s,[s*10.5,31.8,-6],[1.6,1.6,1.6],'steel','Ear '+s,.28,'joint');
                prism('Mask bolt '+s,[s*5.4,23.1,-6.55],[1.8,1.8,1.4],'steel','Head',.27,'joint');
            }
            prism('Blue belly plate',[0,18.5,.5],[6.5,11,6.5],'steel','Body',.27,'navy','navy');
            for(const s of [-1,1]){
                beam('Waist piston '+s,[s*6.4,23,.6],[s*5.5,17,.6],1.1,1.1,'steel','Body');
                beam('Lower waist brace '+s,[s*5.5,17,.6],[s*3.8,14,.6],.9,.9,'steel','Body');
                prism('Hip ball '+s,[s*4,14,.6],[3.4,3.4,3.5],'pink','Body',.3);
            }
        }
        for(const s of [-1,1]){
            const arm='Arm '+s,fore='Forearm '+s,hand='Hand '+s;
            prism('Shoulder cap '+s,[s*shoulder,sy,1],four?[10.5,10,11]:[7.6,7.4,7.6],four?'armor':'navy',arm,.29,four?'shoulder':'blueStar');
            beam('Upper arm '+s,[s*shoulder,sy-3,1],[s*(shoulder+2),elbow+.5,0],four?5.6:3.5,four?6:3.7,'arm',arm,.7);
            prism('Elbow pivot '+s,[s*(shoulder+2),elbow,0],four?[4.3,4.3,4.5]:[2.8,2.8,3],'steel',fore,.28,'joint');
            beam('Forearm shell '+s,[s*(shoulder+2),elbow-1,0],[s*(shoulder+2),wrist,-1],four?6.3:4.1,four?6:3.9,'arm',fore,.7);
            if(four){
                prism('Blue armored fist '+s,[s*(shoulder+2),wrist-2,-1.5],[5.2,4.6,4.5],'navy',hand,.18,'glove','blueStar');
                prism('Curled thumb '+s,[s*(shoulder-.5),wrist-2,-2.2],[1.5,2.7,2.2],'navy',hand,.2,'glove','navy',[0,0,s*18]);
            }else{
                prism('Clawed gauntlet '+s,[s*(shoulder+2),wrist-1,-1.1],[4.6,2.5,4.2],'pink',hand,.2,'stars');
                for(let f=-1;f<=1;f++)beam('Finger claw '+s+' '+f,[s*(shoulder+2)+f*1.5,wrist-1.7,-2],[s*(shoulder+2)+f*1.5,wrist-6,-2.7],.6,.6,'steel',hand,.03);
            }
            const hip=four?7:4,hy=four?24:15,knee=four?13:8,leg='Leg '+s,shin='Shin '+s;
            beam('Thigh armor '+s,[s*hip,hy,1],[s*(hip+1),knee+.5,1],four?6:3.6,four?7:4,'pink',leg,.8);
            prism('Knee joint '+s,[s*(hip+1),knee,.5],four?[5.8,4.7,6]:[3.8,3.5,3.8],'steel',shin,.3,'joint');
            beam('Shin armor '+s,[s*(hip+1),knee-.5,.7],[s*(hip+1),3,0],four?6.2:3.9,four?6:3.7,'shin',shin,.9);
            prism('Boot '+s,[s*(hip+1),1.7,-1.8],four?[6.6,3.6,9]:[5,2.5,5.5],four?'navy':'pink',shin,.24,four?'sole':'stars');
            if(!four)for(let f=-1;f<=1;f++)beam('Toe claw '+s+' '+f,[s*(hip+1)+f*1.5,1,-3],[s*(hip+1)+f*1.5,-1,-5.1],.6,.6,'steel',shin,.03);
        }
    }
    return m;
}

function rotate(p,r){let[x,y,z]=p;for(let i=0;i<3;i++){const a=(r[i]||0)*Math.PI/180,c=Math.cos(a),s=Math.sin(a);if(i===0)[y,z]=[y*c-z*s,y*s+z*c];if(i===1)[x,z]=[x*c+z*s,-x*s+z*c];if(i===2)[x,y]=[x*c-y*s,x*s+y*c];}return[x,y,z];}
function bake(model,cubeBaker){
    const elements=new Map(model.elements.map(e=>[e.uuid,e])),groups=new Map(model.groups.map(g=>[g.uuid,g]));
    const walk=items=>items.map(n=>{
        if(typeof n!=='string'){const g=groups.get(n.uuid)||n;return{name:g.name,origin:g.origin||[0,0,0],rotation:g.rotation||[0,0,0],visible:g.visibility!==false&&g.export!==false,children:walk(n.children)};}
        const e=elements.get(n);if(e.type!=='mesh')return cubeBaker({...model,outliner:[n]})[0];
        const quads=[];
        for(const f of Object.values(e.faces)){
            if(f.texture===null||f.vertices.length<3)continue;
            if(f.vertices.length>4)throw Error('Only triangles and quads are supported: '+e.name);
            const vs=f.vertices.map(k=>[...rotate(e.vertices[k],e.rotation||[0,0,0]).map((v,i)=>v+(e.origin?.[i]||0)),f.uv[k][0]/model.resolution.width,f.uv[k][1]/model.resolution.height]);
            const[a,b,c]=vs,u=b.map((v,i)=>v-a[i]),v=c.map((v,i)=>v-a[i]),normal=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]],len=Math.hypot(...normal);
            if(len<1e-8)continue;if(vs.length===3)vs.push(vs[2].slice());quads.push({vertices:vs,normal:normal.map(v=>v/len)});
        }
        return{name:e.name,visible:e.visibility!==false&&e.export!==false,quads};
    });return walk(model.outliner);
}
module.exports={refine,atlas,bake,SIZE};
