package net.kazi.kazimod.abilities.KameRework;

import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.worldturtle.WorldTurtleEffectEntity;
import net.kazi.kazimod.worldturtle.WorldTurtleBlastDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class DestructionAbility extends Ability {
    public static final int COOLDOWN=1200;
    public static final AbilityCore<DestructionAbility> INSTANCE=new AbilityCore.Builder<>("Destruction",AbilityCategory.DEVIL_FRUITS,DestructionAbility::new)
            .setUnlockCheck(WorldTurtleFormAbility::canUnlock)
            .setSourceHakiNature(SourceHakiNature.IMBUING).setSourceElement(SourceElement.EXPLOSION).setSourceType(SourceType.INDIRECT)
            .setIcon(new ResourceLocation("kazimod","textures/abilities/destruction.png"))
            .addDescriptionLine(new StringTextComponent("Open a dark sky portal above the ground you aim at. Drop a colossal bomb with an expanding 96-block blast and a mushroom cloud."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN),DealDamageComponent.getTooltip(WorldTurtleBlastDamage.MIN_DAMAGE,WorldTurtleBlastDamage.MAX_DAMAGE)).build();
    private final DealDamageComponent damage=new DealDamageComponent(this);
    public DestructionAbility(AbilityCore<DestructionAbility> core){
        super(core);isNew=true;addComponents(damage);
        addCanUseCheck((e,a)->KaziMorphs.WORLD_TURTLE.get().isActive(e)&&target(e)!=null
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Use World Turtle Form and aim at nearby ground.")));
        addUseEvent((e,a)->{if(e.level.isClientSide)return;Vector3d target=target(e);if(target==null)return;
            WorldTurtleEffectEntity.bomb(e,target);cooldownComponent.startCooldown(e,COOLDOWN);
        });
    }
    public static DestructionAbility forCaster(LivingEntity caster){
        IAbility ability=AbilityDataCapability.get(caster).getEquippedAbility(INSTANCE);
        return ability instanceof DestructionAbility?(DestructionAbility)ability:INSTANCE.createAbility();
    }
    public boolean hurtBlast(LivingEntity caster,LivingEntity target,float amount,Entity blast){
        // Preserve ability/Haki attribution, including after the bomb changes owners.
        AbilityDamageSource source=new AbilityDamageSource("ability",caster,INSTANCE){
            @Override public Entity getDirectEntity(){return blast;}
            @Override public Vector3d getSourcePosition(){return blast.position();}
        };
        source.setExplosion();
        return damage.hurtTarget(caster,target,amount,source);
    }
    private static Vector3d target(LivingEntity e){
        Vector3d start=e.getEyePosition(1),end=start.add(e.getLookAngle().scale(144));
        BlockRayTraceResult hit=e.level.clip(new RayTraceContext(start,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,e));
        return hit.getType()==RayTraceResult.Type.BLOCK&&hit.getDirection()==net.minecraft.util.Direction.UP&&e.level.hasChunkAt(hit.getBlockPos())?hit.getLocation():null;
    }
}
