/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class OpenEndExtensionPreviewProvider {
    @Nullable
    private static Preview preview;

    private OpenEndExtensionPreviewProvider() {
    }

    public static void installClient(Preview clientPreview) {
        preview = clientPreview;
    }

    @Nullable
    public static Float extensionBlocksAt(BlockPos anchorPos) {
        return preview == null ? null : preview.extensionBlocksAt(anchorPos);
    }

    @FunctionalInterface
    public static interface Preview {
        @Nullable
        public Float extensionBlocksAt(BlockPos var1);
    }
}

