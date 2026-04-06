package net.kazi.kazimod.abilities.Kyoka;

import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class IllusionCounterAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "illusion_counter",
            new Pair[]{ImmutablePair.of("When struck, the user instantly leaves behind a fake body, avoids the blow, and appears behind the attacker before they can notice the swap.", null)}
    );
    private static final float HOLD_TIME = 80.0F;
    private static final float COOLDOWN = 240.0F;
    private static final float COUNTER_RANGE = 14.0F;
    public static final AbilityCore<IllusionCounterAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addEndEvent(this::endEvent);
    private final DamageTakenComponent damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);

    public IllusionCounterAbility(AbilityCore<IllusionCounterAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.damageTakenComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, HOLD_TIME);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous()) {
            return damage;
        }

        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity) || sourceEntity.distanceTo(entity) > COUNTER_RANGE) {
            return damage;
        }

        LivingEntity attacker = (LivingEntity) sourceEntity;
        Vector3d oldPos = entity.position();
        ShadowDoppelmanEntity decoy = KyokaCloneHelper.spawnClone(entity, oldPos, 0.5F, true);

        Vector3d targetLook = attacker.getLookAngle().normalize();
        Vector3d teleportPos = attacker.position().add(targetLook.scale(-1.75D));
        entity.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
        entity.lookAt(EntityAnchorArgument.Type.EYES, attacker.position().add(0.0D, attacker.getEyeHeight(), 0.0D));
        entity.addEffect(new EffectInstance(Effects.INVISIBILITY, 10, 0, false, false));
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 10, 2, false, false));

        if (decoy != null) {
            decoy.lookAt(EntityAnchorArgument.Type.EYES, attacker.position().add(0.0D, attacker.getEyeHeight(), 0.0D));
        }

        entity.level.playSound(null, entity.blockPosition(), ModSounds.FUTURE_SIGHT_HIT.get(), SoundCategory.PLAYERS, 1.0F, 1.15F);
        this.continuousComponent.stopContinuity(entity);
        return 0.0F;
    }

    private void endEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Illusion Counter", AbilityCategory.DEVIL_FRUITS, IllusionCounterAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME)
                )
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT})
                .build();
    }
}
