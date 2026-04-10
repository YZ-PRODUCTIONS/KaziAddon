package net.kazi.kazimod.abilities.MeraRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.HidarumaProjectile;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class HidarumaRework extends Ability {
    private static final ResourceLocation ICON = new ResourceLocation("mineminenomi", "textures/abilities/hidaruma.png");
    private static final TranslationTextComponent NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.hidaruma", "Hidaruma"));

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "hidaruma",
            new Pair[]{ImmutablePair.of("Creates small green fireballs that set the target on fire", null)}
    );

    public static final AbilityCore<HidarumaRework> INSTANCE =
            new AbilityCore.Builder<>("Hidaruma", AbilityCategory.DEVIL_FRUITS, HidarumaRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(200.0F, 500.0F)
                    )
                    .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setSourceElement(SourceElement.FIRE)
                    .setIcon(ICON)
                    .build();

    private final Interval fireflyInterval = new Interval(2);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public HidarumaRework(AbilityCore<HidarumaRework> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(NAME);
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.projectileComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 60.0F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!this.fireflyInterval.canTick()) {
            return;
        }

        HidarumaProjectile projectile = (HidarumaProjectile) this.projectileComponent.getNewProjectile(entity);
        double xRand = (this.random.nextDouble() - 0.5D) * 0.2D;
        double yRand = (this.random.nextDouble() - 0.5D) * 0.2D;
        double zRand = (this.random.nextDouble() - 0.5D) * 0.2D;
        Vector3d velocity = entity.getLookAngle().normalize().scale(0.375D).add(xRand, yRand, zRand);
        AbilityHelper.setDeltaMovement(projectile, velocity);
        entity.level.addFreshEntity(projectile);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.fireflyInterval.restartIntervalToZero();
        this.cooldownComponent.startCooldown(entity, 200.0F + this.continuousComponent.getContinueTime() * 5.0F);
    }

    private HidarumaProjectile createProjectile(LivingEntity entity) {
        return new FastHidarumaProjectile(entity.level, entity, this);
    }

    public static class FastHidarumaProjectile extends HidarumaProjectile {
        public FastHidarumaProjectile(World world, LivingEntity thrower, Ability ability) {
            super(world, thrower, ability);
        }

        @Override
        public void tick() {
            super.tick();
            Vector3d motion = this.getDeltaMovement();
            if (motion.lengthSqr() > 0.0D) {
                this.setDeltaMovement(motion.scale(1.5D));
            }
        }
    }
}
