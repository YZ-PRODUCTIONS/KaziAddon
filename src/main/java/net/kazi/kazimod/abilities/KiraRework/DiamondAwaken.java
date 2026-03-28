package net.kazi.kazimod.abilities.KiraRework;
import java.awt.Color;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.DamageSource;
import net.minecraft.nbt.CompoundNBT;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.*;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;

public class DiamondAwaken extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("kazimod", "textures/abilities/alts/diamondawaken.png");



    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "diamond_awaken",
            new Pair[]{ImmutablePair.of(
                    "A weaker but temporary form of diamond transformation that coats both the user's body and weapons. Grants 20 seconds of diamond protection.",
                    null
            )}
    );

    private static final float HOLD_TIME = 400.0F;
    private static final float COOLDOWN = 600.0F;
    public static final AbilityCore<DiamondAwaken> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_TOUGHNESS_MODIFIER;
    private static final AbilityAttributeModifier ATTACK_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(100, this::onContinuityStart)
            .addTickEvent(100, this::onContinuityTick)
            .addEndEvent(100, this::onContinuityEnd);

    private final SkinOverlayComponent skinOverlayComponent;
    private final ChangeStatsComponent changeStatsComponent;
    private final HitTriggerComponent hitTriggerComponent;
    private LivingEntity currentUser;

    public DiamondAwaken(AbilityCore<DiamondAwaken> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.changeStatsComponent = new ChangeStatsComponent(this);
        this.hitTriggerComponent = new HitTriggerComponent(this)
                .addTryHitEvent(100, this::tryHitEvent)
                .addOnHitEvent(100, this::onHitEvent);

        this.isNew = true;
        this.addComponents(
                continuousComponent,
                skinOverlayComponent,
                changeStatsComponent,
                hitTriggerComponent
        );

        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, ATTACK_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);

        this.addUseEvent(this::useEvent);

        this.setDisplayIcon(DEFAULT_ICON);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.cooldownComponent.isOnCooldown())
            return;

        this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.applyModifiers(entity);
        this.skinOverlayComponent.showAll(entity);
        this.currentUser = entity;

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!player.getMainHandItem().isEmpty()) {
                if (!player.getMainHandItem().hasTag()) player.getMainHandItem().setTag(new CompoundNBT());
                player.getMainHandItem().getTag().putBoolean("diamondAwakenActive", true);
            }
            if (!player.getOffhandItem().isEmpty()) {
                if (!player.getOffhandItem().hasTag()) player.getOffhandItem().setTag(new CompoundNBT());
                player.getOffhandItem().getTag().putBoolean("diamondAwakenActive", true);
            }
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.PHYSICAL_MOVING_GUARD.get(), 5, 0, false, false));

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!player.getMainHandItem().isEmpty()) {
                if (!player.getMainHandItem().hasTag()) player.getMainHandItem().setTag(new CompoundNBT());
                player.getMainHandItem().getTag().putBoolean("diamondAwakenActive", true);
            }
            if (!player.getOffhandItem().isEmpty()) {
                if (!player.getOffhandItem().hasTag()) player.getOffhandItem().setTag(new CompoundNBT());
                player.getOffhandItem().getTag().putBoolean("diamondAwakenActive", true);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.skinOverlayComponent.hideAll(entity);

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().hasTag())
                player.getMainHandItem().getTag().putBoolean("diamondAwakenActive", false);
            if (!player.getOffhandItem().isEmpty() && player.getOffhandItem().hasTag())
                player.getOffhandItem().getTag().putBoolean("diamondAwakenActive", false);
        }

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.currentUser = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDamage(LivingAttackEvent event) {
        if (this.continuousComponent.isContinuous() && this.currentUser != null) {
            LivingEntity eventEntity = event.getEntityLiving();
            if (eventEntity == this.currentUser) {
                DamageSource source = event.getSource();
                if (source.isProjectile() || source.msgId.equals("player") || source.msgId.equals("mob")) {
                    if (event.getAmount() <= 4.0F) event.setCanceled(true);
                }
            }
        }
    }

    private HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && !entity.getMainHandItem().isEmpty()) return HitResult.HIT;
        return HitResult.PASS;
    }

    private boolean onHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && !entity.getMainHandItem().isEmpty()) {
            source.bypassLogia();
            return true;
        }
        return false;
    }




    static {
        INSTANCE = new AbilityCore.Builder<>("Diamond Awaken", AbilityCategory.DEVIL_FRUITS, DiamondAwaken::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        ChangeStatsComponent.getTooltip()
                )
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setIcon(DEFAULT_ICON)
                .build();

        ARMOR_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("547a5eaa-a969-4328-9364-a40638876d54"),
                INSTANCE,
                "Diamond Awaken Armor Modifier",
                10.0F,
                Operation.ADDITION
        );
        ARMOR_TOUGHNESS_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("764f6317-2d9f-4c54-8906-0201f1521212"),
                INSTANCE,
                "Diamond Awaken Armor Toughness Modifier",
                6.0F,
                Operation.ADDITION
        );
        ATTACK_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("f139d3ce-ac49-42d6-bb47-e93a6b89e44b"),
                INSTANCE,
                "Diamond Awaken Attack Modifier",
                8.0F,
                Operation.ADDITION
        );
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("7db0de61-b5a2-40d9-ab0e-42d6afb5bece"),
                INSTANCE,
                "Diamond Awaken Toughness Modifier",
                10.0F,
                Operation.ADDITION
        );
        OVERLAY = new AbilityOverlay.Builder()
                .setOverlayPart(AbilityOverlay.OverlayPart.LIMB)
                .setTexture(ModResources.DIAMOND_BODY)
                .setColor(new Color(135, 206, 250, 120))
                .build();
    }
}