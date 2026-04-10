//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BrawlerRework;

import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoPistolProjectile;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.minecraft.potion.Effect;

public class FistsOfLoveBarrageRework extends Ability {
    public static final AbilityCore<FistsOfLoveBarrageRework> INSTANCE;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "fists_of_love_barrage", new Pair[]{ImmutablePair.of("The user launches a barrage of punches.", (Object)null)});
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::onContinuityStart).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::repeaterTriggerEvent).addStopEvent(this::repeaterStopEvent);
    private int waves = 60;
    private int punchesPerWave = 5;
    private static final float DAMAGE = 8.0F;
    private final PoolComponent poolComponent;
    private final ChangeStatsComponent changeStatsComponent;
    private final AnimationComponent animationComponent;
    private final HitTrackerComponent hitTrackerComponent;
    private final RangeComponent rangeComponent;
    private final DealDamageComponent dealDamageComponent;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;

    public FistsOfLoveBarrageRework(AbilityCore<FistsOfLoveBarrageRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.changeStatsComponent = new ChangeStatsComponent(this);
        this.animationComponent = new AnimationComponent(this);
        this.hitTrackerComponent = new HitTrackerComponent(this);
        this.rangeComponent = new RangeComponent(this);
        this.dealDamageComponent = new DealDamageComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.changeStatsComponent, this.poolComponent, this.chargeComponent, this.animationComponent, this.hitTrackerComponent, this.repeaterComponent, this.rangeComponent, this.dealDamageComponent});
        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            this.repeaterComponent.stop(entity);
        } else {
            this.chargeComponent.startCharging(entity, 60.0F);
        }

    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.PUNCH_RUSH);
        this.repeaterComponent.start(entity, this.waves, 1);
        this.changeStatsComponent.applyModifiers(entity);
    }

    private void repeaterTriggerEvent(LivingEntity entity, IAbility ability) {
        float speed = 4.4F;
        int projectileSpace = 2;
        float projDmageReduction = 0.6F;

        for(int i = 0; i < this.punchesPerWave; ++i) {
            AbilityProjectileEntity projectile = new GomuGomuNoPistolProjectile(entity.level, entity);
            projectile.setEntityCollisionSize((double)1.25F);
            projectile.setMaxLife(3);
            projectile.setDamage(projectile.getDamage() * (1.0F - projDmageReduction));
            projectile.setMaxLife((int)((double)projectile.getMaxLife() * (double)0.75F));
            double px = entity.getX() + WyHelper.randomWithRange(-projectileSpace, projectileSpace) + WyHelper.randomDouble();
            double py = entity.getEyeY() + WyHelper.randomWithRange(0, projectileSpace) + WyHelper.randomDouble();
            double pz = entity.getZ() + WyHelper.randomWithRange(-projectileSpace, projectileSpace) + WyHelper.randomDouble();
            projectile.moveTo(px, py, pz, 0.0F, 0.0F);
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 3.0F);
        }

        entity.swing(Hand.MAIN_HAND, true);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply(0.7, (double)0.0F, 0.7);
            entity.move(MoverType.SELF, speed);
        }

        entity.addEffect(new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 5, 0));


        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 3.25F, 4.5F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                this.dealDamageComponent.hurtTarget(entity, target, 8.0F);
                Vector3d knockback = entity.getLookAngle().normalize().multiply(1.1, (double)1.0F, 1.1).add((double)0.0F, 0.1, (double)0.0F);
                target.setDeltaMovement(knockback);
            }

            this.hitTrackerComponent.clearHits();
        }

        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)3.0F);
        double x = mop.getLocation().x + WyHelper.randomDouble();
        double z = mop.getLocation().z + WyHelper.randomDouble();
        double y = mop.getLocation().y + WyHelper.randomDouble();
        double i = mop.getLocation().x + WyHelper.randomDouble();
        double j = mop.getLocation().z + WyHelper.randomDouble();
        double k = mop.getLocation().y + WyHelper.randomDouble();
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();

        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 4.25F, 5.0F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 35.0F);
                AbilityHelper.disableAbilities(target, 100, (abl) -> abl.hasComponent(ModAbilityKeys.POOL) && ((PoolComponent)abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE));
                if (flag && !entity.level.isClientSide) {
                }

                Vector3d knockback = entity.getLookAngle().normalize().multiply((double)3.0F, (double)1.0F, (double)3.0F).add((double)0.0F, (double)0.75F, (double)0.0F);
                target.setDeltaMovement(knockback);
            }
        }

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SHIGAN_SFX.get(), SoundCategory.PLAYERS, 3.0F, 1.1F);
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.changeStatsComponent.removeModifiers(entity);
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);
    }

    private void repeaterStopEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.stopCharging(entity);
        super.cooldownComponent.startCooldown(entity, WyHelper.secondsToTicks(10.0F));
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBrawler() && questProps.hasFinishedQuest(CartQuests.BRAWLER_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Fists of Love: Barrage", AbilityCategory.STYLE, FistsOfLoveBarrageRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(8.0F), ChargeComponent.getTooltip(40.0F), CooldownComponent.getTooltip(180.0F), RangeComponent.getTooltip(3.25F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(FistsOfLoveBarrageRework::canUnlock).build();
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(UUID.fromString("29b5b8cc-6507-42a6-bc2b-25c3e574e4b9"), INSTANCE, "Fists of Love: Barrage Step Height Modifier", (double)1.0F, Operation.ADDITION);
    }
}
