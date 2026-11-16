import net.kazi.kazimod.abilities.SupaRework.TracedFinalExecution;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class TracedFinalExecutionTest {
    private static int checks;
    private static void check(boolean result,String reason){checks++;if(!result)throw new AssertionError(reason);}
    private static boolean near(double actual,double expected){return Math.abs(actual-expected)<1e-4;}
    private static final class Loan implements IAbility {
        final CooldownComponent cooldown=new CooldownComponent();
        @SuppressWarnings("unchecked") public <T>java.util.Optional<T> getComponent(AbilityKey<T> key){return java.util.Optional.of((T)cooldown);}
    }
    private static final class Fixture {
        final ServerWorld world=new ServerWorld();final LivingEntity user=new LivingEntity(world);
        final Loan loan=new Loan();final DealDamageComponent damage=new DealDamageComponent();final RangeComponent range=new RangeComponent();
        final HitTrackerComponent hits=new HitTrackerComponent();final AnimationComponent animation=new AnimationComponent();
        void finish(){TracedFinalExecution.finish(user,loan,damage,range,hits,animation);}
        LivingEntity target(double x,float health){LivingEntity target=new LivingEntity(world);target.x=x;target.health=health;range.targets.add(target);return target;}
        ProjectileEntity projectile(double x,LivingEntity owner){ProjectileEntity projectile=new ProjectileEntity(world);projectile.x=x;projectile.y=1.5;projectile.owner=owner;world.projectiles.add(projectile);return projectile;}
        void completeOnce(){check(user.teleports==1&&user.unrides==1,"A charge completes one dash even on a miss");check(world.sounds==1&&world.chunks.broadcasts==1,"A completed charge emits one swoosh and one swing");check(animation.stops==1,"The charging animation always ends once");}
    }
    public static void main(String[] args) {
        Fixture miss=new Fixture();miss.finish();miss.completeOnce();
        check(near(miss.user.x,50),"A missed strike still completes the full native fifty-block dash");
        check(miss.damage.amounts.isEmpty()&&miss.loan.cooldown.start==600,"Misses use the native600tick cooldown without phantom damage");
        check(miss.world.collisionChecks<=200,"Collision work is bounded for the fifty-block dash");

        Fixture hits=new Fixture();LivingEntity first=hits.target(10,100),second=hits.target(15,50);hits.finish();hits.completeOnce();
        check(hits.damage.amounts.equals(java.util.Arrays.asList(30f,80f)),"Each hit retains native base and missing-health damage");
        check(first.effects.size()==1&&first.effects.get(0).duration==200&&second.effects.get(0).duration==200,"Each hit keeps ten seconds of heavy bleeding");
        check(hits.user.particles==2,"Successful targets retain the execution impact particles");
        check(hits.loan.cooldown.start==600&&hits.loan.cooldown.starts==1,"The first native hit cooldown wins over later hit and fallback starts");
        Fixture injured=new Fixture();injured.target(10,50);injured.finish();
        check(injured.loan.cooldown.start==1100,"An injured target produces its native damage-dependent cooldown for Trace to halve");
        Fixture tracked=new Fixture();LivingEntity already=tracked.target(10,100);tracked.hits.hit.add(already);tracked.finish();
        check(tracked.damage.amounts.isEmpty()&&already.effects.isEmpty(),"The native hit tracker prevents duplicate damage and bleeding");

        Fixture friendly=new Fixture();friendly.user.team=1;LivingEntity ally=new LivingEntity(friendly.world);ally.team=1;
        LivingEntity factionAlly=new LivingEntity(friendly.world);factionAlly.friendlyFaction=true;
        friendly.projectile(1,friendly.user);friendly.projectile(2,ally);friendly.projectile(3,factionAlly);
        AbilityProjectileEntity projected=new AbilityProjectileEntity(friendly.world);projected.x=.1;projected.y=1.5;projected.thrower=friendly.user;friendly.world.projectiles.add(projected);
        friendly.target(20,100);friendly.finish();
        check(near(friendly.user.x,50)&&friendly.damage.amounts.size()==1,"Caster-owned, allied and friendly-faction projectiles cannot truncate the trace");
        Fixture hostile=new Fixture();hostile.projectile(10,new LivingEntity(hostile.world));hostile.target(30,100);hostile.finish();hostile.completeOnce();
        check(hostile.user.x>9&&hostile.user.x<10&&hostile.damage.amounts.isEmpty(),"A hostile projectile still stops the dash and prevents distant hits");
        Fixture ownerless=new Fixture();ownerless.projectile(10,null);ownerless.finish();
        check(ownerless.user.x<10,"Unknown-owner projectiles retain the native blocking behavior");
        Fixture offAxis=new Fixture();offAxis.projectile(1,new LivingEntity(offAxis.world)).z=20;offAxis.finish();
        check(near(offAxis.user.x,50),"Projectiles away from the dash line do not shorten it");
        Fixture dead=new Fixture();dead.projectile(1,new LivingEntity(dead.world)).alive=false;dead.finish();
        check(near(dead.user.x,50),"Removed projectiles do not shorten the dash");

        Fixture wall=new Fixture();wall.world.rayWall=12;wall.finish();
        check(wall.user.x>11&&wall.user.x<12,"The block ray stops the dash before a solid wall");
        Fixture body=new Fixture();body.world.bodyWall=7;body.finish();
        check(body.user.x>6&&body.user.x+body.user.width/2<=7,"Whole-body collision stops before low or side obstacles missed by the eye ray");
        Fixture marble=new Fixture();marble.world.marbleWall=20;marble.target(19,100);LivingEntity outside=marble.target(21,100);marble.finish();
        check(near(marble.user.x,20)&&near(marble.range.distance,20),"The dash and damage line both use the marble-clamped destination");
        check(marble.damage.targets.size()==1&&!marble.damage.targets.contains(outside),"Attack width cannot reach a target outside the marble shell");
        Fixture blocked=new Fixture();blocked.world.bodyWall=.3;blocked.finish();blocked.completeOnce();
        check(near(blocked.user.x,0)&&blocked.damage.amounts.isEmpty(),"A blocked route finishes without tunneling or damage beyond it");

        Fixture client=new Fixture();client.world.isClientSide=true;client.target(10,100);client.finish();
        check(client.user.teleports==0&&client.damage.amounts.isEmpty()&&client.world.sounds==0&&client.loan.cooldown.starts==0,"Client callbacks cannot repeat server movement, damage, sound, or cooldown");
        check(client.animation.stops==1,"The native client charge animation still cleans up");
        System.out.println("Traced Final Execution: "+checks+" checks passed.");
    }
}
