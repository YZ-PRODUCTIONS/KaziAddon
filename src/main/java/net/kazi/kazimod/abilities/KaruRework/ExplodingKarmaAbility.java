//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KaruRework;

import java.awt.Color;
import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import xyz.pixelatedw.mineminenomi.abilities.karu.KarmaAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityStat;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityStat.AbilityStatType;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunctionHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModI18n;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;
import net.kazi.kazimod.init.KaziEffects;
import net.kazi.kazimod.entities.projectiles.KarmaProjectiles;
import net.kazi.kazimod.entities.projectiles.KarmaExplosionProjectile;

public class ExplodingKarmaAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "exploding_karma", new Pair[]{ImmutablePair.of("Channels karma energy into a massive red sphere above the user that detonates after charging, dealing devastating damage in a massive area. Scales with karma. Requires Inga Zarashi to be active and at least 75 karma to use.", (Object)null)});
    private static final TranslationTextComponent EXPLODING_KARMA_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.exploding_karma", "Exploding Karma"));
    private static final ResourceLocation EXPLODING_KARMA_ICON = new ResourceLocation("mineminenomi", "textures/abilities/exploding_karma.png");
    private static final float COOLDOWN = 1000.0F;
    private static final float CHARGE_TIME = 160.0F; // 8 seconds (160 ticks) - NON-CANCELLABLE

    // Scaling values based on karma (75-100)
    private static final float MIN_KARMA_REQUIRED = 75.0F;
    private static final float MIN_AOE_RADIUS = 40.0F; // at 75 karma
    private static final float MAX_AOE_RADIUS = 75.0F; // at 100 karma
    private static final float MIN_DAMAGE = 150.0F; // at 75 karma
    private static final float MAX_DAMAGE = 200.0F; // at 100 karma

    private static final AbilityDescriptionLine.IDescriptionLine<ExplodingKarmaAbility> DAMAGE_TOOLTIP = (entity, ability) -> {
        IAbilityData props = AbilityDataCapability.get(entity);
        KarmaAbility karma = (KarmaAbility)props.getPassiveAbility(KarmaAbility.INSTANCE);
        if (karma != null) {
            float currentKarma = karma.getKarma();
            float damage = ability.calculateScaledDamage(currentKarma);
            return new StringTextComponent("§cDamage: §r" + String.format("%.1f", damage) + " (at " + String.format("%.1f", currentKarma) + " karma)");
        }
        return new StringTextComponent("§cDamage: §r" + MIN_DAMAGE + " - " + MAX_DAMAGE);
    };

    private static final AbilityDescriptionLine.IDescriptionLine<ExplodingKarmaAbility> AOE_TOOLTIP = (entity, ability) -> {
        IAbilityData props = AbilityDataCapability.get(entity);
        KarmaAbility karma = (KarmaAbility)props.getPassiveAbility(KarmaAbility.INSTANCE);
        if (karma != null) {
            float currentKarma = karma.getKarma();
            float aoe = ability.calculateScaledAOE(currentKarma);
            return new StringTextComponent("§cAOE Radius: §r" + String.format("%.1f", aoe) + " blocks (at " + String.format("%.1f", currentKarma) + " karma)");
        }
        return new StringTextComponent("§cAOE Radius: §r" + MIN_AOE_RADIUS + " - " + MAX_AOE_RADIUS + " blocks");
    };

    public static final AbilityCore<ExplodingKarmaAbility> INSTANCE;

    private Optional<KarmaAbility> karmaAbility = Optional.empty();
    private double usedKarma = 0.0; // Captured karma value when ability is used
    private float lockedAOE = 0.0F; // Locked AOE based on karma
    private float lockedDamage = 0.0F; // Locked damage based on karma

    private final ChargeComponent chargeComponent;
    private final AnimationComponent animationComponent;
    private final HitTrackerComponent hitTrackerComponent;
    private final DealDamageComponent dealDamageComponent;
    private final ProjectileComponent projectileComponent;
    private final Interval particleInterval;
    private KarmaExplosionProjectile karmaProjectile;
    private SphereEntity sphereEntity; // Visual AOE indicator

    public ExplodingKarmaAbility(AbilityCore<ExplodingKarmaAbility> core) {
        super(core);
        this.chargeComponent = (new ChargeComponent(this))
                .addStartEvent(this::onChargeStart)
                .addTickEvent(this::onChargeTick)
                .addEndEvent(this::onChargeEnd);
        this.animationComponent = new AnimationComponent(this);
        this.hitTrackerComponent = new HitTrackerComponent(this);
        this.dealDamageComponent = new DealDamageComponent(this);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.particleInterval = new Interval(2);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent, this.projectileComponent, this.hitTrackerComponent, this.dealDamageComponent});
        super.addCanUseCheck(this::canUse);
        super.addUseEvent(this::onUseEvent);
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        KarmaAbility karma = (KarmaAbility)props.getPassiveAbility(KarmaAbility.INSTANCE);

        if (karma == null) {
            return AbilityUseResult.fail((ITextComponent)null);
        }

        // Check if player is using Inga Zarashi transformation
        IngaZarashiRework ingaZarashi = (IngaZarashiRework)props.getEquippedAbility(IngaZarashiRework.INSTANCE);
        if (ingaZarashi == null || !ingaZarashi.isContinuous()) {
            return AbilityUseResult.fail(new TranslationTextComponent("Exploding Karma can only be used while Inga Zarashi is active!"));
        }

        // Check if player has at least 75 karma
        if (karma.getKarma() < MIN_KARMA_REQUIRED) {
            return AbilityUseResult.fail(new TranslationTextComponent("You need at least " + MIN_KARMA_REQUIRED + " karma to use Exploding Karma! Current: " + String.format("%.1f", karma.getKarma())));
        }

        this.karmaAbility = Optional.ofNullable(karma);
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        // CAPTURE KARMA VALUE IMMEDIATELY WHEN ABILITY IS USED
        if (this.karmaAbility.isPresent()) {
            this.usedKarma = Math.max(MIN_KARMA_REQUIRED, Math.min(100.0, this.karmaAbility.get().getKarma())); // Clamp between 75-100
        } else {
            this.usedKarma = MIN_KARMA_REQUIRED;
        }

        // NON-CANCELLABLE: Only allow starting charge if not already charging
        if (!entity.level.isClientSide && !this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.particleInterval.restartIntervalToZero();
        this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);

        // Calculate scaled values based on captured karma
        double karmaRatio = (this.usedKarma - MIN_KARMA_REQUIRED) / (100.0 - MIN_KARMA_REQUIRED);
        this.lockedAOE = (float)(MIN_AOE_RADIUS + (MAX_AOE_RADIUS - MIN_AOE_RADIUS) * karmaRatio);
        this.lockedDamage = (float)(MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * karmaRatio);

        // Debug logging
        System.out.println("=== EXPLODING KARMA DEBUG ===");
        System.out.println("Used Karma: " + this.usedKarma);
        System.out.println("Karma Ratio: " + karmaRatio);
        System.out.println("Locked AOE: " + this.lockedAOE);
        System.out.println("Locked Damage: " + this.lockedDamage);

        // Create and spawn the projectile
        this.karmaProjectile = (KarmaExplosionProjectile)this.projectileComponent.getNewProjectile(entity);
        this.karmaProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + 7.5, entity.getZ());

        // Pass the scaled AOE to the projectile
        this.karmaProjectile.setScaledSize(this.lockedAOE / MAX_AOE_RADIUS); // Normalize for projectile scaling

        entity.level.addFreshEntity(this.karmaProjectile);

        // Create visual sphere indicator centered on the projectile position
        this.sphereEntity = new SphereEntity(entity.level, entity);
        this.sphereEntity.setColor(new Color(255, 0, 0, 80)); // Red with transparency
        this.sphereEntity.setRadius(0.0F); // Start at 0
        this.sphereEntity.setDetailLevel(32);
        this.sphereEntity.setAnimationSpeed(1);
        this.sphereEntity.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + 7.5, entity.getZ());
        entity.level.addFreshEntity(this.sphereEntity);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (this.karmaProjectile == null || !this.karmaProjectile.isAlive()) {
            // Don't stop charging - ability is non-cancellable, just create a new projectile if needed
            if (this.karmaProjectile == null || !this.karmaProjectile.isAlive()) {
                this.karmaProjectile = (KarmaExplosionProjectile)this.projectileComponent.getNewProjectile(entity);
                this.karmaProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + 7.5, entity.getZ());
                this.karmaProjectile.setScaledSize(this.lockedAOE / MAX_AOE_RADIUS);
                entity.level.addFreshEntity(this.karmaProjectile);
            }
        }

        this.karmaProjectile.setLife(this.karmaProjectile.getMaxLife());
        this.karmaProjectile.increaseSize();
        this.karmaProjectile.setPos(entity.getX(), entity.getY() + (double)entity.getEyeHeight() + 7.5, entity.getZ());

        // Update sphere to grow with charge percentage using easing for smooth animation
        if (this.sphereEntity != null && this.sphereEntity.isAlive()) {
            float chargeProgress = this.chargeComponent.getChargePercentage();
            // Use easing function for smooth growth
            float easedProgress = EasingFunctionHelper.easeOutCubic(chargeProgress);
            float currentRadius = this.lockedAOE * easedProgress;
            this.sphereEntity.setRadius(currentRadius);
            // Keep sphere centered on projectile
            this.sphereEntity.setPos(this.karmaProjectile.getX(), this.karmaProjectile.getY(), this.karmaProjectile.getZ());

            // Apply Slowness 2 and Weakened Movement to all enemies within the sphere
            if (!entity.level.isClientSide && currentRadius > 0.0F) {
                for(LivingEntity target : WyHelper.getEntitiesAroundCircle(this.karmaProjectile.position(), entity.level, (double)currentRadius, (double)currentRadius, ModEntityPredicates.getEnemyFactions(entity), LivingEntity.class)) {
                    // Add Slowness 2 effect
                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
                    // Add weakened movement effect
                    target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 5, 0, false, false));
                }
            }
        }

        // Slow the entity down while charging (player only gets movement blocked)
        AbilityHelper.slowEntityFall(entity);
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));

        if (!entity.level.isClientSide && this.particleInterval.canTick()) {
            // Red particle effects during charging
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.DAI_ENKAI_2.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        // AUTOMATIC DETONATION after charge completes
        this.triggerExplosion(entity);
    }

    private void triggerExplosion(LivingEntity entity) {
        if (this.karmaProjectile != null && this.karmaProjectile.isAlive()) {
            // Trigger the projectile's explosion
            this.karmaProjectile.onBlockImpactEvent(this.karmaProjectile.blockPosition());

            // Deal AOE damage to all entities in range using locked values
            for(LivingEntity target : WyHelper.getEntitiesAroundCircle(this.karmaProjectile.position(), entity.level, (double)this.lockedAOE, (double)this.lockedAOE, ModEntityPredicates.getEnemyFactions(entity), LivingEntity.class)) {
                if (this.hitTrackerComponent.canHit(target)) {
                    float distance = target.distanceTo(this.karmaProjectile);
                    float damageMultiplier = 1.0F - (distance / this.lockedAOE);
                    float finalDamage = this.lockedDamage * Math.max(0.3F, damageMultiplier);

                    if (this.dealDamageComponent.hurtTarget(entity, target, finalDamage)) {
                        target.setSecondsOnFire(5);
                        // Knockback effect - push entities away from explosion center
                        Vector3d knockbackVec = target.position().subtract(this.karmaProjectile.position()).normalize().scale(2.0);
                        AbilityHelper.setDeltaMovement(target, knockbackVec);
                    }
                }
            }

            this.karmaProjectile.remove();
            this.karmaProjectile = null;
        }

        // Remove the visual sphere
        if (this.sphereEntity != null && this.sphereEntity.isAlive()) {
            this.sphereEntity.remove();
            this.sphereEntity = null;
        }

        this.animationComponent.stop(entity);
        this.hitTrackerComponent.clearHits();

        // DEPLETE ALL KARMA after explosion
        if (this.karmaAbility.isPresent()) {
            float currentKarma = this.karmaAbility.get().getKarma();
            this.karmaAbility.get().addKarma(entity, -currentKarma); // Remove all karma
            System.out.println("=== KARMA DEPLETED ===");
            System.out.println("Removed " + currentKarma + " karma");
        }

        // Reset stored karma for next use
        this.usedKarma = 0.0;

        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        KarmaExplosionProjectile proj = new KarmaExplosionProjectile(entity.level, entity, this);
        return proj;
    }

    // Helper method to calculate scaled damage for tooltips
    private float calculateScaledDamage(float karma) {
        if (karma < MIN_KARMA_REQUIRED) return MIN_DAMAGE;
        double karmaRatio = (karma - MIN_KARMA_REQUIRED) / (100.0 - MIN_KARMA_REQUIRED);
        return (float)(MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * karmaRatio);
    }

    // Helper method to calculate scaled AOE for tooltips
    private float calculateScaledAOE(float karma) {
        if (karma < MIN_KARMA_REQUIRED) return MIN_AOE_RADIUS;
        double karmaRatio = (karma - MIN_KARMA_REQUIRED) / (100.0 - MIN_KARMA_REQUIRED);
        return (float)(MIN_AOE_RADIUS + (MAX_AOE_RADIUS - MIN_AOE_RADIUS) * karmaRatio);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Exploding Karma", AbilityCategory.DEVIL_FRUITS, ExplodingKarmaAbility::new))
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        (e, a) -> EXPLODING_KARMA_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.DARK_RED)),
                        (e, a) -> DESCRIPTION[0],
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        (e, a) -> new StringTextComponent("§4Required Karma: §r" + MIN_KARMA_REQUIRED),
                        (e, a) -> new StringTextComponent("§c§lNON-CANCELLABLE§r - Must complete charge"),
                        (e, a) -> new StringTextComponent("§c§lDEPLETES ALL KARMA§r on use"),
                        AOE_TOOLTIP,
                        (e, a) -> new StringTextComponent("§a" + ModI18n.ABILITY_DESCRIPTION_STAT_NAME_PROJECTILE.getString() + "§r"),
                        DAMAGE_TOOLTIP
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .build();
    }
}