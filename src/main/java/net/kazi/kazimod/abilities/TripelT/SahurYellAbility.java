package net.kazi.kazimod.abilities.TripelT;

import java.awt.Color;
import net.kazi.kazimod.entities.projectiles.LightArrowProjectile;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class SahurYellAbility extends Ability {

    public enum Mode {
        SAHUR_YELL,
        SERAPHIC_WINGS
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "sahur_yell",
            new Pair[]{
                    ImmutablePair.of("A deafening sahur roar rattles everyone around you and leaves them dizzy.", null),
                    ImmutablePair.of("Tripel T God changes the roar into Seraphic Wings, a heavenly beam rush.", null)
            });

    private static final IDescriptionLine BASE_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Sahur Yell")));
    private static final IDescriptionLine ALT_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Seraphic Wings")));
    private static final ResourceLocation ALT_ICON = new ResourceLocation("kazimod", "textures/abilities/seraphic_wings.png");

    public static final AbilityCore<SahurYellAbility> INSTANCE;

    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.SAHUR_YELL, true).addChangeModeEvent(this::onAltModeChanged);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true).addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final Interval beamInterval = new Interval(4);

    private Mode currentMode = Mode.SAHUR_YELL;

    public SahurYellAbility(AbilityCore<SahurYellAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.continuousComponent,
                this.animationComponent,
                this.rangeComponent,
                this.dealDamageComponent,
                this.projectileComponent
        });
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent(this::onUse);
    }

    public void switchToBase(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.SAHUR_YELL);
    }

    public void switchToAlt(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.SERAPHIC_WINGS);
    }

    private void onAltModeChanged(LivingEntity entity, IAbility ability, Enum<?> mode) {
        this.currentMode = (Mode) mode;
        this.setDisplayName(new StringTextComponent(this.currentMode == Mode.SERAPHIC_WINGS ? "Seraphic Wings" : "Sahur Yell"));
        if (this.currentMode == Mode.SERAPHIC_WINGS) {
            this.setDisplayIcon(ALT_ICON);
        } else {
            this.setDisplayIcon(this.getCore());
        }
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.SERAPHIC_WINGS) {
            if (!this.continuousComponent.isContinuous()) {
                playYell(entity, 1.15F);
                this.continuousComponent.startContinuity(entity, 36.0F);
            }
            return;
        }

        playBaseSahurYellSound(entity);
        this.continuousComponent.startContinuity(entity, 20.0F);
        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 20.0F)) {
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 20, 0, false, true));
        }
        this.cooldownComponent.startCooldown(entity, 260.0F);
    }

    private void playBaseSahurYellSound(LivingEntity entity) {
        playYell(entity, 1.0F);
    }

    private void playYell(LivingEntity entity, float pitch) {
        if (entity.level.isClientSide) return;
        entity.level.playSound(null, entity.blockPosition(), KaziSounds.SAHUR_YELL_SFX.get(), SoundCategory.PLAYERS, 4.0F, pitch * .8F);
        entity.level.playSound(null, entity.blockPosition(), net.minecraft.util.SoundEvents.NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 2.0F, .65F);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.beamInterval.restartIntervalToZero();
        // The transformed model supplies its own wing-rush or roar pose.
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.SAHUR_YELL) return;
        AbilityHelper.setDeltaMovement(entity, entity.getLookAngle().normalize().scale(1.6D));
        entity.fallDistance = 0.0F;

        if (this.beamInterval.canTick()) {
            spawnRandomBeam(entity);
        }

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 2.5F)) {
            this.dealDamageComponent.hurtTarget(entity, target, 3.2F);
        }
    }

    private void spawnRandomBeam(LivingEntity entity) {
        if (entity.level.isClientSide) {
            return;
        }
        Vector3d origin = entity.position().add(0.0D, 1.2D, 0.0D);
        float yaw = entity.yRot + (entity.getRandom().nextFloat() - 0.5F) * 70.0F;
        float pitch = -20.0F + entity.getRandom().nextFloat() * 40.0F;
        LightningEntity inner = new LightningEntity(entity, origin.x, origin.y, origin.z, yaw, pitch, 25.0F, 20.0F, this.getCore());
        LightningEntity outer = new LightningEntity(entity, origin.x, origin.y, origin.z, yaw, pitch, 25.0F, 20.0F, this.getCore());
        inner.setSize(0.38F);
        inner.setColor(Color.WHITE);
        inner.setDamage(0.0F);
        inner.setSegments(12);
        inner.setBranches(1);
        inner.setLightningMovement(false);
        inner.setCollideWithEntities(false);
        inner.setMaxLife(16);
        outer.setSize(0.5F);
        outer.setColor(new Color(255, 245, 180));
        outer.setDamage(2.4F);
        outer.setSegments(14);
        outer.setBranches(2);
        outer.setExplosion(1, true, 0.03F);
        outer.disableExplosionKnockback();
        outer.setLightningMovement(false);
        outer.setCollideWithEntities(false);
        outer.setMaxLife(16);
        outer.seed = inner.seed;
        entity.level.addFreshEntity(inner);
        entity.level.addFreshEntity(outer);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        entity.fallDistance = 0.0F;
        this.animationComponent.stop(entity);
        if (this.currentMode == Mode.SERAPHIC_WINGS) this.cooldownComponent.startCooldown(entity, 320.0F);
    }

    private LightArrowProjectile createProjectile(LivingEntity entity) {
        return new LightArrowProjectile(entity.level, entity, 12.0F);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Sahur Yell", AbilityCategory.DEVIL_FRUITS, SahurYellAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        BASE_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(260.0F),
                        RangeComponent.getTooltip(20.0F, RangeType.AOE)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ALT_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(320.0F),
                        ContinuousComponent.getTooltip(36.0F)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.BLUNT, SourceType.PROJECTILE})
                .build();
    }
}
