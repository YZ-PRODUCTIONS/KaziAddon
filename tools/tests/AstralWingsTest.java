import net.kazi.kazimod.client.asriel.AstralWingsMesh;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;

public final class AstralWingsTest {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        check(YagiAwakeningMath.enhanced(49,100),"below half must show wings");
        check(!YagiAwakeningMath.enhanced(50,100)&&!YagiAwakeningMath.enhanced(51,100),"half or healed must hide wings");
        check(!YagiAwakeningMath.enhanced(Float.NaN,100)&&!YagiAwakeningMath.enhanced(30,0),"invalid health");
        int[] peaks=new int[3];
        for(int lod=0;lod<3;lod++)for(boolean flying:new boolean[]{false,true})for(int age=0;age<280;age+=2) {
            final int[] count={0};final float[] bounds={0,0};
            AstralWingsMesh.render((x,y,z,c,a)->{
                count[0]++;bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.max(bounds[1],x);
                check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(a),"finite mesh");
                check(Math.abs(x)<4&&Math.abs(y)<2&&Math.abs(z)<2,"bounded wing geometry");
                check(a>=0&&a<=1,"alpha range");
            },age,flying,lod);
            check(count[0]>0&&count[0]%4==0,"complete mesh while standing and flying");
            check(Math.abs(bounds[0]+bounds[1])<.0001F&&bounds[1]>2.25F,"two spread mirrored wings");
            check(count[0]<8000,"persistent VFX budget");peaks[lod]=Math.max(peaks[lod],count[0]);
        }
        AstralWingsMesh.render((x,y,z,c,a)->{throw new AssertionError("invalid age rendered");},Float.NaN,false,0);
        preview(args[0]);System.out.println("Astral wings: "+checks+" checks passed; peak vertices "+Arrays.toString(peaks));
    }
    private static void preview(String output)throws Exception {
        BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(18,23,34));g.fillRect(0,0,1440,900);
        for(int frame=0;frame<6;frame++){
            final int f=frame;final float[] q=new float[20];final int[] index={0};
            Graphics2D panel=(Graphics2D)g.create(frame%2*720,frame/2*300,720,300);
            AstralWingsMesh.render((x,y,z,c,a)->{
                int k=index[0]++*5;q[k]=x;q[k+1]=y;q[k+2]=z;q[k+3]=c;q[k+4]=a;
                if(index[0]!=4)return;index[0]=0;
                int[] px=new int[4],py=new int[4];float alpha=0;int red=0,green=0,blue=0;
                for(int i=0;i<4;i++){px[i]=360+Math.round(q[i*5]*80);py[i]=145-Math.round(q[i*5+1]*80);alpha+=q[i*5+4]/4;int rgb=(int)q[i*5+3];red+=rgb>>16&255;green+=rgb>>8&255;blue+=rgb&255;}
                panel.setColor(new Color(red/4,green/4,blue/4,Math.round(alpha*255)));panel.fillPolygon(px,py,4);
            },frame*27,frame%2==1,0);
            panel.setColor(new Color(82,91,107));panel.fillRect(349,145,22,60);panel.fillRect(346,118,28,28);panel.fillRect(337,149,11,55);panel.fillRect(372,149,11,55);panel.fillRect(349,206,10,54);panel.fillRect(361,206,10,54);
            panel.setColor(Color.WHITE);panel.drawString((frame%2==0?"Standing":"Flying")+" / tick "+frame*27,15,22);panel.dispose();
        }
        g.dispose();ImageIO.write(image,"png",new File(output));
    }
}
