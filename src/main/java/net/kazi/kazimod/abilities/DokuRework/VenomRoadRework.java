//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.DokuRework;

import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.entities.projectiles.VenomRoadReworkProjectile;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class VenomRoadRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "venom_road", new Pair[]{ImmutablePair.of("Fires a Hydra at the target location that stays there for a few seconds during which time the user can use them to move along their path.", (Object)null)});
    private static final ITextComponent NORMAL_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.venom_road", "Venom Road"));
    private static final ResourceLocation NORMAL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/venom_road.png");
    public static final int COOLDOWN = 240;
    public static final AbilityCore<VenomRoadRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private boolean isMovingOwner;
    private int firstTick;
    private int ticks;
    private List<Pair<Vector3d, VenomRoadReworkProjectile>> projectiles;
    private VenomRoadReworkProjectile projectileUsed;

    public VenomRoadRework(AbilityCore<VenomRoadRework> core) {
        super(core);
        this.animationComponent = new AnimationComponent(this);
        this.projectiles = new ArrayList();
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.animationComponent, this.continuousComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            if (AbilityHelper.canUseMomentumAbilities(entity)) {
                this.isMovingOwner = false;
                VenomRoadReworkProjectile projectile = (VenomRoadReworkProjectile)this.projectileComponent.getNewProjectile(entity);
                entity.level.addFreshEntity(projectile);
                projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 3.0F, 1.0F);
                Pair pair = ImmutablePair.of(entity.position(), projectile);
                this.projectiles.add(pair);
                if (projectile != null) {
                    entity.startRiding(projectile);
                }

                this.animationComponent.start(entity, ModAnimations.SHOOT_SELF_FORWARD);
                this.continuousComponent.startContinuity(entity);
            }
        }
    }

    public void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.isOnGround() && this.continuousComponent.getContinueTime() >= 15.0F) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (entity.isOnGround() && this.continuousComponent.getContinueTime() >= 5.0F) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    public void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 240.0F);
        this.animationComponent.stop(entity);
        entity.stopRiding();
    }

    private VenomRoadReworkProjectile createProjectile(LivingEntity entity) {
        VenomRoadReworkProjectile projectile = new VenomRoadReworkProjectile(entity.level, entity, this);
        projectile.setGravity(0.05F);
        projectile.setMaxLife(500);
        return projectile;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Venom Road", AbilityCategory.DEVIL_FRUITS, VenomRoadRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.POISON).build();
    }
}

