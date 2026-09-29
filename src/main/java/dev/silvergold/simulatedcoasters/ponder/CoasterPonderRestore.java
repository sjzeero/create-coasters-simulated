/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.ponder.CreateSceneBuilder
 *  dev.engine_room.flywheel.lib.visualization.VisualizationHelper
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.createmod.ponder.foundation.PonderScene
 *  net.createmod.ponder.foundation.registration.PonderSceneRegistry
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate$StructureBlockInfo
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.mixin.ponder.PonderLevelCoasterAccessor;
import dev.silvergold.simulatedcoasters.mixin.ponder.PonderLevelCoasterInvoker;
import dev.silvergold.simulatedcoasters.mixin.ponder.SchematicLevelCoasterAccessor;
import dev.silvergold.simulatedcoasters.ponder.CoastersPonderScenes;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public final class CoasterPonderRestore {
    private static final Map<BlockPos, CompoundTag> TEMPLATE_ANCHOR_NBTS = new HashMap<BlockPos, CompoundTag>();

    private CoasterPonderRestore() {
    }

    public static void seedFromStructureTemplate(PonderLevel level, StructureTemplate template) {
        if (level == null || template == null) {
            return;
        }
        TEMPLATE_ANCHOR_NBTS.clear();
        RegistryAccess registries = level.registryAccess();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        List blocks = template.filterBlocks(BlockPos.ZERO, settings, (Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get());
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockEntity loaded;
            BlockPos pos = info.pos();
            CompoundTag nbt = info.nbt();
            if (nbt != null) {
                TEMPLATE_ANCHOR_NBTS.put(pos.immutable(), nbt.copy());
            }
            if (nbt == null || !nbt.contains("AnchorPeerCurves") || !((loaded = BlockEntity.loadStatic((BlockPos)pos, (BlockState)info.state(), (CompoundTag)nbt, (HolderLookup.Provider)registries)) instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)loaded;
            CoasterPonderRestore.installBlockEntity(level, pos, (BlockEntity)anchor);
        }
    }

    public static void applyDisplayedAnchorLayer(CreateSceneBuilder scene, int sourceY, int displayY, BlockPos ... sourceAnchors) {
        int dy = displayY - sourceY;
        HolderLookup.Provider registries = scene.world().getHolderLookupProvider();
        for (BlockPos source : sourceAnchors) {
            BlockPos atSource = source.atY(sourceY);
            BlockPos target = source.atY(displayY);
            CompoundTag tag = TEMPLATE_ANCHOR_NBTS.get(atSource);
            CompoundTag applied = tag == null ? new CompoundTag() : CoasterPonderRestore.translateAnchorTagForDisplay(tag.copy(), dy);
            scene.world().modifyBlockEntity(target, CoasterAnchorpointBlockEntity.class, be -> {
                be.reloadCurveDataForPonder(applied, registries);
                be.repairAnchorPeerCurveKeys();
                AnchorPeerCurveClientIndex.refreshMembership(be);
                VisualizationHelper.queueUpdate((BlockEntity)be);
            });
        }
    }

    @Nullable
    public static CompoundTag templateAnchorNbt(BlockPos schematicPos) {
        return TEMPLATE_ANCHOR_NBTS.get(schematicPos.immutable());
    }

    static CompoundTag translateAnchorTagForDisplay(CompoundTag tag, int dy) {
        return CoasterPonderRestore.translateAnchorTag(tag, dy);
    }

    private static CompoundTag translateAnchorTag(CompoundTag tag, int dy) {
        if (dy == 0) {
            return tag;
        }
        CoasterPonderRestore.offsetPeerLongs(tag, "AnchorPeerCurves", dy);
        CoasterPonderRestore.offsetPeerLongs(tag, "AnchorPeerCurveTints", dy);
        return tag;
    }

    private static void offsetPeerLongs(CompoundTag tag, String listKey, int dy) {
        if (!tag.contains(listKey)) {
            return;
        }
        ListTag list = tag.getList(listKey, 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("Peer")) continue;
            BlockPos peer = BlockPos.of((long)entry.getLong("Peer")).offset(0, dy, 0);
            entry.putLong("Peer", peer.asLong());
        }
    }

    public static void onLevelRestore(PonderLevel level) {
        if (!level.isClientSide()) {
            return;
        }
        if (CoasterPonderRestore.needsCurveSeed(level)) {
            CoasterPonderRestore.seedFromSceneSchematic(level);
        }
        Map<BlockPos, CompoundTag> backup = ((PonderLevelCoasterAccessor)level).simulatedcoasters$originalBlockEntities();
        RegistryAccess registries = level.registryAccess();
        ArrayList<CoasterAnchorpointBlockEntity> anchors = new ArrayList<CoasterAnchorpointBlockEntity>();
        for (BlockEntity be : level.getBlockEntities()) {
            if (!(be instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
            anchors.add(anchor);
            if (anchor.getAnchorPeerCurvesView().isEmpty()) {
                CompoundTag tag = backup.get(anchor.getBlockPos());
                if (tag == null || !tag.contains("AnchorPeerCurves")) continue;
                anchor.reloadCurveDataForPonder(tag, (HolderLookup.Provider)registries);
                continue;
            }
            anchor.repairAnchorPeerCurveKeys();
        }
        BlockPos[] seeds = (BlockPos[])anchors.stream().map(BlockEntity::getBlockPos).toArray(BlockPos[]::new);
        for (CoasterAnchorpointBlockEntity anchor : anchors) {
            AnchorPeerCurveClientIndex.refreshMembership(anchor);
            VisualizationHelper.queueUpdate((BlockEntity)anchor);
        }
        if (seeds.length > 0) {
            AnchorJunctionVisualRefresh.refreshAround((Level)level, seeds);
        }
    }

    private static boolean needsCurveSeed(PonderLevel level) {
        for (BlockEntity be : level.getBlockEntities()) {
            CoasterAnchorpointBlockEntity anchor;
            if (!(be instanceof CoasterAnchorpointBlockEntity) || (anchor = (CoasterAnchorpointBlockEntity)be).getAnchorPeerCurvesView().isEmpty()) continue;
            return false;
        }
        return true;
    }

    private static void seedFromSceneSchematic(PonderLevel level) {
        PonderScene scene = level.scene;
        if (scene == null) {
            return;
        }
        ResourceLocation schematicPath = CoastersPonderScenes.schematicPathFor(scene.getId());
        if (schematicPath == null) {
            return;
        }
        StructureTemplate template = PonderSceneRegistry.loadSchematic((ResourceLocation)schematicPath);
        if (template == null || template.getSize().equals((Object)BlockPos.ZERO)) {
            return;
        }
        CoasterPonderRestore.seedFromStructureTemplate(level, template);
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

