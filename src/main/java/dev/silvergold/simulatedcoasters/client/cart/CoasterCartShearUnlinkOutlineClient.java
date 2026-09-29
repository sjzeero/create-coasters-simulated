/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceKey
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
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartShearUnlinkInteraction;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartShearUnlinkOutlineClient {
    private static final String OUTLINER_KEY = "coaster_cart_shear_unlink";
    private static final int OUTLINE_COLOR = 0xFF4747;

    private CoasterCartShearUnlinkOutlineClient() {
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        SubLevel hoverCart;
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || CoasterCartEditMode.isActive()) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        ItemStack shears = CoasterCartShearUnlinkOutlineClient.shearsInHand(mc);
        if (!CoasterCartShearUnlinkInteraction.holdingShears(shears)) {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY);
            return;
        }
        SubLevel subLevel = hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart((Level)mc.level, mc.hitResult);
        if (hoverCart == null || CoasterCartTrainLinkClientState.linksFor((ResourceKey<Level>)mc.level.dimension(), hoverCart.getUniqueId()).isEmpty()) {
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
        CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, OUTLINER_KEY, plotOrigin, (Quaterniondc)pose.orientation(), 0xFF4747);
    }

    @Nullable
    private static ItemStack shearsInHand(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (CoasterCartShearUnlinkInteraction.holdingShears(main)) {
            return main;
        }
        ItemStack off = mc.player.getOffhandItem();
        if (CoasterCartShearUnlinkInteraction.holdingShears(off)) {
            return off;
        }
        return ItemStack.EMPTY;
    }
}

