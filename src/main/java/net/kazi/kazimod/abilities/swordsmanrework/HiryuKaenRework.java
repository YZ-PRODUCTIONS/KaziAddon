//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.swordsmanrework;

import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.DropHitAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HiryuKaenRework extends DropHitAbility {
    private static final int COOLDOWN = 240;
    private static final float RANGE = 4.5F;
    private static final float DAMAGE = 20.0F;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "hiryu_kaen", new Pair[]{ImmutablePair.of("The user leaps into the air and releases a big flaming shockwave slash when landing", (Object)null)});
    public static final AbilityCore<HiryuKaenRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public HiryuKaenRework(AbilityCore<HiryuKaenRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.dealDamageComponent, this.rangeComponent});
        this.continuousComponent.addStartEvent(100, this::onStartContinuityEvent);
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
    }

    public void onLanding(LivingEntity entity) {
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 4.5F);
        targets.remove(entity);
        AbilityDamageSource source = (AbilityDamageSource)ModDamageSource.causeAbilityDamage(entity, this.getCore()).setSlash();

        for(LivingEntity target : targets) {
            if (this.hitTrackerComponent.canHit(target) && entity.canSee(target) && this.dealDamageComponent.hurtTarget(entity, target, 20.0F, source)) {
                target.setSecondsOnFire(4);
            }
        }

        if (!entity.level.isClientSide) {
            if (targets.size() > 0) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.HIRYU_KAEN.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        Vector3d speed = WyHelper.propulsion(entity, (double)1.0F, (double)1.0F);
        AbilityHelper.setDeltaMovement(entity, speed.x, 1.3, speed.z);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Hiryu: Kaen", AbilityCategory.STYLE, HiryuKaenRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), DealDamageComponent.getTooltip(20.0F), RangeComponent.getTooltip(4.5F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceElement(SourceElement.FIRE).setUnlockCheck(HiryuKaenRework::canUnlock).build();
    }
}