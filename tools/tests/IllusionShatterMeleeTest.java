import java.util.function.Consumer;
import net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingAttackEvent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** Exercise the actual passive rather than a duplicate implementation of its melee rules. */
public final class IllusionShatterMeleeTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for(String held:new String[]{"","minecraft:diamond_sword","minecraft:torch"})for(int route=0;route<3;route++) {
            Fixture f=new Fixture();f.user.held=new ItemStack(held);f.hit(route);
            require(f.target.suppressed==1&&f.target.duration==80,"Every ordinary melee hook suppresses with an empty hand, weapon or other held item");
            require(f.user.sounds==1&&f.user.particles==1,"Each successful contact emits the intended single sound and particle cue");
            require(f.target.health==100&&f.target.damageCalls==0,"The passive neither replaces nor adds to ordinary melee damage");
            f.target.hurt(f.source,12);
            require(f.target.health==88&&f.target.damageCalls==1,"Ordinary melee damage remains free to execute after suppression");
        }
        gates();classification();duplicates();
        for(String name:new String[]{"onMeleeContact","onNativeMeleeHit","onMeleeHit"}) {
            java.lang.reflect.Method found=null;
            for(java.lang.reflect.Method method:IllusionShatterAbility.class.getDeclaredMethods())if(method.getName().equals(name))found=method;
            require(found!=null,"Every ordinary melee hook is present");
            net.minecraftforge.eventbus.api.SubscribeEvent event=found.getAnnotation(net.minecraftforge.eventbus.api.SubscribeEvent.class);
            require(event!=null&&event.priority()==net.minecraftforge.eventbus.api.EventPriority.LOWEST&&!event.receiveCanceled(),
                    "Canceled attacks never reach the passive and hit fallbacks run after ordinary attack gates");
        }
        System.out.println("Imagine Breaker melee behavior: "+checks+" checks passed.");
    }

    private static void gates() {
        blocked("client",f->f.world.isClientSide=true);
        blocked("dead user",f->f.user.alive=false);
        blocked("spectator",f->f.user.spectator=true);
        blocked("missing awakening",f->f.user.unlock=false);
        blocked("missing capability",f->AbilityDataCapability.DATA.remove(f.user));
        blocked("missing passive",f->AbilityDataCapability.DATA.put(f.user,new IAbilityData(null)));
        blocked("below thirty percent",f->f.user.health=29.99F);
        blocked("current maximum health",f->{f.user.health=30;f.user.maxHealth=200;});
        blocked("canUse restriction",f->f.passive.useAllowed=false);
        blocked("disabled passive",f->f.passive.disableComponent.disabled=true);
        blocked("paused passive",f->f.passive.paused=true);
        for(String effect:new String[]{"IMAGINE_BROKEN","LOST_WORKS_FRUIT_LOCK","NO_HANDS","GUARDING"})
            blocked(effect,f->f.user.effects.add(effect));
        blocked("dead target",f->f.target.alive=false);
        blocked("allied target",f->f.target.enemy=false);
        blocked("different dimension",f->f.target.level=new World());
        blocked("protected caster area",f->f.world.denied.add(f.user.id));
        blocked("protected target area",f->f.world.denied.add(f.target.id));
        blocked("PvP team restriction",f->f.user.canHarm=false);
        blocked("server PvP disabled",f->f.user.server.pvp=false);
        blocked("no server",f->f.user.server=null);
        blocked("blocked line of sight",f->f.world.wall=true);
        blocked("out of reach",f->f.target.z=4);
        blocked("target invulnerability",f->f.target.invulnerable=true);
        blocked("target dodge",f->f.target.dodging=true);
        for(int route=0;route<3;route++) {
            Fixture f=new Fixture();f.user.health=30;f.hit(route);
            require(f.target.suppressed==1,"Exactly thirty percent remains active with a held item");
            Fixture vfx=new Fixture();vfx.user.trace=new EntityRayTraceResult(new Entity(vfx.world));vfx.target.unlock=false;vfx.hit(route);
            require(vfx.target.suppressed==1&&vfx.user.sounds==1,
                    "Unrelated VFX in an entity trace cannot swallow the actual target's proc or sound, even for a fruitless target");
            Fixture boundary=new Fixture();boundary.target.z=3.8;boundary.hit(route);
            require(boundary.target.suppressed==1,"Contact with a bounding box exactly at reach is accepted");
        }
    }

    private static void blocked(String reason,Consumer<Fixture> configure) {
        for(int route=0;route<3;route++) {
            Fixture f=new Fixture();configure.accept(f);f.hit(route);
            require(f.target.suppressed==0&&f.user.sounds==0&&f.user.particles==0,
                    "Every held-item contact route preserves the "+reason+" gate");
            require(f.target.damageCalls==0&&f.target.health==100,"A rejected passive proc leaves ordinary damage untouched");
        }
    }

    private static void classification() {
        java.util.List<Consumer<Fixture>> rejected=java.util.Arrays.asList(
            f->f.source=null,
            f->f.source.projectile=true,
            f->f.source.explosion=true,
            f->f.source.magic=true,
            f->f.source.msgId="thorns",
            f->f.user.sweeping=true,
            f->f.source=new AbilityDamageSource(f.user,f.user),
            f->f.source.direct=new Entity(f.world),
            f->{LivingEntity mob=new LivingEntity(f.world);f.source=new DamageSource(mob,mob);},
            f->{f.source.owner=null;f.source.direct=null;}
        );
        for(Consumer<Fixture> configure:rejected)for(int route=1;route<3;route++) {
            Fixture f=new Fixture();configure.accept(f);f.hit(route);
            require(f.target.suppressed==0&&f.user.sounds==0,"Hit fallbacks reject abilities, projectiles, explosions, magic and indirect/non-player damage");
        }
        Fixture fruitUser=new Fixture();fruitUser.user.unlock=true;fruitUser.hit(1);
        require(fruitUser.target.suppressed==1,"An ordinary direct melee hit remains valid for a fruit user");
        Fixture nonLiving=new Fixture();IllusionShatterAbility.onMeleeContact(new AttackEntityEvent(nonLiving.user,new Entity(nonLiving.world)));
        require(nonLiving.target.suppressed==0,"Clicking a nonliving entity cannot produce suppression");
    }

    private static void duplicates() {
        Fixture f=new Fixture();f.hit(0);f.hit(1);f.hit(2);f.hit(0);
        require(f.target.suppressed==1&&f.user.sounds==1&&f.user.particles==1,"Click, native and Forge callbacks for the same contact only proc once");
        PlayerEntity second=new PlayerEntity(f.world);f.user.trace=new EntityRayTraceResult(second);
        IllusionShatterAbility.onNativeMeleeHit(new WyLivingAttackEvent(second,f.source));
        f.user.trace=new EntityRayTraceResult(f.target);f.hit(2);
        require(f.target.suppressed==1&&second.suppressed==1&&f.user.sounds==2,"Interleaved A, B, A contacts allow different targets without replaying A");
        f.world.gameTime++;f.hit(1);f.hit(2);
        require(f.target.suppressed==2&&f.user.sounds==3,"A new tick allows another contact without imposing a proc cooldown");
        PlayerEntity other=new PlayerEntity(f.world);other.trace=new EntityRayTraceResult(f.target);
        AbilityDataCapability.DATA.put(other,new IAbilityData(new IllusionShatterAbility(IllusionShatterAbility.INSTANCE)));
        IllusionShatterAbility.onMeleeHit(new LivingAttackEvent(f.target,new DamageSource(other,other)));
        require(f.target.suppressed==3&&other.sounds==1,"Two attackers do not share their successful-contact deduplication");
        Fixture retry=new Fixture();retry.target.dodging=true;retry.hit(0);retry.target.dodging=false;retry.hit(1);
        require(retry.target.suppressed==1,"A rejected first callback does not consume a later successful contact");
    }

    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Fixture {
        final World world=new World();final PlayerEntity user=new PlayerEntity(world),target=new PlayerEntity(world);
        final IllusionShatterAbility passive=new IllusionShatterAbility(IllusionShatterAbility.INSTANCE);
        DamageSource source=new DamageSource(user,user);
        Fixture(){world.gameTime=10;target.z=2;user.held=new ItemStack("minecraft:diamond_sword");user.trace=new EntityRayTraceResult(target);AbilityDataCapability.DATA.put(user,new IAbilityData(passive));}
        void hit(int route) {
            if(route==0){AttackEntityEvent event=new AttackEntityEvent(user,target);IllusionShatterAbility.onMeleeContact(event);require(!event.canceled,"Click contact is never canceled by outgoing suppression");}
            else if(route==1){WyLivingAttackEvent event=new WyLivingAttackEvent(target,source);IllusionShatterAbility.onNativeMeleeHit(event);require(!event.canceled&&event.amount==12,"Native hit amount and cancellation remain untouched");}
            else {LivingAttackEvent event=new LivingAttackEvent(target,source);IllusionShatterAbility.onMeleeHit(event);require(!event.canceled&&event.amount==12,"Forge hit amount and cancellation remain untouched");}
        }
    }
}
