//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HoroRework;

import net.kazi.kazimod.entities.projectiles.NegativeHollowReworkProjectile;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.horo.NegativeHollowProjectile;

public class NegativeHollowRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "negative_hollow", new Pair[]{ImmutablePair.of("The user launches a ghost that drains the target's will, debuffing them.", (Object)null)});
    private static final int COOLDOWN = 80;
    public static final AbilityCore<NegativeHollowRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public NegativeHollowRework(AbilityCore<NegativeHollowRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.0F, 0.5F);
        this.cooldownComponent.startCooldown(entity, 80.0F);
    }

    private NegativeHollowReworkProjectile createProjectile(LivingEntity entity) {
        NegativeHollowReworkProjectile proj = new NegativeHollowReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Negative Hollow", AbilityCategory.DEVIL_FRUITS, NegativeHollowRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(80.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    }
}
