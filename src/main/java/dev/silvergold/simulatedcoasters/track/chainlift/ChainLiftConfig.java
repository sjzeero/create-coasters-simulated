package dev.silvergold.simulatedcoasters.track.chainlift;

public final class ChainLiftConfig {
   public static final float MAX_SHAFT_RPM = 256.0F;
   public static final double BLOCKS_PER_SECOND_AT_MAX_RPM = 16.0;
   public static final double CHAIN_TEXTURE_WIDTH_BLOCKS = 0.5;
   public static final double CHAIN_TEXTURE_LENGTH_BLOCKS = 0.625;
   public static final double CHAIN_RIBBON_MAX_RENDER_DISTANCE_BLOCKS = 128.0;
   public static final double CART_CHAIN_SPEED_MATCH_BLOCKS_PER_SECOND_SQ = 32.0;
   public static final double CART_CHAIN_OPPOSE_BLOCKS_PER_SECOND_SQ = 64.0;

   private ChainLiftConfig() {
   }
}
