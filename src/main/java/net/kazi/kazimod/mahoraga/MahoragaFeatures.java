package net.kazi.kazimod.mahoraga;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.crew.Crew;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.world.ExtendedWorldData;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class MahoragaFeatures {
    public static final String SUMMON_TAG = "kazimodMahoragaSummon";
    public static final String RESERVE_TAG = "kazimodMahoragaReserve";
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    private static final DeferredRegister<net.minecraft.util.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"kazimod");
    public static final RegistryObject<net.minecraft.util.SoundEvent> WHEEL_SOUND = SOUNDS.register("mahoraga_wheel",()->new net.minecraft.util.SoundEvent(new net.minecraft.util.ResourceLocation("kazimod","mahoraga_wheel")));
    public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA = TYPES.register("mahoraga", () ->
            EntityType.Builder.<MahoragaEntity>of(MahoragaEntity::new, EntityClassification.CREATURE)
                    .sized(2.0F, 4.6F).clientTrackingRange(12).updateInterval(2).setShouldReceiveVelocityUpdates(true)
                    .noSave().build("mahoraga"));
    public static final RegistryObject<EntityType<MahoragaProjectile>> PROJECTILE = TYPES.register("mahoraga_projectile", () ->
            EntityType.Builder.<MahoragaProjectile>of(MahoragaProjectile::new, EntityClassification.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .noSave().build("mahoraga_projectile"));

    public static void init(IEventBus bus) {
        TYPES.register(bus);
        SOUNDS.register(bus);
        KaziRegistry.registerAbility(MahoragaAbility.INSTANCE);
        AbilityCommandGroup.create("KAGE_AWAKENING", () -> new xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore[]{MahoragaAbility.INSTANCE});
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e) -> e.put(MAHORAGA.get(), MahoragaEntity.createAttributes().build()));
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            FruitAbilityInjector.addAbilities(ModAbilities.KAGE_KAGE_NO_MI, MahoragaAbility.INSTANCE);
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, MahoragaFeatures::attacked);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDamageEvent e) -> {
            if(e.getEntityLiving() instanceof MahoragaEntity && !e.getEntityLiving().level.isClientSide)
                e.setAmount(e.getAmount()*((MahoragaEntity)e.getEntityLiving()).finalDamageMultiplier());
        });
        MinecraftForge.EVENT_BUS.addListener(MahoragaFeatures::playerTick);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            MahoragaEntity entity=find(e.getPlayer());
            if(entity!=null){entity.preserveFor(e.getPlayer());entity.remove();}
            e.getPlayer().getPersistentData().remove(SUMMON_TAG);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getPlayer() instanceof ServerPlayerEntity) {
                ServerPlayerEntity p=(ServerPlayerEntity)e.getPlayer();
                ServerWorld old=p.getServer().getLevel(e.getFrom());
                if (old!=null && p.getPersistentData().hasUUID(SUMMON_TAG)) {
                    Entity entity=old.getEntity(p.getPersistentData().getUUID(SUMMON_TAG));
                    if (entity instanceof MahoragaEntity) {
                        ((MahoragaEntity)entity).preserveFor(p);
                        entity.remove();
                    }
                }
                p.getPersistentData().remove(SUMMON_TAG);
            }
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MahoragaClient.init(bus));
    }

    private static void playerTick(TickEvent.PlayerTickEvent e) {
        PlayerEntity p=e.player;
        if (e.phase!=TickEvent.Phase.END || p.level.isClientSide || p.tickCount%20!=0) return;
        IAbilityData data=AbilityDataCapability.get(p);
        MahoragaEntity guardian=find(p);
        if(guardian!=null)guardian.preserveFor(p);
        else recoverReserve(p);
        MahoragaAbility equipped=data.getEquippedAbility(MahoragaAbility.INSTANCE);
        if(equipped!=null){
            CompoundNBT saved=p.getPersistentData().getCompound(RESERVE_TAG);
            float health=guardian!=null?guardian.getHealth():saved.contains("Health")?saved.getFloat("Health"):0;
            float max=guardian!=null?guardian.getMaxHealth():saved.contains("MaxHealth")?saved.getFloat("MaxHealth"):700;
            equipped.updateHud(p,health,max,guardian!=null && !guardian.isDismissing());
        }
        if (MahoragaAbility.awakened(p)) {
            if (!data.hasUnlockedAbility(MahoragaAbility.INSTANCE)) {
                data.addUnlockedAbility(MahoragaAbility.INSTANCE, AbilityUnlock.PROGRESSION);
                WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
            }
        } else {
            dismiss(p);
            if (data.hasUnlockedAbility(MahoragaAbility.INSTANCE)) {
                data.removeEquippedAbility(MahoragaAbility.INSTANCE);
                data.removeUnlockedAbility(MahoragaAbility.INSTANCE);
                WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
            }
        }
    }

    private static void recoverReserve(PlayerEntity player) {
        if(!player.getPersistentData().contains(RESERVE_TAG,10))return;
        CompoundNBT saved=player.getPersistentData().getCompound(RESERVE_TAG);
        long now=player.level.getGameTime(),last=saved.getLong("LastHeal");
        if(last>now){saved.putLong("LastHeal",now);last=now;}
        long seconds=Math.max(0,(now-last)/20);
        if(seconds>0){
            float max=Math.max(1,saved.getFloat("MaxHealth"));
            saved.putFloat("Health",Math.min(max,saved.getFloat("Health")+max*.01F*seconds));
            saved.putLong("LastHeal",last+seconds*20);
        }
        expireSaved(saved,"AdaptedAttacks","AttackExpires",now);
        expireSaved(saved,"AdaptedCategories","CategoryExpires",now);
        player.getPersistentData().put(RESERVE_TAG,saved);
    }

    private static void expireSaved(CompoundNBT saved,String levelsKey,String expiriesKey,long now) {
        CompoundNBT levels=saved.getCompound(levelsKey),expiries=saved.getCompound(expiriesKey);
        for(String key:new java.util.ArrayList<>(levels.getAllKeys())){
            if(expiries.contains(key) && expiries.getLong(key)<=now){levels.remove(key);expiries.remove(key);}
        }
        saved.put(levelsKey,levels);saved.put(expiriesKey,expiries);
    }

    public static boolean allied(LivingEntity owner, Entity other) {
        if (owner==null || other==null) return false;
        if (owner==other || owner.isAlliedTo(other) || other.isAlliedTo(owner)) return true;
        if (other instanceof MahoragaEntity) return owner.getUUID().equals(((MahoragaEntity)other).ownerId());
        if (other instanceof net.minecraft.entity.passive.TameableEntity
                && owner.getUUID().equals(((net.minecraft.entity.passive.TameableEntity)other).getOwnerUUID())) return true;
        if (other instanceof PlayerEntity && !owner.level.isClientSide) {
            Crew crew=ExtendedWorldData.get(owner.level).getCrewWithMember(owner.getUUID());
            return crew!=null && crew.hasMember(other.getUUID());
        }
        return false;
    }

    private static void attacked(LivingAttackEvent e) {
        LivingEntity victim=e.getEntityLiving();
        if (victim.level.isClientSide || !(e.getSource().getEntity() instanceof LivingEntity)) return;
        LivingEntity attacker=(LivingEntity)e.getSource().getEntity();
        for (MahoragaEntity guardian : victim.level.getEntitiesOfClass(MahoragaEntity.class, victim.getBoundingBox().inflate(100))) {
            LivingEntity owner=guardian.owner();
            if ((victim==guardian || allied(owner,victim)) && guardian.validEnemy(attacker)) guardian.defend(attacker);
        }
    }

    public static MahoragaEntity find(LivingEntity owner) {
        if (!(owner.level instanceof ServerWorld) || !owner.getPersistentData().hasUUID(SUMMON_TAG)) return null;
        Entity entity=((ServerWorld)owner.level).getEntity(owner.getPersistentData().getUUID(SUMMON_TAG));
        return entity instanceof MahoragaEntity && entity.isAlive() && owner.getUUID().equals(((MahoragaEntity)entity).ownerId()) ? (MahoragaEntity)entity : null;
    }
    public static void dismiss(LivingEntity owner) {
        MahoragaEntity entity=find(owner);
        if (entity!=null) entity.beginDismiss();
        else owner.getPersistentData().remove(SUMMON_TAG);
    }
    public static MahoragaEntity summon(LivingEntity owner) {
        MahoragaEntity existing=find(owner);
        if (existing!=null) return existing.isDismissing()?null:existing;
        MahoragaEntity entity=new MahoragaEntity(MAHORAGA.get(),owner.level);
        Vector3d forward=owner.getLookAngle().multiply(1,0,1).normalize();
        for (int attempt=0; attempt<16; attempt++) {
            double angle=attempt*Math.PI/4;
            Vector3d offset=attempt==0?forward.scale(5):new Vector3d(Math.cos(angle),0,Math.sin(angle)).scale(4+attempt/8.0);
            BlockPos start=new BlockPos(owner.position().add(offset)).above(6);
            for (int down=0; down<18; down++) {
                BlockPos ground=start.below(down);
                if (!owner.level.hasChunkAt(ground) || !owner.level.getBlockState(ground).isFaceSturdy(owner.level,ground,net.minecraft.util.Direction.UP)) continue;
                entity.setPos(ground.getX()+.5,ground.getY()+1.01,ground.getZ()+.5);
                if (!owner.level.noCollision(entity)) continue;
                entity.setOwner(owner);
                entity.restoreFor(owner);
                entity.yRot=entity.yBodyRot=owner.yRot;
                entity.beginSummon();
                if (!owner.level.addFreshEntity(entity)) return null;
                owner.getPersistentData().putUUID(SUMMON_TAG,entity.getUUID());
                return entity;
            }
        }
        if (owner instanceof PlayerEntity) ((PlayerEntity)owner).displayClientMessage(new net.minecraft.util.text.StringTextComponent("Mahoraga needs clear ground and room to emerge."),true);
        return null;
    }
    private MahoragaFeatures() {}
}
