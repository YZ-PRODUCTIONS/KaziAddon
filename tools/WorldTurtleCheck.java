import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.models.zoan.WorldTurtleMotion;
import net.kazi.kazimod.worldturtle.WorldTurtleMesh;
import net.kazi.kazimod.worldturtle.WorldTurtleAnatomy;
import net.kazi.kazimod.worldturtle.WorldTurtleDeck;
import net.kazi.kazimod.worldturtle.SupernovaMesh;
import net.kazi.kazimod.worldturtle.WorldTurtleSpinMath;
import net.kazi.kazimod.worldturtle.WorldTurtleBlastDamage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Tests and texture-samples the exact baked geometry and animation state used by the game. */
public final class WorldTurtleCheck {
    static int checks, cubes, quads;
    static BufferedImage texture, image;
    static double[] depth;
    static float[] min={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE},max={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
    static double yaw, scale;
    static int flatColor=-1;
    static float flatAlpha=1;
    static boolean additive;
    static boolean filterBackFaces;
    static int submittedQuads;
    static void check(boolean ok,String message) { checks++; if(!ok)throw new AssertionError(message); }
    static float[] rotate(float[] p,float[] r) {
        double x=p[0],y=p[1],z=p[2],a=Math.toRadians(r[0]),b=Math.toRadians(r[1]),c=Math.toRadians(r[2]);
        double yy=y*Math.cos(a)-z*Math.sin(a);z=y*Math.sin(a)+z*Math.cos(a);y=yy;
        double xx=x*Math.cos(b)+z*Math.sin(b);z=-x*Math.sin(b)+z*Math.cos(b);x=xx;
        return new float[]{(float)(x*Math.cos(c)-y*Math.sin(c)),(float)(x*Math.sin(c)+y*Math.cos(c)),(float)z};
    }
    static float[] transform(float[] v,List<CerberusRig.Node> parents,Map<String,CerberusRig.Pose> pose) {
        float[] p=Arrays.copyOf(v,3);
        for(int j=parents.size()-1;j>=0;j--) {
            CerberusRig.Node n=parents.get(j);CerberusRig.Pose s=pose.get(n.name);float[] r=n.rotation.clone();
            for(int k=0;k<3;k++){p[k]-=n.origin[k];if(s!=null)r[k]+=s.rotation[k];}
            p=rotate(p,r);for(int k=0;k<3;k++)p[k]+=n.origin[k]+(s==null?0:s.position[k]);
        }return p;
    }
    static float[] project(float[] p,float[] uv) {
        double x=p[0]*Math.cos(yaw)+p[2]*Math.sin(yaw),z=-p[0]*Math.sin(yaw)+p[2]*Math.cos(yaw),y=p[1]-30;
        return new float[]{(float)(image.getWidth()/2+x*scale),(float)(image.getHeight()/2-(y*.94-z*.342)*scale),(float)(z*.94+y*.342),uv[3],uv[4]};
    }
    static double edge(float[]a,float[]b,double x,double y){return(x-a[0])*(b[1]-a[1])-(y-a[1])*(b[0]-a[0]);}
    static void triangle(float[]a,float[]b,float[]c,double light){
        double area=edge(a,b,c[0],c[1]);if(Math.abs(area)<1e-8)return;
        int w=image.getWidth(),h=image.getHeight();
        int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(w-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(h-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
            double wa=edge(b,c,x+.5,y+.5)/area,wb=edge(c,a,x+.5,y+.5)/area,wc=1-wa-wb;
            if(wa<0||wb<0||wc<0)continue;double z=wa*a[2]+wb*b[2]+wc*c[2];int i=y*w+x;if(z<depth[i])continue;
            int u=Math.max(0,Math.min(255,(int)((wa*a[3]+wb*b[3]+wc*c[3])*256))),v=Math.max(0,Math.min(255,(int)((wa*a[4]+wb*b[4]+wc*c[4])*256)));
            int color=flatColor==-1?texture.getRGB(u,v):0xff000000|flatColor;if((color>>>24)<100)continue;
            if(!additive||flatColor==-1)depth[i]=z;int r=(int)(((color>>16)&255)*light),g=(int)(((color>>8)&255)*light),b1=(int)((color&255)*light);
            if(flatColor!=-1){int old=image.getRGB(x,y);float alpha=flatAlpha,remain=additive?1:1-alpha;
                r=Math.min(255,(int)(r*alpha+(old>>16&255)*remain));g=Math.min(255,(int)(g*alpha+(old>>8&255)*remain));b1=Math.min(255,(int)(b1*alpha+(old&255)*remain));
                int opacity=Math.min(255,(int)(alpha*255+(old>>>24)*(1-alpha)));image.setRGB(x,y,(opacity<<24)|(r<<16)|(g<<8)|b1);
            }else image.setRGB(x,y,0xff000000|(r<<16)|(g<<8)|b1);
        }
    }
    static void draw(CerberusRig.Node[] nodes,List<CerberusRig.Node> parents,Map<String,CerberusRig.Pose> pose,boolean render){
        for(CerberusRig.Node n:nodes){
            if(!n.visible)continue;
            if(n.children!=null){parents.add(n);draw(n.children,parents,pose,render);parents.remove(parents.size()-1);continue;}
            cubes++;for(CerberusRig.Quad q:n.quads){quads++;float[][]world=new float[4][],screen=new float[4][];
                check(Math.abs(Math.sqrt(q.normal[0]*q.normal[0]+q.normal[1]*q.normal[1]+q.normal[2]*q.normal[2])-1)<.001,"Unit normal");
                for(int k=0;k<4;k++){
                    world[k]=transform(q.vertices[k],parents,pose);
                    for(int a=0;a<3;a++){check(Float.isFinite(world[k][a]),"Finite geometry");min[a]=Math.min(min[a],world[k][a]);max[a]=Math.max(max[a],world[k][a]);}
                    check(q.vertices[k][3]>=0&&q.vertices[k][3]<=1&&q.vertices[k][4]>=0&&q.vertices[k][4]<=1,"UV outside texture");
                    if(render)screen[k]=project(world[k],q.vertices[k]);
                }
                if(!render)continue;
                if(filterBackFaces){
                    float[] zero=transform(new float[]{0,0,0},parents,pose),normal=transform(q.normal,parents,pose);
                    double nx=normal[0]-zero[0],ny=normal[1]-zero[1],nz=normal[2]-zero[2];
                    if(-nx*Math.sin(yaw)*.94+ny*.342+nz*Math.cos(yaw)*.94<-.0001)continue;
                }
                submittedQuads++;
                double[]ab={world[1][0]-world[0][0],world[1][1]-world[0][1],world[1][2]-world[0][2]},ac={world[2][0]-world[0][0],world[2][1]-world[0][1],world[2][2]-world[0][2]};
                double nx=ab[1]*ac[2]-ab[2]*ac[1],ny=ab[2]*ac[0]-ab[0]*ac[2],nz=ab[0]*ac[1]-ab[1]*ac[0];
                double light=.65+.35*Math.abs((nx*.4+ny*.8-nz*.45)/Math.sqrt(nx*nx+ny*ny+nz*nz));
                triangle(screen[0],screen[1],screen[2],light);triangle(screen[0],screen[2],screen[3],light);
            }
        }
    }
    static BufferedImage render(CerberusRig rig,Map<String,CerberusRig.Pose> pose,int size,double angle){
        image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);depth=new double[size*size];Arrays.fill(depth,-Double.MAX_VALUE);
        yaw=Math.toRadians(angle);scale=size/220.0;draw(rig.nodes,new ArrayList<>(),pose,true);
        int visible=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((image.getRGB(x,y)>>>24)!=0){visible++;check(x>1&&x<size-2&&y>1&&y<size-2,"Clipped preview");}
        check(visible>size*size*.08,"Blank preview");return image;
    }
    public static void main(String[]args)throws Exception {
        CerberusRig rig;try(Reader r=Files.newBufferedReader(Paths.get("src/main/resources/assets/kazimod/models/zoan/world_turtle.json"))){rig=CerberusRig.read(r);}
        texture=ImageIO.read(new File("src/main/resources/assets/kazimod/textures/models/zoan/world_turtle.png"));
        blastChecks();
        double[] box={-1,-1,-1,1,1,1};
        check(WorldTurtleSpinMath.intersects(box,10,0,0,new double[]{4,-.5,-.5,5,.5,.5}),"Fast spin cannot skip a narrow target");
        check(WorldTurtleSpinMath.intersects(box,-10,0,0,new double[]{-5,-.5,-.5,-4,.5,.5}),"Reverse sweep");
        check(WorldTurtleSpinMath.intersects(box,0,10,0,new double[]{-.5,4,-.5,.5,5,.5}),"Vertical sweep");
        check(WorldTurtleSpinMath.intersects(box,0,0,0,box),"Already overlapping target");
        check(!WorldTurtleSpinMath.intersects(box,10,0,0,new double[]{4,2,-.5,5,3,.5}),"Target outside shell height must miss");
        check(!WorldTurtleSpinMath.intersects(box,10,0,0,new double[]{12,-.5,-.5,13,.5,.5}),"Target beyond travelled path must miss");
        check(!WorldTurtleSpinMath.intersects(box,10,10,0,new double[]{8,0,-.5,9,1,.5}),"Diagonal broad-phase corner must miss");
        double[] attackShell=WorldTurtleAnatomy.fixedBounds(0,0,2.8125F);
        for(int axis=0;axis<3;axis++){attackShell[axis]-=.5;attackShell[axis+3]+=.5;}
        check(WorldTurtleSpinMath.intersects(attackShell,4.05,0,0,new double[]{7.5,0,-.3,8.1,1.8,.3}),"Grounded shell rush reaches standing players");
        for(int fps:new int[]{20,60,144}){
            WorldTurtleMotion spinning=new WorldTurtleMotion();float previous=0;Map<String,CerberusRig.Pose> p=null;
            for(int i=0;i<fps*6;i++){
                float age=i*20F/fps;boolean active=age<60;
                spinning.shieldAt(active,age);spinning.spinAt(active,age);
                p=spinning.sample(rig,age,.9F,true,true,25,30,10,0);
                float angle=p.get("World Turtle").rotation[1];
                float delta=(angle-previous+540)%360-180;previous=angle;
                check(Float.isFinite(angle)&&Math.abs(delta)<=49,"Continuous bounded shell spin");
                if(age>20&&age<60)check(spinning.shield()>.99F,"Spin fully retracts limbs");
            }
            check(spinning.shield()<.001F&&Math.abs(p.get("World Turtle").rotation[1])<.01,"Spin settles and releases after ending");
        }
        for(int yaw=0;yaw<360;yaw+=15)for(int tilt=-15;tilt<=15;tilt+=5){
            CerberusRig.Pose root=new CerberusRig.Pose();root.rotation[2]=tilt;root.rotation[0]=tilt*.5F;
            WorldTurtleDeck deck=new WorldTurtleDeck(root,yaw,2.8125F,123,70,-88);
            for(int u=-8;u<=8;u+=2)for(int v=-8;v<=8;v+=2){
                double[] point=deck.at(u,v),local=deck.local(point[0],point[1],point[2]);
                check(Math.abs(local[0]-u)<1e-6&&Math.abs(local[1]-v)<1e-6,"Rider coordinate round trip");
                check(Math.abs(point[1]-deck.height(point[0],point[2]))<1e-6,"Tilted deck surface height");
                check(deck.contains(point[0],point[2])==(u*u+v*v<=deck.radius*deck.radius),"Deck footprint");
            }
        }
        for(int age=0;age<=120;age++){
            int[] count={0};WorldTurtleMesh.Sink sink=(x,y,z,c,a)->{check(Float.isFinite(x+y+z+a)&&a>=0&&a<=1,"Supernova finite geometry");count[0]++;};
            for(int star=0;star<5;star++){SupernovaMesh.star(sink,age,star);SupernovaMesh.beam(sink,160,age);}
            check(count[0]<16000&&count[0]%4==0,"Bounded five-star mesh");
        }
        check(WorldTurtleAnatomy.BONES.length==8,"Eight turtle-only parts");
        for(int part=0;part<8;part++){
            String name=WorldTurtleAnatomy.BONES[part];check(!name.contains("Elephant")&&!name.contains("World"),"Decorative parts excluded");
            double[] base=WorldTurtleAnatomy.bounds(part,rig.pose(),0,3),twice=WorldTurtleAnatomy.bounds(part,rig.pose(),0,6);
            double[] fixed=WorldTurtleAnatomy.fixedBounds(part,0,3);
            for(int angle=0;angle<360;angle+=5){
                double[] turned=WorldTurtleAnatomy.fixedBounds(part,angle,3);
                for(int axis=0;axis<3;axis++)check(Math.abs((turned[axis+3]-turned[axis])-(fixed[axis+3]-fixed[axis]))<1e-6,"Fixed hitboxes must never stretch");
            }
            for(int k=0;k<6;k++)check(Math.abs(twice[k]-base[k]*2)<1e-6,"Hitbox follows visual scale");
            System.out.println(name+" hit bounds: "+Arrays.toString(base));
            for(CerberusRig.Clip clip:rig.animations)for(int frame=0;frame<=16;frame++){
                Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,clip.name,clip.length*frame/16,1);
                for(int angle=0;angle<360;angle+=45){double[] bounds=WorldTurtleAnatomy.bounds(part,p,angle,3);
                    for(double v:bounds)check(Double.isFinite(v),"Finite animated hitbox");
                    for(int k=0;k<3;k++)check(bounds[k+3]>bounds[k]&&bounds[k+3]-bounds[k]<40,"Bounded hitbox size");
                }
            }
        }
        for(int angle:new int[]{35,125,215,305}){
            filterBackFaces=false;submittedQuads=0;BufferedImage full=render(rig,rig.pose(),256,angle);int fullCount=submittedQuads;
            filterBackFaces=true;submittedQuads=0;BufferedImage culled=render(rig,rig.pose(),256,angle);int differences=0;
            for(int y=0;y<256;y++)for(int x=0;x<256;x++)if(full.getRGB(x,y)!=culled.getRGB(x,y))differences++;
            check(differences<256*256*.005,"Back-face filtering exposed a hole: "+differences);
            check(submittedQuads<fullCount*.65,"Back-face filtering must remove substantial submission work");
            System.out.println("View "+angle+": "+(submittedQuads*4)+" / "+(fullCount*4)+" submitted vertices; "+differences+" differing pixels");
        }
        filterBackFaces=false;cubes=0;quads=0;
        check(rig.animations.length==7,"Expected seven animation clips");
        draw(rig.nodes,new ArrayList<>(),rig.pose(),false);check(cubes==1599,"Original geometry preserved");check(quads*4<25000,"Optimized vertex budget");
        System.out.println("Rest bounds: "+Arrays.toString(min)+" to "+Arrays.toString(max)+"; "+quads*4+" vertices.");
        for(CerberusRig.Clip clip:rig.animations)for(int i=0;i<=64;i++){
            Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,clip.name,i*clip.length/64,1);
            for(CerberusRig.Pose v:p.values())for(float a:v.rotation)check(Float.isFinite(a)&&Math.abs(a)<=35,"Bounded articulation");
            if(i%16==0)draw(rig.nodes,new ArrayList<>(),p,false);
        }
        for(int fps:new int[]{20,60,144}){
            WorldTurtleMotion motion=new WorldTurtleMotion();Map<String,CerberusRig.Pose> last=null;
            for(int i=0;i<fps*12;i++){
                float t=i*20F/fps;Map<String,CerberusRig.Pose> p=motion.sample(rig,t,.6F,t>40&&t<180,t>100,12,15,2,0);
                if(last!=null)for(String bone:p.keySet())if(last.containsKey(bone))for(int a=0;a<3;a++)check(Math.abs(p.get(bone).rotation[a]-last.get(bone).rotation[a])<8,"Animation transition snapped");
                last=new HashMap<>();for(Map.Entry<String,CerberusRig.Pose> e:p.entrySet()){CerberusRig.Pose copy=new CerberusRig.Pose();System.arraycopy(e.getValue().rotation,0,copy.rotation,0,3);last.put(e.getKey(),copy);}
            }
        }
        Path out=Paths.get("deliverables/world_turtle");Files.createDirectories(out);
        BufferedImage sheet=new BufferedImage(1500,1000,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(25,34,40));g.fillRect(0,0,1500,1000);
        String[] clips={"idle","walk","takeoff","fly","fast_fly","land"};float[]time={1,.7F,.7F,.9F,1.8F,.5F};
        for(int i=0;i<6;i++){Map<String,CerberusRig.Pose>p=rig.pose();rig.apply(p,clips[i],time[i],1);BufferedImage frame=render(rig,p,500,i==4?140:215);g.drawImage(frame,i%3*500,i/3*500,null);g.setColor(Color.WHITE);g.drawString(clips[i],i%3*500+20,i/3*500+480);}
        g.dispose();ImageIO.write(sheet,"png",out.resolve("world_turtle_animations.png").toFile());
        ImageIO.write(render(rig,rig.pose(),96,215),"png",new File("src/main/resources/assets/kazimod/textures/abilities/world_turtle_form.png"));
        WorldTurtleMotion fast=new WorldTurtleMotion();Map<String,CerberusRig.Pose> pooled=fast.sample(rig,0,0,false,false,0,0,0,0);
        java.lang.reflect.Method apply=WorldTurtleMotion.class.getDeclaredMethod("apply",String.class,float.class,float.class);apply.setAccessible(true);
        for(CerberusRig.Clip clip:rig.animations)for(int i=0;i<=97;i++){
            for(CerberusRig.Pose p:pooled.values()){Arrays.fill(p.rotation,0);Arrays.fill(p.position,0);}
            float t=clip.length*i/97;Map<String,CerberusRig.Pose> reference=rig.pose();rig.apply(reference,clip.name,t,.73F);apply.invoke(fast,clip.name,t,.73F);
            for(String bone:reference.keySet())for(int k=0;k<3;k++){check(Math.abs(reference.get(bone).rotation[k]-pooled.get(bone).rotation[k])<.0001,"Direct sampler rotation");check(Math.abs(reference.get(bone).position[k]-pooled.get(bone).position[k])<.0001,"Direct sampler position");}
        }
        for(int i=0;i<30;i++)fast.shield(true,1);fast.sample(rig,31,1,false,false,0,0,0,0);
        check(fast.shield()>.99F&&pooled.get("Neck").position[2]>54,"Full shell tuck");
        for(int i=0;i<30;i++)fast.shield(false,1);check(fast.shield()<.01,"Shell releases after cancel");
        combatPreviews(out);
        novaIcon();
        spinIcon(rig);
        System.out.println("World Turtle: "+checks+" geometry, motion, texture and preview checks passed.");
    }
    private static void spinIcon(CerberusRig rig)throws Exception{
        WorldTurtleMotion motion=new WorldTurtleMotion();
        for(int i=0;i<30;i++)motion.shield(true,1);
        Map<String,CerberusRig.Pose> pose=motion.sample(rig,30,0,false,false,0,0,0,0);
        BufferedImage model=render(rig,pose,96,215);
        BufferedImage icon=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(model,0,0,null);
        g.setStroke(new BasicStroke(5));g.setColor(new Color(213,164,52));g.drawArc(5,27,86,44,10,140);g.drawArc(5,27,86,44,190,140);
        g.setStroke(new BasicStroke(2));g.setColor(new Color(255,248,202));g.drawArc(5,25,86,44,10,140);g.drawArc(5,25,86,44,190,140);
        g.fillPolygon(new int[]{7,19,11},new int[]{34,33,44},3);g.fillPolygon(new int[]{89,77,85},new int[]{64,65,54},3);
        g.dispose();ImageIO.write(icon,"png",new File("src/main/resources/assets/kazimod/textures/abilities/world_shaking_spin.png"));
    }
    private static void blastChecks(){
        check(WorldTurtleBlastDamage.damage(0)==80,"Destruction peak damage unchanged");
        check(WorldTurtleBlastDamage.damage(48)==24.5F,"Destruction distance falloff unchanged");
        check(WorldTurtleBlastDamage.damage(96)==6&&WorldTurtleBlastDamage.damage(97)==0,"Blast boundary");
        check(WorldTurtleBlastDamage.radius(30)==48&&WorldTurtleBlastDamage.radius(60)==96,"Expanding wave timing unchanged");
        float previous=80;
        for(int d=0;d<=96;d++){float amount=WorldTurtleBlastDamage.damage(d);check(amount<=previous&&amount>=6,"Monotonic blast falloff");previous=amount;}
        WorldTurtleBlastDamage hits=new WorldTurtleBlastDamage();UUID victim=UUID.randomUUID();
        hits.contact(victim,96,4);check(hits.ready(victim,0,4)==0,"Cannot hit ahead of wave");
        hits.contact(victim,96,60);check(hits.ready(victim,20,60)==0,"Do not consume hit during vanilla immunity");
        check(hits.ready(victim,10,72)==6,"Edge target still receives hit after normal immunity expires");
        hits.resolve(victim);hits.contact(victim,0,64);
        check(hits.ready(victim,0,76)==0,"Successful or deliberately blocked hit cannot repeat");
        UUID delayed=UUID.randomUUID();hits.contact(delayed,20,32);
        float initial=hits.ready(delayed,0,32);hits.contact(delayed,0,36);
        check(hits.ready(delayed,0,36)==initial,"Deferred damage remains tied to first wave contact");
        check(hits.ready(delayed,0,85)==0,"No delayed damage after cleanup window");
        UUID late=UUID.randomUUID();hits.contact(late,0,68);
        check(hits.ready(late,0,68)==0,"Lingering visuals cannot damage new arrivals");
        hits.clear();check(hits.ready(delayed,0,40)==0&&!hits.resolved(victim),"Blast tracker cleans up");
    }
    private static void novaIcon()throws Exception{
        BufferedImage icon=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        for(int i=0;i<5;i++){
            double a=-Math.PI/2+i*Math.PI*2/5;int x=48+(int)(Math.cos(a)*31),y=48+(int)(Math.sin(a)*31);
            g.setColor(new Color(255,158,25,120));g.setStroke(new BasicStroke(7));g.drawLine(x,y,48,48);
            g.setColor(new Color(255,247,204));g.setStroke(new BasicStroke(2));g.drawLine(x,y,48,48);
            g.setColor(new Color(213,107,10));g.fillRect(x-9,y-5,18,10);g.fillRect(x-5,y-9,10,18);
            g.setColor(new Color(255,202,57));g.fillRect(x-7,y-4,14,8);g.fillRect(x-4,y-7,8,14);
            g.setColor(new Color(255,255,230));g.fillRect(x-4,y-4,8,8);
        }
        g.dispose();ImageIO.write(icon,"png",new File("src/main/resources/assets/kazimod/textures/abilities/supernova.png"));
    }
    private static BufferedImage effect(int kind,float age,int size){
        image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);depth=new double[size*size];Arrays.fill(depth,-Double.MAX_VALUE);yaw=Math.toRadians(215);scale=size/(kind==2?60:140.0);
        final float[][]quad=new float[4][];final int[]index={0};
        for(boolean solid:new boolean[]{true,false}){additive=!solid;WorldTurtleMesh.render((x,y,z,c,a)->{
            check(Float.isFinite(x+y+z+a)&&a>=0&&a<=1,"Finite VFX");check(Math.abs(x)<100&&Math.abs(z)<100&&Math.abs(y)<100,"Bounded VFX");
            if(a<=.03F)return;
            quad[index[0]++]=project(new float[]{x,y+(kind==2?30:0),z},new float[]{0,0,0,0,0});
            if(index[0]==4){flatColor=c;flatAlpha=a;triangle(quad[0],quad[1],quad[2],1);triangle(quad[0],quad[2],quad[3],1);index[0]=0;}
        },kind,age,8,solid);}
        flatColor=-1;flatAlpha=1;additive=false;return image;
    }
    private static void combatPreviews(Path out)throws Exception{
        check(WorldTurtleMesh.intersectsShield(-40,0,0,100,0,0),"Fast projectile cannot tunnel through shield");
        check(!WorldTurtleMesh.intersectsShield(-40,20,0,100,0,0),"Projectile above shield must miss");
        check(WorldTurtleMesh.intersectsShield(0,0,0,0,0,0),"Stationary projectile inside shield");
        check(!WorldTurtleMesh.intersectsShield(40,0,0,10,0,0),"Outgoing projectile outside shield");
        check(WorldTurtleMesh.intersectsShield(0,30,0,0,-100,0),"Falling bomb intersects shield");
        for(int kind=0;kind<3;kind++)for(int tick=0;tick<(kind==2?100:220);tick++){
            int[] n={0};for(boolean solid:new boolean[]{true,false})WorldTurtleMesh.render((x,y,z,c,a)->{check(Float.isFinite(x+y+z+a)&&a>=0&&a<=1,"Valid effect vertex");n[0]++;},kind,tick,8,solid);
            check(n[0]%4==0&&n[0]<10000,"VFX budget: "+n[0]);
        }
        BufferedImage sheet=new BufferedImage(1500,500,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(31,37,44));g.fillRect(0,0,1500,500);
        g.drawImage(effect(2,30,500),0,0,null);g.drawImage(effect(0,30,500),500,0,null);g.drawImage(effect(1,80,500),1000,0,null);g.dispose();
        ImageIO.write(sheet,"png",out.resolve("world_turtle_combat.png").toFile());
        BufferedImage shield=effect(2,30,96);Graphics2D icon=shield.createGraphics();icon.drawImage(ImageIO.read(new File("src/main/resources/assets/kazimod/textures/abilities/world_turtle_form.png")),15,15,66,66,null);icon.dispose();
        ImageIO.write(shield,"png",new File("src/main/resources/assets/kazimod/textures/abilities/divine_shield.png"));
        ImageIO.write(effect(1,70,96),"png",new File("src/main/resources/assets/kazimod/textures/abilities/destruction.png"));
    }
}
