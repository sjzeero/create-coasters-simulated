/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.rivet;

import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementParticlesClient;
import dev.silvergold.simulatedcoasters.rivet.RivetPlaceAnimPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class RivetPlacementAnimationClient {
    public static final long DURATION_NANOS = 250000000L;
    public static final float PIVOT_X = 0.5f;
    public static final float PIVOT_Y = 0.9375f;
    public static final float PIVOT_Z = 0.5f;
    private static final Map<UUID, PendingAnim> PENDING_BY_RIVET_SUB = new HashMap<UUID, PendingAnim>();
    private static volatile boolean active;

    private RivetPlacementAnimationClient() {
    }

    public static boolean hasActive() {
        return active;
    }

    public static void begin(RivetPlaceAnimPayload payload) {
        PENDING_BY_RIVET_SUB.put(payload.rivetSubLevelId(), new PendingAnim(System.nanoTime(), SeatFxSnapshot.from(payload)));
        active = true;
    }

    public static float progress(UUID subLevelId) {
        PendingAnim pending = PENDING_BY_RIVET_SUB.get(subLevelId);
        if (pending == null) {
            return -1.0f;
        }
        float t = (float)(System.nanoTime() - pending.startNanos()) / 2.5E8f;
        return t >= 1.0f ? -1.0f : t;
    }

    public static float easeIn(float t) {
        return t * t;
    }

    public static void clientTick(@Nullable ClientLevel level) {
        if (!active) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, PendingAnim>> it = PENDING_BY_RIVET_SUB.entrySet().iterator();
        while (it.hasNext()) {
            ClientLevel mcLevel;
            Map.Entry<UUID, PendingAnim> entry = it.next();
            if (now - entry.getValue().startNanos() < 250000000L) continue;
            PendingAnim pending = entry.getValue();
            it.remove();
            ClientLevel fxLevel = level;
            if (fxLevel == null && (mcLevel = Minecraft.getInstance().level) instanceof ClientLevel) {
                ClientLevel cl;
                fxLevel = cl = mcLevel;
            }
            if (fxLevel == null) continue;
            RivetPlacementParticlesClient.onAnimationComplete(fxLevel, pending.seatFx());
        }
        active = !PENDING_BY_RIVET_SUB.isEmpty();
    }

    public static void clearAll() {
        PENDING_BY_RIVET_SUB.clear();
        active = false;
    }

    private record PendingAnim(long startNanos, SeatFxSnapshot seatFx) {
    }

    record SeatFxSnapshot(Vec3 seatWorld, Vec3 faceOutwardWorld, BlockState hostBlockState) {
        static SeatFxSnapshot from(RivetPlaceAnimPayload payload) {
            return new SeatFxSnapshot(payload.seatWorld(), payload.faceOutwardWorld(), payload.hostBlockState());
        }
    }
}

