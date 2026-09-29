package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTieredDye;
import dev.silvergold.simulatedcoasters.track.CoasterCurveUndyeParticles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

final class CoasterPonderUndyeParticleInstruction extends PonderInstruction {
   private static final ResourceLocation PONDER_TRACK_SCHEMATIC = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "coaster_track/ponder_track_placement"
   );
   private final int dyedSourceY;
   private final int displayY;
   private final BlockPos[] anchorXz;
   private boolean fired;

   CoasterPonderUndyeParticleInstruction(int dyedSourceY, int displayY, BlockPos[] anchorXz) {
      this.dyedSourceY = dyedSourceY;
      this.displayY = displayY;
      this.anchorXz = anchorXz;
   }

   public boolean isComplete() {
      return this.fired;
   }

   public void tick(PonderScene scene) {
      if (!this.fired) {
         this.fired = true;
         PonderLevel level = scene.getWorld();
         int dy = this.displayY - this.dyedSourceY;
         StructureTemplate template = loadTemplate();

         for (BlockPos xz : this.anchorXz) {
            BlockPos sourcePos = xz.atY(this.dyedSourceY);
            BlockPos displaySelf = xz.atY(this.displayY);
            CompoundTag tag = this.resolveAnchorTag(sourcePos, template);
            if (tag != null) {
               spawnFromTag(level, CoasterPonderRestore.translateAnchorTagForDisplay(tag.copy(), dy), displaySelf);
            }
         }
      }
   }

   private static void spawnFromTag(PonderLevel level, CompoundTag tag, BlockPos displaySelf) {
      Map<BlockPos, BlockPos> peerRemap = peerRemapFromTemplateCurves(tag, displaySelf);
      Map<BlockPos, Integer> railTints = railTintsFromTemplate(tag, peerRemap);

      for (Tag value : tag.getList("AnchorPeerCurves", 10)) {
         if (value instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)value;
            if (c.contains("Bezier", 10)) {
               BezierConnection bc = new BezierConnection(c.getCompound("Bezier"), displaySelf);
               if (bc.isPrimary()) {
                  BlockPos oldPeer = c.contains("Peer", 4) ? BlockPos.of(c.getLong("Peer")) : BlockPos.ZERO;
                  BlockPos peer = peerRemap.getOrDefault(oldPeer, oldPeer);
                  int rail = railTints.getOrDefault(peer, 0);
                  int cleared = CoasterCurveTieredDye.nextAxeStep(rail, 0).clearedRgbForParticles();
                  if (cleared != 0) {
                     CoasterCurveUndyeParticles.spawnAlongCurve(level, bc, cleared);
                  }
               }
            }
         }
      }
   }

   private static Map<BlockPos, Integer> railTintsFromTemplate(CompoundTag tag, Map<BlockPos, BlockPos> peerRemap) {
      Set<BlockPos> validPeers = peersFromCurveList(tag, peerRemap);
      Map<BlockPos, Integer> out = new HashMap<>();

      for (Tag value : tag.getList("AnchorPeerCurveTints", 10)) {
         if (value instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)value;
            if (c.contains("Peer", 4)) {
               BlockPos oldPeer = BlockPos.of(c.getLong("Peer"));
               BlockPos peer = peerRemap.getOrDefault(oldPeer, oldPeer);
               if (validPeers.contains(peer)) {
                  int rail = c.contains("RailRGB", 3) ? c.getInt("RailRGB") & 16777215 : c.getInt("RGB") & 16777215;
                  if (rail != 0) {
                     out.put(peer.immutable(), rail);
                  }
               }
            }
         }
      }

      return out;
   }

   private static Set<BlockPos> peersFromCurveList(CompoundTag tag, Map<BlockPos, BlockPos> peerRemap) {
      Set<BlockPos> peers = new HashSet<>();

      for (Tag value : tag.getList("AnchorPeerCurves", 10)) {
         if (value instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)value;
            if (c.contains("Peer", 4)) {
               BlockPos oldPeer = BlockPos.of(c.getLong("Peer"));
               peers.add(peerRemap.getOrDefault(oldPeer, oldPeer).immutable());
            }
         }
      }

      return peers;
   }

   private static Map<BlockPos, BlockPos> peerRemapFromTemplateCurves(CompoundTag tag, BlockPos self) {
      Map<BlockPos, BlockPos> remap = new HashMap<>();

      for (Tag value : tag.getList("AnchorPeerCurves", 10)) {
         if (value instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)value;
            if (c.contains("Peer", 4) && c.contains("Bezier", 10)) {
               BlockPos oldPeer = BlockPos.of(c.getLong("Peer"));
               BezierConnection bc = new BezierConnection(c.getCompound("Bezier"), self);
               BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
               BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
               BlockPos fixed;
               if (self.equals(p0)) {
                  fixed = p1;
               } else if (self.equals(p1)) {
                  fixed = p0;
               } else {
                  fixed = oldPeer;
               }

               if (!oldPeer.equals(p0) && !oldPeer.equals(p1)) {
                  remap.put(oldPeer.immutable(), fixed.immutable());
               }
            }
         }
      }

      return remap;
   }

   @Nullable
   private CompoundTag resolveAnchorTag(BlockPos sourcePos, @Nullable StructureTemplate template) {
      CompoundTag cached = CoasterPonderRestore.templateAnchorNbt(sourcePos);
      if (cached != null) {
         return cached.copy();
      } else if (template == null) {
         return null;
      } else {
         StructurePlaceSettings settings = new StructurePlaceSettings();

         for (StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, settings, (Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
            if (info.pos().equals(sourcePos) && info.nbt() != null) {
               return info.nbt().copy();
            }
         }

         return null;
      }
   }

   @Nullable
   private static StructureTemplate loadTemplate() {
      return PonderSceneRegistry.loadSchematic(PONDER_TRACK_SCHEMATIC);
   }
}
