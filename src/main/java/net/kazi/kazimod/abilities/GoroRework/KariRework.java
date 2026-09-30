package net.kazi.kazimod.abilities.GoroRework;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.goro.KariParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Kari. */
public class KariRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/kari.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/kari.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "kari",
            new Pair[]{
                    ImmutablePair.of(
                            "The user heats the air around them with lightning until it explodes with a thunder clap.",
                            null),
                    ImmutablePair.of("Can be used to avoid and neutralize projectiles.", null)
            });

    public static final AbilityCore<KariRework> INSTANCE =
            new AbilityCore.Builder<KariRework>(
                    "Kari", AbilityCategory.DEVIL_FRUITS, KariRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(60.0F, 240.0F),
                            ChargeComponent.getTooltip(60.0F),
                            RangeComponent.getTooltip(2.0F, RangeComponent.RangeType.AOE))
                    .setSourceElement(SourceElement.FIRE)
                    .setIcon(DEFAULT_ICON)
                    .build();

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private static final KariParticleEffect.Details DETAILS = new KariParticleEffect.Details();
    private final Interval particleInterval = new Interval(2);

    public KariRework(AbilityCore<KariRework> core) {
        super(core);
        updateDisplayIcon();
        this.isNew = true;
        this.addComponents(chargeComponent, rangeComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent((entity, ability) -> updateDisplayIcon());
    }

    private void updateDisplayIcon() {
        this.setDisplayIcon(ClientConfig.INSTANCE.isGoroBlue() ? ALT_ICON : DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!chargeComponent.isCharging()) {
            particleInterval.restartIntervalToZero();
            chargeComponent.startCharging(entity, 60.0F);
        } else {
            chargeComponent.stopCharging(entity);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 1));
        if (particleInterval.canTick()) {
            DETAILS.setRange(2);
            DETAILS.setSize(2.0F);
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.KARI.get(), entity,
                    entity.getX(), entity.getY(), entity.getZ(), DETAILS);
        }

        float range = rangeComponent.getBonusManager().applyBonus(2.0F);
        List<Entity> targets = WyHelper.getNearbyEntities(
                entity.position(), entity.level, range, null, Entity.class);
        for (Entity target : targets) {
            if (target instanceof LivingEntity) {
                AbilityHelper.setSecondsOnFireBy(target, 3, entity);
            } else if (target instanceof AbilityProjectileEntity) {
                AbilityProjectileEntity projectile = (AbilityProjectileEntity) target;
                if (((ModDamageSource) projectile.getDamageSource()).isPhysical()) {
                    LivingEntity thrower = projectile.getThrower();
                    if (thrower != null && thrower != entity) {
                        AbilityHelper.setSecondsOnFireBy(target, 3, entity);
                    }
                } else {
                    target.remove();
                }
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        entity.level.playSound(
                null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER,
                2.0F, 0.5F + entity.getRandom().nextFloat() * 0.2F);
        float cooldown = Math.max(60.0F, 240.0F * chargeComponent.getChargePercentage());
        cooldownComponent.startCooldown(entity, cooldown);
    }
}
