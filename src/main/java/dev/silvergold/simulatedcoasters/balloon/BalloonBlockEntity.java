/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BalloonBlockEntity
extends BlockEntity {
    private UUID hostSubLevelId = new UUID(0L, 0L);
    private BlockPos hostPos = BlockPos.ZERO;
    private BlockPos hostLatticePos = BlockPos.ZERO;

    public BalloonBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)SimulatedCoastersBlocks.BALLOON_BE.get(), pos, state);
    }

    public void bindHost(BalloonHostKey host) {
        this.hostSubLevelId = host.hostSubLevelId();
        this.hostPos = host.cell().immutable();
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.refreshHostLattice(serverLevel, host);
        }
        this.setChanged();
        this.notifyClientSync();
    }

    public void rebindHost(BalloonHostKey host) {
        this.bindHost(host);
    }

    public void refreshHostLattice(ServerLevel serverLevel, BalloonHostKey host) {
        ServerSubLevel hostSub = BalloonPlacement.resolveHostSub((Level)serverLevel, host.hostSubLevelId());
        if (hostSub != null) {
            this.hostLatticePos = BalloonPlacement.plotLatticeCell((SubLevel)hostSub, host.cell()).immutable();
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
        return this.hostLatticePos.equals((Object)BlockPos.ZERO) ? null : this.hostLatticePos;
    }

    public UUID hostSubLevelId() {
        return this.hostSubLevelId;
    }

    @Nullable
    public ServerSubLevel resolveHostSubLevel(ServerLevel serverLevel) {
        ServerSubLevel ssl;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)serverLevel);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(this.hostSubLevelId);
        if (!(sub instanceof ServerSubLevel) || (ssl = (ServerSubLevel)sub).isRemoved()) {
            return null;
        }
        if (ssl.getPlot().getEmbeddedLevelAccessor().getBlockState(this.hostPos).isAir()) {
            return null;
        }
        return ssl;
    }

    private void notifyClientSync() {
        Level level = this.level;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        SubLevel balloonSub = Sable.HELPER.getContaining((Level)serverLevel, (Vec3i)this.getBlockPos());
        if (balloonSub == null) {
            return;
        }
        BlockState state = this.getBlockState();
        serverLevel.sendBlockUpdated(balloonSub.getPlot().getCenterBlock(), state, state, 3);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("HostSubLevel", this.hostSubLevelId);
        tag.putLong("HostPos", this.hostPos.asLong());
        if (!this.hostLatticePos.equals((Object)BlockPos.ZERO)) {
            tag.putLong("HostLatticePos", this.hostLatticePos.asLong());
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.hostSubLevelId = tag.getUUID("HostSubLevel");
        this.hostPos = BlockPos.of((long)tag.getLong("HostPos"));
        this.hostLatticePos = tag.contains("HostLatticePos") ? BlockPos.of((long)tag.getLong("HostLatticePos")) : BlockPos.ZERO;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public void setRemoved() {
        ServerLevel serverLevel;
        SubLevel sub;
        Level level = this.level;
        if (level instanceof ServerLevel && (sub = Sable.HELPER.getContaining((Level)(serverLevel = (ServerLevel)level), (Vec3i)this.getBlockPos())) != null) {
            BalloonTetherController.unregister(sub.getUniqueId());
        }
        super.setRemoved();
    }

    public void onLoad() {
        super.onLoad();
    }
}

