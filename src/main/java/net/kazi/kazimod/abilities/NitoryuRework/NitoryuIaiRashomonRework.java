package net.kazi.kazimod.abilities.NitoryuRework;

import com.sun.javafx.geom.Quat4f;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;


import java.util.List;


public class NitoryuIaiRashomonRework extends Ability {
    private static final float COOLDOWN = 180.0F;
    private static final int CHARGE_TIME = 20;
    private static final float DAMAGE = 35.0F;
    private static final float RANGE = 5.0F;
    private static final float MAX_TELEPORT_DISTANCE = 25.0F;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "nitoryu_iai_rashomon", new Pair[]{ImmutablePair.of("The user dashes forward slightly, and slashes all those in front of them", (Object)null)});


    public static final AbilityCore<NitoryuIaiRashomonRework> INSTANCE;

    static {
        INSTANCE = (new AbilityCore.Builder("Nitoryu Iai Rashomon",
                AbilityCategory.STYLE,
                NitoryuIaiRashomonRework::new
        ))
                .addDescriptionLine(
                        "The user dashes forward slightly, and slashes all those in front of them."
                )
                .addAdvancedDescriptionLine(
                        CooldownComponent.getTooltip(300.0F)
                )
                .setSourceType(SourceType.SLASH)
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setUnlockCheck(NitoryuIaiRashomonRework::canUnlock)
                .build();
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.NITORYU) && questProps.hasFinishedQuest(CartQuests.NITORYU_TRIAL_01);
        }
    }
    private final ContinuousComponent continuousComponent;
    public AbilityCore<NitoryuIaiRashomonRework> core;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);




    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private ChangeStatsComponent changeStatsComponent;
    private Quat4f dir;

    public NitoryuIaiRashomonRework(AbilityCore<NitoryuIaiRashomonRework> core) {
        super(core);

        this.setMaxCooldown(COOLDOWN);

        this.continuousComponent = new ContinuousComponent(this)
                .addStartEvent(this::onContinuityStart)
                .addTickEvent(this::onContinuityTick)
                .addEndEvent(this::onContinuityEnd);

        this.isNew = true;

        this.addComponents(
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.animationComponent,
                this.hitTrackerComponent
        );

        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }





    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if ((!(entity instanceof PlayerEntity) || AbilityLimits.canUseNitoryu((PlayerEntity)entity)) && AbilityHelper.canUseSwordsmanAbilities(entity)) {
            this.chargeComponent.startCharging(entity, 15.0F);
        } else {
            entity.sendMessage(new StringTextComponent("You must be holding a sword in both hands to use this move!"), entity.getUUID());
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, CartAnimations.NITORYU_IAI_RASHOMON);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (DevilFruitHelper.getDifferenceToFloor(entity) < (double)51.0F) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity);
        this.animationComponent.stop(entity);


        if (entity instanceof PlayerEntity) {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, user -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

            stack = entity.getOffhandItem();
            stack.hurtAndBreak(1, entity, user -> user.broadcastBreakEvent(EquipmentSlotType.OFFHAND));
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();



        entity.level.playSound(
                null,
                entity.blockPosition(),
                ModSounds.SPIN.get(),
                SoundCategory.PLAYERS,
                2.0F,
                0.5F + this.random.nextFloat() / 2.0F
        );
    }


    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && entity.isAlive()) {


            if (this.continuousComponent.getContinueTime() > 25.0F || super.canUse(entity).isFail()) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            Vector3d dir = entity.getLookAngle().normalize().scale(2.0D);
            AbilityHelper.setDeltaMovement(entity, dir);

            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 1.8F);

            for (LivingEntity target : targets) { if (this.hitTrackerComponent.canHit(target) && entity.canSee(target)) { this.dealDamageComponent.hurtTarget(entity, target, 40.0F); target.addEffect(new EffectInstance(ModEffects.BLEEDING.get(), 60, 0, false, false)); target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 60, 1, false, false)); Vector3d knockback = dir.scale(0.9D); AbilityHelper.setDeltaMovement(target, knockback.x, 0.5D, knockback.z); this.continuousComponent.stopContinuity(entity); break;

                }
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);
    }

}


