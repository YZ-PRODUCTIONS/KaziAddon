package net.kazi.kazimod.abilities.KendoStyle;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ZanshiAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "zanshi",
                    new Pair[]{ImmutablePair.of(
                            "The user dashes forward with their sword, piercing through the target's chest, stunning them and dealing heavy damage.", null)});

    private static final float COOLDOWN = 360.0F; // 18 seconds
    private static final float DAMAGE = 50.0F;
    private static final float DASH_DISTANCE = 10.0F;
    private static final int STUN_DURATION = 10; // 0.5 seconds (10 ticks)
    private static final float RANGE = 2.0F;

    public static final AbilityCore<ZanshiAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public ZanshiAbility(AbilityCore<ZanshiAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.animationComponent,
                this.hitTrackerComponent
        });

        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, 1.0F);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();

        // Damage the sword
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

        // Raytrace to find dash destination
        BlockPos.Mutable blockPos = WyHelper.rayTraceBlockSafe(entity, DASH_DISTANCE).mutable();

        // Create piercing damage source
        AbilityDamageSource source = (AbilityDamageSource) ((ModDamageSource) this.dealDamageComponent.getDamageSource(entity)).setSlash();
        source.setUnavoidable();

        // Get all targets in the dash line
        List<LivingEntity> targets = this.rangeComponent.getTargetsInLine(entity, DASH_DISTANCE, RANGE);

        for (LivingEntity target : targets) {
            if (this.hitTrackerComponent.canHit(target)) {
                boolean hit = this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source);

                if (hit) {
                    // Stun the target for 0.5 seconds
                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, STUN_DURATION, 255, false, false));
                    target.addEffect(new EffectInstance(Effects.BLINDNESS, STUN_DURATION, 0, false, false));
                    target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), STUN_DURATION, 1, false, false));

                    // Spawn particles
                    if (!entity.level.isClientSide) {
                        WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld) entity.level,
                                target.getX(), target.getY() + (double) target.getEyeHeight(), target.getZ());
                        WyHelper.spawnParticles(ParticleTypes.CRIT, (ServerWorld) entity.level,
                                target.getX(), target.getY() + (double) target.getEyeHeight(), target.getZ());
                    }
                }
            }
        }

        // Teleport the player to the dash destination
        entity.stopRiding();
        entity.teleportToWithTicket((double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ());

        // Swing animation
        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        // Dash sound
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Zanshi", AbilityCategory.STYLE, ZanshiAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(DASH_DISTANCE, RangeType.LINE)
                })
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
