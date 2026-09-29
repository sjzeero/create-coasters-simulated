/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration
 *  dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle
 *  dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetAnchorKey;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import dev.silvergold.simulatedcoasters.rivet.RivetPoseAlignment;
import dev.silvergold.simulatedcoasters.rivet.RivetRemoval;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RivetBlockEntity
extends BlockEntity {
    @Nullable
    private UUID hostSubLevelId;
    private BlockPos hostPos = BlockPos.ZERO;
    private Direction face = Direction.UP;
    private double attachLx;
    private double attachLy;
    private double attachLz;
    private int yRotationDegrees;
    private boolean hostOnCartSubLevel;
    private final List<FixedConstraintHandle> constraintHandles = new ArrayList<FixedConstraintHandle>();

    public RivetBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)SimulatedCoastersBlocks.RIVET_BE.get(), pos, state);
    }

    public void bindHost(RivetHostKey host, Direction face, RivetPlacement.Attachment attachment, int yRotationDegrees) {
        this.hostSubLevelId = host.hostSubLevelId();
        this.hostPos = host.cell().immutable();
        this.face = face;
        this.attachLx = attachment.lx();
        this.attachLy = attachment.ly();
        this.attachLz = attachment.lz();
        this.yRotationDegrees = RivetPlacement.wrapYRotationDegrees(yRotationDegrees);
        this.setChanged();
    }

    public void bindHost(RivetHostKey host, Direction face, RivetPlacement.Attachment attachment) {
        this.bindHost(host, face, attachment, 0);
    }

    public void rebindHost(RivetHostKey host, Direction face, RivetPlacement.Attachment attachment) {
        this.bindHost(host, face, attachment, this.yRotationDegrees);
    }

    public RivetHostKey hostKey() {
        return new RivetHostKey(this.hostSubLevelId, this.hostPos);
    }

    public BlockPos hostPos() {
        return this.hostPos;
    }

    public Direction face() {
        return this.face;
    }

    public RivetPlacement.Attachment attachment() {
        return new RivetPlacement.Attachment(this.attachLx, this.attachLy, this.attachLz);
    }

    public int yRotationDegrees() {
        return this.yRotationDegrees;
    }

    public RivetAnchorKey anchorKey() {
        return new RivetAnchorKey(this.hostKey(), this.face, this.attachLx, this.attachLy, this.attachLz);
    }

    public boolean compensatesCartHostUniversalDrag() {
        return this.hostOnCartSubLevel;
    }

    public void applyHostLock(ServerSubLevel rivetSub) {
        ServerLevel serverLevel;
        block8: {
            block7: {
                this.removeWorldLock();
                Level level = this.level;
                if (!(level instanceof ServerLevel)) break block7;
                serverLevel = (ServerLevel)level;
                if (!rivetSub.isRemoved()) break block8;
            }
            return;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)serverLevel);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        if (serverLevel.getBlockState(this.hostPos).isAir()) {
            RivetBlockEntity.scheduleTeardownIfInvalidHost(serverLevel, rivetSub.getUniqueId());
            return;
        }
        ServerSubLevel hostSub = this.resolveHostSubLevel(serverLevel);
        BlockPos rivetCell = rivetSub.getPlot().getCenterBlock();
        if (hostSub != null && hostSub.getUniqueId().equals(rivetSub.getUniqueId())) {
            RivetBlockEntity.scheduleTeardownIfInvalidHost(serverLevel, rivetSub.getUniqueId());
            return;
        }
        if (hostSub == null) {
            Quaterniond orient = RivetPoseAlignment.nominalWorldOrientation(serverLevel, this);
            this.addWorldLock(container, rivetSub, new Vector3d((Vector3dc)rivetSub.logicalPose().position()), new Vector3d((Vector3dc)rivetSub.logicalPose().rotationPoint()), orient);
            RivetPlacement.Resolved resolved = RivetPoseAlignment.resolvedFrom(this);
            Vec3 distalWorld = RivetPlacement.faceAnchorWorld((Level)serverLevel, resolved).add(RivetPlacement.hostFaceOutwardWorld((Level)serverLevel, resolved).scale(3.0));
            Vec3 rivetDistal = RivetPlacement.rivetDistalAnchorPlot(rivetCell);
            this.addWorldLock(container, rivetSub, new Vector3d(distalWorld.x, distalWorld.y, distalWorld.z), new Vector3d(rivetDistal.x, rivetDistal.y, rivetDistal.z), orient);
            Vec3 lateralWorld = RivetPlacement.faceAnchorWorld((Level)serverLevel, resolved).add(RivetPlacement.referenceRightWorld((Level)serverLevel, resolved).scale(3.0));
            Vec3 rivetLateral = RivetPlacement.rivetLateralAnchorPlot(rivetCell);
            this.addWorldLock(container, rivetSub, new Vector3d(lateralWorld.x, lateralWorld.y, lateralWorld.z), new Vector3d(rivetLateral.x, rivetLateral.y, rivetLateral.z), orient);
            this.refreshHostCartDragCache(serverLevel);
            return;
        }
        Quaterniond relativeOrient = RivetPoseAlignment.nominalRelativeOrientation(serverLevel, hostSub, this);
        Vec3 hostAnchorPlot = RivetPlacement.faceAnchorPlot(this.hostPos, this.face, this.attachLx, this.attachLy, this.attachLz);
        Vec3 rivetAnchorPlot = RivetPlacement.rivetOutwardAnchorPlot(rivetCell);
        this.addHostLock(container, hostSub, rivetSub, hostAnchorPlot, rivetAnchorPlot, relativeOrient);
        Vec3 hostDistalPlot = RivetPlacement.hostDistalAnchorPlot(this.hostPos, this.face, this.attachLx, this.attachLy, this.attachLz);
        Vec3 rivetDistalPlot = RivetPlacement.rivetDistalAnchorPlot(rivetCell);
        this.addHostLock(container, hostSub, rivetSub, hostDistalPlot, rivetDistalPlot, relativeOrient);
        Vec3 hostLateralPlot = RivetPlacement.hostLateralAnchorPlot(this.hostPos, this.face, this.attachLx, this.attachLy, this.attachLz, this.yRotationDegrees);
        Vec3 rivetLateralPlot = RivetPlacement.rivetLateralAnchorPlot(rivetCell);
        this.addHostLock(container, hostSub, rivetSub, hostLateralPlot, rivetLateralPlot, relativeOrient);
        this.refreshHostCartDragCache(serverLevel);
    }

    static void scheduleTeardownIfInvalidHost(ServerLevel level, UUID rivetSubLevelId) {
        level.getServer().execute(() -> {
            ServerSubLevel rivetSub;
            if (RivetRemoval.isRemoving(rivetSubLevelId)) {
                return;
            }
            ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
            if (!(raw instanceof ServerSubLevelContainer)) {
                return;
            }
            ServerSubLevelContainer container = raw;
            SubLevel rawSub = container.getSubLevel(rivetSubLevelId);
            if (!(rawSub instanceof ServerSubLevel) || (rivetSub = (ServerSubLevel)rawSub).isRemoved()) {
                return;
            }
            BlockEntity patt0$temp = rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
            if (!(patt0$temp instanceof RivetBlockEntity)) {
                return;
            }
            RivetBlockEntity be = (RivetBlockEntity)patt0$temp;
            if (!be.hasInvalidHost(level, rivetSub)) {
                return;
            }
            RivetRemoval.onHostBroken(level, rivetSub);
        });
    }

    boolean hasInvalidHost(ServerLevel level, ServerSubLevel rivetSub) {
        if (level.getBlockState(this.hostPos).isAir()) {
            return true;
        }
        SubLevel containing = Sable.HELPER.getContaining((Level)level, (Vec3i)this.hostPos);
        return containing != null && containing.getUniqueId().equals(rivetSub.getUniqueId());
    }

    private void addHostLock(ServerSubLevelContainer container, ServerSubLevel hostSub, ServerSubLevel rivetSub, Vec3 hostPlot, Vec3 rivetPlot, Quaterniond relativeOrient) {
        FixedConstraintHandle handle = (FixedConstraintHandle)container.physicsSystem().getPipeline().addConstraint((PhysicsPipelineBody)hostSub, (PhysicsPipelineBody)rivetSub, (PhysicsConstraintConfiguration)new FixedConstraintConfiguration((Vector3dc)new Vector3d(hostPlot.x, hostPlot.y, hostPlot.z), (Vector3dc)new Vector3d(rivetPlot.x, rivetPlot.y, rivetPlot.z), (Quaterniondc)relativeOrient));
        if (handle != null) {
            this.constraintHandles.add(handle);
        }
    }

    private void addWorldLock(ServerSubLevelContainer container, ServerSubLevel rivetSub, Vector3d worldPoint, Vector3d rivetPoint, Quaterniond orient) {
        FixedConstraintHandle handle = (FixedConstraintHandle)container.physicsSystem().getPipeline().addConstraint(null, (PhysicsPipelineBody)rivetSub, (PhysicsConstraintConfiguration)new FixedConstraintConfiguration((Vector3dc)worldPoint, (Vector3dc)rivetPoint, (Quaterniondc)orient));
        if (handle != null) {
            this.constraintHandles.add(handle);
        }
    }

    public void removeWorldLock() {
        for (FixedConstraintHandle handle : this.constraintHandles) {
            handle.remove();
        }
        this.constraintHandles.clear();
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("HostPos", this.hostPos.asLong());
        tag.putString("Face", this.face.getSerializedName());
        tag.putDouble("AttachLx", this.attachLx);
        tag.putDouble("AttachLy", this.attachLy);
        tag.putDouble("AttachLz", this.attachLz);
        tag.putInt("YRotation", this.yRotationDegrees);
        if (this.hostSubLevelId != null) {
            tag.putUUID("HostSubLevel", this.hostSubLevelId);
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hostPos = BlockPos.of((long)tag.getLong("HostPos"));
        this.face = Direction.byName((String)tag.getString("Face"));
        if (this.face == null) {
            this.face = Direction.UP;
        }
        this.attachLx = tag.contains("AttachLx", 6) ? tag.getDouble("AttachLx") : 0.5;
        this.attachLy = tag.contains("AttachLy", 6) ? tag.getDouble("AttachLy") : 0.0;
        this.attachLz = tag.contains("AttachLz", 6) ? tag.getDouble("AttachLz") : 0.5;
        this.yRotationDegrees = tag.contains("YRotation", 3) ? RivetPlacement.wrapYRotationDegrees(tag.getInt("YRotation")) : 0;
        this.hostSubLevelId = tag.hasUUID("HostSubLevel") ? tag.getUUID("HostSubLevel") : null;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public void setRemoved() {
        this.removeWorldLock();
        super.setRemoved();
    }

    public void onLoad() {
        ServerSubLevel ssl;
        Level level;
        super.onLoad();
        if (this.level == null || this.level.isClientSide() || !((level = this.level) instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        SubLevel sub = Sable.HELPER.getContaining((Level)serverLevel, (Vec3i)this.getBlockPos());
        if (sub instanceof ServerSubLevel && !(ssl = (ServerSubLevel)sub).isRemoved()) {
            this.applyHostLock(ssl);
        }
    }

    @Nullable
    private ServerSubLevel resolveHostSubLevel(ServerLevel serverLevel) {
        ServerSubLevel ssl;
        if (serverLevel.getBlockState(this.hostPos).isAir()) {
            return null;
        }
        SubLevel containing = Sable.HELPER.getContaining((Level)serverLevel, (Vec3i)this.hostPos);
        if (!(containing instanceof ServerSubLevel) || (ssl = (ServerSubLevel)containing).isRemoved()) {
            return null;
        }
        UUID id = ssl.getUniqueId();
        if (!id.equals(this.hostSubLevelId)) {
            this.hostSubLevelId = id;
            this.setChanged();
        }
        return ssl;
    }

    private void refreshHostCartDragCache(ServerLevel serverLevel) {
        ServerSubLevel hostSub = this.resolveHostSubLevel(serverLevel);
        this.hostOnCartSubLevel = hostSub != null && CoasterCartResolve.isCartSubLevel((SubLevel)hostSub);
    }
}

