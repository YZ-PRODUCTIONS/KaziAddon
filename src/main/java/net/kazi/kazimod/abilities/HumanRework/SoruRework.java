//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HumanRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class SoruRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "soru", new Pair[]{ImmutablePair.of("Allows the user to move at an extremely high speed in bursts", (Object)null)});
    private static final float LONG_COOLDOWN = 200.0F;
    private static final float SHORT_COOLDOWN = 10.0F;
    public static final AbilityCore<SoruRework> INSTANCE;
    private final StackComponent stackComponent = (new StackComponent(this, 5)).addStackChangeEvent(this::onStacksChange);

    public SoruRework(AbilityCore<SoruRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.stackComponent});
        this.setOGCD();
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent(this::equipEvent);
    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        this.cooldownComponent.startCooldown(entity, 200.0F);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        Vector3d forward = entity.getLookAngle();
        Vector3d horizontalForward = new Vector3d(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() > 1.0E-6D) {
            horizontalForward = horizontalForward.normalize();
        }

        Vector3d right = new Vector3d(-horizontalForward.z, 0.0D, horizontalForward.x);
        Vector3d dash = horizontalForward.scale((double)entity.zza).add(right.scale((double)(-entity.xxa)));

        if (dash.lengthSqr() <= 1.0E-6D) {
            dash = horizontalForward;
        } else {
            dash = dash.normalize();
        }

        Vector3d look = new Vector3d(dash.x * 1.75D, 0.0D, dash.z * 1.75D);

        if (entity.isInWater()) {
            look = look.multiply(0.2, 0.2, 0.2);
        }

        AbilityHelper.setDeltaMovement(entity, look);
        entity.addEffect(new EffectInstance((Effect)ModEffects.VANISH.get(), 5, 0, false, false));
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.stackComponent.addStacks(entity, this, -1);
    }

    private void onStacksChange(LivingEntity entity, IAbility ability, int stacks) {
        if (stacks <= 0) {
            super.cooldownComponent.startCooldown(entity, 200.0F);
            this.stackComponent.revertStacksToDefault(entity, this);
        }

        super.cooldownComponent.startCooldown(entity, 10.0F);
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        boolean raceCheck = props.isHuman() || DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.HITO_HITO_NO_MI);
        return raceCheck && props.getDoriki() >= (double)500.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Soru", AbilityCategory.RACIAL, SoruRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, AbilityHelper.createShortLongCooldownStat(10.0F, 200.0F)}).setUnlockCheck(SoruRework::canUnlock).build();
    }
}
