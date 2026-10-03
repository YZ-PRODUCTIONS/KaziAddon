package net.kazi.kazimod.worldturtle;

import java.util.*;
import net.kazi.kazimod.abilities.KameRework.DivineShieldAbility;
import net.kazi.kazimod.abilities.KameRework.DestructionAbility;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;

public final class WorldTurtleEffectEntity extends Entity {
    public static final float BLAST_RADIUS=WorldTurtleBlastDamage.RADIUS,MAX_DAMAGE=WorldTurtleBlastDamage.MAX_DAMAGE;
    private static final int CRATER_RADIUS=48,CRATER_DEPTH=20,CRATER_HEIGHT=40;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(WorldTurtleEffectEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> AGE=EntityDataManager.defineId(WorldTurtleEffectEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(WorldTurtleEffectEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> SKY=EntityDataManager.defineId(WorldTurtleEffectEntity.class,DataSerializers.FLOAT);
    private static final DataParameter<Boolean> REFLECTED=EntityDataManager.defineId(WorldTurtleEffectEntity.class,DataSerializers.BOOLEAN);
    private final WorldTurtleBlastDamage blastHits=new WorldTurtleBlastDamage();
    private DestructionAbility blastAbility;
    private int column,layer;
    private static final List<BlockPos> COLUMNS=columns();
    private static final Map<World,Budget> BUDGETS=new WeakHashMap<>();
    private static final class Budget {long tick=-1,deadline;int scanned,destroyed;}
    public WorldTurtleEffectEntity(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(AGE,0);entityData.define(OWNER,-1);entityData.define(SKY,0F);entityData.define(REFLECTED,false);}
    public boolean reflected(){return entityData.get(REFLECTED);}
    public int kind(){return entityData.get(KIND);}public int age(){return entityData.get(AGE);}public float sky(){return entityData.get(SKY);}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity?(LivingEntity)e:null;}
    public static void shield(LivingEntity e){spawn(e,2,e.position().add(0,9,0));}
    public static void bomb(LivingEntity e,Vector3d ground){spawn(e,0,ground.add(0,Math.min(64,250-ground.y),0));}
    private static void spawn(LivingEntity owner,int kind,Vector3d pos){
        WorldTurtleEffectEntity e=new WorldTurtleEffectEntity(WorldTurtleEffects.EFFECT.get(),owner.level);
        e.entityData.set(OWNER,owner.getId());e.entityData.set(KIND,kind);e.entityData.set(SKY,(float)pos.y+10);
        e.setPos(pos.x,pos.y,pos.z);owner.level.addFreshEntity(e);
        if(kind==0)owner.level.playSound(null,e.blockPosition(),SoundEvents.END_PORTAL_SPAWN,SoundCategory.PLAYERS,4,.65F);
    }
    public void tick(){
        super.tick();LivingEntity owner=owner();
        if(kind()==2&&owner!=null)setPos(owner.getX(),owner.getY()+9,owner.getZ());
        if(level.isClientSide)return;
        if(owner==null||!owner.isAlive()){remove();return;}
        if(kind()!=1||age()<220)entityData.set(AGE,age()+1);
        if(kind()==2){if(age()>100||!DivineShieldAbility.active(owner))remove();return;}
        if(kind()==0){
            if(age()>160){remove();return;}
            if(age()<25)return;
            if(!reflected())setDeltaMovement(0,-Math.min(4,.15+(age()-25)*.09),0);
            Vector3d next=position().add(getDeltaMovement());
            if(!level.hasChunkAt(new BlockPos(next))){remove();return;}
            BlockRayTraceResult ray=level.clip(new RayTraceContext(position(),next,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.ANY,this));
            if(reflected()){
                LivingEntity victim=null;Vector3d contact=null;double nearest=ray.getType()==RayTraceResult.Type.BLOCK?position().distanceToSqr(ray.getLocation()):Double.MAX_VALUE;
                for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(position(),next).inflate(4))){
                    if(e==owner||!e.isAlive()||e.isSpectator()||owner.isAlliedTo(e))continue;
                    java.util.Optional<Vector3d> point=e.getBoundingBox().inflate(2.8).clip(position(),next);
                    if(point.isPresent()&&position().distanceToSqr(point.get())<nearest){victim=e;contact=point.get();nearest=position().distanceToSqr(contact);}
                }
                if(victim!=null){if(DivineShieldAbility.active(victim))reflect(victim);else{setPos(contact.x,contact.y,contact.z);detonate(owner);}return;}
            }
            if(ray.getType()==RayTraceResult.Type.BLOCK){setPos(ray.getLocation().x,ray.getLocation().y,ray.getLocation().z);detonate(owner);}
            else{setPos(next.x,next.y,next.z);if(getY()<1)remove();}return;
        }
        if(age()%4==0&&age()<=WorldTurtleBlastDamage.DAMAGE_END)damageWave(owner);
        if(age()==WorldTurtleBlastDamage.DAMAGE_END+1){blastHits.clear();blastAbility=null;}
        destroyTerrain(owner);
        // Finish the larger crater after the visual fades, at the same bounded rate.
        if((age()>=220&&(column>=COLUMNS.size()||!CommonConfig.INSTANCE.isAbilityGriefingEnabled()||!(owner instanceof ServerPlayerEntity)))||tickCount>7360)remove();
    }
    public void reflect(LivingEntity defender){
        LivingEntity shooter=owner();if(kind()!=0||shooter==defender||level.isClientSide)return;
        Vector3d center=defender.position().add(0,9,0),direction=shooter!=null?shooter.getBoundingBox().getCenter().subtract(center).normalize():getDeltaMovement().scale(-1).normalize();
        if(direction.lengthSqr()<.001)direction=defender.getLookAngle();
        Vector3d origin=center.add(direction.scale(26));setPos(origin.x,origin.y,origin.z);
        setDeltaMovement(direction.scale(3));entityData.set(OWNER,defender.getId());entityData.set(REFLECTED,true);hurtMarked=true;
        blastAbility=null;
    }
    private void detonate(LivingEntity owner){
        Explosion explosion=new Explosion(level,owner,getX(),getY(),getZ(),BLAST_RADIUS,false,Explosion.Mode.DESTROY);
        if(ForgeEventFactory.onExplosionStart(level,explosion)){remove();return;}
        entityData.set(KIND,1);entityData.set(AGE,0);
        blastAbility=DestructionAbility.forCaster(owner);
        level.playSound(null,blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundCategory.PLAYERS,12,.45F);
    }
    private void damageWave(LivingEntity owner){
        double radius=WorldTurtleBlastDamage.radius(age());
        Map<LivingEntity,Double> contacts=new HashMap<>();
        for(LivingEntity part:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(position(),position()).inflate(radius))){
            LivingEntity target=part instanceof WorldTurtlePartEntity?((WorldTurtlePartEntity)part).owner():part;
            if(target==null||target==owner||!target.isAlive()||target.isSpectator()||owner.isAlliedTo(target)||blastHits.resolved(target.getUUID()))continue;
            if(part instanceof WorldTurtlePartEntity&&((WorldTurtlePartEntity)part).isShell())continue;
            if(part==target&&WorldTurtleHitboxes.active(target))continue;
            AxisAlignedBB b=part.getBoundingBox();
            Vector3d closest=new Vector3d(MathHelper.clamp(getX(),b.minX,b.maxX),MathHelper.clamp(getY(),b.minY,b.maxY),MathHelper.clamp(getZ(),b.minZ,b.maxZ));
            double distance=closest.distanceTo(position());if(distance>radius)continue;
            BlockPos pos=part.blockPosition();ProtectedArea area=ProtectedAreasData.get(level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
            if(area!=null&&!area.canHurtEntities())continue;
            contacts.merge(target,distance,Math::min);
        }
        if(blastAbility==null)blastAbility=DestructionAbility.forCaster(owner);
        for(Map.Entry<LivingEntity,Double> contact:contacts.entrySet()){
            LivingEntity target=contact.getKey();UUID id=target.getUUID();double distance=contact.getValue();
            blastHits.contact(id,distance,age());
            float amount=blastHits.ready(id,target.invulnerableTime,age());if(amount<=0)continue;
            // Do not clear immunity timers or keep retrying intentional dodges/shields.
            boolean damaged=blastAbility.hurtBlast(owner,target,amount,this);
            blastHits.resolve(id);
            if(!damaged)continue;
            float falloff=(float)Math.max(0,1-distance/BLAST_RADIUS);
            Vector3d push=target.position().subtract(position()).normalize().scale(2*falloff);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x,.4*falloff,push.z));target.hurtMarked=true;
        }
    }
    private static List<BlockPos> columns(){List<BlockPos> list=new ArrayList<>();for(int x=-CRATER_RADIUS;x<=CRATER_RADIUS;x++)for(int z=-CRATER_RADIUS;z<=CRATER_RADIUS;z++)if(x*x+z*z<=CRATER_RADIUS*CRATER_RADIUS)list.add(new BlockPos(x,0,z));list.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));return list;}
    private void destroyTerrain(LivingEntity owner){
        if(!CommonConfig.INSTANCE.isAbilityGriefingEnabled()||!(owner instanceof ServerPlayerEntity))return;
        Budget budget=BUDGETS.computeIfAbsent(level,k->new Budget());long now=level.getGameTime();
        if(budget.tick!=now){budget.tick=now;budget.scanned=0;budget.destroyed=0;budget.deadline=System.nanoTime()+6_000_000L;}
        double wave=Math.min(CRATER_RADIUS,age()*1.6);ServerPlayerEntity player=(ServerPlayerEntity)owner;
        // Hollow Nuke uses direct air placement. Keep that quiet bulk behavior but
        // bound the work across all nukes in this world, including a wall-time limit.
        while(column<COLUMNS.size()&&budget.scanned<32768&&budget.destroyed<4096&&System.nanoTime()<budget.deadline){
            BlockPos c=COLUMNS.get(column);double r2=c.getX()*c.getX()+c.getZ()*c.getZ();if(r2>wave*wave)break;
            int dy=layer-CRATER_DEPTH;BlockPos pos=blockPosition().offset(c.getX(),dy,c.getZ());
            if(++layer>=CRATER_DEPTH+CRATER_HEIGHT+1){layer=0;column++;}budget.scanned++;
            if(dy< -CRATER_DEPTH*(1-r2/(CRATER_RADIUS*CRATER_RADIUS))||pos.getY()<1||pos.getY()>254||!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.getDestroySpeed(level,pos)<0||state.hasTileEntity()||RestrictedBlockProtectionRule.INSTANCE.isBanned(state))continue;
            ProtectedArea area=ProtectedAreasData.get(level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
            if(area!=null&&!area.canDestroyBlocks())continue;
            if(!level.mayInteract(player,pos)||!state.canEntityDestroy(level,pos,this)||!ForgeEventFactory.onEntityDestroyBlock(owner,pos,state))continue;
            if(ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1)continue;
            if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3))budget.destroyed++;
        }
    }
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(kind()==2?24:140,180,140);}
    public boolean shouldRenderAtSqrDistance(double d){return d<320*320;}
    protected void readAdditionalSaveData(CompoundNBT n){remove();}protected void addAdditionalSaveData(CompoundNBT n){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
