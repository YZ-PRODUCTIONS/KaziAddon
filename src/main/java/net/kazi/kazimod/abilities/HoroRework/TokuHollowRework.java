//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HoroRework;

import net.kazi.kazimod.entities.projectiles.TokuHollowReworkProjectile;
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
import xyz.pixelatedw.mineminenomi.entities.projectiles.horo.TokuHollowProjectile;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TokuHollowRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "toku_hollow", new Pair[]{ImmutablePair.of("Creates a huge ghost that causes a massive explosion upon impact.", (Object)null)});
    private static final int COOLDOWN = 300;
    public static final AbilityCore<TokuHollowRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public TokuHollowRework(AbilityCore<TokuHollowRework> core) {
        super(core);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.projectileComponent});
        super.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity);
        super.cooldownComponent.startCooldown(entity, WyHelper.secondsToTicks(15.0F));
    }

    private TokuHollowReworkProjectile createProjectile(LivingEntity entity) {
        TokuHollowReworkProjectile proj = new TokuHollowReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Toku Hollow", AbilityCategory.DEVIL_FRUITS, TokuHollowRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.IMBUING).build();
    }
}