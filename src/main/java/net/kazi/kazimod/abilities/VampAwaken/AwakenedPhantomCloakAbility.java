package net.kazi.kazimod.abilities.VampAwaken;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.particles.effects.battovampire.PhantomVeilParticleEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

/** Cart's Phantom Cloak and Phantom Veil combined as selectable modes. */
public class AwakenedPhantomCloakAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "phantom_cloak",
            new Pair[]{ImmutablePair.of("Switch between a short invisibility cloak and a damaging defensive smoke veil.", null)});
    private static final int VEIL_ON_HOLD = 100;
    private static final int VEIL_MIN_COOLDOWN = 200;
    private static final double VEIL_RANGE = 20.0D;
    private static final float VEIL_DAMAGE = 10.0F;
    private static final ResourceLocation CLOAK_ICON =
            new ResourceLocation("cartaddon", "textures/abilities/phantom_cloak.png");
    private static final ResourceLocation VEIL_ICON =
            new ResourceLocation("cartaddon", "textures/abilities/phantom_veil.png");
    private static final TranslationTextComponent CLOAK_NAME = new TranslationTextComponent(
            WyRegistry.registerName("ability.kazimod.phantom_cloak", "Phantom Cloak"));
    private static final TranslationTextComponent VEIL_NAME = new TranslationTextComponent(
            WyRegistry.registerName("ability.kazimod.phantom_veil", "Phantom Veil"));

    public static final AbilityCore<AwakenedPhantomCloakAbility> INSTANCE;

    private Mode currentMode = Mode.CLOAK;
    private final AltModeComponent<Mode> altModeComponent;
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);
    private final Interval clearHitsInterval = new Interval(20);
    private final DamageTakenComponent damageTakenComponent =
            new DamageTakenComponent(this, this::damageTakenEvent, DamageTakenComponent.DamageState.HURT);

    public AwakenedPhantomCloakAbility(AbilityCore<AwakenedPhantomCloakAbility> core) {
        super(core);
        this.altModeComponent = new AltModeComponent<>(this, Mode.class, Mode.CLOAK)
                .addChangeModeEvent(this::onAltModeChange);
        this.isNew = true;
        this.addComponents(this.altModeComponent, this.continuousComponent, this.hitTrackerComponent,
                this.dealDamageComponent, this.damageTakenComponent);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.VEIL) {
            this.continuousComponent.triggerContinuity(entity, VEIL_ON_HOLD);
        } else {
            this.continuousComponent.triggerContinuity(entity, 100.0F);
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        this.currentMode = mode;
        switch (mode) {
            case VEIL:
                this.setDisplayName(VEIL_NAME);
                this.setDisplayIcon(VEIL_ICON);
                break;
            case CLOAK:
            default:
                this.setDisplayName(CLOAK_NAME);
                this.setDisplayIcon(CLOAK_ICON);
                break;
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (this.currentMode != Mode.VEIL) return;
        this.hitTrackerComponent.clearHits();
        this.particleInterval.restartIntervalToZero();
        ((ModDamageSource) this.dealDamageComponent.getDamageSource(entity)).setUnavoidable();
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.CLOAK) {
            entity.addEffect(new EffectInstance((Effect) ModEffects.VANISH.get(), 2, 0, false, false));
            return;
        }

        entity.addEffect(new EffectInstance((Effect) ModEffects.VANISH.get(), 5, 0, false, false));
        if (super.canUse(entity).isFail()) {
            this.continuousComponent.stopContinuity(entity);
        }
        if (this.particleInterval.canTick()) {
            WyHelper.spawnParticleEffect((ParticleEffect<?>) CartParticleEffects.PHANTOM_VAIL.get(), entity,
                    entity.getX(), entity.getY(), entity.getZ(),
                    new PhantomVeilParticleEffect.Details(5.0F, 4.0F, 4.0F));
        }
        for (LivingEntity target : WyHelper.getNearbyLiving(entity.position(), entity.level, VEIL_RANGE,
                10.0D, VEIL_RANGE, ModEntityPredicates.getEnemyFactions(entity))) {
            if (this.hitTrackerComponent.canHit(target)
                    && this.dealDamageComponent.hurtTarget(entity, target, VEIL_DAMAGE)) {
                target.addEffect(new EffectInstance((Effect) ModEffects.BLEEDING.get(), 100, 0));
                target.addEffect(new EffectInstance(Effects.CONFUSION, 100, 1));
                if (!(target instanceof PlayerEntity)) {
                    target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 15, 0, false, false));
                }
            }
        }
        if (this.clearHitsInterval.canTick()) this.hitTrackerComponent.clearHits();
        AbilityHelper.slowEntityFall(entity);
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.VEIL) {
            float cooldown = VEIL_MIN_COOLDOWN + this.continuousComponent.getContinueTime() / 2.0F;
            this.cooldownComponent.startCooldown(entity, cooldown);
        } else {
            this.cooldownComponent.startCooldown(entity, 300.0F);
        }
    }

    private float damageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) return damage;
        return this.currentMode == Mode.VEIL && this.continuousComponent.isContinuous()
                ? damage * 0.5F : damage;
    }

    private enum Mode { CLOAK, VEIL }

    private static boolean canUnlock(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Phantom Cloak", AbilityCategory.DEVIL_FRUITS, AwakenedPhantomCloakAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(60.0F),
                        CooldownComponent.getTooltip(300.0F))
                .setIcon(CLOAK_ICON)
                .setUnlockCheck(AwakenedPhantomCloakAbility::canUnlock)
                .build();
    }
}
