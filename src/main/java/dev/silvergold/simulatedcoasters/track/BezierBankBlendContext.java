package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import org.jetbrains.annotations.Nullable;

public final class BezierBankBlendContext {
   private static final ThreadLocal<BezierConnection> ACTIVE = new ThreadLocal<>();
   private static final ThreadLocal<Float> SEGMENT_T = new ThreadLocal<>();

   private BezierBankBlendContext() {
   }

   public static void set(BezierConnection bc) {
      ACTIVE.set(bc);
   }

   public static void clear() {
      ACTIVE.remove();
      SEGMENT_T.remove();
   }

   @Nullable
   public static BezierConnection active() {
      return ACTIVE.get();
   }

   public static void setSegmentT(float t) {
      SEGMENT_T.set(t);
   }

   @Nullable
   public static Float segmentT() {
      return SEGMENT_T.get();
   }
}
