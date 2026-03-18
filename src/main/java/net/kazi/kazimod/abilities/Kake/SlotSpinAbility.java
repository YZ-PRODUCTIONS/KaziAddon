package net.kazi.kazimod.abilities.Kake;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import net.kazi.kazimod.particles.LuckySlotParticleEffect;

public class SlotSpinAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "slot_spin",
            new Pair[]{
                    ImmutablePair.of(
                            "Spins the lucky slots, rolling a number from 0-9. Each number unlocks a unique Casino Roll attack.",
                            (Object) null
                    )
            }
    );

    private static final float COOLDOWN  = 300.0f;
    private static final float SPIN_TIME = 20.0f;

    public static final DamageSource ZERO_ROLL = new DamageSource("kazi_zero_roll") {
        @Override
        public ITextComponent getLocalizedDeathMessage(LivingEntity entity) {
            return new StringTextComponent(
                    "§f" + entity.getDisplayName().getString() + " had their luck run out");
        }
    };

    public static final AbilityCore<SlotSpinAbility> INSTANCE;

    private final ContinuousComponent spinContinuous;
    private int spinTick = 0;

    public SlotSpinAbility(AbilityCore<SlotSpinAbility> core) {
        super(core);
        this.isNew = true;

        spinContinuous = new ContinuousComponent(this, true);
        spinContinuous.addTickEvent(this::onSpinTick);
        spinContinuous.addEndEvent(this::onSpinEnd);

        super.addComponents(new AbilityComponent[]{ spinContinuous });
        super.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Block spin if Casino Storm is active
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data != null) {
            CasinoRollAbility casinoRoll = data.getEquippedAbility(CasinoRollAbility.INSTANCE);
            if (casinoRoll != null && casinoRoll.isStormActive()) {
                if (entity instanceof PlayerEntity) {
                    ((PlayerEntity) entity).displayClientMessage(
                            new StringTextComponent("§cCannot spin during Casino Storm!"), true);
                }
                return;
            }
        }

        // Roll — handle Double Down (roll twice, take higher)
        int rolled;
        if (entity.getPersistentData().getBoolean("kazi_double_down")) {
            entity.getPersistentData().remove("kazi_double_down");
            int roll1 = this.random.nextInt(10);
            int roll2 = this.random.nextInt(10);
            rolled = Math.max(roll1, roll2);
            if (entity instanceof PlayerEntity) {
                ((PlayerEntity) entity).displayClientMessage(
                        new StringTextComponent("§6[Double Down] §fRolled " + roll1 + " & " + roll2 + " — taking " + rolled), true);
            }
        } else {
            rolled = this.random.nextInt(10);
        }

        entity.getPersistentData().putInt("kazi_pending_roll", rolled);
        spinTick = 0;
        spinContinuous.startContinuity(entity, SPIN_TIME);
    }

    private void onSpinTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        spinTick++;
        if (spinTick % 2 == 0) {
            LuckySlotParticleEffect.spawnSpinningNumber(entity, entity.level, this.random.nextInt(10));
        }
    }

    private void onSpinEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        int rolled = entity.getPersistentData().getInt("kazi_pending_roll");
        entity.getPersistentData().remove("kazi_pending_roll");

        IAbilityData data = AbilityDataCapability.get(entity);
        if (data != null) {
            LuckySlotAbility luckySlots = data.getPassiveAbility(LuckySlotAbility.INSTANCE);
            if (luckySlots != null) {
                luckySlots.setSlotNumber(entity, rolled);
            }

            CasinoRollAbility casinoRoll = (CasinoRollAbility) data.getEquippedAbility(CasinoRollAbility.INSTANCE);

            if (rolled == 0) {
                // Zero penalty
                float damage = (float) entity.getMaxHealth() * 0.05f;
                entity.hurt(ZERO_ROLL, damage);
                // Clear any previous mode so old roll can't be used
                if (casinoRoll != null) {
                    casinoRoll.setModeForRoll(entity, 0);
                }
            } else {
                // Set Casino Roll mode for valid rolls
                if (casinoRoll != null) {
                    casinoRoll.setModeForRoll(entity, rolled);
                }
            }
        }

        // Spawn the final revealed number particle
        LuckySlotParticleEffect.spawnForNumber(entity, entity.level, rolled);

        // Action bar message
        if (entity instanceof PlayerEntity) {
            String color = rolled <= 3 ? "§c" : rolled <= 6 ? "§e" : "§a";
            String modeName = rolled == 0 ? "Bad luck..." : CasinoRollAbility.Mode.values()[rolled].getDisplayName().getString();
            ((PlayerEntity) entity).displayClientMessage(
                    new StringTextComponent("§7[Lucky Slots] §fYou rolled: " + color + rolled + " §7— " + modeName), true);
        }

        cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Slot Spin",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.ACTION,
                SlotSpinAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .build();
    }
}