package net.kazi.kazimod.abilities.AkumaRework;

import net.kazi.kazimod.entities.projectiles.HellblazeProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
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
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class HellblazeAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "hellblaze",
                    new Pair[]{ImmutablePair.of(
                            "Shoots a blast of purple demonic fire from your fist, burning enemies and disabling their healing.", null)});

    public static final float DAMAGE_VALUE = 25.0F;
    private static final float DAMAGE = DAMAGE_VALUE;
    private static final float COOLDOWN = 200.0F; // 10 seconds
    private static final float RANGE = 15.0F;

    public static final AbilityCore<HellblazeAbility> INSTANCE;

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public HellblazeAbility(AbilityCore<HellblazeAbility> core) {
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
            // Spawn 3 hellblaze projectiles in a spread pattern
            Vector3d look = entity.getLookAngle();
            for (int i = 0; i < 3; i++) {
                HellblazeProjectile fireball = new HellblazeProjectile(entity.level, entity);
                fireball.setPos(
                        entity.getX() + look.x,
                        entity.getY() + 1.0 + look.y,
                        entity.getZ() + look.z
                );
                double spread = (i - 1) * 0.1;
                fireball.setDeltaMovement(
                        look.x * 1.5 + spread * (-look.z),
                        look.y * 1.5,
                        look.z * 1.5 + spread * look.x
                );
                entity.level.addFreshEntity(fireball);
            }

            // Fire trail particles from player
            for (int j = 0; j < 10; j++) {
                double t = j / 10.0 * 3.0;
                double px = entity.getX() + look.x * t;
                double py = entity.getY() + 1.0 + look.y * t;
                double pz = entity.getZ() + look.z * t;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, py, pz, 2, 0.15, 0.15, 0.15, 0.02);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 0.8F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Hellblaze", AbilityCategory.DEVIL_FRUITS, HellblazeAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
