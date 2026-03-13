//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.swordsmanrework;

import java.util.List;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ShiShishiSonsonRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "shi_shishi_sonson", new Pair[]{ImmutablePair.of("The user dashes forward and rapidly slashes the opponent", (Object)null)});
    private static final float COOLDOWN = 180.0F;
    private static final int CHARGE_TIME = 20;
    private static final float DAMAGE = 35.0F;
    private static final float RANGE = 2.5F;
    private static final float MAX_TELEPORT_DISTANCE = 30.0F;
    public static final AbilityCore<ShiShishiSonsonRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public ShiShishiSonsonRework(AbilityCore<ShiShishiSonsonRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, 20.0F);
        }

    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.ITTORYU_CHARGE);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        BlockPos.Mutable blockPos = WyHelper.rayTraceBlockSafe(entity, 30.0F).mutable();
        AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();
        Vector3d startPos = entity.position();
        float actualTeleportDistance = 30.0F;

        for(double f = (double)0.0F; f < (double)1.0F; f += 0.13) {
            double x = MathHelper.lerp(f, startPos.x(), (double)blockPos.getX());
            double y = MathHelper.lerp(f, startPos.y(), (double)blockPos.getY());
            double z = MathHelper.lerp(f, startPos.z(), (double)blockPos.getZ());
            Vector3d pos = new Vector3d(x, y, z);
            List<ProjectileEntity> projectiles = WyHelper.getNearbyEntities(pos, entity.level, (double)entity.getBbWidth(), (double)entity.getBbHeight(), (double)entity.getBbWidth(), (Predicate)null, new Class[]{ProjectileEntity.class});
            if (!projectiles.isEmpty()) {
                projectiles.sort(TargetHelper.closestComparator(startPos));
                actualTeleportDistance = MathHelper.sqrt(((ProjectileEntity)projectiles.get(0)).distanceToSqr(startPos));
                break;
            }
        }

        blockPos.set(WyHelper.rayTraceBlockSafe(entity, actualTeleportDistance));

        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, actualTeleportDistance, 2.5F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 35.0F, source);
                if (flag && !entity.level.isClientSide) {
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                }
            }
        }

        entity.stopRiding();
        entity.teleportToWithTicket((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.DASH_ABILITY_SWOOSH_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isSwordsman() && questProps.hasFinishedQuest(CartQuests.SWORDSMAN_TRIAL_01);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Shi Shishi Sonson", AbilityCategory.STYLE, ShiShishiSonsonRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(180.0F), ChargeComponent.getTooltip(20.0F), DealDamageComponent.getTooltip(35.0F), RangeComponent.getTooltip(30.0F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setUnlockCheck(ShiShishiSonsonRework::canUnlock).build();
    }
}
