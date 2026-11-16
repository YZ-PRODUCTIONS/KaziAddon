import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.client.goru.EaDrawMesh;
import net.kazi.kazimod.models.abilities.EaVfxMesh;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;

public class EaDrawTest {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception {
  InnerDragonsSequence s=new InnerDragonsSequence(.5);
  check(!s.update(50,100,0,true),"exactly 50% must not trigger");
  check(s.update(49,100,1,true),"under 50% must trigger");
  check(!s.update(20,100,2,true),"damage must not restart");
  check(s.isPlaying(160)&&!s.isPlaying(161),"eight-second deadline");
  check(!s.update(40,100,200,true),"remaining low must not replay");
  s.update(80,100,201,true);check(s.update(49,100,202,true),"healed health cycle");
  s.update(40,100,203,false);check(!s.isPlaying(203),"suppression cleanup");
  check(!s.update(40,100,204,true),"suppression must not restart");
  check(!s.update(Double.NaN,100,205,true),"invalid health");
  InnerDragonsSequence nagi=new InnerDragonsSequence();
  check(!nagi.update(49,100,1,true)&&nagi.update(29,100,2,true),"Nagi threshold unchanged");
  for(int lod=0;lod<3;lod++)for(float age=0;age<160;age+=.5F){
   final int[] count={0};
   EaVfxMesh.Sink sink=(x,y,z,c,a)->{count[0]++;check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(a),"finite vertices");check(a>=0&&a<=1,"alpha range");check(Math.abs(x)<9&&Math.abs(y)<9&&Math.abs(z)<9,"bounded geometry");};
   EaDrawMesh.render(sink,sink,age,lod);check(count[0]%4==0,"complete quads");check(count[0]<150000,"vertex budget");
  }
  EaVfxMesh.Sink fail=(x,y,z,c,a)->{throw new AssertionError("inactive mesh");};
  for(float age:new float[]{-1,160,Float.NaN,Float.POSITIVE_INFINITY})EaDrawMesh.render(fail,fail,age,0);
  BufferedImage sheet=new BufferedImage(1200,700,BufferedImage.TYPE_INT_RGB);
  Graphics2D g=sheet.createGraphics();g.setColor(new Color(18,20,29));g.fillRect(0,0,1200,700);
  int[] ages={30,60,83,96,110,134};
  for(int f=0;f<ages.length;f++){
   final java.util.List<float[]> quads=new ArrayList<>();final float[] quad=new float[20];final int[] index={0};
   EaVfxMesh.Sink collect=(x,y,z,c,a)->{int i=index[0]*5;quad[i]=x;quad[i+1]=y;quad[i+2]=z;quad[i+3]=c;quad[i+4]=a;if(++index[0]==4){quads.add(quad.clone());index[0]=0;}};
   EaDrawMesh.render(collect,collect,ages[f],1);
   quads.sort(Comparator.comparingDouble(q->-(q[0]*.65+q[2]*.76)));
   int ox=f%3*400+200,oy=f/3*350+245;
   g.setColor(new Color(45,49,60));g.fillRect(ox-18,oy-92,36,92);g.fillRect(ox-17,oy-126,34,34);
   for(float[]q:quads){int[]xx=new int[4],yy=new int[4];float a=0;for(int k=0;k<4;k++){xx[k]=ox+(int)((q[k*5]*.76-q[k*5+2]*.65)*51);yy[k]=oy-(int)((q[k*5+1]-.12*(q[k*5]*.65+q[k*5+2]*.76))*51);a+=q[k*5+4]/4;}
    int rgb=(int)q[3];g.setColor(new Color(rgb>>16&255,rgb>>8&255,rgb&255,Math.min(255,Math.max(0,(int)(a*255)))));g.fillPolygon(xx,yy,4);}
   g.setColor(Color.WHITE);g.drawString("Ea draw — tick "+ages[f],f%3*400+15,f/3*350+25);
  }
  g.dispose();ImageIO.write(sheet,"png",new File(args[0]));
  System.out.println("Ea draw: "+checks+" threshold, lifecycle and geometry checks passed.");
 }
}
