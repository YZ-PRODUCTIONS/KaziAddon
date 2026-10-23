package net.kazi.kazimod.seiryu;

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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Living targets let both vanilla and MMNM attacks hit the body, sharing the player's health. */
public final class SeiryuPartEntity extends LivingEntity {
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(SeiryuPartEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> INDEX=EntityDataManager.defineId(SeiryuPartEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> SCALE=EntityDataManager.defineId(SeiryuPartEntity.class,DataSerializers.FLOAT);
    private boolean hittable;
    public SeiryuPartEntity(EntityType<? extends LivingEntity> type,World world) {
        super(type,world);noPhysics=true;setNoGravity(true);setSilent(true);
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(OWNER,-1);entityData.define(INDEX,0);entityData.define(SCALE,1F);}
    public void bind(LivingEntity owner,int index){entityData.set(OWNER,owner.getId());entityData.set(INDEX,index);entityData.set(SCALE,(float)SeiryuFlight.scale(owner));}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity&&e!=this?(LivingEntity)e:null;}
    public boolean matchesForm(){LivingEntity owner=owner();return !removed&&SeiryuFlight.active(owner)&&entityData.get(SCALE)==SeiryuFlight.scale(owner);}
    public void updateBounds() {
        LivingEntity owner=owner();hittable=matchesForm();
        if(!hittable){setBoundingBox(new AxisAlignedBB(position(),position()));return;}
        Vector3d[] points=level.isClientSide?SeiryuClient.points(owner,1):SeiryuFlight.points(owner);
        if(points==null){hittable=false;return;}
        AxisAlignedBB box=SeiryuSpine.bounds(points,Math.max(0,Math.min(SeiryuSpine.SEGMENTS-1,entityData.get(INDEX))),entityData.get(SCALE));
        level.increaseMaxEntityRadius(Math.max(box.getYsize(),Math.max(box.getXsize(),box.getZsize())*.5));
        xo=getX();yo=getY();zo=getZ();
        setPos((box.minX+box.maxX)*.5,box.minY,(box.minZ+box.maxZ)*.5);setBoundingBox(box);
    }
    @Override public void tick() {
        // No AI, block scans, gravity or separate health simulation for body sections.
        if(!level.isClientSide&&!matchesForm()) {remove();return;}
        updateBounds();
    }
    @Override public boolean isPickable(){return hittable&&matchesForm()&&(!level.isClientSide||!SeiryuClient.localOwner(owner()));}
    @Override public boolean canBeCollidedWith(){return hittable&&matchesForm();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurt(DamageSource source,float amount) {
        LivingEntity owner=owner();
        if(!hittable||!matchesForm()||source.getEntity()==owner||source.getDirectEntity()==owner)return false;
        return owner.hurt(source,amount);
    }
    @Override public boolean addEffect(EffectInstance effect){LivingEntity owner=owner();return matchesForm()&&owner.addEffect(effect);}
    @Override public void knockback(float power,double x,double z){LivingEntity owner=owner();if(matchesForm())owner.knockback(power,x,z);}
    @Override public boolean isAlliedTo(Entity other){LivingEntity owner=owner();return owner!=null&&(owner==other||owner.isAlliedTo(other));}
    @Override public net.minecraft.scoreboard.Team getTeam(){LivingEntity owner=owner();return owner==null?null:owner.getTeam();}
    @Override public Iterable<ItemStack> getArmorSlots(){return Collections.emptyList();}
    @Override public ItemStack getItemBySlot(EquipmentSlotType slot){return ItemStack.EMPTY;}
    @Override public void setItemSlot(EquipmentSlotType slot,ItemStack stack){}
    @Override public HandSide getMainArm(){return HandSide.RIGHT;}
    @Override public void readAdditionalSaveData(CompoundNBT tag){remove();}
    @Override public void addAdditionalSaveData(CompoundNBT tag){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
