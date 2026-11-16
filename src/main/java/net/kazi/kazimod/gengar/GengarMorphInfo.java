package net.kazi.kazimod.gengar;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.api.morph.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer;
public final class GengarMorphInfo extends MorphInfo {
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/entities/mega_gengar.png");
    private static final EntitySize SIZE=EntitySize.scalable(1.9F,2.3F);
    private static final Map<Pose,EntitySize> SIZES=ImmutableMap.of(Pose.STANDING,SIZE,Pose.CROUCHING,SIZE,Pose.SWIMMING,SIZE,Pose.FALL_FLYING,SIZE);
    @Override public String getForm(){return "mega_gengar";}@Override public String getDisplayName(){return "Mega Gengar";}
    @Override public AkumaNoMiItem getDevilFruit(){return ModAbilities.HORO_HORO_NO_MI;}@Override public ResourceLocation getTexture(){return TEXTURE;}
    @Override @OnlyIn(Dist.CLIENT) public MorphModel getModel(){return new GengarModel<>();}
    @Override @OnlyIn(Dist.CLIENT) public IRenderFactory getRendererFactory(LivingEntity user){return manager->Cache.get(manager,this);}
    @OnlyIn(Dist.CLIENT) private static final class Cache {
        private static net.minecraft.client.renderer.entity.EntityRendererManager manager;private static ZoanMorphRenderer renderer;
        static ZoanMorphRenderer get(net.minecraft.client.renderer.entity.EntityRendererManager current,GengarMorphInfo info){
            if(renderer==null||manager!=current){manager=current;renderer=new ZoanMorphRenderer<>(current,info,false);}return renderer;
        }
    }
    @Override public Map<Pose,EntitySize> getSizes(){return SIZES;}@Override public double getEyeHeight(){return 1.8;}
    @Override public double getCameraZoom(LivingEntity user){return 7;}@Override public float getShadowSize(){return 1.5F;}
    @Override public boolean shouldRenderFirstPersonHand(){return false;}@Override public boolean shouldRenderFirstPersonLeg(){return false;}
    @Override public boolean canMount(){return false;}@Override public boolean hasEqualDepthTest(){return true;}
}
