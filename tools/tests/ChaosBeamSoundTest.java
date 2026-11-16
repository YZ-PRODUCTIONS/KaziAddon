import java.util.*;
import net.kazi.kazimod.client.asriel.ChaosBeamSoundClient;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/** Exercise the production sound controller with a recording sound engine. */
public final class ChaosBeamSoundTest {
    private static final Minecraft MC=Minecraft.getInstance();
    private static int checks;
    public static void main(String[] args) {
        ChaosBeamAttackEntity field=new ChaosBeamAttackEntity(MC.level);
        join(field);
        List<Integer> cues=new ArrayList<>();
        for(int age=0;age<40;age++) {
            field.age=age;int before=MC.sound.played.size();tick();
            if(MC.sound.played.size()>before)cues.add(age);
        }
        require(cues.equals(Arrays.asList(5,8,11,15,18,21)),"Native charge rhythm converted from 30 fps");
        require(count("charge")==6 && count("hold")==0 && count("launch")==0,"No beam sound runs silently during charging");
        for(int i=0;i<6;i++) {
            TickableSound cue=MC.sound.played.get(i);
            near(cue.getPitch(),.5F+i/6F,"Rising native charge pitch");
            if(i<5)require(cue.isStopped(),"Next charge cue replaces the previous one");
        }
        field.firing=true;field.age=0;tick();
        require(count("launch")==1 && count("hold")==1,"One launch plus a distinct sustained loop");
        TickableSound hold=last("hold");
        near(hold.getPitch(),1,"Original launch pitch");
        require(hold.isLooping(),"Hold recording loops");
        field.age=11;tick();near(hold.getVolume(),.8,"Original hold ramp reaches its target gain");
        field.age=72;tick();near(hold.getPitch(),1,"Do not intensify early");
        field.age=73;tick();near(hold.getPitch(),1.3,"First native pitch escalation");
        field.age=153;field.x=22;tick();near(hold.getPitch(),1.8,"Second native pitch escalation");
        near(hold.getX(),22,"Sound follows the moving beam source");
        require(count("launch")==1 && count("hold")==1,"No repeated launch or duplicate hold loops");
        field.age=239;tick();require(hold.getVolume()<.02,"Loop fades with the visual ending");
        field.age=240;tick();require(MC.sound.active.isEmpty(),"Normal completion stops every owned cue");

        MC.sound.played.clear();
        ChaosBeamAttackEntity late=new ChaosBeamAttackEntity(MC.level);late.firing=true;late.age=100;join(late);tick();
        require(count("charge")==0 && count("launch")==0 && count("hold")==1,"Late tracking starts only the live hold phase");
        near(last("hold").getPitch(),1.3,"Late tracking inherits current intensity");
        ChaosBeamAttackEntity other=new ChaosBeamAttackEntity(MC.level);other.firing=true;join(other);tick();
        TickableSound otherHold=last("hold");
        late.removed=true;tick();require(!otherHold.isStopped() && MC.sound.active.contains(otherHold),"Cancellation cannot stop another field's sounds");
        MC.level=new ChaosBeamAttackEntity.World();tick();require(MC.sound.active.isEmpty(),"Dimension change clears lingering audio");
        ChaosBeamAttackEntity logout=new ChaosBeamAttackEntity(MC.level);logout.firing=true;join(logout);tick();
        ChaosBeamSoundClient.logout(new ClientPlayerNetworkEvent.LoggedOutEvent());
        require(MC.sound.active.isEmpty(),"Disconnect stops all beam sounds");
        System.out.println("Chaos Beam sound lifecycle: "+checks+" checks passed.");
    }
    private static void join(ChaosBeamAttackEntity f){ChaosBeamSoundClient.joined(new EntityJoinWorldEvent(f));}
    private static void tick(){ChaosBeamSoundClient.tick(new TickEvent.ClientTickEvent());MC.sound.tick();}
    private static long count(String name){return MC.sound.played.stream().filter(s->s.name.equals(name)).count();}
    private static TickableSound last(String name){for(int i=MC.sound.played.size()-1;i>=0;i--)if(MC.sound.played.get(i).name.equals(name))return MC.sound.played.get(i);throw new AssertionError(name);}
    private static void near(double a,double b,String message){require(Math.abs(a-b)<.00001,message);}
    private static void require(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
}
