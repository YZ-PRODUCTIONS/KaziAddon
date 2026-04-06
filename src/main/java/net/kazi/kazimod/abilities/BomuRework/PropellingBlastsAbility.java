package net.kazi.kazimod.abilities.BomuRework;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class PropellingBlastsAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "propelling_blasts",
            new Pair[]{ImmutablePair.of("The user propels themselves forward using a series of small explosions.", (Object) null)}
    );

    private static final int MAX_STACKS = 10;
    private static final float SHORT_COOLDOWN_PER_STACK = 5.0F;
    private static final float LONG_COOLDOWN_PER_STACK = 20.0F;

    public static final AbilityCore<PropellingBlastsAbility> INSTANCE;

    private final DamageTakenComponent damageTakenComponent;
    private final StackComponent stackComponent;

    private boolean hasFallDamage;
    private boolean hadGravity;
    private int noGravityTime;

    public PropellingBlastsAbility(AbilityCore<PropellingBlastsAbility> core) {
        super(core);
        this.damageTakenComponent = (new DamageTakenComponent(this)).addOnAttackEvent(this::onDamageTaken);
        this.stackComponent = new StackComponent(this, MAX_STACKS);
        this.hasFallDamage = true;
        this.hadGravity = false;
        this.noGravityTime = 8;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.damageTakenComponent, this.stackComponent});
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::tickEvent);
        this.addEquipEvent(this::equipEvent);
    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        this.cooldownComponent.startCooldown(entity, LONG_COOLDOWN_PER_STACK * MAX_STACKS);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        int stacksUsed = 1;
        if (this.noGravityTime <= 0) {
            this.hadGravity = !entity.isNoGravity();
        }

        Vector3d movement = entity.getLookAngle().normalize();
        if (entity.isInWater()) {
            movement = movement.scale(1.75F);
            stacksUsed = this.stackComponent.getStacks();
        } else {
            movement = movement.scale(entity.isOnGround() ? 1.25F : 1.0F);
        }

        entity.setNoGravity(true);
        this.noGravityTime = 8;

        if (entity.xRot < -40.0F) {
            movement = movement.add(0.0F, -(movement.y - movement.y / 2.0F), 0.0F);
        }

        AbilityHelper.setDeltaMovement(entity, movement.x, movement.y, movement.z);
        this.stackComponent.addStacks(entity, this, -stacksUsed);
        this.hasFallDamage = false;

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS,
                2.0F, 1.5F + this.random.nextFloat() / 3.0F);

        if (!entity.level.isClientSide) {
            WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.BAKUGO.get(), entity,
                    entity.getX(), entity.getY() - 0.5, entity.getZ());
        }

        if (this.stackComponent.getStacks() <= 0) {
            super.cooldownComponent.startCooldown(entity, getCooldownTicks());
            this.stackComponent.setStacks(entity, this, MAX_STACKS);
        } else {
            super.cooldownComponent.startCooldown(entity, SHORT_COOLDOWN_PER_STACK);
        }
    }

    public void tickEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide
                && !this.hasFallDamage
                && this.stackComponent.getStacks() < this.stackComponent.getDefaultStacks()
                && entity.isOnGround()
                && entity.level.getGameTime() > this.getLastUseGametime() + 10L) {
            this.resetStacks(entity);
        }

        if (!entity.level.isClientSide && this.noGravityTime-- <= 0 && this.hadGravity) {
            entity.setNoGravity(false);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.resetStacks(entity);
            return 0.0F;
        }
        return damage;
    }

    private void resetStacks(LivingEntity entity) {
        if (this.stackComponent.getStacks() != this.stackComponent.getDefaultStacks()) {
            this.cooldownComponent.stopCooldown(entity);
            this.cooldownComponent.startCooldown(entity, getCooldownTicks());
        }
        this.stackComponent.setStacks(entity, this, MAX_STACKS);
        this.hasFallDamage = true;
    }

    private float getCooldownTicks() {
        return (float)(this.stackComponent.getDefaultStacks() - this.stackComponent.getStacks()) * LONG_COOLDOWN_PER_STACK;
    }

    public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putBoolean("hasFallDamage", this.hasFallDamage);
        nbt.putBoolean("hadGravity", this.hadGravity);
        return nbt;
    }

    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.hasFallDamage = nbt.getBoolean("hasFallDamage");
        this.hadGravity = nbt.getBoolean("hadGravity");
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Propelling Blasts", AbilityCategory.DEVIL_FRUITS, PropellingBlastsAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        AbilityHelper.createShortLongCooldownStat(SHORT_COOLDOWN_PER_STACK, LONG_COOLDOWN_PER_STACK),
                        StackComponent.getTooltip(MAX_STACKS)
                })
                .setUnlockCheck(PropellingBlastsAbility::canUnlock)
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceElement(SourceElement.EXPLOSION)
                .build();
    }
}
