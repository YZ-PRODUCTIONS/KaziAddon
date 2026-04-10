package net.kazi.kazimod.abilities.OpeRework;

import java.util.UUID;
import net.kazi.kazimod.entities.projectiles.PunctureWilleProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import net.minecraft.world.server.ServerWorld;

public class PunctureWilleRework extends Ability {

    private static final int COOLDOWN = 1000;
    private static final int CHARGE_TICKS = 100;
    private static final float PROJECTILE_SPEED = 4.0F;
    private UUID activeProjectileId;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi",
            "puncture_wille",
            new Pair[]{ImmutablePair.of(
                    "The user locks into a charge, then fires an extended K-Room sword projectile that impales targets before throwing them away.",
                    null)}
    );

    public static final AbilityCore<PunctureWilleRework> INSTANCE;

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::tickChargeEvent)
            .addEndEvent(this::endChargeEvent);

    public PunctureWilleRework(AbilityCore<PunctureWilleRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.animationComponent, this.chargeComponent});
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::useEvent);
        this.addTickEvent(this::onAbilityTick);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            return;
        }

        if (!hasKRoomActive(entity)) {
            entity.sendMessage(new StringTextComponent("You need to activate K-Room!"), entity.getUUID());
            return;
        }

        this.chargeComponent.startCharging(entity, CHARGE_TICKS);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.activeProjectileId = null;
        this.animationComponent.start(entity, ModAnimations.POINT_WEAPON);
        this.setSwordStretch(entity, true, 2.2F);
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
        if (!entity.isOnGround() && entity.getDeltaMovement().y < 0.0D) {
            AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x, 0.0D, entity.getDeltaMovement().z);
            entity.fallDistance = 0.0F;
        }
        float progress = this.chargeComponent.getChargePercentage();
        this.setSwordStretch(entity, true, 2.2F + progress * 2.3F);

        if (entity.tickCount % 10 == 0) {
            entity.level.playSound(null, entity.blockPosition(), (SoundEvent) ModSounds.ROOM_CHARGE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.9F);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        try {
            if (!hasKRoomActive(entity)) {
                entity.sendMessage(new StringTextComponent("You need K-Room active to fire Puncture Wille!"), entity.getUUID());
                return;
            }

            if (!AbilityHelper.canUseSwordsmanAbilities(entity)) {
                entity.sendMessage(new StringTextComponent("You need a sword to use this ability!"), entity.getUUID());
                return;
            }

            PunctureWilleProjectile projectile = new PunctureWilleProjectile(entity.level, entity);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, PROJECTILE_SPEED, 0.0F);
            this.activeProjectileId = projectile.getUUID();
            entity.level.addFreshEntity(projectile);
            entity.level.playSound(null, entity.blockPosition(), (SoundEvent) ModSounds.EL_THOR_SFX.get(), SoundCategory.PLAYERS, 10.0F, 1.25F);
            consumeKRoom(entity);
            this.cooldownComponent.startCooldown(entity, COOLDOWN);
        } finally {
            this.clearSwordStretch(entity);
        }
    }

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            return;
        }

        if (this.activeProjectileId == null) {
            this.clearSwordStretch(entity);
            this.animationComponent.stop(entity);
            return;
        }

        if (!entity.level.isClientSide && entity.level instanceof ServerWorld) {
            if (((ServerWorld) entity.level).getEntity(this.activeProjectileId) == null) {
                this.clearProjectileState(entity, this.activeProjectileId);
            }
        }
    }

    public boolean hasKRoomActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        if (props == null) {
            return false;
        }

        KRoomAnesthesiaRework kRoom = (KRoomAnesthesiaRework) props.getEquippedAbility(KRoomAnesthesiaRework.INSTANCE);
        return kRoom != null && kRoom.isContinuous();
    }

    private void consumeKRoom(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        if (props == null) {
            return;
        }

        KRoomAnesthesiaRework kRoom = (KRoomAnesthesiaRework) props.getEquippedAbility(KRoomAnesthesiaRework.INSTANCE);
        if (kRoom != null && kRoom.isContinuous()) {
            kRoom.setAbilityUsed(true);
        }
    }

    private void setSwordStretch(LivingEntity entity, boolean active, float scale) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        if (player.getMainHandItem().isEmpty()) {
            return;
        }

        if (active) {
            player.getMainHandItem().getOrCreateTag().putBoolean("punctureWilleSwordActive", true);
            player.getMainHandItem().getTag().putFloat("punctureWilleSwordScale", scale);
        } else if (player.getMainHandItem().hasTag()) {
            player.getMainHandItem().getTag().putBoolean("punctureWilleSwordActive", false);
            player.getMainHandItem().getTag().putFloat("punctureWilleSwordScale", 1.0F);
        }
    }

    public boolean isBusy() {
        return this.chargeComponent.isCharging() || this.activeProjectileId != null;
    }

    public void clearActiveProjectile(UUID projectileId) {
        if (projectileId != null && projectileId.equals(this.activeProjectileId)) {
            this.activeProjectileId = null;
        }
    }

    public void clearProjectileState(LivingEntity entity, UUID projectileId) {
        this.clearActiveProjectile(projectileId);
        this.clearSwordStretch(entity);
        this.animationComponent.stop(entity);
    }
    public void clearSwordStretch(LivingEntity entity) {
        this.setSwordStretch(entity, false, 1.0F);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = new AbilityCore.Builder("Puncture Wille", AbilityCategory.DEVIL_FRUITS, PunctureWilleRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()})
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip((float) CHARGE_TICKS),
                        CooldownComponent.getTooltip((float) COOLDOWN)
                })
                .setSourceElement(SourceElement.LIGHTNING)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setUnlockCheck(PunctureWilleRework::canUnlock)
                .build();
    }
}
