//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GomuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper.Gears;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoBajrangGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoElephantGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoJetPistolProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoKingKongGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoPistolProjectile;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class GomuGomuNoPistolRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_pistol", new Pair[]{ImmutablePair.of("The user stretches their arm to punch the opponent.", (Object)null)});
    private static final TranslationTextComponent GOMU_GOMU_NO_PISTOL_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_pistol", "Gomu Gomu no Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_JET_PISTOL_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_jet_pistol", "Gomu Gomu no Jet Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_ELEPHANT_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_elephant_gun", "Gomu Gomu no Elephant Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_KING_KONG_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_king_kong_gun", "Gomu Gomu no King Kong Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_BAJRANG_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_bajrang_gun", "Gomu Gomu no Bajrang Gun"));
    private static final ResourceLocation GOMU_GOMU_NO_PISTOL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_JET_PISTOL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_jet_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_ELEPHANT_GUN_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_elephant_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_KING_KONG_GUN_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_king_kong_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_BAJRANG_GUN_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_bajrang_gun.png");
    private static final int NO_GEAR_COOLDOWN = 30;
    private static final int SECOND_GEAR_COOLDOWN = 20;
    private static final int THIRD_GEAR_COOLDOWN = 120;
    private static final int FOURTH_GEAR_COOLDOWN = 80;
    private static final int FIFTH_GEAR_COOLDOWN = 100;
    private static final AbilityDescriptionLine.IDescriptionLine NO_GEAR_NAME_DESC;
    private static final AbilityDescriptionLine.IDescriptionLine SECOND_GEAR_NAME_DESC;
    private static final AbilityDescriptionLine.IDescriptionLine THIRD_GEAR_NAME_DESC;
    private static final AbilityDescriptionLine.IDescriptionLine FOURTH_GEAR_NAME_DESC;
    private static final AbilityDescriptionLine.IDescriptionLine FIFTH_GEAR_NAME_DESC;
    public static final AbilityCore<GomuGomuNoPistolRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<GomuHelper.Gears> altModeComponent;
    private float speed;
    private float cooldown;

    public GomuGomuNoPistolRework(AbilityCore<GomuGomuNoPistolRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent(this, GomuHelper.Gears.class, Gears.NO_GEAR, true)).addChangeModeEvent(this::altModeChangeEvent);
        this.speed = 2.0F;
        this.cooldown = 30.0F;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.altModeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        AbilityProjectileEntity projectile = this.createProjectile(entity);
        this.projectileComponent.shoot(projectile, entity, this.speed, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, this.cooldown);
    }

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, GomuHelper.Gears mode) {
        switch (mode) {
            case GEAR_2:
                this.setDisplayName(GOMU_GOMU_NO_JET_PISTOL_NAME);
                this.cooldown = 20.0F;
                break;
            case GEAR_3:
                this.setDisplayName(GOMU_GOMU_NO_ELEPHANT_GUN_NAME);
                this.cooldown = 120.0F;
                break;
            case GEAR_4:
                this.setDisplayIcon(GOMU_GOMU_NO_KING_KONG_GUN_ICON);
                this.setDisplayName(GOMU_GOMU_NO_KING_KONG_GUN_NAME);
                this.cooldown = 80.0F;
                break;
            case GEAR_5:
                this.setDisplayName(GOMU_GOMU_NO_BAJRANG_GUN_NAME);
                this.cooldown = 100.0F;
                break;
            case NO_GEAR:
            default:
                this.setDisplayIcon(GOMU_GOMU_NO_PISTOL_ICON);
                this.setDisplayName(GOMU_GOMU_NO_PISTOL_NAME);
                this.cooldown = 30.0F;
        }

    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        AbilityProjectileEntity projectile = null;
        this.speed = 2.0F;
        if (GomuHelper.hasGearFifthActive(props)) {
            projectile = new GomuGomuNoBajrangGunProjectile(entity.level, entity, this);
            this.speed = 1.9F;
        } else if (GomuHelper.hasGearFourthActive(props)) {
            projectile = new GomuGomuNoKingKongGunProjectile(entity.level, entity, this);
            this.speed = 1.8F;
        } else if (GomuHelper.hasGearThirdActive(props)) {
            projectile = new GomuGomuNoElephantGunProjectile(entity.level, entity, this);
            this.speed = 1.8F;
        } else if (GomuHelper.hasGearSecondActive(props)) {
            projectile = new GomuGomuNoJetPistolProjectile(entity.level, entity, this);
            this.speed = 2.5F;
        } else {
            projectile = new GomuGomuNoPistolProjectile(entity.level, entity);
        }

        return projectile;
    }

    public void switchNoGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.NO_GEAR);
    }

    public void switchSecondGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.GEAR_2);
    }

    public void switchThirdGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.GEAR_3);
    }

    public void switchFourthGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.GEAR_4);
    }

    public void switchFifthGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.GEAR_5);
    }

    static {
        NO_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_PISTOL_NAME));
        SECOND_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_JET_PISTOL_NAME));
        THIRD_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_ELEPHANT_GUN_NAME));
        FOURTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_KING_KONG_GUN_NAME));
        FIFTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_BAJRANG_GUN_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Pistol", AbilityCategory.DEVIL_FRUITS, GomuGomuNoPistolRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, NO_GEAR_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(30.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, SECOND_GEAR_NAME_DESC, GomuHelper.SECOND_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(20.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, THIRD_GEAR_NAME_DESC, GomuHelper.THIRD_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(120.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, FOURTH_GEAR_NAME_DESC, GomuHelper.FOURTH_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(80.0F)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).build();
    }
}
