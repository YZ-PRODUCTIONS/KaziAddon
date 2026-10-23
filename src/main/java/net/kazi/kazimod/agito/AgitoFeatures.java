package net.kazi.kazimod.agito;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.mahoraga.MahoragaAbility;
import net.kazi.kazimod.mahoraga.MahoragaFeatures;
import net.kazi.kazimod.shikigami.TenShadowsTaming;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class AgitoFeatures {
    public static final String SUMMON_TAG="kazimodAgitoSummon",RESERVE_TAG="kazimodAgitoReserve",DEFEATED_TAG="kazimodAgitoDefeated";
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    private static final DeferredRegister<Effect> EFFECTS=DeferredRegister.create(ForgeRegistries.POTIONS,"kazimod");
    public static final RegistryObject<Effect> EMBRACE=EFFECTS.register("agito_embrace",AgitoEmbraceEffect::new);
    public static final RegistryObject<EntityType<AgitoEntity>> AGITO=TYPES.register("agito",()->
            EntityType.Builder.<AgitoEntity>of(AgitoEntity::new,EntityClassification.CREATURE)
                    .sized(1.9F,3.9F).clientTrackingRange(12).updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true).noSave().build("agito"));

    public static void init(IEventBus bus){
        TYPES.register(bus);EFFECTS.register(bus);KaziRegistry.registerAbility(AgitoAbility.INSTANCE);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e)->e.put(AGITO.get(),AgitoEntity.createAttributes().build()));
        bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{
            FruitAbilityInjector.addAbilities(ModAbilities.KAGE_KAGE_NO_MI,AgitoAbility.INSTANCE);
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,AgitoFeatures::leftClick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(AbilityUseEvent.Pre e)->{
            if(e.getEntityLiving().hasEffect(EMBRACE.get()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.RightClickItem e)->{
            if(e.getPlayer().hasEffect(EMBRACE.get()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.RightClickBlock e)->{
            if(e.getPlayer().hasEffect(EMBRACE.get()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.EntityInteract e)->{
            if(e.getPlayer().hasEffect(EMBRACE.get()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.LeftClickBlock e)->{
            if(e.getPlayer().hasEffect(EMBRACE.get()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,AgitoFeatures::attacked);
        MinecraftForge.EVENT_BUS.addListener(AgitoFeatures::playerTick);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->cleanup(e.getPlayer(),e.getPlayer().level instanceof ServerWorld?(ServerWorld)e.getPlayer().level:null));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->{
            if(e.getPlayer() instanceof ServerPlayerEntity)cleanup(e.getPlayer(),((ServerPlayerEntity)e.getPlayer()).getServer().getLevel(e.getFrom()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AgitoClient.init(bus));
    }

    private static void leftClick(AttackEntityEvent e){
        PlayerEntity p=e.getPlayer();
        if(e.getTarget() instanceof AgitoEntity && !((AgitoEntity)e.getTarget()).isTaming() && p.getUUID().equals(((AgitoEntity)e.getTarget()).ownerId())){
            // Cancel damage even while Agito is still emerging or already dismissing.
            e.setCanceled(true);
            if(!p.level.isClientSide)((AgitoEntity)e.getTarget()).toggleHealing(p);
        }else if(p.hasEffect(EMBRACE.get()))e.setCanceled(true);
    }

    private static void attacked(LivingAttackEvent e){
        LivingEntity victim=e.getEntityLiving();
        if(victim.level.isClientSide || e.getAmount()<=0)return;
        AgitoEntity held=find(victim);
        if(held!=null)held.stopHealing();
        if(victim instanceof AgitoEntity)((AgitoEntity)victim).stopHealing();
        if(!(e.getSource().getEntity() instanceof LivingEntity))return;
        LivingEntity attacker=(LivingEntity)e.getSource().getEntity();
        for(AgitoEntity agito:victim.level.getEntitiesOfClass(AgitoEntity.class,victim.getBoundingBox().inflate(50))){
            if(victim==agito || MahoragaFeatures.allied(agito.owner(),victim))agito.defend(attacker);
        }
    }

    private static void playerTick(TickEvent.PlayerTickEvent e){
        PlayerEntity p=e.player;
        if(e.phase!=TickEvent.Phase.END || p.level.isClientSide)return;
        AgitoEntity entity=find(p);
        if(p.hasEffect(EMBRACE.get())){
            if(entity==null || !entity.isHealing() || !p.isAlive())p.removeEffect(EMBRACE.get());
            else entity.holdOwner(p);
        }
        if(p.tickCount%20!=0)return;
        if(entity!=null)entity.preserveFor(p);else recoverReserve(p);
        IAbilityData data=AbilityDataCapability.get(p);
        AgitoAbility equipped=data.getEquippedAbility(AgitoAbility.INSTANCE);
        if(equipped!=null){
            CompoundNBT saved=p.getPersistentData().getCompound(RESERVE_TAG);
            float health=entity!=null?entity.getHealth():saved.contains("Health")?saved.getFloat("Health"):0;
            float max=entity!=null?entity.getMaxHealth():saved.contains("MaxHealth")?saved.getFloat("MaxHealth"):500;
            equipped.updateHud(p,health,max,entity!=null && !entity.isDismissing());
        }
        if(MahoragaAbility.awakened(p)){
            if(!data.hasUnlockedAbility(AgitoAbility.INSTANCE)){
                data.addUnlockedAbility(AgitoAbility.INSTANCE,AbilityUnlock.PROGRESSION);
                WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
            }
        }else{
            dismiss(p);
            if(data.hasUnlockedAbility(AgitoAbility.INSTANCE)){
                data.removeEquippedAbility(AgitoAbility.INSTANCE);data.removeUnlockedAbility(AgitoAbility.INSTANCE);
                WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
            }
        }
    }

    private static void cleanup(PlayerEntity p,ServerWorld world){
        p.removeEffect(EMBRACE.get());
        if(world!=null && p.getPersistentData().hasUUID(SUMMON_TAG)){
            Entity entity=world.getEntity(p.getPersistentData().getUUID(SUMMON_TAG));
            if(entity instanceof AgitoEntity){
                ((AgitoEntity)entity).stopHealing();
                if(entity.isAlive())((AgitoEntity)entity).preserveFor(p);
                entity.remove();
            }
        }
        p.getPersistentData().remove(SUMMON_TAG);
    }

    private static void recoverReserve(LivingEntity owner){
        if(!owner.getPersistentData().contains(RESERVE_TAG,10))return;
        CompoundNBT saved=owner.getPersistentData().getCompound(RESERVE_TAG);
        long now=owner.level.getGameTime(),last=saved.getLong("LastHeal");
        if(last>now){saved.putLong("LastHeal",now);return;}
        long seconds=(now-last)/20;
        if(seconds>0){
            float max=Math.max(1,saved.getFloat("MaxHealth"));
            saved.putFloat("Health",Math.min(max,saved.getFloat("Health")+max*AgitoEntity.SELF_HEAL*seconds));
            saved.putLong("LastHeal",last+seconds*20);
        }
    }
    public static AgitoEntity find(LivingEntity owner){
        if(!(owner.level instanceof ServerWorld) || !owner.getPersistentData().hasUUID(SUMMON_TAG))return null;
        Entity e=((ServerWorld)owner.level).getEntity(owner.getPersistentData().getUUID(SUMMON_TAG));
        return e instanceof AgitoEntity && e.isAlive() && owner.getUUID().equals(((AgitoEntity)e).ownerId())?(AgitoEntity)e:null;
    }
    public static void dismiss(LivingEntity owner){
        AgitoEntity e=find(owner);if(e!=null)e.beginDismiss();else owner.getPersistentData().remove(SUMMON_TAG);
        owner.removeEffect(EMBRACE.get());
    }
    public static AgitoEntity summon(LivingEntity owner){
        AgitoEntity existing=find(owner);if(existing!=null)return existing.isDismissing()?null:existing;
        if(!TenShadowsTaming.canSummon(owner,"agito"))return null;
        AgitoEntity e=new AgitoEntity(AGITO.get(),owner.level);
        Vector3d forward=owner.getLookAngle().multiply(1,0,1).normalize();
        for(int attempt=0;attempt<16;attempt++){
            double angle=attempt*Math.PI/4;
            Vector3d offset=attempt==0?forward.scale(4):new Vector3d(Math.cos(angle),0,Math.sin(angle)).scale(4+attempt/8.0);
            BlockPos start=new BlockPos(owner.position().add(offset)).above(6);
            for(int down=0;down<18;down++){
                BlockPos ground=start.below(down);
                if(!owner.level.hasChunkAt(ground) || !owner.level.getBlockState(ground).isFaceSturdy(owner.level,ground,Direction.UP))continue;
                e.setPos(ground.getX()+.5,ground.getY()+1.01,ground.getZ()+.5);
                if(!owner.level.noCollision(e))continue;
                recoverReserve(owner);e.setOwner(owner);TenShadowsTaming.prepare(e);e.restoreFor(owner);
                e.yRot=e.yBodyRot=owner.yRot;e.beginSummon();
                if(!owner.level.addFreshEntity(e))return null;
                owner.getPersistentData().putUUID(SUMMON_TAG,e.getUUID());owner.getPersistentData().remove(DEFEATED_TAG);
                TenShadowsTaming.started(e);
                return e;
            }
        }
        if(owner instanceof PlayerEntity)((PlayerEntity)owner).displayClientMessage(new TranslationTextComponent("message.kazimod.agito.no_room"),true);
        return null;
    }
    private AgitoFeatures(){}
}
