/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.client.track.CoasterPathClientRenderSpace;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftPreviewParticles {
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 1.0f);
    private static final double TRACK_UP_OFFSET_BLOCKS = 0.1875;
    private static final double BLOCKS_PER_PARTICLE = 5.0;
    private static final int MIN_PARTICLE_COUNT = 2;

    private ChainLiftPreviewParticles() {
    }

    public static void spawnSpanAlongPath(ClientLevel level, CoasterPathGraphSpans.SpanPath path, float partialTick) {
        if (path.segments().isEmpty()) {
            return;
        }
        double partialTickForRender = partialTick;
        CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of((Level)level, path, partialTickForRender);
        double total = sampler.totalLength();
        if (total <= 1.0E-6) {
            return;
        }
        int count = Math.max(2, (int)Math.ceil(total / 5.0));
        for (int i = 0; i < count; ++i) {
            double arc = total * ((double)i + 0.5) / (double)count;
            @Nullable CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit((Level)level, arc, partialTickForRender);
            if (hit == null) continue;
            Vec3 pos = ChainLiftPreviewParticles.offsetAlongTrackUp((Level)level, hit, partialTick);
            level.addParticle((ParticleOptions)WHITE_DUST, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
        }
    }

    private static Vec3 offsetAlongTrackUp(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge()) ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick) : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTick);
        Vec3 up = basis.up();
        up = up.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : up.normalize();
        Vec3 center = CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge()) ? CoasterPathClientRenderSpace.sampleEdgePosition(level, hit.edge(), hit.t(), partialTick) : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), Double.valueOf(partialTick));
        return center.add(up.scale(0.1875));
    }
}

