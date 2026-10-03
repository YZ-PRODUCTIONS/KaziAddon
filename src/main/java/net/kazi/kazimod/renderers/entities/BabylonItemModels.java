package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.*;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.registries.ForgeRegistries;

/** Same registered weapon families and item renderer as Reality Marble. */
final class BabylonItemModels {
    private static final List<Weapon> WEAPONS = new ArrayList<>();
    private static final BabylonClippedItemBuffer EMERGING = new BabylonClippedItemBuffer();
    private static final BoundsBuffer BOUNDS = new BoundsBuffer();

    private BabylonItemModels() { }

    static void render(int variant, float formed, MatrixStack stack, IRenderTypeBuffer buffers) {
        formed = unit(formed);
        if (formed <= 0.001F) return;
        renderPose(weapon(variant).stack, formed, 0D, stack, buffers);
    }

    /** Full-size item moves through local Z=0; cancellation draws it back through the same plane. */
    static void renderEmerging(int variant, float emergence, float retract,
                               MatrixStack stack, IRenderTypeBuffer buffers) {
        float visible = unit(emergence) * (1F - unit(retract));
        if (visible <= .0001F) return;
        Weapon weapon = weapon(variant);
        double distance = reach(weapon) * (1D - visible);
        // Capture the portal plane before the item's translation, rotations and scale are applied.
        EMERGING.begin(buffers, stack.last().pose());
        try {
            renderPose(weapon.stack, 1F, -distance, stack, EMERGING);
        } finally {
            EMERGING.end();
        }
    }

    private static Weapon weapon(int variant) {
        if (WEAPONS.isEmpty()) {
            List<Item> items = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem) items.add(item);
            }
            items.sort(Comparator.comparing(item -> item.getRegistryName().toString()));
            for (Item item : items) WEAPONS.add(new Weapon(new ItemStack(item)));
            if (WEAPONS.isEmpty()) WEAPONS.add(new Weapon(new ItemStack(Items.IRON_SWORD)));
        }
        return WEAPONS.get(Math.floorMod(variant, WEAPONS.size()));
    }

    private static void renderPose(ItemStack weapon, float scale, double travel,
                                   MatrixStack stack, IRenderTypeBuffer buffers) {
        stack.pushPose();
        try {
            stack.translate(0, 0, -0.65 + 1.25 * scale + travel);
            stack.mulPose(Vector3f.XP.rotationDegrees(90));
            stack.mulPose(Vector3f.ZP.rotationDegrees(45));
            stack.scale(3.75F * scale, 3.75F * scale, 3.75F * scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(weapon,
                    ItemCameraTransforms.TransformType.GROUND, 15728880,
                    OverlayTexture.NO_OVERLAY, stack, buffers);
        } finally {
            stack.popPose();
        }
    }

    private static double reach(Weapon weapon) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        IBakedModel model = renderer.getModel(weapon.stack, null, null);
        if (weapon.model != model) {
            BOUNDS.forward = 0D;
            // One dry render measures the exact item camera transform, including custom models.
            renderPose(weapon.stack, 1F, 0D, new MatrixStack(), BOUNDS);
            weapon.reach = Math.max(.05D, BOUNDS.forward + .015D);
            weapon.model = model;
        }
        return weapon.reach;
    }

    private static float unit(float value) {
        return Float.isNaN(value) ? 0F : Math.max(0F, Math.min(1F, value));
    }

    private static final class Weapon {
        final ItemStack stack;
        IBakedModel model;
        double reach;
        Weapon(ItemStack stack) { this.stack = stack; }
    }

    /** Bounds are sampled through the exact buffer path and refreshed after a model/resource reload. */
    private static final class BoundsBuffer implements IRenderTypeBuffer, IVertexBuilder {
        double forward;
        @Override public IVertexBuilder getBuffer(RenderType type) { return this; }
        @Override public IVertexBuilder vertex(double x, double y, double z) {
            if (Double.isFinite(z)) forward = Math.max(forward, z);
            return this;
        }
        @Override public IVertexBuilder color(int r, int g, int b, int a) { return this; }
        @Override public IVertexBuilder uv(float u, float v) { return this; }
        @Override public IVertexBuilder overlayCoords(int u, int v) { return this; }
        @Override public IVertexBuilder uv2(int u, int v) { return this; }
        @Override public IVertexBuilder normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
    }
}
