package net.kazi.kazimod.mixin.client;

import java.util.Optional;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class ZoanTransitionRenderAccessors {
    @Mixin(ModelRenderer.class)
    public interface Children {
        @Accessor("children") ObjectList<ModelRenderer> kazimod$children();
    }
    @Mixin(targets = "net.minecraft.client.renderer.RenderType$Type")
    public interface Composite {
        @Accessor("state") RenderType.State kazimod$state();
    }
    @Mixin(RenderType.State.class)
    public interface State {
        @Accessor("textureState") RenderState.TextureState kazimod$textureState();
    }
    @Mixin(RenderState.TextureState.class)
    public interface Texture {
        @Accessor("texture") Optional<ResourceLocation> kazimod$texture();
    }
}
