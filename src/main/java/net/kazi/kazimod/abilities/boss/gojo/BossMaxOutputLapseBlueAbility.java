package net.kazi.kazimod.abilities.boss.gojo;

import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.kazi.kazimod.entities.boss.gojo.GojoCooldownTracker;
import net.kazi.kazimod.entities.projectiles.LapseBlueProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;

public class BossMaxOutputLapseBlueAbility extends Ability {

    private static final float  COOLDOWN         = 400.0f;
    private static final double PROJECTILE_SPEED = 2.0;

    public static final AbilityCore<BossMaxOutputLapseBlueAbility> INSTANCE;

    private final AnimationComponent animationComponent;

    public BossMaxOutputLapseBlueAbility(final AbilityCore<BossMaxOutputLapseBlueAbility> core) {
        super(core);
        this.animationComponent = new AnimationComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{ this.animationComponent });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;

        final LivingEntity target = (entity instanceof MobEntity)
                ? ((MobEntity) entity).getTarget() : null;
        if (target == null || !target.isAlive()) return;

        final Vector3d firePos = entity.position().add(0, entity.getEyeHeight() * 0.9, 0);
        final Vector3d dir     = BossAimHelper.leadTarget(entity, target, PROJECTILE_SPEED);

        final LapseBlueProjectile proj = new LapseBlueProjectile(entity.level, entity, (Ability) ability);
        proj.setPos(firePos.x, firePos.y, firePos.z);
        proj.setDeltaMovement(dir.scale(PROJECTILE_SPEED));
        entity.level.addFreshEntity(proj);

        super.cooldownComponent.startCooldown(entity, COOLDOWN);

        // ── Record timestamp for the mutual 5-second cooldown gate ──────────
        GojoCooldownTracker.recordMaxBlue(entity.getUUID());
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Boss: Max Output Lapse Blue",
                AbilityCategory.DEVIL_FRUITS,
                BossMaxOutputLapseBlueAbility::new
        ).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(SourceType.INDIRECT, SourceType.PROJECTILE)
                // Mirror the BossHollowPurpleAbility safeguard so MMNM packet encoding
                // always has a non-null identifier even if the boss ability instance is
                // serialized outside the normal registry-backed lifecycle.
                .setPhantomKey(new net.minecraft.util.ResourceLocation("kazimod", "boss_max_output_lapse_blue"))
                .build();
    }
}
