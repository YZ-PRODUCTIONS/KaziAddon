package net.kazi.kazimod.jerome;

import java.awt.Color;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

/** Native beam collision, synchronization and projectile clashes, with no terrain destruction. */
public final class JeromeBeam extends LightningEntity {
    private float unobstructed=72;
    public JeromeBeam(JeromeEntity owner){
        super(owner,owner.handOrigin().x,owner.handOrigin().y,owner.handOrigin().z,owner.yRot,owner.xRot,72,18,null);
        ModDamageSource source=owner.source("energy_beam",true).setProjectile().setAreaOfEffect();
        setDamage(owner.infused(source,14));setDamageSource(source);setMaxLife(44);setSize(.3F+.12F*owner.rage());setBoxSizeDivision(1);
        setSegments(1);setBranches(1);setLightningMimic(false);setLightningMovement(false);setEnergyEffect(false);
        setColor(new Color(0xffc95e));setExplosion(0,false);setTargetTimeToReset(Integer.MAX_VALUE);setDeltaMovement(Vector3d.ZERO);
    }
    @Override public void tick(){
        if(!level.isClientSide){
            LivingEntity owner=getThrower();if(owner==null||!owner.isAlive()){remove();return;}
            Vector3d start=getEyePosition(1),end=start.add(getLookAngle().scale(72));
            unobstructed=(float)start.distanceTo(level.clip(new RayTraceContext(start,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getLocation());
            if(tickCount%20==0){targets.clear();explosionTargets.clear();clearTargets();}
        }
        super.tick();
    }
    @Override public float getLength(){return Math.min(super.getLength(),unobstructed);}
    @Override public void onEntityImpactEvent(Entity entity){
        LivingEntity owner=getThrower();
        if(!(entity instanceof LivingEntity)||owner==null||entity==owner||owner.isAlliedTo(entity)||!entity.isAlive())return;
        if(owner instanceof JeromeEntity&&!((JeromeEntity)owner).enemy((LivingEntity)entity))return;
        if(!targets.contains(entity.getId())){targets.add(entity.getId());entity.hurt(getDamageSource(),getDamage());}
    }
    @Override public void onBlockImpact(BlockPos position){}
    @Override public void onFirstImpact(BlockPos position){}
}
