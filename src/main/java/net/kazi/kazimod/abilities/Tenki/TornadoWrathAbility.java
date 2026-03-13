package net.kazi.kazimod.abilities.Tenki;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedElThorAbility;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class TornadoWrathAbility extends Ability {

    // ── Mode enum ─────────────────────────────────────────────────────────────
    public enum TornadoMode { NORMAL, GALE_WRATH, THUNDEROUS_WRATH }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "tornado_wrath",
            new Pair[]{
                    ImmutablePair.of("The user wraps themselves in a raging tornado and charges forward, dealing damage to all enemies caught in the vortex.", (Object) null)
            }
    );

    // ── Display names & icons ─────────────────────────────────────────────────
    private static final TranslationTextComponent TORNADO_WRATH_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.tornado_wrath", "Tornado Wrath"));
    private static final TranslationTextComponent GALE_WRATH_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gale_wrath", "Gale Wrath"));
    private static final TranslationTextComponent THUNDEROUS_WRATH_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.thunderous_wrath", "Thunderous Wrath"));

    private static final ResourceLocation TORNADO_WRATH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/tornado_wrath.png");
    private static final ResourceLocation GALE_WRATH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/gale_wrath.png");
    private static final ResourceLocation THUNDEROUS_WRATH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/thunderous_wrath.png");

    // ── Normal mode stats ─────────────────────────────────────────────────────
    private static final float  CHARGE_TIME     = 15.0F;
    private static final float  DASH_TIME       = 60.0F;
    private static final float  DAMAGE          = 15.0F;
    private static final float  TORNADO_SIZE    = 10.0F;
    private static final int    COOLDOWN        = 250;

    private static final float  AOE_HALF_WIDTH  = TORNADO_SIZE / 2.0F;
    private static final double AOE_HEIGHT      = 30.0;
    private static final double PULL_HALF_WIDTH = 13.0;
    private static final double PULL_HEIGHT     = 50.0;
    private static final double PULL_STRENGTH   = 0.25;

    // ── Gale Wrath stats ──────────────────────────────────────────────────────
    private static final float  GALE_DASH_TIME     = 120.0F;
    private static final double GALE_PULL_STRENGTH = 1.2;
    private static final float  GALE_DASH_SPEED    = 1.35F;
    private static final int    GALE_COOLDOWN      = 700;

    // ── Thunderous Wrath stats ────────────────────────────────────────────────
    private static final float  THUNDER_DASH_TIME  = 70.0F;
    private static final float  THUNDER_DASH_SPEED = 1.35F;
    private static final int    THUNDER_COOLDOWN   = 700;
    private static final float  BOLT_DAMAGE        = 10.0F;
    private static final int    BOLT_INNER_LIFE    = 12;
    private static final int    BOLT_OUTER_LIFE    = 18;

    // ── Hit rate: damage allowed every 20 ticks (was every tick) ─────────────
    private static final int DAMAGE_INTERVAL = 20;
    private final Interval damageInterval = new Interval(DAMAGE_INTERVAL);

    public static final AbilityCore<TornadoWrathAbility> INSTANCE;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;

    // ── Components ────────────────────────────────────────────────────────────
    private final AltModeComponent<TornadoMode> altModeComponent;
    private final AnimationComponent   animationComponent   = new AnimationComponent(this);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private final RangeComponent       rangeComponent       = new RangeComponent(this);
    private final DealDamageComponent  dealDamageComponent  = new DealDamageComponent(this);
    private final HitTrackerComponent  hitTrackerComponent  = new HitTrackerComponent(this);

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::tickChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private WhiteTornadoEntity ridingTornado = null;
    private TornadoMode currentMode = TornadoMode.NORMAL;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TornadoWrathAbility(AbilityCore<TornadoWrathAbility> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, TornadoMode.class, TornadoMode.NORMAL, true))
                .addChangeModeEvent(this::onModeChange);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.animationComponent,
                this.changeStatsComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.rangeComponent,
                this.dealDamageComponent,
                this.hitTrackerComponent
        });

        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.addCanUseCheck(AbilityHelper::requiresDryUser);
        this.addUseEvent(this::useEvent);
    }

    // ── Mode change ───────────────────────────────────────────────────────────
    private void onModeChange(LivingEntity entity, IAbility ability, TornadoMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case GALE_WRATH:
                this.setDisplayName(GALE_WRATH_NAME);
                this.setDisplayIcon(GALE_WRATH_ICON);
                break;
            case THUNDEROUS_WRATH:
                this.setDisplayName(THUNDEROUS_WRATH_NAME);
                this.setDisplayIcon(THUNDEROUS_WRATH_ICON);
                break;
            case NORMAL:
            default:
                this.setDisplayName(TORNADO_WRATH_NAME);
                this.setDisplayIcon(TORNADO_WRATH_ICON);
                break;
        }
        applyTornadoColor();
    }

    // ── Public switch methods ─────────────────────────────────────────────────
    public void switchGaleWrath(LivingEntity entity) {
        if (this.currentMode != TornadoMode.THUNDEROUS_WRATH) {
            this.altModeComponent.setMode(entity, TornadoMode.GALE_WRATH);
        }
    }

    public void switchThunderousWrath(LivingEntity entity) {
        this.altModeComponent.setMode(entity, TornadoMode.THUNDEROUS_WRATH);
    }

    public void switchNormal(LivingEntity entity) {
        if (this.currentMode != TornadoMode.THUNDEROUS_WRATH) {
            this.altModeComponent.setMode(entity, TornadoMode.NORMAL);
        }
    }

    public void revertFromThunderous(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GaleStormAbility galeStorm = (GaleStormAbility) props.getEquippedAbility(GaleStormAbility.INSTANCE);
        if (galeStorm != null && galeStorm.isContinuous()) {
            this.altModeComponent.setMode(entity, TornadoMode.GALE_WRATH);
        } else {
            this.altModeComponent.setMode(entity, TornadoMode.NORMAL);
        }
    }

    // ── Use event ─────────────────────────────────────────────────────────────
    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    // ── Charge phase ──────────────────────────────────────────────────────────
    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, CartAnimations.VISIBLE_SPECIAL_FLY);
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new net.minecraft.potion.EffectInstance(
                (net.minecraft.potion.Effect) xyz.pixelatedw.mineminenomi.init.ModEffects.MOVEMENT_BLOCKED.get(),
                2, 0, false, false
        ));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        float dashTime;
        switch (this.currentMode) {
            case GALE_WRATH:       dashTime = GALE_DASH_TIME;    break;
            case THUNDEROUS_WRATH: dashTime = THUNDER_DASH_TIME; break;
            default:               dashTime = DASH_TIME;         break;
        }
        this.continuousComponent.startContinuity(entity, dashTime);
    }

    // ── Dash phase — start ────────────────────────────────────────────────────
    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.damageInterval.restartIntervalToZero();
        this.changeStatsComponent.applyModifiers(entity);

        ridingTornado = new WhiteTornadoEntity(entity.level, entity);
        ridingTornado.setMaxLife(9999);
        ridingTornado.setSize(TORNADO_SIZE);
        ridingTornado.setSpeed(0.0F);
        ridingTornado.setPos(entity.getX(), entity.getY(), entity.getZ());

        applyTornadoColor();
        entity.level.addFreshEntity(ridingTornado);
    }

    // ── Dash phase — tick ─────────────────────────────────────────────────────
    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.isAlive()) return;
        switch (this.currentMode) {
            case GALE_WRATH:       duringGaleWrath(entity);       break;
            case THUNDEROUS_WRATH: duringThunderousWrath(entity); break;
            default:               duringNormal(entity);          break;
        }
    }

    // ── Normal tick ───────────────────────────────────────────────────────────
    private void duringNormal(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            Vector3d look = entity.getLookAngle();
            entity.move(MoverType.SELF, look.multiply(1.1, 0.0, 1.1));
        }

        applyVanish(entity);
        syncTornado(entity);

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        AxisAlignedBB damageBox = makeDamageBox(ex, ey, ez);

        if (this.damageInterval.canTick()) {
            for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, damageBox,
                    e -> e != entity && !e.isAlliedTo(entity))) {
                this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);
            }
        }

        applyNormalPull(entity, ex, ey, ez, damageBox);
    }

    // ── Gale Wrath tick ───────────────────────────────────────────────────────
    private void duringGaleWrath(LivingEntity entity) {
        applyAirMovement(entity, GALE_DASH_SPEED);
        applyVanish(entity);
        syncTornado(entity);

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        AxisAlignedBB damageBox = makeDamageBox(ex, ey, ez);

        if (this.damageInterval.canTick()) {
            for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, damageBox,
                    e -> e != entity && !e.isAlliedTo(entity))) {
                this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);
            }
        }

        applyGalePull(entity, ex, ey, ez, damageBox);
    }

    // ── Thunderous Wrath tick ─────────────────────────────────────────────────
    private void duringThunderousWrath(LivingEntity entity) {
        applyAirMovement(entity, THUNDER_DASH_SPEED);
        applyVanish(entity);
        syncTornado(entity);

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        AxisAlignedBB damageBox = makeDamageBox(ex, ey, ez);

        if (this.damageInterval.canTick()) {
            for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, damageBox,
                    e -> e != entity && !e.isAlliedTo(entity))) {
                if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                    if (!entity.level.isClientSide) {
                        strikeTarget(entity, target);
                    }
                }
            }
        }
    }

    // ── Dash phase — end ─────────────────────────────────────────────────────
    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);

        if (ridingTornado != null && ridingTornado.isAlive()) {
            ridingTornado.remove();
        }
        ridingTornado = null;

        this.hitTrackerComponent.clearHits();
        this.animationComponent.stop(entity);

        int cd;
        switch (this.currentMode) {
            case GALE_WRATH:       cd = GALE_COOLDOWN;    break;
            case THUNDEROUS_WRATH: cd = THUNDER_COOLDOWN; break;
            default:               cd = COOLDOWN;         break;
        }
        this.cooldownComponent.startCooldown(entity, (float) cd);
    }

    // ── Shared movement helpers ───────────────────────────────────────────────
    private void applyAirMovement(LivingEntity entity, float speed) {
        Vector3d look = entity.getLookAngle();
        AbilityHelper.setDeltaMovement(entity, look.x * speed, look.y * speed, look.z * speed);
        entity.fallDistance = 0.0F;
    }

    private void applyVanish(LivingEntity entity) {
        entity.addEffect(new net.minecraft.potion.EffectInstance(
                (net.minecraft.potion.Effect) xyz.pixelatedw.mineminenomi.init.ModEffects.VANISH.get(),
                10, 0, false, false
        ));
    }

    private void syncTornado(LivingEntity entity) {
        if (ridingTornado != null && ridingTornado.isAlive()) {
            ridingTornado.setPos(entity.getX(), entity.getY(), entity.getZ());
            ridingTornado.setVector(null);
            applyTornadoColor();
        }
    }

    private void applyTornadoColor() {
        if (ridingTornado == null) return;
        if (this.currentMode == TornadoMode.THUNDEROUS_WRATH) {
            ridingTornado.setColor(0.0F, 0.0F, 0.0F);
        } else {
            ridingTornado.setColor(1.0F, 1.0F, 1.0F);
        }
    }

    private AxisAlignedBB makeDamageBox(double ex, double ey, double ez) {
        return new AxisAlignedBB(
                ex - AOE_HALF_WIDTH, ey, ez - AOE_HALF_WIDTH,
                ex + AOE_HALF_WIDTH, ey + AOE_HEIGHT, ez + AOE_HALF_WIDTH
        );
    }

    private void applyNormalPull(LivingEntity entity, double ex, double ey, double ez, AxisAlignedBB damageBox) {
        AxisAlignedBB pullBox = new AxisAlignedBB(
                ex - PULL_HALF_WIDTH, ey, ez - PULL_HALF_WIDTH,
                ex + PULL_HALF_WIDTH, ey + PULL_HEIGHT, ez + PULL_HALF_WIDTH
        );
        Vector3d center = new Vector3d(ex, ey + PULL_HEIGHT / 2.0, ez);
        for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, pullBox,
                e -> e != entity && !e.isAlliedTo(entity) && !damageBox.intersects(e.getBoundingBox()))) {
            if (!target.isAlive()) continue;
            Vector3d toCenter = center.subtract(target.position());
            double dist = toCenter.length();
            if (dist < 0.001) continue;
            double falloff = 1.0 - (dist / (PULL_HALF_WIDTH * 2));
            double strength = PULL_STRENGTH * Math.max(falloff, 0.1);
            target.setDeltaMovement(target.getDeltaMovement().add(toCenter.scale(1.0 / dist).scale(strength)));
            target.hurtMarked = true;
        }
    }

    private void applyGalePull(LivingEntity entity, double ex, double ey, double ez, AxisAlignedBB damageBox) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GaleStormAbility galeStorm = (GaleStormAbility) props.getEquippedAbility(GaleStormAbility.INSTANCE);
        if (galeStorm == null || !galeStorm.isContinuous()) return;

        double galeRadius = GaleStormAbility.BARRIER_RADIUS;
        double galeHeight = 60.0;
        AxisAlignedBB galeBox = new AxisAlignedBB(
                ex - galeRadius, ey - galeHeight, ez - galeRadius,
                ex + galeRadius, ey + galeHeight, ez + galeRadius
        );
        Vector3d userPos = entity.position();
        for (LivingEntity target : entity.level.getEntitiesOfClass(LivingEntity.class, galeBox,
                e -> e != entity && !e.isAlliedTo(entity) && !damageBox.intersects(e.getBoundingBox()))) {
            if (!target.isAlive()) continue;
            Vector3d toUser = userPos.subtract(target.position());
            double dist = toUser.length();
            if (dist < 0.001) continue;
            target.setDeltaMovement(toUser.scale(1.0 / dist).scale(GALE_PULL_STRENGTH));
            target.hurtMarked = true;
        }
    }

    // ── Lightning strike on hit ───────────────────────────────────────────────
    private void strikeTarget(LivingEntity caster, LivingEntity target) {
        boolean blue = ClientConfig.INSTANCE.isGoroBlue();
        int worldTop = caster.level.getMaxBuildHeight() - 2;
        double startY = Math.min((double) worldTop, target.getY() + 60.0);
        double jitterX = (target.getRandom().nextDouble() - 0.5) * 1.5;
        double jitterZ = (target.getRandom().nextDouble() - 0.5) * 1.5;
        float travelLength = (float)(startY - (target.getY() + target.getBbHeight() * 0.5)) + 8.0F;

        LightningEntity inner = new LightningEntity(caster,
                target.getX() + jitterX, startY, target.getZ() + jitterZ,
                0.0F, 90.0F, travelLength, 24.0F, this.getCore());
        LightningEntity outer = new LightningEntity(caster,
                target.getX() + jitterX, startY, target.getZ() + jitterZ,
                0.0F, 90.0F, travelLength, 24.0F, this.getCore());

        setBoltProps(inner, 0.34F, travelLength, 0.0F, BOLT_INNER_LIFE, false, Color.WHITE);
        setBoltProps(outer, 0.4F,  travelLength, BOLT_DAMAGE, BOLT_OUTER_LIFE, true,
                blue ? ReworkedElThorAbility.BLUE_THUNDER : ReworkedElThorAbility.YELLOW_THUNDER);
        outer.seed = inner.seed;

        caster.level.addFreshEntity(inner);
        caster.level.addFreshEntity(outer);
        caster.level.playSound(
                (PlayerEntity) null, target.blockPosition(),
                ModSounds.EL_THOR_SFX.get(), SoundCategory.WEATHER,
                1.5F, 1.0F + caster.getRandom().nextFloat() * 0.2F
        );
    }

    private void setBoltProps(LightningEntity bolt, float size, double length, float damage,
                              int timeAlive, boolean explodes, @Nullable Color color) {
        int segments = (int)(length * 0.45F);
        bolt.setBlocksAffectedLimit(30000);
        bolt.setMaxLife(timeAlive);
        bolt.setDamage(damage);
        if (explodes) {
            bolt.setExplosion(3, true, 0.3F);
        } else {
            bolt.setExplosion(0, false);
        }
        bolt.setSize(size);
        bolt.setBoxSizeDivision(0.2);
        bolt.setColor(color);
        bolt.setAngle(160);
        bolt.setTargetTimeToReset(9999);
        bolt.disableExplosionKnockback();
        bolt.setBranches(1);
        bolt.setSegments((int)(segments + WyHelper.randomWithRange(-segments / 4, segments / 4)));
    }

    // ── Static init ───────────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>("Tornado Wrath", AbilityCategory.DEVIL_FRUITS, TornadoWrathAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(DASH_TIME),
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        RangeComponent.getTooltip(TORNADO_SIZE / 2.0F, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();

        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                INSTANCE,
                "Tornado Wrath Step Height Modifier",
                (double)(TORNADO_SIZE / 2.0F),
                Operation.ADDITION
        );
    }
}