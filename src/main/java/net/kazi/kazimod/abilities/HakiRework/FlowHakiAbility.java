package net.kazi.kazimod.abilities.HakiRework;

import java.awt.Color;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import net.kazi.kazimod.init.KaziModPools;
import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiInfusionAbility;

public class FlowHakiAbility extends PunchAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "flow_haki_infusion", new Pair[]{ImmutablePair.of("Allows the user to infuse their flow haki into whatever weapon they're holding. Cannot be used by those with Conqueror's Haki.", (Object)null)});
    public static final AbilityCore<FlowHakiAbility> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final UUID STRENGTH_UUID;
    private final PoolComponent poolComponent;
    private final SkinOverlayComponent skinOverlayComponent;

    private static final Color FLOW_COLOR = new Color(0, 180, 255, 50);

    public FlowHakiAbility(AbilityCore<FlowHakiAbility> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, KaziModPools.HAKI_INFUSION, new AbilityPool2[0]);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        super.continuousComponent.addStartEvent(100, this::onContinuityStart).addTickEvent(100, this::onContinuityTick).addEndEvent(100, this::onContinuityEnd);
        super.addComponents(new AbilityComponent[]{this.poolComponent, this.skinOverlayComponent});
        super.addCanUseCheck(HakiHelper::canEnableHaki);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        AbilityOverlay overlay = (new AbilityOverlay.Builder()).setOverlayPart(OverlayPart.LIMB).setColor(FLOW_COLOR).build();
        this.skinOverlayComponent.show(entity, overlay);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, 3);
            if (isOnMaxOveruse) {
                super.continuousComponent.stopContinuity(entity);
                return;
            }
            // Stop Flow Haki if Haoshoku Infusion is equipped
            if (AbilityDataCapability.get(entity).hasEquippedAbility(HaoshokuHakiInfusionAbility.INSTANCE)) {
                super.continuousComponent.stopContinuity(entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.hideAll(entity);
    }

    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        source.bypassLogia();
        if (entity.getRandom().nextInt(10) < 1) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.7F);
        }

        LightningDischargeEntity discharge = new LightningDischargeEntity(target, target.getX(), target.getY() + (double)1.5F, target.getZ(), target.yRot, target.xRot);
        discharge.setAliveTicks(15);
        discharge.setLightningLength(6.0F);
        discharge.setColor(new Color(0, 0, 0, 100));
        discharge.setOutlineColor(FLOW_COLOR);
        discharge.setRenderTransparent();
        discharge.setDetails(4);
        discharge.setDensity(4);
        discharge.setSize(1.0F);
        discharge.setSkipSegments(1);
        entity.level.addFreshEntity(discharge);
        return true;
    }

    public boolean isActive() {
        return continuousComponent.isContinuous();
    }

    public Predicate<LivingEntity> canActivate() {
        return (entity) -> {
            if (AbilityDataCapability.get(entity).hasEquippedAbility(HaoshokuHakiInfusionAbility.INSTANCE)) {
                return false;
            }
            return continuousComponent.isContinuous();
        };
    }

    public int getUseLimit() {
        return -1;
    }

    public boolean isParallel() {
        return true;
    }

    public float getPunchCooldown() {
        return 0.0F;
    }

    /**
     * Damage is halved compared to Haoshoku Haki Infusion.
     * Original formula: 10 + (originalAmount / 100) * 70 * (0.1 * doriki + 0.9 * haki)
     * Halved:           5  + (originalAmount / 100) * 35 * (0.1 * doriki + 0.9 * haki)
     */
    public static double getDamageBoost(LivingEntity entity, float originalAmount) {
        IEntityStats props = EntityStatsCapability.get(entity);
        IHakiData hakiProps = HakiDataCapability.get(entity);
        double dorikiMultiplier = props.getDoriki() / (double)CommonConfig.INSTANCE.getDorikiLimit();
        float hakiMultiplier = hakiProps.getBusoshokuHakiExp() / (float)CommonConfig.INSTANCE.getHakiExpLimit();
        return (double)5.0F + (double)originalAmount / (double)100.0F * (double)35.0F * (0.1 * dorikiMultiplier + 0.9 * (double)hakiMultiplier);
    }

    private AbilityAttributeModifier getEntryAttackDamage(double amount) {
        return new AbilityAttributeModifier(STRENGTH_UUID, INSTANCE, "Flow Haki Infusion Attack Damage Modifier", amount, Operation.ADDITION);
    }

    private static boolean canUnlock(LivingEntity user) {
        IHakiData props = HakiDataCapability.get(user);
        // Block unlock if the user has Conqueror's Haki (Haoshoku)
        if (AbilityDataCapability.get(user).hasUnlockedAbility(HaoshokuHakiAbility.INSTANCE)) {
            return false;
        }
        return props.getTotalHakiExp() >= props.getMaxHakiExp() * 0.85F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Flow Haki: Infusion", AbilityCategory.HAKI, FlowHakiAbility::new)).addDescriptionLine(DESCRIPTION).setUnlockCheck(FlowHakiAbility::canUnlock).setSourceType(new SourceType[]{SourceType.FIST}).setSourceHakiNature(SourceHakiNature.UNKNOWN).build();
        OVERLAY = (new AbilityOverlay.Builder()).setOverlayPart(OverlayPart.LIMB).setColor(new Color(0, 180, 255, 50)).build();
        STRENGTH_UUID = UUID.fromString("7a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
    }
}