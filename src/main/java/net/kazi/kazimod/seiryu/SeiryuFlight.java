package net.kazi.kazimod.seiryu;

import java.util.*;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.world.ExplosionEvent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class SeiryuFlight {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<SeiryuPartEntity>> PART=TYPES.register("seiryu_body_part",()->
            EntityType.Builder.<SeiryuPartEntity>of(SeiryuPartEntity::new,EntityClassification.MISC)
                    .sized(1,1).clientTrackingRange(20).updateInterval(20).noSave().build("seiryu_body_part"));
    private static final Map<LivingEntity,State> STATES=new WeakHashMap<>();
    static final class State {
        final SeiryuSpine spine;
        final double scale;
        State(){this(1);}
        State(double scale){this.scale=scale;spine=new SeiryuSpine(scale);}
        final SeiryuPartEntity[] parts=new SeiryuPartEntity[SeiryuSpine.SEGMENTS];
        Vector3d[] points;
        long damageTick=Long.MIN_VALUE;
        final Set<List<Object>> hits=new HashSet<>();
        boolean acceptHit(long tick,DamageSource source) {
            if(damageTick!=tick){damageTick=tick;hits.clear();}
            // Damage falloff can differ between sections. It must not create a new attack.
            Object ability=null;
            if(source instanceof AbilityDamageSource && ((AbilityDamageSource)source).getAbilitySource()!=null) {
                xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core=((AbilityDamageSource)source).getAbilitySource();
                // Replaced Cart/MMNM cores may be unregistered and do not compare equal to themselves.
                ability=core.getRegistryName()!=null?core.getRegistryName().toString():core.getUnlocalizedName();
            }
            return hits.add(Arrays.asList(source.getDirectEntity(),source.getEntity(),source.getMsgId(),ability));
        }
    }
    public static boolean active(LivingEntity owner) {
        if(owner==null || !owner.isAlive() || owner.isSpectator())return false;
        MorphInfo morph=morph(owner);
        return morph==CartMorphs.SEIRYU_FLY.get() || morph==CartMorphs.KAEN_DAIKO.get();
    }
    private static MorphInfo morph(LivingEntity owner) {
        return DevilFruitCapability.get(owner).getCurrentMorph().orElseGet(()->MorphHelper.getZoanInfo(owner));
    }
    public static double scale(LivingEntity owner) {
        // Both Cart forms use "seiryu_fly_point"; compare the actual morph, never its legacy name.
        return owner!=null && morph(owner)==CartMorphs.KAEN_DAIKO.get()?13.0/4.0:1;
    }
    public static void init(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(PART.get(),LivingEntity.createLivingAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(SeiryuFlight::tick);
        MinecraftForge.EVENT_BUS.addListener(SeiryuFlight::tracking);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,SeiryuFlight::attack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,SeiryuFlight::explosion);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.ProjectileImpactEvent e)->{
            if(!(e.getRayTraceResult() instanceof net.minecraft.util.math.EntityRayTraceResult)
                    || !(e.getEntity() instanceof net.minecraft.entity.projectile.ProjectileEntity))return;
            Entity target=((net.minecraft.util.math.EntityRayTraceResult)e.getRayTraceResult()).getEntity();
            if(target instanceof SeiryuPartEntity&&((SeiryuPartEntity)target).owner()==((net.minecraft.entity.projectile.ProjectileEntity)e.getEntity()).getOwner())e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->clear(e.getPlayer()));
    }
    private static void clear(LivingEntity owner) {
        State state=STATES.remove(owner);
        if(state!=null) for(SeiryuPartEntity part:state.parts) if(part!=null) part.remove();
    }
    private static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||e.player.level.isClientSide)return;
        PlayerEntity owner=e.player;
        if(!active(owner)) {clear(owner);return;}
        double scale=scale(owner);
        State state=STATES.get(owner);
        boolean changed=state==null || state.scale!=scale;
        if(changed){clear(owner);state=new State(scale);STATES.put(owner,state);}
        state.points=state.spine.update(owner.position(),owner.getLookAngle());
        for(int i=0;i<state.parts.length;i++) {
            SeiryuPartEntity part=state.parts[i];
            if(part==null||!part.isAlive()||part.level!=owner.level) {
                if(part!=null)part.remove();
                part=new SeiryuPartEntity(PART.get(),owner.level);
                part.bind(owner,i);state.parts[i]=part;
                part.updateBounds();owner.level.addFreshEntity(part);
            } else part.updateBounds();
        }
        if(changed || owner.tickCount%2==0) SeiryuSpinePacket.send(owner,state.points);
    }
    private static void tracking(PlayerEvent.StartTracking e) {
        if(!(e.getTarget() instanceof LivingEntity))return;
        LivingEntity owner=(LivingEntity)e.getTarget();State state=STATES.get(owner);
        if(state!=null && state.points!=null && active(owner) && state.scale==scale(owner))SeiryuSpinePacket.sendTo(e.getPlayer(),owner,state.points);
    }
    public static Vector3d[] points(LivingEntity owner) {
        State state=STATES.get(owner);return state==null || state.scale!=scale(owner)?null:state.points;
    }
    private static void attack(LivingAttackEvent e) {
        LivingEntity owner=e.getEntityLiving();
        if(owner.level.isClientSide || !active(owner))return;
        State state=STATES.get(owner);if(state==null)return;
        long tick=owner.level.getGameTime();
        // An AOE/projectile touching several sections is still one hit on the dragon.
        if(!state.acceptHit(tick,e.getSource()))e.setCanceled(true);
    }
    private static void explosion(ExplosionEvent.Detonate e) {
        // Keep the closest physical section for falloff/exposure, but only one per dragon.
        SeiryuTargets.collapseExplosion(e.getAffectedEntities(),e.getExplosion().getPosition());
    }
}
