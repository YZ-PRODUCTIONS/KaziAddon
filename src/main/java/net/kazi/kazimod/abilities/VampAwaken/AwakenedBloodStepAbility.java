package net.kazi.kazimod.abilities.VampAwaken;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Cart 0.7.5 Blood Step, with only its Zoan-form requirement removed. */
public class AwakenedBloodStepAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "cartaddon", "blood_step",
            new Pair[]{ImmutablePair.of("The user teleports to their desired location.", null)});
    private static final float MAX_TELEPORT_DISTANCE = 30.0F;
    private static final float BASE_COOLDOWN = 100.0F;
    private static final ResourceLocation ICON =
            new ResourceLocation("cartaddon", "textures/abilities/blood_step.png");

    public static final AbilityCore<AwakenedBloodStepAbility> INSTANCE;

    private final DamageTakenComponent damageTakenComponent =
            new DamageTakenComponent(this, this::onDamageTaken, DamageTakenComponent.DamageState.ATTACK);
    private boolean hasFallDamage = true;

    public AwakenedBloodStepAbility(AbilityCore<AwakenedBloodStepAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(this.damageTakenComponent);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        float percentHp = entity.getHealth() * 100.0F / entity.getMaxHealth();
        BlockRayTraceResult trace = WyHelper.rayTraceBlocks(entity, MAX_TELEPORT_DISTANCE);
        BlockPos destination;

        if (trace != null && trace.getType() != RayTraceResult.Type.MISS) {
            destination = WyHelper.getClearPositionForPlayer(entity, trace.getBlockPos());
        } else {
            destination = WyHelper.rayTraceBlockSafe(entity, MAX_TELEPORT_DISTANCE);
        }

        if (destination == null) {
            destination = WyHelper.rayTraceBlockSafe(entity, MAX_TELEPORT_DISTANCE);
        }
        if (destination == null) return;

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        entity.stopRiding();
        entity.teleportTo(destination.getX(), destination.getY(), destination.getZ());
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);

        this.hasFallDamage = false;
        this.cooldownComponent.startCooldown(entity, BASE_COOLDOWN + 2.0F * (100.0F - percentHp));
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.hasFallDamage = true;
            return 0.0F;
        }
        return damage;
    }

    private static boolean canUnlock(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Blood Step", AbilityCategory.DEVIL_FRUITS, AwakenedBloodStepAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(100.0F, 300.0F),
                        RangeComponent.getTooltip(MAX_TELEPORT_DISTANCE, RangeComponent.RangeType.LINE))
                .setIcon(ICON)
                .setUnlockCheck(AwakenedBloodStepAbility::canUnlock)
                .build();
    }
}
