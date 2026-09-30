/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.util.text.ITextComponent
 *  org.apache.commons.lang3.tuple.ImmutablePair
 *  org.apache.commons.lang3.tuple.Pair
 *  xyz.pixelatedw.mineminenomi.api.abilities.Ability
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore$Builder
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine$IDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.IAbility
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent
 *  xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature
 *  xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper
 */
package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.entities.projectiles.susu.GokuroProjectile;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

public class GokuroRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"cartaddon", (String)"gokuro", (Pair[])new Pair[]{ImmutablePair.of((Object)"Fires a cloud of soot at the target suffocating and weakening them.", null)});
    private static final float COOLDOWN = 600.0f;
    public static final AbilityCore<GokuroRework> INSTANCE = new AbilityCore.Builder("Gokuro", AbilityCategory.DEVIL_FRUITS, GokuroRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float)600.0f)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);

    public GokuroRework(AbilityCore<GokuroRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.projectileComponent});
        this.addUseEvent(this::onUseEvent);
    }

    public void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
        this.projectileComponent.shoot(entity, 4.0f, 1.0f);
        this.continuousComponent.stopContinuity(entity);
        this.cooldownComponent.startCooldown(entity, 600.0f);
    }

    public GokuroProjectile createProjectile(LivingEntity entity) {
        GokuroProjectile proj = new GokuroProjectile(entity.level, entity);
        return proj;
    }
}



