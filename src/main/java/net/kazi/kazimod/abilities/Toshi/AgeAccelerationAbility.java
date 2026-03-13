package net.kazi.kazimod.abilities.Toshi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effects;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public class AgeAccelerationAbility extends PunchAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "age_acceleration",
            new Pair[]{ImmutablePair.of("Hit the target to rapidly age them, weakening and slowing them", (Object) null)}
    );

    private static final int COOLDOWN = 40;
    private static final long REAPPLY_COOLDOWN_MS = 15_000L; // 30 seconds in milliseconds

    // Tracks the last time each entity (by UUID) was hit
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();

    public static final AbilityCore<AgeAccelerationAbility> INSTANCE;

    public AgeAccelerationAbility(AbilityCore<AgeAccelerationAbility> core) {
        super(core);
    }

    @Override
    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        UUID targetId = target.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if the target was hit recently
        if (lastHitTime.containsKey(targetId)) {
            long timeSinceLastHit = currentTime - lastHitTime.get(targetId);
            if (timeSinceLastHit < REAPPLY_COOLDOWN_MS) {
                return false; // Still on cooldown for this target, skip effect
            }
        }

        // Apply effects and record the hit time
        target.addEffect(new EffectInstance(Effects.WEAKNESS, 200, 1));
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 200, 1));
        target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 200, 0));        lastHitTime.put(targetId, currentTime);

        // Clean up expired entries to avoid memory leaks
        lastHitTime.entrySet().removeIf(entry -> currentTime - entry.getValue() >= REAPPLY_COOLDOWN_MS);

        return true;
    }

    @Override
    public Predicate<LivingEntity> canActivate() {
        return (entity) -> super.continuousComponent.isContinuous();
    }

    @Override
    public int getUseLimit() {
        return 1;
    }

    @Override
    public float getPunchCooldown() {
        return (float) COOLDOWN;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Age Acceleration", AbilityCategory.DEVIL_FRUITS, AgeAccelerationAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE})
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        ContinuousComponent.getTooltip(),
                        ChangeStatsComponent.getTooltip()
                })
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}