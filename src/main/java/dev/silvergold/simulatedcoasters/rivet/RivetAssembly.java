package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class RivetAssembly {
   private RivetAssembly() {
   }

   public static void onHostBlocksMoved(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources, @Nullable UUID sourceSubLevelId) {
      RivetHostIndex index = RivetHostIndex.get(level);
      ServerLevel resultingLevel = transform.getLevel();

      for (BlockPos oldCell : movedSources) {
         if (oldCell != null) {
            BlockPos newCell = transform.apply(oldCell);
            RivetHostKey newHostKey = RivetHostKey.of(resultingLevel, newCell);
            rebindAnchorsOnHostCell(index, level, oldCell, newHostKey, transform);
         }
      }

      refreshAllRivetHostLocks(level);
   }

   private static void rebindAnchorsOnHostCell(RivetHostIndex index, ServerLevel level, BlockPos oldCell, RivetHostKey newHostKey, AssemblyTransform transform) {
      for (RivetAnchorKey anchor : new ArrayList<>(index.anchorsOnHostCell(oldCell))) {
         rebindAnchor(index, level, anchor, newHostKey, transform);
      }
   }

   private static void rebindAnchor(RivetHostIndex index, ServerLevel level, RivetAnchorKey anchor, RivetHostKey newHostKey, AssemblyTransform transform) {
      UUID rivetId = index.removeAnchor(anchor);
      if (rivetId != null) {
         Direction newFace = transform.getRotation().rotate(anchor.face());
         RivetPlacement.Attachment attachment = RivetPlacement.rotateAttachment(
            anchor.face(), new RivetPlacement.Attachment(anchor.lx(), anchor.ly(), anchor.lz()), transform
         );
         RivetAnchorKey newAnchor = anchor.withHost(newHostKey).withFaceAndAttachment(newFace, attachment);
         index.register(newAnchor, rivetId);
         ServerSubLevel rivetSub = findRivetSubLevel(level, rivetId);
         if (rivetSub != null) {
            RivetBlockEntity be = rivetBlockEntity(rivetSub);
            if (be != null) {
               be.rebindHost(newHostKey, newFace, attachment);
               be.applyHostLock(rivetSub);
            }
         }
      }
   }

   @Nullable
   private static ServerSubLevel findRivetSubLevel(ServerLevel level, UUID rivetId) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         if (container.getSubLevel(rivetId) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
            return ssl;
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
      EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BlockPos.ZERO);
      return var3 instanceof RivetBlockEntity ? (RivetBlockEntity)var3 : null;
   }

   public static void refreshAllRivetHostLocks(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (SubLevel sub : container.getAllSubLevels()) {
            if (sub instanceof ServerSubLevel) {
               ServerSubLevel rivetSub = (ServerSubLevel)sub;
               if (!rivetSub.isRemoved()
                  && rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get())) {
                  RivetBlockEntity be = rivetBlockEntity(rivetSub);
                  if (be != null) {
                     be.applyHostLock(rivetSub);
                  }
               }
            }
         }
      }
   }
}
