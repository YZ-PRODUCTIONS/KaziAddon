package net.kazi.kazimod.abilities.BomuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import net.kazi.kazimod.entities.projectiles.ClusterProjectile;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

public class ClusterBombAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "cluster_bomb",
            new Pair[]{ImmutablePair.of("The user rapidly fires a series of small explosive projectiles.", (Object) null)}
    );

    private static final float COOLDOWN = 200.0F;

    public static final AbilityCore<ClusterBombAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(100, this::startContinuityEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(100, this::triggerRepeaterEvent).addStopEvent(100, this::stopRepeaterEvent);

    public ClusterBombAbility(AbilityCore<ClusterBombAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.projectileComponent, this.repeaterComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.repeaterComponent.stop(entity);
        } else {
            this.continuousComponent.startContinuity(entity, -1.0F);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.repeaterComponent.start(entity, 10, 3);
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.0F, 5.0F);
        entity.level.playSound(null, entity.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS,
                1.0F, 1.5F + this.random.nextFloat() / 3.0F);
    }

    private void stopRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private ClusterProjectile createProjectile(LivingEntity entity) {
        return new ClusterProjectile(entity.level, entity, this);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Cluster Bomb", AbilityCategory.DEVIL_FRUITS, ClusterBombAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceElement(SourceElement.EXPLOSION)
                .build();
    }
}