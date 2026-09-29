/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.simibubi.create.AllPartialModels
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$GirderAngles
 *  com.simibubi.create.content.trains.track.BezierConnection$SegmentAngles
 *  com.simibubi.create.content.trains.track.TrackMaterial$TrackModelHolder
 *  com.simibubi.create.foundation.render.SpecialModels
 *  dev.engine_room.flywheel.api.instance.Instance
 *  dev.engine_room.flywheel.api.instance.Instancer
 *  dev.engine_room.flywheel.api.model.Model
 *  dev.engine_room.flywheel.api.visual.BlockEntityVisual
 *  dev.engine_room.flywheel.api.visual.SectionTrackedVisual$SectionCollector
 *  dev.engine_room.flywheel.api.visual.ShaderLightVisual
 *  dev.engine_room.flywheel.api.visualization.VisualizationContext
 *  dev.engine_room.flywheel.lib.instance.InstanceTypes
 *  dev.engine_room.flywheel.lib.instance.TransformedInstance
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  dev.engine_room.flywheel.lib.transform.PoseTransformStack
 *  dev.engine_room.flywheel.lib.transform.TransformStack
 *  dev.engine_room.flywheel.lib.visual.AbstractVisual
 *  it.unimi.dsi.fastutil.longs.LongArraySet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  net.createmod.catnip.data.Couple
 *  net.createmod.catnip.data.Iterate
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.AnchorCurveMiningFilter;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorGirderBeam;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionPlate;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackCenterBeamModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackDyedBezierModels;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstance;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstanceType;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class AnchorPeerTrackCurveVisual
extends AbstractVisual
implements BlockEntityVisual<CoasterAnchorpointBlockEntity>,
ShaderLightVisual {
    private final List<BezierTrackVisual> visuals = new ArrayList<BezierTrackVisual>();
    protected final CoasterAnchorpointBlockEntity blockEntity;
    protected final BlockPos pos;
    protected final BlockPos visualPos;
    protected // Could not load outer class - annotation placement on inner may be incorrect
     @UnknownNullability SectionTrackedVisual.SectionCollector lightSections;

    public AnchorPeerTrackCurveVisual(VisualizationContext context, CoasterAnchorpointBlockEntity anchor, float partialTick) {
        super(context, anchor.getLevel(), partialTick);
        this.blockEntity = anchor;
        this.pos = anchor.getBlockPos();
        this.visualPos = this.pos.subtract(context.renderOrigin());
        this.collectConnections();
    }

    public void setSectionCollector(SectionTrackedVisual.SectionCollector sectionCollector) {
        this.lightSections = sectionCollector;
        this.lightSections.sections(this.collectLightSections());
    }

    public void update(float pt) {
        boolean anyPrimary = this.blockEntity.getAnchorPeerCurvesView().values().stream().anyMatch(BezierConnection::isPrimary);
        if (!anyPrimary) {
            if (!this.visuals.isEmpty()) {
                this._delete();
                if (this.lightSections != null) {
                    this.lightSections.sections(LongSet.of());
                }
            }
            return;
        }
        this._delete();
        this.collectConnections();
        if (this.lightSections != null) {
            this.lightSections.sections(this.collectLightSections());
        }
    }

    private void collectConnections() {
        this.blockEntity.getAnchorPeerCurvesView().entrySet().stream().map(e -> this.createInstance((BlockPos)e.getKey(), (BezierConnection)e.getValue())).filter(Objects::nonNull).forEach(this.visuals::add);
    }

    @Nullable
    private BezierTrackVisual createInstance(BlockPos peerKey, BezierConnection bc) {
        if (!bc.isPrimary()) {
            return null;
        }
        return new BezierTrackVisual(peerKey, bc);
    }

    public void _delete() {
        this.visuals.forEach(BezierTrackVisual::delete);
        this.visuals.clear();
    }

    public LongSet collectLightSections() {
        boolean any = this.blockEntity.getAnchorPeerCurvesView().values().stream().anyMatch(BezierConnection::isPrimary);
        if (!any) {
            return LongSet.of();
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BezierConnection connection : this.blockEntity.getAnchorPeerCurvesView().values()) {
            if (!connection.isPrimary()) continue;
            AABB bounds = connection.getBounds();
            minX = Math.min(minX, Mth.floor((double)bounds.minX) - 1);
            minY = Math.min(minY, Mth.floor((double)bounds.minY) - 1);
            minZ = Math.min(minZ, Mth.floor((double)bounds.minZ) - 1);
            maxX = Math.max(maxX, Mth.ceil((double)bounds.maxX) + 1);
            maxY = Math.max(maxY, Mth.ceil((double)bounds.maxY) + 1);
            maxZ = Math.max(maxZ, Mth.ceil((double)bounds.maxZ) + 1);
        }
        int minSectionX = SectionPos.blockToSectionCoord((int)minX);
        int minSectionY = SectionPos.blockToSectionCoord((int)minY);
        int minSectionZ = SectionPos.blockToSectionCoord((int)minZ);
        int maxSectionX = SectionPos.blockToSectionCoord((int)maxX);
        int maxSectionY = SectionPos.blockToSectionCoord((int)maxY);
        int maxSectionZ = SectionPos.blockToSectionCoord((int)maxZ);
        LongArraySet out = new LongArraySet();
        for (int x = minSectionX; x <= maxSectionX; ++x) {
            for (int y = minSectionY; y <= maxSectionY; ++y) {
                for (int z = minSectionZ; z <= maxSectionZ; ++z) {
                    out.add(SectionPos.asLong((int)x, (int)y, (int)z));
                }
            }
        }
        return out;
    }

    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (BezierTrackVisual instance : this.visuals) {
            instance.collectCrumblingInstances(consumer);
        }
    }

    private class BezierTrackVisual {
        private final BezierConnection connection;
        private final boolean isCoaster;
        private final TransformedInstance[] ties;
        private final TransformedInstance[] rigidLeft;
        private final TransformedInstance[] rigidRight;
        private final BentRailInstance[] bentLeft;
        private final BentRailInstance[] bentRight;
        private final BentRailInstance[] bentCenterBeam;
        @Nullable
        private TransformedInstance junctionPlate0;
        @Nullable
        private TransformedInstance junctionPlate1;
        @Nullable
        private TransformedInstance[] junctionGirder0;
        @Nullable
        private TransformedInstance[] junctionGirder1;
        @Nullable
        private GirderVisual girder;

        private BezierTrackVisual(BlockPos peerKey, BezierConnection bc) {
            this.connection = bc;
            this.isCoaster = bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
            this.girder = bc.hasGirder ? new GirderVisual(bc) : null;
            PoseStack pose = new PoseStack();
            TransformStack.of((PoseStack)pose).translate((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos);
            int segCount = bc.getSegmentCount();
            int openEndRailExt = this.isCoaster ? CoasterCurveRailInstancing.openEndRailExtensionSlotCount(AnchorPeerTrackCurveVisual.this.blockEntity.getLevel(), bc) : 0;
            this.ties = new TransformedInstance[this.isCoaster ? CoasterCurveRailInstancing.curveTieInstanceCount(bc) : segCount];
            TrackMaterial.TrackModelHolder modelHolder = bc.getMaterial().getModelHolder();
            int peerRailTint = AnchorPeerTrackCurveVisual.this.blockEntity.getPeerCurveRailDiffuseRgb(peerKey);
            int peerBeamTint = AnchorPeerTrackCurveVisual.this.blockEntity.getPeerCurveBeamDiffuseRgb(peerKey);
            boolean railDyedCoaster = this.isCoaster && CoasterTrackDyedBezierModels.hasDyedRailTint(peerRailTint);
            boolean beamDyedCoaster = this.isCoaster && CoasterTrackDyedBezierModels.hasDyedBeamTint(peerBeamTint);
            PartialModel tiePartial = beamDyedCoaster ? CoasterTrackDyedBezierModels.TIE_DYED : modelHolder.tie();
            AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)tiePartial)).createInstances((Instance[])this.ties);
            if (this.isCoaster) {
                this.rigidLeft = null;
                this.rigidRight = null;
                this.bentLeft = new BentRailInstance[segCount + openEndRailExt];
                this.bentRight = new BentRailInstance[segCount + openEndRailExt];
                this.bentCenterBeam = new BentRailInstance[segCount];
                PartialModel leftPartial = railDyedCoaster ? CoasterTrackDyedBezierModels.LEFT_SEGMENT_DYED : modelHolder.leftSegment();
                PartialModel rightPartial = railDyedCoaster ? CoasterTrackDyedBezierModels.RIGHT_SEGMENT_DYED : modelHolder.rightSegment();
                PartialModel beamPartial = beamDyedCoaster ? CoasterTrackDyedBezierModels.CENTER_BEAM_SEGMENT_DYED : CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT;
                AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(BentRailInstanceType.INSTANCE, SpecialModels.flatChunk((PartialModel)leftPartial)).createInstances((Instance[])this.bentLeft);
                AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(BentRailInstanceType.INSTANCE, SpecialModels.flatChunk((PartialModel)rightPartial)).createInstances((Instance[])this.bentRight);
                AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(BentRailInstanceType.INSTANCE, SpecialModels.flatChunk((PartialModel)beamPartial)).createInstances((Instance[])this.bentCenterBeam);
                CoasterCurveRailInstancing.apply(Vec3.atLowerCornerOf((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos), bc, this.ties, this.bentLeft, this.bentRight, this.bentCenterBeam, peerRailTint, peerBeamTint, AnchorPeerTrackCurveVisual.this.blockEntity.getLevel());
                this.junctionPlate0 = CoasterTrackAnchorJunctionPlate.createFlywheelPlate(AnchorPeerTrackCurveVisual.this.instancerProvider(), AnchorPeerTrackCurveVisual.this.blockEntity.getLevel(), bc, (BlockPos)bc.bePositions.getFirst(), Vec3.atLowerCornerOf((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos));
                this.junctionPlate1 = CoasterTrackAnchorJunctionPlate.createFlywheelPlate(AnchorPeerTrackCurveVisual.this.instancerProvider(), AnchorPeerTrackCurveVisual.this.blockEntity.getLevel(), bc, (BlockPos)bc.bePositions.getSecond(), Vec3.atLowerCornerOf((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos));
                this.junctionGirder0 = CoasterTrackAnchorGirderBeam.createFlywheelGirders(AnchorPeerTrackCurveVisual.this.instancerProvider(), AnchorPeerTrackCurveVisual.this.blockEntity.getLevel(), bc, (BlockPos)bc.bePositions.getFirst(), Vec3.atLowerCornerOf((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos));
                this.junctionGirder1 = CoasterTrackAnchorGirderBeam.createFlywheelGirders(AnchorPeerTrackCurveVisual.this.instancerProvider(), AnchorPeerTrackCurveVisual.this.blockEntity.getLevel(), bc, (BlockPos)bc.bePositions.getSecond(), Vec3.atLowerCornerOf((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos));
            } else {
                this.bentLeft = null;
                this.bentRight = null;
                this.bentCenterBeam = null;
                this.rigidLeft = new TransformedInstance[segCount];
                this.rigidRight = new TransformedInstance[segCount];
                AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)modelHolder.leftSegment())).createInstances((Instance[])this.rigidLeft);
                AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)modelHolder.rightSegment())).createInstances((Instance[])this.rigidRight);
                BezierConnection.SegmentAngles segment = bc.getBakedSegments();
                for (int i = 1; i < segment.length; ++i) {
                    int modelIndex = i - 1;
                    this.ties[modelIndex].setTransform(pose).mul(segment.tieTransform[i]).setChanged();
                    for (boolean first : Iterate.trueAndFalse) {
                        PoseStack.Pose transform = (PoseStack.Pose)segment.railTransforms[i].get(first);
                        (first ? this.rigidLeft : this.rigidRight)[modelIndex].setTransform(pose).mul(transform).setChanged();
                    }
                }
            }
        }

        void delete() {
            for (TransformedInstance transformedInstance : this.ties) {
                transformedInstance.delete();
            }
            if (this.rigidLeft != null) {
                for (TransformedInstance transformedInstance : this.rigidLeft) {
                    transformedInstance.delete();
                }
            }
            if (this.rigidRight != null) {
                for (TransformedInstance transformedInstance : this.rigidRight) {
                    transformedInstance.delete();
                }
            }
            if (this.bentLeft != null) {
                for (BentRailInstance bentRailInstance : this.bentLeft) {
                    bentRailInstance.delete();
                }
            }
            if (this.bentRight != null) {
                for (BentRailInstance bentRailInstance : this.bentRight) {
                    bentRailInstance.delete();
                }
            }
            if (this.bentCenterBeam != null) {
                for (BentRailInstance bentRailInstance : this.bentCenterBeam) {
                    bentRailInstance.delete();
                }
            }
            if (this.junctionPlate0 != null) {
                this.junctionPlate0.delete();
                this.junctionPlate0 = null;
            }
            if (this.junctionPlate1 != null) {
                this.junctionPlate1.delete();
                this.junctionPlate1 = null;
            }
            if (this.junctionGirder0 != null) {
                for (TransformedInstance transformedInstance : this.junctionGirder0) {
                    transformedInstance.delete();
                }
                this.junctionGirder0 = null;
            }
            if (this.junctionGirder1 != null) {
                for (TransformedInstance transformedInstance : this.junctionGirder1) {
                    transformedInstance.delete();
                }
                this.junctionGirder1 = null;
            }
            if (this.girder != null) {
                this.girder.delete();
            }
        }

        public void collectCrumblingInstances(Consumer<Instance> consumer) {
            if (!AnchorCurveMiningFilter.crumblingMatches(this.connection)) {
                return;
            }
            for (TransformedInstance transformedInstance : this.ties) {
                consumer.accept((Instance)transformedInstance);
            }
            if (this.rigidLeft != null) {
                for (TransformedInstance transformedInstance : this.rigidLeft) {
                    consumer.accept((Instance)transformedInstance);
                }
            }
            if (this.rigidRight != null) {
                for (TransformedInstance transformedInstance : this.rigidRight) {
                    consumer.accept((Instance)transformedInstance);
                }
            }
            if (this.bentLeft != null) {
                for (BentRailInstance bentRailInstance : this.bentLeft) {
                    consumer.accept((Instance)bentRailInstance);
                }
            }
            if (this.bentRight != null) {
                for (BentRailInstance bentRailInstance : this.bentRight) {
                    consumer.accept((Instance)bentRailInstance);
                }
            }
            if (this.bentCenterBeam != null) {
                for (BentRailInstance bentRailInstance : this.bentCenterBeam) {
                    consumer.accept((Instance)bentRailInstance);
                }
            }
            if (this.junctionPlate0 != null) {
                consumer.accept((Instance)this.junctionPlate0);
            }
            if (this.junctionPlate1 != null) {
                consumer.accept((Instance)this.junctionPlate1);
            }
            if (this.junctionGirder0 != null) {
                for (TransformedInstance transformedInstance : this.junctionGirder0) {
                    consumer.accept((Instance)transformedInstance);
                }
            }
            if (this.junctionGirder1 != null) {
                for (TransformedInstance transformedInstance : this.junctionGirder1) {
                    consumer.accept((Instance)transformedInstance);
                }
            }
            if (this.girder != null) {
                this.girder.collectCrumblingInstances(consumer);
            }
        }

        private class GirderVisual {
            private final Couple<TransformedInstance[]> beams;
            private final Couple<Couple<TransformedInstance[]>> beamCaps;

            private GirderVisual(BezierConnection bc) {
                PoseStack pose = new PoseStack();
                ((PoseTransformStack)TransformStack.of((PoseStack)pose).translate((Vec3i)AnchorPeerTrackCurveVisual.this.visualPos)).nudge((int)((BlockPos)bc.bePositions.getFirst()).asLong());
                int segCount = bc.getSegmentCount();
                this.beams = Couple.create(() -> new TransformedInstance[segCount]);
                this.beamCaps = Couple.create(() -> Couple.create(() -> new TransformedInstance[segCount]));
                this.beams.forEach(arg_0 -> ((Instancer)AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)AllPartialModels.GIRDER_SEGMENT_MIDDLE))).createInstances(arg_0));
                this.beamCaps.forEachWithContext((c, top) -> {
                    Model partialModel = SpecialModels.flatChunk((PartialModel)(top != false ? AllPartialModels.GIRDER_SEGMENT_TOP : AllPartialModels.GIRDER_SEGMENT_BOTTOM));
                    c.forEach(arg_0 -> ((Instancer)AnchorPeerTrackCurveVisual.this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, partialModel)).createInstances(arg_0));
                });
                BezierConnection.GirderAngles segment = bc.getBakedGirders();
                for (int i = 1; i < segment.length; ++i) {
                    int modelIndex = i - 1;
                    for (boolean first : Iterate.trueAndFalse) {
                        PoseStack.Pose beamTransform = (PoseStack.Pose)segment.beams[i].get(first);
                        ((TransformedInstance[])this.beams.get(first))[modelIndex].setTransform(pose).mul(beamTransform).setChanged();
                        for (boolean top2 : Iterate.trueAndFalse) {
                            PoseStack.Pose beamCapTransform = (PoseStack.Pose)((Couple)segment.beamCaps[i].get(top2)).get(first);
                            ((TransformedInstance[])((Couple)this.beamCaps.get(top2)).get(first))[modelIndex].setTransform(pose).mul(beamCapTransform).setChanged();
                        }
                    }
                }
            }

            void delete() {
                this.beams.forEach(arr -> {
                    for (TransformedInstance d : arr) {
                        d.delete();
                    }
                });
                this.beamCaps.forEach(c -> c.forEach(arr -> {
                    for (TransformedInstance d : arr) {
                        d.delete();
                    }
                }));
            }

            public void collectCrumblingInstances(Consumer<Instance> consumer) {
                this.beams.forEach(arr -> {
                    for (TransformedInstance d : arr) {
                        consumer.accept((Instance)d);
                    }
                });
                this.beamCaps.forEach(c -> c.forEach(arr -> {
                    for (TransformedInstance d : arr) {
                        consumer.accept((Instance)d);
                    }
                }));
            }
        }
    }
}

