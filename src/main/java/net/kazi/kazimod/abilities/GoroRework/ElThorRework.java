package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's El Thor. */
public class ElThorRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/el_thor.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/el_thor.png");

    public static final Color YELLOW_THUNDER = new Color(-135916, true);
    public static final Color BLUE_THUNDER = WyHelper.hexToRGB("#70EAFF22");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "el_thor",
            new Pair[]{ImmutablePair.of(
                    "Focuses a large cluster of electricity above the target, then sends a powerful lightning bolt crashing down from the sky",
                    null)});

    public static final AbilityCore<ElThorRework> INSTANCE =
            new AbilityCore.Builder<ElThorRework>(
                    "El Thor", AbilityCategory.DEVIL_FRUITS, ElThorRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(360.0F),
                            ChargeComponent.getTooltip(80.0F))
                    .setSourceElement(SourceElement.LIGHTNING)
                    .setSourceHakiNature(SourceHakiNature.SPECIAL)
                    .setIcon(DEFAULT_ICON)
                    .build();

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(2);

    public ElThorRework(AbilityCore<ElThorRework> core) {
        super(core);
        updateDisplayIcon();
        this.isNew = true;
        this.addComponents(chargeComponent, animationComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent((entity, ability) -> updateDisplayIcon());
    }

    private void updateDisplayIcon() {
        this.setDisplayIcon(ClientConfig.INSTANCE.isGoroBlue() ? ALT_ICON : DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        chargeComponent.startCharging(entity, 80.0F);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        particleInterval.restartIntervalToZero();
        animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        AbilityHelper.slowEntityFall(entity);
        if (!particleInterval.canTick()) return;

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, 256.0D, 0.4F);
        double x = mop.getLocation().x;
        double y = mop.getLocation().y;
        double z = mop.getLocation().z;
        double particleAmount = chargeComponent.getChargeTime();
        for (int n = 0; n < particleAmount; n++) {
            double offsetX = WyHelper.randomDouble() * n * 0.225D;
            double offsetZ = WyHelper.randomDouble() * n * 0.225D;
            if (entity instanceof PlayerEntity) {
                WyHelper.spawnParticleEffectForOwner(
                        (ParticleEffect) ModParticleEffects.EL_THOR_AIM.get(),
                        (PlayerEntity) entity,
                        x + offsetX, y, z + offsetZ, null);
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity);
        double time = chargeComponent.getChargePercentage();
        float multiplier = (float) (0.4D + time * 0.6D);
        if (((MorphInfo) ModMorphs.VOLT_AMARU.get()).isActive(entity)) {
            multiplier += 0.25F;
        }

        Vector3d mopPos = mop.getLocation();
        BlockRayTraceResult hitResult = entity.level.clip(new RayTraceContext(
                mopPos,
                mopPos.add(0.0D, 128.0D, 0.0D),
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.ANY,
                entity));
        float targetY = hitResult.getType() == RayTraceResult.Type.BLOCK
                ? (float) hitResult.getLocation().y : 128.0F;
        float travelLength = targetY + 16.0F * multiplier;
        Vector3d pos = new Vector3d(mopPos.x, targetY, mopPos.z);

        LightningEntity boltInner = new LightningEntity(
                entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, travelLength, 24.0F, getCore());
        LightningEntity boltOuter = new LightningEntity(
                entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, travelLength, 24.0F, getCore());
        setBoltProperties(boltInner, 2.0F, 0.0F, 90, 40, false, Color.WHITE, multiplier);
        setBoltProperties(
                boltOuter, 2.5F, 50.0F, 100, 9999, true,
                ClientConfig.INSTANCE.isGoroBlue() ? BLUE_THUNDER : YELLOW_THUNDER,
                multiplier);
        boltOuter.seed = boltInner.seed;
        entity.level.addFreshEntity(boltInner);
        entity.level.addFreshEntity(boltOuter);
        entity.level.playSound(
                null, new BlockPos(mopPos), ModSounds.EL_THOR_SFX.get(),
                SoundCategory.PLAYERS, 20.0F, 1.0F);
        animationComponent.stop(entity);
        cooldownComponent.startCooldown(entity, 440.0F);
    }

    private void setBoltProperties(
            LightningEntity bolt, float size, float damage, int timeAlive, int resetTime,
            boolean explodes, @Nullable Color color, float multiplier) {
        bolt.setBlocksAffectedLimit(8150);
        bolt.setAngle(160);
        bolt.setBranches(1);
        bolt.setSegments(1);
        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision(0.225D);
        bolt.setLightningMovement(false);
        bolt.setExplosion(explodes ? (int) (10.0F * multiplier) : 0, true, 0.25F);
        if (color != null) bolt.setColor(color);
        bolt.setMaxLife(timeAlive);
        bolt.setDamage(damage * multiplier);
        bolt.setTargetTimeToReset(resetTime);
    }
}
