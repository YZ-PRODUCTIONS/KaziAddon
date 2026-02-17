//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BlacklegRework;

import java.util.Iterator;
import java.util.List;

import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartDiableJambeAbility;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
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
import xyz.pixelatedw.mineminenomi.abilities.blackleg.DiableJambeAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class PartyTableKickCourseRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "party_table_kick_course", new Pair[]{ImmutablePair.of("The user does a hand stand on the ground, legs spread out spinning and dealing damage to all nearby enemies", (Object)null)});
    private static final float COOLDOWN = 240.0F;
    private static final float RANGE = 2.5F;
    private static final float DAMAGE = 25.0F;
    public static final AbilityCore<PartyTableKickCourseRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::onStartContinuityEvent).addEndEvent(this::onStopContinuityEvent).addTickEvent(this::duringContinuityEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public PartyTableKickCourseRework(AbilityCore<PartyTableKickCourseRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.animationComponent, this.dealDamageComponent, this.rangeComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 20.0F);
    }

    private boolean onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.HAND_STAND_SPIN);
        return true;
    }

    private boolean onStopContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, 240.0F);
        return true;
    }

    private boolean duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive() && entity.isOnGround()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply((double)0.25F, (double)0.0F, (double)0.25F);
            entity.move(MoverType.SELF, speed);
        }

        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1));
        CartDiableJambeAbility diableJambeAbility = (CartDiableJambeAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartDiableJambeAbility.INSTANCE);
        List<LivingEntity> list = this.rangeComponent.getTargetsInArea(entity, 3.0F);
        Iterator var4 = list.iterator();

        boolean isAbilityEnabled;
        LivingEntity target;
        float dmg;
        for(isAbilityEnabled = diableJambeAbility != null && diableJambeAbility.isContinuous(); var4.hasNext(); ) {
            target = (LivingEntity)var4.next();
            dmg = 25.0F;
            if (isAbilityEnabled) {
                if (diableJambeAbility.isIfrit()) {
                    dmg = 60.0F;
                } else if (!diableJambeAbility.isIfrit()) {
                    dmg = 50.0F;
                }
            } else {
                dmg = 40.0F;
            }

            if (this.dealDamageComponent.hurtTarget(entity, target, dmg)) {
                Vector3d speed = WyHelper.propulsion(entity, (double)1.5F, (double)1.5F);
                AbilityHelper.setDeltaMovement(target, speed.x, (double)1.5F, speed.z);
            }
        }

        if (!entity.level.isClientSide && list.size() > 0) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        if (isAbilityEnabled) {
            if (!diableJambeAbility.isIfrit()) {
                WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.PARTY_TABLE_KICK.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            } else if (diableJambeAbility.isIfrit()) {
                WyHelper.spawnParticleEffect((ParticleEffect) CartParticleEffects.PARTYTABLE_IFRIT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            }
        }

        return true;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBlackLeg() && questProps.hasFinishedQuest(ModQuests.BLACK_LEG_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Party-Table Kick Course", AbilityCategory.STYLE, PartyTableKickCourseRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), DealDamageComponent.getTooltip(25.0F), RangeComponent.getTooltip(2.5F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setUnlockCheck(PartyTableKickCourseRework::canUnlock).build();
    }
}
