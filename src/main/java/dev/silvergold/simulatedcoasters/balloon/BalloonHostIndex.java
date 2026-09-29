package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.Nullable;

public final class BalloonHostIndex extends SavedData {
   private static final String ID = "simulatedcoasters_balloon_hosts";
   private final Map<BalloonHostKey, UUID> hostToBalloon = new HashMap<>();

   private BalloonHostIndex() {
   }

   public static BalloonHostIndex get(ServerLevel level) {
      return level.getDataStorage()
         .computeIfAbsent(new SavedData.Factory<>(BalloonHostIndex::new, BalloonHostIndex::load, null), "simulatedcoasters_balloon_hosts");
   }

   private static BalloonHostIndex load(CompoundTag tag, Provider registries) {
      BalloonHostIndex index = new BalloonHostIndex();
      if (!tag.contains("Hosts", 9)) {
         return index;
      } else {
         for (Tag entry : tag.getList("Hosts", 10)) {
            CompoundTag c = (CompoundTag)entry;
            BalloonHostKey host = new BalloonHostKey(c.getUUID("HostSubLevel"), BlockPos.of(c.getLong("HostPos")));
            index.hostToBalloon.put(host, c.getUUID("BalloonSubLevel"));
         }

         return index;
      }
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();

      for (Entry<BalloonHostKey, UUID> e : this.hostToBalloon.entrySet()) {
         CompoundTag c = new CompoundTag();
         c.putUUID("HostSubLevel", e.getKey().hostSubLevelId());
         c.putLong("HostPos", e.getKey().cell().asLong());
         c.putUUID("BalloonSubLevel", e.getValue());
         list.add(c);
      }

      tag.put("Hosts", list);
      return tag;
   }

   public boolean hasBalloonAt(BalloonHostKey host) {
      return this.hostToBalloon.containsKey(host);
   }

   public void unregisterHost(BalloonHostKey host) {
      if (this.hostToBalloon.remove(host) != null) {
         this.setDirty();
      }
   }

   public void register(BalloonHostKey host, UUID balloonSubLevelId) {
      this.hostToBalloon.put(host, balloonSubLevelId);
      this.setDirty();
   }

   public void registerLoaded(BalloonHostKey host, UUID balloonSubLevelId) {
      Iterator<Entry<BalloonHostKey, UUID>> it = this.hostToBalloon.entrySet().iterator();

      while (it.hasNext()) {
         if (it.next().getValue().equals(balloonSubLevelId)) {
            it.remove();
         }
      }

      this.hostToBalloon.put(host, balloonSubLevelId);
      this.setDirty();
   }

   public void unregisterBalloon(UUID balloonSubLevelId) {
      if (this.hostToBalloon.entrySet().removeIf(e -> e.getValue().equals(balloonSubLevelId))) {
         this.setDirty();
      }
   }

   public List<UUID> balloonsOnHostCell(BlockPos cell) {
      List<UUID> result = new ArrayList<>();

      for (Entry<BalloonHostKey, UUID> e : this.hostToBalloon.entrySet()) {
         if (e.getKey().cell().equals(cell)) {
            result.add(e.getValue());
         }
      }

      return result;
   }

   public List<UUID> balloonsOnHostLatticeCell(ServerLevel level, BlockPos latticeCell) {
      List<UUID> result = new ArrayList<>();

      for (Entry<BalloonHostKey, UUID> e : this.hostToBalloon.entrySet()) {
         BlockPos lattice = this.hostPlotLattice(level, e.getKey());
         if (lattice != null && lattice.equals(latticeCell)) {
            result.add(e.getValue());
         }
      }

      return result;
   }

   @Nullable
   public BlockPos hostPlotLattice(ServerLevel level, BalloonHostKey host) {
      ServerSubLevel hostSub = BalloonPlacement.resolveHostSub(level, host.hostSubLevelId());
      return hostSub == null ? null : BalloonPlacement.plotLatticeCell(hostSub, host.cell());
   }

   @Nullable
   public BlockPos hostPlotLattice(ServerLevel level, BalloonBlockEntity be) {
      BlockPos stored = be.hostLatticePos();
      return stored != null ? stored : this.hostPlotLattice(level, be.hostKey());
   }

   public List<Entry<BalloonHostKey, UUID>> entries() {
      return new ArrayList<>(this.hostToBalloon.entrySet());
   }

   public void rebindHost(BalloonHostKey oldHost, BalloonHostKey newHost, UUID balloonSubLevelId) {
      if (!oldHost.equals(newHost)) {
         this.hostToBalloon.remove(oldHost);
         this.hostToBalloon.put(newHost, balloonSubLevelId);
         this.setDirty();
      }
   }

   public void breakBalloonsOnHostCell(ServerLevel level, BlockPos cell) {
      for (UUID balloonId : new ArrayList<>(this.balloonsOnHostLatticeCell(level, cell))) {
         ServerSubLevel balloon = BalloonResolve.findBalloonById(level, balloonId);
         if (balloon != null) {
            BalloonRemoval.onHostBroken(level, balloon);
         }
      }
   }

   public void breakBalloonsOnHostSubLevel(ServerLevel level, UUID hostSubLevelId) {
      for (Entry<BalloonHostKey, UUID> entry : new ArrayList<>(this.entries())) {
         if (entry.getKey().hostSubLevelId().equals(hostSubLevelId)) {
            ServerSubLevel balloon = BalloonResolve.findBalloonById(level, entry.getValue());
            if (balloon != null) {
               BalloonRemoval.onHostBroken(level, balloon);
            }
         }
      }
   }

   @Nullable
   public ServerSubLevel findByHost(ServerLevel level, BalloonHostKey host) {
      UUID id = this.hostToBalloon.get(host);
      return id == null ? null : BalloonResolve.findBalloonById(level, id);
   }
}
