package net.kazi.kazimod.abilities.GomuRework;

import net.kazi.kazimod.entities.projectiles.GomuGomuNoDawnGatlingProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper.Gears;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.RepeaterAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoElephantGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoJetPistolProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoKingKongGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoPistolProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class GomuGomuNoGatlingRework extends RepeaterAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_gatling", new Pair[]{ImmutablePair.of("Rapidly punches enemies in front of the user.", (Object)null)});
    private static final TranslationTextComponent GOMU_GOMU_NO_GATLING_NAME        = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_gatling",        "Gomu Gomu no Gatling"));
    private static final TranslationTextComponent GOMU_GOMU_NO_JET_GATLING_NAME    = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_jet_gatling",    "Gomu Gomu no Jet Gatling"));
    private static final TranslationTextComponent GOMU_GOMU_NO_ELEPHANT_GATLING_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_elephant_gatling", "Gomu Gomu no Elephant Gatling"));
    private static final TranslationTextComponent GOMU_GOMU_NO_KONG_GATLING_NAME   = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_kong_gatling",   "Gomu Gomu no Kong Gatling"));
    private static final TranslationTextComponent GOMU_GOMU_NO_DAWN_GATLING_NAME   = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_dawn_gatling",        "Gomu Gomu no Dawn Gatling"));

    private static final ResourceLocation GOMU_GOMU_NO_GATLING_ICON          = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_gatling.png");
    private static final ResourceLocation GOMU_GOMU_NO_JET_GATLING_ICON      = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_jet_gatling.png");
    private static final ResourceLocation GOMU_GOMU_NO_ELEPHANT_GATLING_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_elephant_gatling.png");
    private static final ResourceLocation GOMU_GOMU_NO_KONG_GATLING_ICON     = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_kong_gatling.png");
    private static final ResourceLocation GOMU_GOMU_NO_DAWN_GATLING_ICON     = new ResourceLocation("kazimod",      "textures/abilities/gomu_gomu_no_dawn_gatling.png");

    private static final int NO_GEAR_COOLDOWN     = 140, NO_GEAR_TRIGGERS     = 20, NO_GEAR_INTERVAL     = 3;
    private static final int SECOND_GEAR_COOLDOWN = 100, SECOND_GEAR_TRIGGERS = 35, SECOND_GEAR_INTERVAL = 2;
    private static final int THIRD_GEAR_COOLDOWN  = 250, THIRD_GEAR_TRIGGERS  = 10, THIRD_GEAR_INTERVAL  = 5;
    private static final int FOURTH_GEAR_COOLDOWN = 200, FOURTH_GEAR_TRIGGERS =  8, FOURTH_GEAR_INTERVAL = 5;
    private static final int FIFTH_GEAR_COOLDOWN  = 240, FIFTH_GEAR_TRIGGERS  = 53, FIFTH_GEAR_INTERVAL  = 2;

    private static final IDescriptionLine NO_GEAR_NAME_DESC;
    private static final IDescriptionLine SECOND_GEAR_NAME_DESC;
    private static final IDescriptionLine THIRD_GEAR_NAME_DESC;
    private static final IDescriptionLine FOURTH_GEAR_NAME_DESC;
    private static final IDescriptionLine FIFTH_GEAR_NAME_DESC;

    public static final AbilityCore<GomuGomuNoGatlingRework> INSTANCE;

    private final AltModeComponent<Gears> altModeComponent;
    private final AnimationComponent animationComponent;
    private Gears currentGear = Gears.NO_GEAR;
    private float projectileSpeed;
    private int   projectileSpread;
    private float cooldown;
    private int   triggers;
    private int   interval;

    public GomuGomuNoGatlingRework(AbilityCore<GomuGomuNoGatlingRework> core) {
        super(core);
        this.altModeComponent  = new AltModeComponent<>(this, Gears.class, Gears.NO_GEAR, true)
                .addChangeModeEvent(this::altModeChangeEvent);
        this.animationComponent = new AnimationComponent(this);
        this.projectileSpeed    = 3.0F;
        this.projectileSpread   = 2;
        this.cooldown           = 140.0F;
        this.triggers           = 20;
        this.interval           = 3;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.altModeComponent, this.animationComponent});
        this.setCustomShootLogic((living) -> {
            for (int i = 0; i < 5; i++) {
                AbilityProjectileEntity p = this.getProjectileFactory(living);
                this.projectileComponent.shootWithSpread(p, living, this.projectileSpeed, 3.0F, this.projectileSpread);
            }
        });
        this.repeaterComponent.addTriggerEvent(100, this::triggerRepeaterEvent);
        this.continuousComponent.addStartEvent(100, this::startContinuityEvent);
        this.continuousComponent.addTickEvent(100, this::tickContinuityEvent);
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.PUNCH_RUSH);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {}

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS,
                2.0F, 0.6F + this.random.nextFloat() / 2.0F);
    }

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, Gears mode) {
        this.currentGear = mode;
        switch (mode) {
            case GEAR_2:
                this.setDisplayName(GOMU_GOMU_NO_JET_GATLING_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_JET_GATLING_ICON);
                this.cooldown = 100.0F; this.triggers = 35; this.interval = 2;
                break;
            case GEAR_3:
                this.setDisplayName(GOMU_GOMU_NO_ELEPHANT_GATLING_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_ELEPHANT_GATLING_ICON);
                this.cooldown = 250.0F; this.triggers = 10; this.interval = 5;
                break;
            case GEAR_4:
                this.setDisplayName(GOMU_GOMU_NO_KONG_GATLING_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_KONG_GATLING_ICON);
                this.cooldown = 200.0F; this.triggers = 8; this.interval = 5;
                break;
            case GEAR_5:
                this.setDisplayName(GOMU_GOMU_NO_DAWN_GATLING_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_DAWN_GATLING_ICON);
                this.cooldown = 240.0F; this.triggers = 53; this.interval = 2;
                break;
            case NO_GEAR:
            default:
                this.setDisplayIcon(GOMU_GOMU_NO_GATLING_ICON);
                this.setDisplayName(GOMU_GOMU_NO_GATLING_NAME);
                this.cooldown = 140.0F; this.triggers = 20; this.interval = 3;
                break;
        }
    }

    public void switchNoGear(LivingEntity entity)    { this.altModeComponent.setMode(entity, Gears.NO_GEAR); }
    public void switchSecondGear(LivingEntity entity){ this.altModeComponent.setMode(entity, Gears.GEAR_2);  }
    public void switchThirdGear(LivingEntity entity) { this.altModeComponent.setMode(entity, Gears.GEAR_3);  }
    public void switchFourthGear(LivingEntity entity){ this.altModeComponent.setMode(entity, Gears.GEAR_4);  }
    public void switchFifthGear(LivingEntity entity) { this.altModeComponent.setMode(entity, Gears.GEAR_5);  }

    @Override public int   getMaxTriggers()       { return this.triggers; }
    @Override public int   getTriggerInterval()   { return this.interval; }
    @Override public float getRepeaterCooldown()  { return this.cooldown; }

    public AbilityProjectileEntity getProjectileFactory(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        AbilityProjectileEntity projectile;
        this.projectileSpeed  = 3.0F;
        float projDmgReduction = 0.8F;
        this.projectileSpread = 2;

        if (this.currentGear == Gears.GEAR_5) {
            projectile = new GomuGomuNoDawnGatlingProjectile(entity.level, entity, this);
            this.projectileSpeed = 3.6F; this.projectileSpread = 2; projDmgReduction = 0.2F;
        } else if (GomuHelper.hasGearFourthActive(props)) {
            projectile = new GomuGomuNoKingKongGunProjectile(entity.level, entity, this);
            projectile.setEntityCollisionSize(2.5D);
            this.projectileSpeed = 2.2F; this.projectileSpread = 6; projDmgReduction = 0.6F;
        } else if (GomuHelper.hasGearThirdActive(props)) {
            projectile = new GomuGomuNoElephantGunProjectile(entity.level, entity, this);
            projectile.setEntityCollisionSize(2.5D);
            this.projectileSpeed = 2.4F; this.projectileSpread = 9; projDmgReduction = 0.6F;
        } else if (GomuHelper.hasGearSecondActive(props) || hasGearSecondReworkActive(props)) {
            // FIX: also check GearSecondRework — GomuHelper only checks vanilla GearSecondAbility
            projectile = new GomuGomuNoJetPistolProjectile(entity.level, entity, this);
            this.projectileSpeed = 3.6F;
        } else {
            projectile = new GomuGomuNoPistolProjectile(entity.level, entity);
        }

        projectile.setDamage(projectile.getDamage() * (1.0F - projDmgReduction));
        projectile.setMaxLife((int)(projectile.getMaxLife() * 0.75F));
        return projectile;
    }

    private static boolean hasGearSecondReworkActive(IAbilityData props) {
        GearSecondRework g2 = (GearSecondRework) props.getEquippedAbility(GearSecondRework.INSTANCE);
        return g2 != null && g2.isContinuous();
    }

    static {
        NO_GEAR_NAME_DESC     = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_GATLING_NAME));
        SECOND_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_JET_GATLING_NAME));
        THIRD_GEAR_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_ELEPHANT_GATLING_NAME));
        FOURTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_KONG_GATLING_NAME));
        FIFTH_GEAR_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_DAWN_GATLING_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Gatling", AbilityCategory.DEVIL_FRUITS, GomuGomuNoGatlingRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, NO_GEAR_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(140.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, SECOND_GEAR_NAME_DESC, GomuHelper.SECOND_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(100.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, THIRD_GEAR_NAME_DESC, GomuHelper.THIRD_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(250.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, FOURTH_GEAR_NAME_DESC, GomuHelper.FOURTH_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, FIFTH_GEAR_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(100.0F)})
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}