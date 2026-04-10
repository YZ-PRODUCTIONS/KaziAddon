package net.kazi.kazimod.abilities.SakuRework;

import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
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
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SenkeiAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "senkei",
                    new Pair[]{ImmutablePair.of(
                            "The user condenses all petal blades into concentrated swords that form around them, greatly increasing attack power.", null)});

    private static final float CONTINUOUS_DURATION = 200.0F; // 10 seconds
    private static final float COOLDOWN = 900.0F; // 45 seconds

    public static final AbilityCore<SenkeiAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);

    private static final AbilityAttributeModifier DAMAGE_BOOST_MODIFIER;

    public SenkeiAbility(AbilityCore<SenkeiAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.changeStatsComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            this.continuousComponent.startContinuity(entity, CONTINUOUS_DURATION);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_BOOST_MODIFIER);
        this.changeStatsComponent.applyModifiers(entity);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.2F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 5; i++) {
                double angle = (entity.tickCount + i * 72) * 0.1;
                double radius = 2.5;
                double px = entity.getX() + Math.cos(angle) * radius;
                double pz = entity.getZ() + Math.sin(angle) * radius;
                double py = entity.getY() + 1.0 + Math.sin(entity.tickCount * 0.15 + i) * 0.5;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(10);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isContinuous() {
        return this.continuousComponent.isContinuous();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Senkei", AbilityCategory.DEVIL_FRUITS, SenkeiAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(CONTINUOUS_DURATION),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();

        DAMAGE_BOOST_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
                INSTANCE,
                "Senkei Damage Boost",
                0.4,
                Operation.MULTIPLY_TOTAL
        );
    }
}
