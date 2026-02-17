//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import net.MrMagicalCart.cartaddon.abilities.axestyle.AxeHelper;
import net.MrMagicalCart.cartaddon.abilities.axestyle.TyrantCleaveAbility;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.axestyle.TyrantCleaveProjectile;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.kazi.kazimod.entities.projectiles.TyrantCleaveProjectileRework;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HealComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class TyrantCleaveRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "tyrant_cleave", new Pair[]{ImmutablePair.of("", (Object)null)});
    public static final AbilityCore<TyrantCleaveRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HealComponent healComponent = new HealComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    protected final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public TyrantCleaveRework(AbilityCore<TyrantCleaveRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.hitTrackerComponent, this.projectileComponent, this.chargeComponent, this.animationComponent, this.dealDamageComponent, this.rangeComponent, this.healComponent});
        this.isNew = true;
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addUseEvent(this::onUseEvent);
    }

    private Vector3d rotateYaw(Vector3d vec, float degrees) {
        double radians = Math.toRadians((double)degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vec.x * cos + vec.z * sin;
        double z = vec.z * cos - vec.x * sin;
        return new Vector3d(x, vec.y, z);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 3.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.RAISE_ARMS);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply(1.1, (double)0.0F, 1.1);
            entity.move(MoverType.SELF, speed);

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 4.0F)) {
                float weaponDamage = (float)entity.getMainHandItem().getItem().getDamage(entity.getMainHandItem());
                float maxhealthdamage = target.getHealth() * 0.1F + weaponDamage;
                float totaldamage = maxhealthdamage * 0.1F;
                if (this.hitTrackerComponent.canHit(target) && entity.canSee(target) && this.dealDamageComponent.hurtTarget(entity, target, maxhealthdamage)) {
                    this.dealDamageComponent.getBonusManager().removeBonus(AxeHelper.AXE_DAMAGE_BONUS);
                    this.healComponent.healTarget(entity, entity, totaldamage);
                }
            }
        }

        if (!entity.level.isClientSide) {
            Vector3d forward = entity.getLookAngle().normalize();
            Vector3d leftOffset = new Vector3d(-0.3, (double)0.0F, (double)0.0F);
            Vector3d rightOffset = new Vector3d(0.3, (double)0.0F, (double)0.0F);
            leftOffset = leftOffset.yRot((float)Math.toRadians((double)(-entity.yRot)));
            rightOffset = rightOffset.yRot((float)Math.toRadians((double)(-entity.yRot)));
            TyrantCleaveProjectile projLeft = new TyrantCleaveProjectile(entity.level, entity);
            projLeft.setPos(projLeft.getX() + leftOffset.x, projLeft.getY(), projLeft.getZ() + leftOffset.z);
            projLeft.shoot(forward.x, forward.y, forward.z, 4.5F, 0.1F);
            entity.level.addFreshEntity(projLeft);
            TyrantCleaveProjectile projRight = new TyrantCleaveProjectile(entity.level, entity);
            projRight.setPos(projRight.getX() + rightOffset.x, projRight.getY(), projRight.getZ() + rightOffset.z);
            projRight.shoot(forward.x, forward.y, forward.z, 4.5F, 0.1F);
            entity.level.addFreshEntity(projRight);
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.hitTrackerComponent.clearHits();
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 200.0F);
    }

    private TyrantCleaveProjectileRework createProjectile(LivingEntity entity) {
        TyrantCleaveProjectileRework proj = new TyrantCleaveProjectileRework(entity.level, entity);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Tyrant Cleave", AbilityCategory.STYLE, TyrantCleaveAbility::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F), ChargeComponent.getTooltip(3.0F)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(TyrantCleaveRework::canUnlock).build();
    }
}
