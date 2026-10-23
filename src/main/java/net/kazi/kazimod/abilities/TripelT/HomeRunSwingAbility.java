package net.kazi.kazimod.abilities.TripelT;

import java.awt.Color;
import javax.annotation.Nullable;
import net.kazi.kazimod.abilities.GoroRework.ElThorRework;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HomeRunSwingAbility extends Ability {

    public enum Mode {
        HOME_RUN_SWING,
        DIVINE_RESONANCE
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "home_run_swing",
            new Pair[]{
                    ImmutablePair.of("Dash forward with a crushing baseball swing that sends enemies flying.", null),
                    ImmutablePair.of("Triple T God transforms this into Divine Resonance. Aim during charge-up to direct the heavenly strike.", null)
            });

    private static final IDescriptionLine BASE_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Home Run Swing")));
    private static final IDescriptionLine ALT_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Divine Resonance")));
    private static final ResourceLocation ALT_ICON = new ResourceLocation("kazimod", "textures/abilities/divine_resonance.png");
    private static final float BASE_CHARGE_TIME = 20.0F;
    private static final float ALT_CHARGE_TIME = 40.0F;
    private static final float BASE_DASH_TIME = 10.0F;
    private static final float BASE_DAMAGE = 34.0F;
    private static final float ALT_DAMAGE = 80.0F;
    private static final float BASE_RANGE = 2.8F;
    private static final float BASE_COOLDOWN = 260.0F;
    private static final float ALT_COOLDOWN = 600.0F;
    private static final float ALT_BEAM_SIZE = 2.5F;
    private static final float ALT_AOE_RADIUS = (ALT_BEAM_SIZE * 0.5F) + 0.1F;
    private static final double ALT_BOX_SIZE_DIVISION = 0.225D;

    public static final AbilityCore<HomeRunSwingAbility> INSTANCE;

    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.HOME_RUN_SWING, true).addChangeModeEvent(this::onAltModeChanged);
    private final ChargeComponent chargeComponent =
            new ChargeComponent(this).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true).addStartEvent(this::onDashStart).addTickEvent(this::onDashTick).addEndEvent(this::onDashEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    private Mode currentMode = Mode.HOME_RUN_SWING;
    private net.kazi.kazimod.preserved.sahur.SahurLightEntity chargeVisual;

    public HomeRunSwingAbility(AbilityCore<HomeRunSwingAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.animationComponent,
                this.hitTrackerComponent,
                this.dealDamageComponent,
                this.rangeComponent
        });
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent(this::onUse);
    }

    public void switchToBase(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.HOME_RUN_SWING);
    }

    public void switchToAlt(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.DIVINE_RESONANCE);
    }

    private void onAltModeChanged(LivingEntity entity, IAbility ability, Enum<?> mode) {
        this.currentMode = (Mode) mode;
        this.setDisplayName(new StringTextComponent(this.currentMode == Mode.DIVINE_RESONANCE ? "Divine Resonance" : "Home Run Swing"));
        if (this.currentMode == Mode.DIVINE_RESONANCE) {
            this.setDisplayIcon(ALT_ICON);
        } else {
            this.setDisplayIcon(this.getCore());
        }
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            TripelTHelper.playTungSound(entity, 2.35F, this.currentMode == Mode.DIVINE_RESONANCE ? 1.1F : 0.95F);
            this.chargeComponent.startCharging(entity, this.getChargeTime());
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        clearChargeVisual();
        if (this.currentMode == Mode.DIVINE_RESONANCE) {
            this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
            if (!entity.level.isClientSide) {
                Vector3d aim = getGroundAim(entity);
                if (aim != null) this.chargeVisual = net.kazi.kazimod.preserved.sahur.SahurLightEntity.spawn(entity, 0, aim, 40);
            }
        } else {
            this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.DIVINE_RESONANCE) {
            if (!entity.level.isClientSide) {
                Vector3d aim = getGroundAim(entity);
                if (aim == null) {
                    clearChargeVisual();
                } else if (this.chargeVisual == null || !this.chargeVisual.isAlive()) {
                    this.chargeVisual = net.kazi.kazimod.preserved.sahur.SahurLightEntity.spawn(entity, 0, aim, 40);
                } else {
                    this.chargeVisual.setPos(aim.x, aim.y, aim.z);
                }
            }
        } else {
            AbilityHelper.slowEntityFall(entity);
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        clearChargeVisual();
        if (this.currentMode == Mode.DIVINE_RESONANCE) {
            fireDivineResonance(entity);
        } else {
            this.continuousComponent.startContinuity(entity, BASE_DASH_TIME);
        }
    }

    private void onDashStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
    }

    private void onDashTick(LivingEntity entity, IAbility ability) {
        Vector3d speed = entity.getLookAngle().normalize().scale(3.6D);
        AbilityHelper.setDeltaMovement(entity, speed.x, entity.isOnGround() ? 0.15D : speed.y, speed.z);
        entity.fallDistance = 0.0F;

        if (!entity.level.isClientSide) {
            ServerWorld world = (ServerWorld) entity.level;
            for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, BASE_RANGE)) {
                if (!this.hitTrackerComponent.canHit(target)) {
                    continue;
                }
                if (this.dealDamageComponent.hurtTarget(entity, target, BASE_DAMAGE)) {
                    Vector3d knockback = target.position().subtract(entity.position()).normalize().scale(2.8D);
                    target.push(knockback.x, 0.7D, knockback.z);
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, world, target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
                }
            }
        }
    }

    private void onDashEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, BASE_COOLDOWN);
    }

    private void fireDivineResonance(LivingEntity entity) {
        if (entity.level.isClientSide) {
            this.cooldownComponent.startCooldown(entity, ALT_COOLDOWN);
            return;
        }

        Vector3d pos = getGroundAim(entity);
        if (pos == null) {
            this.cooldownComponent.startCooldown(entity, ALT_COOLDOWN);
            return;
        }
        net.kazi.kazimod.preserved.sahur.SahurLightEntity.spawn(entity, 1, pos, 106);
        entity.level.playSound((PlayerEntity) null, new BlockPos(pos), ModSounds.EL_THOR_SFX.get(), SoundCategory.PLAYERS, 20.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, ALT_COOLDOWN);
    }

    private void clearChargeVisual() {
        if (this.chargeVisual != null) this.chargeVisual.remove();
        this.chargeVisual = null;
    }

    @Nullable
    private Vector3d getGroundAim(LivingEntity entity) {
        Vector3d eye = entity.getEyePosition(1.0F);
        BlockRayTraceResult hit = entity.level.clip(new RayTraceContext(eye,
                eye.add(entity.getLookAngle().scale(64.0D)),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        if (hit.getType() != RayTraceResult.Type.BLOCK || hit.getDirection() != net.minecraft.util.Direction.UP) return null;
        return hit.getLocation().add(0, 0.01D, 0);
    }

    public LightningEntity fireDivineGameplay(LivingEntity entity, Vector3d pos) {
        // Keep the original server-side strike/destruction; its old renderer is replaced by the light mesh.
        BlockRayTraceResult hitResult = entity.level.clip(new RayTraceContext(pos, pos.add(0, 128, 0),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.ANY, entity));
        float targetY = hitResult.getType() == RayTraceResult.Type.BLOCK ? (float) hitResult.getLocation().y : 128.0F;
        float travelLength = targetY + 16.0F;
        Vector3d strikeBase = new Vector3d(pos.x, targetY, pos.z);
        LightningEntity bolt = new LightningEntity(entity, strikeBase.x, strikeBase.y, strikeBase.z, 0, 90, travelLength, 24, this.getCore());
        bolt.setBlocksAffectedLimit(8150); bolt.setAngle(160); bolt.setBranches(1); bolt.setSegments(1);
        bolt.setSize(ALT_BEAM_SIZE); bolt.setBoxSizeDivision(ALT_BOX_SIZE_DIVISION); bolt.setLightningMovement(false);
        bolt.setExplosion(10, true, .25F); bolt.setColor(ElThorRework.YELLOW_THUNDER);
        bolt.setMaxLife(100); bolt.setDamage(ALT_DAMAGE); bolt.setTargetTimeToReset(9999); bolt.setInvisible(true);
        entity.level.addFreshEntity(bolt);
        applyDivineResonanceColumnDamage(entity, strikeBase, travelLength);
        return bolt;
    }

    private void applyDivineResonanceColumnDamage(LivingEntity entity, Vector3d strikeBase, float travelLength) {
        AxisAlignedBB hitbox = new AxisAlignedBB(
                strikeBase.x - ALT_AOE_RADIUS, strikeBase.y, strikeBase.z - ALT_AOE_RADIUS,
                strikeBase.x + ALT_AOE_RADIUS, strikeBase.y + travelLength, strikeBase.z + ALT_AOE_RADIUS
        );
        for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, hitbox, target -> target != entity && target.isAlive())) {
            Vector3d targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double horizontalDistanceSqr = targetCenter.subtract(strikeBase).multiply(1.0D, 0.0D, 1.0D).lengthSqr();
            if (horizontalDistanceSqr <= ALT_AOE_RADIUS * ALT_AOE_RADIUS) {
                this.dealDamageComponent.hurtTarget(entity, target, ALT_DAMAGE);
            }
        }
    }

    private float getChargeTime() {
        return this.currentMode == Mode.DIVINE_RESONANCE ? ALT_CHARGE_TIME : BASE_CHARGE_TIME;
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Home Run Swing", AbilityCategory.DEVIL_FRUITS, HomeRunSwingAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        BASE_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(BASE_CHARGE_TIME),
                        CooldownComponent.getTooltip(BASE_COOLDOWN),
                        DealDamageComponent.getTooltip(BASE_DAMAGE),
                        RangeComponent.getTooltip(BASE_RANGE, RangeType.AOE)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ALT_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(ALT_CHARGE_TIME),
                        CooldownComponent.getTooltip(ALT_COOLDOWN),
                        DealDamageComponent.getTooltip(ALT_DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.BLUNT})
                .build();
    }
}
