package net.kazi.kazimod.decay;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;

/** A radial, bounded terrain job. No falling-block entities, drops, or forced chunk loads. */
final class CatastropheTerrain {
    static final double SAFE_RADIUS=2.5;
    static final int HALF_HEIGHT=10, BLOCKS_PER_TICK=512, DUST_LIFE=32;
    private final DecayField field;
    private final List<BlockPos> columns=new ArrayList<>();
    private final ArrayDeque<int[]> dust=new ArrayDeque<>();
    private int column,y=HALF_HEIGHT,lastDust=-DUST_LIFE;

    CatastropheTerrain(DecayField field){
        this.field=field;int radius=(int)Math.ceil(field.size());
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            int d=x*x+z*z;
            if(d<=field.size()*field.size()&&d>SAFE_RADIUS*SAFE_RADIUS)columns.add(new BlockPos(x,0,z));
        }
        columns.sort(Comparator.comparingDouble(p->p.getX()*p.getX()+p.getZ()*p.getZ()));
    }

    boolean tick(ServerPlayerEntity user,int age){
        while(!dust.isEmpty()&&age-dust.peekFirst()[4]>=DUST_LIFE)dust.removeFirst();
        if(!xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.isAbilityGriefingEnabled())column=columns.size();
        int inspected=0,removed=0,samples=0;double wave=field.wave(0);
        while(column<columns.size()&&inspected<4096&&removed<BLOCKS_PER_TICK){
            BlockPos offset=columns.get(column);double d=offset.getX()*offset.getX()+offset.getZ()*offset.getZ();
            if(d>wave*wave)break;
            BlockPos pos=field.blockPosition().offset(offset.getX(),y,offset.getZ());
            if(!field.level.hasChunkAt(pos)){column++;y=HALF_HEIGHT;continue;}
            if(--y < -HALF_HEIGHT){column++;y=HALF_HEIGHT;}
            inspected++;
            if(pos.getY()<1||pos.getY()>=field.level.getMaxBuildHeight())continue;
            BlockState state=field.level.getBlockState(pos);
            if(state.isAir()||state.getDestroySpeed(field.level,pos)<0||!state.getFluidState().isEmpty()
                    ||state.hasTileEntity()||RestrictedBlockProtectionRule.INSTANCE.isBanned(state))continue;
            ProtectedArea area=ProtectedAreasData.get(field.level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
            if(area!=null&&!area.canDestroyBlocks())continue;
            if(!field.level.mayInteract(user,pos)||!state.canEntityDestroy(field.level,pos,field)
                    ||!ForgeEventFactory.onEntityDestroyBlock(user,pos,state)
                    ||ForgeHooks.onBlockBreakEvent(field.level,user.gameMode.getGameModeForPlayer(),user,pos)==-1)continue;
            int color=state.getMapColor(field.level,pos).col;
            if(field.level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){
                removed++;
                if(samples<16&&(removed==1||removed%32==0)){
                    dust.addLast(new int[]{pos.getX(),pos.getY(),pos.getZ(),color,age});samples++;lastDust=age;
                    while(dust.size()>256)dust.removeFirst();
                }
            }
        }
        if(age%2==0){int[] data=new int[dust.size()*5];int i=0;for(int[] entry:dust)for(int value:entry)data[i++]=value;field.dust(data);}
        return column>=columns.size()&&age-lastDust>=DUST_LIFE;
    }
}
