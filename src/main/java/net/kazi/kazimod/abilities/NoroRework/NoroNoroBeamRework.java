//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NoroRework;

import net.kazi.kazimod.entities.projectiles.NoroNoroBeamReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.noro.NoroNoroBeamProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class NoroNoroBeamRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "noro_noro_beam", new Pair[]{ImmutablePair.of("Shoots a beam of photons at the opponent, completely slowing them down (multiple hits stack the Slowness effect)", (Object)null)});
    private static final float COOLDOWN = 60.0F;
    private static final int ANIMATION_TICK = 10;
    public static final AbilityCore<NoroNoroBeamRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public NoroNoroBeamRework(AbilityCore<NoroNoroBeamRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.animationComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.AIM_SNIPER, 10);
        this.projectileComponent.shoot(entity, 4.0F, 1.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.NORO_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 60.0F);
    }

    private NoroNoroBeamReworkProjectile createProjectile(LivingEntity entity) {
        NoroNoroBeamReworkProjectile proj = new NoroNoroBeamReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Noro Noro Beam", AbilityCategory.DEVIL_FRUITS, NoroNoroBeamRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(60.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).build();
    }
}
