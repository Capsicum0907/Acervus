package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

public final class BlockSurface {
    private static final double HALF = 0.5;
    private static final double LIFT = 0.01;
    private static final float WINDOW = 6.0F / 16.0F;
    private static final float OUTSIDE = 0.001F;
    private static final float WINDOW_START = 2.0F / 16.0F;
    private static final float WINDOW_END = 14.0F / 16.0F;

    private BlockSurface() {
    }

    public interface Label {
        void draw(PoseStack pose, int light);
    }

    public static void onEachSide(PoseStack pose, Level level, BlockPos pos, int fallbackLight, Label label) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(side);
            if (level != null && level.getBlockState(next).isSolidRender(level, next)) {
                continue;
            }
            onSide(pose, level, pos, side, fallbackLight, label);
        }
    }

    public static void onSide(PoseStack pose, Level level, BlockPos pos, Direction side, int fallbackLight,
            Label label) {
        int light = level == null ? fallbackLight : LevelRenderer.getLightColor(level, pos.relative(side));
        pose.pushPose();
        flatOn(pose, side);
        label.draw(pose, light);
        pose.popPose();
    }

    private static void flatOn(PoseStack pose, Direction face) {
        pose.translate(HALF, HALF, HALF);
        pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));
        pose.translate(0.0, 0.0, HALF + LIFT);
    }

    public static void fillWindows(PoseStack pose, MultiBufferSource buffers, Level level, BlockPos pos,
            TextureAtlasSprite sprite, int argb, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        int alpha = argb >>> 24;
        int red = argb >> 16 & 0xFF;
        int green = argb >> 8 & 0xFF;
        int blue = argb & 0xFF;
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (level != null && level.getBlockState(next).isSolidRender(level, next)) {
                continue;
            }
            pose.pushPose();
            pose.translate(HALF, HALF, HALF);
            pose.mulPose(side.getRotation());
            PoseStack.Pose last = pose.last();
            float y = (float) HALF + OUTSIDE;
            float u0 = sprite.getU(WINDOW_START);
            float u1 = sprite.getU(WINDOW_END);
            float v0 = sprite.getV(WINDOW_START);
            float v1 = sprite.getV(WINDOW_END);
            corner(consumer, last, -WINDOW, y, -WINDOW, u0, v0, red, green, blue, alpha, light);
            corner(consumer, last, -WINDOW, y, WINDOW, u0, v1, red, green, blue, alpha, light);
            corner(consumer, last, WINDOW, y, WINDOW, u1, v1, red, green, blue, alpha, light);
            corner(consumer, last, WINDOW, y, -WINDOW, u1, v0, red, green, blue, alpha, light);
            pose.popPose();
        }
    }

    private static void corner(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int red, int green, int blue, int alpha, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
