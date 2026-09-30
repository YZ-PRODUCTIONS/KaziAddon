package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
import net.kazi.kazimod.entities.projectiles.MaxOutputRedProjectile;
import net.kazi.kazimod.entities.projectiles.RedProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class RedAbility extends Ability {

    public enum RedMode { NORMAL, MAX_OUTPUT }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "red",
            new Pair[]{ImmutablePair.of("Reverses the force of Infinity and fires it as a devastating repelling projectile", (Object) null)}
    );

    private static final TranslationTextComponent RED_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.red", "Red"));
    private static final TranslationTextComponent MAX_OUTPUT_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.max_output_red", "Red: Maximum Output"));

    private static final ResourceLocation RED_ICON =
            new ResourceLocation("kazimod", "textures/abilities/red.png");

    public static final float COOLDOWN = 400.0F;
    public static final float MAX_OUTPUT_COOLDOWN = 800.0F;
    private static final float CHARGE_TIME = 20.0F;
    private static final float HOLLOW_NUKE_COOLDOWN = 1800.0F;
    public static final AbilityCore<RedAbility> INSTANCE;

    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createRedProjectile);
    private final ProjectileComponent maxOutputProjectileComponent =
            new ProjectileComponent(this, this::createMaxOutputProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final AltModeComponent<RedMode> altModeComponent;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);

    private final KokuChargeVisual chargeVisual = new KokuChargeVisual();
    private RedMode currentMode = RedMode.NORMAL;

    public RedAbility(AbilityCore<RedAbility> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, RedMode.class, RedMode.NORMAL))
                .addChangeModeEvent(this::onModeChange);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.animationComponent,
                this.projectileComponent,
                this.maxOutputProjectileComponent
        });
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
        this.addRemoveEvent((entity, ability) -> this.chargeVisual.stop());
    }

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        HollowPurpleAbility hollowPurple = (HollowPurpleAbility) data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        if (hollowPurple != null && hollowPurple.isCharging()) {
            return AbilityUseResult.fail(null);
        }
        return AbilityUseResult.success();
    }

    private void onModeChange(LivingEntity entity, IAbility ability, RedMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case MAX_OUTPUT:
                this.setDisplayName(MAX_OUTPUT_NAME);
                break;
            case NORMAL:
            default:
                this.setDisplayName(RED_NAME);
                this.setDisplayIcon(RED_ICON);
                break;
        }
    }

    public void switchToNormalMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RedMode.NORMAL);
    }

    public void switchToMaxOutputMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RedMode.MAX_OUTPUT);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.animationComponent.start(entity, KaziAnimations.GOJO_RED);
            this.chargeVisual.start(entity, ability, net.kazi.kazimod.entities.KokuVfxEntity.RED_CHARGE, (int) CHARGE_TIME);
            entity.level.playSound(
                    (PlayerEntity) null,
                    entity.blockPosition(),
                    KaziSounds.RED_CHARGE_SFX.get(),
                    SoundCategory.PLAYERS,
                    1.0F, 1.0F
            );
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        this.chargeVisual.update(this.chargeComponent.getChargePercentage());
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.chargeVisual.stop();
        if (!entity.level.isClientSide) {
            net.kazi.kazimod.entities.KokuVfxEntity.impact(entity.level,
                    net.kazi.kazimod.entities.KokuVfxEntity.castOrigin(entity, 1.0F, 0),
                    net.kazi.kazimod.entities.KokuVfxEntity.RED_IMPACT, 2.0F);
            this.animationComponent.stop(entity);
            if (currentMode == RedMode.MAX_OUTPUT) {
                this.maxOutputProjectileComponent.shoot(entity, 3.0F, 1.0F);
            } else {
                this.projectileComponent.shoot(entity, 3.0F, 1.0F);
            }
            ((ServerWorld) entity.level).getChunkSource()
                    .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            entity.level.playSound(
                    (PlayerEntity) null,
                    entity.blockPosition(),
                    KaziSounds.RED_FIRE_SFX.get(),
                    SoundCategory.PLAYERS,
                    1.0F, 1.0F
            );
            float cooldown = currentMode == RedMode.MAX_OUTPUT ? MAX_OUTPUT_COOLDOWN : COOLDOWN;
            super.cooldownComponent.startCooldown(entity, cooldown);
        }
    }

    private RedProjectile createRedProjectile(LivingEntity entity) {
        return new RedProjectile(entity.level, entity, this);
    }

    private MaxOutputRedProjectile createMaxOutputProjectile(LivingEntity entity) {
        return new MaxOutputRedProjectile(entity.level, entity, this);
    }

    public static void startHollowNukeCooldown(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        RedAbility red = (RedAbility) data.getEquippedAbility(INSTANCE);
        if (red != null) {
            red.cooldownComponent.startCooldown(entity, HOLLOW_NUKE_COOLDOWN);
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Red", AbilityCategory.DEVIL_FRUITS, RedAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(new SourceType[]{SourceType.INDIRECT, SourceType.PROJECTILE})
                .setUnlockCheck(RedAbility::canUnlock)
                .build();
    }
}
