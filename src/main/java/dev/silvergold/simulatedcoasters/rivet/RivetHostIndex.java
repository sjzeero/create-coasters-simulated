package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.Nullable;

public final class RivetHostIndex extends SavedData {
   private static final String ID = "simulatedcoasters_rivet_hosts";
   private final Map<RivetAnchorKey, UUID> anchorToSubLevel = new HashMap<>();

   private RivetHostIndex() {
   }

   public static RivetHostIndex get(ServerLevel level) {
      return level.getDataStorage()
         .computeIfAbsent(new SavedData.Factory<>(RivetHostIndex::new, RivetHostIndex::load, null), "simulatedcoasters_rivet_hosts");
   }

   private static RivetHostIndex load(CompoundTag tag, Provider registries) {
      RivetHostIndex index = new RivetHostIndex();
      if (tag.contains("Anchors", 9)) {
         for (Tag entry : tag.getList("Anchors", 10)) {
            index.loadAnchorEntry((CompoundTag)entry);
         }
      } else if (tag.contains("Hosts", 9)) {
         for (Tag entry : tag.getList("Hosts", 10)) {
            CompoundTag c = (CompoundTag)entry;
            CompoundTag migrated = new CompoundTag();
            migrated.putLong("Host", c.getLong("Host"));
            if (c.hasUUID("HostSubLevel")) {
               migrated.putUUID("HostSubLevel", c.getUUID("HostSubLevel"));
            }

            migrated.putString("Face", Direction.UP.getSerializedName());
            migrated.putDouble("AttachLx", 0.5);
            migrated.putDouble("AttachLy", 0.0);
            migrated.putDouble("AttachLz", 0.5);
            migrated.putUUID("SubLevel", c.getUUID("SubLevel"));
            index.loadAnchorEntry(migrated);
         }
      }

      return index;
   }

   private void loadAnchorEntry(CompoundTag c) {
      UUID hostSubLevelId = c.hasUUID("HostSubLevel") ? c.getUUID("HostSubLevel") : null;
      RivetHostKey host = new RivetHostKey(hostSubLevelId, BlockPos.of(c.getLong("Host")));
      Direction face = Direction.byName(c.getString("Face"));
      if (face == null) {
         face = Direction.UP;
      }

      double lx = c.contains("AttachLx", 6) ? c.getDouble("AttachLx") : 0.5;
      double ly = c.contains("AttachLy", 6) ? c.getDouble("AttachLy") : 0.0;
      double lz = c.contains("AttachLz", 6) ? c.getDouble("AttachLz") : 0.5;
      this.anchorToSubLevel.put(new RivetAnchorKey(host, face, lx, ly, lz), c.getUUID("SubLevel"));
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();

      for (Entry<RivetAnchorKey, UUID> e : this.anchorToSubLevel.entrySet()) {
         CompoundTag c = new CompoundTag();
         RivetAnchorKey anchor = e.getKey();
         c.putLong("Host", anchor.host().cell().asLong());
         if (anchor.host().hostSubLevelId() != null) {
            c.putUUID("HostSubLevel", anchor.host().hostSubLevelId());
         }

         c.putString("Face", anchor.face().getSerializedName());
         c.putDouble("AttachLx", anchor.lx());
         c.putDouble("AttachLy", anchor.ly());
         c.putDouble("AttachLz", anchor.lz());
         c.putUUID("SubLevel", e.getValue());
         list.add(c);
      }

      tag.put("Anchors", list);
      return tag;
   }

   public boolean hasRivetAt(RivetAnchorKey anchor) {
      return this.anchorToSubLevel.containsKey(anchor);
   }

   public List<RivetAnchorKey> anchorsOnHost(RivetHostKey host) {
      List<RivetAnchorKey> result = new ArrayList<>();

      for (RivetAnchorKey anchor : this.anchorToSubLevel.keySet()) {
         if (anchor.host().equals(host)) {
            result.add(anchor);
         }
      }

      return result;
   }

   public List<RivetAnchorKey> anchorsOnHostCell(BlockPos cell) {
      List<RivetAnchorKey> result = new ArrayList<>();

      for (RivetAnchorKey anchor : this.anchorToSubLevel.keySet()) {
         if (anchor.host().cell().equals(cell)) {
            result.add(anchor);
         }
      }

      return result;
   }

   public void register(RivetAnchorKey anchor, UUID subLevelId) {
      this.anchorToSubLevel.put(anchor, subLevelId);
      this.setDirty();
   }

   public void registerLoadedRivet(RivetAnchorKey anchor, UUID subLevelId) {
      boolean changed = false;
      Iterator<Entry<RivetAnchorKey, UUID>> it = this.anchorToSubLevel.entrySet().iterator();

      while (it.hasNext()) {
         Entry<RivetAnchorKey, UUID> entry = it.next();
         if (entry.getValue().equals(subLevelId) && !entry.getKey().equals(anchor)) {
            it.remove();
            changed = true;
         }
      }

      UUID previous = this.anchorToSubLevel.put(anchor, subLevelId);
      if (!subLevelId.equals(previous) || changed) {
         this.setDirty();
      }
   }

   @Nullable
   public UUID removeAnchor(RivetAnchorKey anchor) {
      UUID removed = this.anchorToSubLevel.remove(anchor);
      if (removed != null) {
         this.setDirty();
      }

      return removed;
   }

   public void unregisterRivet(UUID subLevelId) {
      Iterator<Entry<RivetAnchorKey, UUID>> it = this.anchorToSubLevel.entrySet().iterator();
      boolean changed = false;

      while (it.hasNext()) {
         if (it.next().getValue().equals(subLevelId)) {
            it.remove();
            changed = true;
         }
      }

      if (changed) {
         this.setDirty();
      }
   }

   public void breakRivetsOnHost(ServerLevel level, RivetHostKey host) {
      for (RivetAnchorKey anchor : new ArrayList<>(this.anchorsOnHost(host))) {
         this.breakRivetAtAnchor(level, anchor);
      }
   }

   public void breakRivetsOnHostCell(ServerLevel level, BlockPos cell) {
      for (RivetAnchorKey anchor : new ArrayList<>(this.anchorsOnHostCell(cell))) {
         this.breakRivetAtAnchor(level, anchor);
      }
   }

   private void breakRivetAtAnchor(ServerLevel level, RivetAnchorKey anchor) {
      UUID id = this.removeAnchor(anchor);
      if (id != null) {
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer container) {
            if (container.getSubLevel(id) instanceof ServerSubLevel sub && !sub.isRemoved()) {
               RivetRemoval.onHostBroken(level, sub);
            }
         }
      }
   }

   @Nullable
   public static ServerSubLevel findByAnchor(ServerLevel level, RivetAnchorKey anchor) {
      RivetHostIndex index = get(level);
      UUID id = index.anchorToSubLevel.get(anchor);
      if (id == null) {
         return null;
      } else {
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer container) {
            if (container.getSubLevel(id) instanceof ServerSubLevel sub && !sub.isRemoved()) {
               return sub;
            }

            return null;
         } else {
            return null;
         }
      }
   }
}
