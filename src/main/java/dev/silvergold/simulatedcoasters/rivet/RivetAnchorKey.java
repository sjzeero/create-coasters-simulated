package dev.silvergold.simulatedcoasters.rivet;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public record RivetAnchorKey(RivetHostKey host, Direction face, double lx, double ly, double lz) {
   public RivetAnchorKey(RivetHostKey host, Direction face, double lx, double ly, double lz) {
      host = Objects.requireNonNull(host);
      face = Objects.requireNonNull(face);
      this.host = host;
      this.face = face;
      this.lx = lx;
      this.ly = ly;
      this.lz = lz;
   }

   public static RivetAnchorKey from(Level level, RivetPlacement.Resolved placement) {
      return new RivetAnchorKey(RivetHostKey.of(level, placement.host()), placement.face(), placement.lx(), placement.ly(), placement.lz());
   }

   public RivetAnchorKey withHost(RivetHostKey newHost) {
      return new RivetAnchorKey(newHost, this.face, this.lx, this.ly, this.lz);
   }

   public RivetAnchorKey withFaceAndAttachment(Direction newFace, RivetPlacement.Attachment attachment) {
      return new RivetAnchorKey(this.host, newFace, attachment.lx(), attachment.ly(), attachment.lz());
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof RivetAnchorKey other)
            ? false
            : this.host.equals(other.host)
               && this.face == other.face
               && doubleEquals(this.lx, other.lx)
               && doubleEquals(this.ly, other.ly)
               && doubleEquals(this.lz, other.lz);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.host, this.face, Double.hashCode(this.lx), Double.hashCode(this.ly), Double.hashCode(this.lz));
   }

   private static boolean doubleEquals(double a, double b) {
      return Math.abs(a - b) < 1.0E-4;
   }
}
