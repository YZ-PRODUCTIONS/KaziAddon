//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.ItoRework;

import net.MrMagicalCart.cartaddon.abilities.mochi2.MochiHelper;
import net.MrMagicalCart.cartaddon.entities.projectiles.itoextra.GodThreadProjectile;
import net.kazi.kazimod.entities.projectiles.GodThreadProjectileRework;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GodThreadRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "god_thread", new Pair[]{ImmutablePair.of("The user creates sixteen razor-sharp string spears, eight on each side, charging them for a moment before unleashing them all at a single point.", (Object)null)});
    private static final int COOLDOWN = 1100;
    private static final int CHARGE_TICKS = 40;
    public static final AbilityCore<GodThreadRework> INSTANCE;
    private static final float PROJECTILE_SPEED = 4.25F;
    private static final double SIDE_OFFSET = (double)6.0F;
    private static final double VERTICAL_SPREAD = (double)1.0F;
    private static final double FORWARD_OFFSET = (double)1.0F;
    private static final double MAX_TARGET_DISTANCE = (double)64.0F;
    private static final float DAMAGE_MULTIPLIER = 2.5F;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::onChargeStart).addEndEvent(this::onChargeEnd);

    public GodThreadRework(AbilityCore<GodThreadRework> core) {
        super(core);
        this.isNew = true;
        this.addCanUseCheck(MochiHelper::hasHaki);
        this.addComponents(new AbilityComponent[]{this.chargeComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity user, IAbility ability) {
        if (!user.level.isClientSide) {
            if (!this.cooldownComponent.isOnCooldown()) {
                if (!this.chargeComponent.isCharging()) {
                    this.chargeComponent.startCharging(user, 40.0F);
                }

            }
        }
    }

    private void onChargeStart(LivingEntity user, IAbility ability) {
    }

    private void onChargeEnd(LivingEntity user, IAbility ability) {
        if (!user.level.isClientSide) {
            Vector3d look = user.getLookAngle().normalize();
            if (look.lengthSqr() == (double)0.0F) {
                look = new Vector3d((double)0.0F, (double)0.0F, (double)1.0F);
            }

            Vector3d forwardHoriz = (new Vector3d(look.x, (double)0.0F, look.z)).normalize();
            if (forwardHoriz.lengthSqr() == (double)0.0F) {
                forwardHoriz = new Vector3d((double)0.0F, (double)0.0F, (double)1.0F);
            }

            Vector3d right = (new Vector3d(-forwardHoriz.z, (double)0.0F, forwardHoriz.x)).normalize();
            Vector3d basePos = new Vector3d(user.getX() + forwardHoriz.x * (double)1.0F, user.getY() + (double)user.getBbHeight() * 0.6, user.getZ() + forwardHoriz.z * (double)1.0F);
            AimData aim = this.getAim(user);
            this.spawnSideColumn(user, basePos, right, 1, aim.targetPos, aim.targetEntity);
            this.spawnSideColumn(user, basePos, right, -1, aim.targetPos, aim.targetEntity);
            this.cooldownComponent.startCooldown(user, 1100.0F);
        }
    }

    private void spawnSideColumn(LivingEntity user, Vector3d basePos, Vector3d right, int sideDir, Vector3d targetPos, LivingEntity targetEntity) {
        Vector3d sideOffset = right.scale((double)6.0F * (double)sideDir);

        for(int i = 0; i < 8; ++i) {
            double verticalOffset = ((double)i - (double)3.5F) * (double)1.0F;
            Vector3d spawnPos = basePos.add(sideOffset).add((double)0.0F, verticalOffset, (double)0.0F);
            GodThreadProjectileRework proj = new GodThreadProjectileRework(user.level, user, this);
            proj.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            proj.setEntityCollisionSize((double)2.75F);
            proj.setDamage(proj.getDamage() * 2.5F);
            if (targetEntity != null && targetEntity.isAlive()) {
                proj.setHomingTarget(targetEntity);
            }

            Vector3d dir = targetPos.subtract(spawnPos).normalize();
            if (dir.lengthSqr() == (double)0.0F) {
                dir = new Vector3d((double)0.0F, (double)0.0F, (double)1.0F);
            }

            proj.shoot(dir.x, dir.y, dir.z, 4.25F, 0.0F);
            user.level.addFreshEntity(proj);
        }

    }

    private AimData getAim(LivingEntity user) {
        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(user, (double)64.0F);
        if (mop instanceof EntityRayTraceResult) {
            EntityRayTraceResult er = (EntityRayTraceResult)mop;
            if (er.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity)er.getEntity();
                Vector3d pos = target.position().add((double)0.0F, (double)target.getBbHeight() * 0.6, (double)0.0F);
                return new AimData(target, pos);
            }
        }

        Vector3d eye = user.getEyePosition(1.0F);
        Vector3d look = user.getLookAngle().normalize();
        if (look.lengthSqr() == (double)0.0F) {
            look = new Vector3d((double)0.0F, (double)0.0F, (double)1.0F);
        }

        Vector3d fallback = eye.add(look.scale((double)64.0F));
        Vector3d hit = mop != null && mop.getType() != Type.MISS ? mop.getLocation() : fallback;
        return new AimData((LivingEntity)null, hit);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("God Thread", AbilityCategory.DEVIL_FRUITS, GodThreadRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE}).setSourceHakiNature(SourceHakiNature.HARDENING).build();
    }

    private static class AimData {
        final LivingEntity targetEntity;
        final Vector3d targetPos;

        AimData(LivingEntity targetEntity, Vector3d targetPos) {
            this.targetEntity = targetEntity;
            this.targetPos = targetPos;
        }
    }
}
