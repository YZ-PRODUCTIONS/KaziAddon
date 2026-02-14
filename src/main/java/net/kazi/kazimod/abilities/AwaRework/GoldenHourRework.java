package net.kazi.kazimod.abilities.AwaRework;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GoldenHourRework extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText(
                    "kazimod",
                    "golden_hour",
                    new Pair[]{ImmutablePair.of(
                            "Spreads bubbles on enemies around, leaving them weakened and immobile",
                            null)}
            );

    public static final float RANGE = 10.0F;
    public static final AbilityCore<GoldenHourRework> INSTANCE;

    private final RangeComponent rangeComponent = new RangeComponent(this);

    // ⭐ particle field timer
    private final ContinuousComponent particleField =
            new ContinuousComponent(this, true)
                    .addTickEvent(this::onParticleTick);

    // ⭐ stored AOE center
    private BlockPos fieldCenter = BlockPos.ZERO;

    private final Random random = new Random();

    public GoldenHourRework(AbilityCore<GoldenHourRework> core) {
        super(core);
        super.isNew = true;
        super.addComponents(this.rangeComponent, this.particleField);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {

        // ===============================
        // ONE TIME STUN (ORIGINAL)
        // ===============================
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
        targets.removeIf(Entity::isInWater);

        for (LivingEntity target : targets) {
            if (!target.hasEffect(ModEffects.WASHED.get())) {
                target.addEffect(new EffectInstance(ModEffects.WASHED.get(), 140, 1));
            }

            WyHelper.spawnParticleEffect(
                    ModParticleEffects.GOLDEN_HOUR.get(),
                    entity,
                    target.getX(),
                    target.getY(),
                    target.getZ()
            );
        }

        // ===============================
        // STORE CAST LOCATION
        // ===============================
        this.fieldCenter = entity.blockPosition();

        // ===============================
        // START PARTICLE FIELD (140 ticks)
        // ===============================
        this.particleField.startContinuity(entity, 20);

        this.cooldownComponent.startCooldown(entity, 1200.0F);
    }

    // ===============================
    // WORLD AOE PARTICLE FIELD
    // ===============================
    private void onParticleTick(LivingEntity entity, IAbility ability) {

        for (int i = 0; i < 30; i++) {

            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * RANGE;

            double x = fieldCenter.getX() + 0.5 + Math.cos(angle) * dist;
            double z = fieldCenter.getZ() + 0.5 + Math.sin(angle) * dist;
            double y = fieldCenter.getY() + random.nextDouble() * 2;

            WyHelper.spawnParticleEffect(
                    ModParticleEffects.GOLDEN_HOUR.get(),
                    entity,
                    x,
                    y,
                    z
            );
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder(
                "Golden Hour",
                AbilityCategory.DEVIL_FRUITS,
                GoldenHourRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(200.0F, 1200.0F),
                        ContinuousComponent.getTooltip(200.0F),
                        RangeComponent.getTooltip(10.0F, RangeType.AOE))
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .build();
    }
}
