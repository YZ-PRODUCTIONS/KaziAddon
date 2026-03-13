//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import java.awt.Color;

import net.MrMagicalCart.cartaddon.abilities.axestyle.BerserkAbility;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.MrMagicalCart.cartaddon.entities.projectiles.axestyle.FutenrakuProjectile;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FutenrakuRework extends Ability {
    private static final TranslationTextComponent FUTENRAKU = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.futenraku", "Futenraku"));
    private static final ResourceLocation PROJECTILE_ICON = new ResourceLocation("cartaddon", "textures/abilities/divine_departure.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "futenraku", new Pair[]{ImmutablePair.of("Infuse your axes with haoskoku and deliver a powerful horizontal slice. (Fruitless)", (Object)null)});
    public static final AbilityCore<FutenrakuRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    public static int overuse = 3000;
    private LightningDischargeEntity discharge;
    private Color color = new Color(16711680);
    private int radius = 0;
    private int haoMastery = 0;
    private final PoolComponent poolComponent;

    public FutenrakuRework(AbilityCore<FutenrakuRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.projectileComponent, this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addCanUseCheck(AbilityLimits::fruitless);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
            entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
        } else {
            if (!WyHelper.isInChallengeDimension(entity.level)) {
                boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, overuse);
                if (isOnMaxOveruse) {
                    return;
                }
            }

            this.chargeComponent.startCharging(entity, 10.0F);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, CartAnimations.FUTENRAKU);
        BerserkRework Berserk = (BerserkRework)AbilityDataCapability.get(entity).getEquippedAbility(BerserkRework.INSTANCE);
        boolean isBerserk = Berserk != null && Berserk.isContinuous();
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        IHakiData hakiProps = HakiDataCapability.get(entity);
        float haoLevel = hakiProps.getTotalHakiExp() / 100.0F;
        if (haoLevel <= 1.0F) {
            this.radius = 10;
            this.haoMastery = 0;
        } else if (haoLevel > 1.0F && haoLevel <= 1.75F) {
            this.radius = 25;
            this.haoMastery = 1;
        } else if (haoLevel > 1.75F) {
            this.radius = 40;
            this.haoMastery = 2;
        }

        if (isBerserk) {
            this.radius = 60;
        }

        if (entity instanceof PlayerEntity) {
            this.color = new Color(HakiHelper.getHaoshokuColour(entity));
        }

        this.discharge = new LightningDischargeEntity(entity, entity.getX(), entity.getY() + (double)1.5F, entity.getZ(), entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength((float)(this.radius * 2));
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(this.color);
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        int density = this.haoMastery == 2 ? 32 : 16;
        this.discharge.setDensity(density);
        this.discharge.setSize(1.0F);
        this.discharge.setSkipSegments(1);
        if (this.haoMastery == 0) {
            this.discharge.setSplit();
        }

        if (entity instanceof PlayerEntity) {
            entity.level.addFreshEntity(this.discharge);
            if (this.discharge != null) {
                this.discharge.setAliveTicks(40);
            }
        }

    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
        if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
            this.discharge.setPos(entity.getX(), entity.getY() + (double)1.0F, entity.getZ());
        }

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        }

        if (this.discharge != null && !entity.isAlive()) {
            this.discharge.setAliveTicks(0);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.animationComponent.stop(entity);
        FutenrakuProjectile proj = new FutenrakuProjectile(entity.level, entity);
        this.projectileComponent.shoot(proj, entity, 3.5F, 1.0F);
        super.cooldownComponent.startCooldown(entity, 1500.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (this.discharge != null) {
            this.discharge.setAliveTicks(30);
        }

    }

    private FutenrakuProjectile createProjectile(LivingEntity entity) {
        FutenrakuProjectile proj = new FutenrakuProjectile(entity.level, entity);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_08);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Futenraku", AbilityCategory.STYLE, FutenrakuRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{ChargeComponent.getTooltip(10.0F), CooldownComponent.getTooltip(1500.0F)}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(FutenrakuRework::canUnlock).build();
    }

    public static enum Mode {
        PROJECTILE;

        private Mode() {
        }
    }
}
