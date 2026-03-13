//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KitsuneRework;

import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleTypes;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FoxfireExplosionRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "foxfire_explosion", new Pair[]{ImmutablePair.of("The user creates a cylindrical area of blue flames that damages and sets enemies on fire", (Object)null)});
    private static final float COOLDOWN = 500.0F;
    private static final int HOLD_TIME = 150;
    public static final AbilityCore<FoxfireExplosionRework> INSTANCE;
    private final AnimationComponent animationComponent;
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final RequireMorphComponent requireMorphComponent;
    private final ContinuousComponent continuousComponent;

    private final Map<UUID, Double> playerStartYPositions = new HashMap<>();

    public FoxfireExplosionRework(AbilityCore<FoxfireExplosionRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KITSUNE_HYBRID.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KITSUNE_WALK.get()});
        super.isNew = true;
        this.animationComponent = new AnimationComponent(this);
        this.continuousComponent = (new ContinuousComponent(this))
                .addTickEvent(this::duringContinuityEvent)
                .addEndEvent(this::endContinuityEvent);

        super.addComponents(new AbilityComponent[]{
                this.requireMorphComponent,
                this.animationComponent,
                this.rangeComponent,
                this.continuousComponent
        });
        super.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);
        this.playerStartYPositions.put(entity.getUUID(), entity.getY());
        this.continuousComponent.triggerContinuity(entity, (float)HOLD_TIME);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 3.0F, 1.0F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        double currentX = entity.getX();
        double currentY = entity.getY();
        double currentZ = entity.getZ();

        double radius = 15.0;
        double height = 10.0;

        // Particles (unchanged)
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 80; i++) {
                double angle = (2 * Math.PI * i) / 80;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                data.setLife(25);
                data.setSize(7.0F);
                WyHelper.spawnParticles(data, (ServerWorld)entity.level,
                        currentX + offsetX, currentY + WyHelper.randomDouble() * height, currentZ + offsetZ);
            }
            for (int i = 0; i < 120; i++) {
                double offsetX = (WyHelper.randomDouble() - 0.5) * radius * 2;
                double offsetZ = (WyHelper.randomDouble() - 0.5) * radius * 2;
                if (offsetX * offsetX + offsetZ * offsetZ <= radius * radius) {
                    SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                    data.setLife(20);
                    data.setSize(6.0F);
                    WyHelper.spawnParticles(data, (ServerWorld)entity.level,
                            currentX + offsetX, currentY + WyHelper.randomDouble() * height, currentZ + offsetZ);
                }
            }
            for (int i = 0; i < 40; i++) {
                double spiralAngle = (entity.tickCount * 0.2 + i * 0.5) % (2 * Math.PI);
                double spiralRadius = radius * 0.8 * (1.0 - (i / 40.0));
                double offsetX = Math.cos(spiralAngle) * spiralRadius;
                double offsetZ = Math.sin(spiralAngle) * spiralRadius;
                double offsetY = (i / 40.0) * height;
                SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                data.setLife(30);
                data.setSize(8.0F);
                WyHelper.spawnParticles(data, (ServerWorld)entity.level,
                        currentX + offsetX, currentY + offsetY, currentZ + offsetZ);
            }
        }

        // ✅ Use rangeComponent.getTargetsInArea() — automatically skips teammates
        int power = 0;
        int duration = 100;
        float damage = 2.0F;

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 15.0F)) {
            // Extra check: still enforce the cylinder height bound
            if (!this.isInCylinder(target, currentX, currentY, currentZ, radius, height)) continue;

            target.hurt(AbilityDamageSource.causeAbilityDamage(entity, this), damage);
            target.setSecondsOnFire(5);

            if (!target.hasEffect((Effect) KaziEffects.FLAMING_ROT.get())) {
                target.addEffect(new EffectInstance((Effect)KaziEffects.FLAMING_ROT.get(), duration, 0));
            }

            if (!target.hasEffect(Effects.WEAKNESS)) {
                target.addEffect(new EffectInstance(Effects.WEAKNESS, duration, power));
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.playerStartYPositions.remove(entity.getUUID());
    }

    // Helper method to check if an entity is within the cylinder
    private boolean isInCylinder(LivingEntity target, double centerX, double baseY, double centerZ, double radius, double height) {
        double dx = target.getX() - centerX;
        double dz = target.getZ() - centerZ;
        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= radius * radius &&
                target.getY() >= baseY &&
                target.getY() <= baseY + height;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Foxfire Explosion", AbilityCategory.DEVIL_FRUITS, FoxfireExplosionRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        RangeComponent.getTooltip(30.0F, 10.0F, RangeType.AOE),
                        CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip((float)HOLD_TIME)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        RequireMorphComponent.getTooltip()
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .build();
    }
}