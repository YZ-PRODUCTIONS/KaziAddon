import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import net.kazi.kazimod.client.asriel.HyperGonerMesh;
import net.kazi.kazimod.client.asriel.HyperGonerSkull;

/** Software-rendered production mesh. The blue-gray preview stage is not part of the game effect. */
public final class HyperGonerPreview {
    private static final Color INK = new Color(230, 239, 249), MUTED = new Color(172, 191, 208);
    private static final Camera FRONT = new Camera(0, .035);
    private static final Camera ANGLED = new Camera(Math.PI / 4, .10);
    private static final Camera PROFILE = new Camera(Math.PI / 2, 0);
    private HyperGonerPreview() { }

    public static void main(String[] args) throws Exception {
        File folder = new File(args.length == 0 ? "deliverables/asriel_vfx/hyper_goner" : args[0]);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("Cannot create " + folder);
        sheet(folder);
        turnaround(folder);
        poses(folder);
        ImageIO.write(frame(110, 1400, 1100, FRONT, 12.8, .4, false), "png", new File(folder, "hyper_goner_skull_front.png"));
        ImageIO.write(frame(110, 1400, 1100, ANGLED, 13.2, .4, false), "png", new File(folder, "hyper_goner_skull_angle.png"));
        ImageIO.write(skullFrame(1, 1000, 1100, PROFILE, 10.5, .4), "png", new File(folder, "hyper_goner_skull_profile.png"));
        if (args.length < 2 || !"--stills-only".equals(args[1])) animate(folder);
        System.out.println("Hyper Goner production-mesh previews: " + folder.getAbsolutePath());
    }

    private static void turnaround(File folder) throws Exception {
        BufferedImage image = new BufferedImage(1800, 930, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); quality(g);
        g.setColor(new Color(27,38,51));g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.setColor(INK);g.setFont(new Font("SansSerif",Font.BOLD,29));g.drawString("HYPER GONER / IMPORTED MODEL",26,42);
        g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,15));
        g.drawString("Supplied model and original laughing expression / Gravity effects hidden for inspection",27,68);
        Camera[] cameras={FRONT,ANGLED,PROFILE};String[] labels={"FRONT","THREE-QUARTER / 45 degrees","PROFILE / 90 degrees"};
        for(int i=0;i<cameras.length;i++) {
            int x=18+i*594;
            g.drawImage(skullFrame(1,576,772,cameras[i],10.5,.4),x,90,null);
            label(g,labels[i],x+16,117);
        }
        g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,13));
        g.drawString("Offline production-mesh view. The blue-gray grid is a preview stage, not a game background.",26,900);
        g.dispose();ImageIO.write(image,"png",new File(folder,"hyper_goner_skull_turnaround.png"));
    }

    private static void poses(File folder) throws Exception {
        BufferedImage image=new BufferedImage(1500,900,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();quality(g);
        g.setColor(new Color(27,38,51));g.fillRect(0,0,1500,900);
        g.setColor(INK);g.setFont(new Font("SansSerif",Font.BOLD,29));
        g.drawString("HYPER GONER / YOUR MODEL",26,42);
        g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,15));
        g.drawString("Original geometry and expressions, connected to the cinematic",27,68);
        for(int i=0;i<2;i++) {
            int x=18+i*741;
            g.drawImage(skullFrame(i,723,738,FRONT,10.2,.45),x,90,null);
            label(g,i==0?"NEUTRAL":"LAUGH / SUCTION",x+16,117);
        }
        g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,13));
        g.drawString("Offline model preview. The blue-gray grid is a preview stage, not an in-game background.",26,866);
        g.dispose();ImageIO.write(image,"png",new File(folder,"hyper_goner_model_poses.png"));
    }

    private static BufferedImage skullFrame(float open,int width,int height,Camera camera,double span,double centerY) {
        BufferedImage image=backdrop(width,height);
        Mesh skull=new Mesh();HyperGonerSkull.render(skull,open,0);
        Raster raster=new Raster(image,camera,width/span,centerY);
        skull.faces.sort(Comparator.comparingDouble(face->camera.depth(face)));
        for(Vertex[] face:skull.faces)raster.quad(face,false);
        return image;
    }

    private static void sheet(File folder) throws Exception {
        BufferedImage image = new BufferedImage(1600, 1110, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); quality(g);
        g.setColor(new Color(27, 38, 51)); g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setFont(new Font("SansSerif", Font.BOLD, 29)); g.setColor(INK); g.drawString("HYPER GONER", 26, 42);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15)); g.setColor(MUTED);
        g.drawString("Imported skull with existing debris and gravity effects / Blue-gray preview stage", 27, 68);
        g.drawImage(frame(110, 777, 620, FRONT, 12.8, .4, false), 18, 88, null);
        g.drawImage(frame(110, 777, 620, ANGLED, 13.2, .4, false), 805, 88, null);
        label(g, "FACE / 5.5 s", 35, 115); label(g, "THREE-QUARTER / 5.5 s", 822, 115);
        int[] ages = {28, 65, 105, 165, 215};
        for (int i = 0; i < ages.length; i++) {
            int x = 18 + i * 317;
            g.drawImage(frame(ages[i], 307, 315, FRONT, 35, -1, false), x, 730, null);
            label(g, String.format("%.2f s", ages[i] / 20.0), x + 12, 754);
        }
        g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Offline mesh inspection; Minecraft lighting, world occlusion and cinematic camera are not simulated.", 26, 1082);
        g.dispose(); ImageIO.write(image, "png", new File(folder, "hyper_goner_remake.png"));
    }

    private static void animate(File folder) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (FileImageOutputStream out = new FileImageOutputStream(new File(folder, "hyper_goner_remake.gif"))) {
            writer.setOutput(out); writer.prepareWriteSequence(null);
            for (int age = 0; age <= 260; age += 5) {
                BufferedImage image = frame(age, 800, 650, FRONT, 35, -.5, true);
                ImageWriteParam param = writer.getDefaultWriteParam();
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), param);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
                IIOMetadataNode control = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod", "none"); control.setAttribute("delayTime", "25");
                control.setAttribute("userInputFlag", "FALSE"); control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("transparentColorIndex", "0");
                if (age == 0) {
                    IIOMetadataNode applications = new IIOMetadataNode("ApplicationExtensions"), loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE"); loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[] {1, 0, 0}); applications.appendChild(loop); root.appendChild(applications);
                }
                metadata.setFromTree(format, root); writer.writeToSequence(new IIOImage(image, null, metadata), param);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }

    private static BufferedImage frame(float age, int width, int height, Camera camera, double span, double centerY, boolean overlay) {
        BufferedImage image = backdrop(width,height);
        Graphics2D g;
        Mesh body = new Mesh(), glow = new Mesh(); HyperGonerMesh.render(body, glow, age, 0);
        Raster raster = new Raster(image, camera, width / span, centerY);
        body.faces.sort(Comparator.comparingDouble(face -> camera.depth(face)));
        for (Vertex[] face : body.faces) raster.quad(face, false);
        for (Vertex[] face : glow.faces) raster.quad(face, true);
        if (overlay) {
            g = image.createGraphics(); quality(g);
            float flash = HyperGonerMesh.flash(age);
            if (flash > 0) { g.setColor(new Color(1F, 1F, 1F, flash)); g.fillRect(0, 0, width, height); }
            label(g, String.format("HYPER GONER  /  %.2f s", age / 20), 20, 31);
            g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("Production geometry / offline preview", 20, height - 18); g.dispose();
        }
        return image;
    }

    private static BufferedImage backdrop(int width,int height) {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();quality(g);
        g.setPaint(new GradientPaint(0, 0, new Color(75, 99, 118), 0, height, new Color(38, 54, 71)));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(128, 153, 171, 28));
        int grid = Math.max(24, width / 16);
        for (int x = 0; x < width; x += grid) g.drawLine(x, 0, x, height);
        for (int y = 0; y < height; y += grid) g.drawLine(0, y, width, y);
        g.dispose();
        return image;
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    private static void label(Graphics2D g, String label, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13)); g.setColor(new Color(27, 38, 51, 190));
        g.fillRoundRect(x - 7, y - 18, g.getFontMetrics().stringWidth(label) + 14, 25, 6, 6);
        g.setColor(INK); g.drawString(label, x, y);
    }
    private static final class Vertex {
        final double x, y, z, r, g, b, a;
        Vertex(float x, float y, float z, int rgb, float a) {
            this.x=x; this.y=y; this.z=z; this.r=rgb>>16&255; this.g=rgb>>8&255; this.b=rgb&255; this.a=a;
        }
    }
    private static final class Mesh implements HyperGonerMesh.Sink {
        final List<Vertex[]> faces = new ArrayList<>(); private Vertex[] face = new Vertex[4]; private int at;
        public void vertex(float x, float y, float z, int rgb, float alpha) {
            face[at++] = new Vertex(x,y,z,rgb,alpha);
            if (at == 4) { faces.add(face); face = new Vertex[4]; at = 0; }
        }
    }
    private static final class Camera {
        final double sy, cy, sp, cp;
        Camera(double yaw, double pitch) { sy=Math.sin(yaw); cy=Math.cos(yaw); sp=Math.sin(pitch); cp=Math.cos(pitch); }
        double depth(Vertex[] face) { double sum=0; for(Vertex v:face) sum+=(-v.x*sy+v.z*cy)*cp+v.y*sp; return sum/4; }
        double[] project(Vertex v) { double z=-v.x*sy+(v.z+1.8)*cy; return new double[]{v.x*cy+(v.z+1.8)*sy,v.y*cp-z*sp,z*cp+v.y*sp}; }
    }
    private static final class Raster {
        final int width, height; final int[] pixels; final double[] depth; final Camera camera; final double scale, center;
        Raster(BufferedImage image, Camera camera, double scale, double center) {
            width=image.getWidth(); height=image.getHeight(); pixels=((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            depth=new double[width*height]; Arrays.fill(depth,Double.NEGATIVE_INFINITY); this.camera=camera; this.scale=scale; this.center=center;
        }
        void quad(Vertex[] q, boolean additive) {
            double[][] p=new double[4][];
            for(int i=0;i<4;i++){p[i]=camera.project(q[i]);p[i][0]=width*.5+p[i][0]*scale;p[i][1]=height*.5-(p[i][1]-center)*scale;}
            triangle(p[0],p[1],p[2],q[0],q[1],q[2],additive);triangle(p[0],p[2],p[3],q[0],q[2],q[3],additive);
        }
        void triangle(double[] a,double[] b,double[] c,Vertex va,Vertex vb,Vertex vc,boolean additive) {
            double den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]); if(Math.abs(den)<1e-9)return;
            int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(width-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
            int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(height-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
                double u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,w=1-u-v;
                if(u<0||v<0||w<0)continue; double z=a[2]*u+b[2]*v+c[2]*w; int at=y*width+x;if(z+0.002<depth[at])continue;
                double alpha=Math.max(0,Math.min(1,va.a*u+vb.a*v+vc.a*w));if(alpha<.001)continue;
                double red=va.r*u+vb.r*v+vc.r*w,green=va.g*u+vb.g*v+vc.g*w,blue=va.b*u+vb.b*v+vc.b*w;int old=pixels[at];
                double retain=additive?1:1-alpha;
                int r=(int)Math.min(255,(old>>16&255)*retain+red*alpha),g=(int)Math.min(255,(old>>8&255)*retain+green*alpha),bl=(int)Math.min(255,(old&255)*retain+blue*alpha);
                pixels[at]=0xFF000000|r<<16|g<<8|bl;if(!additive)depth[at]=z;
            }
        }
    }
}
