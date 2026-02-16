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

    // FIX: Use a Map to store Y positions per player UUID instead of a single instance variable
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

        // FIX: Store the starting Y position per player
        this.playerStartYPositions.put(entity.getUUID(), entity.getY());

        this.continuousComponent.triggerContinuity(entity, (float)HOLD_TIME);

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 3.0F, 1.0F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        // FIX: Retrieve the stored Y position for this specific player
        double startY = this.playerStartYPositions.getOrDefault(entity.getUUID(), entity.getY());

        // Create the cylindrical AoE bounding box (30 block diameter = 15 block radius, 10 block height)
        double radius = 15.0;
        double height = 10.0;
        AxisAlignedBB aoeBounds = new AxisAlignedBB(
                entity.getX() - radius, startY, entity.getZ() - radius,
                entity.getX() + radius, startY + height, entity.getZ() + radius
        );

        // FIX: Reduce particle spam - spawn every 3 ticks and reduce count
        if (!entity.level.isClientSide && entity.tickCount % 3 == 0) {
            for (int i = 0; i < 20; i++) { // Reduced from 60 to 20
                double offsetX = (WyHelper.randomDouble() - 0.5) * radius * 2;
                double offsetZ = (WyHelper.randomDouble() - 0.5) * radius * 2;

                // Check if the particle is within the cylinder radius
                if (offsetX * offsetX + offsetZ * offsetZ <= radius * radius) {
                    SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                    data.setLife(20);
                    data.setSize(6.0F);
                    WyHelper.spawnParticles(data, (ServerWorld)entity.level,
                            entity.getX() + offsetX, startY + WyHelper.randomDouble() * height, entity.getZ() + offsetZ);
                }
            }
        }

        // Apply effects to entities in the AoE
        int power = 0;
        int duration = 100;
        float damage = 2.0F; // FIX: Added damage value

        // Get all living entities in the AoE
        List<LivingEntity> entitiesInRange = entity.level.getEntitiesOfClass(
                LivingEntity.class,
                aoeBounds,
                target -> target != entity && this.isInCylinder(target, entity.getX(), startY, entity.getZ(), radius, height)
        );

        // Apply effects to each entity
        for (LivingEntity target : entitiesInRange) {
            // FIX: Apply damage to targets
            target.hurt(AbilityDamageSource.causeAbilityDamage(entity, this), damage);

            // FIX: Set targets on fire as described
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

        // FIX: Clean up the stored Y position to prevent memory leaks
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