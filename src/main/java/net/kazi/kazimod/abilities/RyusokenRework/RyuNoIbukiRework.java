//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.RyusokenRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.MrMagicalCart.cartaddon.particles.effects.ryusoken.GroundCrackParticleEffect;
import net.MrMagicalCart.cartaddon.particles.effects.ryusoken.IbukiGroundRangeEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.DropHitAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
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
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class RyuNoIbukiRework extends DropHitAbility {
    public static final GroundCrackParticleEffect CRACK_PARTICLES = new GroundCrackParticleEffect();
    public static final IbukiGroundRangeEffect RANGE_PARTICLES = new IbukiGroundRangeEffect();
    private static final int COOLDOWN = 240;
    private static final float RANGE = 6.0F;
    private static final float DAMAGE = 40.0F;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "ryu_no_ibuki", new Pair[]{
            ImmutablePair.of("The user slams into the ground, charging up a shockwave that dazes nearby enemies.", (Object) null)
    });
    public static final AbilityCore<RyuNoIbukiRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final PoolComponent poolComponent;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addTickEvent(this::tickChargeEvent)
            .addEndEvent(this::endChargeEvent);

    public RyuNoIbukiRework(AbilityCore<RyuNoIbukiRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.RYUSOKEN, new AbilityPool2[]{CartAbilityPools.INIT_JUMP});
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.chargeComponent, this.dealDamageComponent, this.rangeComponent});
        this.continuousComponent.addStartEvent(100, this::startContinuityEvent).addEndEvent(100, this::endContinuityEvent);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
    }

    // Copied straight from DancingDragonSlam - lands and starts the Ibuki charge
    public void onLanding(LivingEntity entity) {
        if (!this.chargeComponent.isCharging()) {
            // Impact particles on landing, same as DancingDragonSlam
            if (!entity.level.isClientSide) {
                CRACK_PARTICLES.spawn(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 60.0F, -90.0F, 0.0F);
            }
            // Start the 20 tick (1 second) Ibuki-style charge
            this.chargeComponent.startCharging(entity, 20.0F);
        }
    }

    // Copied straight from DancingDragonSlam startContinuityEvent - slams down, no jump
    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        Vector3d speed = WyHelper.propulsion(entity, (double) 0.0F, (double) 0.0F);
        AbilityHelper.setDeltaMovement(entity, speed.x, -5.0, speed.z);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.25F);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    // Copied from Ibuki tickChargeEvent - freeze + dizzy nearby targets each tick
    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 0, false, false));

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
        targets.remove(entity);
        targets.forEach((target) -> {
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
        });
    }

    // Copied from Ibuki endChargeEvent - deal damage, dizzy, knockback
    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
        targets.remove(entity);

        AbilityDamageSource source = (AbilityDamageSource) ModDamageSource.causeAbilityDamage(entity, this.getCore()).setFistDamage().bypassLogia().setPiercing(0.25F);

        targets.forEach((target) -> {
            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 40, 0, false, false));
                AbilityHelper.setDeltaMovement(target, entity.position().subtract(target.position()).normalize().scale((double) 1.5F));
            }
        });

        if (!entity.level.isClientSide) {
            RANGE_PARTICLES.spawn(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 150.0F, -90.0F, 0.0F);
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.GURA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.6F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity) entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.RYUSOKEN) && questProps.hasFinishedQuest(CartQuests.RYUSOKEN_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Ryu No Ibuki", AbilityCategory.STYLE, RyuNoIbukiRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(DAMAGE),
                        CooldownComponent.getTooltip(240.0F),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceType(new SourceType[]{SourceType.FIST, SourceType.INDIRECT})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setUnlockCheck(RyuNoIbukiRework::canUnlock)
                .build();
    }
}