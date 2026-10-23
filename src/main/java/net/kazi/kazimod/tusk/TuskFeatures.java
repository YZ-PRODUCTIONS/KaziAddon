package net.kazi.kazimod.tusk;

import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.guru.*;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
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
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.packets.server.*;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class TuskFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    public static final RegistryObject<EntityType<TuskProjectile>> PROJECTILE = TYPES.register("tusk_nail", () ->
            EntityType.Builder.<TuskProjectile>of(TuskProjectile::new, EntityClassification.MISC).sized(.25F, .25F)
                    .clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("tusk_nail"));
    public static final RegistryObject<EntityType<TuskEffect>> EFFECT = TYPES.register("tusk_effect", () ->
            EntityType.Builder.<TuskEffect>of(TuskEffect::new, EntityClassification.MISC).sized(.25F, .25F)
                    .clientTrackingRange(12).updateInterval(2).noSave().build("tusk_effect"));
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod", "tusk"), () -> "1", "1"::equals, "1"::equals);
    private static final String ROTATION = "KaziInfiniteRotation";

    public static void init(IEventBus bus) {
        TYPES.register(bus);
        for (AbilityCore<TuskAbility> core : TuskAbility.CORES) KaziRegistry.registerAbility(core);
        AbilityCommandGroup.create("TUSK", () -> TuskAbility.CORES);
        CHANNEL.registerMessage(0, TuskPacket.class, TuskPacket::encode, TuskPacket::decode, TuskPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            FruitAbilityInjector.addAbilities(CartAbilities.GURU_GURU_NO_MI, TuskAbility.CORES);
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(TuskFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(TuskFeatures::rotationTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TuskFeatures::beforeUse);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (UnlockAbilityEvent event) -> {
            if (blocked(event.getEntityLiving(), event.getAbilityCore())) event.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, TuskFeatures::dodge);
        MinecraftForge.EVENT_BUS.addListener(TuskFeatures::clonePlayer);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> { clearRuntime(event.getPlayer()); reconcile(event.getPlayer()); sync(event.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> { reconcile(event.getPlayer()); sync(event.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> clearRuntime(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> { clearRuntime(event.getPlayer()); sync(event.getPlayer()); });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntityLiving() instanceof PlayerEntity) clearRuntime((PlayerEntity) event.getEntityLiving());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getPlayer() instanceof ServerPlayerEntity && event.getTarget() instanceof PlayerEntity
                    && event.getTarget().getPersistentData().contains(TuskState.TAG, 10))
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()), packet((PlayerEntity) event.getTarget()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TuskClient.init(bus));
    }

    public static boolean original(AbilityCore<?> core) {
        if (core == null || TuskAbility.isTusk(core)) return false;
        if (core == GuruFlightAbility.INSTANCE || core == GuruRideableAbility.INSTANCE || core == KazekiriAbility.INSTANCE
                || core == TashahoAbility.INSTANCE || core == ToppuMatasaburoAbility.INSTANCE) return true;
        for (AbilityCore<?> old : CartAbilities.GURU_GURU_NO_MI.getAbilities()) if (old == core) return true;
        return false;
    }
    public static boolean blocked(LivingEntity user, AbilityCore<?> core) { return TuskState.awakened(user) && original(core); }

    public static boolean awaken(PlayerEntity player, ItemStack essence) {
        if (player.level.isClientSide || !xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasAwakeningsEnabled()
                || !TuskState.hasFruit(player) || TuskState.awakened(player) || essence.isEmpty()
                || !(essence.getItem() instanceof net.kazi.kazimod.items.AwakeningEssenceItem)) return false;
        DevilFruitCapability.get(player).setAwakenedFruit(true); TuskState.initialize(player);
        if (!player.isCreative()) essence.shrink(1);
        TuskState.pose(player, 1, 40); reconcile(player); sync(player);
        WyNetwork.sendTo(new SSyncDevilFruitPacket(player.getId(), DevilFruitCapability.get(player)), player);
        player.displayClientMessage(new TranslationTextComponent("message.kazimod.tusk.awakened"), false);
        play(player, SoundEvents.BEACON_ACTIVATE, .8F, 1.3F); return true;
    }

    /** Uses a second Awakening Essence to unlock the remaining Tusk Acts. */
    public static boolean skipMastery(PlayerEntity player, ItemStack essence) {
        if (player.level.isClientSide || !xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasAwakeningsEnabled()
                || !TuskState.awakened(player) || TuskState.tier(player) >= 4 || essence.isEmpty()
                || !(essence.getItem() instanceof net.kazi.kazimod.items.AwakeningEssenceItem)) return false;
        CompoundNBT state = TuskState.get(player);
        state.putInt("Tier", 4); state.putInt("Act", 4); state.putBoolean("Manifest", true);
        state.putFloat("Nails", 10); TuskState.pose(player, 1, 40);
        if (!player.isCreative()) essence.shrink(1);
        reconcile(player); sync(player);
        player.displayClientMessage(new TranslationTextComponent("message.kazimod.tusk.mastery_skipped"), false);
        play(player, SoundEvents.BEACON_POWER_SELECT, .8F, 1.15F); return true;
    }

    public static void reconcile(PlayerEntity player) {
        if (player.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(player); boolean awakened = TuskState.awakened(player), changed = false;
        if (awakened) TuskState.initialize(player);
        Set<AbilityCore<?>> cores = new HashSet<>();
        for (AbilityCoreUnlockWrapper<?> wrapper : data.getUnlockedAbilities()) cores.add(wrapper.getAbilityCore());
        for (IAbility ability : data.getEquippedAndPassiveAbilities()) cores.add(ability.getCore());
        List<Integer> vacated = new ArrayList<>();
        for (AbilityCore<?> core : cores) {
            if (!(awakened ? original(core) : TuskAbility.isTusk(core))) continue;
            IAbility equipped = data.getEquippedAbility(core), passive = data.getPassiveAbility(core);
            if (equipped != null) { vacated.add(data.getEquippedAbilitySlot(equipped)); AbilityHelper.emergencyStopAbility(player, equipped); }
            if (passive != null) AbilityHelper.emergencyStopAbility(player, passive);
            data.removeEquippedAbility(core); data.removePassiveAbility(core); data.removeUnlockedAbility(core); changed = true;
        }
        if (awakened) {
            for (AbilityCore<TuskAbility> core : TuskAbility.CORES) if (core.canUnlock(player) && !data.hasUnlockedAbility(core)) {
                data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION); changed = true;
            }
            // Replace only vacated Guru slots; preserve the player's Haki/style loadout.
            Collections.sort(vacated);
            int index = 0;
            for (TuskAbility.Move move : new TuskAbility.Move[]{TuskAbility.Move.NAIL, TuskAbility.Move.SHOTGUN, TuskAbility.Move.GLIDE, TuskAbility.Move.MANIFEST}) {
                AbilityCore<TuskAbility> core = TuskAbility.core(move);
                if (data.getEquippedAbility(core) != null) continue;
                while (index < vacated.size() && data.getEquippedAbility(vacated.get(index)) != null) index++;
                if (index < vacated.size()) data.setEquippedAbility(vacated.get(index++), core.createAbility());
            }
        } else if (player.getPersistentData().contains(TuskState.TAG, 10)) {
            TuskState.get(player).putBoolean("Active", false); TuskState.get(player).putLong("ActionEnd", 0);
            TuskState.get(player).putLong("DodgeUntil", 0);
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }

    private static void beforeUse(AbilityUseEvent.Pre event) {
        LivingEntity user = event.getEntityLiving();
        if (blocked(user, event.getAbility().getCore()) || user.getPersistentData().getLong("KaziTuskGrabUntil") > user.level.getGameTime()) {
            event.setCanceled(true); return;
        }
        if (!TuskState.awakened(user)) return;
        int action = TuskState.action(user);
        if (action == 7 || action == 9 || action == 10 || action == 11) {
            TuskAbility.Move allowed = action == 7 ? TuskAbility.Move.SLIP : action == 9 ? TuskAbility.Move.BARRAGE
                    : action == 10 ? TuskAbility.Move.BEATDOWN : TuskAbility.Move.INFINITE;
            if (event.getAbility().getCore() != TuskAbility.core(allowed)) event.setCanceled(true);
        }
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        PlayerEntity p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level.isClientSide) return;
        if (!TuskState.hasFruit(p) && !p.getPersistentData().contains(TuskState.TAG, 10)) return;
        if (p.tickCount % 10 == 0) reconcile(p);
        if (!TuskState.awakened(p)) {
            if (p.tickCount % 20 == 0 && p.getPersistentData().contains(TuskState.TAG, 10)) sync(p);
            return;
        }
        TuskState.initialize(p); CompoundNBT n = TuskState.get(p);
        boolean active = p.isAlive() && !p.isSpectator() && !AbilityHelper.isWeakenedByKairosekiOrWater(p);
        n.putBoolean("Active", active);
        if (active) {
            float regen = TuskState.act(p) == 1 ? .025F : TuskState.act(p) == 4 ? .02F : 1F / 60;
            n.putFloat("Nails", Math.min(10, TuskState.nails(p) + regen));
            double distance = Math.hypot(p.getX() - n.getDouble("LastX"), p.getZ() - n.getDouble("LastZ"));
            if (n.getLong("CombatUntil") > p.level.getGameTime() && p.isSprinting() && distance > .08 && distance < 1.5)
                TuskState.addSpin(p, .16F);
            else if (n.getLong("CombatUntil") + 200 < p.level.getGameTime()) TuskState.addSpin(p, -.025F);
        } else { n.putLong("DodgeUntil", 0); n.putLong("ActionEnd", 0); }
        n.putDouble("LastX", p.getX()); n.putDouble("LastZ", p.getZ());
        if (p.tickCount % 5 == 0) sync(p);
    }

    public static void landed(LivingEntity user, LivingEntity target, int kind, boolean distant) {
        if (!TuskState.awakened(user) || target == user) return;
        TuskState.get(user).putLong("CombatUntil", user.level.getGameTime() + 200);
        TuskState.addSpin(user, kind == TuskProjectile.INFINITE ? 0 : 5);
        if (kind == TuskProjectile.HOLE) TuskState.progress(user, "HoleHits");
        else if (kind == TuskProjectile.WORMHOLE) TuskState.progress(user, "WormholeHits");
        else if (kind == TuskProjectile.NAIL) { TuskState.progress(user, "NailHits"); if (distant) TuskState.progress(user, "LongShots"); }
    }

    private static void dodge(LivingAttackEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || !TuskState.awakened(user) || event.getAmount() <= 0 || !user.isAlive()
                || AbilityHelper.isWeakenedByKairosekiOrWater(user) || user.isSpectator()) return;
        DamageSource source = event.getSource();
        if (source.getEntity() == null || source.isBypassInvul() || source instanceof ModDamageSource && ((ModDamageSource) source).isUnavoidable()) return;
        CompoundNBT n = TuskState.get(user);
        if (n.getLong("DodgeUntil") <= user.level.getGameTime()) return;
        event.setCanceled(true);
        if (!n.getBoolean("DodgeCredited")) {
            n.putBoolean("DodgeCredited", true); TuskState.progress(user, "Dodges");
            TuskEffect.spawn(user, TuskEffect.PORTAL, user.position(), 16, 1.1F);
            play(user, SoundEvents.ENDERMAN_TELEPORT, .35F, 1.65F);
        }
    }

    public static boolean applyRotation(LivingEntity owner, LivingEntity target) {
        CompoundNBT root = target.getPersistentData();
        if (root.contains(ROTATION, 10) && root.getCompound(ROTATION).getLong("Until") > target.level.getGameTime()) return false;
        CompoundNBT n = new CompoundNBT(); n.putUUID("Owner", owner.getUUID());
        n.putLong("Until", target.level.getGameTime() + 200); n.putLong("Next", target.level.getGameTime() + 20);
        root.put(ROTATION, n); TuskEffect.spawn(owner, TuskEffect.ROTATION, target.position(), 200, target.getBbHeight()).attach(target); return true;
    }
    private static void rotationTick(LivingEvent.LivingUpdateEvent event) {
        LivingEntity target = event.getEntityLiving();
        if (target.level.isClientSide || !target.getPersistentData().contains(ROTATION, 10)) return;
        CompoundNBT n = target.getPersistentData().getCompound(ROTATION); long now = target.level.getGameTime();
        if (now > n.getLong("Until") || !target.isAlive() || !n.hasUUID("Owner")) { target.getPersistentData().remove(ROTATION); return; }
        if (now < n.getLong("Next")) return;
        n.putLong("Next", now + 20);
        PlayerEntity owner = target.level.getPlayerByUUID(n.getUUID("Owner"));
        if (owner == null || !TuskState.awakened(owner) || !owner.isAlive()) return;
        if (!canHit(owner, target) || AbilityHelper.isWeakenedByKairosekiOrWater(owner)) return;
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 25, 1, false, false));
        DealDamageComponent damage = new DealDamageComponent(TuskAbility.core(TuskAbility.Move.INFINITE).createAbility());
        damage.hurtTarget(owner, target, 4);
    }

    public static boolean canHit(LivingEntity user, LivingEntity target) {
        return net.kazi.kazimod.saiyan.SaiyanAbility.canHit(user, target)
                && !net.kazi.kazimod.mahoraga.MahoragaFeatures.allied(user, target);
    }
    public static LivingEntity aim(LivingEntity user, double range) {
        Vector3d start = user.getEyePosition(1), end = start.add(user.getLookAngle().scale(range));
        Vector3d stop = user.level.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user)).getLocation();
        LivingEntity best = null; double distance = start.distanceToSqr(stop);
        for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(start, stop).inflate(1))) {
            if (!canHit(user, target)) continue;
            Optional<Vector3d> hit = target.getBoundingBox().inflate(.45).clip(start, stop);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < distance) { best = target; distance = hit.get().distanceToSqr(start); }
        }
        return best;
    }
    public static boolean safe(LivingEntity user, Vector3d at) {
        BlockPos pos = new BlockPos(at);
        if (!Double.isFinite(at.lengthSqr()) || at.y < 1 || at.y + user.getBbHeight() >= user.level.getMaxBuildHeight()
                || !user.level.hasChunkAt(pos) || !user.level.getWorldBorder().isWithinBounds(pos)
                || !user.level.noCollision(user, user.getBoundingBox().move(at.subtract(user.position())))) return false;
        xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea area =
                xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData.get(user.level).getProtectedArea(pos.getX(), pos.getY(), pos.getZ());
        return area == null || area.canHurtEntities();
    }
    public static void teleport(LivingEntity user, Vector3d at) {
        user.teleportTo(at.x, at.y, at.z); user.fallDistance = 0; AbilityHelper.setDeltaMovement(user, Vector3d.ZERO);
    }
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(TuskState.TAG, 10))
            event.getPlayer().getPersistentData().put(TuskState.TAG, TuskState.get(event.getOriginal()).copy());
        clearRuntime(event.getPlayer());
    }
    private static void clearRuntime(PlayerEntity p) {
        if (!p.getPersistentData().contains(TuskState.TAG, 10)) return;
        if (!p.level.isClientSide) for (IAbility ability : new ArrayList<>(AbilityDataCapability.get(p).getEquippedAndPassiveAbilities()))
            if (TuskAbility.isTusk(ability.getCore())) AbilityHelper.emergencyStopAbility(p, ability);
        CompoundNBT n = TuskState.get(p); n.putLong("ActionEnd", 0); n.putLong("DodgeUntil", 0); n.putBoolean("Active", false);
        n.putLong("CombatUntil", 0); n.putDouble("LastX", p.getX()); n.putDouble("LastZ", p.getZ());
    }
    private static TuskPacket packet(PlayerEntity p) {
        CompoundNBT data = new CompoundNBT(), source = p.getPersistentData().getCompound(TuskState.TAG);
        for (String key : new String[]{"Tier", "Act", "Nails", "Spin", "Manifest", "Action", "ActionStart", "ActionEnd",
                "NailHits", "LongShots", "HoleHits", "Bursts", "WormholeHits", "Dodges"})
            if (source.contains(key)) data.put(key, source.get(key).copy());
        data.putBoolean("Active", TuskState.awakened(p) && p.isAlive() && !p.isSpectator() && !AbilityHelper.isWeakenedByKairosekiOrWater(p));
        return new TuskPacket(p.getId(), data);
    }
    public static void sync(PlayerEntity p) {
        if (p instanceof ServerPlayerEntity) CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packet(p));
    }
    public static void play(LivingEntity user, SoundEvent sound, float volume, float pitch) {
        user.level.playSound(null, user.blockPosition(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
    private TuskFeatures() {}
}
