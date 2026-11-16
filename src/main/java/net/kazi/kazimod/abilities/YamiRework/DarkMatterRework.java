package net.kazi.kazimod.abilities.YamiRework;

import net.kazi.kazimod.entities.projectiles.DarkMatterReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DarkMatterRework extends Ability {
    private static final int COOLDOWN=280, CHARGE_TIME=80;
    // Cart reads this core during construction, before description registries exist.
    public static final AbilityCore<DarkMatterRework> INSTANCE = new AbilityCore.Builder<DarkMatterRework>(
            "Dark Matter Rework", AbilityCategory.DEVIL_FRUITS, DarkMatterRework::new)
            .addDescriptionLine(new StringTextComponent("Launches an overhead darkness sphere. On impact it devours enemies, projectiles and nearby terrain for five seconds."))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(COOLDOWN), ChargeComponent.getTooltip(CHARGE_TIME))
            .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
            .setIcon(new ResourceLocation("mineminenomi","textures/abilities/dark_matter.png"))
            .setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.GRAVITY).build();
    private final ChargeComponent charge = new ChargeComponent(this).addStartEvent(this::start)
            .addTickEvent(this::charging).addEndEvent(this::release);
    private final ProjectileComponent projectiles = new ProjectileComponent(this,
            owner -> new DarkMatterReworkProjectile(owner.level,owner));
    private final AnimationComponent animation = new AnimationComponent(this);
    private DarkMatterReworkProjectile sphere;

    public DarkMatterRework(AbilityCore<DarkMatterRework> core) {
        super(core);isNew=true;addComponents(charge,projectiles,animation);
        addUseEvent((owner,ability)->charge.startCharging(owner,CHARGE_TIME));
    }
    private void start(LivingEntity owner,IAbility ability) {
        if(sphere!=null&&sphere.isAlive())sphere.remove();
        sphere=null;animation.start(owner,ModAnimations.RAISE_RIGHT_ARM);
    }
    private void charging(LivingEntity owner,IAbility ability) {
        if(owner.level.isClientSide)return;
        if(sphere==null) {
            sphere=(DarkMatterReworkProjectile)projectiles.getNewProjectile(owner);
            sphere.hold(owner,charge.getChargePercentage());
            // The initial spawn packet must already contain the overhead position.
            owner.level.addFreshEntity(sphere);
        } else if(!sphere.isAlive()) {
            charge.stopCharging(owner);return;
        }
        sphere.hold(owner,charge.getChargePercentage());
    }
    private void release(LivingEntity owner,IAbility ability) {
        animation.stop(owner);
        if(!owner.level.isClientSide&&sphere!=null&&sphere.isAlive()) {
            if(charge.getChargePercentage()>=.99F) {
                sphere.hold(owner,1);
                sphere.launch(WyHelper.rayTraceBlocksAndEntities(owner,128).getLocation());
            } else sphere.remove();
        }
        sphere=null;cooldownComponent.startCooldown(owner,COOLDOWN);
    }
}
