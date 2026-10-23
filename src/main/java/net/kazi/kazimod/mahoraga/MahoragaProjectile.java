package net.kazi.kazimod.mahoraga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class MahoragaProjectile extends ThrowableEntity {
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(MahoragaProjectile.class,DataSerializers.INT);
    private static final DataParameter<Integer> BLOCK=EntityDataManager.defineId(MahoragaProjectile.class,DataSerializers.INT);
    private static final DataParameter<Boolean> HIT=EntityDataManager.defineId(MahoragaProjectile.class,DataSerializers.BOOLEAN);
    private int impactTicks;
    private UUID guardianId;
    public MahoragaProjectile(EntityType<? extends MahoragaProjectile> type,World world) {super(type,world);}
    @Override protected void defineSynchedData() {entityData.define(KIND,0);entityData.define(BLOCK,Block.getId(Blocks.STONE.defaultBlockState()));entityData.define(HIT,false);}
    public void configure(int kind,BlockState block) {entityData.set(KIND,kind);entityData.set(BLOCK,Block.getId(block.isAir()?Blocks.STONE.defaultBlockState():block));}
    public int kind() {return entityData.get(KIND);}
    public BlockState block() {return Block.stateById(entityData.get(BLOCK));}
    public boolean impacted() {return entityData.get(HIT);}
    public void setGuardian(MahoragaEntity guardian) {guardianId=guardian.getUUID();setOwner(guardian);}
    public int impactTicks() {return impactTicks;}
    @Override public void tick() {
        baseTick();
        if(impacted()) {setDeltaMovement(Vector3d.ZERO);if(++impactTicks>8 && !level.isClientSide)remove();return;}
        if(!level.isClientSide && (getOwner()==null || !getOwner().isAlive() || tickCount>70 || !level.hasChunkAt(blockPosition()))) {remove();return;}
        if(!level.isClientSide) {
            Entity origin=guardianId==null?null:((ServerWorld)level).getEntity(guardianId);
            if(!(origin instanceof MahoragaEntity) || !origin.isAlive()){remove();return;}
            MahoragaEntity source=(MahoragaEntity)origin;
            if(source.isDismissing()){remove();return;}
            if(source.owner()==null || distanceToSqr(source.owner())>3600) {remove();return;}
            boolean reflected=getOwner()!=source;
            RayTraceResult result=ProjectileHelper.getHitResult(this,e->e instanceof LivingEntity && e.isAlive()
                    && (reflected?e!=getOwner() && !(getOwner() instanceof LivingEntity && MahoragaFeatures.allied((LivingEntity)getOwner(),e))
                    :source.validEnemy((LivingEntity)e)));
            if(result.getType()!=RayTraceResult.Type.MISS) {
                if(net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,result))return;
                setPos(result.getLocation().x,result.getLocation().y,result.getLocation().z);
                if(result instanceof EntityRayTraceResult) {
                    LivingEntity target=(LivingEntity)((EntityRayTraceResult)result).getEntity();
                    ModDamageSource damage=new ModIndirectEntityDamageSource(kind()==0?"mahoraga_rock":"mahoraga_air_blast",this,getOwner());
                    damage.setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.PROJECTILE,SourceType.BLUNT)));
                    if(kind()==1)damage.setSourceElement(SourceElement.AIR);
                    damage.setProjectile();
                    float amount=kind()==0?38:32;
                    if(!reflected)amount=MahoragaHaki.infuse(source,damage,amount);
                    if(target.hurt(damage,amount)) {
                        Vector3d dir=getDeltaMovement().normalize();AbilityHelper.setDeltaMovement(target,dir.x*1.5,Math.max(.25,dir.y*.5),dir.z*1.5);
                    }
                }
                entityData.set(HIT,true);setDeltaMovement(Vector3d.ZERO);
                playSound(kind()==0?SoundEvents.STONE_BREAK:SoundEvents.GENERIC_EXPLODE,1.5F,.85F);return;
            }
        }
        Vector3d motion=getDeltaMovement();setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
        if(kind()==0)setDeltaMovement(motion.scale(.995).add(0,-.045,0));
    }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) {super.addAdditionalSaveData(nbt);nbt.putInt("Kind",kind());nbt.putInt("Block",entityData.get(BLOCK));if(guardianId!=null)nbt.putUUID("Guardian",guardianId);}
    @Override protected void readAdditionalSaveData(CompoundNBT nbt) {super.readAdditionalSaveData(nbt);entityData.set(KIND,nbt.getInt("Kind"));entityData.set(BLOCK,nbt.getInt("Block"));guardianId=nbt.hasUUID("Guardian")?nbt.getUUID("Guardian"):null;}
    @Override public AxisAlignedBB getBoundingBoxForCulling() {return getBoundingBox().inflate(5);}
    @Override public IPacket<?> getAddEntityPacket() {return NetworkHooks.getEntitySpawningPacket(this);}
}
