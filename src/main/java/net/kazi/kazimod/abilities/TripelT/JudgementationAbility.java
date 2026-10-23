package net.kazi.kazimod.abilities.TripelT;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class JudgementationAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "judgementation",
            new Pair[]{ImmutablePair.of("A divine counter that restores 20 HP on success, blasts the attacker away, and punishes them hard.", null)});

    public static final AbilityCore<JudgementationAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true).addEndEvent(this::onEnd);
    private final DamageTakenComponent damageTakenComponent =
            new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public JudgementationAbility(AbilityCore<JudgementationAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.damageTakenComponent, this.dealDamageComponent});
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent((entity, ability) -> {
            TripelTHelper.playTungSound(entity, 2.0F, 1.08F);
            this.continuousComponent.startContinuity(entity, 30.0F);
        });
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous()) {
            return damage;
        }

        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity)) {
            return damage;
        }

        LivingEntity attacker = (LivingEntity) attackerEntity;
        this.dealDamageComponent.hurtTarget(entity, attacker, 28.0F);
        Vector3d knockback = attacker.position().subtract(entity.position()).normalize().scale(3.2D);
        attacker.push(knockback.x, 1.0D, knockback.z);
        entity.heal(20.0F);

        if (!entity.level.isClientSide) {
            ServerWorld world = (ServerWorld) entity.level;
            WyHelper.spawnParticles(ParticleTypes.END_ROD, world, attacker.getX(), attacker.getY() + attacker.getEyeHeight(), attacker.getZ());
            WyHelper.spawnParticles(ParticleTypes.EXPLOSION, world, attacker.getX(), attacker.getY() + 1.0D, attacker.getZ());
        }

        entity.level.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 2.0F, 1.2F);
        this.continuousComponent.stopContinuity(entity);
        this.cooldownComponent.startCooldown(entity, 320.0F);
        return 0.0F;
    }

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (!this.cooldownComponent.isOnCooldown()) {
            this.cooldownComponent.startCooldown(entity, 320.0F);
        }
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Judgementation", AbilityCategory.DEVIL_FRUITS, JudgementationAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(30.0F),
                        CooldownComponent.getTooltip(320.0F),
                        DealDamageComponent.getTooltip(28.0F)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.BLUNT})
                .build();
    }
}
