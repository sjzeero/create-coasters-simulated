package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class TrackOutlineBezierAccess {
   private TrackOutlineBezierAccess() {
   }

   @Nullable
   public static BezierConnection primaryBezierForSelection(BezierPointSelection sel) {
      TrackBlockEntity tbe = sel.blockEntity();
      BlockPos key = sel.loc().curveTarget();

      for (BezierConnection c : tbe.getConnections().values()) {
         if (c.isPrimary() && c.getKey().equals(key)) {
            return c;
         }
      }

      return null;
   }
}
