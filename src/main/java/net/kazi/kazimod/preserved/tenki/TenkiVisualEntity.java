package net.kazi.kazimod.preserved.tenki;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.kazi.kazimod.abilities.Tenki.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

public final class TenkiVisualEntity extends Entity {
    private static final DataParameter<Integer> SOURCE=EntityDataManager.defineId(TenkiVisualEntity.class,DataSerializers.INT),KIND=EntityDataManager.defineId(TenkiVisualEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> RADIUS=EntityDataManager.defineId(TenkiVisualEntity.class,DataSerializers.FLOAT);
    private static final DataParameter<Boolean> FOLLOW_SOURCE=EntityDataManager.defineId(TenkiVisualEntity.class,DataSerializers.BOOLEAN);
    private Entity source;private int life;
    public TenkiVisualEntity(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(SOURCE,-1);entityData.define(KIND,0);entityData.define(RADIUS,1F);entityData.define(FOLLOW_SOURCE,false);}
    public int kind(){return entityData.get(KIND);} public float radius(){return entityData.get(RADIUS);}
    public Entity source(){return level.isClientSide?level.getEntity(entityData.get(SOURCE)):source;}
    public static TenkiVisualEntity spawn(Entity source,int kind,float radius,int life){
        return spawnAt(source,kind,radius,life,source.position());
    }
    public static TenkiVisualEntity spawnAt(Entity source,int kind,float radius,int life,Vector3d position){
        TenkiVisualEntity e=new TenkiVisualEntity(TenkiEffects.EFFECT.get(),source.level);e.source=source;e.life=life;
        e.entityData.set(SOURCE,source.getId());e.entityData.set(KIND,kind);e.entityData.set(RADIUS,radius);
        e.entityData.set(FOLLOW_SOURCE,life==0);
        e.setPos(position.x,position.y,position.z);source.level.addFreshEntity(e);return e;
    }
    public void tick(){super.tick();Entity parent=source();
        if(!level.isClientSide&&(parent==null||!parent.isAlive()||(life>0&&tickCount>=life)||tickCount>24000)){remove();return;}
        if(!level.isClientSide&&kind()==0&&parent instanceof LivingEntity){
            LivingEntity caster=(LivingEntity)parent;
            if(!active(caster,CloudyDayAbility.INSTANCE)){remove();return;}
            setInvisible(active(caster,ThunderstormAbility.INSTANCE)||active(caster,GaleStormAbility.INSTANCE));
        }
        // Lifetime is server-only; attachment must be synchronized for client-side positioning.
        if(parent!=null&&entityData.get(FOLLOW_SOURCE))setPos(parent.getX(),parent.getY(),parent.getZ());
    }
    private static boolean active(LivingEntity caster,AbilityCore<?> core){
        IAbility ability=AbilityDataCapability.get(caster).getEquippedOrPassiveAbility(core);
        return ability!=null&&ability.getComponent(ModAbilityKeys.CONTINUOUS).map(ContinuousComponent::isContinuous).orElse(false);
    }
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(radius()+12,65,radius()+12);}
    public boolean shouldRenderAtSqrDistance(double d){return d<384*384;}
    protected void readAdditionalSaveData(CompoundNBT n){remove();} protected void addAdditionalSaveData(CompoundNBT n){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
