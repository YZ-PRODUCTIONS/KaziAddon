package net.kazi.kazimod.kake;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class KakeVisuals {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<KakeVfxEntity>> EFFECT=TYPES.register("kake_vfx",()->EntityType.Builder.<KakeVfxEntity>of(KakeVfxEntity::new,EntityClassification.MISC).sized(1,1).clientTrackingRange(12).updateInterval(2).noSave().build("kake_vfx"));
    private static final Map<World,long[]> BUDGET=new WeakHashMap<>();
    public static void init(IEventBus bus){TYPES.register(bus);}
    public static KakeVfxEntity aura(LivingEntity owner,int kind,int life,float radius){return KakeVfxEntity.spawn(owner,kind,owner.position(),life,radius,0,true);}
    public static void cast(LivingEntity owner,int roll){KakeVfxEntity.spawn(owner,KakeVfxEntity.CAST,owner.position(),18,2,roll,false);}
    public static void impact(Entity projectile,LivingEntity owner,int variant){
        if(owner==null||projectile.level.isClientSide)return;
        long[] budget=BUDGET.computeIfAbsent(projectile.level,k->new long[]{-1,0});long tick=projectile.level.getGameTime();
        if(budget[0]!=tick){budget[0]=tick;budget[1]=0;}if(budget[1]++>=24)return;
        KakeVfxEntity.spawn(owner,KakeVfxEntity.IMPACT,projectile.position(),16,4,variant,false);
    }
    public static void explode(Entity projectile,float strength){
        World world=projectile.level;if(world.isClientSide)return;
        Explosion explosion=new Explosion(world,projectile,projectile.getX(),projectile.getY(),projectile.getZ(),strength,false,Explosion.Mode.NONE);
        if(ForgeEventFactory.onExplosionStart(world,explosion))return;
        explosion.explode();explosion.finalizeExplosion(false);
        // Preserve the native explosion's physics without its vanilla particle packet.
        explosion.getHitPlayers().keySet().forEach(player->player.hurtMarked=true);
        world.playSound(null,projectile.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundCategory.PLAYERS,2,1.2F);
    }
}
