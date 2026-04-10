package net.kazi.kazimod.abilities.SakuRework;

import net.kazi.kazimod.entities.projectiles.PetalBladeProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SenbonzakuraAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "senbonzakura",
                    new Pair[]{ImmutablePair.of(
                            "The user scatters their blade into a thousand tiny petal-like fragments, dealing damage to all nearby enemies.", null)});

    public static final float DAMAGE_VALUE = 11.0F;
    private static final float DAMAGE = DAMAGE_VALUE;
    private static final float COOLDOWN = 240.0F; // 12 seconds
    private static final float RANGE = 8.0F;

    public static final AbilityCore<SenbonzakuraAbility> INSTANCE;

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public SenbonzakuraAbility(AbilityCore<SenbonzakuraAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.dealDamageComponent,
                this.rangeComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // Spawn 12 petal blade projectiles scattered outward
            for (int i = 0; i < 12; i++) {
                PetalBladeProjectile petal = new PetalBladeProjectile(entity.level, entity, DAMAGE);
                petal.setPos(entity.getX(), entity.getY() + 1.0, entity.getZ());

                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double elevAngle = (entity.getRandom().nextDouble() - 0.3) * 0.5;
                double speed = 0.8 + entity.getRandom().nextDouble() * 0.4;
                petal.setDeltaMovement(
                        Math.cos(angle) * speed,
                        elevAngle * speed,
                        Math.sin(angle) * speed
                );
                entity.level.addFreshEntity(petal);
            }

            // Scatter particles around the user
            for (int i = 0; i < 30; i++) {
                double pAngle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * RANGE;
                double px = entity.getX() + Math.cos(pAngle) * dist;
                double pz = entity.getZ() + Math.sin(pAngle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 2.0;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(20);
                data.setSize(4.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.5F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Senbonzakura", AbilityCategory.DEVIL_FRUITS, SenbonzakuraAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
