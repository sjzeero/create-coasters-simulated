package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.mixin.ponder.PonderLevelCoasterAccessor;
import dev.silvergold.simulatedcoasters.mixin.ponder.PonderLevelCoasterInvoker;
import dev.silvergold.simulatedcoasters.mixin.ponder.SchematicLevelCoasterAccessor;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorJunctionVisualRefresh;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

public final class CoasterPonderRestore {
   private static final Map<BlockPos, CompoundTag> TEMPLATE_ANCHOR_NBTS = new HashMap<>();

   private CoasterPonderRestore() {
   }

   public static void seedFromStructureTemplate(PonderLevel level, StructureTemplate template) {
      if (level != null && template != null) {
         TEMPLATE_ANCHOR_NBTS.clear();
         Provider registries = level.registryAccess();
         StructurePlaceSettings settings = new StructurePlaceSettings();

         for (StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, settings, (Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
            BlockPos pos = info.pos();
            CompoundTag nbt = info.nbt();
            if (nbt != null) {
               TEMPLATE_ANCHOR_NBTS.put(pos.immutable(), nbt.copy());
            }

            if (nbt != null
               && nbt.contains("AnchorPeerCurves")
               && BlockEntity.loadStatic(pos, info.state(), nbt, registries) instanceof CoasterAnchorpointBlockEntity anchor) {
               installBlockEntity(level, pos, anchor);
            }
         }
      }
   }

   public static void applyDisplayedAnchorLayer(CreateSceneBuilder scene, int sourceY, int displayY, BlockPos... sourceAnchors) {
      int dy = displayY - sourceY;
      Provider registries = scene.world().getHolderLookupProvider();

      for (BlockPos source : sourceAnchors) {
         BlockPos atSource = source.atY(sourceY);
         BlockPos target = source.atY(displayY);
         CompoundTag tag = TEMPLATE_ANCHOR_NBTS.get(atSource);
         CompoundTag applied = tag == null ? new CompoundTag() : translateAnchorTagForDisplay(tag.copy(), dy);
         scene.world().modifyBlockEntity(target, CoasterAnchorpointBlockEntity.class, be -> {
            be.reloadCurveDataForPonder(applied, registries);
            be.repairAnchorPeerCurveKeys();
            AnchorPeerCurveClientIndex.refreshMembership(be);
            VisualizationHelper.queueUpdate(be);
         });
      }
   }

   @Nullable
   public static CompoundTag templateAnchorNbt(BlockPos schematicPos) {
      return TEMPLATE_ANCHOR_NBTS.get(schematicPos.immutable());
   }

   static CompoundTag translateAnchorTagForDisplay(CompoundTag tag, int dy) {
      return translateAnchorTag(tag, dy);
   }

   private static CompoundTag translateAnchorTag(CompoundTag tag, int dy) {
      if (dy == 0) {
         return tag;
      } else {
         offsetPeerLongs(tag, "AnchorPeerCurves", dy);
         offsetPeerLongs(tag, "AnchorPeerCurveTints", dy);
         return tag;
      }
   }

   private static void offsetPeerLongs(CompoundTag tag, String listKey, int dy) {
      if (tag.contains(listKey)) {
         ListTag list = tag.getList(listKey, 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.contains("Peer")) {
               BlockPos peer = BlockPos.of(entry.getLong("Peer")).offset(0, dy, 0);
               entry.putLong("Peer", peer.asLong());
            }
         }
      }
   }

   public static void onLevelRestore(PonderLevel level) {
      if (level.isClientSide()) {
         if (needsCurveSeed(level)) {
            seedFromSceneSchematic(level);
         }

         Map<BlockPos, CompoundTag> backup = ((PonderLevelCoasterAccessor)level).simulatedcoasters$originalBlockEntities();
         Provider registries = level.registryAccess();
         List<CoasterAnchorpointBlockEntity> anchors = new ArrayList<>();

         for (BlockEntity be : level.getBlockEntities()) {
            if (be instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
               anchors.add(anchor);
               if (anchor.getAnchorPeerCurvesView().isEmpty()) {
                  CompoundTag tag = backup.get(anchor.getBlockPos());
                  if (tag != null && tag.contains("AnchorPeerCurves")) {
                     anchor.reloadCurveDataForPonder(tag, registries);
                  }
               } else {
                  anchor.repairAnchorPeerCurveKeys();
               }
            }
         }

         BlockPos[] seeds = anchors.stream().map(BlockEntity::getBlockPos).toArray(BlockPos[]::new);

         for (CoasterAnchorpointBlockEntity anchor : anchors) {
            AnchorPeerCurveClientIndex.refreshMembership(anchor);
            VisualizationHelper.queueUpdate(anchor);
         }

         if (seeds.length > 0) {
            AnchorJunctionVisualRefresh.refreshAround(level, seeds);
         }
      }
   }

   private static boolean needsCurveSeed(PonderLevel level) {
      for (BlockEntity be : level.getBlockEntities()) {
         if (be instanceof CoasterAnchorpointBlockEntity anchor && !anchor.getAnchorPeerCurvesView().isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static void seedFromSceneSchematic(PonderLevel level) {
      PonderScene scene = level.scene;
      if (scene != null) {
         ResourceLocation schematicPath = CoastersPonderScenes.schematicPathFor(scene.getId());
         if (schematicPath != null) {
            StructureTemplate template = PonderSceneRegistry.loadSchematic(schematicPath);
            if (template != null && !template.getSize().equals(BlockPos.ZERO)) {
               seedFromStructureTemplate(level, template);
            }
         }
      }
   }

   private static void installBlockEntity(PonderLevel level, BlockPos pos, BlockEntity blockEntity) {
      ((PonderLevelCoasterInvoker)level).simulatedcoasters$onBEAdded(blockEntity, pos);
      SchematicLevelCoasterAccessor access = (SchematicLevelCoasterAccessor)level;
      access.simulatedcoasters$blockEntities().put(pos.immutable(), blockEntity);
      if (!access.simulatedcoasters$renderedBlockEntities().contains(blockEntity)) {
         access.simulatedcoasters$renderedBlockEntities().add(blockEntity);
      }
   }
}
