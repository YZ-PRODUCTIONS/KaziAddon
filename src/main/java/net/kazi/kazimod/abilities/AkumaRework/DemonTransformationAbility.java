package net.kazi.kazimod.abilities.AkumaRework;

import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class DemonTransformationAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "demon_transformation",
                    new Pair[]{ImmutablePair.of(
                            "The user transforms into their demon form, gaining increased toughness, attack damage, and the ability to fly.", null)});

    private static final float COOLDOWN = 1200.0F; // 60 seconds

    public static final AbilityCore<DemonTransformationAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);

    private static final AbilityAttributeModifier ATTACK_DAMAGE_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_TOUGHNESS_MODIFIER;

    public DemonTransformationAbility(AbilityCore<DemonTransformationAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.changeStatsComponent
        });

        this.changeStatsComponent.addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_MODIFIER);

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, -1.0F); // Infinite duration, toggle off manually
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        // Apply stat buffs
        this.changeStatsComponent.applyModifiers(entity);

        // Enable flight
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            player.abilities.mayfly = true;
            player.onUpdateAbilities();
        }

        // Transformation particles
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 40; i++) {
                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * 3.0;
                double px = entity.getX() + Math.cos(angle) * dist;
                double pz = entity.getZ() + Math.sin(angle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 3.0;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, py, pz, 1, 0, 0.2, 0, 0.05);
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 1, 0, 0.1, 0, 0.03);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.3F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        // Dark aura particles while transformed
        if (!entity.level.isClientSide && entity.tickCount % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (entity.getRandom().nextDouble() - 0.5) * 1.5;
                double offsetY = entity.getRandom().nextDouble() * 2.0;
                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 1.5;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                        1, 0, 0.05, 0, 0.01);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        // Remove stat buffs
        this.changeStatsComponent.removeModifiers(entity);

        // Disable flight
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!player.isCreative() && !player.isSpectator()) {
                player.abilities.mayfly = false;
                player.abilities.flying = false;
                player.onUpdateAbilities();
            }
        }

    }

    public boolean isContinuous() {
        return this.continuousComponent.isContinuous();
    }

    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Demon Transformation", AbilityCategory.DEVIL_FRUITS, DemonTransformationAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChangeStatsComponent.getTooltip()
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();

        ATTACK_DAMAGE_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012"),
                INSTANCE,
                "Demon Form Attack Boost",
                0.3,
                Operation.MULTIPLY_TOTAL
        );

        ARMOR_TOUGHNESS_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123"),
                INSTANCE,
                "Demon Form Toughness Boost",
                0.4,
                Operation.MULTIPLY_TOTAL
        );
    }
}
