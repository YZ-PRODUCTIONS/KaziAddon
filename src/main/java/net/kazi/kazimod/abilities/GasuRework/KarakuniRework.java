//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GasuRework;

import java.util.List;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KarakuniRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "karakuni", new Pair[]{ImmutablePair.of("Removes the oxygen around the user, suffocating and weakening everyone in the vicinity", (Object)null)});
    private static final UUID SHINOKUNI_RANGE_BONUS = UUID.fromString("f8c0c599-1f32-4a91-b5e2-d4b0a481dec8");
    private static final int COOLDOWN = 600;
    private static final int HOLD_TIME = 100;
    private static final int RANGE = 9;
    public static final AbilityCore<KarakuniRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(100, this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::stopContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private Interval suffocateInterval = new Interval(2);

    public KarakuniRework(AbilityCore<KarakuniRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.rangeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 100.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.suffocateInterval.restartIntervalToZero();
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.suffocateInterval.canTick()) {
            this.rangeComponent.getBonusManager().removeBonus(SHINOKUNI_RANGE_BONUS);
            if (((MorphInfo)CartMorphs.SHINOKUNI.get()).isActive(entity)) {
                this.rangeComponent.getBonusManager().addBonus(SHINOKUNI_RANGE_BONUS, "Shinokuni Range Bonus", BonusOperation.ADD, 3.0F);
            }

            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 9.0F);
            List<BlockPos> blocks = WyHelper.getNearbyBlocks(entity, 9);

            for(LivingEntity target : targets) {
                if (!target.canBreatheUnderwater()) {
                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 500, 2, false, false));
                    target.addEffect(new EffectInstance(Effects.WEAKNESS, 500, 1, false, false));
                    target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 500, 0, false, false));                    target.setAirSupply(target.getAirSupply() - 50);
                    int airLeft = target.getAirSupply();
                    if (airLeft <= 0) {
                        if (target.getHealth() > 8.0F) {
                            target.hurt(ModDamageSource.causeAbilityDamage(entity, this, "drown").setInternal(), 8.0F);
                        } else {
                            target.addEffect(new EffectInstance((Effect)ModEffects.UNCONSCIOUS.get(), 200, 1));
                        }
                    }

                    if (target.isOnFire()) {
                        target.clearFire();
                    }
                }
            }

            if (entity.isOnFire()) {
                entity.clearFire();
            }

            BlockPos.Mutable blockUp = new BlockPos.Mutable();

            for(BlockPos blockPos : blocks) {
                blockUp.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
                if (entity.level.getBlockState(blockPos).getBlock() == Blocks.FIRE && entity.level.getBlockState(blockUp).getBlock() == Blocks.AIR) {
                    entity.level.removeBlock(blockPos, false);
                }
            }

            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.GASTANET.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

    }

    private void stopContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 600.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Karakuni", AbilityCategory.DEVIL_FRUITS, KarakuniRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(600.0F), ContinuousComponent.getTooltip(100.0F), RangeComponent.getTooltip(9.0F, RangeType.AOE)}).build();
    }
}