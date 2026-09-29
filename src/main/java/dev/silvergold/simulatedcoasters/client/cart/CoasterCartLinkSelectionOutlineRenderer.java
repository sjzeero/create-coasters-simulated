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
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartPlacementPreviewRenderer;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
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
public final class CoasterCartLinkSelectionOutlineRenderer {
    private static final String OUTLINER_KEY_PREFIX = "coaster_cart_link_selection";

    private CoasterCartLinkSelectionOutlineRenderer() {
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_PREFIX);
            return;
        }
        @Nullable UUID targetId = CoasterCartLinkSelectionOutlineRenderer.linkTargetFromHands(mc);
        if (targetId == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_PREFIX);
            return;
        }
        SubLevel cart = CoasterCartResolve.findCartById((Level)mc.level, targetId);
        if (cart == null || cart.isRemoved()) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_PREFIX);
            return;
        }
        Pose3d pose = new Pose3d();
        cart.lastPose().lerp((Pose3dc)cart.logicalPose(), (double)partialTick, pose);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
        if (plotOrigin == null) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_PREFIX);
            return;
        }
        CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, OUTLINER_KEY_PREFIX, plotOrigin, (Quaterniondc)pose.orientation(), CoasterCartPlacementPreviewRenderer.linkPartnerOutlineColor());
    }

    @Nullable
    private static UUID linkTargetFromHands(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (main.is(SimulatedCoasters.COASTER_CART) && CoasterCartItem.hasLinkSelection(main)) {
            return CoasterCartLinkSelection.readLinkTarget(main);
        }
        ItemStack off = mc.player.getOffhandItem();
        if (off.is(SimulatedCoasters.COASTER_CART) && CoasterCartItem.hasLinkSelection(off)) {
            return CoasterCartLinkSelection.readLinkTarget(off);
        }
        return null;
    }
}

