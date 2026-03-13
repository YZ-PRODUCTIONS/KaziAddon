//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.JikiRework;

import java.util.List;

import net.kazi.kazimod.entities.projectiles.GenocideRaidReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.jiki.JikiHelper;
import xyz.pixelatedw.mineminenomi.abilities.sabi.RustSkinAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.jiki.GenocideRaidEffectEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.jiki.GenocideRaidProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class GenocideRaidRework extends Ability {
    public static final int REQUIRED_IRON = 20;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "genocide_raid", new Pair[]{ImmutablePair.of("Uses %s magnetic items and sends them towards the target, spiriling around it and dealing damage over time.", new Object[]{AbilityHelper.mentionText(20)})});
    private static final int COOLDOWN = 400;
    public static final int EFFECT_TIMER = 100;
    public static final AbilityCore<GenocideRaidRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public GenocideRaidRework(AbilityCore<GenocideRaidRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addCanUseCheck(JikiHelper.getMetalicItemsCheck(20));
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        JikiHelper.spawnAttractEffect(entity);
        this.projectileComponent.shoot(entity, 3.0F, 0.0F);
        this.cooldownComponent.startCooldown(entity, 400.0F);
    }

    private GenocideRaidReworkProjectile createProjectile(LivingEntity entity) {
        List<ItemStack> inventory = ItemsHelper.getAllInventoryItems(entity);
        List<ItemStack> magneticItems = JikiHelper.getMagneticItemsNeeded(inventory, 20.0F);
        GenocideRaidReworkProjectile proj = new GenocideRaidReworkProjectile(entity.level, entity);
        proj.onEntityImpactEvent = (target) -> {
            IAbilityData abilityDataProps = AbilityDataCapability.get(target);
            if (abilityDataProps != null) {
                RustSkinAbility rustSkinAbility = (RustSkinAbility)abilityDataProps.getPassiveAbility(RustSkinAbility.INSTANCE);
                if (rustSkinAbility != null && !rustSkinAbility.isPaused()) {
                    proj.kill();
                    return;
                }
            }

            target.addEffect(new EffectInstance((Effect)ModEffects.GENOCIDE_RAID.get(), 100, 0));
            JikiHelper.spawnAttractEffect(target);
            GenocideRaidEffectEntity effect = new GenocideRaidEffectEntity(entity.level);
            effect.moveTo(target.getX(), target.getY() + (double)1.0F, target.getZ(), 0.0F, 0.0F);
            effect.setTarget(target);
            effect.setOwner(entity);
            effect.setItemsUsed(magneticItems);
            entity.level.addFreshEntity(effect);
        };
        proj.onBlockImpactEvent = (hitPos) -> {
            GenocideRaidEffectEntity effect = new GenocideRaidEffectEntity(entity.level);
            effect.moveTo((double)hitPos.getX(), (double)(hitPos.getY() + 1), (double)hitPos.getZ(), 0.0F, 0.0F);
            effect.setTarget((LivingEntity)null);
            effect.setOwner(entity);
            effect.setItemsUsed(magneticItems);
            entity.level.addFreshEntity(effect);
        };
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Genocide Raid", AbilityCategory.DEVIL_FRUITS, GenocideRaidRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(400.0F)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceElement(SourceElement.METAL).setSourceType(new SourceType[]{SourceType.BLUNT}).build();
    }
}
