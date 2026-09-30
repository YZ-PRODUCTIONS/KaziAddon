package net.kazi.kazimod.abilities.GoroRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Volt Amaru Flight passive. */
public class VoltAmaruFlightRework extends PropelledFlightAbility {
    public static final AbilityCore<VoltAmaruFlightRework> INSTANCE =
            new AbilityCore.Builder<VoltAmaruFlightRework>(
                    "Volt Amaru Flight", AbilityCategory.DEVIL_FRUITS,
                    AbilityType.PASSIVE, VoltAmaruFlightRework::new)
                    .build();

    public VoltAmaruFlightRework(AbilityCore<VoltAmaruFlightRework> core) {
        super(core);
        this.addCanUseCheck(this::canFly);
        this.addDuringPassiveEvent(this::onDuringPassive);
    }

    private void onDuringPassive(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity) || this.isRecovering) return;
        PlayerEntity player = (PlayerEntity) entity;
        double difference = DevilFruitHelper.getDifferenceToFloor(entity);
        if (difference < 5.0D && player.abilities.flying) {
            AbilityHelper.setDeltaMovement(
                    entity,
                    entity.getDeltaMovement().add(0.0D, 1.0D, 0.0D)
                            .multiply(1.0D, 0.25D, 1.0D));
        }
    }

    @Override
    public float getMaxSpeed(LivingEntity entity) {
        return entity.isSprinting() ? 2.1F : 1.1F;
    }

    @Override
    protected float getAcceleration(LivingEntity entity) {
        return 0.015F;
    }

    @Override
    protected int getHeightDifference(LivingEntity entity) {
        return 36;
    }

    private AbilityUseResult canFly(LivingEntity entity, IAbility ability) {
        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) return AbilityUseResult.fail(null);
        VoltAmaruRework voltAmaru =
                abilityData.getEquippedAbility(VoltAmaruRework.INSTANCE);
        if (voltAmaru != null
                && voltAmaru.getComponent(ModAbilityKeys.CONTINUOUS).get().isContinuous()) {
            return AbilityUseResult.success();
        }
        return AbilityUseResult.fail(null);
    }
}
