package net.kazi.kazimod.gengar;

import java.util.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.kazi.kazimod.tusk.TuskFeatures;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.server.*;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class GengarFeatures {
    public static final String TAG = "KaziMegaGengar", HAUNT = "KaziGengarHaunt";
    public static final RegistryObject<GengarMorphInfo> MORPH = KaziRegistry.registerMorph("mega_gengar", GengarMorphInfo::new);
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    public static final RegistryObject<EntityType<GengarProjectile>> PROJECTILE = TYPES.register("gengar_shadow", () ->
            EntityType.Builder.<GengarProjectile>of(GengarProjectile::new, EntityClassification.MISC).sized(.45F,.45F)
                    .clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("gengar_shadow"));
    public static final RegistryObject<EntityType<GengarEffect>> EFFECT = TYPES.register("gengar_effect", () ->
            EntityType.Builder.<GengarEffect>of(GengarEffect::new, EntityClassification.MISC).sized(.1F,.1F)
                    .clientTrackingRange(12).updateInterval(2).noSave().build("gengar_effect"));
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod","gengar"), () -> "1", "1"::equals,"1"::equals);
    public static AbilityCore<?>[] cores() {
        AbilityCore<?>[] result = new AbilityCore<?>[GengarAbility.CORES.length+1]; result[0]=GengarFormAbility.INSTANCE;
        System.arraycopy(GengarAbility.CORES,0,result,1,GengarAbility.CORES.length); return result;
    }
    public static void init(IEventBus bus) {
        // Keep registry IDs readable for old saves; none of these abilities remain obtainable.
        TYPES.register(bus); KaziRegistry.registerAbility(GengarFormAbility.INSTANCE);
        for(AbilityCore<GengarAbility> core:GengarAbility.CORES) KaziRegistry.registerAbility(core);
        for(AbilityCore<?> core:cores())core.setHidden();
        CHANNEL.registerMessage(0,GengarPacket.class,GengarPacket::encode,GengarPacket::decode,GengarPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            FruitAbilityInjector.removeAbilities(ModAbilities.HORO_HORO_NO_MI,cores());
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(GengarFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,GengarFeatures::beforeUse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(UnlockAbilityEvent event)->{
            if(blocked(event.getEntityLiving(),event.getAbilityCore()))event.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.EntityJoinWorldEvent event)->{
            if(event.getEntity() instanceof GengarProjectile||event.getEntity() instanceof GengarEffect)event.setCanceled(true);
            if(event.getEntity() instanceof LivingEntity){
                event.getEntity().getPersistentData().remove(HAUNT);
                event.getEntity().getPersistentData().remove("KaziGengarHeldUntil");
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event)->reconcile(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->clear(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event)->reconcile(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event)->reconcile(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event)->{if(event.getEntityLiving() instanceof PlayerEntity)clear((PlayerEntity)event.getEntityLiving());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event)->{
            if(event.getPlayer() instanceof ServerPlayerEntity && event.getTarget() instanceof PlayerEntity
                    &&event.getTarget().getPersistentData().contains(TAG,10))
                CHANNEL.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)event.getPlayer()),new GengarPacket(event.getTarget().getId(),state((LivingEntity)event.getTarget()).copy()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GengarClient.init(bus));
    }
    public static boolean hasFruit(LivingEntity user){return DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.HORO_HORO_NO_MI);}
    public static boolean awakened(LivingEntity user){return false;}
    public static boolean ours(AbilityCore<?> core){return core==GengarFormAbility.INSTANCE||GengarAbility.isGengar(core);}
    public static boolean original(AbilityCore<?> core){
        if(core==null||ours(core))return false;
        if(core==xyz.pixelatedw.mineminenomi.abilities.horo.MiniHollowAbility.INSTANCE
                ||core==xyz.pixelatedw.mineminenomi.abilities.horo.NegativeHollowAbility.INSTANCE
                ||core==xyz.pixelatedw.mineminenomi.abilities.horo.TokuHollowAbility.INSTANCE)return true;
        for(AbilityCore<?> old:ModAbilities.HORO_HORO_NO_MI.getAbilities())if(old==core)return true;
        return core==net.kazi.kazimod.abilities.HoroRework.MiniHollowRework.INSTANCE
                ||core==net.kazi.kazimod.abilities.HoroRework.NegativeHollowRework.INSTANCE
                ||core==net.kazi.kazimod.abilities.HoroRework.TokuHollowRework.INSTANCE;
    }
    public static boolean blocked(LivingEntity user,AbilityCore<?> core){return ours(core);}
    public static CompoundNBT state(LivingEntity user){
        CompoundNBT root=user.getPersistentData();if(!root.contains(TAG,10))root.put(TAG,new CompoundNBT());return root.getCompound(TAG);
    }
    public static boolean active(LivingEntity user){return false;}
    public static int action(LivingEntity user){CompoundNBT n=user.getPersistentData().getCompound(TAG);return n.getLong("End")>user.level.getGameTime()?n.getInt("Action"):0;}
    public static void pose(LivingEntity user,int action,int ticks){
        CompoundNBT n=state(user);n.putInt("Action",action);n.putLong("Start",user.level.getGameTime());n.putLong("End",user.level.getGameTime()+ticks);sync(user);
    }
    public static void sync(LivingEntity user){if(user instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->user),new GengarPacket(user.getId(),user.getPersistentData().getCompound(TAG).copy()));}
    public static boolean awaken(PlayerEntity user,ItemStack essence){
        return false;
    }
    public static void reconcile(PlayerEntity user){
        if(user.level.isClientSide)return;
        IAbilityData data=AbilityDataCapability.get(user);
        xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit fruit=DevilFruitCapability.get(user);
        boolean horo=hasFruit(user),wasAwakened=horo&&fruit.hasAwakenedFruit();
        boolean changed=wasAwakened||user.getPersistentData().contains(TAG)||fruit.hasMorphInQueue(MORPH.get());
        Set<AbilityCore<?>> all=new HashSet<>();for(AbilityCoreUnlockWrapper<?> w:data.getUnlockedAbilities())all.add(w.getAbilityCore());
        for(IAbility a:data.getEquippedAndPassiveAbilities())all.add(a.getCore());
        List<Integer> slots=new ArrayList<>();
        List<IAbility> equippedBefore=new ArrayList<>(data.getRawEquippedAbilities());
        for(int i=0;i<equippedBefore.size();i++)if(equippedBefore.get(i)!=null&&ours(equippedBefore.get(i).getCore()))slots.add(i);
        if(wasAwakened)fruit.setAwakenedFruit(false);
        for(AbilityCore<?> core:all){
            if(!ours(core))continue;
            IAbility equipped=data.getEquippedAbility(core),passive=data.getPassiveAbility(core);
            if(equipped!=null)AbilityHelper.emergencyStopAbility(user,equipped);
            if(passive!=null)AbilityHelper.emergencyStopAbility(user,passive);
            data.removeEquippedAbility(core);data.removePassiveAbility(core);data.removeUnlockedAbility(core);changed=true;
        }
        if(changed)clear(user);
        if(horo&&changed){
            int index=0;
            for(AbilityCore<?> core:ModAbilities.HORO_HORO_NO_MI.getAbilities()){
                if(ours(core)||core.hasUnlockCheck()&&!core.canUnlock(user))continue;
                if(!data.hasUnlockedAbility(core))data.addUnlockedAbility(core,AbilityUnlock.PROGRESSION);
                if(core.getType()==AbilityType.PASSIVE){
                    if(!data.hasPassiveAbility(core))data.addPassiveAbility(core.createAbility());
                    continue;
                }
                if(data.getEquippedAbility(core)!=null)continue;
                while(index<slots.size()&&data.getEquippedAbility(slots.get(index))!=null)index++;
                if(index<slots.size())data.setEquippedAbility(slots.get(index++),core.createAbility());
            }
        }
        if(changed){
            WyNetwork.sendTo(new SSyncAbilityDataPacket(user.getId(),data),user);
            WyNetwork.sendToAllTrackingAndSelf(new SSyncDevilFruitPacket(user.getId(),fruit),user);
        }
    }
    public static void clear(PlayerEntity user){
        if(user.level.isClientSide)return;
        for(IAbility a:new ArrayList<>(AbilityDataCapability.get(user).getEquippedAndPassiveAbilities()))if(ours(a.getCore()))AbilityHelper.emergencyStopAbility(user,a);
        xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit fruit=DevilFruitCapability.get(user);
        if(fruit.hasMorphInQueue(MORPH.get())){fruit.removeMorph(MORPH.get());user.refreshDimensions();}
        removeLegacyModifier(user,net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH,xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper.MORPH_HEALTH_UUID);
        removeLegacyModifier(user,net.minecraft.entity.ai.attributes.Attributes.MOVEMENT_SPEED,xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper.MORPH_MOVEMENT_SPEED_UUID);
        removeLegacyModifier(user,ModAttributes.STEP_HEIGHT.get(),xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper.MORPH_STEP_HEIGHT_UUID);
        user.setHealth(Math.min(user.getHealth(),user.getMaxHealth()));
        user.getPersistentData().remove(TAG);user.getPersistentData().remove(HAUNT);user.getPersistentData().remove("KaziGengarHeldUntil");sync(user);
    }
    private static void removeLegacyModifier(LivingEntity user,net.minecraft.entity.ai.attributes.Attribute attribute,UUID id){
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance instance=user.getAttribute(attribute);
        if(instance!=null&&instance.getModifier(id)!=null&&instance.getModifier(id).getName().startsWith("Mega Gengar"))instance.removeModifier(id);
    }
    private static void tick(TickEvent.PlayerTickEvent event){
        PlayerEntity user=event.player;if(event.phase!=TickEvent.Phase.END||user.level.isClientSide)return;
        if(user.tickCount%20==0)reconcile(user);
    }
    private static void beforeUse(AbilityUseEvent.Pre event){
        LivingEntity user=event.getEntityLiving();AbilityCore<?> core=event.getAbility().getCore();
        if(ours(core))event.setCanceled(true);
    }
    private static void avoid(LivingAttackEvent event){
        LivingEntity user=event.getEntityLiving();DamageSource source=event.getSource();
        if(!user.level.isClientSide&&active(user)&&awakened(user)&&action(user)==4
                &&!AbilityHelper.isWeakenedByKairosekiOrWater(user)&&source.getEntity()!=null&&!source.isBypassInvul()
                &&!(source instanceof ModDamageSource&&((ModDamageSource)source).isUnavoidable()))event.setCanceled(true);
    }
    private static void hurt(LivingHurtEvent event){
        LivingEntity target=event.getEntityLiving();if(target.level.isClientSide)return;
        if(active(target)&&awakened(target)&&!event.getSource().isBypassInvul()
                &&!AbilityHelper.isWeakenedByKairosekiOrWater(target))event.setAmount(event.getAmount()*.75F);
        Entity attacker=event.getSource().getEntity();
        if(attacker instanceof LivingEntity&&hauntStacks((LivingEntity)attacker)>0)event.setAmount(event.getAmount()*.85F);
    }
    public static int hauntStacks(LivingEntity target){CompoundNBT n=target.getPersistentData().getCompound(HAUNT);return n.getLong("Until")>target.level.getGameTime()?Math.min(3,n.getInt("Stacks")):0;}
    public static int consumeHaunt(LivingEntity user,LivingEntity target){
        CompoundNBT n=target.getPersistentData().getCompound(HAUNT);if(!n.hasUUID("Owner")||!n.getUUID("Owner").equals(user.getUUID()))return 0;
        int stacks=hauntStacks(target);target.getPersistentData().remove(HAUNT);return stacks;
    }
    public static boolean hauntedBy(LivingEntity user,LivingEntity target){CompoundNBT n=target.getPersistentData().getCompound(HAUNT);return hauntStacks(target)>0&&n.hasUUID("Owner")&&n.getUUID("Owner").equals(user.getUUID());}
    public static void haunt(LivingEntity user,LivingEntity target){
        CompoundNBT n=target.getPersistentData().getCompound(HAUNT);int stacks=hauntedBy(user,target)?hauntStacks(target):0;
        n.putUUID("Owner",user.getUUID());n.putInt("Stacks",Math.min(3,stacks+1));n.putLong("Until",target.level.getGameTime()+160);target.getPersistentData().put(HAUNT,n);
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,80,0,false,false));
        GengarEffect.spawn(user,GengarEffect.HAUNT,target.position(),40,target.getBbHeight()).attach(target);
    }
    public static boolean canHit(LivingEntity user,LivingEntity target){return TuskFeatures.canHit(user,target);}
    public static LivingEntity aim(LivingEntity user,double range){return TuskFeatures.aim(user,range);}
    public static void sound(LivingEntity user,SoundEvent sound,float volume,float pitch){user.level.playSound(null,user.blockPosition(),sound,SoundCategory.PLAYERS,volume,pitch);}
    private GengarFeatures(){}
}
