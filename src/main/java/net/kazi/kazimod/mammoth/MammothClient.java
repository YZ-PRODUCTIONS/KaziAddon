package net.kazi.kazimod.mammoth;

import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class MammothClient {
    private static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/models/zoan/mammoth_full.png");
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->{
        RenderingRegistry.registerEntityRenderingHandler(MammothFeatures.EFFECT.get(),EffectRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(MammothFeatures.WIND.get(),WindRenderer::new);
    });}
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean soft){return RenderType.create(soft?"mammoth_air":"mammoth_ground",DefaultVertexFormats.POSITION_COLOR,7,32768,false,true,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setWriteMaskState(soft?COLOR_WRITE:COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType SOLID=State.type(false),SOFT=State.type(true);
    public static void playVacuumSound(MammothVfxEntity effect){Minecraft.getInstance().getSoundManager().play(new VacuumSound(effect));}
    private static final class VacuumSound extends TickableSound {
        private final MammothVfxEntity effect;
        VacuumSound(MammothVfxEntity effect){
            super(SoundEvents.ELYTRA_FLYING,SoundCategory.PLAYERS);this.effect=effect;
            looping=true;delay=0;volume=.8F;pitch=.65F;
            x=effect.getX();y=effect.getY();z=effect.getZ();
        }
        @Override public void tick(){
            LivingEntity owner=effect.owner();
            if(effect.removed||effect.age()>=effect.life()||Minecraft.getInstance().level!=effect.level
                    ||owner==null||!owner.isAlive()||!MammothCombat.active(owner)){stop();return;}
            x=effect.getX();y=effect.getY();z=effect.getZ();
        }
    }
    private static MammothMesh.Sink sink(MatrixStack s,IRenderTypeBuffer buffers,boolean soft){
        IVertexBuilder b=buffers.getBuffer(soft?SOFT:SOLID);Vector4f v=new Vector4f(0,0,0,1);
        return(x,y,z,c,a)->{v.set((float)x,(float)y,(float)z,1);v.transform(s.last().pose());b.vertex(v.x(),v.y(),v.z()).color(c>>16&255,c>>8&255,c&255,(int)(MathHelper.clamp(a,0,1)*255)).endVertex();};
    }
    private static final class Ground {
        final float[] heights=new float[24];final int[] colors=new int[24];
        Ground(MammothVfxEntity e){for(int i=0;i<24;i++){
            double angle=Math.PI*2*i/24,r=e.radius()*(.23+.67*((i*17%23)/23.0));Vector3d p=e.position().add(Math.cos(angle)*r,4,Math.sin(angle)*r);
            BlockRayTraceResult hit=e.level.clip(new RayTraceContext(p,p.add(0,-10,0),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,e));
            heights[i]=hit.getType()==RayTraceResult.Type.BLOCK?(float)(hit.getLocation().y-e.getY()):0;
            colors[i]=hit.getType()==RayTraceResult.Type.BLOCK?e.level.getBlockState(hit.getBlockPos()).getMapColor(e.level,hit.getBlockPos()).col:0x827665;
        }}
    }
    public static final class EffectRenderer extends EntityRenderer<MammothVfxEntity> {
        private final Map<MammothVfxEntity,Ground> ground=new WeakHashMap<>();
        public EffectRenderer(EntityRendererManager m){super(m);}
        @Override public void render(MammothVfxEntity e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
            float age=e.age()+partial;if(age>=e.life())return;
            boolean far=entityRenderDispatcher.camera.getPosition().distanceToSqr(e.position())>64*64;
            Ground g=e.kind()==MammothVfxEntity.VACUUM?null:ground.computeIfAbsent(e,Ground::new);
            for(boolean soft:new boolean[]{false,true}){
                MammothMesh.Sink output=sink(s,b,soft);
                if(e.kind()==MammothVfxEntity.VACUUM){if(e.owner()!=null){Vector3d tip=MammothCombat.mouth(e.owner()).subtract(e.owner().position());MammothMesh.vacuum(output,age,e.life(),tip.x,tip.y,tip.z,soft,far);}}
                else MammothMesh.ground(output,age,e.life(),e.radius(),e.kind()==MammothVfxEntity.FINALE,soft,far,g.heights,g.colors);
            }
        }
        @Override public ResourceLocation getTextureLocation(MammothVfxEntity e){return TEXTURE;}
    }
    public static final class WindRenderer extends EntityRenderer<MammothWindEntity> {
        public WindRenderer(EntityRendererManager m){super(m);}
        @Override public void render(MammothWindEntity e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
            Vector3d v=e.getDeltaMovement().normalize();s.pushPose();
            s.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(v.x,v.z))));
            s.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.asin(MathHelper.clamp(v.y,-1,1)))));
            for(boolean soft:new boolean[]{false,true})MammothMesh.wind(sink(s,b,soft),e.tickCount+partial,soft);
            s.popPose();
        }
        @Override public ResourceLocation getTextureLocation(MammothWindEntity e){return TEXTURE;}
    }
}
