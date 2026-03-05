package net.kazi.kazimod.abilities.Toshi;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
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

public class GiantPunchAbility extends Ability {

    // =========================================================
    // STATIC FIELDS
    // =========================================================

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "giant_punch",
            new Pair[]{ImmutablePair.of(
                    "Winds up a massive punch with a giant fist. Requires Giant Future to be active.",
                    (Object) null
            )}
    );

    private static final int   COOLDOWN_TICKS = 120;
    private static final float PROJECTILE_SPEED = 1.8F;

    public static final AbilityCore<GiantPunchAbility> INSTANCE;

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GiantPunchAbility(AbilityCore<GiantPunchAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    // =========================================================
    // USE EVENT
    // =========================================================

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!isGiantFutureActive(entity)) {
            entity.sendMessage(
                    new StringTextComponent("Giant Future must be active to use Giant Punch!"),
                    entity.getUUID()
            );
            return;
        }

        AbilityProjectileEntity projectile = this.createProjectile(entity);
        this.projectileComponent.shoot(projectile, entity, PROJECTILE_SPEED, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(),
                SoundCategory.PLAYERS,
                2.0F,
                1.0F
        );
        this.cooldownComponent.startCooldown(entity, COOLDOWN_TICKS);
    }

    // =========================================================
    // PROJECTILE FACTORY
    // =========================================================

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        // Always fires the Elephant Gun projectile, matching that alt mode exactly
        return new GomuGomuNoElephantGunProjectile(entity.level, entity, null);
    }

    // =========================================================
    // GIANT FUTURE CHECK
    // =========================================================

    /**
     * Returns {@code true} if the {@link GiantFutureAbility} is currently active
     * (i.e. its continuous component is running) on the given entity.
     */
    private static boolean isGiantFutureActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        if (props == null) return false;
        GiantFutureAbility giantFuture = (GiantFutureAbility) props.getEquippedAbility(GiantFutureAbility.INSTANCE);
        if (giantFuture == null) return false;
        return giantFuture.getContinuousComponent().isContinuous();
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder("Giant Punch", AbilityCategory.DEVIL_FRUITS, GiantPunchAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN_TICKS / 20.0F)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}