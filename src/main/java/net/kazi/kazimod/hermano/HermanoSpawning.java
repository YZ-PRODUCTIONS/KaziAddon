package net.kazi.kazimod.hermano;

import java.util.*;
import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public final class HermanoSpawning extends WorldSavedData {
    private static final String NAME="kazimod_el_hermano";
    private UUID active;
    private BlockPos lastPosition=BlockPos.ZERO;
    private int remaining,missingTicks;
    public HermanoSpawning(){super(NAME);}
    public static HermanoSpawning get(ServerWorld world){
        return world.getServer().overworld().getDataStorage().computeIfAbsent(HermanoSpawning::new,NAME);
    }
    public static int nextDelay(Random random,int minimum,int maximum){
        int low=Math.max(1,Math.min(minimum,maximum)),high=Math.max(low,Math.max(minimum,maximum));
        return (low*60+random.nextInt((high-low)*60+1))*20;
    }
    private void schedule(Random random){
        remaining=nextDelay(random,KaziConfig.INSTANCE.elHermanoMinMinutes.get(),KaziConfig.INSTANCE.elHermanoMaxMinutes.get());setDirty();
    }
    public void track(HermanoEntity boss){
        if(!boss.isNaturalBoss())return;
        if(!boss.getUUID().equals(active)||!boss.blockPosition().equals(lastPosition)){
            active=boss.getUUID();lastPosition=boss.blockPosition();setDirty();
        }
        missingTicks=0;
    }
    public void defeated(HermanoEntity boss){
        if(boss.getUUID().equals(active)){active=null;missingTicks=0;schedule(boss.level.random);}
    }
    public static void announce(HermanoEntity boss){
        if(!(boss.level instanceof ServerWorld))return;
        ServerWorld world=(ServerWorld)boss.level;
        world.getServer().getPlayerList().broadcastMessage(new TranslationTextComponent("message.kazimod.el_hermano.spawn",
                boss.blockPosition().getX(),boss.blockPosition().getY(),boss.blockPosition().getZ(),world.dimension().location().toString())
                .withStyle(TextFormatting.GOLD),ChatType.SYSTEM,net.minecraft.util.Util.NIL_UUID);
    }
    public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        net.minecraft.server.MinecraftServer server=ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.getTickCount()%20!=0)return;
        ServerWorld world=server.overworld();if(world==null)return;
        HermanoSpawning data=get(world);
        if(data.active!=null){
            Entity boss=world.getEntity(data.active);
            if(boss instanceof HermanoEntity){data.track((HermanoEntity)boss);return;}
            // An unloaded boss still owns the slot. Only recover a missing entity in a fully loaded chunk.
            if(world.getChunkSource().getChunkNow(data.lastPosition.getX()>>4,data.lastPosition.getZ()>>4)!=null){
                data.missingTicks+=20;
                if(data.missingTicks>=400){data.active=null;data.schedule(world.random);}
            }else data.missingTicks=0;
            return;
        }
        if(!KaziConfig.INSTANCE.elHermanoSpawns.get()||world.getDifficulty()==Difficulty.PEACEFUL)return;
        List<ServerPlayerEntity> players=new ArrayList<>();
        for(ServerPlayerEntity player:world.players())if(player.isAlive()&&!player.isSpectator()&&!player.isCreative())players.add(player);
        if(players.isEmpty())return;
        if(data.remaining<=0)data.schedule(world.random);
        data.remaining=Math.max(0,data.remaining-20);data.setDirty();
        if(data.remaining>0)return;
        ServerPlayerEntity near=players.get(world.random.nextInt(players.size()));
        for(int attempt=0;attempt<24;attempt++){
            double angle=world.random.nextDouble()*Math.PI*2,radius=64+world.random.nextInt(49);
            int x=MathHelper.floor(near.getX()+Math.cos(angle)*radius),z=MathHelper.floor(near.getZ()+Math.sin(angle)*radius);
            // Never force generation or load chunks just to find a spawn location.
            if(world.getChunkSource().getChunkNow(x>>4,z>>4)==null)continue;
            BlockPos ground=world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
            if(!world.getWorldBorder().isWithinBounds(ground)||ground.getY()<2||ground.getY()>245
                    ||!world.getFluidState(ground.below()).isEmpty()||!world.getBlockState(ground.below()).getMaterial().isSolid())continue;
            AxisAlignedBB area=new AxisAlignedBB(x-1.1,ground.getY(),z-1.1,x+2.1,ground.getY()+4,z+2.1);
            if(!world.noCollision(area)||world.containsAnyLiquid(area))continue;
            HermanoEntity boss=HermanoFeatures.BOSS.get().create(world);if(boss==null)break;
            boss.moveTo(x+.5,ground.getY(),z+.5,world.random.nextFloat()*360,0);boss.setNaturalBoss();
            if(world.addFreshEntity(boss)){data.track(boss);return;}
        }
        data.remaining=1200;data.setDirty();
    }
    @Override public void load(CompoundNBT n){remaining=Math.max(0,n.getInt("Remaining"));active=n.hasUUID("Active")?n.getUUID("Active"):null;lastPosition=BlockPos.of(n.getLong("Position"));}
    @Override public CompoundNBT save(CompoundNBT n){n.putInt("Remaining",remaining);if(active!=null)n.putUUID("Active",active);n.putLong("Position",lastPosition.asLong());return n;}
}
