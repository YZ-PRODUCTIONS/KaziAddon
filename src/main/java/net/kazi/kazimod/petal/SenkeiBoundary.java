package net.kazi.kazimod.petal;

import java.util.*;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

/** Matching client/server shells, without placing or modifying terrain blocks. */
public final class SenkeiBoundary {
    public static final double FLOOR=-.5, CEILING=16.95;
    private static final Map<PetalField,List<AxisAlignedBB>> WALLS=Collections.synchronizedMap(new WeakHashMap<>());

    public static Stream<VoxelShape> collisions(Entity mover,AxisAlignedBB query){
        if(!(mover instanceof LivingEntity)||mover.isSpectator())return Stream.empty();
        Stream.Builder<VoxelShape> shapes=Stream.builder();
        for(PetalField field:mover.level.getEntitiesOfClass(PetalField.class,query.inflate(28,20,28),
                f->f.isAlive()&&f.age(0)<f.life()&&(f.kind()==7||f.kind()==4&&f.age(0)>=20))){
            if(field.kind()==4&&mover==field.owner())continue;
            for(AxisAlignedBB wall:WALLS.computeIfAbsent(field,SenkeiBoundary::walls))
                if(wall.intersects(query))shapes.add(VoxelShapes.create(wall));
        }
        return shapes.build();
    }

    private static List<AxisAlignedBB> walls(PetalField field){
        if(field.kind()==4)return prison(field);
        List<AxisAlignedBB> walls=new ArrayList<>(128);
        for(int i=0;i<128;i++){
            double a=i*Math.PI*2/128,b=(i+1)*Math.PI*2/128;
            double x1=field.getX()+Math.cos(a)*field.size(),z1=field.getZ()+Math.sin(a)*field.size();
            double x2=field.getX()+Math.cos(b)*field.size(),z2=field.getZ()+Math.sin(b)*field.size();
            walls.add(new AxisAlignedBB(Math.min(x1,x2)-.2,field.getY()+FLOOR,Math.min(z1,z2)-.2,
                    Math.max(x1,x2)+.2,field.getY()+CEILING,Math.max(z1,z2)+.2));
        }
        // Narrow overlapping strips close the circular floor and roof without square corners.
        double radius=field.size()+.2,step=radius*2/128;
        for(int i=0;i<128;i++){
            double x=-radius+i*step,next=x+step;
            double nearest=x<=0&&next>=0?0:Math.min(Math.abs(x),Math.abs(next));
            double z=Math.sqrt(Math.max(0,radius*radius-nearest*nearest));
            for(double y:new double[]{FLOOR,CEILING})walls.add(new AxisAlignedBB(
                    field.getX()+x,field.getY()+y-.2,field.getZ()-z,
                    field.getX()+next,field.getY()+y+.2,field.getZ()+z));
        }
        return walls;
    }

    private static List<AxisAlignedBB> prison(PetalField field){
        List<AxisAlignedBB> shell=new ArrayList<>(768);
        double radius=field.size(),height=radius*.75;
        for(int row=0;row<16;row++){
            double low=-Math.PI/2+row*Math.PI/16,high=low+Math.PI/16;
            double r1=Math.cos(low)*radius,r2=Math.cos(high)*radius;
            for(int i=0;i<48;i++){
                double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;
                double[] xs={Math.cos(a)*r1,Math.cos(b)*r1,Math.cos(a)*r2,Math.cos(b)*r2};
                double[] zs={Math.sin(a)*r1,Math.sin(b)*r1,Math.sin(a)*r2,Math.sin(b)*r2};
                shell.add(new AxisAlignedBB(field.getX()+Arrays.stream(xs).min().getAsDouble()-.08,
                        field.getY()+1.2+Math.sin(low)*height-.08,field.getZ()+Arrays.stream(zs).min().getAsDouble()-.08,
                        field.getX()+Arrays.stream(xs).max().getAsDouble()+.08,
                        field.getY()+1.2+Math.sin(high)*height+.08,field.getZ()+Arrays.stream(zs).max().getAsDouble()+.08));
            }
        }
        return shell;
    }

    private SenkeiBoundary(){}
}
