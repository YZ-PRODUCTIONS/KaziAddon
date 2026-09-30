package net.kazi.kazimod.abilities.NikyuRework;

import java.util.EnumSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.PainEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class PainRepelRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"mineminenomi", (String)"pain_repel", (Pair[])new Pair[]{ImmutablePair.of((Object)"Extracts all the damage their target has suffered condensing it into a small paw-shaped ball.", null)});
    private static final float COOLDOWN = 6000.0f;
    public static final AbilityCore<PainRepelRework> INSTANCE = new AbilityCore.Builder<PainRepelRework>("Pain Repel", AbilityCategory.DEVIL_FRUITS, PainRepelRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip((float)600.0f), CooldownComponent.getTooltip((float)6000.0f)}).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final HitTriggerComponent hitTriggerComponent = new HitTriggerComponent((IAbility)this).addTryHitEvent(100, this::tryHitEvent);
    private final SwingTriggerComponent swingTriggerComponent = new SwingTriggerComponent((IAbility)this).addSwingEvent(0, this::onSwing);
    private Interval painAddInterval = new Interval(10);
    private LivingEntity target;
    private State state = State.IDLE;
    private float pain = 0.0f;
    private float initialTargetHealthDiff = 0.0f;

    public PainRepelRework(AbilityCore<PainRepelRework> core) {
        super(core);
        this.isNew = true;
        this.hitTriggerComponent.setBypassSameGroupProtection();
        super.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTriggerComponent, this.swingTriggerComponent});
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 600.0f);
    }

    private HitTriggerComponent.HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (target.getHealth() >= target.getMaxHealth()) {
            return HitTriggerComponent.HitResult.PASS;
        }
        if (AbilityHelper.isTargetBlocking((LivingEntity)entity, (LivingEntity)target)) {
            return HitTriggerComponent.HitResult.PASS;
        }
        if (this.continuousComponent.isContinuous() && entity.getMainHandItem().isEmpty() && this.state == State.IDLE) {
            this.painAddInterval.restartIntervalToZero();
            this.state = State.HEALING;
            this.target = target;
            this.initialTargetHealthDiff = target.getMaxHealth() - target.getHealth();
            return HitTriggerComponent.HitResult.FAIL;
        }
        return HitTriggerComponent.HitResult.PASS;
    }

    private void onSwing(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.continuousComponent.isContinuous() && entity.getMainHandItem().isEmpty() && this.state == State.READY) {
            PainEntity proj = new PainEntity(entity.level, entity);
            proj.setDamage(this.pain);
            proj.shootFromRotation((Entity)entity, entity.xRot, entity.yRot, 0.0f, 2.0f, 1.0f);
            entity.level.addFreshEntity((Entity)proj);
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.state != State.IDLE && this.target != null && this.state == State.HEALING) {
            float diff;
            this.target.xRot = this.target.xRotO;
            this.target.yRot = this.target.yRotO;
            if (this.target instanceof PlayerEntity) {
                EnumSet<SPlayerPositionLookPacket.Flags> flags = EnumSet.of(SPlayerPositionLookPacket.Flags.X, SPlayerPositionLookPacket.Flags.Y, SPlayerPositionLookPacket.Flags.Z);
                ((ServerPlayerEntity)this.target).connection.teleport(this.target.getX(), this.target.getY(), this.target.getZ(), this.target.yRotO, this.target.xRotO, flags);
            }
            this.target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1));
            entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1));
            if (this.painAddInterval.canTick()) {
                float diff2 = this.target.getMaxHealth() / 10.0f;
                this.pain += diff2;
                this.target.heal(diff2);
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PAD_HO_SFX.get(), SoundCategory.PLAYERS, 2.0f, 1.0f);
            }
            if (((diff = this.target.getMaxHealth() - this.target.getHealth()) <= 0.0f || this.pain >= this.initialTargetHealthDiff) && !entity.swinging) {
                this.state = State.READY;
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.target = null;
        this.state = State.IDLE;
        this.pain = 0.0f;
        this.initialTargetHealthDiff = 0.0f;
        this.cooldownComponent.startCooldown(entity, 6000.0f);
    }

    private static enum State {
        IDLE,
        HEALING,
        READY;

    }
}

