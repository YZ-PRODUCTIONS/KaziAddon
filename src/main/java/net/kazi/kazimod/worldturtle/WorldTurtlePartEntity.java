package net.kazi.kazimod.worldturtle;

import java.util.Collections;
import net.minecraft.entity.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Lightweight living target for MMNM attacks; no AI, physics, drops or independent health. */
public final class WorldTurtlePartEntity extends LivingEntity {
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(WorldTurtlePartEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> PART=EntityDataManager.defineId(WorldTurtlePartEntity.class,DataSerializers.INT);
    private boolean hittable;
    public WorldTurtlePartEntity(EntityType<? extends LivingEntity> type,World world){super(type,world);noPhysics=true;setNoGravity(true);setSilent(true);}
    protected void defineSynchedData(){super.defineSynchedData();entityData.define(OWNER,-1);entityData.define(PART,0);}
    public void bind(LivingEntity owner,int part){entityData.set(OWNER,owner.getId());entityData.set(PART,part);}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity&&e!=this?(LivingEntity)e:null;}
    public boolean isShell(){return entityData.get(PART)==0;}
    public void updateBounds(){
        LivingEntity owner=owner();hittable=WorldTurtleHitboxes.active(owner);
        if(!hittable)return;
        AxisAlignedBB box=WorldTurtleHitboxes.bounds(owner,Math.min(WorldTurtleAnatomy.BONES.length-1,Math.max(0,entityData.get(PART))));
        hittable=box!=null;if(box==null)box=new AxisAlignedBB(owner.position(),owner.position());
        // Register the actual extents with Forge's broad phase so a hit near a
        // chunk/section boundary can still find a part whose center is elsewhere.
        level.increaseMaxEntityRadius(Math.max(box.getYsize(),Math.max(box.getXsize(),box.getZsize())*.5));
        setPos((box.minX+box.maxX)*.5,box.minY,(box.minZ+box.maxZ)*.5);setBoundingBox(box);
    }
    @Override public void tick(){
        // Intentionally do not call LivingEntity.tick: these parts never scan blocks or run mob logic.
        if(!level.isClientSide&&!WorldTurtleHitboxes.active(owner())){remove();return;}
        updateBounds();
    }
    @Override public boolean isPickable(){
        return hittable&&(!level.isClientSide||!WorldTurtleClient.isLocalOwner(owner()));
    }
    @Override public boolean canBeCollidedWith(){return hittable;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurt(DamageSource source,float amount){
        LivingEntity owner=owner();if(!hittable||!WorldTurtleHitboxes.active(owner)||source.getEntity()==owner||source.getDirectEntity()==owner)return false;
        if(isShell()&&!net.kazi.kazimod.abilities.KameRework.DivineShieldAbility.active(owner))return false;
        // All parts share the owner's armor, hurt cooldown, shield and damage reduction.
        return owner.hurt(source,amount);
    }
    @Override public boolean addEffect(EffectInstance effect){LivingEntity owner=owner();return !isShell()&&owner!=null&&owner.addEffect(effect);}
    @Override public void knockback(float power,double x,double z){LivingEntity owner=owner();if(!isShell()&&owner!=null)owner.knockback(power,x,z);}
    @Override public boolean isAlliedTo(Entity other){LivingEntity owner=owner();return owner!=null&&(other==owner||owner.isAlliedTo(other));}
    @Override public net.minecraft.scoreboard.Team getTeam(){LivingEntity owner=owner();return owner==null?null:owner.getTeam();}
    @Override public Iterable<ItemStack> getArmorSlots(){return Collections.emptyList();}
    @Override public ItemStack getItemBySlot(EquipmentSlotType slot){return ItemStack.EMPTY;}
    @Override public void setItemSlot(EquipmentSlotType slot,ItemStack stack){}
    @Override public HandSide getMainArm(){return HandSide.RIGHT;}
    @Override public void readAdditionalSaveData(CompoundNBT n){remove();}
    @Override public void addAdditionalSaveData(CompoundNBT n){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
