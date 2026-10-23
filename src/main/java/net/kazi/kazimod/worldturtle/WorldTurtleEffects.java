package net.kazi.kazimod.worldturtle;

import net.kazi.kazimod.abilities.KameRework.DivineShieldAbility;
import net.kazi.kazimod.abilities.KameRework.WorldShakingSpinAbility;
import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.preserved.sahur.HeavenlyShield;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class WorldTurtleEffects {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<WorldTurtleEffectEntity>> EFFECT=TYPES.register("world_turtle_effect",()->
            EntityType.Builder.<WorldTurtleEffectEntity>of(WorldTurtleEffectEntity::new,EntityClassification.MISC)
                    .sized(1,1).clientTrackingRange(16).updateInterval(2).noSave().build("world_turtle_effect"));
    public static final RegistryObject<EntityType<WorldTurtlePartEntity>> PART=TYPES.register("world_turtle_part",()->
            EntityType.Builder.<WorldTurtlePartEntity>of(WorldTurtlePartEntity::new,EntityClassification.MISC)
                    .sized(1,1).clientTrackingRange(16).updateInterval(20).noSave().build("world_turtle_part"));
    public static final RegistryObject<EntityType<SupernovaEntity>> SUPERNOVA=TYPES.register("world_turtle_supernova",()->
            EntityType.Builder.<SupernovaEntity>of(SupernovaEntity::new,EntityClassification.MISC)
                    .sized(1,1).clientTrackingRange(16).updateInterval(2).noSave().build("world_turtle_supernova"));
    private static boolean reflecting;
    public static void init(IEventBus bus){
        TYPES.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e)->e.put(PART.get(),LivingEntity.createLivingAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(WorldTurtleHitboxes::tick);
        MinecraftForge.EVENT_BUS.addListener(WorldTurtlePlatforms::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,WorldTurtleImmunity::applicable);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,WorldTurtleEffects::attack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,WorldTurtleEffects::damage);
        MinecraftForge.EVENT_BUS.addListener(WorldTurtleEffects::impact);
    }
    public static void shield(LivingEntity e){if(!e.level.isClientSide)WorldTurtleEffectEntity.shield(e);}
    private static void damage(LivingDamageEvent e){
        if(WorldShakingSpinAbility.active(e.getEntityLiving())){e.setAmount(0);return;}
        if(!e.getEntityLiving().level.isClientSide&&KaziMorphs.WORLD_TURTLE.get().isActive(e.getEntityLiving()))e.setAmount(e.getAmount()*.5F);
    }
    private static void attack(LivingAttackEvent e){
        LivingEntity user=e.getEntityLiving();if(user.level.isClientSide)return;
        if(WorldShakingSpinAbility.active(user)){e.setCanceled(true);return;}
        if(!DivineShieldAbility.active(user))return;
        e.setCanceled(true);
        Entity direct=e.getSource().getDirectEntity(),attacker=e.getSource().getEntity();
        if(direct instanceof ProjectileEntity){HeavenlyShield.reflect(user,(ProjectileEntity)direct,false);return;}
        if(reflecting||attacker==user||!(attacker instanceof LivingEntity)||e.getAmount()<=0)return;
        reflecting=true;
        try{attacker.hurt(new EntityDamageSource("divine_reflection",user),e.getAmount()*.1F);}finally{reflecting=false;}
    }
    private static void impact(ProjectileImpactEvent e){
        if(e.getEntity().level.isClientSide||!(e.getEntity() instanceof ProjectileEntity)||!(e.getRayTraceResult() instanceof EntityRayTraceResult))return;
        Entity target=((EntityRayTraceResult)e.getRayTraceResult()).getEntity();
        if(target instanceof WorldTurtlePartEntity){
            target=((WorldTurtlePartEntity)target).owner();
            if(target==((ProjectileEntity)e.getEntity()).getOwner()){e.setCanceled(true);return;}
        }
        if(target instanceof LivingEntity&&DivineShieldAbility.active((LivingEntity)target)){
            HeavenlyShield.reflect((LivingEntity)target,(ProjectileEntity)e.getEntity(),false);e.setCanceled(true);
        }
    }
    public static void deflectNearby(LivingEntity owner){
        if(owner.level.isClientSide)return;
        Vector3d center=owner.position().add(0,9,0);AxisAlignedBB box=new AxisAlignedBB(center,center).inflate(38,29,38);
        int handled=0;
        for(ProjectileEntity p:owner.level.getEntitiesOfClass(ProjectileEntity.class,box)){
            if(p.getOwner()==owner||!p.isAlive())continue;
            Vector3d start=p.position().subtract(center),v=p.getDeltaMovement();
            if(intersects(start.x,start.y,start.z,v.x,v.y,v.z))HeavenlyShield.reflect(owner,p,false);
            if(++handled>=128)break;
        }
        for(WorldTurtleEffectEntity bomb:owner.level.getEntitiesOfClass(WorldTurtleEffectEntity.class,box))if(bomb.kind()==0&&bomb.owner()!=owner){
            Vector3d start=bomb.position().subtract(center),v=bomb.getDeltaMovement();
            if(intersects(start.x,start.y,start.z,v.x,v.y,v.z))bomb.reflect(owner);
        }
    }
    public static boolean intersects(double x,double y,double z,double dx,double dy,double dz){
        return WorldTurtleMesh.intersectsShield(x,y,z,dx,dy,dz);
    }
}
