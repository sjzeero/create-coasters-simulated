package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class OpenEndExtensionPreviewProvider {
   @Nullable
   private static OpenEndExtensionPreviewProvider.Preview preview;

   private OpenEndExtensionPreviewProvider() {
   }

   public static void installClient(OpenEndExtensionPreviewProvider.Preview clientPreview) {
      preview = clientPreview;
   }

   @Nullable
   public static Float extensionBlocksAt(BlockPos anchorPos) {
      return preview == null ? null : preview.extensionBlocksAt(anchorPos);
   }

   @FunctionalInterface
   public interface Preview {
      @Nullable
      Float extensionBlocksAt(BlockPos var1);
   }
}
