package net.kazi.kazimod.abilities.TripelT;

import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.entities.projectiles.LightArrowProjectile;
import net.kazi.kazimod.entities.projectiles.TripelTBatProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TungTungTungBarrageAbility extends Ability {

    public enum Mode {
        TUNG_TUNG_TUNG_BARRAGE,
        ARROWS_OF_LIGHT
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "tung_tung_tung_barrage",
            new Pair[]{
                    ImmutablePair.of("Unleash a swarm of Triple T bats in front of you.", null),
                    ImmutablePair.of("Triple T God summons five overhead portals that fire heavenly arrows toward your aim.", null)
            });

    private static final IDescriptionLine BASE_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Tung Tung Tung Barrage")));
    private static final IDescriptionLine ALT_NAME = IDescriptionLine.of(AbilityHelper.mentionText(new StringTextComponent("Arrows of Light")));
    private static final ResourceLocation ALT_ICON = new ResourceLocation("kazimod", "textures/abilities/arrows_of_light.png");
    private static final int BASE_WAVES = 26;
    private static final int BASE_INTERVAL = 1;
    private static final int BASE_PROJECTILES_PER_WAVE = 5;
    private static final float BASE_PROJECTILE_DAMAGE = 10.0F;
    private static final float BASE_COOLDOWN = 240.0F;
    private static final float ALT_CHARGE_TIME = 20.0F;
    private static final float ALT_CONTINUOUS_TIME = 60.0F;
    private static final int ALT_VOLLEYS = 18;
    private static final int ALT_INTERVAL = 3;
    private static final float ALT_PROJECTILE_DAMAGE = 12.0F;
    private static final float ALT_COOLDOWN = 240.0F;
    public static final AbilityCore<TungTungTungBarrageAbility> INSTANCE;

    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.TUNG_TUNG_TUNG_BARRAGE, true).addChangeModeEvent(this::onAltModeChanged);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this).addStartEvent(this::onContinuityStart).addEndEvent(this::onContinuityEnd);
    private final ChargeComponent chargeComponent =
            new ChargeComponent(this).addStartEvent(this::onPortalChargeStart).addEndEvent(this::onPortalChargeEnd);
    private final RepeaterComponent repeaterComponent =
            new RepeaterComponent(this).addTriggerEvent(this::onRepeaterTrigger).addStopEvent(this::onRepeaterStop);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    private Mode currentMode = Mode.TUNG_TUNG_TUNG_BARRAGE;
    private List<net.kazi.kazimod.preserved.sahur.SahurLightEntity> portals = new ArrayList<>();

    public TungTungTungBarrageAbility(AbilityCore<TungTungTungBarrageAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.continuousComponent,
                this.chargeComponent,
                this.repeaterComponent,
                this.projectileComponent
        });
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent(this::onUse);
    }

    public void switchToBase(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.TUNG_TUNG_TUNG_BARRAGE);
    }

    public void switchToAlt(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.ARROWS_OF_LIGHT);
    }

    private void onAltModeChanged(LivingEntity entity, IAbility ability, Enum<?> mode) {
        this.currentMode = (Mode) mode;
        this.setDisplayName(new StringTextComponent(this.currentMode == Mode.ARROWS_OF_LIGHT ? "Arrows of Light" : "Tung Tung Tung Barrage"));
        if (this.currentMode == Mode.ARROWS_OF_LIGHT) {
            this.setDisplayIcon(ALT_ICON);
        } else {
            this.setDisplayIcon(this.getCore());
        }
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        TripelTHelper.playTungSound(entity, 2.25F, this.currentMode == Mode.ARROWS_OF_LIGHT ? 1.12F : 0.92F);
        if (this.currentMode == Mode.ARROWS_OF_LIGHT) {
            if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
                this.chargeComponent.startCharging(entity, ALT_CHARGE_TIME);
            }
            return;
        }

        if (this.continuousComponent.isContinuous()) {
            this.repeaterComponent.stop(entity);
        } else {
            this.continuousComponent.triggerContinuity(entity);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.currentMode == Mode.TUNG_TUNG_TUNG_BARRAGE) {
            this.repeaterComponent.start(entity, BASE_WAVES, BASE_INTERVAL);
        }
    }

    private void onPortalChargeStart(LivingEntity entity, IAbility ability) {
        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.close(this.portals);
        this.portals = net.kazi.kazimod.preserved.sahur.HeavenlyArrows.open(entity, (int)(ALT_CHARGE_TIME + ALT_CONTINUOUS_TIME));
    }

    private void onPortalChargeEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, ALT_CONTINUOUS_TIME);
        if (!entity.level.isClientSide) {
            this.repeaterComponent.start(entity, ALT_VOLLEYS, ALT_INTERVAL);
        }
    }

    private void onRepeaterTrigger(LivingEntity entity, IAbility ability) {
        if (this.currentMode == Mode.ARROWS_OF_LIGHT) {
            shootPortalVolley(entity);
            return;
        }

        float speed = 2.15F;
        for (int i = 0; i < BASE_PROJECTILES_PER_WAVE; i++) {
            TripelTBatProjectile projectile = new TripelTBatProjectile(entity.level, entity, BASE_PROJECTILE_DAMAGE);
            Vector3d look = entity.getLookAngle();
            projectile.xRot = (float) look.x;
            projectile.yRot = (float) look.y;
            projectile.setKnockbackStrength(1);
            projectile.setMaxLife(10);
            double px = entity.getX() + WyHelper.randomWithRange(-2, 2) + WyHelper.randomDouble();
            double py = entity.getEyeY() + WyHelper.randomWithRange(0, 2) + WyHelper.randomDouble();
            double pz = entity.getZ() + WyHelper.randomWithRange(-2, 2) + WyHelper.randomDouble();
            projectile.moveTo(px, py, pz, 0.0F, 0.0F);
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 3.0F);
        }
        entity.swing(Hand.MAIN_HAND, true);
    }

    private void shootPortalVolley(LivingEntity entity) {
        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.volley(entity, this.portals, ALT_PROJECTILE_DAMAGE);
    }

    private void onRepeaterStop(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
        this.cooldownComponent.startCooldown(entity, this.currentMode == Mode.ARROWS_OF_LIGHT ? ALT_COOLDOWN : BASE_COOLDOWN);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.close(this.portals);
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        if (this.currentMode == Mode.ARROWS_OF_LIGHT) {
            return new LightArrowProjectile(entity.level, entity, ALT_PROJECTILE_DAMAGE);
        }
        return new TripelTBatProjectile(entity.level, entity, BASE_PROJECTILE_DAMAGE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Tung Tung Tung Barrage", AbilityCategory.DEVIL_FRUITS, TungTungTungBarrageAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        BASE_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(BASE_COOLDOWN)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ALT_NAME,
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(ALT_CHARGE_TIME),
                        CooldownComponent.getTooltip(ALT_COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.BLUNT, SourceType.PROJECTILE})
                .build();
    }
}
