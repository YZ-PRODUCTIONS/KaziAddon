package net.kazi.kazimod.seiryu;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.entity.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

public final class SeiryuTargets {
    public static Entity owner(Entity target) {
        return target instanceof SeiryuPartEntity?((SeiryuPartEntity)target).owner():target;
    }
    public static Predicate<Entity> ownerPredicate(Predicate<Entity> predicate) {
        if(predicate==null)return null;
        return target->{Entity actual=owner(target);return actual!=null
                &&(!(target instanceof SeiryuPartEntity)||((SeiryuPartEntity)target).matchesForm())&&predicate.test(actual);};
    }
    public static boolean livingQuery(Class<?>[] types) {
        if(types.length==0)return false;
        for(Class<?> type:types)if(!LivingEntity.class.isAssignableFrom(type))return false;
        return true;
    }
    public static List<Entity> livingTargets(List<Entity> found,IWorld world,AxisAlignedBB box,
                                            Predicate<Entity> predicate,Class<?>[] types) {
        // Player-only queries otherwise miss a tail when its player's head is outside the area.
        boolean acceptsPart=false;
        for(Class<?> type:types)if(type.isAssignableFrom(SeiryuPartEntity.class))acceptsPart=true;
        if(acceptsPart)return normalize(found,predicate);
        List<Entity> candidates=new ArrayList<>(found);
        for(SeiryuPartEntity part:world.getEntitiesOfClass(SeiryuPartEntity.class,box,e->true)) {
            Entity actual=part.owner();
            if(actual!=null)for(Class<?> type:types)if(type.isInstance(actual)){candidates.add(part);break;}
        }
        return normalize(candidates,predicate);
    }
    public static List<Entity> normalize(List<Entity> found,Predicate<Entity> predicate) {
        Set<Entity> dragons=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Entity target:found)if(target instanceof SeiryuPartEntity&&owner(target)!=null)dragons.add(owner(target));
        if(dragons.isEmpty())return found;
        Set<Entity> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        List<Entity> result=new ArrayList<>();
        for(Entity target:found) {
            Entity actual=owner(target);
            if(actual==null)continue;
            if(target instanceof SeiryuPartEntity && (!((SeiryuPartEntity)target).matchesForm()
                    || !ModEntityPredicates.IS_ALIVE_AND_SURVIVAL.test(actual)
                    || predicate!=null&&!predicate.test(actual)))continue;
            if(!dragons.contains(actual)||seen.add(actual))result.add(actual);
        }
        return result;
    }
    public static void collapseExplosion(List<Entity> found,Vector3d center) {
        found.removeIf(target->target instanceof SeiryuPartEntity&&!((SeiryuPartEntity)target).matchesForm());
        Set<Entity> dragons=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Entity target:found)if(target instanceof SeiryuPartEntity&&owner(target)!=null)dragons.add(owner(target));
        if(dragons.isEmpty())return;
        Map<Entity,Entity> nearest=new IdentityHashMap<>();
        for(Entity target:found) {
            Entity actual=owner(target);
            if(!dragons.contains(actual))continue;
            Entity previous=nearest.get(actual);
            if(previous==null||target.distanceToSqr(center)<previous.distanceToSqr(center))nearest.put(actual,target);
        }
        Set<Entity> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        found.removeIf(target->{Entity actual=owner(target);return dragons.contains(actual)
                &&(nearest.get(actual)!=target||!seen.add(actual));});
    }
}
