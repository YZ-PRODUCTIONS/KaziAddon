package net.kazi.kazimod.abilities.KamaRework;

import net.kazi.kazimod.abilities.Koku.RedAbility;
import net.kazi.kazimod.entities.projectiles.FugaProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;

public class FugaAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "fuga", new Pair[]{ImmutablePair.of("The users fires off a devastating flame arrow", (Object) null)});
    private static final float CHARGE_TIME = 80.0F;
    private static final float COOLDOWN    = 600.0F;

    public static final AbilityCore<FugaAbility> INSTANCE;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this, (comp) -> comp.getChargePercentage() >= 1.0F))
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public FugaAbility(AbilityCore<FugaAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent, this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // If a projectile is already live, detonate it instead of charging again
            FugaProjectile proj = FugaProjectile.ACTIVE_PROJECTILES.get(entity.getUUID());
            if (proj != null && proj.isAlive() && !proj.isFinished()) {
                proj.detonate();
                return;
            }
        }
        this.chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        // No launch — player floats via slowEntityFall in duringChargeEvent
        this.animationComponent.start(entity, KaziAnimations.FUGA_SUKUNA, (int) CHARGE_TIME);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) KaziSounds.FUGA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.0F);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        if (this.chargeComponent.getChargeTime() > 10.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.FUGA.get(), entity,
                    entity.getX(), entity.getY(), entity.getZ());
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        float multiplier = this.chargeComponent.getChargePercentage();

        FugaProjectile projectile = new FugaProjectile(entity.level, entity);
        projectile.multiplier = multiplier;
        projectile.setSize(multiplier > 0.75F ? 1.2F : 10.0F * (1.0F - multiplier));
        entity.level.addFreshEntity(projectile);
        projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 4.0F, 0.0F);

        // Cooldown starts when the projectile detonates, not when fired
    }

    private FugaProjectile createProjectile(LivingEntity entity) {
        return new FugaProjectile(entity.level, entity);
    }

    public static void triggerCooldownForEntity(LivingEntity entity) {
        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability.get(entity);
        if (data == null) return;
        FugaAbility ability = (FugaAbility) data.getEquippedAbility(INSTANCE);
        if (ability == null) return;
        ability.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Fuga", AbilityCategory.DEVIL_FRUITS, FugaAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .setSourceType(new SourceType[]{SourceType.PROJECTILE, SourceType.INTERNAL})
                .setUnlockCheck(FugaAbility::canUnlock)
                .build();
    }
}