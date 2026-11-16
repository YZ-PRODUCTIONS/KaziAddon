package net.kazi.kazimod.abilities.Yagi;

import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.abilities.FlyAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.packets.server.SFlightValuePacket;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SUpdatePassiveAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Gasu's passive flight controls and speed with an astral hovering animation. */
public final class AstralFlightAbility extends FlyAbility {
    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/astral_flight.png");
    public static final AbilityCore<AstralFlightAbility> INSTANCE = new AbilityCore.Builder<>(
            "Astral Flight", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, AstralFlightAbility::new)
            .setIcon(ICON)
            .setUnlockCheck(AstralFlightAbility::canUnlock)
            .addDescriptionLine(new StringTextComponent("Double-tap jump to fly. Use jump and sneak to rise and descend. Rainbow astral afterimages remain visible even when grounded."),
                    new StringTextComponent("Matches Gasu's flight speed, including its 30% speed increase outside combat and 40-block flight height limit."),
                    new StringTextComponent("Below 50% health, rainbow wings remain visible even when you are not flying."))
            .build();

    private boolean flying;
    private Boolean flightPermission;
    private boolean passiveRanThisTick;

    public AstralFlightAbility(AbilityCore<AstralFlightAbility> core) {
        super(core);
        setDisplayName(new StringTextComponent("Astral Flight"));
        setDisplayIcon(ICON);
        addCanUseCheck((user, ability) -> user.isAlive() && canUnlock(user)
                && !user.hasEffect(ModEffects.ABILITY_OFF.get())
                && !AbilityHelper.isWeakenedByKairosekiOrWater(user)
                ? AbilityUseResult.success() : AbilityUseResult.fail(null));
        addRemoveEvent((user, ability) -> {
            animation().stop(user);
            if (user instanceof PlayerEntity && !user.level.isClientSide) {
                PlayerEntity player = (PlayerEntity) user;
                updateFlying(player, false);
                // FlyAbility restores the flight flags captured when the passive was equipped.
                if (!player.isCreative() && !player.isSpectator()) {
                    player.onUpdateAbilities();
                    WyNetwork.sendTo(new SFlightValuePacket(player.abilities.mayfly), player);
                }
                flightPermission = null;
            }
        });
    }

    public static boolean canUnlock(LivingEntity user) {
        if (user == null || KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT == null
                || !KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT.isPresent()) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasAwakenedFruit()
                && fruit.hasDevilFruit(KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT.get());
    }

    /** Uses the server-synchronized passive state so other players see the same trail. */
    public static boolean isFlying(LivingEntity user) {
        if (user == null || !user.isAlive() || user.isSpectator()) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        AstralFlightAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        return passive != null && passive.flying && !passive.isPaused()
                && !passive.disableComponent.isDisabled() && canUnlock(user);
    }

    /** Afterimages depend on the passive, never on the flight toggle or grounded state. */
    public static boolean hasAfterimages(LivingEntity user) {
        if (user == null || !user.isAlive() || user.isSpectator() || !canUnlock(user)) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        AstralFlightAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        return passive != null && !passive.isPaused() && !passive.disableComponent.isDisabled();
    }

    /** Low-health wings share the afterimages' passive eligibility. */
    public static boolean hasRainbowWings(LivingEntity user) {
        return hasAfterimages(user)
                && net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath.enhanced(user.getHealth(), user.getMaxHealth());
    }

    @Override public void tick(LivingEntity user) {
        passiveRanThisTick = false;
        super.tick(user);
        if (!(user instanceof PlayerEntity) || user.level.isClientSide) return;
        PlayerEntity player = (PlayerEntity) user;
        boolean allowed = passiveRanThisTick && canUse(player).isSuccess()
                && AbilityHelper.canUseMomentumAbilities(player);
        if (!player.isCreative() && !player.isSpectator()) setFlightPermission(player, allowed);
        boolean airborne = allowed && player.abilities.flying && !player.isOnGround();
        updateFlying(player, airborne);
        if (airborne) player.fallDistance = 0;
        if (airborne && canAnimate(player)) animation().start(player, KaziAnimations.ASTRAL_FLIGHT);
        else animation().stop(player);
    }

    @Override public void duringPassiveEvent(LivingEntity user) {
        if (!(user instanceof PlayerEntity)) return;
        passiveRanThisTick = true;
        PlayerEntity player = (PlayerEntity) user;
        if (!player.level.isClientSide) setFlightPermission(player, true);
        if (!player.abilities.flying) return;
        // Exact Gasu/FlyAbility movement damping; vanilla flight supplies the directional input.
        float speed = getSpeedMultiplier(player);
        AbilityHelper.setDeltaMovement(player, player.getDeltaMovement().multiply(speed, speed - 0.25F, speed));
        if (!player.level.isClientSide && !CommonConfig.INSTANCE.hasYRestrictionRemoved()) {
            boolean atMaxHeight = DevilFruitHelper.flyingAtMaxHeight(player, getHeightDifference(player));
            DevilFruitHelper.vanillaFlightThreshold(player, atMaxHeight ? 256 : (int) player.getY() - 1);
        }
    }

    private AnimationComponent animation() { return getComponent(ModAbilityKeys.ANIMATION).get(); }

    private boolean canAnimate(PlayerEntity player) {
        for (IAbility other : AbilityDataCapability.get(player).getEquippedAndPassiveAbilities()) {
            if (other != null && other != this && other.getComponent(ModAbilityKeys.ANIMATION)
                    .map(AnimationComponent::isPlaying).orElse(false)) return false;
        }
        return true;
    }

    private void setFlightPermission(PlayerEntity player, boolean allowed) {
        boolean changed = player.abilities.mayfly != allowed || !allowed && player.abilities.flying;
        player.abilities.mayfly = allowed;
        if (!allowed) player.abilities.flying = false;
        if (changed) player.onUpdateAbilities();
        if (flightPermission == null || flightPermission != allowed) {
            flightPermission = allowed;
            WyNetwork.sendTo(new SFlightValuePacket(allowed), player);
        }
    }

    private void updateFlying(PlayerEntity player, boolean value) {
        if (flying == value) return;
        flying = value;
        WyNetwork.sendToAllTrackingAndSelf(new SUpdatePassiveAbilityDataPacket(player, this), player);
    }

    @Override public int getHeightDifference(PlayerEntity player) { return 40; }
    @Override public float getSpeedMultiplier(PlayerEntity player) {
        float speed = 0.75F;
        if (!WyHelper.isInCombat(player)) speed += speed * 0.3F;
        return speed;
    }

    // FlyAbility requires this supplier; our overridden passive event renders no Gasu particles.
    @Override public Supplier<ParticleEffect<?>> getParticleEffects() { return ModParticleEffects.GASU_FLY; }

    @Override public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putBoolean("AstralFlying", flying);
        return nbt;
    }
    @Override public void load(CompoundNBT nbt) {
        super.load(nbt);
        flying = nbt.getBoolean("AstralFlying");
    }
}
