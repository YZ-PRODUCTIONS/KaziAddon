package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.entities.asriel.AsrielLightningEntity;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.kazi.kazimod.init.KaziAnimations;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PauseTickComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/** Five marking rounds normally, or Chaos Beam at 10% health or lower. */
public final class ShockerBreakerAbility extends Ability {
    public static final float COOLDOWN_TICKS = 320.0F;
    public static final float CHARGE_TICKS = ShockerBreakerVisualTiming.CHARGE_TICKS;
    private static final ResourceLocation BASE_ICON = new ResourceLocation("kazimod", "textures/abilities/shocker_breaker.png");
    private static final ResourceLocation ENHANCED_ICON = new ResourceLocation("kazimod", "textures/abilities/chaos_beam.png");
    public static final AbilityCore<ShockerBreakerAbility> INSTANCE = new AbilityCore.Builder<>(
            "Shocker Breaker", AbilityCategory.DEVIL_FRUITS, ShockerBreakerAbility::new)
            .setUnlockCheck(YagiAwakeningSupport::canUnlock)
            .setIcon(BASE_ICON)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.shocker_breaker.description.0"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    ChargeComponent.getTooltip(CHARGE_TICKS), CooldownComponent.getTooltip(COOLDOWN_TICKS))
            .setSourceElement(SourceElement.LIGHTNING)
            .setSourceHakiNature(SourceHakiNature.SPECIAL)
            .build();

    private final ChargeComponent charge = new ChargeComponent(this, false)
            .addStartEvent((user, ability) -> {
                if (user.level.isClientSide) castEnhanced = usesChaosBeam(user);
                updateName(user);
            }).addEndEvent(this::release);
    private final AnimationComponent animation = new AnimationComponent(this);
    private final DamageTakenComponent protection = new DamageTakenComponent(this)
            .addOnAttackEvent(this::blockDamageDuringBeam);
    private final ContinuousComponent beam = new ContinuousComponent(this, false)
            .addStartEvent((user, ability) -> castEnhanced = true)
            .addTickEvent(this::tickBeam).addEndEvent((user, ability) -> finishBeam(user));
    private final List<AsrielLightningEntity> markers = new ArrayList<>();
    private boolean castEnhanced;
    private boolean cancelled;
    private ChaosBeamAttackEntity chaosBeam;

    public ShockerBreakerAbility(AbilityCore<ShockerBreakerAbility> core) {
        super(core);
        this.isNew = true;
        addComponents(charge, animation, beam, protection);
        addUseEvent(this::start);
        addEquipEvent((user, ability) -> updateName(user));
        addTickEvent((user, ability) -> {
            updateName(user);
            if (!user.level.isClientSide && !markers.isEmpty()
                    && (isDisabled() || !charge.isCharging()
                        || markers.stream().anyMatch(marker -> marker.level != user.level))) cancel(user);
            if (!user.level.isClientSide && chaosBeam != null
                    && (!canContinue(user) || chaosBeam.removed || chaosBeam.level != user.level
                        || !charge.isCharging() && !beam.isContinuous())) cancel(user);
        });
        addRemoveEvent((user, ability) -> cancel(user));
    }

    private static boolean usesChaosBeam(LivingEntity user) {
        return user != null && YagiAwakeningMath.usesChaosBeam(user.getHealth(), user.getMaxHealth());
    }

    private void updateName(LivingEntity user) {
        boolean enhanced = charge.isCharging() || beam.isContinuous() ? castEnhanced : usesChaosBeam(user);
        setDisplayName(new TranslationTextComponent(enhanced
                ? "ability.kazimod.chaos_beam" : "ability.kazimod.shocker_breaker"));
        setDisplayIcon(enhanced ? ENHANCED_ICON : BASE_ICON);
    }

    private void start(LivingEntity user, IAbility ability) {
        if (!(user.level instanceof ServerWorld) || charge.isCharging() || beam.isContinuous()) return;
        castEnhanced = usesChaosBeam(user);
        cancelled = false;
        markers.clear();
        if (castEnhanced) {
            chaosBeam = ChaosBeamAttackEntity.spawn(user);
            if (chaosBeam == null) return;
        } else {
            AsrielLightningEntity aura = AsrielLightningEntity.chargeAura(user.level, user);
            if (user.level.addFreshEntity(aura)) {
                markers.add(aura);
                aura.startRegularSequence(user);
            }
        }
        charge.startCharging(user, castEnhanced ? ChaosBeamPattern.CHARGE_TICKS : CHARGE_TICKS);
        if (castEnhanced) {
            animation.start(user, KaziAnimations.CHAOS_BEAM,
                    ChaosBeamPattern.CHARGE_TICKS + ChaosBeamPattern.FIRE_TICKS);
            if (charge.isCharging() && chaosBeam != null && !chaosBeam.removed) ChaosBeamAnnouncement.send(user);
        } else animation.start(user, ModAnimations.RAISE_RIGHT_ARM, (int) CHARGE_TICKS);
        updateName(user);
        if (!castEnhanced) ShockerBreakerSounds.cast(user);
    }

    private void release(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) {
            if (!castEnhanced) animation.stop(user);
            return;
        }
        boolean finished = !cancelled && charge.getChargePercentage() >= 0.99F && canContinue(user);
        if (castEnhanced) {
            if (finished && chaosBeam != null && !chaosBeam.removed && chaosBeam.level == user.level) {
                chaosBeam.beginBeam();
                // Recheck the field after firing callbacks before starting continuity.
                if (!cancelled && canContinue(user) && chaosBeam != null && !chaosBeam.removed)
                    beam.startContinuity(user, ChaosBeamPattern.FIRE_TICKS);
                else finishBeam(user);
            } else finishBeam(user);
            return;
        }
        animation.stop(user);
        for (AsrielLightningEntity marker : markers) {
            if (finished) marker.arm();
            else marker.cancelCast();
        }
        markers.clear();
        cooldownComponent.startCooldown(user, COOLDOWN_TICKS);
    }

    private void cancel(LivingEntity user) {
        cancelled = true;
        for (AsrielLightningEntity marker : markers) marker.cancelCast();
        markers.clear();
        if (chaosBeam != null) { chaosBeam.remove(); chaosBeam = null; }
        animation.stop(user);
        if (beam.isContinuous()) beam.stopContinuity(user);
        if (charge.isCharging()) charge.forceStopCharging(user);
    }

    private boolean canContinue(LivingEntity user) {
        return user.isAlive() && !user.removed && !isDisabled()
                && !getComponent(ModAbilityKeys.PAUSE_TICK).map(PauseTickComponent::isPaused).orElse(false);
    }

    /** Continuous Kami-e-style attack cancellation, scoped to the live Chaos Beam cast. */
    private float blockDamageDuringBeam(LivingEntity user, IAbility ability, DamageSource source, float damage) {
        if (ChaosBeamRecoil.isRecoil(source)) return damage;
        if (!user.level.isClientSide && castEnhanced && canContinue(user)
                && chaosBeam != null && chaosBeam.level == user.level
                && chaosBeam.getCasterId() == user.getId() && chaosBeam.hasActiveAimLock()
                && ownsChaosBeam(chaosBeam)) return 0.0F;
        return damage;
    }

    private void tickBeam(LivingEntity user, IAbility ability) {
        if (!user.level.isClientSide && (!canContinue(user) || chaosBeam == null || chaosBeam.removed
                || chaosBeam.level != user.level)) beam.stopContinuity(user);
    }

    private void finishBeam(LivingEntity user) {
        animation.stop(user);
        if (user.level.isClientSide) return;
        if (chaosBeam != null) {
            if (!cancelled && canContinue(user)) chaosBeam.finishFiring(user);
            chaosBeam.remove();
            chaosBeam = null;
        }
        cooldownComponent.startCooldown(user, COOLDOWN_TICKS);
    }

    public boolean ownsChaosBeam(ChaosBeamAttackEntity field) {
        return chaosBeam == field && !cancelled && !isDisabled()
                && (charge.isCharging() || beam.isContinuous())
                && !getComponent(ModAbilityKeys.PAUSE_TICK).map(PauseTickComponent::isPaused).orElse(false);
    }

    /** Release before player data is saved, and recover a lock left by an interrupted shutdown. */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "kazimod")
    public static final class MovementCleanup {
        private static void cleanup(LivingEntity user) {
            if (user.level.isClientSide) return;
            xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                    xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability.get(user);
            ShockerBreakerAbility ability = data == null ? null : data.getEquippedAbility(INSTANCE);
            if (ability != null && ability.chaosBeam != null) ability.cancel(user);
            ChaosBeamAttackEntity.clearMovementLock(user);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) { cleanup(event.getPlayer()); }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) { cleanup(event.getPlayer()); }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) { cleanup(event.getPlayer()); }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) { cleanup(event.getEntityLiving()); }
    }
}
