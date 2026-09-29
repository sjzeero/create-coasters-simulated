/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

final class CoasterCartTrainLinkData {
    static final int MAX_LINKS = 2;
    static final String USER_DATA_LINKS = "simulatedcoasters:cart_train_links";
    private static final String USER_DATA_LINK_LEGACY = "simulatedcoasters:cart_train_link";
    private static final String PARTNER_TAG = "partner";
    private static final String DISTANCE_TAG = "distance";

    private CoasterCartTrainLinkData() {
    }

    static List<LinkRecord> readAll(ServerSubLevel cart) {
        CompoundTag root = cart.getUserDataTag();
        if (root == null) {
            return List.of();
        }
        if (root.contains(USER_DATA_LINKS, 9)) {
            return CoasterCartTrainLinkData.readList(root.getList(USER_DATA_LINKS, 10));
        }
        LinkRecord legacy = CoasterCartTrainLinkData.readLegacyEntry(root);
        if (legacy != null) {
            return List.of(legacy);
        }
        return List.of();
    }

    static boolean hasLink(ServerSubLevel cart, UUID partnerId) {
        for (LinkRecord r : CoasterCartTrainLinkData.readAll(cart)) {
            if (!r.partnerId().equals(partnerId)) continue;
            return true;
        }
        return false;
    }

    static int linkCount(ServerSubLevel cart) {
        return CoasterCartTrainLinkData.readAll(cart).size();
    }

    static boolean canAddLink(ServerSubLevel cart, UUID partnerId) {
        if (CoasterCartTrainLinkData.hasLink(cart, partnerId)) {
            return false;
        }
        return CoasterCartTrainLinkData.linkCount(cart) < 2;
    }

    static void addLink(ServerSubLevel cart, UUID partnerId, double centerDistance) {
        ArrayList<LinkRecord> links = new ArrayList<LinkRecord>(CoasterCartTrainLinkData.readAll(cart));
        for (LinkRecord r : links) {
            if (!r.partnerId().equals(partnerId)) continue;
            return;
        }
        if (links.size() >= 2) {
            return;
        }
        links.add(new LinkRecord(partnerId, centerDistance));
        CoasterCartTrainLinkData.writeAll(cart, links);
    }

    static void removeLink(ServerSubLevel cart, UUID partnerId) {
        ArrayList<LinkRecord> links = new ArrayList<LinkRecord>(CoasterCartTrainLinkData.readAll(cart));
        if (links.removeIf(r -> r.partnerId().equals(partnerId))) {
            CoasterCartTrainLinkData.writeAll(cart, links);
        }
    }

    static void setLinkDistance(ServerSubLevel cart, UUID partnerId, double centerDistance) {
        ArrayList<LinkRecord> links = new ArrayList<LinkRecord>(CoasterCartTrainLinkData.readAll(cart));
        for (int i = 0; i < links.size(); ++i) {
            if (!((LinkRecord)links.get(i)).partnerId().equals(partnerId)) continue;
            links.set(i, new LinkRecord(partnerId, centerDistance));
            CoasterCartTrainLinkData.writeAll(cart, links);
            return;
        }
    }

    static void clearAll(ServerSubLevel cart) {
        CompoundTag root = cart.getUserDataTag();
        if (root == null) {
            return;
        }
        root.remove(USER_DATA_LINKS);
        root.remove(USER_DATA_LINK_LEGACY);
        if (root.isEmpty()) {
            cart.setUserDataTag(null);
        } else {
            cart.setUserDataTag(root);
        }
    }

    private static void writeAll(ServerSubLevel cart, List<LinkRecord> links) {
        CompoundTag root = cart.getUserDataTag();
        if (root == null) {
            root = new CompoundTag();
        }
        root.remove(USER_DATA_LINK_LEGACY);
        if (links.isEmpty()) {
            root.remove(USER_DATA_LINKS);
            if (root.isEmpty()) {
                cart.setUserDataTag(null);
            } else {
                cart.setUserDataTag(root);
            }
            return;
        }
        ListTag list = new ListTag();
        for (LinkRecord r : links) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(PARTNER_TAG, r.partnerId());
            entry.putDouble(DISTANCE_TAG, r.centerDistance());
            list.add((Object)entry);
        }
        root.put(USER_DATA_LINKS, (Tag)list);
        cart.setUserDataTag(root);
    }

    private static List<LinkRecord> readList(ListTag list) {
        ArrayList<LinkRecord> out = new ArrayList<LinkRecord>(list.size());
        for (Tag tag : list) {
            CompoundTag entry;
            if (!(tag instanceof CompoundTag) || !(entry = (CompoundTag)tag).hasUUID(PARTNER_TAG)) continue;
            out.add(new LinkRecord(entry.getUUID(PARTNER_TAG), entry.getDouble(DISTANCE_TAG)));
        }
        return out;
    }

    @Nullable
    private static LinkRecord readLegacyEntry(CompoundTag root) {
        if (!root.contains(USER_DATA_LINK_LEGACY, 10)) {
            return null;
        }
        CompoundTag link = root.getCompound(USER_DATA_LINK_LEGACY);
        if (!link.hasUUID(PARTNER_TAG)) {
            return null;
        }
        return new LinkRecord(link.getUUID(PARTNER_TAG), link.getDouble(DISTANCE_TAG));
    }

    record LinkRecord(UUID partnerId, double centerDistance) {
    }
}

