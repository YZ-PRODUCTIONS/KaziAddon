package net.kazi.kazimod.entities.projectiles;

import java.util.*;
import net.kazi.kazimod.abilities.YamiRework.DarkMatterRework;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.mahoraga.MahoragaFeatures;
import net.kazi.kazimod.yami.YamiVisualPacket;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** One sphere for the entire cast: held, flying, then a finite anchored gravity well. */
public class DarkMatterReworkProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    public static final int HELD=0, FLYING=1, VORTEX=2;
    public static final int VORTEX_TICKS=100;
    public static final double PULL_RADIUS=24;
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(DarkMatterReworkProjectile.class,DataSerializers.FLOAT);
    private static final DataParameter<Integer> STATE=EntityDataManager.defineId(DarkMatterReworkProjectile.class,DataSerializers.INT);
    private static final List<BlockPos> TERRAIN=terrainOffsets();
    private int refreshed,remaining=200,terrainCursor;

    public DarkMatterReworkProjectile(EntityType<?> type,World world) {super((EntityType)type,world);configure();}
    public DarkMatterReworkProjectile(World world,LivingEntity owner) {
        super((EntityType)KaziEntities.DARK_MATTER_PROJECTILE.get(),world,owner,DarkMatterRework.INSTANCE);configure();
    }
    private void configure() {
        setDamage(10);setMaxLife(220);setNoGravity(true);
        setCollideWithEntities(false);setCollideWithBlocks(false);
        setEntityCollisionSize(0);setBlockCollisionSize(0);
        onTickEvent=this::updateVortex;
    }
    public int state(){return entityData.get(STATE);}
    public float radius(){return getSize()*.5F;}
    // The empty collision box must not collapse Minecraft's visual render distance.
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<128.0D*128.0D;}
    public void hold(LivingEntity owner,float progress) {
        if(state()!=HELD)return;
        setSize(2+6*MathHelper.clamp(progress,0,1));refreshed=tickCount;
        setPos(owner.getX(),owner.getEyeY()+radius()*2.1+1,owner.getZ());
        setDeltaMovement(Vector3d.ZERO);setLife(220);
    }
    public void launch(Vector3d target) {
        if(state()!=HELD)return;
        entityData.set(STATE,FLYING);remaining=200;
        Vector3d direction=target.subtract(position()).normalize();
        if(direction.lengthSqr()<.001)direction=new Vector3d(0,-1,0);
        setDeltaMovement(direction.scale(2));hasImpulse=true;
    }
    public void anchor(Vector3d position) {
        if(state()!=FLYING)return;
        setPos(position.x,position.y,position.z);setDeltaMovement(Vector3d.ZERO);
        entityData.set(STATE,VORTEX);remaining=VORTEX_TICKS;terrainCursor=0;hasImpulse=true;
    }
    @Override public void tick() {
        if(!level.isClientSide) {
            LivingEntity owner=getThrower();
            if(owner==null||!owner.isAlive()||owner.level!=level){remove();return;}
            if(state()==HELD&&tickCount-refreshed>5){remove();return;}
            // Sweep before movement: fast shots cannot skip thin walls or the ground.
            if(state()==FLYING) {
                BlockRayTraceResult hit=level.clip(new RayTraceContext(position(),position().add(getDeltaMovement()),
                        RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this));
                if(hit.getType()==RayTraceResult.Type.BLOCK) {
                    Vector3d normal=Vector3d.atLowerCornerOf(hit.getDirection().getNormal());
                    anchor(hit.getLocation().add(normal.scale(radius()*.65+.1)));
                } else {
                    AxisAlignedBB swept=getBoundingBox().expandTowards(getDeltaMovement()).inflate(radius()*.5);
                    for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,swept,this::enemy)) {
                        if(target.getBoundingBox().inflate(radius()*.5).clip(position(),position().add(getDeltaMovement())).isPresent()
                                ||target.getBoundingBox().inflate(radius()*.5).contains(position())) {
                            anchor(target.position().add(0,target.getBbHeight()*.5,0));break;
                        }
                    }
                }
            }
            setLife(220);
        }
        if(state()==VORTEX)setDeltaMovement(Vector3d.ZERO);
        super.tick();
    }
    private boolean enemy(LivingEntity target) {
        LivingEntity owner=getThrower();
        return owner!=null&&target.isAlive()&&!target.isSpectator()
                &&!(target instanceof PlayerEntity&&((PlayerEntity)target).isCreative())
                &&!MahoragaFeatures.allied(owner,target)&&ModEntityPredicates.getEnemyFactions(owner).test(target);
    }
    public boolean absorbs(ProjectileEntity projectile) {
        Entity caster=projectile.getOwner();LivingEntity owner=getThrower();
        return isAlive()&&projectile!=this&&!(projectile instanceof DarkMatterReworkProjectile)
                &&owner!=null&&(caster==null||!MahoragaFeatures.allied(owner,caster));
    }
    public boolean absorbContact(AbilityProjectileEntity other) {
        if(absorbs(other))other.remove();
        return true;
    }
    private void updateVortex() {
        if(level.isClientSide||!isAlive()||getThrower()==null)return;
        double reach=state()==HELD?radius()+5:PULL_RADIUS;
        AxisAlignedBB bounds=new AxisAlignedBB(position(),position()).inflate(reach);
        for(ProjectileEntity projectile:level.getEntitiesOfClass(ProjectileEntity.class,bounds,this::absorbs)) {
            Vector3d offset=position().subtract(projectile.position());double distance=offset.length();
            if(distance>reach)continue;
            AxisAlignedBB core=new AxisAlignedBB(position(),position()).inflate(radius()*.85);
            if(distance<radius()||core.clip(projectile.position(),projectile.position().add(projectile.getDeltaMovement())).isPresent()) {
                projectile.remove();continue;
            }
            Vector3d velocity=projectile.getDeltaMovement().scale(.65).add(offset.normalize().scale(.65));
            projectile.setDeltaMovement(velocity);projectile.hasImpulse=true;
        }
        if(state()!=HELD) {
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,bounds,this::enemy)) {
                Vector3d offset=position().subtract(target.position().add(0,target.getBbHeight()*.5,0));
                double distance=offset.length();if(distance>reach||AbilityHelper.isDodging(target))continue;
                double strength=.16+.42*(1-distance/reach);
                Vector3d velocity=target.getDeltaMovement().scale(.78).add(offset.normalize().scale(strength));
                if(velocity.lengthSqr()>2.25)velocity=velocity.normalize().scale(1.5);
                target.setDeltaMovement(velocity);target.hurtMarked=true;target.fallDistance=0;
                if(distance<radius()+.5&&tickCount%10==0) {
                    target.hurt(getDamageSource(),getDamage());
                    AbilityHelper.disableAbilities(target,20,a->a.getCore().getCategory()==AbilityCategory.DEVIL_FRUITS);
                }
            }
            if(state()==VORTEX)consumeTerrain();
            if(--remaining<=0)remove();
        }
    }
    private void consumeTerrain() {
        int removed=0,scanned=0;
        while(terrainCursor<TERRAIN.size()&&removed<6&&scanned++<128) {
            BlockPos pos=blockPosition().offset(TERRAIN.get(terrainCursor++));
            if(!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            if(state.isAir()||state.hasTileEntity()||state.getDestroySpeed(level,pos)<0||state.getMaterial().isLiquid())continue;
            if(AbilityHelper.placeBlockIfAllowed(getThrower(),pos,Blocks.AIR.defaultBlockState(),DefaultProtectionRules.AIR_CORE_FOLIAGE_ORE)) {
                YamiVisualPacket.debris(this,Vector3d.atCenterOf(pos),position(),Block.getId(state));removed++;
            }
        }
    }
    private static List<BlockPos> terrainOffsets() {
        List<BlockPos> offsets=new ArrayList<>();
        for(int x=-9;x<=9;x++)for(int y=-9;y<=9;y++)for(int z=-9;z<=9;z++)
            if(x*x+y*y+z*z<=81)offsets.add(new BlockPos(x,y,z));
        offsets.sort(Comparator.comparingDouble(p->p.distSqr(BlockPos.ZERO)));return offsets;
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(SIZE,2F);entityData.define(STATE,HELD);}
    @Override public void setSize(float size){entityData.set(SIZE,MathHelper.clamp(size,.5F,8));}
    @Override public float getSize(){return entityData.get(SIZE);}
}
