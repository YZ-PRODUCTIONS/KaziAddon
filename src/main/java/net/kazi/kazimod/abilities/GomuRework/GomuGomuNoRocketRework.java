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
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoRocketProjectile;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class GomuGomuNoRocketRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_rocket", new Pair[]{ImmutablePair.of("Stretches towards a block, then launches the user on an arch depending on where they fist landed.", (Object)null)});
    private static final TranslationTextComponent GOMU_GOMU_NO_ROCKET_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_rocket", "Gomu Gomu no Rocket"));
    private static final TranslationTextComponent GOMU_GOMU_NO_DAWN_ROCKET_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_dawn_rocket", "Gomu Gomu no Dawn Rocket"));
    private static final ResourceLocation GOMU_GOMU_NO_ROCKET_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_rocket.png");
    private static final ResourceLocation GOMU_GOMU_NO_DAWN_ROCKET_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_dawn_rocket.png");
    private static final int COOLDOWN = 60;
    public static final AbilityCore<GomuGomuNoRocketRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final SwingTriggerComponent swingTriggerComponent = (new SwingTriggerComponent(this)).addSwingEvent(this::triggerSwingEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<GomuHelper.Gears> altModeComponent;
    private int airTime;

    public GomuGomuNoRocketRework(AbilityCore<GomuGomuNoRocketRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent(this, GomuHelper.Gears.class, Gears.NO_GEAR, true)).addChangeModeEvent(this::altModeChangeEvent);
        this.airTime = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.swingTriggerComponent, this.projectileComponent, this.altModeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.airTime = 0;
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.isOnGround() && this.airTime < 10) {
            AbilityHelper.slowEntityFall(entity, 10);
            ++this.airTime;
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 60.0F);
    }

    private void triggerSwingEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            IAbilityData props = AbilityDataCapability.get(entity);
            float speed = GomuHelper.hasGearSecondActive(props) ? 4.0F : 3.125F;
            this.projectileComponent.shoot(entity, speed, 0.0F);
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
            entity.swing(Hand.MAIN_HAND, true);
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, GomuHelper.Gears mode) {
        switch (mode) {
            case GEAR_5:
            case GEAR_4:
            case GEAR_3:
            case GEAR_2:
            case NO_GEAR:
            default:
                this.setDisplayIcon(GOMU_GOMU_NO_ROCKET_ICON);
                this.setDisplayName(GOMU_GOMU_NO_ROCKET_NAME);
        }
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        AbilityProjectileEntity projectile = null;
        projectile = new GomuGomuNoRocketProjectile(entity.level, entity, this);
        return projectile;
    }

    public void switchNoGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.NO_GEAR);
    }

    public void switchFifthGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Gears.GEAR_5);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Rocket", AbilityCategory.DEVIL_FRUITS, GomuGomuNoRocketRework::new)).addDescriptionLine(DESCRIPTION).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).build();
    }
}
