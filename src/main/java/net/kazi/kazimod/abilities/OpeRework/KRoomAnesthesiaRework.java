//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.OpeRework;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedOpeHelper;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class KRoomAnesthesiaRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "kroom", new Pair[]{ImmutablePair.of("The user becomes more experienced with their ROOM, allowing them to coat themselves in it. This ability allows the use of §aShock Wille§r or §aPuncture Wille§r.", new Object[]{"§a" + Math.round(19.999998F) + "%§r"})});
    private static final float COOLDOWN = 200.0F;
    public static final AbilityCore<KRoomAnesthesiaRework> INSTANCE;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
    private static final AbilityOverlay OVERLAY;
    private final SkinOverlayComponent skinOverlayComponent;
    private final PoolComponent poolComponent;
    private Interval playSoundInterval;
    private boolean abilityUsed;

    public KRoomAnesthesiaRework(AbilityCore<KRoomAnesthesiaRework> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.ROOM, new AbilityPool2[0]);
        this.playSoundInterval = new Interval(18);
        this.abilityUsed = false;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.skinOverlayComponent, this.continuousComponent, this.animationComponent, this.chargeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (ReworkedOpeHelper.checkRoomActive(entity)) {
            entity.sendMessage(new StringTextComponent("K-Room can't be used alongside ROOM!"), entity.getUUID());
        } else {
            if (!this.continuousComponent.isContinuous()) {
                this.chargeComponent.startCharging(entity, 50.0F);
            } else {
                this.continuousComponent.stopContinuity(entity);
            }

        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.abilityUsed = false;
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.playSoundInterval.canTick()) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.ROOM_CHARGE_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.85F);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.showAll(entity);
        this.updateKRoomSwordTint(entity, true);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        this.updateKRoomSwordTint(entity, true);
        if (this.abilityUsed) {
            this.continuousComponent.stopContinuity(entity);
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.hideAll(entity);
        this.updateKRoomSwordTint(entity, false);
        this.cooldownComponent.startCooldown(entity, 200.0F);
    }

    private void updateKRoomSwordTint(LivingEntity entity, boolean active) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        clearKRoomSwordTint(player);
        if (!active) {
            return;
        }

        applyKRoomSwordTint(player.getMainHandItem());
        applyKRoomSwordTint(player.getOffhandItem());
    }

    private void clearKRoomSwordTint(PlayerEntity player) {
        for (ItemStack stack : player.inventory.items) {
            clearKRoomSwordTint(stack);
        }
        for (ItemStack stack : player.inventory.offhand) {
            clearKRoomSwordTint(stack);
        }
    }

    private void clearKRoomSwordTint(ItemStack stack) {
        if (!stack.isEmpty() && stack.hasTag()) {
            stack.getTag().putBoolean("kroomSwordActive", false);
        }
    }

    private void applyKRoomSwordTint(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return;
        }

        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean("kroomSwordActive", true);
    }

    public void setAbilityUsed(boolean use) {
        this.abilityUsed = use;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("K-Room", AbilityCategory.DEVIL_FRUITS, KRoomAnesthesiaRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)}).build();
        OVERLAY = (new AbilityOverlay.Builder()).setOverlayPart(OverlayPart.ARM).setTexture(CartResources.KROOM).build();
    }
}
