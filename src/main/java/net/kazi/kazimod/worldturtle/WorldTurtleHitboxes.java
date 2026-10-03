package net.kazi.kazimod.worldturtle;

import java.util.*;
import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.morphs.WorldTurtleMorphInfo;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;
import net.minecraftforge.event.TickEvent;

public final class WorldTurtleHitboxes {
    private static final String IDS="kazimodWorldTurtleParts";
    private static final Map<LivingEntity,PoseState> POSES=new WeakHashMap<>();
    private static final class PoseState {
        int tick=Integer.MIN_VALUE;
        final AxisAlignedBB[] boxes=new AxisAlignedBB[WorldTurtleAnatomy.BONES.length];
    }
    public static boolean active(LivingEntity owner){return owner!=null&&owner.isAlive()&&KaziMorphs.WORLD_TURTLE.get().isActive(owner);}
    public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.level.isClientSide)return;
        LivingEntity owner=event.player;int[] ids=owner.getPersistentData().getIntArray(IDS);
        if(!active(owner)){
            for(int id:ids){Entity e=owner.level.getEntity(id);if(e instanceof WorldTurtlePartEntity&&((WorldTurtlePartEntity)e).owner()==owner)e.remove();}
            owner.getPersistentData().remove(IDS);POSES.remove(owner);return;
        }
        int count=WorldTurtleAnatomy.BONES.length;if(ids.length!=count)ids=new int[count];
        WorldTurtleImmunity.clear(owner);
        for(int i=0;i<count;i++){
            Entity e=owner.level.getEntity(ids[i]);
            if(!(e instanceof WorldTurtlePartEntity)||((WorldTurtlePartEntity)e).owner()!=owner||!e.isAlive()){
                WorldTurtlePartEntity part=new WorldTurtlePartEntity(WorldTurtleEffects.PART.get(),owner.level);
                part.bind(owner,i);part.updateBounds();owner.level.addFreshEntity(part);ids[i]=part.getId();
            }else ((WorldTurtlePartEntity)e).updateBounds();
        }
        owner.getPersistentData().putIntArray(IDS,ids);
    }
    public static AxisAlignedBB bounds(LivingEntity owner,int part){
        PoseState state=POSES.computeIfAbsent(owner,k->new PoseState());
        if(state.tick!=owner.tickCount){
            state.tick=owner.tickCount;
            for(int i=0;i<state.boxes.length;i++){
                // ZoanMorphRenderer also inherits PlayerRenderer's 0.9375 scale.
                double[] b=WorldTurtleAnatomy.fixedBounds(i,owner.yBodyRot,WorldTurtleMorphInfo.SCALE*.9375F);
                state.boxes[i]=new AxisAlignedBB(b[0],b[1],b[2],b[3],b[4],b[5]);
            }
        }
        AxisAlignedBB b=state.boxes[part];return b==null?null:b.move(owner.position());
    }
}
