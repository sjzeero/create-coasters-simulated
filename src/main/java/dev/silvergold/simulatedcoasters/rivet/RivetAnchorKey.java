/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.Level
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public record RivetAnchorKey(RivetHostKey host, Direction face, double lx, double ly, double lz) {
    public RivetAnchorKey {
        host = Objects.requireNonNull(host);
        face = Objects.requireNonNull(face);
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
        }
        if (!(obj instanceof RivetAnchorKey)) {
            return false;
        }
        RivetAnchorKey other = (RivetAnchorKey)obj;
        return this.host.equals(other.host) && this.face == other.face && RivetAnchorKey.doubleEquals(this.lx, other.lx) && RivetAnchorKey.doubleEquals(this.ly, other.ly) && RivetAnchorKey.doubleEquals(this.lz, other.lz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.face, Double.hashCode(this.lx), Double.hashCode(this.ly), Double.hashCode(this.lz));
    }

    private static boolean doubleEquals(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }
}

