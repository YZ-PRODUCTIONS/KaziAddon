//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NikyuRework;

import net.kazi.kazimod.entities.projectiles.PadHoReworkProjectile;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.PadHoProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class PadHoRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "pad_ho", new Pair[]{ImmutablePair.of("Launches a paw-shaped shockwave at the opponent", (Object)null)});
    private static final int COOLDOWN = 30;
    public static final AbilityCore<PadHoRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public PadHoRework(AbilityCore<PadHoRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.animationComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.AIM_SNIPER, 7);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PAD_HO_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.projectileComponent.shoot(entity, 3.5F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 30.0F);
    }

    private PadHoReworkProjectile createProjectile(LivingEntity entity) {
        PadHoReworkProjectile proj = new PadHoReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Pad Ho", AbilityCategory.DEVIL_FRUITS, PadHoRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(30.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.SHOCKWAVE).build();
    }
}
