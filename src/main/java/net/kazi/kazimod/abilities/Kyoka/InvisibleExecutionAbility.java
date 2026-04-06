package net.kazi.kazimod.abilities.Kyoka;

import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import java.util.ArrayList;
import java.util.Arrays;

public class InvisibleExecutionAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "invisible_execution",
            new Pair[]{ImmutablePair.of("Leaves a false self behind while the real user vanishes from sight and repositions unnoticed. The illusion breaks the moment the user attacks.", null)}
    );
    private static final float HOLD_TIME = 160.0F;
    private static final float COOLDOWN = 260.0F;
    public static final AbilityCore<InvisibleExecutionAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::startEvent)
            .addTickEvent(this::tickEvent)
            .addEndEvent(this::endEvent);
    private final PoolComponent poolComponent = new PoolComponent(this, ModAbilityPools.DODGE_ABILITY, new AbilityPool2[0]);
    private final DamageTakenComponent damageTakenComponent = (new DamageTakenComponent(this)).addOnAttackEvent(this::onDamageTakenEvent);
    private ShadowDoppelmanEntity decoy;

    public InvisibleExecutionAbility(AbilityCore<InvisibleExecutionAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.poolComponent, this.damageTakenComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            entity.setInvisible(true);
            entity.addEffect(new EffectInstance(ModEffects.SUKE_INVISIBILITY.get(), Integer.MAX_VALUE, 0, false, false, true));
            AbilityHelper.disableAbilities(entity, 20, abl -> abl != this);
            this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
        }
    }

    private void startEvent(LivingEntity entity, IAbility ability) {
        entity.setInvisible(true);
        if (!entity.hasEffect(ModEffects.SUKE_INVISIBILITY.get())) {
            entity.addEffect(new EffectInstance(ModEffects.SUKE_INVISIBILITY.get(), Integer.MAX_VALUE, 0, false, false, true));
        }
        entity.addEffect(new EffectInstance(ModEffects.VANISH.get(), 20, 0, false, false));
        AbilityHelper.disableAbilities(entity, 20, abl -> abl != this);

        Vector3d startingPos = entity.position();
        entity.level.playSound(null, entity.blockPosition(), ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.3F);
        this.decoy = KyokaCloneHelper.spawnClone(entity, startingPos, 0.25F, true);
    }

    private void tickEvent(LivingEntity entity, IAbility ability) {
        entity.setInvisible(true);
        if (!entity.hasEffect(ModEffects.SUKE_INVISIBILITY.get())) {
            entity.addEffect(new EffectInstance(ModEffects.SUKE_INVISIBILITY.get(), Integer.MAX_VALUE, 0, false, false, true));
        }
        entity.addEffect(new EffectInstance(ModEffects.VANISH.get(), 5, 0, false, false));
        AbilityHelper.disableAbilities(entity, 5, abl -> abl != this);
        if (entity.swinging || this.decoy == null || !this.decoy.isAlive()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void endEvent(LivingEntity entity, IAbility ability) {
        entity.setInvisible(false);
        entity.removeEffect(ModEffects.SUKE_INVISIBILITY.get());
        entity.removeEffect(ModEffects.VANISH.get());
        if (this.decoy != null && this.decoy.isAlive()) {
            this.decoy.remove();
        }
        this.decoy = null;
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private float onDamageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.continuousComponent.isContinuous()) {
            return damage;
        }

        boolean unavoidable = damageSource instanceof ModDamageSource && ((ModDamageSource) damageSource).isUnavoidable();
        if (unavoidable) {
            return damage;
        }

        ArrayList<String> acceptableInstantSources = new ArrayList<>(Arrays.asList("mob", "player", "ability_projectile", "ability"));
        boolean dodgeable = (damageSource.getDirectEntity() instanceof LivingEntity || damageSource.getDirectEntity() instanceof ProjectileEntity)
                && acceptableInstantSources.contains(damageSource.getMsgId());

        if (dodgeable && !entity.level.isClientSide) {
            SoundEvent sfx = ModSounds.DODGE_1.get();
            if (entity.getRandom().nextBoolean()) {
                sfx = ModSounds.DODGE_2.get();
            }
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), sfx, SoundCategory.PLAYERS, 1.0F, 0.75F + entity.getRandom().nextFloat() / 2.0F);
        }

        return 0.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Invisible Execution", AbilityCategory.DEVIL_FRUITS, InvisibleExecutionAbility::new))
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
