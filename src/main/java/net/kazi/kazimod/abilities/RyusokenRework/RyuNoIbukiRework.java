//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.RyusokenRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.entities.mobs.quests.givers.RyusokenTrainerEntity;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.MrMagicalCart.cartaddon.particles.effects.ryusoken.GroundCrackParticleEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
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

public class DancingDragonSlamRework extends DropHitAbility {
    public static final GroundCrackParticleEffect PARTICLES = new GroundCrackParticleEffect();
    private static final int COOLDOWN = 240;
    private static final float RANGE = 6.0F;
    private static final float DAMAGE = 40.0F;
    // Charge duration in ticks (20 ticks = 1 second)
    private static final int CHARGE_TICKS = 20;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "dancing_dragon_slam", new Pair[]{ImmutablePair.of("The user charges up and slams into the ground.", (Object)null)});
    public static final AbilityCore<DancingDragonSlamRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final PoolComponent poolComponent;

    // Tracks charge progress per entity (server-side)
    private int chargeTicks = 0;
    private boolean isCharging = false;
    private boolean hasFiredDamage = false;

    public DancingDragonSlamRework(AbilityCore<DancingDragonSlamRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.RYUSOKEN, new AbilityPool2[]{CartAbilityPools.INIT_JUMP});
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.dealDamageComponent, this.rangeComponent});
        this.continuousComponent.addStartEvent(100, this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(100, this::endContinuityEvent);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
    }

    /**
     * Called when the entity lands on the ground.
     * Instead of dealing damage immediately, it starts the charge phase.
     */
    @Override
    public void onLanding(LivingEntity entity) {
        if (!isCharging && !hasFiredDamage) {
            isCharging = true;
            chargeTicks = 0;

            // Play a wind-up sound to signal the charge
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.75F);

            // Freeze the entity in place during the charge
            AbilityHelper.setDeltaMovement(entity, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Deals the actual AOE slam damage and triggers effects.
     */
    private void performSlam(LivingEntity entity) {
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
        targets.remove(entity);
        AbilityDamageSource source = (AbilityDamageSource) ModDamageSource.causeAbilityDamage(entity, this.getCore()).setFistDamage();

        for (LivingEntity target : targets) {
            if (this.hitTrackerComponent.canHit(target) && entity.canSee(target) && this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 15, 0));
                AbilityHelper.disableAbilities(target, 100, (abl) -> abl.hasComponent(ModAbilityKeys.POOL)
                        && ((PoolComponent) abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE));
            }
        }

        if (!entity.level.isClientSide) {
            if (targets.size() > 0) {
                ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            PARTICLES.spawn(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), (double) 60.0F, -90.0F, 0.0F);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        // Reset charge state when the ability starts
        isCharging = false;
        hasFiredDamage = false;
        chargeTicks = 0;

        // Slam the entity straight down immediately (no jump)
        Vector3d speed = WyHelper.propulsion(entity, 0.0, 0.0);
        AbilityHelper.setDeltaMovement(entity, speed.x, -5.0, speed.z);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.25F);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        // If RyusokenTrainer, keep regenerating as before
        if (entity instanceof RyusokenTrainerEntity) {
            entity.addEffect(new EffectInstance(Effects.REGENERATION, 200, 2));
        }

        // Handle charge countdown after landing
        if (isCharging && !hasFiredDamage) {
            chargeTicks++;

            // Keep the entity locked in place during charge
            AbilityHelper.setDeltaMovement(entity, 0.0, 0.0, 0.0);

            // After 1 second (20 ticks), release the slam
            if (chargeTicks >= CHARGE_TICKS) {
                hasFiredDamage = true;
                isCharging = false;
                performSlam(entity);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        // Reset charge state on ability end in case it ended early
        isCharging = false;
        hasFiredDamage = false;
        chargeTicks = 0;

        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity) entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.RYUSOKEN) && questProps.hasFinishedQuest(CartQuests.RYUSOKEN_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Dancing Dragon Slam", AbilityCategory.STYLE, DancingDragonSlamRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(40.0F),
                        CooldownComponent.getTooltip(240.0F),
                        RangeComponent.getTooltip(6.0F, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setUnlockCheck(DancingDragonSlamRework::canUnlock)
                .build();
    }
}