package net.kazi.kazimod.abilities.TripelT;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.IPlayerAbilities;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

public class TripelTFlightAbility extends PropelledFlightAbility {

    public static final AbilityCore<TripelTFlightAbility> INSTANCE =
            new AbilityCore.Builder<>("Triple T Flight", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, TripelTFlightAbility::new)
                    .build();

    public TripelTFlightAbility(AbilityCore<TripelTFlightAbility> core) {
        super(core);
        this.addCanUseCheck(this::canFly);
        this.addDuringPassiveEvent(this::onDuringPassive);
    }

    private void onDuringPassive(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) {
            return;
        }

        TripelTGodFormAbility godForm = (TripelTGodFormAbility) data.getEquippedAbility(TripelTGodFormAbility.INSTANCE);
        boolean active = godForm != null && godForm.isContinuous();
        if (!active) {
            return;
        }

        if (((IPlayerAbilities) player.abilities).hasCustomFlight() && player.abilities.flying
                && DevilFruitHelper.getDifferenceToFloor(player) < 2.0D) {
            AbilityHelper.setDeltaMovement(player, player.getLookAngle().add(0.0D, 1.0D, 0.0D).multiply(1.0D, 0.25D, 1.0D));
        }
    }

    private AbilityUseResult canFly(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) {
            return AbilityUseResult.fail(null);
        }
        TripelTGodFormAbility godForm = (TripelTGodFormAbility) data.getEquippedAbility(TripelTGodFormAbility.INSTANCE);
        return godForm != null && godForm.isContinuous() ? AbilityUseResult.success() : AbilityUseResult.fail(null);
    }

    @Override
    public float getMaxSpeed(LivingEntity entity) {
        return entity.isSprinting() ? 0.9F : 0.4F;
    }

    @Override
    protected float getAcceleration(LivingEntity entity) {
        return entity.isSprinting() ? 0.007F : 0.0035F;
    }

    @Override
    protected int getHeightDifference(LivingEntity entity) {
        return 50;
    }
}
