package net.kazi.kazimod.abilities.DokuRework;

import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.entities.projectiles.ClonedVenomRoadProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class NewVenomRoadRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"venom_road", (Pair[])new Pair[]{ImmutablePair.of((Object)"Fires a Hydra at the target location that stays there for a few seconds during which time the user can use them to move along their path.", null)});
    private static final ITextComponent NORMAL_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.venom_road", (String)"Venom Road"));
    private static final ITextComponent VENOM_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.venom_road_venom", (String)"Demon Road"));
    private static final ResourceLocation NORMAL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/venom_road.png");
    private static final ResourceLocation VENOM_ICON = new ResourceLocation("mineminenomi", "textures/abilities/venom_road_venom.png");
    public static final int COOLDOWN = 300;
    public static final AbilityCore<NewVenomRoadRework> INSTANCE = new AbilityCore.Builder<NewVenomRoadRework>("Venom Road", AbilityCategory.DEVIL_FRUITS, NewVenomRoadRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float)120.0f)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.POISON).build();
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);
    private final AltModeComponent<Mode> altModeComponent;
    private final AnimationComponent animationComponent;
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private boolean isMovingOwner;
    private int firstTick;
    private int ticks;
    private List<Pair<Vector3d, ClonedVenomRoadProjectile>> projectiles;
    private ClonedVenomRoadProjectile projectileUsed;

    public NewVenomRoadRework(AbilityCore<NewVenomRoadRework> core) {
        super(core);
        this.altModeComponent = new AltModeComponent<Mode>((IAbility)this, Mode.class, Mode.NORMAL, true).addChangeModeEvent(this::onAltModeChange);
        this.animationComponent = new AnimationComponent((IAbility)this);
        this.projectiles = new ArrayList<Pair<Vector3d, ClonedVenomRoadProjectile>>();
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.altModeComponent, this.animationComponent, this.continuousComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            if (this.altModeComponent.isMode(Mode.VENOM)) {
                this.continuousComponent.startContinuity(entity, 10.0f);
            } else if (AbilityHelper.canUseMomentumAbilities((LivingEntity)entity)) {
                this.isMovingOwner = false;
                ClonedVenomRoadProjectile projectile = (ClonedVenomRoadProjectile)this.projectileComponent.getNewProjectile(entity);
                entity.level.addFreshEntity((Entity)projectile);
                projectile.shootFromRotation((Entity)entity, entity.xRot, entity.yRot, 0.0f, 2.0f, 1.0f);
                ImmutablePair pair = ImmutablePair.of((Object)entity.position(), (Object)((Object)projectile));
                this.projectiles.add((Pair<Vector3d, ClonedVenomRoadProjectile>)pair);
                if (projectile != null) {
                    entity.startRiding((Entity)projectile);
                }
                this.animationComponent.start(entity, ModAnimations.SHOOT_SELF_FORWARD);
                this.continuousComponent.startContinuity(entity);
            }
        }
    }

    public void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.isOnGround() && this.continuousComponent.getContinueTime() >= 15.0f) {
            this.continuousComponent.stopContinuity(entity);
        }
        if (this.altModeComponent.isMode(Mode.VENOM)) {
            Vector3d look = entity.getLookAngle().multiply(1.75, entity.getLookAngle().y >= 0.0 ? 0.0 : 0.6, 1.75);
            if (entity.zza < 0.0f) {
                look = look.multiply(-1.0, 1.0, -1.0);
            }
            if (entity.isInWater()) {
                look = look.multiply(0.2, 0.2, 0.2);
            }
            AbilityHelper.setDeltaMovement((Entity)entity, (Vector3d)look);
        } else if (entity.isOnGround() && this.continuousComponent.getContinueTime() >= 5.0f) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    public void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 120.0f);
        this.animationComponent.stop(entity);
        entity.stopRiding();
    }

    private ClonedVenomRoadProjectile createProjectile(LivingEntity entity) {
        boolean isDemonForm = ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity);
        ClonedVenomRoadProjectile projectile = new ClonedVenomRoadProjectile(entity.level, entity, this, isDemonForm);
        projectile.setGravity(0.05f);
        projectile.setMaxLife(500);
        return projectile;
    }

    public void setNormalMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.NORMAL);
    }

    public void setVenomMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.VENOM);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.VENOM) {
            super.setDisplayName(VENOM_NAME);
            super.setDisplayIcon(VENOM_ICON);
        } else if (mode == Mode.NORMAL) {
            super.setDisplayName(NORMAL_NAME);
            super.setDisplayIcon(NORMAL_ICON);
        }
    }

    private static enum Mode {
        NORMAL,
        VENOM;

    }
}

