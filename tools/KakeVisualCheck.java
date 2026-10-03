import net.kazi.kazimod.kake.KakeMesh;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Software previews exercise the same geometry submitted by the in-game renderers. */
public final class KakeVisualCheck {
    static BufferedImage image;static double[] depth;static int size,checks,maxVertices;static double zoom,centerY,yaw=.35,tilt=.30;static boolean glow;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static double[] project(double x,double y,double z){
        y-=centerY;double X=x*Math.cos(yaw)-z*Math.sin(yaw),Z=x*Math.sin(yaw)+z*Math.cos(yaw);
        return new double[]{size/2+X*zoom,size/2-(y*Math.cos(tilt)-Z*Math.sin(tilt))*zoom,y*Math.sin(tilt)+Z*Math.cos(tilt)};
    }
    static double edge(double[]a,double[]b,double x,double y){return(x-a[0])*(b[1]-a[1])-(y-a[1])*(b[0]-a[0]);}
    static void triangle(double[]a,double[]b,double[]c,int color,float opacity){
        if(opacity<.001)return;double area=edge(a,b,c[0],c[1]);if(Math.abs(area)<.0001)return;
        int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(size-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(size-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
            double u=edge(b,c,x+.5,y+.5)/area,v=edge(c,a,x+.5,y+.5)/area,w=1-u-v;
            if(u<0||v<0||w<0)continue;double z=u*a[2]+v*b[2]+w*c[2];int index=y*size+x;
            if(z<depth[index]-.003)continue;
            int old=image.getRGB(x,y);
            if(glow){int r=Math.min(255,(old>>16&255)+(int)((color>>16&255)*opacity)),g=Math.min(255,(old>>8&255)+(int)((color>>8&255)*opacity)),b1=Math.min(255,(old&255)+(int)((color&255)*opacity));image.setRGB(x,y,0xff000000|r<<16|g<<8|b1);}
            else{depth[index]=z;image.setRGB(x,y,0xff000000|color);}
        }
    }
    static KakeMesh.Sink sink(){
        double[][]q=new double[4][];int[]i={0};return(x,y,z,c,a)->{q[i[0]++]=project(x,y,z);if(i[0]==4){triangle(q[0],q[1],q[2],c,a);triangle(q[0],q[2],q[3],c,a);i[0]=0;}};
    }
    static void scene(KakeMesh.Sink s,int scene,float age,boolean light){
        switch(scene){
            case 0:KakeMesh.slot(s,age,-1,-1,light);break;
            case 1:KakeMesh.slot(s,age,20,7,light);break;
            case 2:KakeMesh.coin(KakeMesh.rotate(s,.35,1),light);break;
            case 3:KakeMesh.dice(KakeMesh.rotate(KakeMesh.rotate(s,.35,0),.4,1),light);break;
            case 4:KakeMesh.card(KakeMesh.rotate(s,-.24,2),1,light);break;
            case 5:KakeMesh.chip(KakeMesh.rotate(s,.2,1),2,light);break;
            default:int kind=scene-4;KakeMesh.effect(s,kind,0,age,900,kind==6?40:30,light,false);
        }
    }
    static BufferedImage render(int scene,int pixels,float age,boolean transparent){
        size=pixels;image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);depth=new double[size*size];Arrays.fill(depth,-Double.MAX_VALUE);
        if(!transparent){Graphics2D g=image.createGraphics();g.setColor(new Color(24,29,38));g.fillRect(0,0,size,size);g.dispose();}
        double span=scene<2?3.5:scene==3?2.1:scene<6?1.65:scene==6?4.6:scene==7?3.5:scene==8?61:scene==9?11:scene==10?96:12;
        centerY=scene<2?1.15:scene<6?0:scene==6?1:scene==7?1.1:scene==8?10:scene==9?1:scene==10?13:2;
        zoom=size/span;for(boolean pass:new boolean[]{false,true}){glow=pass;scene(sink(),scene,age,pass);}
        int changed=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++)if(image.getRGB(x,y)!=(transparent?0:0xff181d26))changed++;
        check(changed>size*size*.005,"Blank preview "+scene);return image;
    }
    public static void main(String[]args)throws Exception{
        for(int result=0;result<10;result++)for(int reel=0;reel<3;reel++){
            double finalPosition=KakeMesh.reelPosition(50,20,result,reel);
            check(Math.abs((finalPosition+result)%10)<1e-6,"Reel must stop on authoritative result");
            double previous=KakeMesh.reelPosition(20,20,result,reel);
            for(int i=1;i<=120;i++){double now=KakeMesh.reelPosition(20+i*.1F,20,result,reel);check(now>=previous-.0001&&Double.isFinite(now),"Smooth forward reel braking");previous=now;}
        }
        for(int kind=0;kind<12;kind++)for(int age=0;age<=120;age+=2){
            int[]count={0};KakeMesh.Sink s=(x,y,z,c,a)->{check(Double.isFinite(x+y+z)&&Float.isFinite(a)&&a>=0&&a<=1,"Finite mesh/opacity");check(Math.abs(x)<110&&Math.abs(y)<100&&Math.abs(z)<110,"Effect bounds");count[0]++;};
            for(boolean glow:new boolean[]{false,true})scene(s,kind,age,glow);
            check(count[0]%4==0&&count[0]<16000,"Bounded mesh "+kind+": "+count[0]);maxVertices=Math.max(maxVertices,count[0]);
        }
        for(int kind:new int[]{1,8})for(int variant=0;variant<=9;variant++)for(int age=0;age<=16;age++){
            int[]count={0};KakeMesh.Sink sink=(x,y,z,c,a)->{check(Double.isFinite(x+y+z)&&a>=0&&a<=1,"Finite impact geometry");count[0]++;};
            for(boolean pass:new boolean[]{false,true})KakeMesh.effect(sink,kind,variant,age,16,4,pass,false);
            check(count[0]%4==0&&count[0]<16000,"Bounded impact shards");maxVertices=Math.max(maxVertices,count[0]);
        }
        for(int kind:new int[]{4,6,7}){
            int[]near={0},far={0};for(boolean pass:new boolean[]{false,true}){
                KakeMesh.effect((x,y,z,c,a)->near[0]++,kind,0,50,900,40,pass,false);
                KakeMesh.effect((x,y,z,c,a)->far[0]++,kind,0,50,900,40,pass,true);
            }check(far[0]<near[0],"Distant area effects reduce geometry");
        }
        Path out=Paths.get("deliverables/kake_vfx");Files.createDirectories(out);
        BufferedImage sheet=new BufferedImage(1600,1260,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(15,19,25));g.fillRect(0,0,1600,1260);
        String[] names={"SLOT SPIN / ROLLING","SLOT SPIN / RESULT","COIN FLICK","LOADED DICE","CARD SLASH","CASINO CHIPS","DOUBLE DOWN","JACKPOT SHOT","CHIP RAIN","LUCKY SEVEN","CASINO STORM","JACKPOT"};
        for(int i=0;i<12;i++){g.drawImage(render(i,390,i==0?10:i==1?40:50,false),(i%4)*400+5,(i/4)*420,null);g.setColor(new Color(238,231,211));g.setFont(new Font("SansSerif",Font.BOLD,15));g.drawString(names[i],(i%4)*400+16,(i/4)*420+408);}
        g.dispose();ImageIO.write(sheet,"png",out.resolve("kake_vfx_preview.png").toFile());
        Path icons=Paths.get("src/main/resources/assets/kazimod/textures/abilities");Files.createDirectories(icons);
        ImageIO.write(render(1,96,40,true),"png",icons.resolve("slot_spin.png").toFile());
        ImageIO.write(render(1,96,40,true),"png",icons.resolve("lucky_slot.png").toFile());
        BufferedImage card=render(4,96,30,true),dice=render(3,96,30,true),coin=render(2,96,30,true);
        BufferedImage icon=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);g=icon.createGraphics();g.drawImage(card,1,0,58,76,null);g.drawImage(dice,28,3,67,67,null);g.drawImage(coin,7,42,53,53,null);g.dispose();ImageIO.write(icon,"png",icons.resolve("casino_roll.png").toFile());
        System.out.println("Kake checks: "+checks+" passed; largest effect "+maxVertices+" vertices. Preview and transparent icons generated.");
    }
}
