import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility.Mode;
import net.kazi.kazimod.entities.EnkiduVfxEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class EnkiduMissTest {
    private static int checks;
    private static void check(boolean result,String reason){checks++;if(!result)throw new AssertionError(reason);}
    private static final class Fixture {
        final World world=new World();final LivingEntity owner=new PlayerEntity(world);
        final EnkiduAbility ability=new EnkiduAbility();final EnkiduVfxEntity cast=new EnkiduVfxEntity(new EntityType<>(),world);
        Fixture(){owner.hostile=false;owner.equipped=ability;ability.cast=cast;}
        LivingEntity target(double x,boolean player){LivingEntity target=player?new PlayerEntity(world):new LivingEntity(world);target.setPos(x,0,0);return target;}
        void begin(Mode mode,double x,LivingEntity selected){cast.begin(owner,ability,mode,new Vector3d(x,0,0),selected);}
        void tick(int n){for(int i=0;i<n;i++){world.gameTime++;if(!cast.removed)cast.tick();}}
        void finishMiss(){
            cast.launch();check(cast.getPhase()==EnkiduVfxEntity.LAUNCHING,"An aimed miss still launches its chain effects");
            check(cast.getVisualTargets().length==0,"A miss must never manufacture a damage target");
            check(world.sounds.contains("TRIDENT_RETURN"),"A missed launch keeps its release sound");
            tick(EnkiduVfxEntity.TRAVEL_TICKS);check(cast.isFading(),"Empty chains retract after their normal travel window");
            check(cast.getVisualPhase()==EnkiduVfxEntity.LAUNCHING,"A missed launch fades its release visuals");
            tick(EnkiduVfxEntity.FADE_TICKS);check(cast.removed,"A missed cast cleans up its portals");
        }
    }
    public static void main(String[] args){
        for(Mode mode:new Mode[]{Mode.SINGLE,Mode.AREA}){
            Fixture empty=new Fixture();LivingEntity offAim=empty.target(20,true);empty.begin(mode,64,null);
            empty.tick(mode.chargeTicks);check(empty.cast.isCharging(),"Empty aim at64blocks finishes charging instead of being canceled");
            check(empty.cast.getCenter(1).x==64,"Empty aim remains at the captured point");
            empty.finishMiss();check(offAim.boundApplications==0&&offAim.floatApplications==0&&offAim.strikes==0,"An off-aim player cannot steal an empty cast");
        }

        Fixture lost=new Fixture();LivingEntity selected=lost.target(30,true);LivingEntity replacement=lost.target(31,false);
        lost.begin(Mode.SINGLE,30,selected);selected.alive=false;lost.tick(Mode.SINGLE.chargeTicks);
        check(lost.cast.isCharging()&&lost.cast.getVisualTargets().length==0,"A dead selected target turns into a miss without canceling charge");
        selected.alive=true;lost.finishMiss();check(selected.boundApplications==0&&replacement.boundApplications==0,"Lost targets are not reacquired or replaced by nearby enemies");

        Fixture escaped=new Fixture();LivingEntity escapee=escaped.target(30,true);escaped.begin(Mode.SINGLE,30,escapee);
        escapee.setPos(100,0,0);escaped.tick(Mode.SINGLE.chargeTicks);
        check(escaped.cast.isCharging()&&escaped.cast.getVisualTargets().length==0,"A target escaping during charge leaves an ordinary missed cast");
        escaped.finishMiss();check(escapee.strikes==0&&escapee.boundApplications==0,"An escaped target receives no restraint or damage");

        for(boolean player:new boolean[]{true,false}){
            Fixture hit=new Fixture();LivingEntity target=hit.target(64,player);hit.begin(Mode.SINGLE,64,target);hit.tick(5);
            check(hit.cast.isCharging(),"A valid player or mob at advertised64block range remains targetable");
            hit.cast.launch();hit.tick(2);check(hit.cast.getPhase()==EnkiduVfxEntity.BOUND&&target.boundApplications>0&&target.floatApplications>0,"Valid focused chains bind and float their target");
            hit.tick(39);check(target.strikes==0,"Binding does not deal its delayed strike early");
            hit.tick(3);check(target.strikes==1&&hit.cast.isFading(),"A completed bind deals exactly one delayed strike");
        }

        Fixture area=new Fixture();LivingEntity inside=area.target(70,false),outside=area.target(77,true);area.begin(Mode.AREA,64,null);
        area.tick(10);area.cast.launch();area.tick(2);
        check(inside.boundApplications>0&&outside.boundApplications==0,"Area binds only eligible targets inside its marked12block radius");
        area.tick(42);check(inside.strikes==1&&outside.strikes==0,"Area's edge and delayed damage remain consistent");

        Fixture travel=new Fixture();LivingEntity dodge=travel.target(30,true);travel.begin(Mode.SINGLE,30,dodge);travel.tick(5);travel.cast.launch();
        dodge.setPos(34,0,0);travel.tick(2);check(travel.cast.isFading()&&dodge.boundApplications==0,"Moving out of a launched chain still avoids its bind");

        Fixture counter=new Fixture();LivingEntity attacker=counter.target(20,false);counter.begin(Mode.COUNTER,20,attacker);counter.tick(5);counter.cast.launch();counter.tick(2);
        check(attacker.boundApplications>0,"Counter retains mob-attacker binding");
        Fixture absolute=new Fixture();LivingEntity enemy=absolute.target(20,false);absolute.begin(Mode.ABSOLUTE,0,null);absolute.tick(10);absolute.cast.launch();absolute.tick(2);
        check(enemy.boundApplications>0,"Absolute Bind retains nearby mob targeting");

        Fixture client=new Fixture();client.world.isClientSide=true;client.begin(Mode.SINGLE,64,null);client.tick(10);client.cast.launch();
        check(client.cast.isCharging(),"Clients cannot start authoritative launch or bind logic");
        System.out.println("Enkidu production cast: "+checks+" miss, range, binding and cleanup checks passed.");
    }
}
