//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HoroRework;

import net.kazi.kazimod.entities.projectiles.MiniHollowReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.horo.MiniHollowProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class MiniHollowRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "mini_hollow", new Pair[]{ImmutablePair.of("Launches small ghosts at the opponent, exploding upon impact.", (Object)null)});
    private static final int COOLDOWN = 180;
    public static final AbilityCore<MiniHollowRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::triggerRepeaterEvent).addStopEvent(this::endRepeaterEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public MiniHollowRework(AbilityCore<MiniHollowRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.repeaterComponent, this.projectileComponent, this.animationComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.repeaterComponent.start(entity, 4, 5);
        this.animationComponent.start(entity, ModAnimations.AIM_SNIPER);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.0F, 3.0F);
    }

    private void endRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
    }

    private MiniHollowReworkProjectile createProjectile(LivingEntity entity) {
        MiniHollowReworkProjectile proj = new MiniHollowReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Mini Hollow", AbilityCategory.DEVIL_FRUITS, MiniHollowRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(180.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    }
}
