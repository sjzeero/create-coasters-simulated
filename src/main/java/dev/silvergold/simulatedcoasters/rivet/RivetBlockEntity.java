package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class RivetBlockEntity extends BlockEntity {
   @Nullable
   private UUID hostSubLevelId;
   private BlockPos hostPos = BlockPos.ZERO;
   private Direction face = Direction.UP;
   private double attachLx;
   private double attachLy;
   private double attachLz;
   private int yRotationDegrees;
   private boolean hostOnCartSubLevel;
   private final List<FixedConstraintHandle> constraintHandles = new ArrayList<>();

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
      this.removeWorldLock();
      if (this.level instanceof ServerLevel serverLevel && !rivetSub.isRemoved()) {
         SubLevelContainer raw = SubLevelContainer.getContainer(serverLevel);
         if (raw instanceof ServerSubLevelContainer container) {
            if (serverLevel.getBlockState(this.hostPos).isAir()) {
               scheduleTeardownIfInvalidHost(serverLevel, rivetSub.getUniqueId());
               return;
            }

            ServerSubLevel hostSub = this.resolveHostSubLevel(serverLevel);
            BlockPos rivetCell = rivetSub.getPlot().getCenterBlock();
            if (hostSub != null && hostSub.getUniqueId().equals(rivetSub.getUniqueId())) {
               scheduleTeardownIfInvalidHost(serverLevel, rivetSub.getUniqueId());
               return;
            }

            if (hostSub == null) {
               Quaterniond orient = RivetPoseAlignment.nominalWorldOrientation(serverLevel, this);
               this.addWorldLock(
                  container, rivetSub, new Vector3d(rivetSub.logicalPose().position()), new Vector3d(rivetSub.logicalPose().rotationPoint()), orient
               );
               RivetPlacement.Resolved resolved = RivetPoseAlignment.resolvedFrom(this);
               Vec3 distalWorld = RivetPlacement.faceAnchorWorld(serverLevel, resolved)
                  .add(RivetPlacement.hostFaceOutwardWorld(serverLevel, resolved).scale(3.0));
               Vec3 rivetDistal = RivetPlacement.rivetDistalAnchorPlot(rivetCell);
               this.addWorldLock(
                  container,
                  rivetSub,
                  new Vector3d(distalWorld.x, distalWorld.y, distalWorld.z),
                  new Vector3d(rivetDistal.x, rivetDistal.y, rivetDistal.z),
                  orient
               );
               Vec3 lateralWorld = RivetPlacement.faceAnchorWorld(serverLevel, resolved)
                  .add(RivetPlacement.referenceRightWorld(serverLevel, resolved).scale(3.0));
               Vec3 rivetLateral = RivetPlacement.rivetLateralAnchorPlot(rivetCell);
               this.addWorldLock(
                  container,
                  rivetSub,
                  new Vector3d(lateralWorld.x, lateralWorld.y, lateralWorld.z),
                  new Vector3d(rivetLateral.x, rivetLateral.y, rivetLateral.z),
                  orient
               );
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
            Vec3 hostLateralPlot = RivetPlacement.hostLateralAnchorPlot(
               this.hostPos, this.face, this.attachLx, this.attachLy, this.attachLz, this.yRotationDegrees
            );
            Vec3 rivetLateralPlot = RivetPlacement.rivetLateralAnchorPlot(rivetCell);
            this.addHostLock(container, hostSub, rivetSub, hostLateralPlot, rivetLateralPlot, relativeOrient);
            this.refreshHostCartDragCache(serverLevel);
            return;
         }

         return;
      }
   }

   static void scheduleTeardownIfInvalidHost(ServerLevel level, UUID rivetSubLevelId) {
      level.getServer().execute(() -> {
         if (!RivetRemoval.isRemoving(rivetSubLevelId)) {
            SubLevelContainer raw = SubLevelContainer.getContainer(level);
            if (raw instanceof ServerSubLevelContainer container) {
               if (!(container.getSubLevel(rivetSubLevelId) instanceof ServerSubLevel rivetSub) || rivetSub.isRemoved()) {
                  return;
               }

               if (!(rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof RivetBlockEntity be)) {
                  return;
               }

               if (be.hasInvalidHost(level, rivetSub)) {
                  RivetRemoval.onHostBroken(level, rivetSub);
               }
            }
         }
      });
   }

   boolean hasInvalidHost(ServerLevel level, ServerSubLevel rivetSub) {
      if (level.getBlockState(this.hostPos).isAir()) {
         return true;
      } else {
         SubLevel containing = Sable.HELPER.getContaining(level, this.hostPos);
         return containing != null && containing.getUniqueId().equals(rivetSub.getUniqueId());
      }
   }

   private void addHostLock(
      ServerSubLevelContainer container, ServerSubLevel hostSub, ServerSubLevel rivetSub, Vec3 hostPlot, Vec3 rivetPlot, Quaterniond relativeOrient
   ) {
      FixedConstraintHandle handle = (FixedConstraintHandle)container.physicsSystem()
         .getPipeline()
         .addConstraint(
            hostSub,
            rivetSub,
            new FixedConstraintConfiguration(
               new Vector3d(hostPlot.x, hostPlot.y, hostPlot.z), new Vector3d(rivetPlot.x, rivetPlot.y, rivetPlot.z), relativeOrient
            )
         );
      if (handle != null) {
         this.constraintHandles.add(handle);
      }
   }

   private void addWorldLock(ServerSubLevelContainer container, ServerSubLevel rivetSub, Vector3d worldPoint, Vector3d rivetPoint, Quaterniond orient) {
      FixedConstraintHandle handle = (FixedConstraintHandle)container.physicsSystem()
         .getPipeline()
         .addConstraint(null, rivetSub, new FixedConstraintConfiguration(worldPoint, rivetPoint, orient));
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

   protected void saveAdditional(CompoundTag tag, Provider registries) {
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

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.hostPos = BlockPos.of(tag.getLong("HostPos"));
      this.face = Direction.byName(tag.getString("Face"));
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
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public void setRemoved() {
      this.removeWorldLock();
      super.setRemoved();
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide() && this.level instanceof ServerLevel serverLevel) {
         if (Sable.HELPER.getContaining(serverLevel, this.getBlockPos()) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
            this.applyHostLock(ssl);
         }
      }
   }

   @Nullable
   private ServerSubLevel resolveHostSubLevel(ServerLevel serverLevel) {
      if (serverLevel.getBlockState(this.hostPos).isAir()) {
         return null;
      } else {
         if (Sable.HELPER.getContaining(serverLevel, this.hostPos) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
            UUID id = ssl.getUniqueId();
            if (!id.equals(this.hostSubLevelId)) {
               this.hostSubLevelId = id;
               this.setChanged();
            }

            return ssl;
         }

         return null;
      }
   }

   private void refreshHostCartDragCache(ServerLevel serverLevel) {
      ServerSubLevel hostSub = this.resolveHostSubLevel(serverLevel);
      this.hostOnCartSubLevel = hostSub != null && CoasterCartResolve.isCartSubLevel(hostSub);
   }
}
