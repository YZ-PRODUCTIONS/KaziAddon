package net.kazi.kazimod.abilities.HakiRework;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiHardeningAbility;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BusoshokuHakiFullBodyHardeningRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "busoshoku_haki_full_body_hardening", new Pair[]{ImmutablePair.of("Covers the whole body of the user user in a layer of Armament haki, used for a balance between offense and defense.", null)});
    public static final AbilityCore<BusoshokuHakiFullBodyHardeningRework> INSTANCE;
    public static final AbilityOverlay OVERLAY;
    private static final UUID ARMOR_UUID;
    private static final UUID ARMOR_THOUGNESS_UUID;
    private static final UUID TOUGHNESS_UUID;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::continuousStartEvent).addEndEvent(this::continuousStopEvent).addTickEvent(this::continuousTickEvent);
    private final ChangeStatsComponent statsComponent = new ChangeStatsComponent(this);
    private final SkinOverlayComponent skinOverlayComponent;

    public BusoshokuHakiFullBodyHardeningRework(AbilityCore<BusoshokuHakiFullBodyHardeningRework> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.continuousComponent, this.statsComponent, this.skinOverlayComponent});
        super.addCanUseCheck(HakiHelper::canEnableHaki);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        IHakiData hakiProps = HakiDataCapability.get(entity);
        double defense = hakiProps.getBusoshokuHakiExp() / 12.5F;
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, this.getArmorMod(defense), (e) -> this.continuousComponent.isContinuous());
        this.statsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, this.getArmorThougnessMod(defense / 4.0D), (e) -> this.continuousComponent.isContinuous());
        this.statsComponent.addAttributeModifier((Attribute) ModAttributes.TOUGHNESS.get(), this.getToughnessMod(8.0D * (hakiProps.getBusoshokuHakiExp() / (float) CommonConfig.INSTANCE.getHakiExpLimit())), (e) -> this.continuousComponent.isContinuous());
        this.continuousComponent.triggerContinuity(entity);
    }

    private void continuousStartEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.BUSOSHOKU_HAKI_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.8F);
        this.skinOverlayComponent.showAll(entity);
    }

    private void continuousStopEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.hideAll(entity);
    }

    private void continuousTickEvent(LivingEntity entity, IAbility ability) {
        boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, 0);
        if (isOnMaxOveruse) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private AbilityAttributeModifier getArmorMod(double amount) {
        return new AbilityAttributeModifier(ARMOR_UUID, INSTANCE, "Full Body Haki Armor Modifier", amount, Operation.ADDITION);
    }

    private AbilityAttributeModifier getArmorThougnessMod(double amount) {
        return new AbilityAttributeModifier(ARMOR_THOUGNESS_UUID, INSTANCE, "Full Body Haki Armor Toughness Modifier", amount, Operation.ADDITION);
    }

    private AbilityAttributeModifier getToughnessMod(double amount) {
        return new AbilityAttributeModifier(TOUGHNESS_UUID, INSTANCE, "Full Body Haki Toughness Modifier", amount, Operation.ADDITION);
    }

    private static boolean canUnlock(LivingEntity user) {
        IAbilityData abilityProps = AbilityDataCapability.get(user);
        IHakiData props = HakiDataCapability.get(user);
        IEntityStats statsProps = EntityStatsCapability.get(user);
        boolean hasHardeningUnlocked = abilityProps.hasUnlockedAbility(BusoshokuHakiHardeningAbility.INSTANCE);
        return hasHardeningUnlocked && statsProps.getDoriki() > 5000.0D && props.getBusoshokuHakiExp() > HakiHelper.getBusoshokuFullBodyExpNeeded(user);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Busoshoku Haki: Full Body Hardening", AbilityCategory.HAKI, BusoshokuHakiFullBodyHardeningRework::new)).addDescriptionLine(DESCRIPTION).setUnlockCheck(BusoshokuHakiFullBodyHardeningRework::canUnlock).build();
        OVERLAY = (new AbilityOverlay.Builder()).setTexture(ModResources.BUSOSHOKU_HAKI_ARM).setColor(WyHelper.hexToRGB("#FFFFFFAA")).build();
        ARMOR_UUID = UUID.fromString("0457f786-0a5a-4e83-9ea6-f924c259a798");
        ARMOR_THOUGNESS_UUID = UUID.fromString("0457f786-0a5a-4e83-9ea6-f924c259a799");
        TOUGHNESS_UUID = UUID.fromString("9121ac66-fb1c-48a7-a636-0cdc3f01d96e");
    }
}
