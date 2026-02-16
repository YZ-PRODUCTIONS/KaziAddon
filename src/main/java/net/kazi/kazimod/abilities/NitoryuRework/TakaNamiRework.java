
package net.kazi.kazimod.abilities.NitoryuRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.kazi.kazimod.entities.projectiles.TakaNamiReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;

public class TakaNamiRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "taka_nami", new Pair[]{ImmutablePair.of("The user fires an air shockwave in the shape of a bird.", (Object)null)});
    private static final int COOLDOWN = 300;
    public static final AbilityCore<TakaNamiRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);

    public TakaNamiRework(AbilityCore<TakaNamiRework> core) {
        super(core);
        super.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.chargeComponent});
        this.addUseEvent(this::onUseEvent);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (AbilityLimits.canUseNitoryu(entity) && AbilityHelper.canUseSwordsmanAbilities(entity)) {
            this.chargeComponent.startCharging(entity, 10.0F);
        } else {
            entity.sendMessage(new StringTextComponent("You must be holding a sword in both hands to use this move!"), entity.getUUID());
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN.getEffect(), 2, 0, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        stack = entity.getOffhandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.OFFHAND));
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.projectileComponent.shoot(entity, 3.5F, 0.0F);
        super.cooldownComponent.startCooldown(entity, 300.0F);
    }

    private TakaNamiReworkProjectile createProjectile(LivingEntity entity) {
        TakaNamiReworkProjectile proj = new TakaNamiReworkProjectile(entity.level, entity, this);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.NITORYU) && questProps.hasFinishedQuest(CartQuests.NITORYU_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Taka Nami", AbilityCategory.STYLE, TakaNamiRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(10.0F), CooldownComponent.getTooltip(300.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceElement(SourceElement.AIR).setUnlockCheck(TakaNamiRework::canUnlock).build();
    }
}