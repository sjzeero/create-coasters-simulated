/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetAnchorKey;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetRemoval;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public final class RivetHostIndex
extends SavedData {
    private static final String ID = "simulatedcoasters_rivet_hosts";
    private final Map<RivetAnchorKey, UUID> anchorToSubLevel = new HashMap<RivetAnchorKey, UUID>();

    private RivetHostIndex() {
    }

    public static RivetHostIndex get(ServerLevel level) {
        return (RivetHostIndex)level.getDataStorage().computeIfAbsent(new SavedData.Factory(RivetHostIndex::new, RivetHostIndex::load, null), ID);
    }

    private static RivetHostIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        RivetHostIndex index;
        block4: {
            block3: {
                index = new RivetHostIndex();
                if (!tag.contains("Anchors", 9)) break block3;
                ListTag list = tag.getList("Anchors", 10);
                for (Tag entry : list) {
                    index.loadAnchorEntry((CompoundTag)entry);
                }
                break block4;
            }
            if (!tag.contains("Hosts", 9)) break block4;
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
        RivetHostKey host = new RivetHostKey(hostSubLevelId, BlockPos.of((long)c.getLong("Host")));
        Direction face = Direction.byName((String)c.getString("Face"));
        if (face == null) {
            face = Direction.UP;
        }
        double lx = c.contains("AttachLx", 6) ? c.getDouble("AttachLx") : 0.5;
        double ly = c.contains("AttachLy", 6) ? c.getDouble("AttachLy") : 0.0;
        double lz = c.contains("AttachLz", 6) ? c.getDouble("AttachLz") : 0.5;
        this.anchorToSubLevel.put(new RivetAnchorKey(host, face, lx, ly, lz), c.getUUID("SubLevel"));
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<RivetAnchorKey, UUID> e : this.anchorToSubLevel.entrySet()) {
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
            list.add((Object)c);
        }
        tag.put("Anchors", (Tag)list);
        return tag;
    }

    public boolean hasRivetAt(RivetAnchorKey anchor) {
        return this.anchorToSubLevel.containsKey(anchor);
    }

    public List<RivetAnchorKey> anchorsOnHost(RivetHostKey host) {
        ArrayList<RivetAnchorKey> result = new ArrayList<RivetAnchorKey>();
        for (RivetAnchorKey anchor : this.anchorToSubLevel.keySet()) {
            if (!anchor.host().equals(host)) continue;
            result.add(anchor);
        }
        return result;
    }

    public List<RivetAnchorKey> anchorsOnHostCell(BlockPos cell) {
        ArrayList<RivetAnchorKey> result = new ArrayList<RivetAnchorKey>();
        for (RivetAnchorKey anchor : this.anchorToSubLevel.keySet()) {
            if (!anchor.host().cell().equals((Object)cell)) continue;
            result.add(anchor);
        }
        return result;
    }

    public void register(RivetAnchorKey anchor, UUID subLevelId) {
        this.anchorToSubLevel.put(anchor, subLevelId);
        this.setDirty();
    }

    public void registerLoadedRivet(RivetAnchorKey anchor, UUID subLevelId) {
        boolean changed = false;
        Iterator<Map.Entry<RivetAnchorKey, UUID>> it = this.anchorToSubLevel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RivetAnchorKey, UUID> entry = it.next();
            if (!entry.getValue().equals(subLevelId) || entry.getKey().equals(anchor)) continue;
            it.remove();
            changed = true;
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
        Iterator<Map.Entry<RivetAnchorKey, UUID>> it = this.anchorToSubLevel.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            if (!it.next().getValue().equals(subLevelId)) continue;
            it.remove();
            changed = true;
        }
        if (changed) {
            this.setDirty();
        }
    }

    public void breakRivetsOnHost(ServerLevel level, RivetHostKey host) {
        ArrayList<RivetAnchorKey> anchors = new ArrayList<RivetAnchorKey>(this.anchorsOnHost(host));
        for (RivetAnchorKey anchor : anchors) {
            this.breakRivetAtAnchor(level, anchor);
        }
    }

    public void breakRivetsOnHostCell(ServerLevel level, BlockPos cell) {
        ArrayList<RivetAnchorKey> anchors = new ArrayList<RivetAnchorKey>(this.anchorsOnHostCell(cell));
        for (RivetAnchorKey anchor : anchors) {
            this.breakRivetAtAnchor(level, anchor);
        }
    }

    private void breakRivetAtAnchor(ServerLevel level, RivetAnchorKey anchor) {
        ServerSubLevel sub;
        UUID id = this.removeAnchor(anchor);
        if (id == null) {
            return;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        SubLevel rawSub = container.getSubLevel(id);
        if (rawSub instanceof ServerSubLevel && !(sub = (ServerSubLevel)rawSub).isRemoved()) {
            RivetRemoval.onHostBroken(level, sub);
        }
    }

    @Nullable
    public static ServerSubLevel findByAnchor(ServerLevel level, RivetAnchorKey anchor) {
        ServerSubLevel sub;
        RivetHostIndex index = RivetHostIndex.get(level);
        UUID id = index.anchorToSubLevel.get(anchor);
        if (id == null) {
            return null;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel rawSub = container.getSubLevel(id);
        if (rawSub instanceof ServerSubLevel && !(sub = (ServerSubLevel)rawSub).isRemoved()) {
            return sub;
        }
        return null;
    }
}

