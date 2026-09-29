/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniondc
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartEditOutlineClient {
    private static final String OUTLINER_KEY = "coaster_cart_edit_active";
    private static final int OUTLINE_COLOR_CONNECTED = 0xFFDD55;
    private static final int OUTLINE_COLOR_UNCONNECTED = 0x888888;

    private CoasterCartEditOutlineClient() {
    }

    @Deprecated
    public static void clientTick(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        CoasterCartEditOutlineClient.renderFrame(mc, AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level));
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        Outliner outliner = Outliner.getInstance();
        if (!CoasterCartEditMode.isActive()) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        @Nullable UUID cartId = CoasterCartEditMode.getActiveCartId();
        if (cartId == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        SubLevel cart = CoasterCartResolve.findCartById((Level)mc.level, cartId);
        if (cart == null || cart.isRemoved()) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        boolean hasConnections = !CoasterCartTrainLinkClientState.linksFor((ResourceKey<Level>)mc.level.dimension(), cartId).isEmpty();
        int color = hasConnections ? 0xFFDD55 : 0x888888;
        CoasterCartEditOutlineClient.drawCartOutline(outliner, cart, partialTick, color);
    }

    private static void drawCartOutline(Outliner outliner, SubLevel cart, float partialTick, int color) {
        Pose3d pose = new Pose3d();
        cart.lastPose().lerp((Pose3dc)cart.logicalPose(), (double)partialTick, pose);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
        if (plotOrigin == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, OUTLINER_KEY, plotOrigin, (Quaterniondc)pose.orientation(), color);
    }
}

