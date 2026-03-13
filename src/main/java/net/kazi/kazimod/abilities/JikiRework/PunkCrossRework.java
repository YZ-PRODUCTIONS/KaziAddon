//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.JikiRework;

import java.util.ArrayList;
import java.util.List;

import net.kazi.kazimod.entities.projectiles.PunkCrossReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.jiki.JikiHelper;
import xyz.pixelatedw.mineminenomi.abilities.jiki.PunkCrossAbility;
import xyz.pixelatedw.mineminenomi.abilities.sabi.RustSkinAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.jiki.PunkCrossProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class PunkCrossRework extends Ability {
    private static final int REQUIRED_IRON = 50;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "punk_cross", new Pair[]{ImmutablePair.of("Uses %s magnetic items and stuns the target in a cross-shaped structure.", new Object[]{AbilityHelper.mentionText(50)})});
    private static final int COOLDOWN = 400;
    private static final int EFFECT_TIMER = 80;
    public static final AbilityCore<PunkCrossAbility> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private LivingEntity target;
    private boolean hasTarget;
    private Vector3d lastKnownTargetPos;
    private Vector3d lastKnownProjPos;
    private List<ItemStack> magneticItems = new ArrayList();

    public PunkCrossRework(AbilityCore<PunkCrossAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.projectileComponent});
        this.addCanUseCheck(JikiHelper.getMetalicItemsCheck(50));
        this.addUseEvent(this::useEvent);
        this.addTickEvent(this::tickEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 80.0F);
    }

    private void tickEvent(LivingEntity entity, IAbility ability) {
        if (this.hasTarget && (this.target == null || !this.target.isAlive() || !this.target.hasEffect((Effect)ModEffects.PUNK_CROSS.get()))) {
            JikiHelper.dropComponentItems(entity, this.lastKnownTargetPos, this.magneticItems);
            this.target = null;
            this.hasTarget = false;
        }

    }

    private void startContinuityEvent(LivingEntity player, IAbility ability) {
        this.target = null;
        this.hasTarget = false;
        List<ItemStack> inventory = ItemsHelper.getAllInventoryItems(player);
        this.magneticItems = JikiHelper.getMagneticItemsNeeded(inventory, 50.0F);
        this.projectileComponent.shoot(player, 3.0F, 0.0F);
    }

    private void duringContinuityEvent(LivingEntity player, IAbility ability) {
        if (this.target != null) {
            this.lastKnownTargetPos = this.target.position();
        }

        if (!this.hasTarget && this.projectileComponent.hasProjectileAlive()) {
            this.lastKnownProjPos = this.projectileComponent.getShotProjectile().position();
        } else if (!this.hasTarget && !this.projectileComponent.hasProjectileAlive()) {
            JikiHelper.dropComponentItems(player, this.lastKnownProjPos, this.magneticItems);
            this.continuousComponent.stopContinuity(player);
        }

    }

    private void endContinuityEvent(LivingEntity player, IAbility ability) {
        if (this.target != null) {
            this.target.removeEffect((Effect)ModEffects.PUNK_CROSS.get());
        }

        if (this.target != null) {
            JikiHelper.dropComponentItems(player, this.lastKnownTargetPos, this.magneticItems);
        }

        this.hasTarget = false;
        this.target = null;
        this.lastKnownProjPos = null;
        this.cooldownComponent.startCooldown(player, 400.0F);
    }

    private PunkCrossReworkProjectile createProjectile(LivingEntity entity) {
        PunkCrossReworkProjectile proj = new PunkCrossReworkProjectile(entity.level, entity);
        proj.onEntityImpactEvent = (target) -> {
            IAbilityData abilityDataProps = AbilityDataCapability.get(target);
            if (abilityDataProps != null) {
                RustSkinAbility rustSkinAbility = (RustSkinAbility)abilityDataProps.getPassiveAbility(RustSkinAbility.INSTANCE);
                if (rustSkinAbility != null && !rustSkinAbility.isPaused()) {
                    proj.kill();
                    return;
                }
            }

            target.addEffect(new EffectInstance((Effect)ModEffects.PUNK_CROSS.get(), 80, 1));
            JikiHelper.spawnAttractEffect(target);
            this.hasTarget = true;
            this.target = target;
            this.lastKnownTargetPos = target.position();
        };
        proj.onBlockImpactEvent = (hitPos) -> {
            JikiHelper.dropComponentItems(entity, proj.position(), this.magneticItems);
            this.continuousComponent.stopContinuity(entity);
        };
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Punk Cross", AbilityCategory.DEVIL_FRUITS, PunkCrossAbility::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(400.0F), ContinuousComponent.getTooltip(80.0F)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceElement(SourceElement.METAL).setSourceType(new SourceType[]{SourceType.BLUNT}).build();
    }
}
