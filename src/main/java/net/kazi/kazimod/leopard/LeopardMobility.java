package net.kazi.kazimod.leopard;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

final class LeopardMobility {
    static boolean move(LivingEntity user, Vector3d delta) {
        if(!Double.isFinite(delta.lengthSqr())||delta.lengthSqr()>9)return false;
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.35));
        Vector3d start=user.position(),step=delta.scale(1.0/steps);
        for(int i=0;i<steps;i++) {
            Vector3d next=user.position().add(step);BlockPos pos=new BlockPos(next);
            if(next.y<1||next.y+user.getBbHeight()>=user.level.getMaxBuildHeight()
                    ||!user.level.hasChunkAt(pos)||!user.level.getWorldBorder().isWithinBounds(pos))return false;
            user.move(MoverType.SELF,step);
        }
        user.fallDistance=0;
        AbilityHelper.setDeltaMovement(user,Vector3d.ZERO);
        return user.position().distanceToSqr(start)>.015;
    }
    private LeopardMobility(){}
}
