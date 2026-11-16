import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kazi.kazimod.renderers.entities.asriel.*;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Replays the actual attack meshes through the shared wing material. */
public final class AsrielFlowPatternTest {
    private static long checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void mesh(String name, Consumer<Sink> draw, float scale, float age) {
        List<double[]> normal = new ArrayList<>(), ink = new ArrayList<>();
        Sink original=(x,y,z,r,g,b,a)->normal.add(new double[]{x,y,z,r,g,b,a});
        Sink result=(x,y,z,r,g,b,a)->ink.add(new double[]{x,y,z,r,g,b,a});
        draw.accept(original);
        draw.accept(AsrielVisualGeometry.flowingPalette(result,.6F,age,scale));
        check(!normal.isEmpty(),name+" must render");
        check(ink.size()==normal.size(),name+" must reuse its existing geometry");
        float darkest=1,lightest=0;
        for(int i=0;i<normal.size();i++) {
            double[] a=normal.get(i),b=ink.get(i);
            for(int j=0;j<3;j++)check(a[j]==b[j],name+" changed position");
            check(b[6]>=0 && b[6]<=a[6]+.00001,name+" ink must retain the authored fade envelope");
            for(int j=3;j<6;j++) {
                check(Double.isFinite(b[j])&&b[j]>=0&&b[j]<=a[j]*.60001+.00001,name+" invalid tint");
                if(a[j]>.01) { float shade=(float)(b[j]/a[j]);darkest=Math.min(darkest,shade);lightest=Math.max(lightest,shade); }
            }
        }
        check(lightest-darkest>.12F,name+" needs visible spatial bands, not a uniform fade");
    }
    public static void main(String[] args) {
        for(int detail=0;detail<3;detail++) {
            final int d=detail;
            mesh("star",s->AsrielVisualGeometry.star(s,8,20,true,false,false,d),.3F,20);
            mesh("fragment",s->AsrielVisualGeometry.star(s,.8F,10,true,true,false,d),3F,10);
            mesh("burst",s->AsrielVisualGeometry.starBurst(s,8,6,true,false,d),.3F,6);
            mesh("lightning",s->AsrielVisualGeometry.lightning(s,48,4,true,d),.3F,48);
            mesh("charge aura",s->ShockerBreakerVisuals.chargeAura(s,18,d),1F,18);
            mesh("sabers",s->AsrielVisualGeometry.sabers(s,49,44,10,d),2.4F/11,49);
            mesh("cannon",s->AsrielVisualGeometry.busterCharge(s,12,.6F,3.2F,true,d),.8F,12);
            mesh("beam",s->AsrielVisualGeometry.rainbowBeam(s,20,66,96,3.2F,true,d),6F/96,20);
            mesh("buster bullet",s->AsrielVisualGeometry.comet(s,10,2,true,d),6F,10);
            mesh("angel trail",s->AngelOfDeathVisuals.render(s,new double[][]{{0,0,0},{1,.2,-2},{2,.6,-6},{3,1,-12}},12,2,d,1,2,10),.24F,12);
        }
        mesh("ground warning",s->ShockerBreakerWarningVisuals.ground(s,18,12),.2F,18);
        mesh("warning icon",s->ShockerBreakerWarningVisuals.icon(s,18),1F,18);
        int moving=0;
        for(float x=-3;x<=3;x+=.15F)for(float y=-3;y<=3;y+=.15F) {
            float flow=x*.115F+y*.075F;
            float a=AsrielFlowPattern.shade(flow,y);
            float b=AsrielFlowPattern.shade(flow+AsrielFlowPattern.phase(7),y);
            if(Math.abs(a-b)>.1F)moving++;
            float before=AsrielFlowPattern.shade(flow+AsrielFlowPattern.phase(279.999F),y);
            float after=AsrielFlowPattern.shade(flow+AsrielFlowPattern.phase(280F),y);
            check(Math.abs(before-after)<.001F,"Ink must loop without a flash");
        }
        check(moving>500,"Ink must travel rather than stay attached as fixed markings");
        System.out.println("Asriel flowing ink: "+checks+" geometry, color, opacity and animation checks passed across every attack type and detail level.");
    }
}
