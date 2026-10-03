import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.models.zoan.MammothMotion;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.ZipFile;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Exercises the shipped rigs and animation controller without a Minecraft client. */
public final class MammothCheck {
    static int checks;
    static BufferedImage atlas, skin, image;
    static float[] depth;
    static double yaw, pitch, scale, centerY, centerZ;
    static void check(boolean ok,String why) { checks++;if(!ok)throw new AssertionError(why); }
    static float[] rotate(float[] p,float[] r) {
        double x=p[0],y=p[1],z=p[2],a=Math.toRadians(r[0]),b=Math.toRadians(r[1]),c=Math.toRadians(r[2]);
        double q=y*Math.cos(a)-z*Math.sin(a);z=y*Math.sin(a)+z*Math.cos(a);y=q;
        q=x*Math.cos(b)+z*Math.sin(b);z=-x*Math.sin(b)+z*Math.cos(b);x=q;
        return new float[]{(float)(x*Math.cos(c)-y*Math.sin(c)),(float)(x*Math.sin(c)+y*Math.cos(c)),(float)z};
    }
    static float[] transform(float[] point,List<CerberusRig.Node> path,Map<String,CerberusRig.Pose> pose) {
        float[] p=Arrays.copyOf(point,3);
        for(int i=path.size()-1;i>=0;i--) {
            CerberusRig.Node node=path.get(i);CerberusRig.Pose v=pose.get(node.name);float[] r=node.rotation.clone();
            for(int k=0;k<3;k++){p[k]-=node.origin[k];if(v!=null)r[k]+=v.rotation[k];}
            p=rotate(p,r);for(int k=0;k<3;k++)p[k]+=node.origin[k]+(v==null?0:v.position[k]);
        }return p;
    }
    static Map<String,CerberusRig.Pose> copy(Map<String,CerberusRig.Pose> from) {
        Map<String,CerberusRig.Pose> out=new HashMap<>();
        for(String n:from.keySet()){CerberusRig.Pose p=new CerberusRig.Pose();System.arraycopy(from.get(n).rotation,0,p.rotation,0,3);System.arraycopy(from.get(n).position,0,p.position,0,3);out.put(n,p);}return out;
    }
    static void geometry(CerberusRig.Node[] nodes,int[] counts) {
        for(CerberusRig.Node n:nodes) {
            if(n.children!=null){geometry(n.children,counts);continue;}
            counts[0]++;counts[1]+=n.quads.length;
            for(CerberusRig.Quad q:n.quads) {
                float len=0;for(float v:q.normal)len+=v*v;check(Math.abs(len-1)<.001,"Unit normals");
                for(float[] p:q.vertices){for(float v:p)check(Float.isFinite(v),"Finite mesh");check(p[3]>=0&&p[3]<=1&&p[4]>=0&&p[4]<=1,"UV bounds");}
            }
        }
    }
    static void test(CerberusRig rig,boolean hybrid) {
        int[]counts={0,0};geometry(rig.nodes,counts);check(counts[0]==(hybrid?370:601),"Original cube count");check(rig.animations.length==20,"Twenty animation clips");
        for(CerberusRig.Clip clip:rig.animations) {
            check(clip.length>0,"Clip duration");
            for(CerberusRig.Track t:clip.tracks) {
                check(!rig.path(t.bone).isEmpty(),"Known animated joint");
                for(String channel:new String[]{"rotation","position"}) {
                    CerberusRig.Key first=null,last=null;
                    for(CerberusRig.Key key:t.keys)if(key.channel.equals(channel)) {
                        if(first==null)first=key;
                        if(last!=null)check(key.time>last.time,"Sorted samples");
                        last=key;for(float value:key.value)check(Float.isFinite(value),"Finite animation");
                    }
                    if(clip.loop&&first!=null)for(int k=0;k<3;k++)check(Math.abs(first.value[k]-last.value[k])<.001,"Seamless loop: "+clip.name+"/"+t.bone);
                }
            }
        }
        if(!hybrid)for(boolean running:new boolean[]{false,true})for(String leg:new String[]{"hind_left","front_left","hind_right","front_right"}) {
            float phase=leg.equals("hind_left")?0:leg.equals("front_left")?.24F:leg.equals("hind_right")?.5F:.74F;
            float duty=running?.62F:.76F,maxError=0;
            List<CerberusRig.Node> path=rig.path(leg+"_foot");float[] point=path.get(path.size()-1).origin;
            for(int i=0;i<240;i++) {
                float progress=i/240F;Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,running?"run":"walk",progress*(running?1.05F:2.15F),1);
                float[] foot=transform(point,path,p);
                if((progress+phase)%1<duty-.035F)maxError=Math.max(maxError,Math.abs(foot[1]-3));
                check(foot[1]>2.96,"Foot sinks through the ground");
                check(Math.abs(p.get(leg+"_leg").rotation[0]+p.get(leg+"_shin").rotation[0]+p.get(leg+"_foot").rotation[0])<.001,"Flat planted feet");
            }
            check(maxError<.08,"Planted foot drift: "+maxError);
        }
        for(int fps:new int[]{20,60,144}) {
            MammothMotion motion=new MammothMotion(hybrid);MammothMotion.Input in=new MammothMotion.Input();Map<String,CerberusRig.Pose> last=null;
            for(int i=0;i<fps*36;i++) {
                float t=i*20F/fps;in.age=t;in.grounded=!(t>130&&t<145);in.running=t>80&&t<120;
                in.limbAmount=t>40&&t<125?.6F:0;in.limbSwing+=in.limbAmount*20/fps;
                in.charging=t>=170&&t<210;in.charge=(t-170)/40;
                in.stomping=t>=250&&t<290;in.armed=t>=310&&t<335;
                in.vacuum=t>=360&&t<440;in.stampedeCharge=t>=470&&t<530;in.stampedeProgress=(t-470)/60;
                in.stampeding=t>=530&&t<630;if(in.stampeding){in.running=true;in.limbAmount=.8F;}
                if(t>=210&&t<210+20F/fps)motion.signal(MammothMotion.SWEEP,210);
                if(t>=330&&t<330+20F/fps)motion.signal(MammothMotion.TRUNK_SHOT,330);
                Map<String,CerberusRig.Pose> p=motion.sample(rig,in);
                for(CerberusRig.Pose v:p.values())for(int k=0;k<3;k++){check(Float.isFinite(v.rotation[k])&&Math.abs(v.rotation[k])<180,"Bounded motion");check(Float.isFinite(v.position[k])&&Math.abs(v.position[k])<20,"Bounded translation");}
                if(last!=null&&t<165)for(String name:p.keySet())if(last.containsKey(name))for(int k=0;k<3;k++)check(Math.abs(p.get(name).rotation[k]-last.get(name).rotation[k])<35,"Movement transition snapped: "+name+" at "+t+" ticks / "+fps+" fps: "+last.get(name).rotation[k]+" -> "+p.get(name).rotation[k]);
                last=copy(p);
            }
            check(Math.abs(last.get("trunk_base").rotation[0])<5,"Actions release back to idle");
        }
        MammothMotion a=new MammothMotion(hybrid),b=new MammothMotion(hybrid);MammothMotion.Input in=new MammothMotion.Input();in.grounded=true;
        for(int t=0;t<=100;t++){in.age=t;a.sample(rig,in);b.sample(rig,in);}
        a.signal(MammothMotion.SWEEP,100);in.age=102;float attacked=a.sample(rig,in).get("trunk_base").rotation[0],idle=b.sample(rig,in).get("trunk_base").rotation[0];
        check(attacked-idle>35,"Per-player attack state isolation");
        if(hybrid)for(boolean slim:new boolean[]{false,true})for(boolean right:new boolean[]{false,true}) {
            String arm=right?"right_arm":"left_arm";float palmX=(right?-1:1)*(slim?7.425F:8.1F);
            List<CerberusRig.Node> path=rig.path(arm);CerberusRig.Node joint=path.get(path.size()-1);
            float localX=right?(slim?-.5F:-1):(slim?.5F:1);
            float[] skinPalm={joint.origin[0]+localX*1.35F,joint.origin[1]-2-9,0};
            for(String clip:new String[]{"walk","run","attack_left","attack_right","airborne"})for(int i=0;i<=30;i++) {
                Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,clip,rig.clip(clip).length*i/30,1);
                float[] hand=transform(skinPalm,path,p),item=transform(new float[]{palmX,11,0},path,p);
                for(int k=0;k<3;k++)check(Math.abs(hand[k]-item[k])<.0001,"Held item follows the "+(slim?"slim":"classic")+" hand");
            }
        }
        System.out.println((hybrid?"Hybrid":"Full")+": "+counts[0]+" original cubes, "+counts[1]+" quads, 20 clips; geometry, gait, transitions and isolation passed.");
    }
    static float[] project(float[] p) {
        double x=p[0]*Math.cos(yaw)+(p[2]-centerZ)*Math.sin(yaw),z=-p[0]*Math.sin(yaw)+(p[2]-centerZ)*Math.cos(yaw),y=p[1]-centerY;
        return new float[]{(float)(image.getWidth()/2+x*scale),(float)(image.getHeight()/2-(y*Math.cos(pitch)-z*Math.sin(pitch))*scale),(float)(z*Math.cos(pitch)+y*Math.sin(pitch))};
    }
    static double edge(float[]a,float[]b,double x,double y){return(x-a[0])*(b[1]-a[1])-(y-a[1])*(b[0]-a[0]);}
    static void triangle(float[]a,float[]b,float[]c,float[][]uv,BufferedImage texture,double light) {
        double area=edge(a,b,c[0],c[1]);if(Math.abs(area)<1e-8)return;int w=image.getWidth(),h=image.getHeight();
        int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(w-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(h-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
            double wa=edge(b,c,x+.5,y+.5)/area,wb=edge(c,a,x+.5,y+.5)/area,wc=1-wa-wb;
            if(wa<0||wb<0||wc<0)continue;float z=(float)(wa*a[2]+wb*b[2]+wc*c[2]);int index=y*w+x;if(z<depth[index])continue;
            int u=Math.max(0,Math.min(texture.getWidth()-1,(int)((wa*uv[0][0]+wb*uv[1][0]+wc*uv[2][0])*texture.getWidth())));
            int v=Math.max(0,Math.min(texture.getHeight()-1,(int)((wa*uv[0][1]+wb*uv[1][1]+wc*uv[2][1])*texture.getHeight())));
            int color=texture.getRGB(u,v);if((color>>>24)<100)continue;depth[index]=z;
            int r=(int)((color>>16&255)*light),g=(int)((color>>8&255)*light),b1=(int)((color&255)*light);image.setRGB(x,y,0xff000000|r<<16|g<<8|b1);
        }
    }
    static void quad(float[][]points,float[][]uv,BufferedImage tex) {
        float[][]screen=new float[4][];for(int i=0;i<4;i++)screen[i]=project(points[i]);
        float[]a=points[0],b=points[1],c=points[2];double nx=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),ny=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),nz=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
        double light=.65+.35*Math.abs((nx*.4+ny*.8-nz*.45)/Math.sqrt(nx*nx+ny*ny+nz*nz));
        triangle(screen[0],screen[1],screen[2],new float[][]{uv[0],uv[1],uv[2]},tex,light);triangle(screen[0],screen[2],screen[3],new float[][]{uv[0],uv[2],uv[3]},tex,light);
    }
    static void draw(CerberusRig.Node[] nodes,List<CerberusRig.Node> parents,Map<String,CerberusRig.Pose> pose) {
        for(CerberusRig.Node n:nodes) {
            if(!n.visible)continue;if(n.children!=null){parents.add(n);draw(n.children,parents,pose);parents.remove(parents.size()-1);continue;}
            for(CerberusRig.Quad q:n.quads){float[][]points=new float[4][],uv=new float[4][];for(int i=0;i<4;i++){points[i]=transform(q.vertices[i],parents,pose);uv[i]=new float[]{q.vertices[i][3],q.vertices[i][4]};}quad(points,uv,atlas);}
        }
    }
    static void skinBox(CerberusRig rig,Map<String,CerberusRig.Pose> pose,String name,float x,float y,float z,int w,int h,int d,int tx,int ty) {
        List<CerberusRig.Node> path=rig.path(name);CerberusRig.Node joint=path.get(path.size()-1);boolean head=name.equals("head");float X=x+w,Y=y+h,Z=z+d;
        float[][][]points={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};
        int[][]rects={{tx+d,ty+d,tx+d+w,ty+d+h},{tx+d+w+d,ty+d,tx+d+w+d+w,ty+d+h},{tx,ty+d,tx+d,ty+d+h},{tx+d+w,ty+d,tx+d+w+d,ty+d+h},{tx+d+w,ty,tx+d+w+w,ty+d},{tx+d,ty,tx+d+w,ty+d}};
        for(int side=0;side<6;side++) {
            for(int i=0;i<4;i++){float[] p=points[side][i];p[0]=joint.origin[0]+p[0]*(head?1:1.35F);p[1]=joint.origin[1]-p[1]-(name.endsWith("_arm")?2:0);p[2]=joint.origin[2]+p[2]*(head?1:1.15F);points[side][i]=transform(p,path,pose);}
            int[]r=rects[side];float[][]uv={{r[2]/64F,r[1]/64F},{r[0]/64F,r[1]/64F},{r[0]/64F,r[3]/64F},{r[2]/64F,r[3]/64F}};quad(points[side],uv,skin);
        }
    }
    static BufferedImage render(CerberusRig rig,Map<String,CerberusRig.Pose> pose,boolean hybrid,int size,double angle) {
        image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);depth=new float[size*size];Arrays.fill(depth,-Float.MAX_VALUE);
        yaw=Math.toRadians(angle);pitch=Math.toRadians(10);scale=size/(hybrid?70.0:145.0);centerY=hybrid?17:29;centerZ=hybrid?-3:-12;
        draw(rig.nodes,new ArrayList<>(),pose);
        if(hybrid) {
            skinBox(rig,pose,"head",-4,-8,-4,8,8,8,0,0);skinBox(rig,pose,"body",-4,0,-2,8,12,4,16,16);
            skinBox(rig,pose,"right_arm",-3,-2,-2,4,12,4,40,16);skinBox(rig,pose,"left_arm",-1,-2,-2,4,12,4,32,48);
            skinBox(rig,pose,"right_leg",-2,0,-2,4,12,4,0,16);skinBox(rig,pose,"left_leg",-2,0,-2,4,12,4,16,48);
        }
        int occupied=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++)if((image.getRGB(x,y)>>>24)!=0){occupied++;check(x>1&&x<size-2&&y>1&&y<size-2,"Clipped preview");}
        check(occupied>size*size*.07,"Blank preview");return image;
    }
    static void preview(CerberusRig full,CerberusRig hybrid,Path out)throws Exception {
        BufferedImage sheet=new BufferedImage(1800,950,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(24,32,38));g.fillRect(0,0,1800,950);
        String[]names={"stampede_charge","stampede_run","sweep","stomp_finish","walk","run","vacuum","attack_right"};float[]times={.5F,.25F,.23F,.12F,.65F,.28F,.15F,.18F};
        for(int i=0;i<8;i++) {
            boolean h=i>=4;CerberusRig rig=h?hybrid:full;atlas=ImageIO.read(new File("src/main/resources/assets/kazimod/textures/models/zoan/mammoth_"+(h?"hybrid":"full")+".png"));
            Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,names[i],times[i],1);if(names[i].equals("stampede_run"))rig.apply(p,"run",times[i],1);g.drawImage(render(rig,p,h,450,h?204:218),i%4*450,i/4*465,null);
            g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString((h?"Hybrid: ":"Full: ")+names[i].replace('_',' '),i%4*450+24,i/4*465+440);
        }
        g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(new Color(163,177,187));g.drawString("Hybrid shown with the default player skin; in game it uses your own skin.",24,937);g.dispose();ImageIO.write(sheet,"png",out.resolve("mammoth_animations.png").toFile());
        Iterator<ImageWriter>writers=ImageIO.getImageWritersBySuffix("gif");ImageWriter writer=writers.next();
        try(ImageOutputStream stream=ImageIO.createImageOutputStream(out.resolve("mammoth_walk_run.gif").toFile())) {
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            for(int frame=0;frame<36;frame++) {
                BufferedImage contact=new BufferedImage(960,480,BufferedImage.TYPE_INT_RGB);Graphics2D c=contact.createGraphics();c.setColor(new Color(24,32,38));c.fillRect(0,0,960,480);
                for(int n=0;n<2;n++) {
                    boolean h=n==1;CerberusRig rig=h?hybrid:full;atlas=ImageIO.read(new File("src/main/resources/assets/kazimod/textures/models/zoan/mammoth_"+(h?"hybrid":"full")+".png"));
                    Map<String,CerberusRig.Pose>p=rig.pose();rig.apply(p,"walk",frame/36F*2.15F,1);c.drawImage(render(rig,p,h,240,220),n*480,0,null);
                    p=rig.pose();rig.apply(p,"run",frame/36F*2.1F,1);c.drawImage(render(rig,p,h,240,220),n*480+240,0,null);
                    c.setColor(Color.WHITE);c.setFont(new Font("SansSerif",Font.BOLD,16));c.drawString(h?"Hybrid / walk and run":"Full / walk and run",n*480+18,268);
                    p=rig.pose();String attack=h?"trunk_shot":"sweep";rig.apply(p,attack,(frame%18)/18F*rig.clip(attack).length,1);c.drawImage(render(rig,p,h,210,220),n*480+22,272,null);
                    p=rig.pose();rig.apply(p,h?"attack_right":"stomp",frame/36F*(h?.96F:1.2F),1);c.drawImage(render(rig,p,h,210,220),n*480+252,272,null);
                }
                c.dispose();ImageWriteParam param=writer.getDefaultWriteParam();IIOMetadata md=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(contact),param);
                String fmt=md.getNativeMetadataFormatName();IIOMetadataNode root=(IIOMetadataNode)md.getAsTree(fmt),control=new IIOMetadataNode("GraphicControlExtension");
                control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("delayTime","6");control.setAttribute("transparentColorIndex","0");root.appendChild(control);
                if(frame==0){IIOMetadataNode apps=new IIOMetadataNode("ApplicationExtensions"),app=new IIOMetadataNode("ApplicationExtension");app.setAttribute("applicationID","NETSCAPE");app.setAttribute("authenticationCode","2.0");app.setUserObject(new byte[]{1,0,0});apps.appendChild(app);root.appendChild(apps);}
                md.setFromTree(fmt,root);writer.writeToSequence(new IIOImage(contact,null,md),param);
            }writer.endWriteSequence();
        }finally{writer.dispose();}
    }
    public static void main(String[]args)throws Exception {
        CerberusRig full,hybrid;try(Reader r=Files.newBufferedReader(Paths.get("src/main/resources/assets/kazimod/models/zoan/mammoth_full.json"))){full=CerberusRig.read(r);}
        try(Reader r=Files.newBufferedReader(Paths.get("src/main/resources/assets/kazimod/models/zoan/mammoth_hybrid.json"))){hybrid=CerberusRig.read(r);}
        test(full,false);test(hybrid,true);
        try(ZipFile zip=new ZipFile(args[0])){skin=ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/entity/steve.png")));}
        Path out=Paths.get("deliverables/mammoth");Files.createDirectories(out);preview(full,hybrid,out);
        for(boolean vacuum:new boolean[]{false,true}){
            CerberusRig rig=vacuum?hybrid:full;atlas=ImageIO.read(new File("src/main/resources/assets/kazimod/textures/models/zoan/mammoth_"+(vacuum?"hybrid":"full")+".png"));
            Map<String,CerberusRig.Pose> p=rig.pose();rig.apply(p,vacuum?"vacuum":"stampede_run",.3F,1);if(!vacuum)rig.apply(p,"run",.25F,1);
            BufferedImage icon=render(rig,p,vacuum,128,220);Graphics2D g=icon.createGraphics();g.setColor(vacuum?new Color(196,239,241):new Color(236,211,162));g.setStroke(new BasicStroke(2));
            if(vacuum)for(int j=0;j<3;j++)g.drawArc(75+j*6,42+j*4,24-j*5,30-j*5,-70,145);
            else for(int j=0;j<3;j++)g.drawLine(10+j*4,60+j*8,33+j*4,60+j*8);
            g.dispose();ImageIO.write(icon,"png",new File("src/main/resources/assets/kazimod/textures/abilities/"+(vacuum?"ancient_trunk_vacuum":"ancient_stampede")+".png"));
        }
        System.out.println("Mammoth: "+checks+" checks passed; textured still and animated previews exported.");
    }
}
