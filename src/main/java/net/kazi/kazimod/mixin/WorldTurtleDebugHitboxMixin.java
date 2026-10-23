package net.kazi.kazimod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.worldturtle.WorldTurtleHitboxes;
import net.kazi.kazimod.worldturtle.WorldTurtlePartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRendererManager.class)
public abstract class WorldTurtleDebugHitboxMixin {
    @Inject(method="renderHitbox",at=@At("HEAD"),cancellable=true)
    private void hideOwnTurtleDebugBoxes(MatrixStack stack,IVertexBuilder buffer,Entity entity,float partial,CallbackInfo ci){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getCameraEntity()!=mc.player||!mc.options.getCameraType().isFirstPerson())return;
        boolean ownPart=entity instanceof WorldTurtlePartEntity&&((WorldTurtlePartEntity)entity).owner()==mc.player;
        boolean ownCore=entity==mc.player&&WorldTurtleHitboxes.active(mc.player);
        if(ownPart||ownCore)ci.cancel();
    }
}
