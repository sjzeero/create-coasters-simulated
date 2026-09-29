package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftPreviewParticles {
   private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);
   private static final double TRACK_UP_OFFSET_BLOCKS = 0.1875;
   private static final double BLOCKS_PER_PARTICLE = 5.0;
   private static final int MIN_PARTICLE_COUNT = 2;

   private ChainLiftPreviewParticles() {
   }

   public static void spawnSpanAlongPath(ClientLevel level, CoasterPathGraphSpans.SpanPath path, float partialTick) {
      if (!path.segments().isEmpty()) {
         double partialTickForRender = (double)partialTick;
         CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, partialTickForRender);
         double total = sampler.totalLength();
         if (!(total <= 1.0E-6)) {
            int count = Math.max(2, (int)Math.ceil(total / 5.0));

            for (int i = 0; i < count; i++) {
               double arc = total * ((double)i + 0.5) / (double)count;
               CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, partialTickForRender);
               if (hit != null) {
                  Vec3 pos = offsetAlongTrackUp(level, hit, partialTick);
                  level.addParticle(WHITE_DUST, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   private static Vec3 offsetAlongTrackUp(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
      CoasterPathTrackFrame.TrackBasis basis = CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge())
         ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick)
         : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTick);
      Vec3 up = basis.up();
      if (up.lengthSqr() < 1.0E-12) {
         up = new Vec3(0.0, 1.0, 0.0);
      } else {
         up = up.normalize();
      }

      Vec3 center = CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge())
         ? CoasterPathClientRenderSpace.sampleEdgePosition(level, hit.edge(), hit.t(), partialTick)
         : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), (double)partialTick);
      return center.add(up.scale(0.1875));
   }
}
