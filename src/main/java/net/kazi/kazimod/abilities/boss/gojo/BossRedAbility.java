package net.kazi.kazimod.abilities.boss.gojo;

import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.kazi.kazimod.entities.boss.gojo.GojoCooldownTracker;
import net.kazi.kazimod.entities.projectiles.RedProjectile;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;

public class BossRedAbility extends Ability {

    private static final float  COOLDOWN         = 100.0f;
    private static final double PROJECTILE_SPEED = 2.8;

    public static final double MAX_OUTPUT_RANGE = 10.0;

    public static final AbilityCore<BossRedAbility> INSTANCE;

    public BossRedAbility(final AbilityCore<BossRedAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[0]);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;

        final LivingEntity target = (entity instanceof MobEntity)
                ? ((MobEntity) entity).getTarget() : null;
        if (target == null || !target.isAlive()) return;

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                KaziSounds.RED_CHARGE_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);

        final Vector3d firePos = entity.position().add(0, entity.getEyeHeight() * 0.9, 0);
        final Vector3d dir     = BossAimHelper.leadTarget(entity, target, PROJECTILE_SPEED);

        final RedProjectile proj = new RedProjectile(entity.level, entity, (Ability) ability);
        proj.setPos(firePos.x, firePos.y, firePos.z);
        proj.setDeltaMovement(dir.scale(PROJECTILE_SPEED));
        entity.level.addFreshEntity(proj);

        if (entity.level instanceof ServerWorld) {
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(
                    entity, new SAnimateHandPacket(entity, 0));
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                KaziSounds.RED_FIRE_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);

        super.cooldownComponent.startCooldown(entity, COOLDOWN);

        // ── Record timestamp for the mutual 5-second cooldown gate ──────────
        GojoCooldownTracker.recordRed(entity.getUUID());
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Boss: Red",
                AbilityCategory.DEVIL_FRUITS,
                BossRedAbility::new
        ).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(SourceType.INDIRECT, SourceType.PROJECTILE)
                .setPhantomKey(new net.minecraft.util.ResourceLocation("kazimod", "boss_red"))
                .build();
    }
}
