package net.kazi.kazimod.blood;

import java.util.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.enums.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class BloodFeatures {
    public static final String TAG = "KaziBlood";
    public static final int HIT_EFFECT_TICKS = 100;
    public static final ResourceLocation FRUIT = new ResourceLocation("kazimod", "chi_chi_no_mi");
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    public static final RegistryObject<EntityType<BloodProjectile>> PROJECTILE = TYPES.register("blood_projectile",
            () -> EntityType.Builder.<BloodProjectile>of(BloodProjectile::new, EntityClassification.MISC)
                    .sized(.3F, .3F).clientTrackingRange(12).updateInterval(1).noSave().build("blood_projectile"));
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod", "blood"), () -> "1", "1"::equals, "1"::equals);
    private static final UUID LEGACY_EDGE_DAMAGE = UUID.fromString("8dfc56a7-483f-4ca7-a216-7a32e652e401"),
            LEGACY_EDGE_REACH = UUID.fromString("8dfc56a7-483f-4ca7-a216-7a32e652e402");
    public static final UUID SCALE_DAMAGE = UUID.fromString("8dfc56a7-483f-4ca7-a216-7a32e652e403"),
            SCALE_SPEED = UUID.fromString("8dfc56a7-483f-4ca7-a216-7a32e652e404"),
            SCALE_RESIST = UUID.fromString("8dfc56a7-483f-4ca7-a216-7a32e652e405");

    public static void init(IEventBus bus) {
        TYPES.register(bus);
        for (int kind = 0; kind <= BloodAbility.CORES.length; kind++)
            KaziRegistry.registerAbility(kind == 3 ? BloodAbility.RETIRED_EDGE : BloodAbility.core(kind));
        KaziRegistry.registerItem("chi_chi_no_mi", () -> new AkumaNoMiItem("Chi Chi no Mi", 2, FruitType.PARAMECIA, BloodAbility.CORES));
        AbilityCommandGroup.create("BLOOD", () -> BloodAbility.CORES);
        CHANNEL.registerMessage(0, BloodPacket.class, BloodPacket::encode, BloodPacket::decode, BloodPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(BloodFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, BloodFeatures::damage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PotionEvent.PotionApplicableEvent e) -> {
            if(e.getPotionEffect().getEffect()==net.kazi.kazimod.init.KaziEffects.BLOOD_POISON.get() && fruit(e.getEntityLiving()))
                e.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent.Pre e) -> {
            LivingEntity p = e.getEntityLiving(); int action = action(p);
            if (action >= 0 && e.getAbility().getCore() != BloodAbility.core(action) && fruit(p)) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { clear(e.getPlayer()); reconcile(e.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { clear(e.getPlayer()); reconcile(e.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent e) -> { if (e.getEntityLiving() instanceof PlayerEntity) clear((PlayerEntity)e.getEntityLiving()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getPlayer() instanceof ServerPlayerEntity && e.getTarget() instanceof PlayerEntity)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)e.getPlayer()), packet((LivingEntity)e.getTarget()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BloodClient.init(bus));
    }
    public static boolean fruit(LivingEntity p) { return p != null && DevilFruitCapability.get(p).getDevilFruit().filter(FRUIT::equals).isPresent(); }
    public static boolean usable(LivingEntity p) { return fruit(p) && p.isAlive() && !p.isSpectator() && !AbilityHelper.isWeakenedByKairosekiOrWater(p); }
    public static CompoundNBT state(LivingEntity p) {
        if (!p.getPersistentData().contains(TAG, 10)) p.getPersistentData().put(TAG, new CompoundNBT());
        return p.getPersistentData().getCompound(TAG);
    }
    public static boolean scale(LivingEntity p) { return state(p).getLong("ScaleEnd") > p.level.getGameTime(); }
    public static int action(LivingEntity p) {
        CompoundNBT n = state(p);
        int kind = n.getInt("Action");
        return n.getLong("End") > p.level.getGameTime() && BloodAbility.core(kind) != null ? kind : -1;
    }
    public static void pose(LivingEntity p, int action, int ticks) {
        state(p).putInt("Action", action); state(p).putLong("Start", p.level.getGameTime()); state(p).putLong("End", p.level.getGameTime() + ticks); sync(p);
    }
    private static BloodPacket packet(LivingEntity p) {
        CompoundNBT n = state(p).copy(); n.remove("Casts"); n.remove("SwingReceipt"); return new BloodPacket(p.getId(), n);
    }
    public static void sync(LivingEntity p) {
        if (p instanceof ServerPlayerEntity) CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packet(p));
    }
    public static void reconcile(PlayerEntity p) {
        if (p.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(p); boolean changed = false;
        AbilityCore<?> retired = BloodAbility.RETIRED_EDGE;
        IAbility equipped = data.getEquippedAbility(retired), passive = data.getPassiveAbility(retired);
        if (equipped != null || passive != null || data.hasUnlockedAbility(retired)) {
            if (equipped != null) AbilityHelper.emergencyStopAbility(p, equipped);
            if (passive != null) AbilityHelper.emergencyStopAbility(p, passive);
            data.removeEquippedAbility(retired);
            data.removePassiveAbility(retired);
            data.removeUnlockedAbility(retired);
            clearLegacyEdgeState(p);
            sync(p);
            changed = true;
        }
        for (AbilityCore<?> core : BloodAbility.CORES) {
            if (fruit(p)) { if (!data.hasUnlockedAbility(core)) changed |= data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION); }
            else if (data.hasUnlockedAbility(core)) {
                IAbility ability = data.getEquippedAbility(core);
                if (ability != null) AbilityHelper.emergencyStopAbility(p, ability);
                data.removeEquippedAbility(core); data.removeUnlockedAbility(core); changed = true;
            }
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(), data), p);
    }
    private static void damage(LivingDamageEvent e) {
        if (e.getEntityLiving().level.isClientSide || e.getAmount() <= 0) return;
        LivingEntity defender=e.getEntityLiving();
        Entity attacker=e.getSource().getEntity();
        if(attacker instanceof LivingEntity && "player".equals(e.getSource().getMsgId())
                && usable((LivingEntity)attacker) && scale((LivingEntity)attacker)
                && net.kazi.kazimod.petal.PetalFeatures.canHit((LivingEntity)attacker,defender))applyHitEffects(defender);
        if(BloodFeatures.action(defender)==0){
            IAbility a=AbilityDataCapability.get(defender).getEquippedAbility(BloodAbility.core(0));
            if(a instanceof BloodAbility)((BloodAbility)a).interrupt(defender);
        }
    }
    public static void applyHitEffects(LivingEntity target) {
        if(target.level.isClientSide || !(target instanceof PlayerEntity) || !target.isAlive())return;
        target.addEffect(new net.minecraft.potion.EffectInstance(net.minecraft.potion.Effects.MOVEMENT_SLOWDOWN,HIT_EFFECT_TICKS,0));
        target.addEffect(new net.minecraft.potion.EffectInstance(net.kazi.kazimod.init.KaziEffects.WEAKENED_MOVEMENT.get(),HIT_EFFECT_TICKS,0));
        target.addEffect(new net.minecraft.potion.EffectInstance(net.kazi.kazimod.init.KaziEffects.BLOOD_POISON.get(),HIT_EFFECT_TICKS,0));
    }
    private static void tick(TickEvent.PlayerTickEvent e) {
        PlayerEntity p = e.player;
        if (e.phase != TickEvent.Phase.END || p.level.isClientSide) return;
        if(fruit(p))p.removeEffect(net.kazi.kazimod.init.KaziEffects.BLOOD_POISON.get());
        if (p.getPersistentData().contains(TAG)) {
            BloodLedger.tick(p);
            if (!usable(p) && (scale(p) || action(p) >= 0)) clear(p);
        }
        if (p.tickCount % 20 == 0) reconcile(p);
    }
    public static void clear(PlayerEntity p) {
        if (p.level.isClientSide) return;
        clearLegacyEdgeState(p);
        BloodLedger.clear(p);
        for (IAbility a : new ArrayList<IAbility>(AbilityDataCapability.get(p).getEquippedAndPassiveAbilities()))
            if (Arrays.asList(BloodAbility.CORES).contains(a.getCore())) AbilityHelper.emergencyStopAbility(p, a);
        state(p).putLong("ScaleEnd", 0); state(p).putLong("End", 0);state(p).putLong("TetherUntil",0);
        state(p).remove("NovaAt");
        state(p).putLong("NovaHoldUntil",0);
        remove(p, Attributes.ATTACK_DAMAGE, SCALE_DAMAGE);
        remove(p, Attributes.MOVEMENT_SPEED, SCALE_SPEED); remove(p, Attributes.KNOCKBACK_RESISTANCE, SCALE_RESIST);
        if (p.getServer() != null) for (net.minecraft.world.server.ServerWorld world : p.getServer().getAllLevels())
            for (Entity shot : world.getEntities(PROJECTILE.get(), s -> p.getUUID().equals(((BloodProjectile)s).originalOwner()))) shot.remove();
        sync(p);
    }
    private static void clearLegacyEdgeState(PlayerEntity p) {
        CompoundNBT n = state(p);
        if (n.hasUUID("SwingReceipt")) BloodLedger.neutral(p, n.getUUID("SwingReceipt"));
        n.remove("SwingReceipt");
        n.remove("LastSwing");
        n.remove("SlashSide");
        n.remove("Edge");
        if (n.getInt("Action") == 3) n.remove("End");
        remove(p, Attributes.ATTACK_DAMAGE, LEGACY_EDGE_DAMAGE);
        remove(p, ForgeMod.REACH_DISTANCE.get(), LEGACY_EDGE_REACH);
        remove(p, ModAttributes.ATTACK_RANGE.get(), LEGACY_EDGE_REACH);
    }
    private static void remove(LivingEntity p, Attribute attribute, UUID id) {
        ModifiableAttributeInstance a = p.getAttribute(attribute); if (a != null) a.removeModifier(id);
    }
    private BloodFeatures() {}
}
