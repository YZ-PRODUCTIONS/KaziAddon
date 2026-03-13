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

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int   COOLDOWN         = 240;
    private static final float RANGE            = 4.5F;
    private static final float DAMAGE           = 20.0F;
    private static final int   FOXFIRE_COOLDOWN = 240;
    private static final float FOXFIRE_RANGE    = 4.5F;
    private static final float FOXFIRE_DAMAGE   = 45.0F;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hiryu_kaen",
            new Pair[]{ImmutablePair.of(
                    "The user leaps into the air and releases a big flaming shockwave slash when landing. " +
                            "When §6Foxfire Style§r is active, the slash becomes a superheated foxfire dive with greater damage.",
                    (Object) null
            )}
    );

    private static final ITextComponent[] FOXFIRE_DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hiryu_kaen_foxfire",
            new Pair[]{ImmutablePair.of(
                    "§6Foxfire Mode§r (requires §6Foxfire Style§r): superheated dive with greater damage and piercing.",
                    (Object) null
            )}
    );

    public static final AbilityCore<HiryuKaenRework> INSTANCE;

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent           = new RangeComponent(this);

    // No AltModeComponent — foxfire state is controlled exclusively by FoxfireStyleAbility
    private boolean isFoxfireMode = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public HiryuKaenRework(AbilityCore<HiryuKaenRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{
                this.dealDamageComponent,
                this.rangeComponent
        });

        this.continuousComponent.addStartEvent(100, this::onStartContinuityEvent);
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
        this.continuousComponent.addTickEvent(100, this::onContinuityTick);

        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
    }

    // =========================================================
    // FOXFIRE MODE — only FoxfireStyleAbility may call these
    // =========================================================

    public void switchFoxfireMode(LivingEntity entity) {
        this.isFoxfireMode = true;
    }

    public void switchNormalMode(LivingEntity entity) {
        this.isFoxfireMode = false;
    }

    // =========================================================
    // CONTINUITY EVENTS
    // =========================================================

    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.isFoxfireMode) {
            Vector3d speed = WyHelper.propulsion(entity, 1.1, 1.1);
            AbilityHelper.setDeltaMovement(entity, speed.x, 1.75F, speed.z);
        } else {
            Vector3d speed = WyHelper.propulsion(entity, 1.0F, 1.0F);
            AbilityHelper.setDeltaMovement(entity, speed.x, 1.3, speed.z);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!this.isFoxfireMode) return;

        if (this.continuousComponent.getContinueTime() >= 15.0F) {
            Vector3d speed = entity.getLookAngle().multiply(1.25F, 1.0F, 1.25F);
            AbilityHelper.setDeltaMovement(entity, speed.x, -3.0F, speed.z);

            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, FOXFIRE_RANGE);
            targets.remove(entity);
            AbilityDamageSource source = (AbilityDamageSource) ModDamageSource
                    .causeAbilityDamage(entity, this.getCore())
                    .setSlash()
                    .setPiercing(0.25F);

            for (LivingEntity target : targets) {
                if (this.hitTrackerComponent.canHit(target) && entity.canSee(target)
                        && this.dealDamageComponent.hurtTarget(entity, target, FOXFIRE_DAMAGE, source)) {
                    target.setSecondsOnFire(4);
                }
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, this.isFoxfireMode ? FOXFIRE_COOLDOWN : COOLDOWN);
    }

    // =========================================================
    // ON LANDING
    // =========================================================

    @Override
    public void onLanding(LivingEntity entity) {
        if (this.isFoxfireMode) {
            if (!entity.level.isClientSide) {
                ((ServerWorld) entity.level).getChunkSource()
                        .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HIRYU_KAEN.get(),
                        entity, entity.getX(), entity.getY(), entity.getZ()
                );
            }
            // Notify FoxfireStyleAbility that a foxfire ability was used
            notifyFoxfireStyle(entity);
        } else {
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
            targets.remove(entity);
            AbilityDamageSource source = (AbilityDamageSource) ModDamageSource
                    .causeAbilityDamage(entity, this.getCore())
                    .setSlash();

            for (LivingEntity target : targets) {
                if (this.hitTrackerComponent.canHit(target) && entity.canSee(target)
                        && this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                    target.setSecondsOnFire(4);
                }
            }

            if (!entity.level.isClientSide) {
                if (targets.size() > 0) {
                    ((ServerWorld) entity.level).getChunkSource()
                            .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
                }
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HIRYU_KAEN.get(),
                        entity, entity.getX(), entity.getY(), entity.getZ()
                );
            }
        }
    }

    // =========================================================
    // NOTIFY FOXFIRE STYLE
    // =========================================================

    private static void notifyFoxfireStyle(LivingEntity entity) {
        FoxfireStyleAbility foxfire = FoxfireStyleAbility.getEquippedAbility(
                entity, FoxfireStyleAbility.INSTANCE, FoxfireStyleAbility.class);
        if (foxfire != null) {
            foxfire.setAbilityUsed(true);
        }
    }

    // =========================================================
    // UNLOCK CHECK
    // =========================================================

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        IEntityStats props = EntityStatsCapability.get(player);
        IQuestData questProps = QuestDataCapability.get(player);
        return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_05);
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder<>("Hiryu: Kaen", AbilityCategory.STYLE, HiryuKaenRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .addDescriptionLine(FOXFIRE_DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) FOXFIRE_COOLDOWN),
                        DealDamageComponent.getTooltip(FOXFIRE_DAMAGE),
                        RangeComponent.getTooltip(FOXFIRE_RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .setSourceElement(SourceElement.FIRE)
                .setUnlockCheck(HiryuKaenRework::canUnlock)
                .build();
    }
}