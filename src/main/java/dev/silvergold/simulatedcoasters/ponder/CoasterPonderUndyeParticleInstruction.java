/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.createmod.ponder.foundation.PonderScene
 *  net.createmod.ponder.foundation.instruction.PonderInstruction
 *  net.createmod.ponder.foundation.registration.PonderSceneRegistry
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate$StructureBlockInfo
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderRestore;
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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

final class CoasterPonderUndyeParticleInstruction
extends PonderInstruction {
    private static final ResourceLocation PONDER_TRACK_SCHEMATIC = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track/ponder_track_placement");
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
        if (this.fired) {
            return;
        }
        this.fired = true;
        PonderLevel level = scene.getWorld();
        int dy = this.displayY - this.dyedSourceY;
        StructureTemplate template = CoasterPonderUndyeParticleInstruction.loadTemplate();
        for (BlockPos xz : this.anchorXz) {
            BlockPos sourcePos = xz.atY(this.dyedSourceY);
            BlockPos displaySelf = xz.atY(this.displayY);
            CompoundTag tag = this.resolveAnchorTag(sourcePos, template);
            if (tag == null) continue;
            CoasterPonderUndyeParticleInstruction.spawnFromTag(level, CoasterPonderRestore.translateAnchorTagForDisplay(tag.copy(), dy), displaySelf);
        }
    }

    private static void spawnFromTag(PonderLevel level, CompoundTag tag, BlockPos displaySelf) {
        Map<BlockPos, BlockPos> peerRemap = CoasterPonderUndyeParticleInstruction.peerRemapFromTemplateCurves(tag, displaySelf);
        Map<BlockPos, Integer> railTints = CoasterPonderUndyeParticleInstruction.railTintsFromTemplate(tag, peerRemap);
        ListTag curveList = tag.getList("AnchorPeerCurves", 10);
        for (Tag value : curveList) {
            BlockPos oldPeer;
            BlockPos peer;
            int rail;
            int cleared;
            BezierConnection bc;
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Bezier", 10) || !(bc = new BezierConnection(c.getCompound("Bezier"), displaySelf)).isPrimary() || (cleared = CoasterCurveTieredDye.nextAxeStep(rail = railTints.getOrDefault(peer = peerRemap.getOrDefault(oldPeer = c.contains("Peer", 4) ? BlockPos.of((long)c.getLong("Peer")) : BlockPos.ZERO, oldPeer), 0).intValue(), 0).clearedRgbForParticles()) == 0) continue;
            CoasterCurveUndyeParticles.spawnAlongCurve(level, bc, cleared);
        }
    }

    private static Map<BlockPos, Integer> railTintsFromTemplate(CompoundTag tag, Map<BlockPos, BlockPos> peerRemap) {
        Set<BlockPos> validPeers = CoasterPonderUndyeParticleInstruction.peersFromCurveList(tag, peerRemap);
        HashMap<BlockPos, Integer> out = new HashMap<BlockPos, Integer>();
        ListTag tintList = tag.getList("AnchorPeerCurveTints", 10);
        for (Tag value : tintList) {
            int rail;
            BlockPos oldPeer;
            BlockPos peer;
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Peer", 4) || !validPeers.contains(peer = peerRemap.getOrDefault(oldPeer = BlockPos.of((long)c.getLong("Peer")), oldPeer)) || (rail = c.contains("RailRGB", 3) ? c.getInt("RailRGB") & 0xFFFFFF : c.getInt("RGB") & 0xFFFFFF) == 0) continue;
            out.put(peer.immutable(), rail);
        }
        return out;
    }

    private static Set<BlockPos> peersFromCurveList(CompoundTag tag, Map<BlockPos, BlockPos> peerRemap) {
        HashSet<BlockPos> peers = new HashSet<BlockPos>();
        ListTag curveList = tag.getList("AnchorPeerCurves", 10);
        for (Tag value : curveList) {
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Peer", 4)) continue;
            BlockPos oldPeer = BlockPos.of((long)c.getLong("Peer"));
            peers.add(peerRemap.getOrDefault(oldPeer, oldPeer).immutable());
        }
        return peers;
    }

    private static Map<BlockPos, BlockPos> peerRemapFromTemplateCurves(CompoundTag tag, BlockPos self) {
        HashMap<BlockPos, BlockPos> remap = new HashMap<BlockPos, BlockPos>();
        ListTag curveList = tag.getList("AnchorPeerCurves", 10);
        for (Tag value : curveList) {
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Peer", 4) || !c.contains("Bezier", 10)) continue;
            BlockPos oldPeer = BlockPos.of((long)c.getLong("Peer"));
            BezierConnection bc = new BezierConnection(c.getCompound("Bezier"), self);
            BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
            BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
            BlockPos fixed = self.equals((Object)p0) ? p1 : (self.equals((Object)p1) ? p0 : oldPeer);
            if (oldPeer.equals((Object)p0) || oldPeer.equals((Object)p1)) continue;
            remap.put(oldPeer.immutable(), fixed.immutable());
        }
        return remap;
    }

    @Nullable
    private CompoundTag resolveAnchorTag(BlockPos sourcePos, @Nullable StructureTemplate template) {
        CompoundTag cached = CoasterPonderRestore.templateAnchorNbt(sourcePos);
        if (cached != null) {
            return cached.copy();
        }
        if (template == null) {
            return null;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, settings, (Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
            if (!info.pos().equals((Object)sourcePos) || info.nbt() == null) continue;
            return info.nbt().copy();
        }
        return null;
    }

    @Nullable
    private static StructureTemplate loadTemplate() {
        return PonderSceneRegistry.loadSchematic((ResourceLocation)PONDER_TRACK_SCHEMATIC);
    }
}

