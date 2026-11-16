import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.imageio.ImageIO;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ChaosBeamAttackVisuals;

/** Preview the exact production geometry with the existing offline mesh renderer. */
public final class ChaosBeamAttackPreview {
    private static BufferedImage draw(float age, boolean firing, int width, int height, boolean transparent) throws Exception {
        Class<?> scene = Class.forName("AsrielVisualPreview$Scene");
        Object callback = Proxy.newProxyInstance(scene.getClassLoader(), new Class<?>[]{scene}, (p,m,args) -> {
            ChaosBeamAttackVisuals.render((AsrielVisualGeometry.Sink)args[0],age,firing,48,0); return null;
        });
        Method render = AsrielVisualPreview.class.getDeclaredMethod("render",scene,int.class,int.class,double[].class,boolean.class);
        render.setAccessible(true);
        return (BufferedImage)render.invoke(null,callback,width,height,new double[]{0,1,.3},transparent);
    }
    public static void main(String[] args) throws Exception {
        String root = args[0];
        File folder = new File(root,"deliverables/asriel_vfx/chaos_beam"); folder.mkdirs();
        BufferedImage sheet = new BufferedImage(1280,1000,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=sheet.createGraphics(); g.setColor(new Color(12,15,27));g.fillRect(0,0,1280,1000);
        String[] labels={"Charge: two hand orbs","Charge: merged core","Beam: initial cone","Beam: near full width (11 seconds)"};
        float[] ages={10,33,18,220};
        g.setFont(new Font("SansSerif",Font.BOLD,22));
        for(int i=0;i<4;i++) {
            int x=i%2*640,y=i/2*500;
            g.drawImage(draw(ages[i],i>=2,620,450,false),x+10,y+40,null);
            g.setColor(Color.WHITE);g.drawString(labels[i],x+20,y+30);
        }
        g.dispose();ImageIO.write(sheet,"png",new File(folder,"chaos_beam_preview.png"));
        if (args.length > 1 && "--icon".equals(args[1]))
            ImageIO.write(draw(220,true,64,64,true),"png",new File(root,"src/main/resources/assets/kazimod/textures/abilities/chaos_beam.png"));
        System.out.println("Saved Chaos Beam preview.");
    }
}
