package net.kazi.kazimod.abilities.BlacklegRework;

import java.util.List;

import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartDiableJambeAbility;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BienCultGrillShotRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "bien_cuit_grill_shot", new Pair[]{ImmutablePair.of("A strong kick that launches the user forwards.", null), ImmutablePair.of("A strong kick that launches the user forwards and creates a grill-patterned particle to appear, which sets anyone touching it on fire.", null), ImmutablePair.of("A stronger kick that launches the user forwards and creates a blue grill-patterned particle to appear, which sets anyone touching it on fire.", null)});
    private static final TranslationTextComponent MOUTON_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.mouton_shot", "Mouton Shot"));
    private static final TranslationTextComponent DIABLE_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.bien_cuit_grill_shot", "Bien Cuit: Grill Shot"));
    private static final TranslationTextComponent IFRIT_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.ifrit_bien_cuit_grill_shot", "Ifrit Bien Cuit: Grill Shot"));
    private static final ResourceLocation MOUTON_ICON = new ResourceLocation("cartaddon", "textures/abilities/mouton_shot.png");
    private static final ResourceLocation DIABLE_ICON = new ResourceLocation("mineminenomi", "textures/abilities/bien_cuit_grill_shot.png");
    private static final ResourceLocation IFRIT_ICON = new ResourceLocation("cartaddon", "textures/abilities/ifrit_bien_cuit_grill_shot.png");
    private static final float COOLDOWN = 300.0F;
    private static final float RANGE = 2.0F;
    private static final float DAMAGE = 30.0F;
    public static final AbilityCore<BienCultGrillShotRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent;
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(100, this::startContinuityEvent).addTickEvent(this::continuousTickEvent).addEndEvent(this::continuousEndsEvent);
    private final AltModeComponent<Mode> altModeComponent;
    private boolean ifrite;
    private boolean diable;

    public BienCultGrillShotRework(AbilityCore<BienCultGrillShotRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, Mode.class, BienCultGrillShotRework.Mode.DIABLE, true)).addChangeModeEvent(this::onAltModeChange);
        this.ifrite = false;
        this.diable = false;
        this.animationComponent = new AnimationComponent(this);
        this.isNew = true;
        this.addComponents(this.altModeComponent, this.dealDamageComponent, this.rangeComponent, this.hitTrackerComponent, this.continuousComponent);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent(this::onEquip);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.continuousComponent.startContinuity(entity, 10.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        float horizontalPropulsion = 3.75F;
        CartDiableJambeAbility diableJambeAbility = AbilityDataCapability.get(entity).getEquippedAbility(CartDiableJambeAbility.INSTANCE);

        if (diableJambeAbility.isContinuous() && diableJambeAbility.isIfrit()) {
            this.setIfrite(entity);
            WyHelper.spawnParticleEffect(CartParticleEffects.IFRIT_BIEN_CUIT_GRILL_SHOT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            horizontalPropulsion = 4.75F;
            this.ifrite = true;
        } else if (diableJambeAbility.isContinuous() && !diableJambeAbility.isIfrit()) {
            this.setDiable(entity);
            WyHelper.spawnParticleEffect(ModParticleEffects.BIEN_CUIT_GRILL_SHOT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            this.diable = true;
        } else {
            this.setMouton(entity);
        }

        Vector3d speed = entity.getLookAngle().multiply(horizontalPropulsion, 0.0F, horizontalPropulsion);
        AbilityHelper.setDeltaMovement(entity, speed.x, 0.3, speed.z);
    }

    private void continuousTickEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 2.0F);
            targets.remove(entity);
            Vector3d pushSpeed = entity.getLookAngle().multiply(2.0F, 0.0F, 2.0F);

            for(LivingEntity target : targets) {
                float dmg = 25.0F;
                if (this.ifrite) {
                    dmg = 65.0F;
                }

                if (this.diable) {
                    dmg = 45.0F;
                }

                if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, dmg)) {
                    AbilityHelper.setDeltaMovement(target, pushSpeed.x, 0.2, pushSpeed.z);
                    if (this.diable || this.ifrite) {
                        AbilityHelper.setSecondsOnFireBy(target, 2, entity);
                    }
                }
            }
        }

    }

    private void continuousEndsEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 300.0F);
        this.ifrite = false;
        this.diable = false;

        // Reset to appropriate mode after ability ends
        CartDiableJambeAbility diableJambeAbility = AbilityDataCapability.get(entity).getEquippedAbility(CartDiableJambeAbility.INSTANCE);
        if (diableJambeAbility.isContinuous() && diableJambeAbility.isIfrit()) {
            this.setIfrite(entity);
        } else if (diableJambeAbility.isContinuous()) {
            this.setDiable(entity);
        } else {
            this.setMouton(entity);
        }
    }

    private void onEquip(LivingEntity entity, IAbility ability) {
        this.setMouton(entity);
    }

    public void setMouton(LivingEntity entity) {
        this.altModeComponent.setMode(entity, BienCultGrillShotRework.Mode.MOUTON);
    }

    public void setDiable(LivingEntity entity) {
        this.altModeComponent.setMode(entity, BienCultGrillShotRework.Mode.DIABLE);
    }

    public void setIfrite(LivingEntity entity) {
        this.altModeComponent.setMode(entity, BienCultGrillShotRework.Mode.IFRIT);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == BienCultGrillShotRework.Mode.MOUTON) {
            this.setDisplayIcon(MOUTON_ICON);
            this.setDisplayName(MOUTON_NAME);
        } else if (mode == BienCultGrillShotRework.Mode.DIABLE) {
            this.setDisplayIcon(DIABLE_ICON);
            this.setDisplayName(DIABLE_NAME);
        } else if (mode == BienCultGrillShotRework.Mode.IFRIT) {
            this.setDisplayIcon(IFRIT_ICON);
            this.setDisplayName(IFRIT_NAME);
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBlackLeg() && questProps.hasFinishedQuest(CartQuests.BLACKLEG_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Bien Cuit: Grill Shot", AbilityCategory.STYLE, BienCultGrillShotRework::new)).addAdvancedDescriptionLine((e, a) -> MOUTON_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[0], DealDamageComponent.getTooltip(25.0F), CooldownComponent.getTooltip(300.0F), RangeComponent.getTooltip(2.0F, RangeType.AOE)).addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, (e, a) -> DIABLE_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[1], DealDamageComponent.getTooltip(65.0F), CooldownComponent.getTooltip(300.0F), RangeComponent.getTooltip(2.0F, RangeType.AOE)).addAdvancedDescriptionLine((e, a) -> IFRIT_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[2], DealDamageComponent.getTooltip(45.0F), CooldownComponent.getTooltip(300.0F), RangeComponent.getTooltip(2.0F, RangeType.AOE)).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(SourceType.FIST).setUnlockCheck(BienCultGrillShotRework::canUnlock).build();
    }

    public enum Mode {
        MOUTON,
        DIABLE,
        IFRIT;

        Mode() {
        }
    }
}