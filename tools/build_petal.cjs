const fs=require('fs'),path=require('path'),crypto=require('crypto');
const sharp=require('C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=process.argv[2]||path.resolve(__dirname,'..');
const write=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,d);};
const uuid=s=>{let h=crypto.createHash('md5').update(s).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;};
async function model(name,wing){
 let colors=['#e6ecf4','#a5b5cd','#292431','#ec88b1','#f6d894','#ffffff','#d0edff','#b85c8b'];
 let raw=Buffer.alloc(32*32*4);for(let y=0;y<32;y++)for(let x=0;x<32;x++){let c=parseInt(colors[Math.floor(y/4)].slice(1),16),o=(y*32+x)*4,shade=x%8===0?.88:1;raw[o]=(c>>16)*shade;raw[o+1]=(c>>8&255)*shade;raw[o+2]=(c&255)*shade;raw[o+3]=255;}
 let png=await sharp(raw,{raw:{width:32,height:32,channels:4}}).png().toBuffer();let elements=[],nodes=[],serial=0;
 function cube(n,a,b,row,rotation=[0,0,0],origin=[0,0,0]){
  let id=uuid(name+n+serial++),uv=[1,row*4+1,7,row*4+3],faces={};for(let face of ['north','south','east','west','up','down'])faces[face]={uv,texture:0};
  elements.push({name:n,uuid:id,type:'cube',from:a,to:b,origin,rotation,faces,box_uv:false});
  let [x,y,z]=a,[X,Y,Z]=b,quads=[];let f=[[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z]],[[X,y,Z],[x,y,Z],[x,Y,Z],[X,Y,Z]],[[X,y,z],[X,y,Z],[X,Y,Z],[X,Y,z]],[[x,y,Z],[x,y,z],[x,Y,z],[x,Y,Z]],[[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]],[[x,y,Z],[X,y,Z],[X,y,z],[x,y,z]]];
  f.forEach((verts,i)=>quads.push({normal:[[0,0,-1],[0,0,1],[1,0,0],[-1,0,0],[0,1,0],[0,-1,0]][i],vertices:verts.map((v,j)=>[...v,uv[j===1||j===2?2:0]/32,uv[j>=2?3:1]/32])}));
  nodes.push(rotation.some(v=>v!==0)?{name:n,origin,rotation,visible:true,children:[{name:n+' mesh',visible:true,quads}]}:{name:n,origin,rotation,visible:true,quads});
 }
 if(!wing){cube('Wrapped hilt',[-.7,-7,-.65],[.7,0,.65],2);for(let i=0;i<5;i++)cube('Pink binding '+i,[-.75,-6.8+i*1.3,-.7],[.75,-6.2+i*1.3,.7],3);cube('Pommel',[-.85,-7.4,-.8],[.85,-6.8,.8],4);
  cube('Guard left',[-2.8,-.3,-.5],[-.5,.3,.5],4);cube('Guard right',[.5,-.3,-.5],[2.8,.3,.5],4);cube('Guard front',[-.5,-.3,-2.8],[.5,.3,2.8],4);cube('Habaki',[-.8,0,-.55],[.8,1.5,.55],4);
  cube('Blade',[-.65,1.5,-.24],[.65,25,.24],0);cube('Spine',[-.65,1.5,-.27],[-.35,25,.27],1);cube('Cutting edge',[.65,1.5,-.13],[.85,24.5,.13],5);
  cube('Blade tip base',[-.65,25,-.24],[.35,25.6,.24],0);cube('Blade tip taper',[-.65,25.6,-.2],[-.05,26.3,.2],0);cube('Blade point',[-.6,26.3,-.13],[-.3,27,.13],5);
 }else for(let side of [-1,1])for(let i=0;i<12;i++){
  let x=side*3,y=18,length=19+Math.sin(i/11*Math.PI)*11,rot=[0,side*-8,side*(-20-i*5)];
  cube('White wing '+side+' '+i,[x-.65,y,2],[x+.65,y+length*.8,3],i%3===0?6:5,rot,[x,y,3]);
  cube('Feather tip '+side+' '+i,[x-.3,y+length*.8,2.2],[x+.3,y+length,2.8],5,rot,[x,y,3]);
 }
 let m={meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'Senbonzakura '+name,resolution:{width:32,height:32},elements,outliner:elements.map(e=>e.uuid),textures:[{uuid:uuid(name+'texture'),name:name+'.png',id:'0',width:32,height:32,source:'data:image/png;base64,'+png.toString('base64'),internal:true}],animations:[]};
 write('deliverables/petal/'+name+'.bbmodel',JSON.stringify(m));write('src/main/resources/assets/kazimod/models/entities/petal_'+name+'.json',JSON.stringify({nodes,animations:[],sourceHash:crypto.createHash('sha256').update(JSON.stringify(m)).digest('hex')}));write('src/main/resources/assets/kazimod/textures/entities/petal_'+name+'.png',png);
}
async function main(){await model('sword',false);await model('hakuteiken_wings',true);
 let shapes=[
 '<path d="M30 57L30 34L34 34L34 57ZM31 31L32 6L34 30Z"/><path d="M8 14Q25 11 18 25Q7 28 8 14ZM44 12Q59 13 52 26Q39 24 44 12ZM9 34Q25 31 18 45Q7 48 9 34Z"/>',
 '<path d="M5 49Q45 43 48 10L59 5L55 24Q38 57 5 55Z"/><path d="M9 34Q34 30 38 7M17 59Q50 52 59 35" fill="none"/>',
 '<path d="M11 12L32 5L53 12L49 39L32 58L15 39Z" fill="none" stroke-width="5"/><path d="M19 23Q35 13 43 25Q35 42 23 43Z"/>',
 '<path d="M5 29L36 22L31 8L59 29L31 55L36 38L5 45Z"/>',
 '<ellipse cx="32" cy="32" rx="23" ry="26" fill="none" stroke-width="4"/><ellipse cx="32" cy="32" rx="12" ry="26" fill="none"/><path d="M8 30L56 30M12 45L52 45M12 18L52 18" fill="none"/>',
 '<path d="M25 4L39 4L39 37L52 31L32 56L12 31L25 37Z"/><path d="M5 58Q32 45 59 58" fill="none"/>',
 '<path d="M8 55L8 15L12 5L16 15L16 55ZM26 55L26 10L32 1L38 10L38 55ZM48 55L48 15L52 5L56 15L56 55Z"/>',
 '<ellipse cx="32" cy="40" rx="26" ry="17" fill="none"/><path d="M9 40L9 10M21 29L21 4M43 29L43 4M55 40L55 10" fill="none" stroke-width="5"/>',
 '<path d="M30 58L30 21L32 2L34 21L34 58ZM27 33Q8 8 3 17L12 43L27 46ZM37 33Q56 8 61 17L52 43L37 46Z"/>',
 '<circle cx="32" cy="32" r="24" fill="none" stroke-width="5"/><path d="M14 14L29 25L22 24L23 32M50 14L36 26L42 24L40 32M17 51L30 37L24 39L32 40M48 50L36 38L42 39L40 32" fill="none" stroke-width="4"/>'
 ];for(let i=0;i<10;i++){let svg=`<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect x="1" y="1" width="62" height="62" rx="5" fill="#201a2a" stroke="#e9a7ca" stroke-width="2"/><g fill="${i===8?'#f3f7ff':'#f4b1d5'}" stroke="#fae1ef" stroke-width="1.5" stroke-linejoin="round">${shapes[i]}</g></svg>`;write('src/main/resources/assets/kazimod/textures/abilities/petal/'+i+'.png',await sharp(Buffer.from(svg)).png().toBuffer());}
 await require('./build_hanabira_item.cjs').build(root);write('src/main/resources/assets/kazimod/models/item/petal_petal_no_mi.json',JSON.stringify({parent:'item/generated',textures:{layer0:'kazimod:items/petal_petal_no_mi'}}));
 let langPath=path.join(root,'src/main/resources/assets/kazimod/lang/en_us.json'),lang=JSON.parse(fs.readFileSync(langPath));lang['item.kazimod.petal_petal_no_mi']='Hanabira Hanabira no Mi';let names=['Scatter','Petal Stream','Blossom Guard','Petal Step','Blooming Prison','Blossom Burial','Senbonzakura Kageyoshi','Senkei','Shukei: Hakuteiken'];for(let n of names)lang['ability.kazimod.petal_'+n.toLowerCase().replace(/[^a-z0-9 ]/g,'').replace(/ /g,'_')]=n;write('src/main/resources/assets/kazimod/lang/en_us.json',JSON.stringify(lang,null,2)+'\n');
 let lootPath=path.join(root,'src/main/resources/data/kazimod/loot_tables/dfboxes/golden_box.json'),loot=JSON.parse(fs.readFileSync(lootPath));let entries=loot.pools[0].entries;if(!entries.some(e=>e.name==='kazimod:petal_petal_no_mi'))entries.push({type:'minecraft:item',weight:100,functions:[{function:'mineminenomi:fruit_already_exists'}],name:'kazimod:petal_petal_no_mi'});write('src/main/resources/data/kazimod/loot_tables/dfboxes/golden_box.json',JSON.stringify(loot,null,2)+'\n');
}
main().catch(e=>{console.error(e);process.exit(1)});
