//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HumanRework;

import java.util.ArrayList;
import java.util.Arrays;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.AfterImagePacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class KamieRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "kamie", new Pair[]{ImmutablePair.of("Makes the user's body scalable in order to avoid attacks", (Object)null)});
    private static final float HOLD_TIME = 60.0F;
    private static final float MIN_COOLDOWN = 100.0F;
    private static final float MAX_COOLDOWN = 500.0F;
    private static final float PROTECTION_TIME = 10.0F;
    private static final int AFTERIMAGE_INTERVAL = 3;
    public static final AbilityCore<KamieRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(100, this::onStartContinuityEvent).addTickEvent(100, this::duringContinuityEvent).addEndEvent(100, this::onEndContinuityEvent);
    private final PoolComponent poolComponent;
    private final DamageTakenComponent damageTakenComponent;
    private int hitsTaken;
    private float protTimer;
    private int afterImageTick;

    public KamieRework(AbilityCore<KamieRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.DODGE_ABILITY, new AbilityPool2[0]);
        this.damageTakenComponent = (new DamageTakenComponent(this)).addOnAttackEvent(this::onDamageTakenEvent);
        this.hitsTaken = 0;
        this.protTimer = 10.0F;
        this.afterImageTick = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.poolComponent, this.damageTakenComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 60.0F);
    }

    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitsTaken = 0;
        this.protTimer = 0.0F;
        this.afterImageTick = 0;
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.protTimer > 0.0F) {
            --this.protTimer;
        }

        if (!entity.level.isClientSide && entity instanceof PlayerEntity) {
            if (++this.afterImageTick >= AFTERIMAGE_INTERVAL) {
                this.afterImageTick = 0;
                Vector3d motion = entity.getDeltaMovement();
                double speedSq = motion.x * motion.x + motion.z * motion.z;
                if (speedSq > 0.01D) {
                    PlayerEntity player = (PlayerEntity)entity;
                    AfterImagePacket packet = new AfterImagePacket(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            entity.yRot,
                            entity.xRot,
                            player.getGameProfile().getName(),
                            AfterImagePacket.DEFAULT_LIFE
                    );
                    KaziPacketHandler.sendAfterImage(entity, packet);
                }
            }
        }

    }

    private void onEndContinuityEvent(LivingEntity entity, IAbility ability) {
        float cooldown = 100.0F + this.continuousComponent.getContinueTime() * 5.0F;
        cooldown = Math.min(500.0F, cooldown);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    public float onDamageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (super.isContinuous() && AbilityHelper.canUseMomentumAbilities(entity) && !AbilityHelper.isGrabbing(entity)) {
            boolean isDamageTaken = true;
            boolean isUnavoidable = damageSource instanceof ModDamageSource && ((ModDamageSource)damageSource).isUnavoidable();
            ArrayList<String> acceptableInstantSources = new ArrayList(Arrays.asList("mob", "player", "ability_projectile", "ability"));
            if ((damageSource.getDirectEntity() instanceof LivingEntity || damageSource.getDirectEntity() instanceof ProjectileEntity) && acceptableInstantSources.contains(damageSource.getMsgId()) && !isUnavoidable) {
                isDamageTaken = false;
            }

            if (this.protTimer <= 0.0F) {
                if (!isDamageTaken) {
                    SoundEvent sfx = (SoundEvent)ModSounds.DODGE_1.get();
                    if (entity.getRandom().nextBoolean()) {
                        sfx = (SoundEvent)ModSounds.DODGE_2.get();
                    }

                    entity.level.playSound((PlayerEntity)null, entity.blockPosition(), sfx, SoundCategory.PLAYERS, 1.0F, 0.75F + entity.getRandom().nextFloat() / 2.0F);
                    ++this.hitsTaken;
                    this.protTimer = 10.0F;
                }

                return isDamageTaken ? damage : 0.0F;
            } else {
                return 0.0F;
            }
        } else {
            return damage;
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        boolean raceCheck = props.isHuman() || DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.HITO_HITO_NO_MI);
        return raceCheck && props.getDoriki() >= (double)3600.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kami-E", AbilityCategory.RACIAL, KamieRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(100.0F, 500.0F), ContinuousComponent.getTooltip(60.0F)}).setUnlockCheck(KamieRework::canUnlock).build();
    }
}
