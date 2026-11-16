package net.kazi.kazimod.decay;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import net.kazi.kazimod.petal.PetalFeatures;

public final class DecayField extends Entity {
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(DecayField.class,DataSerializers.INT),OWNER=EntityDataManager.defineId(DecayField.class,DataSerializers.INT),TARGET=EntityDataManager.defineId(DecayField.class,DataSerializers.INT),START=EntityDataManager.defineId(DecayField.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(DecayField.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(DecayField.class,DataSerializers.FLOAT);
    private static final DataParameter<CompoundNBT> DATA=EntityDataManager.defineId(DecayField.class,DataSerializers.COMPOUND_TAG);
    private UUID ownerId,targetId;
    private final Map<BlockPos,Integer> ground=new LinkedHashMap<>();
    private final ArrayDeque<BlockPos> frontier=new ArrayDeque<>();
    private final Set<Long> columns=new HashSet<>();
    private final Set<UUID> hit=new HashSet<>();
    private final Set<BlockPos> processed=new HashSet<>();
    private int destroyed,sourceMove;private DecayAbility attack;private boolean initialized;
    private CatastropheTerrain catastrophe;
    public DecayField source(int move){sourceMove=move;return this;}
    public DecayField(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(TARGET,-1);entityData.define(START,0);entityData.define(LIFE,20);entityData.define(SIZE,1F);entityData.define(DATA,new CompoundNBT());}
    public static DecayField spawn(LivingEntity p,int kind,Vector3d pos,int life,float size){DecayField f=new DecayField(DecayFeatures.FIELD.get(),p.level);f.entityData.set(KIND,kind);f.entityData.set(OWNER,p.getId());f.ownerId=p.getUUID();f.entityData.set(START,(int)p.level.getGameTime());f.entityData.set(LIFE,life);f.entityData.set(SIZE,size);f.setPos(pos.x,pos.y,pos.z);p.level.addFreshEntity(f);return f;}
    public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}public float age(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity&&(ownerId==null||ownerId.equals(e.getUUID()))?(LivingEntity)e:null;}
    public LivingEntity target(){Entity e=level.getEntity(entityData.get(TARGET));return e instanceof LivingEntity&&(targetId==null||targetId.equals(e.getUUID()))?(LivingEntity)e:null;}
    public void target(LivingEntity t){entityData.set(TARGET,t.getId());targetId=t.getUUID();}
    public void end(Vector3d v){CompoundNBT n=entityData.get(DATA).copy();n.putDouble("X",v.x);n.putDouble("Y",v.y);n.putDouble("Z",v.z);entityData.set(DATA,n);}
    public Vector3d end(){CompoundNBT n=entityData.get(DATA);return new Vector3d(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));}
    public int[] nodes(){return entityData.get(DATA).getIntArray("Ground");}
    public int[] dust(){return entityData.get(DATA).getIntArray("Dust");}
    void dust(int[] values){CompoundNBT n=entityData.get(DATA).copy();n.putIntArray("Dust",values);entityData.set(DATA,n);}
    public int warning(){return kind()==5?60:16;}
    public float wave(float partial){return Math.min(size(),Math.max(0,(age(partial)-warning())*.65F));}
    public static BlockPos surface(World w,BlockPos at){for(int dy=1;dy>=-1;dy--){BlockPos p=at.offset(0,dy,0);if(!w.hasChunkAt(p))continue;BlockState b=w.getBlockState(p);if(b.getMaterial().isSolid()&&!b.getCollisionShape(w,p).isEmpty()&&w.getBlockState(p.above()).getCollisionShape(w,p.above()).isEmpty())return p;}return null;}
    private void mapGround(){
        if(!initialized){initialized=true;BlockPos root=surface(level,blockPosition().below());if(root!=null){ground.put(root,0);frontier.add(root);columns.add(column(root));}}
        boolean changed=false;for(int count=0;count<96&&!frontier.isEmpty()&&ground.size()<2048;count++){
            BlockPos at=frontier.remove();int d=ground.get(at);if(d>=size()*1.5)continue;
            for(Direction direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){BlockPos offset=at.relative(direction);if(columns.contains(column(offset))||offset.distSqr(blockPosition())>size()*size()+9)continue;BlockPos next=surface(level,offset);if(next!=null){columns.add(column(next));ground.put(next,d+1);frontier.add(next);changed=true;}}
        }
        if(changed||age(0)==1){int[] nodes=new int[ground.size()*4];int i=0;for(Map.Entry<BlockPos,Integer> entry:ground.entrySet()){BlockPos p=entry.getKey();nodes[i++]=p.getX();nodes[i++]=p.getY();nodes[i++]=p.getZ();nodes[i++]=entry.getValue();}CompoundNBT n=entityData.get(DATA).copy();n.putIntArray("Ground",nodes);entityData.set(DATA,n);}
    }
    private static long column(BlockPos p){return ((long)p.getX()<<32)^(p.getZ()&0xffffffffL);}
    private double groundDistance(BlockPos p){double x=p.getX()-getX(),z=p.getZ()-getZ();return Math.sqrt(x*x+z*z);}
    public boolean reached(LivingEntity t){
        BlockPos feet=t.blockPosition().below();
        if(kind()==5){double x=t.getX()-getX(),z=t.getZ()-getZ();
            double edge=Math.min(size(),wave(0)+1);
            return x*x+z*z<=edge*edge&&Math.abs(t.getY()-getY())<=CatastropheTerrain.HALF_HEIGHT
                    &&level.hasChunkAt(feet)&&!level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()
                    &&t.getY()<=feet.getY()+1.35;
        }
        for(int y=-1;y<=1;y++){BlockPos p=feet.offset(0,y,0);if(ground.containsKey(p)&&groundDistance(p)<=wave(0)&&t.getY()>=p.getY()+.7&&t.getY()<=p.getY()+1.35)return true;}return false;
    }
    @Override public void tick(){super.tick();if(level.isClientSide)return;LivingEntity p=owner();int age=(int)age(0);if(p==null||!DecayFeatures.usable(p)||age>=life()||p.distanceToSqr(this)>128*128){remove();return;}
        if(attack==null)attack=DecayAbility.CORES[kind()==6?sourceMove:Math.min(kind(),5)].createAbility();
        if(kind()==1||kind()==5){
            if(kind()==1)mapGround();
            if(age>=warning()&&(kind()==5||age%2==0)&&(kind()==1||age<=warning()+Math.ceil(size()/.65)+20)){
                for(LivingEntity t:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(size(),12,size()),t->PetalFeatures.canHit(p,t))){LivingEntity body=PetalFeatures.bodyOwner(t);if(body!=null&&!hit.contains(body.getUUID())&&reached(t)){hit.add(body.getUUID());if(attack.strike(p,t,kind()==5?40:18))attack.mark(p,t,kind()==5?3:2);}}
                if(kind()==1)destroyTerrain(p);
            }
            if(kind()==5&&age>=warning()&&p instanceof ServerPlayerEntity){
                if(catastrophe==null)catastrophe=new CatastropheTerrain(this);
                boolean done=catastrophe.tick((ServerPlayerEntity)p,age);
                if(done&&age>warning()+Math.ceil(size()/.65)+32)remove();
            }
        }
        else if(kind()==6){LivingEntity t=target();if(t==null||!t.isAlive()||!PetalFeatures.canHit(p,t)){remove();return;}setPos(t.getX(),t.getY(),t.getZ());if(age>0&&age%20==0)attack.strike(p,t,size());}
        else if(kind()!=7){if(kind()==4&&target()!=null)setPos(target().getX(),target().getY(),target().getZ());else setPos(p.getX(),p.getY(),p.getZ());}
    }
    private void destroyTerrain(LivingEntity user){
        if(!(user instanceof ServerPlayerEntity)||!xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.isAbilityGriefingEnabled()||destroyed>=(kind()==5?192:48))return;
        ServerPlayerEntity player=(ServerPlayerEntity)user;int work=0,removed=0;
        // At most eight surface blocks per field per tick. No deep craters or block drops.
        for(Map.Entry<BlockPos,Integer> entry:ground.entrySet()){BlockPos pos=entry.getKey();if(processed.contains(pos)||groundDistance(pos)>wave(0)-1)continue;if(++work>64||removed>=8)break;processed.add(pos);
            if(Math.floorMod(pos.getX()*31+pos.getZ()*17,4)!=0||!level.hasChunkAt(pos)||pos.getY()<1||pos.distSqr(user.blockPosition())<4)continue;
            BlockState state=level.getBlockState(pos);float hardness=state.getDestroySpeed(level,pos);if(state.isAir()||hardness<0||hardness>5||!state.getFluidState().isEmpty()||state.hasTileEntity()||RestrictedBlockProtectionRule.INSTANCE.isBanned(state))continue;
            ProtectedArea area=ProtectedAreasData.get(level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());if(area!=null&&!area.canDestroyBlocks())continue;
            if(!level.mayInteract(player,pos)||!state.canEntityDestroy(level,pos,this)||!ForgeEventFactory.onEntityDestroyBlock(user,pos,state)||ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1)continue;
            if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){destroyed++;removed++;if(destroyed>=(kind()==5?192:48))break;}
        }
    }
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(kind()==1||kind()==5?size()+4:kind()==7?24:4);}
    protected void readAdditionalSaveData(CompoundNBT n){remove();}protected void addAdditionalSaveData(CompoundNBT n){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
