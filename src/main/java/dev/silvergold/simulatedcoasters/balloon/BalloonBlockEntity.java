package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BalloonBlockEntity extends BlockEntity {
   private UUID hostSubLevelId = new UUID(0L, 0L);
   private BlockPos hostPos = BlockPos.ZERO;
   private BlockPos hostLatticePos = BlockPos.ZERO;

   public BalloonBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)SimulatedCoastersBlocks.BALLOON_BE.get(), pos, state);
   }

   public void bindHost(BalloonHostKey host) {
      this.hostSubLevelId = host.hostSubLevelId();
      this.hostPos = host.cell().immutable();
      if (this.level instanceof ServerLevel serverLevel) {
         this.refreshHostLattice(serverLevel, host);
      }

      this.setChanged();
      this.notifyClientSync();
   }

   public void rebindHost(BalloonHostKey host) {
      this.bindHost(host);
   }

   public void refreshHostLattice(ServerLevel serverLevel, BalloonHostKey host) {
      ServerSubLevel hostSub = BalloonPlacement.resolveHostSub(serverLevel, host.hostSubLevelId());
      if (hostSub != null) {
         this.hostLatticePos = BalloonPlacement.plotLatticeCell(hostSub, host.cell()).immutable();
      }
   }

   public BalloonHostKey hostKey() {
      return new BalloonHostKey(this.hostSubLevelId, this.hostPos);
   }

   public BlockPos hostPos() {
      return this.hostPos;
   }

   @Nullable
   public BlockPos hostLatticePos() {
      return this.hostLatticePos.equals(BlockPos.ZERO) ? null : this.hostLatticePos;
   }

   public UUID hostSubLevelId() {
      return this.hostSubLevelId;
   }

   @Nullable
   public ServerSubLevel resolveHostSubLevel(ServerLevel serverLevel) {
      SubLevelContainer raw = SubLevelContainer.getContainer(serverLevel);
      if (raw instanceof ServerSubLevelContainer container) {
         if (!(container.getSubLevel(this.hostSubLevelId) instanceof ServerSubLevel ssl) || ssl.isRemoved()) {
            return null;
         }

         return ssl.getPlot().getEmbeddedLevelAccessor().getBlockState(this.hostPos).isAir() ? null : ssl;
      } else {
         return null;
      }
   }

   private void notifyClientSync() {
      if (this.level instanceof ServerLevel serverLevel) {
         SubLevel balloonSub = Sable.HELPER.getContaining(serverLevel, this.getBlockPos());
         if (balloonSub != null) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(balloonSub.getPlot().getCenterBlock(), state, state, 3);
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putUUID("HostSubLevel", this.hostSubLevelId);
      tag.putLong("HostPos", this.hostPos.asLong());
      if (!this.hostLatticePos.equals(BlockPos.ZERO)) {
         tag.putLong("HostLatticePos", this.hostLatticePos.asLong());
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.hostSubLevelId = tag.getUUID("HostSubLevel");
      this.hostPos = BlockPos.of(tag.getLong("HostPos"));
      this.hostLatticePos = tag.contains("HostLatticePos") ? BlockPos.of(tag.getLong("HostLatticePos")) : BlockPos.ZERO;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveWithoutMetadata(registries);
   }

   public void setRemoved() {
      if (this.level instanceof ServerLevel serverLevel) {
         SubLevel sub = Sable.HELPER.getContaining(serverLevel, this.getBlockPos());
         if (sub != null) {
            BalloonTetherController.unregister(sub.getUniqueId());
         }
      }

      super.setRemoved();
   }

   public void onLoad() {
      super.onLoad();
   }
}
