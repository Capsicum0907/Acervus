package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BlockSurface {
    private static final double HALF = 0.5;
    private static final double LIFT = 0.01;
    private static final float WINDOW = 6.0F / 16.0F;
    private static final float OUTSIDE = 0.001F;
    private static final float WINDOW_START = 2.0F / 16.0F;
    private static final float WINDOW_END = 14.0F / 16.0F;

    private BlockSurface() {
    }

    public static void faceCamera(PoseStack pose, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 toward = camera.subtract(Vec3.atCenterOf(pos));
        double reach = Math.max(Math.abs(toward.x), Math.max(Math.abs(toward.y), Math.abs(toward.z)));
        Vec3 onSurface = reach > HALF
                ? toward.scale(HALF / reach).add(toward.normalize().scale(LIFT))
                : Vec3.ZERO;
        pose.translate(HALF + onSurface.x, HALF + onSurface.y, HALF + onSurface.z);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
    }

    public static boolean faceCameraOn(PoseStack pose, BlockPos pos, Direction face) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 toward = minecraft.gameRenderer.getMainCamera().getPosition().subtract(Vec3.atCenterOf(pos));
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        if (toward.dot(normal) <= HALF) {
            return false;
        }
        Vec3 onFace = normal.scale(HALF + LIFT);
        pose.translate(HALF + onFace.x, HALF + onFace.y, HALF + onFace.z);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        return true;
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
