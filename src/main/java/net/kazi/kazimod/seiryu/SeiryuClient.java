package net.kazi.kazimod.seiryu;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
public final class SeiryuClient {
    private static final Map<LivingEntity,Pose> POSES=new WeakHashMap<>();
    private static Object level;
    static final class Pose {
        SeiryuSpine fallback = new SeiryuSpine();
        double scale=1,targetScale=1;
        Vector3d[] before, after, target;
        Vector3d lastHead;
        double[] headings, oldHeadings;
        int tick = Integer.MIN_VALUE, received;
        double neckTurn = Double.NaN;

        void receive(Vector3d head, Vector3d[] points, int tick) {
            receive(head,points,tick,1);
        }
        void receive(Vector3d head, Vector3d[] points, int tick, double packetScale) {
            targetScale=packetScale;
            target = relative(points, head);
            if (scale==packetScale && (after == null || lastHead != null && lastHead.distanceToSqr(head) > 1024))
                before = after = target;
            received = tick;
        }

        void advance(Vector3d head, Vector3d look, int age) {
            advance(head,look,age,1);
        }
        void advance(Vector3d head, Vector3d look, int age, double formScale) {
            if (tick == age && scale==formScale) return;
            boolean resized=scale!=formScale;
            if(resized){scale=formScale;fallback=new SeiryuSpine(scale);before=after=null;}
            if(targetScale!=scale)target=null;
            boolean reset = lastHead != null && lastHead.distanceToSqr(head) > 1024;
            if (reset) { target = null; before = after = null; }
            Vector3d[] desired = target != null && age - received < 20
                    ? target : relative(fallback.update(head, look), head);
            before = after == null ? desired : after;
            after = blend(before, desired, .5);
            // Length corrections happen once per tick, never recursively on every render frame.
            for (int i=1;i<after.length;i++)
                after[i]=after[i-1].add(SeiryuSpine.unit(after[i].subtract(after[i-1]), look.scale(-1))
                        .scale(SeiryuSpine.length(i-1)*scale));
            if (tick == Integer.MIN_VALUE || reset || resized) {
                neckTurn = Double.NaN;
                headings = null;
                before = after;
            }
            oldHeadings = headings;
            headings = SeiryuCurve.advanceHeadings(after, headings, Math.atan2(-look.x, look.z));
            if (oldHeadings == null) oldHeadings = headings;
            tick = age; lastHead = head;
        }
    }
    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e)->RenderingRegistry.registerEntityRenderingHandler(SeiryuFlight.PART.get(),manager->
                new EntityRenderer<SeiryuPartEntity>(manager) {
                    @Override public ResourceLocation getTextureLocation(SeiryuPartEntity entity){return new ResourceLocation("cartaddon","textures/models/zoanmorph/seiryu_fly_point.png");}
                }));
        MinecraftForge.EVENT_BUS.addListener(SeiryuClient::tick);
    }
    private static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        checkLevel();
        Iterator<Map.Entry<LivingEntity,Pose>> it=POSES.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<LivingEntity,Pose> entry=it.next();LivingEntity owner=entry.getKey();
            if(owner.removed || !SeiryuFlight.active(owner)){it.remove();continue;}
            entry.getValue().advance(owner.position(),owner.getLookAngle(),owner.tickCount,SeiryuFlight.scale(owner));
        }
    }
    private static void checkLevel() {
        Object current=Minecraft.getInstance().level;
        if(current!=level){POSES.clear();level=current;}
    }
    public static boolean localOwner(LivingEntity owner){return owner==Minecraft.getInstance().player;}
    public static void accept(SeiryuSpinePacket packet) {
        checkLevel();Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        Entity e=mc.level.getEntity(packet.owner);if(!(e instanceof LivingEntity))return;
        LivingEntity owner=(LivingEntity)e;
        POSES.computeIfAbsent(owner,k->new Pose()).receive(packet.head,packet.points,owner.tickCount,packet.scale);
    }
    private static Pose pose(LivingEntity owner) {
        Pose pose=POSES.computeIfAbsent(owner,k->new Pose());
        pose.advance(owner.position(),owner.getLookAngle(),owner.tickCount,SeiryuFlight.scale(owner));
        return pose;
    }
    public static Vector3d position(LivingEntity owner,float partial) {
        return new Vector3d(MathHelper.lerp(partial,owner.xo,owner.getX()),MathHelper.lerp(partial,owner.yo,owner.getY()),MathHelper.lerp(partial,owner.zo,owner.getZ()));
    }
    public static Vector3d[] points(LivingEntity owner,float partial) {
        Pose pose=pose(owner);
        Vector3d[] result=blend(pose.before,pose.after,MathHelper.clamp(partial,0,1));
        Vector3d origin=position(owner,partial);
        for(int i=0;i<result.length;i++)result[i]=result[i].add(origin);
        return result;
    }
    public static SeiryuCurve curve(LivingEntity owner,float partial) {
        Pose pose=pose(owner);
        double yaw=Math.toRadians(MathHelper.rotLerp(partial,owner.yRotO,owner.yRot));
        Vector3d right=new Vector3d(Math.cos(yaw),0,Math.sin(yaw));
        double[] headings=new double[pose.headings.length];
        for(int i=0;i<headings.length;i++)headings[i]=MathHelper.lerp(partial,pose.oldHeadings[i],pose.headings[i]);
        SeiryuCurve curve=new SeiryuCurve(points(owner,partial),headings,
                owner.getViewVector(partial),right,pose.neckTurn,pose.scale);
        pose.neckTurn=curve.neckTurn();
        return curve;
    }
    static Vector3d[] relative(Vector3d[] points,Vector3d origin) {
        Vector3d[] result=new Vector3d[points.length];
        for(int i=0;i<result.length;i++)result[i]=points[i].subtract(origin);
        return result;
    }
    static Vector3d[] blend(Vector3d[] a,Vector3d[] b,double t) {
        Vector3d[] result=new Vector3d[a.length];
        for(int i=0;i<result.length;i++)result[i]=a[i].add(b[i].subtract(a[i]).scale(t));
        return result;
    }
}
