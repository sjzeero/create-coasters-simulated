/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
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
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartMidTrackPlacementClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartHoverOutlineClient {
    private static final String OUTLINER_KEY = "coaster_cart_hover";
    private static final int OUTLINE_COLOR = 1940687;

    private CoasterCartHoverOutlineClient() {
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        SubLevel hoverCart;
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) || CoasterCartEditMode.isActive()) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        SubLevel subLevel = hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart((Level)mc.level, mc.hitResult);
        if (hoverCart == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        @Nullable UUID linkTargetId = CoasterCartHoverOutlineClient.linkTargetFromHands(mc);
        if (linkTargetId != null && linkTargetId.equals(hoverCart.getUniqueId())) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        Pose3d pose = new Pose3d();
        hoverCart.lastPose().lerp((Pose3dc)hoverCart.logicalPose(), (double)partialTick, pose);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(hoverCart, pose);
        if (plotOrigin == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, OUTLINER_KEY, plotOrigin, (Quaterniondc)pose.orientation(), 1940687);
    }

    @Nullable
    private static UUID linkTargetFromHands(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (CoasterCartItem.hasLinkSelection(main)) {
            return CoasterCartLinkSelection.readLinkTarget(main);
        }
        ItemStack off = mc.player.getOffhandItem();
        if (CoasterCartItem.hasLinkSelection(off)) {
            return CoasterCartLinkSelection.readLinkTarget(off);
        }
        return null;
    }
}

