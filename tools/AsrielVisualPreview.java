import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ChaosSaberVisuals;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;

/** Renders the game's exact native meshes for visual QA and inventory icons. */
public final class AsrielVisualPreview {
    private static final class Vertex {
        final double x,y,z; final float r,g,b,a;
        Vertex(double x,double y,double z,float r,float g,float b,float a){this.x=x;this.y=y;this.z=z;this.r=r;this.g=g;this.b=b;this.a=a;}
    }
    private interface Scene { void draw(AsrielVisualGeometry.Sink sink); }
    private static final class Mesh implements AsrielVisualGeometry.Sink {
        final List<Vertex[]> faces=new ArrayList<>(); final Vertex[] next=new Vertex[4];int index;
        public void vertex(double x,double y,double z,float r,float g,float b,float a){
            if(!Double.isFinite(x+y+z)||!Float.isFinite(r+g+b+a))throw new AssertionError("Nonfinite mesh vertex");
            if(a<0||a>1.001)throw new AssertionError("Invalid alpha");
            next[index++]=new Vertex(x,y,z,r,g,b,a);
            if(index==4){faces.add(next.clone());index=0;}
        }
    }
    private static double dot(double[] p,double[] q){return p[0]*q[0]+p[1]*q[1]+p[2]*q[2];}
    private static double[] cross(double[] p,double[] q){return new double[]{p[1]*q[2]-p[2]*q[1],p[2]*q[0]-p[0]*q[2],p[0]*q[1]-p[1]*q[0]};}
    private static double[] norm(double[] p){double n=Math.sqrt(dot(p,p));return new double[]{p[0]/n,p[1]/n,p[2]/n};}
    private static BufferedImage render(Scene scene,int width,int height,double[] camera,boolean transparent) {
        return render(scene,width,height,camera,transparent,null);
    }
    private static BufferedImage render(Scene scene,int width,int height,double[] camera,boolean transparent,double[] fixedBounds) {
        Mesh mesh=new Mesh();scene.draw(mesh);if(mesh.index!=0)throw new AssertionError("Incomplete quad");
        double[] forward=norm(camera),right=norm(cross(new double[]{0,1,0},forward)),up=norm(cross(forward,right));
        double minx=1e9,miny=1e9,maxx=-1e9,maxy=-1e9;
        for(Vertex[] face:mesh.faces)for(Vertex v:face){double[] p={v.x,v.y,v.z};double x=dot(p,right),y=dot(p,up);minx=Math.min(minx,x);maxx=Math.max(maxx,x);miny=Math.min(miny,y);maxy=Math.max(maxy,y);}
        if(fixedBounds!=null){minx=fixedBounds[0];miny=fixedBounds[1];maxx=fixedBounds[2];maxy=fixedBounds[3];}
        double scale=Math.min(width/Math.max(.1,maxx-minx),height/Math.max(.1,maxy-miny))*.84;
        double cx=(minx+maxx)/2,cy=(miny+maxy)/2;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        if(!transparent){Graphics2D g=image.createGraphics();g.setColor(new Color(12,15,27));g.fillRect(0,0,width,height);g.dispose();}
        mesh.faces.sort(Comparator.comparingDouble(f->{double z=0;for(Vertex v:f)z+=dot(new double[]{v.x,v.y,v.z},forward);return z;}));
        for(Vertex[] f:mesh.faces){
            double[][] ps=new double[4][2];for(int i=0;i<4;i++){double[] p={f[i].x,f[i].y,f[i].z};ps[i][0]=(dot(p,right)-cx)*scale+width/2.;ps[i][1]=height/2.-(dot(p,up)-cy)*scale;}
            triangle(image,ps[0],ps[1],ps[2],f[0],f[1],f[2],transparent);
            triangle(image,ps[0],ps[2],ps[3],f[0],f[2],f[3],transparent);
        }
        return image;
    }
    private static void triangle(BufferedImage image,double[] a,double[] b,double[] c,Vertex ca,Vertex cb,Vertex cc,boolean transparent){
        double den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-9)return;
        int minx=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),maxx=Math.min(image.getWidth()-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int miny=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),maxy=Math.min(image.getHeight()-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=miny;y<=maxy;y++)for(int x=minx;x<=maxx;x++){
            double u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den;
            double v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den;if(u<0||v<0||u+v>1)continue;
            double w=1-u-v;int old=image.getRGB(x,y);float alpha=(float)(ca.a*u+cb.a*v+cc.a*w);
            int r=Math.min(255,((old>>16)&255)+(int)((ca.r*u+cb.r*v+cc.r*w)*alpha*255));
            int g=Math.min(255,((old>>8)&255)+(int)((ca.g*u+cb.g*v+cc.g*w)*alpha*255));
            int b0=Math.min(255,(old&255)+(int)((ca.b*u+cb.b*v+cc.b*w)*alpha*255));
            int outA=transparent?Math.min(255,(old>>>24)+(int)(alpha*255)):255;
            image.setRGB(x,y,(outA<<24)|(r<<16)|(g<<8)|b0);
        }
    }
    private static void write(BufferedImage image,String path)throws Exception{File f=new File(path);f.getParentFile().mkdirs();ImageIO.write(image,"png",f);}
    private static final double[] THREE={.8,.45,1}, SIDE={1,.3,1}, FLAT={.1,1,.01};
    private static void stars(String root)throws Exception{
        String[] labels={"Star Blazing: 12.8-block diameter","Final star: 25.6-block diameter",
                "Impact: expanding ghost star","Final impact: outward white star rings"};
        Scene[] scenes={s->AsrielVisualGeometry.star(s,6.4F,16,false,false,false,0),
                s->AsrielVisualGeometry.star(s,12.8F,24,false,false,true,0),
                s->AsrielVisualGeometry.starBurst(s,6.4F,4,false,false,0),
                s->AsrielVisualGeometry.starBurst(s,12.8F,15,false,true,0)};
        BufferedImage sheet=new BufferedImage(1280,1000,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(12,15,27));g.fillRect(0,0,1280,1000);
        g.setFont(new Font("SansSerif",Font.BOLD,21));
        for(int i=0;i<4;i++){
            int x=(i%2)*640,y=(i/2)*500;
            g.drawImage(render(scenes[i],640,450,new double[]{.12,.06,1},false),x,y+38,null);
            g.setColor(new Color(226,232,246));g.drawString(labels[i],x+20,y+29);
        }
        g.dispose();write(sheet,root+"/deliverables/asriel_vfx/star_blazing_rework.png");
        int maxQuads=0,samples=0;
        for(int detail=0;detail<3;detail++)for(int age=0;age<70;age++)for(boolean enhanced:new boolean[]{false,true})for(boolean finisher:new boolean[]{false,true}){
            float radius=finisher?(enhanced?16:12.8F):(enhanced?8:6.4F);
            Mesh fall=new Mesh(),burst=new Mesh(),fragment=new Mesh();
            AsrielVisualGeometry.star(fall,radius,age,enhanced,false,finisher,detail);
            AsrielVisualGeometry.starBurst(burst,radius,age,enhanced,finisher,detail);
            AsrielVisualGeometry.star(fragment,enhanced?.8F:.65F,age,enhanced,true,false,detail);
            for(Mesh mesh:new Mesh[]{fall,burst,fragment}){
                if(mesh.index!=0)throw new AssertionError("Incomplete star quad");
                maxQuads=Math.max(maxQuads,mesh.faces.size());
            }
            if(age>=(finisher?28:16)&&!burst.faces.isEmpty())throw new AssertionError("Expired burst still emits geometry");
            samples++;
        }
        if(maxQuads>500)throw new AssertionError("Unbounded star VFX: "+maxQuads);
        System.out.println("Star rework validated "+samples+" time/detail/variant samples; maximum "+maxQuads+" quads. Dedicated preview exported; icons and other assets untouched.");
    }
    private static void buster(String root)throws Exception{
        double[] three=ChaosBusterPattern.group(0).warningAnglesRadians();
        double[] four=ChaosBusterPattern.group(1).warningAnglesRadians();
        String[] labels={"White skeletal cannon / hollow muzzle","White diamond burst: three fanning lanes",
                "Thin prefire guides: three or four lanes","Rainbow filling the cannon before release",
                "Rainbow beam: torn plasma and pressure bursts","Beam tail-off: shrinking and fading"};
        Scene diamonds=s->{for(int row=0;row<5;row++)for(int lane=0;lane<3;lane++){
            final double ox=(lane-1)*(.60+row*.3),oy=1.8-row*.9;
            AsrielVisualGeometry.Sink offset=(x,y,z,r,g,b,a)->s.vertex(x+ox,y+oy,z,r,g,b,a);
            AsrielVisualGeometry.busterBullet(offset,6+row*2,0);
        }};
        Scene guides=s->{
            AsrielVisualGeometry.busterCannon(s,4,0,false,0);
            AsrielVisualGeometry.busterTelegraph(s,4,.8F,16,four,0);
        };
        Scene[] scenes={s->AsrielVisualGeometry.busterCannon(s,0,0,false,0),diamonds,guides,
                s->AsrielVisualGeometry.busterCharge(s,155,ChaosBusterPattern.chargeFraction(155,false),
                        (float)ChaosBusterPattern.BEAM_RADIUS,false,0),
                s->AsrielVisualGeometry.rainbowBeam(s,4,ChaosBusterPattern.BEAM_LIFETIME,48,busterRadius(4),false,0),
                s->AsrielVisualGeometry.rainbowBeam(s,ChaosBusterPattern.BEAM_LIFETIME-3,
                        ChaosBusterPattern.BEAM_LIFETIME,48,busterRadius(ChaosBusterPattern.BEAM_LIFETIME-3),false,0)};
        double[][] cameras={new double[]{1,.35,1},new double[]{0,0,1},new double[]{.2,1,.7},
                new double[]{1,.35,1},new double[]{1,.18,.55},new double[]{1,.18,.55}};
        BufferedImage sheet=new BufferedImage(1440,1440,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(12,15,27));g.fillRect(0,0,1440,1440);
        g.setFont(new Font("SansSerif",Font.BOLD,21));
        for(int i=0;i<scenes.length;i++){
            int x=(i%2)*720,y=(i/2)*480;
            g.drawImage(render(scenes[i],720,430,cameras[i],false),x,y+38,null);
            g.setColor(new Color(226,232,246));g.drawString(labels[i],x+20,y+29);
        }
        g.dispose();write(sheet,root+"/deliverables/asriel_vfx/chaos_buster_rework.png");
        int maxQuads=0,samples=0;
        for(int detail=0;detail<3;detail++)for(int age=0;age<220;age++)for(boolean enhanced:new boolean[]{false,true}){
            Mesh bullet=new Mesh(),cannon=new Mesh(),telegraph=new Mesh(),charge=new Mesh(),beam=new Mesh();
            AsrielVisualGeometry.busterBullet(bullet,age,detail);
            AsrielVisualGeometry.busterCannon(cannon,age,0,enhanced,detail);
            AsrielVisualGeometry.busterTelegraph(telegraph,age,(age%12)/12F,48,age%2==0?three:four,detail);
            double spin=Math.toRadians(enhanced?0:ChaosBusterPattern.cannonSpinDegrees(age));
            AsrielVisualGeometry.Sink rotating=(x,y,z,r,gc,b,a)->charge.vertex(x*Math.cos(spin)-y*Math.sin(spin),
                    x*Math.sin(spin)+y*Math.cos(spin),z,r,gc,b,a);
            AsrielVisualGeometry.busterCharge(rotating,age,ChaosBusterPattern.chargeFraction(age,enhanced),
                    (float)ChaosBusterPattern.BEAM_RADIUS,enhanced,detail);
            AsrielVisualGeometry.rainbowBeam(beam,age,ChaosBusterPattern.BEAM_LIFETIME,48,busterRadius(age),enhanced,detail);
            for(Mesh mesh:new Mesh[]{bullet,cannon,telegraph,charge,beam}){
                if(mesh.index!=0)throw new AssertionError("Incomplete Buster quad");
                int budget=mesh==beam?1600:500;
                if(mesh.faces.size()>budget)throw new AssertionError("Unbounded Buster VFX: "+mesh.faces.size());
                maxQuads=Math.max(maxQuads,mesh.faces.size());
            }
            if(age>=ChaosBusterPattern.BEAM_LIFETIME&&!beam.faces.isEmpty())throw new AssertionError("Expired beam still emits geometry");
            samples++;
        }
        System.out.println("Chaos Buster validated "+samples+" time/detail/variant samples; maximum "+maxQuads+" quads. Dedicated preview exported; icons and other assets untouched.");
    }
    private static float busterRadius(float age) {
        return (float)(ChaosBusterPattern.BEAM_RADIUS * ChaosBusterPattern.beamRadiusScale(
                age,ChaosBusterPattern.BEAM_LIFETIME));
    }
    private static void saber(String root)throws Exception{
        String[] labels={"Half the previous size / same framing","Blades drop level before the slice",
                "Faster horizontal slice / shorter trail","Faster paired horizontal crossing sweep",
                "Fading blades / four-point twinkles","Lingering white fragments across the field"};
        float[] ages={34,46,49.6F,151.6F,154,175};
        BufferedImage sheet=new BufferedImage(1440,1440,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(12,15,27));g.fillRect(0,0,1440,1440);
        g.setFont(new Font("SansSerif",Font.BOLD,21));
        for(int i=0;i<ages.length;i++){
            final float age=ages[i];int x=(i%2)*720,y=(i/2)*480;
            double[] camera={.25,.70,1};
            double[] priorBounds=projectedBounds(s->ChaosSaberVisuals.render(s,age,22,5,0),camera);
            g.drawImage(render(s->AsrielVisualGeometry.sabers(s,age,(float)ChaosSaberPattern.LENGTH,
                    (float)ChaosSaberPattern.SPARK_RADIUS,0),720,430,camera,false,priorBounds),x,y+38,null);
            g.setColor(new Color(226,232,246));g.drawString(labels[i],x+20,y+29);
        }
        g.dispose();write(sheet,root+"/deliverables/asriel_vfx/chaos_saber_rework.png");
        int maxQuads=0,lowQuads=0,samples=0,horizontalTrails=0;
        for(int detail=0;detail<3;detail++)for(int halfTick=0;halfTick<(ChaosSaberPattern.DURATION+5)*2;halfTick++){
            float age=halfTick*.5F;Mesh mesh=new Mesh();
            AsrielVisualGeometry.sabers(mesh,age,(float)ChaosSaberPattern.LENGTH,(float)ChaosSaberPattern.SPARK_RADIUS,detail);
            if(mesh.index!=0)throw new AssertionError("Incomplete Saber quad");
            if(age>=ChaosSaberPattern.DURATION&&!mesh.faces.isEmpty())throw new AssertionError("Expired saber still emits geometry");
            maxQuads=Math.max(maxQuads,mesh.faces.size());samples++;
            if(detail==2)lowQuads=Math.max(lowQuads,mesh.faces.size());
            for(Vertex[] face:mesh.faces){
                float minAlpha=1,maxAlpha=0;double minY=1e9,maxY=-1e9;
                for(Vertex v:face){minAlpha=Math.min(minAlpha,v.a);maxAlpha=Math.max(maxAlpha,v.a);minY=Math.min(minY,v.y);maxY=Math.max(maxY,v.y);}
                // Only the feathered history ribbon uses varying vertex alpha; its emitted surface must stay level.
                if(maxAlpha-minAlpha>.0001){
                    if(maxY-minY>.00001)throw new AssertionError("Horizontal slice left a vertical history ribbon at age "+age);
                    if(Math.abs(minY-.5)>.00001)throw new AssertionError("Scaled horizontal ribbon has the wrong hand height at age "+age);
                    horizontalTrails++;
                }
            }
        }
        // A second cosmetic halving makes the unchanged 44/10 gameplay call equal direct 11/2.5 geometry.
        for(float age:ages){
            Mesh actual=new Mesh(),small=new Mesh(),large=new Mesh();
            AsrielVisualGeometry.sabers(actual,age,44,10,0);
            ChaosSaberVisuals.render(small,age,11,2.5F,0);ChaosSaberVisuals.render(large,age,22,5,0);
            if(actual.faces.size()!=small.faces.size())throw new AssertionError("Gameplay visual quarter-scale wrapper differs");
            if(small.faces.size()!=large.faces.size())throw new AssertionError("Scale changed the mesh topology");
            for(int face=0;face<small.faces.size();face++)for(int v=0;v<4;v++){
                Vertex a=small.faces.get(face)[v],b=large.faces.get(face)[v],call=actual.faces.get(face)[v];
                if(Math.abs(a.x*2-b.x)+Math.abs(a.y*2-b.y)+Math.abs(a.z*2-b.z)>1e-7)
                    throw new AssertionError("Saber geometry did not double uniformly");
                if(Math.abs(a.x-call.x)+Math.abs(a.y-call.y)+Math.abs(a.z-call.z)>1e-7)
                    throw new AssertionError("Actual saber visual was not halved again");
            }
        }
        if(maxQuads>600 || lowQuads>310)throw new AssertionError("Unbounded Saber VFX: "+maxQuads+" / low "+lowQuads);
        if(horizontalTrails==0)throw new AssertionError("Horizontal history ribbons were never emitted");
        saberAnimation(root);
        System.out.println("Chaos Saber validated "+samples+" time/detail samples, "+horizontalTrails+" level history quads and actual quarter-size wrapper; maximum "+maxQuads+" quads (low "+lowQuads+"). Static and fixed-frame animated previews retain the previous size framing; icons and other assets untouched.");
    }
    private static double[] projectedBounds(Scene scene,double[] camera){
        double[] forward=norm(camera),right=norm(cross(new double[]{0,1,0},forward)),up=norm(cross(forward,right));
        double[] bounds={1e9,1e9,-1e9,-1e9};Mesh mesh=new Mesh();scene.draw(mesh);
        for(Vertex[] face:mesh.faces)for(Vertex v:face){
            double[] p={v.x,v.y,v.z};double x=dot(p,right),y=dot(p,up);
            bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.min(bounds[1],y);
            bounds[2]=Math.max(bounds[2],x);bounds[3]=Math.max(bounds[3],y);
        }
        return bounds;
    }
    private static void saberAnimation(String root)throws Exception{
        List<Float> ages=new ArrayList<>();List<String> labels=new ArrayList<>();
        for(int i=0;i<45;i++){ages.add(33+i*2F/3F);labels.add("Faster horizontal slice - half previous size");}
        for(int i=0;i<39;i++){ages.add(141+i*2F/3F);labels.add("Faster paired sweep - same camera framing");}
        for(int i=0;i<27;i++){ages.add(167+i*2F/3F);labels.add("Falling white fragments");}
        double[] camera={.30,.75,1},forward=norm(camera),right=norm(cross(new double[]{0,1,0},forward)),up=norm(cross(forward,right));
        double[] bounds={1e9,1e9,-1e9,-1e9};
        for(float age:ages){
            // Preserve the prior 22/5 framing so the new size reduction cannot disappear through auto-zoom.
            Mesh mesh=new Mesh();ChaosSaberVisuals.render(mesh,age,22,5,0);
            for(Vertex[] face:mesh.faces)for(Vertex v:face){
                double[] p={v.x,v.y,v.z};double x=dot(p,right),y=dot(p,up);
                bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.min(bounds[1],y);
                bounds[2]=Math.max(bounds[2],x);bounds[3]=Math.max(bounds[3],y);
            }
        }
        File file=new File(root+"/deliverables/asriel_vfx/chaos_saber_motion.gif");file.getParentFile().mkdirs();
        ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();ImageWriteParam param=writer.getDefaultWriteParam();
        try(FileImageOutputStream output=new FileImageOutputStream(file)){
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int i=0;i<ages.size();i++){
                final float age=ages.get(i);BufferedImage rendered=render(s->AsrielVisualGeometry.sabers(s,age,44,10,0),
                        640,520,camera,false,bounds);
                BufferedImage frame=new BufferedImage(640,560,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=frame.createGraphics();
                graphics.setColor(new Color(12,15,27));graphics.fillRect(0,0,640,560);graphics.drawImage(rendered,0,40,null);
                graphics.setColor(new Color(226,232,246));graphics.setFont(new Font("SansSerif",Font.BOLD,18));
                graphics.drawString(labels.get(i),18,27);graphics.dispose();
                IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame),param);
                String format=metadata.getNativeMetadataFormatName();IIOMetadataNode tree=(IIOMetadataNode)metadata.getAsTree(format);
                IIOMetadataNode control=new IIOMetadataNode("GraphicControlExtension");control.setAttribute("disposalMethod","none");
                control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");
                control.setAttribute("delayTime",Integer.toString(i%3==2?4:3));control.setAttribute("transparentColorIndex","0");tree.appendChild(control);
                if(i==0){IIOMetadataNode extensions=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");
                    loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);tree.appendChild(extensions);}
                metadata.setFromTree(format,tree);writer.writeToSequence(new IIOImage(frame,null,metadata),param);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
    }
    public static void main(String[] args)throws Exception{
        String root=args.length==0?".":args[0];
        if(args.length>1 && "--stars".equals(args[1])){stars(root);return;}
        if(args.length>1 && "--buster".equals(args[1])){buster(root);return;}
        if(args.length>1 && "--saber".equals(args[1])){saber(root);return;}
        String[] names={"Star Blazing","Shocker Breaker warning","Shocker Breaker strike","Chaos Bullets","Chaos Saber","Chaos Blaster"};
        Scene[] scenes={s->AsrielVisualGeometry.star(s,1.6F,8,false,false,0),s->AsrielVisualGeometry.lightning(s,28,3,false,0),
                s->AsrielVisualGeometry.lightning(s,43,3,false,0),s->AsrielVisualGeometry.comet(s,8,4,false,0),
                s->AsrielVisualGeometry.sabers(s,9,22,5,0),s->AsrielVisualGeometry.rainbowBeam(s,7,ChaosBusterPattern.BEAM_LIFETIME,48,3.2F,true,0)};
        double[][] cameras={THREE,new double[]{.3,1,.5},THREE,new double[]{1,.45,.5},new double[]{.2,1,.5},SIDE};
        BufferedImage sheet=new BufferedImage(1440,960,BufferedImage.TYPE_INT_ARGB);Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(12,15,27));g.fillRect(0,0,1440,960);g.setFont(new Font("SansSerif",Font.BOLD,22));
        for(int i=0;i<scenes.length;i++){
            int x=(i%3)*480,y=(i/3)*480;BufferedImage image=render(scenes[i],480,425,cameras[i],false);
            g.drawImage(image,x,y+40,null);g.setColor(new Color(226,232,246));g.drawString(names[i],x+22,y+29);
            write(image,root+"/deliverables/asriel_vfx/"+names[i].toLowerCase().replace(' ','_')+".png");
        }
        g.dispose();write(sheet,root+"/deliverables/asriel_vfx/preview.png");
        String[] ids={"star_blazing","galacta_blazing","shocker_breaker","shocker_breaker_ex","chaos_saber","chaos_bullets","chaos_buster","chaos_blaster"};
        Scene[] icons={s->AsrielVisualGeometry.star(s,1,5,false,true,0),s->AsrielVisualGeometry.star(s,1,5,true,true,0),
                s->AsrielVisualGeometry.lightning(s,42,3,false,1),s->AsrielVisualGeometry.lightning(s,42,4,true,1),
                s->AsrielVisualGeometry.sabers(s,8,12,3,0),s->AsrielVisualGeometry.comet(s,8,6,false,0),
                s->AsrielVisualGeometry.charge(s,30,40,1,false,0),s->AsrielVisualGeometry.rainbowBeam(s,7,ChaosBusterPattern.BEAM_LIFETIME,12,1.8F,true,1)};
        for(int i=0;i<ids.length;i++){
            BufferedImage icon=render(icons[i],64,64,i<2?new double[]{0,0,1}:i==4?new double[]{0,1,.01}:THREE,true);
            BufferedImage enlarged=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);Graphics2D scale=enlarged.createGraphics();
            scale.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);scale.drawImage(icon,0,0,128,128,null);scale.dispose();
            write(enlarged,root+"/src/main/resources/assets/kazimod/textures/abilities/"+ids[i]+".png");
        }
        int maxQuads=0;
        for(int detail=0;detail<3;detail++)for(int age=0;age<60;age++){
            Mesh[] m={new Mesh(),new Mesh(),new Mesh(),new Mesh(),new Mesh(),new Mesh()};
            AsrielVisualGeometry.star(m[0],2,age,true,false,detail);AsrielVisualGeometry.lightning(m[1],age,4,true,detail);
            AsrielVisualGeometry.comet(m[2],age,5,false,detail);AsrielVisualGeometry.sabers(m[3],age,22,5,detail);
            AsrielVisualGeometry.charge(m[4],age,40,2,true,detail);
            AsrielVisualGeometry.rainbowBeam(m[5],age,ChaosBusterPattern.BEAM_LIFETIME,48,
                    (float)(3.2F*ChaosBusterPattern.beamRadiusScale(age,ChaosBusterPattern.BEAM_LIFETIME)),true,detail);
            for(int effect=0;effect<m.length;effect++) {
                Mesh mesh=m[effect];
                if(mesh.index!=0)throw new AssertionError("Incomplete quad");
                int budget=effect==1?1200:effect==5?1600:500;
                if(mesh.faces.size()>budget)throw new AssertionError("Unbounded VFX mesh: "+effect+" / "+mesh.faces.size());
                maxQuads=Math.max(maxQuads,mesh.faces.size());
            }
        }
        System.out.println("Asriel visual meshes validated across 180 time/detail samples. Maximum "+maxQuads+" quads per effect; preview and eight icons exported.");
    }
}
