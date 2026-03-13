package net.kazi.kazimod.abilities.Toshi;

import net.kazi.kazimod.entities.projectiles.GomuGomuNoRedRocProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoElephantGunProjectile;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class GiantPunchAbility extends Ability {

    // =========================================================
    // ALT MODE ENUM
    // =========================================================

    public enum GiantPunchMode {
        GIANT_PUNCH,
        NIKA_PUNCH
    }

    // =========================================================
    // DESCRIPTIONS & ICONS
    // =========================================================

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "giant_punch",
            new Pair[]{ImmutablePair.of(
                    "Winds up a massive punch with a giant fist. Requires Giant Future to be active.",
                    (Object) null
            )}
    );

    private static final TranslationTextComponent GIANT_PUNCH_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.giant_punch", "Giant Punch"));
    private static final TranslationTextComponent NIKA_PUNCH_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.nika_punch", "Nika Punch"));

    private static final ResourceLocation GIANT_PUNCH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/giant_punch.png");
    private static final ResourceLocation NIKA_PUNCH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/nika_punch.png");

    // =========================================================
    // CONSTANTS (all in ticks)
    // =========================================================

    private static final int GIANT_PUNCH_COOLDOWN   = 60; // 6 seconds
    private static final int NIKA_PUNCH_COOLDOWN    = 800; // 40 seconds
    private static final int NIKA_PUNCH_CHARGE_TIME = 100; // 5 seconds

    private static final float NIKA_PUNCH_PROJECTILE_SPEED = 2.0F;

    private static final IDescriptionLine GIANT_PUNCH_NAME_DESC;
    private static final IDescriptionLine NIKA_PUNCH_NAME_DESC;

    public static final AbilityCore<GiantPunchAbility> INSTANCE;

    // =========================================================
    // INSTANCE STATE
    // =========================================================

    private GiantPunchMode currentMode = GiantPunchMode.GIANT_PUNCH;

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final AltModeComponent<GiantPunchMode> altModeComponent =
            (new AltModeComponent<>(this, GiantPunchMode.class, GiantPunchMode.GIANT_PUNCH, true))
                    .addChangeModeEvent(this::altModeChangeEvent);

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::tickChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GiantPunchAbility(AbilityCore<GiantPunchAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.projectileComponent
        });
        this.addUseEvent(this::useEvent);
    }

    // =========================================================
    // ALT MODE SWITCHING
    // =========================================================

    /** Called by FutureOfFreedomAbility when it activates. */
    public void switchNikaPunch(LivingEntity entity) {
        this.altModeComponent.setMode(entity, GiantPunchMode.NIKA_PUNCH);
    }

    /** Called by FutureOfFreedomAbility when it deactivates. */
    public void switchGiantPunch(LivingEntity entity) {
        this.altModeComponent.setMode(entity, GiantPunchMode.GIANT_PUNCH);
    }

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, GiantPunchMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case NIKA_PUNCH:
                this.setDisplayName(NIKA_PUNCH_NAME);
                this.setDisplayIcon(NIKA_PUNCH_ICON);
                break;
            case GIANT_PUNCH:
            default:
                this.setDisplayName(GIANT_PUNCH_NAME);
                this.setDisplayIcon(GIANT_PUNCH_ICON);
                break;
        }
    }

    // =========================================================
    // USE EVENT
    // =========================================================

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.currentMode == GiantPunchMode.NIKA_PUNCH) {
            if (!isFutureOfFreedomActive(entity)) {
                entity.sendMessage(
                        new StringTextComponent("Future of Freedom must be active to use Nika Punch!"),
                        entity.getUUID()
                );
                return;
            }
            if (!this.chargeComponent.isCharging()) {
                this.chargeComponent.startCharging(entity, NIKA_PUNCH_CHARGE_TIME);
            }
        } else {
            if (!isGiantFutureActive(entity)) {
                entity.sendMessage(
                        new StringTextComponent("Giant Future must be active to use Giant Punch!"),
                        entity.getUUID()
                );
                return;
            }
            fireGiantPunch(entity);
        }
    }

    // =========================================================
    // CHARGE EVENTS (Nika Punch only)
    // =========================================================

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS,
                2.5F,
                0.5F + entity.getRandom().nextFloat()
        );
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        if (this.chargeComponent.getChargeTime() % 20.0F == 0.0F) {
            entity.level.playSound(
                    (PlayerEntity) null,
                    entity.blockPosition(),
                    (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                    SoundCategory.PLAYERS,
                    3.0F,
                    0.5F + entity.getRandom().nextFloat()
            );
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        fireNikaPunch(entity);
    }

    // =========================================================
    // FIRE HELPERS
    // =========================================================

    private void fireGiantPunch(LivingEntity entity) {
        GomuGomuNoElephantGunProjectile proj =
                (GomuGomuNoElephantGunProjectile) this.projectileComponent.getNewProjectile(entity);
        this.projectileComponent.shoot(proj, entity, 1.8F, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        this.cooldownComponent.startCooldown(entity, (float) GIANT_PUNCH_COOLDOWN);
    }

    private void fireNikaPunch(LivingEntity entity) {
        GomuGomuNoRedRocProjectile proj = new GomuGomuNoRedRocProjectile(entity.level, entity);
        this.projectileComponent.shoot(proj, entity, NIKA_PUNCH_PROJECTILE_SPEED, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(),
                SoundCategory.PLAYERS,
                2.0F,
                1.0F
        );
        this.cooldownComponent.startCooldown(entity, (float) NIKA_PUNCH_COOLDOWN);
    }

    // =========================================================
    // PROJECTILE FACTORY
    // =========================================================

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        if (this.currentMode == GiantPunchMode.NIKA_PUNCH) {
            return new GomuGomuNoRedRocProjectile(entity.level, entity);
        }
        return new GomuGomuNoElephantGunProjectile(entity.level, entity, this);
    }

    // =========================================================
    // CONDITION CHECKS
    // =========================================================

    private static boolean isGiantFutureActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        if (props == null) return false;
        for (IAbility equipped : props.getEquippedAbilities()) {
            if (equipped instanceof GiantFutureAbility) {
                return ((GiantFutureAbility) equipped).getContinuousComponent().isContinuous();
            }
        }
        return false;
    }

    private static boolean isFutureOfFreedomActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        if (props == null) return false;
        for (IAbility equipped : props.getEquippedAbilities()) {
            if (equipped instanceof FutureOfFreedomAbility) {
                return ((FutureOfFreedomAbility) equipped).getContinuousComponent().isContinuous();
            }
        }
        return false;
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        GIANT_PUNCH_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GIANT_PUNCH_NAME));
        NIKA_PUNCH_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(NIKA_PUNCH_NAME));

        INSTANCE = (new AbilityCore.Builder<>("Giant Punch", AbilityCategory.DEVIL_FRUITS, GiantPunchAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        GIANT_PUNCH_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) GIANT_PUNCH_COOLDOWN)
                })
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        NIKA_PUNCH_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) NIKA_PUNCH_COOLDOWN),
                        ChargeComponent.getTooltip((float) NIKA_PUNCH_CHARGE_TIME)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}