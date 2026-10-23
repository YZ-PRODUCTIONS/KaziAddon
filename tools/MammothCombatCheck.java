import net.kazi.kazimod.mammoth.MammothCombatMath;
import net.kazi.kazimod.mammoth.MammothMesh;
import net.kazi.kazimod.models.zoan.CerberusRig;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public final class MammothCombatCheck {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static final class Face {double[][]p=new double[4][];int color;float alpha;}
    static List<Face> mesh(int kind,float age,boolean far){
        List<Face> faces=new ArrayList<>();final Face[] current={new Face()};final int[]index={0};
        MammothMesh.Sink sink=(x,y,z,c,a)->{
            check(Double.isFinite(x+y+z)&&Math.abs(x)<40&&Math.abs(z)<40&&Math.abs(y)<30,"Bounded effect geometry");check(Float.isFinite(a)&&a>=0&&a<=1,"Valid opacity");
            current[0].p[index[0]++]=new double[]{x,y,z};current[0].color=c;current[0].alpha=a;
            if(index[0]==4){faces.add(current[0]);current[0]=new Face();index[0]=0;}
        };
        for(boolean soft:new boolean[]{false,true}){
            if(kind<=1)MammothMesh.ground(sink,age,kind==0?18:28,15,kind==1,soft,far,null,null);
            else if(kind==2)MammothMesh.wind(sink,age,soft);
            else MammothMesh.vacuum(sink,age,80,0,5.3,8.7,soft,far);
        }
        check(index[0]==0&&faces.size()*4<3000,"Bounded complete quads");return faces;
    }
    static double[]project(double[]p,int kind){
        double yaw=Math.toRadians(kind==2?28:32),pitch=Math.toRadians(kind==2?12:48),scale=kind==2?48:10;
        double x=p[0]*Math.cos(yaw)+p[2]*Math.sin(yaw),z=-p[0]*Math.sin(yaw)+p[2]*Math.cos(yaw),y=p[1];
        return new double[]{210+x*scale,(kind==2?165:225)-(y*Math.cos(pitch)-z*Math.sin(pitch))*scale,z*Math.cos(pitch)+y*Math.sin(pitch)};
    }
    static BufferedImage frame(int kind,float age){
        BufferedImage img=new BufferedImage(420,360,BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();g.setColor(new Color(25,32,36));g.fillRect(0,0,420,360);
        if(kind<=1){
            g.setColor(new Color(67,75,62));int[]x=new int[4],y=new int[4];double[][]corners={{-15,0,-15},{15,0,-15},{15,0,15},{-15,0,15}};for(int i=0;i<4;i++){double[]p=project(corners[i],kind);x[i]=(int)p[0];y[i]=(int)p[1];}g.fillPolygon(x,y,4);
            g.setColor(new Color(77,83,70));for(int j=-15;j<=15;j+=3){double[]a=project(new double[]{j,0,-15},kind),b=project(new double[]{j,0,15},kind),c=project(new double[]{-15,0,j},kind),d=project(new double[]{15,0,j},kind);g.drawLine((int)a[0],(int)a[1],(int)b[0],(int)b[1]);g.drawLine((int)c[0],(int)c[1],(int)d[0],(int)d[1]);}
        }
        List<Face> faces=mesh(kind,age,false);faces.sort(Comparator.comparingDouble(f->Arrays.stream(f.p).mapToDouble(p->project(p,kind)[2]).average().orElse(0)));
        for(Face f:faces){int[]x=new int[4],y=new int[4];for(int i=0;i<4;i++){double[]p=project(f.p[i],kind);x[i]=(int)p[0];y[i]=(int)p[1];}g.setColor(new Color((f.color>>16&255)/255F,(f.color>>8&255)/255F,(f.color&255)/255F,f.alpha));g.fillPolygon(x,y,4);}
        g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString(new String[]{"Stomp tremor","Final ground rupture","Ancient Sweep wind","Trunk Vacuum airflow"}[kind],18,32);g.dispose();
        int changed=0;for(int y=50;y<330;y++)for(int x=10;x<410;x++)if(img.getRGB(x,y)!=(0xff000000|25<<16|32<<8|36))changed++;
        check(changed>600,"Visible effect preview");return img;
    }
    public static void main(String[]args)throws Exception{
        check(MammothCombatMath.STAMPEDE_CHARGE==60&&MammothCombatMath.STAMPEDE_DURATION==100&&MammothCombatMath.STAMPEDE_SPEED==2.3,"Corna Dio charge, duration and speed");
        check(MammothCombatMath.inStomp(15,0,0)&&MammothCombatMath.inStomp(9,6,12),"Stomp includes 15-block boundary");
        check(!MammothCombatMath.inStomp(15.01,0,0)&&!MammothCombatMath.inStomp(15,0,15)&&!MammothCombatMath.inStomp(0,6.01,0),"Stomp uses a bounded cylinder, not a square");
        check(MammothCombatMath.finalDamage(0)==80&&MammothCombatMath.finalDamage(15)==72.5,"Final quake falloff");
        check(MammothCombatMath.reducedDamage(100,true)==75&&MammothCombatMath.reducedDamage(100,false)==90,"Requested form damage reduction");
        check(MammothCombatMath.pullSpeed(0)==0&&MammothCombatMath.pullSpeed(.8)==0,"Vacuum settles near the trunk");
        for(double d=0;d<100;d+=.01)check(MammothCombatMath.pullSpeed(d)>=0&&MammothCombatMath.pullSpeed(d)<=1.1,"Bounded vacuum velocity");
        for(int kind=0;kind<4;kind++)for(boolean far:new boolean[]{false,true})for(int t=0;t<(kind==0?18:kind==1?28:80);t++)mesh(kind,t+.1F,far);
        for(boolean full:new boolean[]{false,true}){
            CerberusRig rig;try(Reader r=Files.newBufferedReader(Paths.get("src/main/resources/assets/kazimod/models/zoan/mammoth_"+(full?"full":"hybrid")+".json"))){rig=CerberusRig.read(r);}
            List<CerberusRig.Node> path=rig.path("trunk_tip");float[] tip=full?new float[]{0,8.5F,-61.05F}:new float[]{0,12.3F,-15.6F};
            for(int t=0;t<100;t++){
                Map<String,CerberusRig.Pose>pose=rig.pose();rig.apply(pose,"vacuum",t/20F,1);float[] actual=MammothCheck.transform(tip,path,pose);double[]computed=MammothCombatMath.vacuumTip(full,t,0,0);
                for(int k=0;k<3;k++)check(Math.abs(actual[k]-computed[k])<.035,"Vacuum endpoint matches the animated trunk");
            }
        }
        BufferedImage sheet=new BufferedImage(1680,360,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();float[]ages={7,9,6,33};for(int k=0;k<4;k++)g.drawImage(frame(k,ages[k]),420*k,0,null);g.dispose();ImageIO.write(sheet,"png",new File("deliverables/mammoth/mammoth_combat_vfx.png"));
        System.out.println("Mammoth combat: "+checks+" range, damage, suction, rig endpoint, VFX budget and preview checks passed.");
    }
}
