package net.kazi.kazimod.abilities.TripelT;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SwingingCounterAbility extends Ability {

    public enum Mode {
        SWINGING_COUNTER,
        JUDGEMENTATION
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "swinging_counter",
            new Pair[]{
                    ImmutablePair.of("Bat away incoming projectiles and return them to their shooter at 1.5x power.", null),
                    ImmutablePair.of("In God form, Judgementation shields you for 5 seconds, returns incoming projectiles to their shooter, and heals 10 HP per hit without ending early.", null)
            });

    private static final IDescriptionLine BASE_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Swinging Counter")));
    private static final IDescriptionLine JUDGEMENT_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Judgementation")));
    private static final ResourceLocation ALT_ICON = new ResourceLocation("kazimod", "textures/abilities/judgementation.png");

    public static final AbilityCore<SwingingCounterAbility> INSTANCE;

    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.SWINGING_COUNTER, true).addChangeModeEvent(this::onAltModeChanged);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true).addTickEvent(this::onTick).addEndEvent(this::onEnd);
    private final DamageTakenComponent damageTakenComponent =
            new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    private Mode currentMode = Mode.SWINGING_COUNTER;

    public SwingingCounterAbility(AbilityCore<SwingingCounterAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.altModeComponent, this.continuousComponent, this.damageTakenComponent, this.dealDamageComponent});
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent((entity, ability) -> {
            TripelTHelper.playTungSound(entity, 2.0F, this.currentMode == Mode.JUDGEMENTATION ? 1.08F : 0.92F);
            this.continuousComponent.startContinuity(entity, this.currentMode == Mode.JUDGEMENTATION ? 100.0F : 26.0F);
            if (this.currentMode == Mode.JUDGEMENTATION) net.kazi.kazimod.preserved.sahur.HeavenlyShield.begin(entity);
        });
    }

    public void switchToBase(LivingEntity entity) {
        if (this.currentMode == Mode.JUDGEMENTATION && this.continuousComponent.isContinuous()) this.continuousComponent.stopContinuity(entity);
        this.altModeComponent.setMode(entity, Mode.SWINGING_COUNTER);
    }

    public void switchToJudgementation(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.JUDGEMENTATION);
    }

    private void onAltModeChanged(LivingEntity entity, IAbility ability, Enum<?> mode) {
        this.currentMode = (Mode) mode;
        this.setDisplayName(new StringTextComponent(this.currentMode == Mode.JUDGEMENTATION ? "Judgementation" : "Swinging Counter"));
        if (this.currentMode == Mode.JUDGEMENTATION) {
            this.setDisplayIcon(ALT_ICON);
        } else {
            this.setDisplayIcon(this.getCore());
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous()) {
            return damage;
        }

        if (this.currentMode == Mode.JUDGEMENTATION) {
            if (!net.kazi.kazimod.preserved.sahur.HeavenlyShield.active(entity) || damage <= 0
                    || source.getEntity() == entity || (source.getEntity() == null && !source.isProjectile())) return damage;
            Entity directHit = source.getDirectEntity();
            if (!(directHit instanceof ProjectileEntity)
                    || !net.kazi.kazimod.preserved.sahur.HeavenlyShield.reflect(entity, (ProjectileEntity) directHit))
                net.kazi.kazimod.preserved.sahur.HeavenlyShield.hit(entity);
            return 0.0F;
        }

        Entity direct = source.getDirectEntity();
        return direct instanceof ProjectileEntity && reflectCounter(entity, (ProjectileEntity) direct) ? 0.0F : damage;
    }

    public static boolean reflectCounter(LivingEntity entity, ProjectileEntity projectile) {
        IAbility equipped = AbilityDataCapability.get(entity).getEquippedAbility(INSTANCE);
        if (!(equipped instanceof SwingingCounterAbility)) return false;
        SwingingCounterAbility counter = (SwingingCounterAbility) equipped;
        if (counter.currentMode != Mode.SWINGING_COUNTER || !counter.continuousComponent.isContinuous()
                || projectile.getOwner() == entity || entity.level.isClientSide) return false;
        if (projectile instanceof AbilityProjectileEntity) {
            AbilityProjectileEntity abilityProjectile = (AbilityProjectileEntity) projectile;
            abilityProjectile.setDamage(abilityProjectile.getDamage() * 1.5F);
        }
        if (!net.kazi.kazimod.preserved.sahur.HeavenlyShield.reflect(entity, projectile, false)) return false;
        ServerWorld world = (ServerWorld) entity.level;
        WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, world, projectile.getX(), projectile.getY(), projectile.getZ());
        world.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.2F, 0.8F);
        counter.continuousComponent.stopContinuity(entity);
        return true;
    }

    private void onTick(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.JUDGEMENTATION) {
            if (!TripelTHelper.isGodForm(entity)) { this.continuousComponent.stopContinuity(entity); return; }
            net.kazi.kazimod.preserved.sahur.HeavenlyShield.tick(entity);
        } else if (!entity.level.isClientSide) {
            AxisAlignedBB guard = entity.getBoundingBox().inflate(0.8);
            for (ProjectileEntity projectile : entity.level.getEntitiesOfClass(ProjectileEntity.class, guard.inflate(8))) {
                if (projectile.getOwner() == entity || !projectile.isAlive()) continue;
                Vector3d motion = projectile.getDeltaMovement();
                if (guard.contains(projectile.position())
                        || (motion.lengthSqr() > 0.0001 && guard.clip(projectile.position(), projectile.position().add(motion)).isPresent())) {
                    if (reflectCounter(entity, projectile)) break;
                }
            }
        }
    }

    private void onEnd(LivingEntity entity, IAbility ability) {
        net.kazi.kazimod.preserved.sahur.HeavenlyShield.end(entity);
        if (!this.cooldownComponent.isOnCooldown()) {
            float cooldown = this.currentMode == Mode.JUDGEMENTATION ? 320.0F : 220.0F;
            this.cooldownComponent.startCooldown(entity, cooldown);
        }
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Swinging Counter", AbilityCategory.DEVIL_FRUITS, SwingingCounterAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        BASE_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(26.0F),
                        CooldownComponent.getTooltip(220.0F)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        JUDGEMENT_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(100.0F),
                        CooldownComponent.getTooltip(320.0F)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.BLUNT})
                .build();
    }
}
