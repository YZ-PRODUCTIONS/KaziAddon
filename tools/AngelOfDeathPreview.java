import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import net.kazi.kazimod.abilities.YagiAwakening.AngelOfDeathPattern;
import net.kazi.kazimod.renderers.entities.asriel.AngelOfDeathVisuals;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;

/** Offline native-mesh preview using the production fan schedule and bounded steering math. */
public final class AngelOfDeathPreview {
    private static final int BACKGROUND=0xFF0A101A;
    private static final double[] TARGET={0,1.6,32};
    private static final List<double[]>[] PATHS=paths();
    public static void main(String[] args)throws Exception {
        File folder=new File(args[0],"deliverables/asriel_vfx/angel_of_death");folder.mkdirs();
        int[] ticks={29,44,63,91};
        BufferedImage sheet=new BufferedImage(1600,1150,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(BACKGROUND));g.fillRect(0,0,1600,1150);
        g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,26));g.drawString("Angel of death — alternating curved beam fans",24,35);
        g.setFont(new Font("SansSerif",Font.PLAIN,16));g.drawString("Production pattern and mesh | outward launch, then aim assist | hollow pastel heads and real flight trails",24,62);
        for(int i=0;i<4;i++)g.drawImage(frame(ticks[i],800,530),i%2*800,82+i/2*530,null);
        g.dispose();ImageIO.write(sheet,"png",new File(folder,"angel_of_death_pattern.png"));
        ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(FileImageOutputStream output=new FileImageOutputStream(new File(folder,"angel_of_death_pattern.gif"))) {
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int tick=0;tick<=130;tick+=2) {
                BufferedImage image=frame(tick,700,680);
                ImageWriteParam params=writer.getDefaultWriteParam();
                IIOMetadata meta=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),params);
                String format=meta.getNativeMetadataFormatName();IIOMetadataNode tree=(IIOMetadataNode)meta.getAsTree(format);
                IIOMetadataNode control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod","none");control.setAttribute("delayTime","10");
                control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("transparentColorIndex","0");
                if(tick==0){IIOMetadataNode extensions=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);tree.appendChild(extensions);}
                meta.setFromTree(format,tree);writer.writeToSequence(new IIOImage(image,null,meta),params);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
        System.out.println("Angel of death native-mesh previews: "+folder.getAbsolutePath());
    }
    @SuppressWarnings("unchecked") private static List<double[]>[] paths() {
        List<double[]>[] paths=(List<double[]>[])new List<?>[AngelOfDeathPattern.SHOT_COUNT];
        for(int shot=0;shot<paths.length;shot++) {
            List<double[]> path=new ArrayList<>();double[] p={AngelOfDeathPattern.side(shot)*1.1,2,.6};
            double[] d=AngelOfDeathPattern.initialDirection(shot),v={d[0]*AngelOfDeathPattern.SPEED,d[1]*AngelOfDeathPattern.SPEED,d[2]*AngelOfDeathPattern.SPEED};
            AngelOfDeathPattern.Guidance guidance=new AngelOfDeathPattern.Guidance();
            path.add(p.clone());
            for(int age=0;age<AngelOfDeathPattern.MAX_LIFE;age++) {
                double dx=TARGET[0]-p[0],dy=TARGET[1]-p[1],dz=TARGET[2]-p[2],distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
                if(distance<1.0)break;
                v=guidance.steer(v[0],v[1],v[2],dx,dy,dz,age);
                p=new double[]{p[0]+v[0],p[1]+v[1],p[2]+v[2]};path.add(p);
            }
            paths[shot]=path;
        }
        return paths;
    }
    private static BufferedImage frame(int tick,int width,int height) {
        Mesh mesh=new Mesh();int active=0;
        for(int shot=0;shot<PATHS.length;shot++) {
            int age=tick-AngelOfDeathPattern.shotTick(shot);List<double[]> path=PATHS[shot];
            if(age<0||age>=path.size())continue;active++;
            int count=Math.min(14,age+1);double[][] trail=new double[count][];
            for(int i=0;i<count;i++)trail[i]=path.get(age-i);
            AngelOfDeathVisuals.render(mesh,trail,age,shot,0,0,80,10);
        }
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(BACKGROUND));g.fillRect(0,0,width,height);g.dispose();
        double scale=Math.min((width-60)/42.0,(height-100)/40.0);
        for(Vertex[] face:mesh.faces){double[][] p=new double[4][];for(int i=0;i<4;i++)p[i]=project(face[i].x,face[i].y,face[i].z,width,scale);triangle(image,p[0],p[1],p[2],face[0],face[1],face[2]);triangle(image,p[0],p[2],p[3],face[0],face[2],face[3]);}
        g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setFont(new Font("SansSerif",Font.BOLD,18));g.setColor(Color.WHITE);
        g.drawString(String.format("%.2fs after use | %d beams in flight",tick/20.0,active),18,28);
        double[] caster=project(0,2,.6,width,scale),target=project(TARGET[0],TARGET[1],TARGET[2],width,scale);
        g.setColor(new Color(146,164,188));g.drawOval((int)caster[0]-5,(int)caster[1]-5,10,10);g.setFont(new Font("SansSerif",Font.PLAIN,14));g.drawString("Caster",(int)caster[0]+12,(int)caster[1]+5);
        g.setColor(new Color(245,99,122));g.drawOval((int)target[0]-6,(int)target[1]-6,12,12);g.drawString("Target / aim point",(int)target[0]+12,(int)target[1]+5);g.dispose();return image;
    }
    private static double[] project(double x,double y,double z,int width,double scale){return new double[]{width*.5+x*scale,70+(z*.992-y*.124)*scale};}
    private static void triangle(BufferedImage image,double[] a,double[] b,double[] c,Vertex va,Vertex vb,Vertex vc){
        double den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-10)return;
        int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(image.getWidth()-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(image.getHeight()-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
            double u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den;if(u<0||v<0||u+v>1)continue;
            double w=1-u-v,alpha=va.a*u+vb.a*v+vc.a*w;int prior=image.getRGB(x,y);
            int r=Math.min(255,(prior>>16&255)+(int)((va.r*u+vb.r*v+vc.r*w)*alpha*255)),gr=Math.min(255,(prior>>8&255)+(int)((va.g*u+vb.g*v+vc.g*w)*alpha*255)),bl=Math.min(255,(prior&255)+(int)((va.b*u+vb.b*v+vc.b*w)*alpha*255));
            image.setRGB(x,y,0xFF000000|r<<16|gr<<8|bl);
        }
    }
    private static final class Vertex{final double x,y,z;final float r,g,b,a;Vertex(double x,double y,double z,float r,float g,float b,float a){this.x=x;this.y=y;this.z=z;this.r=r;this.g=g;this.b=b;this.a=a;}}
    private static final class Mesh implements AsrielVisualGeometry.Sink{final List<Vertex[]>faces=new ArrayList<>();final Vertex[]pending=new Vertex[4];int index;public void vertex(double x,double y,double z,float r,float g,float b,float a){pending[index++]=new Vertex(x,y,z,r,g,b,a);if(index==4){faces.add(pending.clone());index=0;}}}
}
