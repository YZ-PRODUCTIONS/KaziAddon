package net.kazi.kazimod.saiyan;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.worldturtle.WorldTurtleMesh.Sink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.common.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class SaiyanClient {
    private static final ForgeConfigSpec.IntValue HUD_X,HUD_Y;
    private static final ForgeConfigSpec.BooleanValue HUD;
    private static final ForgeConfigSpec CONFIG;
    static {
        ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();
        HUD=b.comment("Show the ki gauge for Saiyan fruit users.").define("showKiBar",true);
        HUD_X=b.comment("Horizontal offset from screen center.").defineInRange("kiBarX",-91,-4000,4000);
        HUD_Y=b.comment("Vertical offset from screen bottom; adjust to avoid other HUD mods.").defineInRange("kiBarY",-73,-4000,0);
        CONFIG=b.build();
    }
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean glow){return RenderType.create(glow?"saiyan_energy":"saiyan_dust",DefaultVertexFormats.POSITION_COLOR,7,65536,false,true,
                RenderType.State.builder().setTransparencyState(glow?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setShadeModelState(SMOOTH_SHADE).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                        .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
        static RenderType aura(boolean glow){
            return RenderType.create(glow?"saiyan_aura_glow":"saiyan_aura_shell",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,
                    RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                            .setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setFogState(NO_FOG)
                            .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
        }
        static RenderType space(boolean glow){
            return RenderType.create(glow?"saiyan_space_atmosphere":"saiyan_space_sky",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,
                    RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                            .setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setFogState(NO_FOG)
                            .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));
        }
        static RenderType earth(){
            return RenderType.create("saiyan_earth_surface",DefaultVertexFormats.POSITION_COLOR_TEX,7,524288,false,true,
                    RenderType.State.builder().setTextureState(new TextureState(new ResourceLocation("kazimod","textures/saiyan/earth_blue_marble.png"),true,false))
                            .setAlphaState(NO_ALPHA).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setFogState(NO_FOG)
                            .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));
        }
    }
    private static final RenderType ENERGY=State.type(true),SOLID=State.type(false);
    private static final RenderType AURA=State.aura(true),AURA_SHELL=State.aura(false);
    private static final RenderType SPACE_SKY=State.space(false),SPACE_GLOW=State.space(true);
    private static final RenderType EARTH_SURFACE=State.earth();
    private static final IRenderTypeBuffer.Impl SPACE_BUFFERS=IRenderTypeBuffer.immediate(new BufferBuilder(262144));
    private static final Map<Integer,KiSound> SOUNDS=new HashMap<>();
    private static final Map<Integer,InstinctTheme> THEMES=new HashMap<>();
    private static final Map<PlayerEntity,FlightFrame> FLIGHT_FRAMES=new WeakHashMap<>();
    private static final class FlightFrame {
        float yaw,pitch,oldYaw,oldPitch,speed,oldSpeed;
        FlightFrame(float yaw,float pitch){this.yaw=this.oldYaw=yaw;this.pitch=this.oldPitch=pitch;}
    }
    private static final List<SaiyanEffect> IMPACTS=new ArrayList<>();
    private static boolean flightVisible(PlayerEntity player,PlayerEntity local){
        if(player==local)return SaiyanState.get(player).getBoolean("Active")
                &&player.abilities.flying&&!player.isOnGround()&&!SaiyanFeatures.isActionLocked(player);
        return SaiyanState.get(player).getBoolean("Flight");
    }
    private static boolean valid(FlightFrame frame){
        return frame!=null&&Float.isFinite(frame.yaw)&&Float.isFinite(frame.oldYaw)
                &&Float.isFinite(frame.pitch)&&Float.isFinite(frame.oldPitch)
                &&Float.isFinite(frame.speed)&&Float.isFinite(frame.oldSpeed);
    }
    public static Sink sink(MatrixStack stack,IRenderTypeBuffer buffers,boolean glow){
        return sink(stack,buffers,glow?ENERGY:SOLID);
    }
    private static Sink sink(MatrixStack stack,IRenderTypeBuffer buffers,RenderType type){
        IVertexBuilder b=buffers.getBuffer(type);Matrix4f pose=stack.last().pose();
        return(x,y,z,c,a)->b.vertex(pose,x,y,z).color(c>>16&255,c>>8&255,c&255,MathHelper.clamp((int)(a*255),0,255)).endVertex();
    }
    public static void init(IEventBus bus){
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,CONFIG,"kazimod-saiyan-client.toml");
        bus.addListener((FMLClientSetupEvent e)->{
            RenderingRegistry.registerEntityRenderingHandler(SaiyanFeatures.PROJECTILE.get(),ProjectileRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(SaiyanFeatures.EFFECT.get(),EffectRenderer::new);
            e.enqueueWork(SaiyanModels::install);
        });
        MinecraftForge.EVENT_BUS.addListener(SaiyanClient::hud);
        MinecraftForge.EVENT_BUS.addListener(SaiyanClient::playerAura);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(EntityViewRenderEvent.FogColors e)->spaceFog(e));
        MinecraftForge.EVENT_BUS.addListener(SaiyanClient::audioTick);
        MinecraftForge.EVENT_BUS.addListener(SaiyanClient::impactFlash);
        MinecraftForge.EVENT_BUS.addListener(SaiyanClient::impactCamera);
        MinecraftForge.EVENT_BUS.addListener((InputUpdateEvent e)->{
            if(!SaiyanFeatures.isActionLocked(e.getPlayer()))return;
            net.minecraft.util.MovementInput input=e.getMovementInput();
            input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;
            input.up=false;input.down=false;input.left=false;input.right=false;
            e.getPlayer().setSprinting(false);e.getPlayer().setDeltaMovement(Vector3d.ZERO);
        });
    }
    public static void receive(SaiyanPacket packet){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||packet.data==null)return;
        Entity entity=mc.level.getEntity(packet.entity);
        if(entity instanceof LivingEntity){
            entity.getPersistentData().put(SaiyanState.TAG,packet.data);
            if(entity instanceof PlayerEntity&&!hasInstinctTheme((PlayerEntity)entity)){
                InstinctTheme theme=THEMES.remove(entity.getId());
                if(theme!=null)mc.getSoundManager().stop(theme);
            }
        }
    }
    private static void hud(RenderGameOverlayEvent.Post e){
        Minecraft mc=Minecraft.getInstance();PlayerEntity p=mc.player;
        if(e.getType()!=RenderGameOverlayEvent.ElementType.ALL||p==null||p.isSpectator()||!HUD.get()||mc.options.hideGui)return;
        CompoundNBT state=SaiyanState.get(p);if(!state.getBoolean("Active"))return;
        int x=e.getWindow().getGuiScaledWidth()/2+HUD_X.get(),y=e.getWindow().getGuiScaledHeight()+HUD_Y.get();
        x=MathHelper.clamp(x,2,Math.max(2,e.getWindow().getGuiScaledWidth()-184));y=MathHelper.clamp(y,12,e.getWindow().getGuiScaledHeight()-10);
        MatrixStack s=e.getMatrixStack();float fraction=SaiyanState.ki(p)/SaiyanState.maxKi(p);
        int color=0xff000000|SaiyanBalance.COLORS[SaiyanState.form(p)];
        AbstractGui.fill(s,x-1,y-1,x+183,y+8,0xffb5cad8);AbstractGui.fill(s,x,y,x+182,y+7,0xff101920);
        AbstractGui.fill(s,x+1,y+1,x+1+Math.round(180*fraction),y+6,color);
        AbstractGui.fill(s,x+1,y+1,x+1+Math.round(180*fraction),y+2,0xffe7fcff);
        for(int i=1;i<10;i++)AbstractGui.fill(s,x+i*18,y+2,x+i*18+1,y+6,0x55323f47);
        String text="Ki "+Math.round(SaiyanState.ki(p))+" / "+SaiyanState.maxKi(p);
        mc.font.drawShadow(s,text,x,y-10,0xffe9fbff);
        String form=SaiyanBalance.FORMS[SaiyanState.form(p)];mc.font.drawShadow(s,form,x+182-mc.font.width(form),y-10,color);
    }
    private static void playerAura(RenderPlayerEvent.Post event){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        PlayerEntity p=event.getPlayer();
        CompoundNBT data=SaiyanState.get(p);if(!data.getBoolean("Active")||p.isInvisible()||p.distanceToSqr(mc.player)>64*64)return;
        if(p==mc.player&&mc.options.getCameraType().isFirstPerson())return;
        float partial=event.getPartialRenderTick();
        Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer buffers=event.getBuffers();MatrixStack stack=event.getMatrixStack();
        // Use the same view, render target and buffer lifecycle as the visible player model.
        stack.pushPose();
        try{
            boolean charging=data.getBoolean("Charging"),flying=flightVisible(p,mc.player);int form=SaiyanState.form(p);
            if(!p.isAlive()||form==0&&!charging&&!flying)return;
            double x=MathHelper.lerp(partial,p.xOld,p.getX()),y=MathHelper.lerp(partial,p.yOld,p.getY()),z=MathHelper.lerp(partial,p.zOld,p.getZ());
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return;
            float age=p.tickCount+partial,powerUp=SaiyanState.action(p)==10?(float)(p.level.getGameTime()-data.getLong("ActionStart"))+partial:-1;
            if(powerUp>=0)powerUp=SaiyanBalance.transformationTime(form,powerUp);
            if(flying){
                // A bad travel frame must not strand either aura until the next login.
                FlightFrame frame=FLIGHT_FRAMES.get(p);
                if(!valid(frame)){FLIGHT_FRAMES.remove(p);frame=null;}
                double yaw=Math.toRadians(frame==null?-p.yRot:MathHelper.lerp(partial,frame.oldYaw,frame.yaw));
                double pitch=Math.toRadians(frame==null?p.xRot:MathHelper.lerp(partial,frame.oldPitch,frame.pitch));
                float speed=frame==null?0:MathHelper.lerp(partial,frame.oldSpeed,frame.speed);
                if(!Double.isFinite(yaw)||!Double.isFinite(pitch)){yaw=0;pitch=0;}
                stack.pushPose();stack.translate(0,p.getBbHeight()*.55,0);
                stack.mulPose(Vector3f.YP.rotation((float)yaw));stack.mulPose(Vector3f.XP.rotation((float)pitch));
                Vector3d view=camera.subtract(x,y+p.getBbHeight()*.55,z).normalize();
                double vx=Math.cos(yaw)*view.x-Math.sin(yaw)*view.z,vz=Math.sin(yaw)*view.x+Math.cos(yaw)*view.z;
                float vy=(float)(Math.cos(pitch)*view.y+Math.sin(pitch)*vz);
                float localZ=(float)(-Math.sin(pitch)*view.y+Math.cos(pitch)*vz);
                SaiyanMesh.flightAura(sink(stack,buffers,AURA_SHELL),age,speed,(float)vx,vy,localZ,false);
                flushAura(buffers,AURA_SHELL);
                SaiyanMesh.flightAura(sink(stack,buffers,AURA),age,speed,(float)vx,vy,localZ,true);
                stack.popPose();
            }
            if(form>0)SaiyanMesh.transformationAura(sink(stack,buffers,AURA),age,form,1,powerUp);
            else if(charging)SaiyanMesh.aura(sink(stack,buffers,AURA),age,SaiyanBalance.COLORS[form],1,false);
            flushAura(buffers,AURA);
            if(powerUp>=0){SaiyanMesh.transformationDust(sink(stack,buffers,AURA_SHELL),powerUp);flushAura(buffers,AURA_SHELL);}
        }finally{
            stack.popPose();
        }
    }
    private static void flushAura(IRenderTypeBuffer buffers,RenderType type){
        if(buffers instanceof IRenderTypeBuffer.Impl)((IRenderTypeBuffer.Impl)buffers).endBatch(type);
    }
    public static void beam(LightningEntity beam,float partial,MatrixStack stack,IRenderTypeBuffer buffers){
        stack.pushPose();stack.mulPose(Vector3f.YN.rotationDegrees(beam.yRot));stack.mulPose(Vector3f.XP.rotationDegrees(beam.xRot));
        float visualOffset=Math.min(1.6F,Math.max(0,beam.getLength()-.1F));
        stack.translate(0,0,visualOffset);
        Sink visual=SaiyanMesh.tint(sink(stack,buffers,true),beam.getColor()&0xffffff);
        Minecraft mc=Minecraft.getInstance();
        if(beam.getOwner()==mc.player&&mc.options.getCameraType().isFirstPerson()){
            Sink full=visual;visual=(x,y,z,c,a)->full.vertex(x,y,z,c,a*.12F);
        }
        SaiyanMesh.beam(visual,Math.max(0,beam.getLength()-visualOffset),beam.getSize(),beam.tickCount+partial);stack.popPose();
    }
    private static int soundKind(PlayerEntity p){
        if(!p.isAlive()||!SaiyanState.get(p).getBoolean("Active"))return 0;
        int action=SaiyanState.action(p);
        return action==2?2:action==3?3:action==9?1:0;
    }
    private static boolean hasInstinctTheme(PlayerEntity player){
        return player.isAlive()&&!player.removed&&!player.isSpectator()&&SaiyanState.get(player).getBoolean("Active")
                &&SaiyanState.form(player)==SaiyanBalance.ULTRA_INSTINCT;
    }
    private static void audioTick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();
        Iterator<KiSound> iterator=SOUNDS.values().iterator();
        while(iterator.hasNext()){
            KiSound sound=iterator.next();
            if(mc.level==null||sound.player.level!=mc.level||sound.player.removed||soundKind(sound.player)!=sound.kind){mc.getSoundManager().stop(sound);iterator.remove();}
        }
        Iterator<InstinctTheme> themes=THEMES.values().iterator();
        while(themes.hasNext()){
            InstinctTheme theme=themes.next();
            if(mc.level==null||mc.player==null||theme.player.level!=mc.level||!hasInstinctTheme(theme.player)
                    ||theme.player.distanceToSqr(mc.player)>48*48){mc.getSoundManager().stop(theme);themes.remove();}
        }
        if(mc.level==null||mc.player==null){IMPACTS.clear();FLIGHT_FRAMES.clear();return;}
        SaiyanModels.tick();
        FLIGHT_FRAMES.keySet().removeIf(p->p.removed||p.level!=mc.level||!flightVisible(p,mc.player));
        for(PlayerEntity p:mc.level.players())if(flightVisible(p,mc.player)){
            Vector3d motion=new Vector3d(p.getX()-p.xOld,p.getY()-p.yOld,p.getZ()-p.zOld);
            double speed=motion.length();Vector3d direction=speed>.035?motion.normalize():p.getLookAngle();
            if(!Double.isFinite(speed)||!Double.isFinite(direction.x)||!Double.isFinite(direction.y)||!Double.isFinite(direction.z)){
                FLIGHT_FRAMES.remove(p);continue;
            }
            float yaw=(float)Math.toDegrees(Math.atan2(direction.x,direction.z));
            float pitch=(float)-Math.toDegrees(Math.asin(MathHelper.clamp(direction.y,-1,1)));
            FlightFrame frame=FLIGHT_FRAMES.computeIfAbsent(p,k->new FlightFrame(yaw,pitch));
            frame.oldYaw=frame.yaw;frame.oldPitch=frame.pitch;frame.oldSpeed=frame.speed;
            frame.yaw+=MathHelper.wrapDegrees(yaw-frame.yaw)*.35F;
            frame.pitch=MathHelper.lerp(.35F,frame.pitch,pitch);frame.speed=MathHelper.lerp(.25F,frame.speed,(float)speed);
            if(!valid(frame))FLIGHT_FRAMES.remove(p);
        }
        if(mc.player.tickCount%4==0){
            IMPACTS.clear();
            for(Entity entity:mc.level.entitiesForRendering())
                if(entity instanceof SaiyanEffect&&(((SaiyanEffect)entity).kind()==SaiyanEffect.SPIRIT_IMPACT||((SaiyanEffect)entity).kind()==SaiyanEffect.FINISHER_IMPACT)
                        &&entity.distanceToSqr(mc.player)<192*192)IMPACTS.add((SaiyanEffect)entity);
        }
        for(PlayerEntity p:mc.level.players()){
            if(hasInstinctTheme(p)&&p.distanceToSqr(mc.player)<=48*48&&!THEMES.containsKey(p.getId())){
                InstinctTheme theme=new InstinctTheme(p);THEMES.put(p.getId(),theme);mc.getSoundManager().play(theme);
            }
            int kind=soundKind(p);if(kind==0||p.distanceToSqr(mc.player)>48*48||SOUNDS.containsKey(p.getId()))continue;
            KiSound sound=new KiSound(p,kind);SOUNDS.put(p.getId(),sound);mc.getSoundManager().play(sound);
        }
    }
    private static final class InstinctTheme extends TickableSound {
        final PlayerEntity player;
        InstinctTheme(PlayerEntity player){
            super(SaiyanFeatures.INSTINCT_THEME.get(),SoundCategory.PLAYERS);
            this.player=player;looping=true;delay=0;volume=.6F;pitch=1;
            x=player.getX();y=player.getY();z=player.getZ();
        }
        @Override public void tick(){
            Minecraft mc=Minecraft.getInstance();
            if(mc.level==null||mc.player==null||player.level!=mc.level||!hasInstinctTheme(player)
                    ||player.distanceToSqr(mc.player)>48*48){stop();return;}
            x=player.getX();y=player.getY();z=player.getZ();
        }
    }
    private static float impactStrength(float partial,boolean flash){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return 0;
        float strength=0;
        for(SaiyanEffect effect:IMPACTS){
            if(!effect.isAlive()||effect.level!=mc.level)continue;
            float age=effect.age()+partial,distance=effect.distanceTo(mc.player);
            boolean finisher=effect.kind()==SaiyanEffect.FINISHER_IMPACT;
            float range=finisher?48:flash?150:110;
            float falloff=MathHelper.clamp(1-distance/range,0,1);
            float decay=1-smooth(age/(finisher?(flash?7:24):(flash?20:48)));
            strength=Math.max(strength,falloff*falloff*decay);
        }
        return strength;
    }
    private static void impactFlash(RenderGameOverlayEvent.Post event){
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;
        int alpha=(int)(impactStrength(event.getPartialTicks(),true)*190);
        if(alpha>0)AbstractGui.fill(event.getMatrixStack(),0,0,event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),(alpha<<24)|0xd9f8ff);
    }
    private static void impactCamera(EntityViewRenderEvent.CameraSetup event){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        CompoundNBT cinematic=SaiyanState.get(mc.player);
        if(SaiyanFeatures.isFinisherActive(mc.player)&&cinematic.getInt("Finisher")==2){
            int phase=cinematic.getInt("FinisherPhase");
            float age=(float)(mc.level.getGameTime()-cinematic.getLong("FinisherStart")+event.getRenderPartialTicks());
            if(phase>=SaiyanFinisherMotion.ASCEND){
                float pitch=SaiyanFinisherMotion.meteorPitch(phase,age);
                event.setYaw(cinematic.getFloat("FinisherYaw"));event.setPitch(pitch);
            }
        }
        float power=Math.max(impactStrength((float)event.getRenderPartialTicks(),false)*1.8F,
                transformationShake((float)event.getRenderPartialTicks()));
        double t=mc.player.tickCount+event.getRenderPartialTicks();
        event.setPitch(event.getPitch()+(float)Math.sin(t*2.31)*power);
        event.setYaw(event.getYaw()+(float)Math.sin(t*1.73)*power*.55F);
        event.setRoll(event.getRoll()+(float)Math.sin(t*1.19)*power*.3F);
    }
    public static float spaceOpacity(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!SaiyanFeatures.isFinisherActive(mc.player))return 0;
        CompoundNBT n=SaiyanState.get(mc.player);
        return n.getInt("Finisher")==2?SaiyanFinisherMotion.spaceOpacity(n.getInt("FinisherPhase"),n.getFloat("FinisherSpace")):0;
    }
    private static void spaceFog(EntityViewRenderEvent.FogColors event){
        float alpha=spaceOpacity();if(alpha<=0)return;
        event.setRed(MathHelper.lerp(alpha,event.getRed(),1/255F));
        event.setGreen(MathHelper.lerp(alpha,event.getGreen(),3/255F));
        event.setBlue(MathHelper.lerp(alpha,event.getBlue(),11/255F));
    }
    public static void renderSpaceSky(MatrixStack stack,float partial){
        float opacity=spaceOpacity();if(opacity<=.001F)return;
        Minecraft mc=Minecraft.getInstance();CompoundNBT n=SaiyanState.get(mc.player);
        int phase=n.getInt("FinisherPhase");double age=mc.level.getGameTime()-n.getLong("FinisherStart")+partial;
        // Draw before terrain using the actual view matrix, on every render-distance setting.
        stack.pushPose();stack.scale(.25F,.25F,.25F);
        boolean fog=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_FOG);
        com.mojang.blaze3d.systems.RenderSystem.disableFog();
        try{
            SaiyanSpaceMesh.backdrop(sink(stack,SPACE_BUFFERS,SPACE_SKY),opacity);SPACE_BUFFERS.endBatch(SPACE_SKY);
            double yaw=Math.toRadians(n.getFloat("FinisherYaw"));
            float pitch=phase==SaiyanFinisherMotion.DIVE?SaiyanFinisherMotion.meteorPitch(phase,age):35;
            double angle=Math.toRadians(pitch),horizontal=115*Math.cos(angle);
            stack.translate(-Math.sin(yaw)*horizontal,-115*Math.sin(angle),Math.cos(yaw)*horizontal);
            stack.mulPose(Vector3f.YP.rotationDegrees(180-n.getFloat("FinisherYaw")));
            stack.mulPose(Vector3f.XP.rotationDegrees(-pitch));
            float radius=SaiyanFinisherMotion.earthRadius(phase,age);stack.scale(radius,radius,radius);
            IVertexBuilder globe=SPACE_BUFFERS.getBuffer(EARTH_SURFACE);Matrix4f matrix=stack.last().pose();
            SaiyanSpaceMesh.earth((x,y,z,u,v,c,a)->globe.vertex(matrix,x,y,z)
                    .color(c>>16&255,c>>8&255,c&255,(int)(a*255)).uv(u,v).endVertex(),opacity);
            SPACE_BUFFERS.endBatch(EARTH_SURFACE);
            // The limb follows the sphere's projected silhouette as Earth grows during re-entry.
            float ratio=radius/115,limb=(float)Math.sqrt(1-ratio*ratio);
            stack.translate(0,0,ratio);stack.scale(limb,limb,1);
            SaiyanSpaceMesh.atmosphere(sink(stack,SPACE_BUFFERS,SPACE_GLOW),opacity);SPACE_BUFFERS.endBatch(SPACE_GLOW);
        }finally{
            stack.popPose();
            if(fog)com.mojang.blaze3d.systems.RenderSystem.enableFog();
        }
    }
    private static float transformationShake(float partial){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return 0;
        float strength=0;
        for(PlayerEntity player:mc.level.players()){
            CompoundNBT state=SaiyanState.get(player);
            if(!player.isAlive()||!state.getBoolean("Active")||SaiyanState.action(player)!=10)continue;
            int form=SaiyanState.form(player);
            float elapsed=(float)(mc.level.getGameTime()-state.getLong("ActionStart"))+partial;
            float range=form==SaiyanBalance.ULTRA_INSTINCT?48:24;
            float falloff=player==mc.player?1:MathHelper.clamp(1-player.distanceTo(mc.player)/range,0,1);
            strength=Math.max(strength,SaiyanBalance.transformationShake(form,elapsed)*falloff*falloff);
        }
        return strength;
    }
    private static final class KiSound extends TickableSound{
        final PlayerEntity player;final int kind;
        KiSound(PlayerEntity player,int kind){
            super(kind==2?SaiyanFeatures.CHARGE.get():kind==3?SaiyanFeatures.BEAM.get():SaiyanFeatures.POWER_UP.get(),SoundCategory.PLAYERS);
            this.player=player;this.kind=kind;looping=kind==1;delay=0;volume=kind==3?.75F:.45F;pitch=1;
            x=player.getX();y=player.getY();z=player.getZ();
        }
        public void tick(){x=player.getX();y=player.getY();z=player.getZ();if(!player.isAlive()||soundKind(player)!=kind)stop();}
    }
    private static final class ProjectileRenderer extends EntityRenderer<SaiyanProjectile>{
        ProjectileRenderer(EntityRendererManager manager){super(manager);}
        public void render(SaiyanProjectile e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
            Sink s=sink(stack,buffers,true);int color=e.kind()==2?0x66bcff:0xffcf36;
            if(e.form()>0){color=SaiyanBalance.COLORS[e.form()];s=SaiyanMesh.tint(s,color);}
            SaiyanMesh.orb(s,e.size(),color,1,e.tickCount+partial);
            Vector3d trail=e.getDeltaMovement().normalize().scale(-Math.min(5,e.tickCount*.3));
            SaiyanMesh.line(s,0,0,0,(float)trail.x,(float)trail.y,(float)trail.z,e.size()*.35F,color,.55F);
        }
        public ResourceLocation getTextureLocation(SaiyanProjectile e){return SaiyanAbility.icon("ki_blast");}
    }
    private static final class EffectRenderer extends EntityRenderer<SaiyanEffect>{
        EffectRenderer(EntityRendererManager manager){super(manager);}
        public void render(SaiyanEffect e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
            float age=e.age()+partial,progress=Math.min(1,age/e.life()),fade=Math.min(1,(e.life()-age)/6);LivingEntity owner=e.owner();
            stack.pushPose();
            Sink energy=sink(stack,buffers,true);
            if(e.form()>0)energy=SaiyanMesh.tint(energy,SaiyanBalance.COLORS[e.form()]);
            switch(e.kind()){
                case 0:
                    SaiyanMesh.aura(sink(stack,buffers,true),age,owner==null?0xffdf45:SaiyanBalance.COLORS[SaiyanState.form(owner)],fade,true);
                    SaiyanMesh.ring(sink(stack,buffers,true),.08F,progress*e.size()*2,.1F,0xffef9f,(1-progress)*.7F,0);break;
                case 1:
                    if(owner==null)break;
                    Vector3d point=SaiyanAbility.kamehamehaOrigin(owner).subtract(e.position());
                    stack.translate(point.x,point.y,point.z);
                    Sink charge=coloredSink(stack,buffers,e.form());
                    Minecraft mc=Minecraft.getInstance();
                    if(owner==mc.player&&mc.options.getCameraType().isFirstPerson()){
                        Sink full=charge;charge=(x,y,z,c,a)->full.vertex(x,y,z,c,a*.12F);
                    }
                    SaiyanMesh.orb(charge,.12F+progress*.6F,0x3faaff,fade,age);break;
                case 2:
                    stack.translate(0,8,0);energy=coloredSink(stack,buffers,e.form());SaiyanMesh.orb(energy,e.size()*(.15F+progress*.85F),0x59bdff,fade,age);
                    for(int i=0;i<10;i++){
                        double a=i*Math.PI/5;float phase=(age*.025F+i*.1F)%1;
                        SaiyanMesh.line(energy,(float)Math.cos(a)*(12*(1-phase)),(1-phase)*-5,(float)Math.sin(a)*(12*(1-phase)),
                                (float)Math.cos(a)*(10*(1-phase)),(1-phase)*-4,(float)Math.sin(a)*(10*(1-phase)),.035F,0xd8f8ff,.7F);
                    }break;
                case 3:
                    if(owner!=null){
                        stack.translate(MathHelper.lerp(partial,owner.xOld,owner.getX())-MathHelper.lerp(partial,e.xOld,e.getX()),
                                MathHelper.lerp(partial,owner.yOld,owner.getY())-MathHelper.lerp(partial,e.yOld,e.getY())+owner.getBbHeight()*.6,
                                MathHelper.lerp(partial,owner.zOld,owner.getZ())-MathHelper.lerp(partial,e.zOld,e.getZ()));
                        stack.mulPose(Vector3f.YN.rotationDegrees(owner.yRot));stack.mulPose(Vector3f.XP.rotationDegrees(owner.xRot));
                    }
                    SaiyanModels.dragon(stack,buffers,age,fade,e.form());break;
                case SaiyanEffect.INSTINCT_DRAGON:
                    boolean anchored=e.target()>=0;
                    Entity captive=anchored?e.level.getEntity(e.target()):owner;
                    if(captive!=null){
                        stack.translate(MathHelper.lerp(partial,captive.xOld,captive.getX())-MathHelper.lerp(partial,e.xOld,e.getX()),
                                MathHelper.lerp(partial,captive.yOld,captive.getY())-MathHelper.lerp(partial,e.yOld,e.getY())+captive.getBbHeight()*(anchored?.5:.6),
                                MathHelper.lerp(partial,captive.zOld,captive.getZ())-MathHelper.lerp(partial,e.zOld,e.getZ()));
                    }
                    stack.mulPose(Vector3f.YN.rotationDegrees(e.carryYaw()));
                    float pitch=e.carryPitch();
                    if(e.phase()==SaiyanFinisherMotion.ASCEND)pitch=MathHelper.lerp(smooth((e.phaseAge()+partial)/4),0,-90);
                    if(e.phase()==SaiyanFinisherMotion.DIVE)pitch=MathHelper.lerp(smooth((e.phaseAge()+partial)/4),-90,90);
                    stack.mulPose(Vector3f.XP.rotationDegrees(pitch));
                    float bite=anchored?(e.phase()==SaiyanFinisherMotion.BITE?smooth((e.phaseAge()+partial)/5):1):0;
                    SaiyanModels.dragon(stack,buffers,age,fade,e.form(),SaiyanFinisherMotion.DRAGON_SCALE,bite,anchored);break;
                case SaiyanEffect.FINISHER_IMPACT:
                    SaiyanSpaceMesh.impact(sink(stack,buffers,AURA),age,e.size());break;
                case SaiyanEffect.METEOR_REENTRY:
                    if(e.phase()!=SaiyanFinisherMotion.DIVE)break;
                    reentryBody(e,owner,partial,stack,buffers);
                    Entity grabbed=e.level.getEntity(e.target());
                    if(grabbed!=owner)reentryBody(e,grabbed,partial,stack,buffers);
                    break;
                case 4:
                    float r=e.size()*(float)Math.sin(Math.min(1,progress*1.5)*Math.PI*.5);
                    SaiyanMesh.orb(energy,Math.max(.1F,r),0xffdd69,(1-progress)*.75F,age);
                    SaiyanMesh.ring(energy,.1F,r*1.3F,.1F,0xf1fcff,(1-progress),0);break;
                case SaiyanEffect.SPIRIT_IMPACT:
                    SaiyanMesh.spiritImpact(energy,age,e.size());break;
                case SaiyanEffect.FLIGHT_AURA:
                case SaiyanEffect.TRANSFORMATION_AURA:
                    // Kept for compatibility with effect IDs; player auras render once in world().
                    break;
                case SaiyanEffect.INSTINCT_DODGE:
                    SaiyanMesh.instinctDodge(energy,age,progress);break;
                case 5:
                    for(int i=0;i<9;i++){float x=(i-4)*.09F;SaiyanMesh.line(energy,x,0,0,x,2.2F,0,.012F,0xd3e9ff,fade);}
                    break;
                default:break;
            }
            stack.popPose();
        }
        public ResourceLocation getTextureLocation(SaiyanEffect e){return SaiyanAbility.icon("ki_charge");}
    }
    private static void reentryBody(SaiyanEffect effect,Entity body,float partial,MatrixStack stack,IRenderTypeBuffer buffers){
        if(body==null||!body.isAlive())return;
        Minecraft mc=Minecraft.getInstance();
        boolean inside=body==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson();
        float radius=MathHelper.clamp(body.getBbWidth()*.6F+.35F,.55F,8),height=MathHelper.clamp(body.getBbHeight(),.6F,12);
        float age=effect.phaseAge()+partial,heat=smooth(age/3)*(inside?.28F:1);
        stack.pushPose();
        try{
            stack.translate(MathHelper.lerp(partial,body.xOld,body.getX())-MathHelper.lerp(partial,effect.xOld,effect.getX()),
                    MathHelper.lerp(partial,body.yOld,body.getY())-MathHelper.lerp(partial,effect.yOld,effect.getY())+body.getBbHeight()*.5,
                    MathHelper.lerp(partial,body.zOld,body.getZ())-MathHelper.lerp(partial,effect.zOld,effect.getZ()));
            SaiyanReentryMesh.render(sink(stack,buffers,AURA_SHELL),age,radius,height,heat,true);
            flushAura(buffers,AURA_SHELL);
            SaiyanReentryMesh.render(sink(stack,buffers,AURA),age,radius,height,heat,false);
            flushAura(buffers,AURA);
        }finally{stack.popPose();}
    }
    private static Sink coloredSink(MatrixStack stack,IRenderTypeBuffer buffers,int form){
        Sink out=sink(stack,buffers,true);
        return form>0?SaiyanMesh.tint(out,SaiyanBalance.COLORS[SaiyanBalance.tier(form)]):out;
    }
    public static void pose(PlayerModel<?> model,LivingEntity player,float age){
        CompoundNBT state=SaiyanState.get(player);if(!state.getBoolean("Active"))return;
        int action=SaiyanState.action(player);float t=(float)(player.level.getGameTime()-state.getLong("ActionStart"));
        t+=age-player.tickCount;
        if(action==10)t=SaiyanBalance.transformationTime(SaiyanState.form(player),t);
        float blend=MathHelper.clamp(t/5,0,1);blend=blend*blend*(3-2*blend);
        float rx=0,lx=0,ry=0,ly=0,rz=0,lz=0;boolean override=true;
        switch(action){
            case 1:rx=lx=-.3F;rz=.22F;lz=-.22F;break;
            case 2:rx=-1.1F;lx=-1.25F;ry=-.8F;ly=-.45F;rz=-.35F;lz=.55F;break;
            case 3:rx=lx=-1.58F;ry=-.18F;ly=.18F;break;
            case 4:rx=lx=-3.05F;rz=-.15F;lz=.15F;break;
            case 5:case 9:rx=-1.5F;ry=-.08F;lx=-.3F;break;
            case 6:rx=-1.65F;lx=-.9F;ry=-.12F;ly=.22F;rz=-.15F;lz=.1F;break;
            case 11:
                rx=lx=-1.35F;ry=-.38F;ly=.38F;rz=-.18F;lz=.18F;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,-.12F);
                model.rightLeg.xRot=MathHelper.lerp(blend,model.rightLeg.xRot,-.5F);model.leftLeg.xRot=MathHelper.lerp(blend,model.leftLeg.xRot,.25F);break;
            case 12:
                rx=lx=-.6F;ry=-.3F;ly=.3F;rz=-.12F;lz=.12F;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,.55F);
                model.rightLeg.xRot=MathHelper.lerp(blend,model.rightLeg.xRot,.4F);model.leftLeg.xRot=MathHelper.lerp(blend,model.leftLeg.xRot,.3F);break;
            case 13:
                float recover=1-smooth(t/12);rx=lx=-.4F*recover;
                model.body.xRot=.5F*recover;model.rightLeg.xRot=-.45F*recover;model.leftLeg.xRot=.3F*recover;break;
            case 7:rx=-1.7F;lx=-.7F;break;
            case 14:
                rx=-1.7F;lx=-.65F;ry=-.1F;ly=.35F;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,.16F);break;
            case 15:
                rx=lx=-.65F;ry=-.42F;ly=.42F;rz=-.18F;lz=.18F;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,-.28F);
                model.rightLeg.xRot=-.4F;model.leftLeg.xRot=.2F;break;
            case 16:
                rx=lx=-.65F;ry=-.35F;ly=.35F;rz=-.12F;lz=.12F;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,.9F);
                model.rightLeg.xRot=.5F;model.leftLeg.xRot=.45F;break;
            case 8:rx=-2.1F;ry=-.35F;rz=.2F;break;
            case 10:
                float lift=smooth((t-5)/9),release=smooth((t-15)/7),settle=smooth((t-25)/15);
                rx=lx=MathHelper.lerp(release,MathHelper.lerp(lift,-1.55F,-3.12F),-.22F);
                ry=MathHelper.lerp(lift,-.85F,-.1F)*(1-release);ly=-ry;
                rz=MathHelper.lerp(release,.28F*(1-lift),-.5F+.22F*settle);lz=-rz;
                model.body.xRot=MathHelper.lerp(blend,model.body.xRot,.16F*(1-release)-.08F*release*(1-settle));
                model.head.xRot=MathHelper.lerp(blend,model.head.xRot,MathHelper.lerp(release,.2F,-.22F)*(1-settle));
                model.rightLeg.zRot=MathHelper.lerp(blend,model.rightLeg.zRot,.12F*(1-.5F*settle));
                model.leftLeg.zRot=-model.rightLeg.zRot;
                model.rightLeg.xRot=MathHelper.lerp(blend,model.rightLeg.xRot,-.12F*(1-settle));
                model.leftLeg.xRot=model.rightLeg.xRot;
                model.jacket.copyFrom(model.body);model.hat.copyFrom(model.head);
                model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
                break;
            default:override=false;
        }
        if(!override)return;
        model.rightArm.xRot=MathHelper.lerp(blend,model.rightArm.xRot,rx);model.leftArm.xRot=MathHelper.lerp(blend,model.leftArm.xRot,lx);
        model.rightArm.yRot=MathHelper.lerp(blend,model.rightArm.yRot,ry);model.leftArm.yRot=MathHelper.lerp(blend,model.leftArm.yRot,ly);
        model.rightArm.zRot=MathHelper.lerp(blend,model.rightArm.zRot,rz);model.leftArm.zRot=MathHelper.lerp(blend,model.leftArm.zRot,lz);
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.jacket.copyFrom(model.body);model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
    }
    private static float smooth(float t){t=MathHelper.clamp(t,0,1);return t*t*(3-2*t);}
}
