package net.kazi.kazimod.abilities.MochiRework;

import net.MrMagicalCart.cartaddon.entities.mobs.ability.KuriMochiEntity;
import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.KuriMochiProjectile;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import javax.annotation.Nullable;

/** Local, independently editable clone of CartAddon's visible Kuri Mochi ability. */
public class KuriMochiClone extends Ability {
    private static final float HOLD_TIME = 200.0F;
    private static final float COOLDOWN = 500.0F;
    private static final double HEIGHT_ABOVE_HEAD = 1.0D;
    private static final int FIRE_INTERVAL = 4;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "kuri_mochi",
            new Pair[]{ImmutablePair.of(
                    "The user summons a spiky donut that fires spikes at their opponents.",
                    null)});

    public static final AbilityCore<KuriMochiClone> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(100, this::onContinuityStart)
            .addTickEvent(100, this::onContinuityTick)
            .addEndEvent(100, this::onContinuityEnd);
    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);
    private KuriMochiEntity kuriMochiEntity;

    public KuriMochiClone(AbilityCore<KuriMochiClone> core) {
        super(core);
        this.isNew = true;
        this.addComponents(continuousComponent, projectileComponent);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        continuousComponent.triggerContinuity(entity, HOLD_TIME);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        kuriMochiEntity = new KuriMochiEntity(entity.level, entity);
        kuriMochiEntity.moveTo(entity.getX(), getFollowHeight(entity), entity.getZ(), entity.yRot, entity.xRot);
        // Disable Cart Addon's AI-driven 24-shot burst. This clone handles
        // firing directly for the summon's entire lifetime instead.
        kuriMochiEntity.setNoAi(true);
        entity.level.addFreshEntity(kuriMochiEntity);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (kuriMochiEntity != null && kuriMochiEntity.isAlive()) {
            kuriMochiEntity.setPos(entity.getX(), getFollowHeight(entity), entity.getZ());
            kuriMochiEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            LivingEntity target = findPriorityTarget(entity);
            if (target != null) {
                kuriMochiEntity.lookAt(EntityAnchorArgument.Type.EYES,
                        target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D));
                kuriMochiEntity.yHeadRot = kuriMochiEntity.yRot;
                kuriMochiEntity.yBodyRot = kuriMochiEntity.yRot;
                if (!entity.level.isClientSide
                        && continuousComponent.getContinueTime() % FIRE_INTERVAL == 0.0F) {
                    entity.level.playSound(null, kuriMochiEntity.blockPosition(),
                            ModSounds.PISTOL_SHOOT.get(), SoundCategory.PLAYERS, 10.0F, 2.0F);
                    projectileComponent.shoot(kuriMochiEntity, 3.75F, 0.45F);
                }
            }
            kuriMochiEntity.addEffect(new EffectInstance(
                    ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
        } else {
            continuousComponent.stopContinuity(entity);
        }
    }

    private double getFollowHeight(LivingEntity entity) {
        return entity.getY() + entity.getBbHeight() + HEIGHT_ABOVE_HEAD;
    }

    @Nullable
    private LivingEntity findPriorityTarget(LivingEntity owner) {
        return owner.level.getEntitiesOfClass(LivingEntity.class,
                        kuriMochiEntity.getBoundingBox().inflate(64.0D),
                        target -> target != owner
                                && target != kuriMochiEntity
                                && target.isAlive()
                                && !owner.isAlliedTo(target))
                .stream()
                .min((first, second) -> Double.compare(
                        targetPriority(first), targetPriority(second)))
                .orElse(null);
    }

    private double targetPriority(LivingEntity target) {
        double categoryPenalty = target instanceof PlayerEntity ? 0.0D : 1000000.0D;
        return categoryPenalty + kuriMochiEntity.distanceToSqr(target);
    }

    private KuriMochiProjectile createProjectile(LivingEntity entity) {
        return new KuriMochiProjectile(entity.level, entity, this);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        float activeTime = Math.min(HOLD_TIME, continuousComponent.getContinueTime());
        if (kuriMochiEntity != null) {
            kuriMochiEntity.remove();
            kuriMochiEntity = null;
        }
        cooldownComponent.startCooldown(entity, COOLDOWN * (activeTime / HOLD_TIME));
    }

    @Nullable
    public KuriMochiEntity getKuriMochiEntity() {
        return kuriMochiEntity;
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Kuri Mochi", AbilityCategory.DEVIL_FRUITS, KuriMochiClone::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        CooldownComponent.getTooltip(0.0F, COOLDOWN))
                .setIcon(new ResourceLocation("cartaddon", "textures/abilities/kuri_mochi.png"))
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .build();
    }
}
