package net.kazi.kazimod.saiyan;

import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.generic.CheappoAbility;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.enums.*;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.packets.server.*;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class SaiyanFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "kazimod");
    public static final RegistryObject<EntityType<SaiyanProjectile>> PROJECTILE = TYPES.register("saiyan_ki", () ->
            EntityType.Builder.<SaiyanProjectile>of(SaiyanProjectile::new, EntityClassification.MISC).sized(.5F,.5F)
                    .clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("saiyan_ki"));
    public static final RegistryObject<EntityType<SaiyanEffect>> EFFECT = TYPES.register("saiyan_effect", () ->
            EntityType.Builder.<SaiyanEffect>of(SaiyanEffect::new, EntityClassification.MISC).sized(1,1)
                    .clientTrackingRange(12).updateInterval(2).noSave().build("saiyan_effect"));
    public static final RegistryObject<SoundEvent> BLAST = sound("saiyan_blast"), CHARGE = sound("saiyan_charge"),
            BEAM = sound("saiyan_kamehameha"), TELEPORT = sound("saiyan_teleport"), POWER_UP = sound("saiyan_power_up"),
            TRANSFORM = sound("saiyan_transform"), INSTINCT_THEME = sound("saiyan_ultra_instinct_theme");
    private static RegistryObject<SoundEvent> sound(String name) { return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation("kazimod",name))); }
    private static final UUID POWER_ID = UUID.fromString("e519ed41-a56b-4769-bffe-b1f138d0871d");
    private static final UUID SPEED_ID = UUID.fromString("a98e5479-dfa3-47bc-9258-a8d77af10d6c");
    private static final String TRANSFORM_GRAVITY = "InstinctTransformGravity";
    private static final String SPIRIT_BOOST_UNTIL = "KaziSpiritBombBoostUntil";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod","saiyan"), () -> "1", "1"::equals, "1"::equals);

    public static void init(IEventBus bus) {
        TYPES.register(bus); SOUNDS.register(bus);
        for (AbilityCore<SaiyanAbility> core : SaiyanAbility.CORES) KaziRegistry.registerAbility(core);
        KaziRegistry.registerAbility(SaiyanFlightAbility.INSTANCE);
        KaziRegistry.registerItem("hito_hito_no_mi_model_saiyan", () -> new AkumaNoMiItem(
                "Hito Hito no Mi, Model: Saiyan", 3, FruitType.MYTHICAL_ZOAN, allAbilities()));
        AbilityCommandGroup.create("SAIYAN", SaiyanFeatures::allAbilities);
        CHANNEL.registerMessage(0, SaiyanPacket.class, SaiyanPacket::encode, SaiyanPacket::decode, SaiyanPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(SaiyanFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener((LivingEvent.LivingUpdateEvent e)->SaiyanFinisher.recoverExpired(e.getEntityLiving()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,SaiyanFeatures::beforeUse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(LivingAttackEvent e)->{
            if(isInstinctTransforming(e.getEntityLiving())||isDragonFistCaptiveAttack(e.getSource().getEntity(),e.getEntityLiving()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(LivingHurtEvent e)->{
            if(isInstinctTransforming(e.getEntityLiving())||isDragonFistCaptiveAttack(e.getSource().getEntity(),e.getEntityLiving())){
                e.setCanceled(true);return;
            }
            LivingEntity target=e.getEntityLiving();
            if(e.getSource()==DamageSource.MAGIC&&e.getAmount()>0&&target.hasEffect(Effects.HARM)
                    &&target.getPersistentData().contains(SPIRIT_BOOST_UNTIL,4)){
                long until=target.getPersistentData().getLong(SPIRIT_BOOST_UNTIL);
                if(until>=target.level.getGameTime())e.setAmount(e.getAmount()*1.1F);
                else target.getPersistentData().remove(SPIRIT_BOOST_UNTIL);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(LivingDamageEvent e)->{
            if(isInstinctTransforming(e.getEntityLiving()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(LivingKnockBackEvent e)->{
            if(isInstinctTransforming(e.getEntityLiving()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(ProjectileImpactEvent e)->{
            if(!(e.getRayTraceResult() instanceof EntityRayTraceResult))return;
            Entity target=((EntityRayTraceResult)e.getRayTraceResult()).getEntity();
            if(target instanceof LivingEntity&&isInstinctTransforming((LivingEntity)target))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(AttackEntityEvent e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.RightClickItem e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.RightClickBlock e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.EntityInteract e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.EntityInteractSpecific e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.LeftClickBlock e)->{
            if(isActionLocked(e.getPlayer()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { SaiyanFinisher.recover(e.getPlayer(),true);resetRuntime(e.getPlayer()); sync(e.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> resetRuntime(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { resetRuntime(e.getPlayer()); sync(e.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone e) -> {
            if(e.getOriginal().getPersistentData().contains(SaiyanState.TAG,10))
                e.getPlayer().getPersistentData().put(SaiyanState.TAG, SaiyanState.get(e.getOriginal()).copy());
            resetRuntime(e.getPlayer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if(e.getPlayer() instanceof ServerPlayerEntity && e.getTarget() instanceof PlayerEntity)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)e.getPlayer()), packet((PlayerEntity)e.getTarget()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SaiyanClient.init(bus));
    }

    public static ActionResult<ItemStack> awaken(World world, PlayerEntity player, Hand hand) {
        ItemStack item = player.getItemInHand(hand);
        if(!SaiyanState.hasFruit(player)) return null;
        if(world.isClientSide) return ActionResult.success(item);
        if(!CommonConfig.INSTANCE.hasAwakeningsEnabled()) return fail(player,item,"Awakenings are disabled on this server.");
        int tier = SaiyanState.tier(player);
        if(tier >= SaiyanBalance.ULTRA_INSTINCT) return fail(player,item,"You have mastered all Saiyan awakenings.");
        if(player.getCooldowns().isOnCooldown(item.getItem())) return ActionResult.fail(item);
        SaiyanState.get(player).putInt("Tier", ++tier);
        SaiyanState.get(player).putFloat("Ki", SaiyanBalance.maxKi(tier));
        DevilFruitCapability.get(player).setAwakenedFruit(true);
        player.getCooldowns().addCooldown(item.getItem(), 20);
        if(!player.isCreative()) item.shrink(1);
        WyNetwork.sendTo(new SSyncDevilFruitPacket(player.getId(),DevilFruitCapability.get(player)),player);
        SaiyanState.pose(player,1,30);
        reconcile(player); sync(player);
        SaiyanEffect.spawn(player, 0, player.position(), 35, 3);
        player.sendMessage(new StringTextComponent("Unlocked " + SaiyanBalance.FORMS[tier] + "! Maximum Ki: " + SaiyanBalance.maxKi(tier) + ". Select it with Saiyan Transformation's alternate mode."),player.getUUID());
        return ActionResult.success(item);
    }
    private static ActionResult<ItemStack> fail(PlayerEntity p, ItemStack item, String message) {
        p.displayClientMessage(new StringTextComponent(message), true); return ActionResult.fail(item);
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        PlayerEntity p=event.player;
        if(event.phase!=TickEvent.Phase.END || p.level.isClientSide) return;
        boolean fruit=SaiyanState.hasFruit(p);
        if(p.tickCount%20==0) reconcile(p);
        if(!fruit) { releaseTransformationLock(p); return; }
        CompoundNBT state=SaiyanState.get(p);
        if(!p.isAlive() || AbilityHelper.isWeakenedByKairosekiOrWater(p)) { resetRuntime(p); if(p.tickCount%5==0)sync(p); return; }
        int form=SaiyanState.form(p);
        if(p.tickCount%20==0)clearLegacyAura(p);
        float drain=SaiyanBalance.DRAIN[form]/20F;
        if(drain>0 && !SaiyanState.spend(p,drain)) { setForm(p,0); SaiyanFlightAbility.stopFlight(p); }
        if(state.getBoolean("Flight")&&(!p.abilities.flying||p.isOnGround()||p.isSpectator())){
            state.putBoolean("Flight",false);sync(p);
        }
        updateTransformationLock(p);
        if(form==0 && !state.getBoolean("Flight") && !state.getBoolean("Charging") && SaiyanState.action(p)==0)
            SaiyanState.addKi(p,.04F);
        if(p.tickCount%5==0)sync(p);
    }

    private static boolean originalRacial(AbilityCore<?> core) { return core.getCategory()==AbilityCategory.RACIAL && !SaiyanAbility.isSaiyan(core); }
    public static boolean isKiCharging(LivingEntity user){
        return SaiyanState.get(user).getBoolean("Charging")&&SaiyanState.hasFruit(user);
    }
    public static boolean isInstinctTransforming(LivingEntity user){
        if(!user.isAlive()||user.isSpectator()||!user.getPersistentData().contains(SaiyanState.TAG,10))return false;
        return SaiyanState.form(user)==SaiyanBalance.ULTRA_INSTINCT&&SaiyanState.action(user)==10
                &&(user.level.isClientSide?SaiyanState.get(user).getBoolean("Active"):SaiyanState.hasFruit(user));
    }
    public static boolean isFinisherActive(LivingEntity user){
        if(!user.getPersistentData().contains(SaiyanState.TAG,10))return false;
        CompoundNBT n=SaiyanState.get(user);
        return n.getInt("Finisher")>0&&n.getLong("FinisherEnd")>user.level.getGameTime();
    }
    private static boolean isDragonFistCaptiveAttack(Entity source,LivingEntity target){
        if(!(source instanceof LivingEntity)||!isFinisherActive((LivingEntity)source))return false;
        CompoundNBT n=SaiyanState.get((LivingEntity)source);
        return !n.getBoolean("FinisherOwner")&&n.getInt("FinisherMove")==SaiyanAbility.Move.DRAGON.ordinal()
                &&n.getInt("FinisherCaptor")==target.getId();
    }
    public static boolean isActionLocked(LivingEntity user){return isKiCharging(user)||isInstinctTransforming(user)||isFinisherActive(user);}
    public static void markSpiritBombTarget(LivingEntity target){
        target.getPersistentData().putLong(SPIRIT_BOOST_UNTIL,target.level.getGameTime()+21);
    }
    static void updateTransformationLock(LivingEntity user){
        if(!isInstinctTransforming(user)){releaseTransformationLock(user);return;}
        CompoundNBT state=SaiyanState.get(user);
        if(!state.contains(TRANSFORM_GRAVITY))state.putBoolean(TRANSFORM_GRAVITY,user.isNoGravity());
        user.setNoGravity(true);
        user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),3,0,false,false,false));
        user.stopUsingItem();user.setSprinting(false);user.setDeltaMovement(Vector3d.ZERO);user.fallDistance=0;
    }
    private static void releaseTransformationLock(LivingEntity user){
        if(!user.getPersistentData().contains(SaiyanState.TAG,10))return;
        CompoundNBT state=SaiyanState.get(user);
        if(state.contains(TRANSFORM_GRAVITY)){
            user.setNoGravity(state.getBoolean(TRANSFORM_GRAVITY));state.remove(TRANSFORM_GRAVITY);
        }
    }
    private static void beforeUse(AbilityUseEvent.Pre e) {
        if(isFinisherActive(e.getEntityLiving())){
            int move=SaiyanState.get(e.getEntityLiving()).getInt("FinisherMove");
            if(!SaiyanState.get(e.getEntityLiving()).getBoolean("FinisherOwner")||move<0||move>=SaiyanAbility.Move.values().length||e.getAbility().getCore()!=SaiyanAbility.core(SaiyanAbility.Move.values()[move])){
                e.setCanceled(true);return;
            }
        }
        if(isInstinctTransforming(e.getEntityLiving())){e.setCanceled(true);return;}
        if(isKiCharging(e.getEntityLiving())&&e.getAbility().getCore()!=SaiyanAbility.core(SaiyanAbility.Move.CHARGE)){
            e.setCanceled(true);return;
        }
        if(SaiyanState.hasFruit(e.getEntityLiving()) && e.getAbility().getCore()!=CheappoAbility.INSTANCE
                && originalRacial(e.getAbility().getCore())) e.setCanceled(true);
    }

    // Keep the original race identity untouched; only its move loadout is suspended.
    private static void reconcile(PlayerEntity p) {
        IAbilityData abilities=AbilityDataCapability.get(p);
        CompoundNBT state=SaiyanState.get(p);
        boolean changed=false;
        AbilityCore<SaiyanAbility> retiredBlast=SaiyanAbility.core(SaiyanAbility.Move.BLAST);
        if(abilities.getEquippedAbility(retiredBlast)!=null||abilities.getPassiveAbility(retiredBlast)!=null||abilities.hasUnlockedAbility(retiredBlast)){
            IAbility equipped=abilities.getEquippedAbility(retiredBlast);
            IAbility passive=abilities.getPassiveAbility(retiredBlast);
            if(equipped!=null)AbilityHelper.emergencyStopAbility(p,equipped);
            if(passive!=null)AbilityHelper.emergencyStopAbility(p,passive);
            abilities.removeEquippedAbility(retiredBlast);abilities.removePassiveAbility(retiredBlast);abilities.removeUnlockedAbility(retiredBlast);
            changed=true;
        }
        if(SaiyanState.hasFruit(p)) {
            if(!state.getBoolean("Installed")) { state.putBoolean("Installed",true); state.putFloat("Ki",SaiyanState.maxKi(p)); }
            if(state.contains("OldFlySpeed")){
                p.abilities.setFlyingSpeed(state.getFloat("OldFlySpeed"));
                state.remove("OldFlySpeed");state.remove("OldMayFly");p.onUpdateAbilities();
            }
            ListNBT saved=state.getList("RaceUnlocks",10);
            Set<String> recorded=new HashSet<>();
            for(int i=0;i<saved.size();i++)recorded.add(saved.getCompound(i).getString("Core"));
            for(AbilityCoreUnlockWrapper wrapper:new ArrayList<>(abilities.getUnlockedAbilities())) {
                AbilityCore<?> core=wrapper.getAbilityCore();
                if(core==CheappoAbility.INSTANCE&&state.getBoolean("CheappoInstalled"))continue;
                if(!originalRacial(core))continue;
                String key=core.getKey().toString();
                if(recorded.add(key)) {
                    CompoundNBT entry=new CompoundNBT(); entry.putString("Core",key);entry.put("Unlock",wrapper.save());
                    IAbility equipped=abilities.getEquippedAbility(core);
                    entry.putInt("Slot",equipped==null?-1:abilities.getEquippedAbilitySlot(equipped));
                    IAbility passive=abilities.getPassiveAbility(core);
                    entry.putBoolean("Passive",passive!=null);
                    if(equipped!=null){AbilityHelper.emergencyStopAbility(p,equipped);entry.put("EquippedState",saveAbility(equipped));}
                    if(passive!=null){AbilityHelper.emergencyStopAbility(p,passive);entry.put("PassiveState",saveAbility(passive));}
                    saved.add(entry);
                }
                abilities.removeEquippedAbility(core);abilities.removePassiveAbility(core);abilities.removeUnlockedAbility(core);changed=true;
            }
            state.put("RaceUnlocks",saved);
            if(!abilities.hasUnlockedAbility(CheappoAbility.INSTANCE)){
                abilities.addUnlockedAbility(CheappoAbility.INSTANCE,AbilityUnlock.PROGRESSION);changed=true;
            }
            state.putBoolean("CheappoInstalled",true);
            for(AbilityCore<SaiyanAbility> core:SaiyanAbility.CORES) {
                if(core!=retiredBlast&&core.canUnlock(p) && !abilities.hasUnlockedAbility(core)) {abilities.addUnlockedAbility(core,AbilityUnlock.PROGRESSION);changed=true;}
            }
            // Migrate the previous combat-bar toggle to an automatically equipped passive.
            if(abilities.removeEquippedAbility(SaiyanFlightAbility.INSTANCE))changed=true;
            if(!abilities.hasUnlockedAbility(SaiyanFlightAbility.INSTANCE)){
                abilities.addUnlockedAbility(SaiyanFlightAbility.INSTANCE,AbilityUnlock.PROGRESSION);changed=true;
            }
            if(!abilities.hasPassiveAbility(SaiyanFlightAbility.INSTANCE)){
                abilities.addPassiveAbility(SaiyanFlightAbility.INSTANCE.createAbility());changed=true;
            }
        } else if(state.getBoolean("Installed")) {
            resetRuntime(p);
            for(AbilityCore<?> core:allAbilities()) {
                abilities.removeEquippedAbility(core);abilities.removePassiveAbility(core);abilities.removeUnlockedAbility(core);
            }
            ListNBT saved=state.getList("RaceUnlocks",10);
            for(int i=0;i<saved.size();i++) {
                CompoundNBT entry=saved.getCompound(i); AbilityCore<?> core=AbilityCore.get(new ResourceLocation(entry.getString("Core")));
                if(core==null)continue;
                abilities.addUnlockedAbility(AbilityCoreUnlockWrapper.of(entry.getCompound("Unlock")));
                int slot=entry.getInt("Slot");
                if(slot>=0 && slot<abilities.getRawEquippedAbilities().size() && abilities.getEquippedAbility(slot)==null)
                    abilities.setEquippedAbility(slot,restoreAbility(core,entry.getCompound("EquippedState")));
                if(entry.getBoolean("Passive"))abilities.addPassiveAbility(restoreAbility(core,entry.getCompound("PassiveState")));
            }
            p.getPersistentData().remove(SaiyanState.TAG);changed=true;sync(p);
        }
        if(changed)WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),abilities),p);
    }

    public static void setForm(LivingEntity user,int form) {
        form=Math.min(SaiyanState.tier(user),SaiyanBalance.tier(form));
        CompoundNBT state=SaiyanState.get(user);
        if(state.getInt("Form")!=form){
            Entity old=user.level.getEntity(state.getInt("AuraEntity"));
            if(old instanceof SaiyanEffect&&((SaiyanEffect)old).owner()==user&&((SaiyanEffect)old).kind()==SaiyanEffect.TRANSFORMATION_AURA)old.remove();
            state.remove("AuraEntity");
        }
        state.putInt("Form",form);
        if(form!=SaiyanBalance.ULTRA_INSTINCT)releaseTransformationLock(user);
        if(!user.level.isClientSide)clearLegacyAura(user);
        if(form==0&&SaiyanState.action(user)==10)SaiyanState.pose(user,0,0);
        modifier(user,Attributes.ATTACK_DAMAGE,POWER_ID,form==0?0:(SaiyanBalance.POWER[form]-1));
        modifier(user,Attributes.MOVEMENT_SPEED,SPEED_ID,form*.035);
        if(user instanceof PlayerEntity)sync((PlayerEntity)user);
    }
    private static void clearLegacyAura(LivingEntity user){
        CompoundNBT state=SaiyanState.get(user);
        Entity current=user.level.getEntity(state.getInt("AuraEntity"));
        if(current instanceof SaiyanEffect&&((SaiyanEffect)current).owner()==user
                &&((SaiyanEffect)current).kind()==SaiyanEffect.TRANSFORMATION_AURA)current.remove();
        state.remove("AuraEntity");
    }
    private static void modifier(LivingEntity user,Attribute attribute,UUID id,double value) {
        ModifiableAttributeInstance instance=user.getAttribute(attribute);if(instance==null)return;
        instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,"Saiyan transformation",value,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void resetRuntime(PlayerEntity p) {
        if(!p.getPersistentData().contains(SaiyanState.TAG,10))return;
        if(!p.level.isClientSide)for(IAbility ability:new ArrayList<>(AbilityDataCapability.get(p).getEquippedAndPassiveAbilities()))
            if(SaiyanAbility.isSaiyan(ability.getCore()))AbilityHelper.emergencyStopAbility(p,ability);
        SaiyanFinisher.recover(p,false);
        SaiyanFlightAbility.stopFlight(p);setForm(p,0);
        CompoundNBT state=SaiyanState.get(p);state.putBoolean("Charging",false);state.putLong("ActionEnd",0);
        state.remove("InstinctNextEffect");
        for(String key:new String[]{"Finisher","FinisherPhase","FinisherStart","FinisherEnd","FinisherYaw","FinisherSpace","FinisherMove","FinisherOwner"})state.remove(key);
    }
    private static CompoundNBT saveAbility(IAbility ability){
        CompoundNBT nbt=ability.save(new CompoundNBT()),components=new CompoundNBT();
        ability.getComponents().forEach((key,value)->{CompoundNBT saved=value.save();if(saved!=null)components.put(key.getId().toString(),saved);});
        nbt.put("components",components);return nbt;
    }
    private static IAbility restoreAbility(AbilityCore<?> core,CompoundNBT saved){
        IAbility ability=core.createAbility();
        if(!saved.isEmpty()){
            ability.load(saved);CompoundNBT components=saved.getCompound("components");
            ability.getComponents().forEach((key,value)->{String id=key.getId().toString();if(components.contains(id,10))value.load(components.getCompound(id));});
        }
        return ability;
    }
    private static SaiyanPacket packet(PlayerEntity p) {
        CompoundNBT state=SaiyanState.get(p), visible=new CompoundNBT();
        for(String key:new String[]{"Tier","Form","Ki","Flight","Charging","Action","ActionStart","ActionEnd",
                "Finisher","FinisherPhase","FinisherStart","FinisherEnd","FinisherYaw","FinisherSpace","FinisherMove","FinisherOwner"})
            if(state.contains(key))visible.put(key,state.get(key).copy());
        visible.putBoolean("Active",SaiyanState.hasFruit(p));
        return new SaiyanPacket(p.getId(),visible);
    }
    public static void sync(PlayerEntity p) {
        if(p instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),packet(p));
    }
    public static void refillKi(PlayerEntity player) {
        if(player.level.isClientSide || !SaiyanState.hasFruit(player))return;
        SaiyanState.get(player).putFloat("Ki",SaiyanState.maxKi(player));
        sync(player);
    }
    private static AbilityCore<?>[] allAbilities(){
        List<AbilityCore<?>> result=new ArrayList<>(Arrays.asList(SaiyanAbility.CORES));
        result.remove(SaiyanAbility.core(SaiyanAbility.Move.BLAST));
        result.add(SaiyanFlightAbility.INSTANCE);result.add(CheappoAbility.INSTANCE);
        return result.toArray(new AbilityCore<?>[0]);
    }
    public static void play(LivingEntity user,SoundEvent sound,float volume,float pitch) {
        user.level.playSound(null,user.blockPosition(),sound,SoundCategory.PLAYERS,volume,pitch);
    }
}
