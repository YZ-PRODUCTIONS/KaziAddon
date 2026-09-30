package net.kazi.kazimod.abilities.BlacklegRework;

import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartDiableJambeAbility;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.MrMagicalCart.cartaddon.entities.projectiles.blacklegextra.IfritPoeleAFrireProjectile;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.blackleg.ExtraHachisProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.blackleg.PoeleAFrireProjectile;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

/** Kazi-owned clone of Cart Addon's Extra Hachis ability. */
public class ExtraHachisRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "extra_hachis",
            new Pair[]{
                    ImmutablePair.of("Launches a rapid barrage of kicks.", null),
                    ImmutablePair.of("Launches a stronger rapid barrage of kicks that light opponents on fire.", null),
                    ImmutablePair.of("Launches an even stronger rapid barrage of kicks that light opponents on fire for longer.", null)
            });
    private static final TranslationTextComponent EXTRA_HACHIS =
            new TranslationTextComponent("ability.cartaddon.extra_hachis");
    private static final TranslationTextComponent POELE_A_FRIRE_NAME = new TranslationTextComponent(
            WyRegistry.registerName("ability.mineminenomi.poele_a_frire", "Poêle à Frire"));
    private static final TranslationTextComponent IFRIT_POELE_A_FRIRE_NAME = new TranslationTextComponent(
            CartRegistry.registerName("ability.cartaddon.ifrit_poele_a_frire", "Ifrit: Poêle à Frire"));
    private static final ResourceLocation POELE_A_FRIRE_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/poele_a_frire.png");
    private static final ResourceLocation IFRIT_POELE_A_FRIRE_ICON =
            new ResourceLocation("cartaddon", "textures/abilities/ifrit_poele_a_frire.png");
    private static final ResourceLocation EXTRA_HACHIS_ICON =
            new ResourceLocation("cartaddon", "textures/abilities/extra_hachis.png");

    public static final AbilityCore<ExtraHachisRework> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart);
    private final RepeaterComponent repeaterComponent = new RepeaterComponent(this)
            .addTriggerEvent(this::onRepeaterTrigger)
            .addStopEvent(this::onRepeaterStop);
    private final AltModeComponent<Mode> altModeComponent = new AltModeComponent<>(this, Mode.class, Mode.NORMAL, true)
            .addChangeModeEvent(this::onAltModeChange);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public ExtraHachisRework(AbilityCore<ExtraHachisRework> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(EXTRA_HACHIS);
        this.setDisplayIcon(EXTRA_HACHIS_ICON);
        this.addComponents(continuousComponent, repeaterComponent, altModeComponent, animationComponent);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (continuousComponent.isContinuous()) {
            repeaterComponent.stop(entity);
        } else {
            continuousComponent.triggerContinuity(entity);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            syncJambeMode(entity);
            Vector3d momentum = entity.getDeltaMovement();
            entity.setDeltaMovement(momentum.x * 1.5, momentum.y, momentum.z * 1.5);
            animationComponent.start(entity, CartAnimations.HACHIS);
            repeaterComponent.start(entity, 30, 1);
        }
    }

    private void onRepeaterTrigger(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            syncJambeMode(entity);
        }

        float projectileSpace = 1.5F;
        float speed = 2.0F;
        Vector3d pos = new Vector3d(
                entity.getX() + WyHelper.randomWithRange((int) -projectileSpace, (int) projectileSpace) + WyHelper.randomDouble(),
                entity.getY() + 1.5 + WyHelper.randomWithRange(0, (int) projectileSpace) + WyHelper.randomDouble(),
                entity.getZ() + WyHelper.randomWithRange((int) -projectileSpace, (int) projectileSpace) + WyHelper.randomDouble());

        AbilityProjectileEntity extraHachisProjectile = new ExtraHachisProjectile(entity.level, entity, this);
        if (altModeComponent.getCurrentMode() == Mode.POELE_A_FRIRE) {
            speed = 3.0F;
            PoeleAFrireProjectile projectile = new PoeleAFrireProjectile(entity.level, entity, this);
            projectile.setMaxLife(13);
            projectile.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 1.5F);
        }

        if (altModeComponent.getCurrentMode() == Mode.IFRIT_POELE_A_FRIRE) {
            speed = 3.5F;
            IfritPoeleAFrireProjectile projectile = new IfritPoeleAFrireProjectile(entity.level, entity, this);
            projectile.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            entity.level.addFreshEntity(projectile);
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 1.5F);
        }

        extraHachisProjectile.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        extraHachisProjectile.setMaxLife(3);
        entity.level.addFreshEntity(extraHachisProjectile);
        extraHachisProjectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 0.75F);
        ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
    }

    private void onRepeaterStop(LivingEntity entity, IAbility ability) {
        animationComponent.stop(entity);
        continuousComponent.stopContinuity(entity);
        if (altModeComponent.getCurrentMode() == Mode.NORMAL) {
            cooldownComponent.startCooldown(entity, 240.0F);
        } else if (altModeComponent.getCurrentMode() == Mode.POELE_A_FRIRE) {
            cooldownComponent.startCooldown(entity, 300.0F);
        } else {
            cooldownComponent.startCooldown(entity, 340.0F);
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.NORMAL) {
            setDisplayIcon(EXTRA_HACHIS_ICON);
            setDisplayName(EXTRA_HACHIS);
        } else if (mode == Mode.POELE_A_FRIRE) {
            setDisplayName(POELE_A_FRIRE_NAME);
            setDisplayIcon(POELE_A_FRIRE_ICON);
        } else {
            setDisplayName(IFRIT_POELE_A_FRIRE_NAME);
            setDisplayIcon(IFRIT_POELE_A_FRIRE_ICON);
        }
    }

    public void setNormal(LivingEntity entity) {
        altModeComponent.setMode(entity, Mode.NORMAL);
    }

    public void setDiable(LivingEntity entity) {
        altModeComponent.setMode(entity, Mode.POELE_A_FRIRE);
    }

    public void setIfrite(LivingEntity entity) {
        altModeComponent.setMode(entity, Mode.IFRIT_POELE_A_FRIRE);
    }

    /**
     * Cart's Diable Jambe only updates Cart's original Extra Hachis instance.
     * Mirror that link here so this Kazi-owned replacement receives the same
     * Poele a Frire and Ifrit Poele a Frire boosts.
     */
    private void syncJambeMode(LivingEntity entity) {
        IAbilityData abilityData = AbilityDataCapability.get(entity);
        CartDiableJambeAbility jambe = abilityData == null
                ? null
                : (CartDiableJambeAbility) abilityData.getEquippedAbility(CartDiableJambeAbility.INSTANCE);

        Mode desiredMode = Mode.NORMAL;
        if (jambe != null && jambe.isContinuous()) {
            desiredMode = jambe.isIfrit() ? Mode.IFRIT_POELE_A_FRIRE : Mode.POELE_A_FRIRE;
        }

        if (altModeComponent.getCurrentMode() != desiredMode) {
            altModeComponent.setMode(entity, desiredMode);
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        IEntityStats props = EntityStatsCapability.get(entity);
        IQuestData questProps = QuestDataCapability.get((PlayerEntity) entity);
        return props.isBlackLeg() && questProps.hasFinishedQuest(CartQuests.BLACKLEG_TRIAL_02);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Extra Hachis", AbilityCategory.STYLE, ExtraHachisRework::new)
                .addAdvancedDescriptionLine(
                        (e, a) -> EXTRA_HACHIS.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                        (e, a) -> DESCRIPTION[0],
                        DealDamageComponent.getTooltip(8.0F),
                        CooldownComponent.getTooltip(240.0F))
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        (e, a) -> POELE_A_FRIRE_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                        (e, a) -> DESCRIPTION[1],
                        DealDamageComponent.getTooltip(10.0F),
                        CooldownComponent.getTooltip(300.0F))
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        (e, a) -> IFRIT_POELE_A_FRIRE_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                        (e, a) -> DESCRIPTION[2],
                        ChargeComponent.getTooltip(12.0F),
                        CooldownComponent.getTooltip(340.0F))
                .setIcon(EXTRA_HACHIS_ICON)
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(SourceType.FIST)
                .setUnlockCheck(ExtraHachisRework::canUnlock)
                .build();
    }

    public enum Mode {
        NORMAL,
        POELE_A_FRIRE,
        IFRIT_POELE_A_FRIRE
    }
}
