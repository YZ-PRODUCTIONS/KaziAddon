package net.kazi.kazimod.abilities.MeraRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HeatDashRework extends Ability {
    private static final ResourceLocation ICON = new ResourceLocation("mineminenomi", "textures/abilities/heat_dash.png");
    private static final TranslationTextComponent NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.heat_dash", "Heat Dash"));

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "heat_dash",
            new Pair[]{ImmutablePair.of("Transforms the user into fire and launches them forward, setting on fire all enemies around the user.", null)}
    );

    private static final float SHORT_COOLDOWN = 60.0F;
    private static final float LONG_COOLDOWN = 340.0F;
    private static final float HOLD_TIME = 13.0F;
    private static final float RANGE = 1.0F;
    private static final int STACKS = 2;

    public static final AbilityCore<HeatDashRework> INSTANCE =
            new AbilityCore.Builder<>("Heat Dash", AbilityCategory.DEVIL_FRUITS, HeatDashRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            AbilityHelper.createShortLongCooldownStat(SHORT_COOLDOWN, LONG_COOLDOWN),
                            ContinuousComponent.getTooltip(HOLD_TIME),
                            RangeComponent.getTooltip(RANGE, RangeType.AOE),
                            StackComponent.getTooltip(STACKS)
                    )
                    .setIcon(ICON)
                    .build();

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this)
                    .addStartEvent(this::onContinuityStart)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final StackComponent stackComponent = new StackComponent(this, STACKS);
    private final Interval particleInterval = new Interval(2);
    private float pendingCooldown = LONG_COOLDOWN;

    public HeatDashRework(AbilityCore<HeatDashRework> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(NAME);
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTrackerComponent, this.rangeComponent, this.stackComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!AbilityHelper.canUseMomentumAbilities(entity) || this.continuousComponent.isContinuous()) {
            return;
        }

        this.continuousComponent.startContinuity(entity);
        this.stackComponent.addStacks(entity, this, -1);
        if (this.stackComponent.getStacks() <= 0) {
            this.pendingCooldown = LONG_COOLDOWN;
            this.stackComponent.setStacks(entity, this, STACKS);
        } else {
            this.pendingCooldown = SHORT_COOLDOWN;
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.particleInterval.restartIntervalToZero();
        entity.level.playSound(null, entity.blockPosition(), (SoundEvent) ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.5F + this.random.nextFloat() / 3.0F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }
        if (this.continuousComponent.getContinueTime() > HOLD_TIME) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }
        if (super.canUse(entity).isFail()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        AbilityHelper.setDeltaMovement(entity, entity.getLookAngle().normalize().scale(3.0D));
        if (this.particleInterval.canTick()) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.HEAT_DASH.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, RANGE)) {
            if (this.hitTrackerComponent.canHit(target) && entity.canSee(target)) {
                AbilityHelper.setSecondsOnFireBy(target, 2, entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, this.pendingCooldown);
        this.pendingCooldown = LONG_COOLDOWN;
    }
}
