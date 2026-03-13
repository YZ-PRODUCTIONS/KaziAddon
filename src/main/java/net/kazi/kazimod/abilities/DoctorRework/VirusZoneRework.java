//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.DoctorRework;

import com.google.common.collect.Lists;
import java.util.Optional;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModArmors;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class VirusZoneRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "virus_zone", new Pair[]{ImmutablePair.of("Throws a lingering splash potion of the mummy virus or ice oni virus.", (Object)null)});
    private static final TranslationTextComponent MUMMY_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.virus_zone_mummy", "Virus Zone: Mummy"));
    private static final TranslationTextComponent ICE_ONI_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.virus_zone_ice_oni", "Virus Zone: Ice Oni"));
    private static final ResourceLocation MUMMY_ICON = new ResourceLocation("mineminenomi", "textures/abilities/virus_zone_mummy.png");
    private static final ResourceLocation ICE_ONI_ICON = new ResourceLocation("mineminenomi", "textures/abilities/virus_zone_ice_oni.png");
    private static final int COOLDOWN = 400;
    private static final int EFFECT_TIME = 200;
    public static final AbilityCore<VirusZoneRework> INSTANCE;
    private final AltModeComponent<Mode> altModeComponent;

    public VirusZoneRework(AbilityCore<VirusZoneRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<Mode>(this, Mode.class, Mode.MUMMY)).addChangeModeEvent(this::onAltModeChange);        this.setDisplayName(MUMMY_NAME);
        this.setDisplayIcon(MUMMY_ICON);
        super.isNew = true;
        this.addComponents(new AbilityComponent[]{this.altModeComponent});
        this.addCanUseCheck(AbilityHelper::requiresMedicBag);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity livingEntity, IAbility ability) {
        ItemStack stack = new ItemStack(Items.LINGERING_POTION);
        if (this.altModeComponent.getCurrentMode() == VirusZoneRework.Mode.MUMMY) {
            stack = PotionUtils.setCustomEffects(stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance((Effect) ModEffects.MUMMY_VIRUS.get(), 200, 1)}));
        } else if (this.altModeComponent.getCurrentMode() == VirusZoneRework.Mode.ICE_ONI) {
            stack = PotionUtils.setCustomEffects(stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance((Effect)ModEffects.ICE_ONI.get(), 200, 10)}));
        }

        PotionEntity potion = new PotionEntity(livingEntity.level, livingEntity);
        potion.setItem(stack);
        potion.xRot -= -20.0F;
        potion.shootFromRotation(livingEntity, livingEntity.xRot, livingEntity.yRot, -20.0F, 1.0F, 0.0F);
        livingEntity.level.addFreshEntity(potion);
        if (livingEntity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)livingEntity;
            Optional<ItemStack> medicBag = ItemsHelper.findItemInSlot(player, EquipmentSlotType.CHEST, (Item)ModArmors.MEDIC_BAG.get());
            if (medicBag.isPresent()) {
                ((ItemStack)medicBag.get()).hurtAndBreak(10, player, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            }
        }

        this.cooldownComponent.startCooldown(livingEntity, 200.0F);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == VirusZoneRework.Mode.MUMMY) {
            this.setDisplayName(MUMMY_NAME);
            this.setDisplayIcon(MUMMY_ICON);
        } else if (mode == VirusZoneRework.Mode.ICE_ONI) {
            this.setDisplayName(ICE_ONI_NAME);
            this.setDisplayIcon(ICE_ONI_ICON);
        }

    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_04);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Virus Zone", AbilityCategory.STYLE, VirusZoneRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(400.0F)}).setUnlockCheck(VirusZoneRework::canUnlock).build();
    }

    public static enum Mode {
        MUMMY,
        ICE_ONI;

        private Mode() {
        }
    }
}
