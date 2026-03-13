//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.SpearRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.GroundParticlesEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class VaultRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "vault", new Pair[]{ImmutablePair.of("Punches the ground to cause a quake that damages everyone around", (Object)null)});
    public static final ParticleEffect PARTICLES = new GroundParticlesEffect(7, 100);
    private static final float COOLDOWN = 280.0F;
    private static final float DAMAGE = 20.0F;
    private static final float RANGE = 9.0F;
    public static final AbilityCore<VaultRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addTickEvent(this::onTickContinuityEvent).addEndEvent(this::onEndContinuityEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private boolean wasAirborne = false;
    private boolean didSecondSlam = false;

    public VaultRework(AbilityCore<VaultRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent});
        this.addCanUseCheck(AbilityLimits::requiresSpear);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity player, IAbility ability) {
        this.animationComponent.start(player, CartAnimations.VAULT);
        this.wasAirborne = false;
        this.didSecondSlam = false;
        player.fallDistance = 0.0F;
        this.doGroundPound(player);
        Vector3d forward = player.getLookAngle().scale(0.6);
        AbilityHelper.setDeltaMovement(player, forward.x, 1.15, forward.z);
        this.continuousComponent.triggerContinuity(player, 60.0F);
    }

    private void onTickContinuityEvent(LivingEntity player, IAbility ability) {
        player.fallDistance = 0.0F;
        if (!this.wasAirborne && !player.isOnGround()) {
            this.wasAirborne = true;
        }

        if (this.wasAirborne && !this.didSecondSlam && player.isOnGround()) {
            this.didSecondSlam = true;
            player.fallDistance = 0.0F;
            if (this.continuousComponent.getContinueTime() > 10.0F) {
                this.doGroundPound(player);
                this.continuousComponent.stopContinuity(player);
            }
        }

    }

    private void onEndContinuityEvent(LivingEntity player, IAbility ability) {
        player.fallDistance = 0.0F;
        this.animationComponent.stop(player);
        this.cooldownComponent.startCooldown(player, 280.0F);
    }

    private void doGroundPound(LivingEntity player) {
        player.fallDistance = 0.0F;
        PARTICLES.spawn(player.level, player.getX(), player.getY(), player.getZ(), (double)0.0F, (double)0.0F, (double)0.0F);
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, 9.0F);
        targets.remove(player);
        targets.removeIf((entity) -> !entity.isOnGround() && DevilFruitHelper.getDifferenceToFloor(player) > (double)3.5F);

        for(LivingEntity target : targets) {
            boolean flag = this.dealDamageComponent.hurtTarget(player, target, 20.0F);
            if (flag) {
                AbilityHelper.setDeltaMovement(target, (double)0.0F, (double)0.75F, (double)0.0F);
                target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 40, 0, false, false));
            }
        }

    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SPEAR) && questProps.hasFinishedQuest(CartQuests.SPEAR_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Vault", AbilityCategory.STYLE, VaultRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(280.0F), DealDamageComponent.getTooltip(20.0F), RangeComponent.getTooltip(9.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.SLASH, SourceType.INDIRECT}).setSourceElement(SourceElement.SHOCKWAVE).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(VaultRework::canUnlock).build();
    }
}
