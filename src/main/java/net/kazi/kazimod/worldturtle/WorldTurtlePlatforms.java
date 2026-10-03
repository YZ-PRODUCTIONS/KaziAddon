package net.kazi.kazimod.worldturtle;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Stream;
import net.kazi.kazimod.models.zoan.*;
import net.kazi.kazimod.morphs.WorldTurtleMorphInfo;
import net.kazi.kazimod.abilities.KameRework.DivineShieldAbility;
import net.kazi.kazimod.abilities.KameRework.WorldShakingSpinAbility;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;

public final class WorldTurtlePlatforms {
    private static final Map<PlayerEntity,State> STATES=new WeakHashMap<>();
    private static final Map<PlayerEntity,Rider> RIDERS=new WeakHashMap<>();
    private static final class State {int tick=-1;final WorldTurtleMotion motion=new WorldTurtleMotion();CerberusRig.Pose root;}
    private static final class Rider {
        final WeakReference<PlayerEntity> owner;final double u,v;final Vector3d previous;
        Rider(PlayerEntity owner,double[] local,double[] previous){this.owner=new WeakReference<>(owner);u=local[0];v=local[1];this.previous=new Vector3d(previous[0],previous[1],previous[2]);}
    }
    private static WorldTurtleDeck deck(PlayerEntity owner){
        State s=STATES.computeIfAbsent(owner,k->new State());
        if(s.tick!=owner.tickCount){s.tick=owner.tickCount;
            boolean spinning=WorldShakingSpinAbility.active(owner);
            s.motion.shieldAt(DivineShieldAbility.active(owner)||spinning,owner.tickCount);
            s.motion.spinAt(spinning,owner.tickCount);
            s.root=s.motion.sample(WorldTurtleAnatomy.rig(),owner.tickCount,owner.animationSpeed,!owner.isOnGround(),owner.isSprinting(),
                    MathHelper.wrapDegrees(owner.yHeadRot-owner.yBodyRot),owner.xRot,MathHelper.wrapDegrees(owner.yBodyRot-owner.yBodyRotO),owner.getAttackAnim(1)).get("World Turtle");}
        return new WorldTurtleDeck(s.root,owner.yBodyRot,WorldTurtleMorphInfo.SCALE*.9375F,owner.getX(),owner.getY(),owner.getZ());
    }
    public static Stream<VoxelShape> collisions(Entity mover,AxisAlignedBB query){
        if(!(mover instanceof PlayerEntity)||mover.isSpectator())return Stream.empty();
        Stream.Builder<VoxelShape> out=Stream.builder();
        for(PlayerEntity owner:mover.level.players()){
            if(owner==mover||owner.distanceToSqr(mover)>2500||!WorldTurtleHitboxes.active(owner))continue;
            WorldTurtleDeck deck=deck(owner);double x=(query.minX+query.maxX)*.5,z=(query.minZ+query.maxZ)*.5;
            if(!deck.overlaps(query.minX,query.maxX,query.minZ,query.maxZ))continue;
            double top=deck.height(x,z);
            // A thin, one-way landing surface avoids catching players on its underside.
            if(mover.getBoundingBox().minY<top-.3||query.minY>top+.02||query.maxY<top-.06)continue;
            out.add(VoxelShapes.create(new AxisAlignedBB(query.minX-.01,top-.06,query.minZ-.01,query.maxX+.01,top,query.maxZ+.01)));
        }
        return out.build();
    }
    public static void tick(TickEvent.PlayerTickEvent event){
        PlayerEntity player=event.player;
        if(player.isSpectator()||player.isPassenger()||player.abilities.flying){RIDERS.remove(player);return;}
        if(event.phase==TickEvent.Phase.START){
            Rider rider=RIDERS.remove(player);if(rider==null||player.getDeltaMovement().y>.1)return;
            if(!player.isOnGround()&&Math.abs(player.getY()-rider.previous.y)>.55)return;
            PlayerEntity owner=rider.owner.get();if(owner==null||owner.level!=player.level||!WorldTurtleHitboxes.active(owner))return;
            double[] p=deck(owner).at(rider.u,rider.v);Vector3d delta=new Vector3d(p[0],p[1],p[2]).subtract(rider.previous);
            if(delta.lengthSqr()>16)return; // Do not drag riders through a teleport.
            player.move(MoverType.SELF,delta);player.setOnGround(true);player.fallDistance=0;
        }else if(player.getDeltaMovement().y<=0){
            for(PlayerEntity owner:player.level.players()){
                if(owner==player||owner.distanceToSqr(player)>2500||!WorldTurtleHitboxes.active(owner))continue;
                WorldTurtleDeck deck=deck(owner);double y=deck.height(player.getX(),player.getZ());
                if(deck.overlaps(player.getBoundingBox().minX,player.getBoundingBox().maxX,
                        player.getBoundingBox().minZ,player.getBoundingBox().maxZ)
                        && player.getY()>=y-.3 && player.getY()<=y+.45){
                    if(!player.isOnGround()){
                        player.setPos(player.getX(),y,player.getZ());
                        player.setDeltaMovement(player.getDeltaMovement().x,0,player.getDeltaMovement().z);
                        player.setOnGround(true);
                    }
                    double[] local=deck.local(player.getX(),y,player.getZ());RIDERS.put(player,new Rider(owner,local,deck.at(local[0],local[1])));player.fallDistance=0;break;
                }
            }
        }
    }
}
