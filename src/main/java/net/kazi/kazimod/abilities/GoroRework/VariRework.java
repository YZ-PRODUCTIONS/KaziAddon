package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import java.util.List;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Vari. */
public class VariRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/vari.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/vari.png");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "vari",
            new Pair[]{ImmutablePair.of(
                    "A basic move where the user discharges variable amounts of electricity", null)});
    private static final ITextComponent ONE_MILLION_V_VARI_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.1_million_vari", "1 Million Vari"));
    private static final ITextComponent TWENTY_MILLION_V_VARI_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.20_million_vari", "20 Million Vari"));
    private static final ITextComponent ONE_HUNDRED_MILLION_V_VARI_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.100_million_vari", "100 Million Vari"));
    private static final ITextComponent TWO_HUNDRED_MILLION_V_VARI_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.200_million_vari", "200 Million Vari"));

    public static final AbilityCore<VariRework> INSTANCE =
            new AbilityCore.Builder<VariRework>(
                    "Vari", AbilityCategory.DEVIL_FRUITS, VariRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> ONE_MILLION_V_VARI_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> CooldownComponent.getTooltip(Mode.ONE_MILLION_V.cooldown).expand(e, a),
                            ChargeComponent.getTooltip(20.0F))
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> TWENTY_MILLION_V_VARI_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> CooldownComponent.getTooltip(Mode.TWENTY_MILLION_V.cooldown).expand(e, a),
                            ChargeComponent.getTooltip(20.0F))
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> ONE_HUNDRED_MILLION_V_VARI_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> CooldownComponent.getTooltip(Mode.ONE_HUNDRED_MILLION_V.cooldown).expand(e, a),
                            ChargeComponent.getTooltip(20.0F))
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> TWO_HUNDRED_MILLION_V_VARI_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> CooldownComponent.getTooltip(Mode.TWO_HUNDRED_MILLION_V.cooldown).expand(e, a),
                            ChargeComponent.getTooltip(20.0F))
                    .setSourceHakiNature(SourceHakiNature.SPECIAL)
                    .setSourceElement(SourceElement.LIGHTNING)
                    .setIcon(DEFAULT_ICON)
                    .build();

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this, true)
                    .addStartEvent(100, this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);
    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.ONE_MILLION_V)
                    .addChangeModeEvent(this::onAltModeChange);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval dischargeInterval = new Interval(5);

    public VariRework(AbilityCore<VariRework> core) {
        super(core);
        updateDisplayIcon();
        this.isNew = true;
        this.addComponents(
                chargeComponent, altModeComponent, dealDamageComponent, animationComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent((entity, ability) -> updateDisplayIcon());
    }

    private void updateDisplayIcon() {
        this.setDisplayIcon(ClientConfig.INSTANCE.isGoroBlue() ? ALT_ICON : DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        chargeComponent.startCharging(entity, 20.0F);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        dischargeInterval.restartIntervalToZero();
        animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !dischargeInterval.canTick()) return;

        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(
                entity.position(), entity.yBodyRot, 0.5D, 1.15D, 0.8D);
        float multiplier = getVoltAmaruMultiplier(entity);
        LightningDischargeEntity discharge = new LightningDischargeEntity(
                entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot);
        discharge.setAliveTicks(5);
        discharge.setLightningLength(0.2F * multiplier);
        discharge.setColor(Color.WHITE);
        discharge.setOutlineColor(
                ClientConfig.INSTANCE.isGoroBlue()
                        ? ElThorRework.BLUE_THUNDER : ElThorRework.YELLOW_THUNDER);
        discharge.setRenderTransparent();
        discharge.setDetails(20);
        discharge.setDensity(40);
        discharge.setSize(0.2F * multiplier);
        discharge.setSkipSegments(1);
        entity.level.addFreshEntity(discharge);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        float multiplier = getVoltAmaruMultiplier(entity);
        ModDamageSource source = AbilityDamageSource.causeAbilityDamage(entity, getCore())
                .setPiercing(0.75F)
                .setUnavoidable();
        float radius = 12.0F * chargeComponent.getChargePercentage();
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(
                entity.position(), entity.yBodyRot, 0.5D, 1.15D, 0.8D);

        LightningDischargeEntity discharge = new LightningDischargeEntity(
                entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot);
        discharge.setAliveTicks(15);
        discharge.setLightningLength(radius / 2.0F * multiplier);
        discharge.setColor(Color.WHITE);
        discharge.setOutlineColor(
                ClientConfig.INSTANCE.isGoroBlue()
                        ? ElThorRework.BLUE_THUNDER : ElThorRework.YELLOW_THUNDER);
        discharge.setRenderTransparent();
        discharge.setDetails(20);
        discharge.setDensity(40);
        discharge.setSize(radius / 2.0F * multiplier);
        discharge.setSkipSegments(1);
        entity.level.addFreshEntity(discharge);

        List<LivingEntity> targets = WyHelper.getNearbyEntities(
                pos, entity.level, radius,
                ModEntityPredicates.getEnemyFactions(entity), LivingEntity.class);
        targets.remove(entity);
        Mode currentMode = altModeComponent.getCurrentMode();
        for (LivingEntity target : targets) {
            float damage = (float) (
                    currentMode.damage * chargeComponent.getChargePercentage()
                            * (1.0D - pos.distanceTo(target.position()) / (radius * 3.0F)))
                    * multiplier;
            if (dealDamageComponent.hurtTarget(entity, target, damage, source)) {
                target.addEffect(new EffectInstance(
                        ModEffects.PARALYSIS.get(), 10, 0, false, false, true));
            }
        }

        animationComponent.stop(entity);
        cooldownComponent.startCooldown(entity, currentMode.cooldown);
    }

    private float getVoltAmaruMultiplier(LivingEntity entity) {
        return ((MorphInfo) ModMorphs.VOLT_AMARU.get()).isActive(entity) ? 1.25F : 1.0F;
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        this.setDisplayName(mode.displayName);
    }

    public enum Mode {
        ONE_MILLION_V(ONE_MILLION_V_VARI_NAME, 10.0F, 200.0F),
        TWENTY_MILLION_V(TWENTY_MILLION_V_VARI_NAME, 35.0F, 300.0F),
        ONE_HUNDRED_MILLION_V(ONE_HUNDRED_MILLION_V_VARI_NAME, 50.0F, 500.0F),
        TWO_HUNDRED_MILLION_V(TWO_HUNDRED_MILLION_V_VARI_NAME, 65.0F, 600.0F);

        private final ITextComponent displayName;
        private final float damage;
        private final float cooldown;

        Mode(ITextComponent displayName, float damage, float cooldown) {
            this.displayName = displayName;
            this.damage = damage;
            this.cooldown = cooldown;
        }
    }
}
