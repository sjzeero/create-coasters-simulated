/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.sounds.SoundManager
 *  net.minecraft.core.BlockPos
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.sound;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundCache;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundInstance;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartRailSoundController {
    private static final Map<UUID, CoasterCartRailSoundInstance> ACTIVE = new HashMap<UUID, CoasterCartRailSoundInstance>();

    private CoasterCartRailSoundController() {
    }

    public static void clientTick(ClientLevel level) {
        CoasterCartRailSoundCache.beginClientTick(level);
        if (SubLevelContainer.getContainer((ClientLevel)level) == null) {
            CoasterCartRailSoundController.stopAll();
            return;
        }
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        Set<UUID> cartIds = CoasterCartRailSoundCache.bearingPlotByCart().keySet();
        for (UUID leaderId : CoasterCartRailSoundCache.soundLeaderIds()) {
            SubLevel sub;
            BlockPos bp = CoasterCartRailSoundCache.bearingPlotByCart().get(leaderId);
            if (bp == null || (sub = CoasterCartRailSoundCache.cartSubLevel(leaderId)) == null || sub.isRemoved() || ACTIVE.containsKey(leaderId)) continue;
            CoasterCartRailSoundInstance snd = new CoasterCartRailSoundInstance(sub, bp);
            sm.play((SoundInstance)snd);
            ACTIVE.put(leaderId, snd);
        }
        for (UUID id : new HashSet<UUID>(ACTIVE.keySet())) {
            CoasterCartRailSoundInstance rm;
            if (cartIds.contains(id) && CoasterCartRailSoundCache.isSoundLeader(id) || (rm = ACTIVE.remove(id)) == null) continue;
            sm.stop((SoundInstance)rm);
        }
    }

    private static void stopAll() {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        for (CoasterCartRailSoundInstance snd : ACTIVE.values()) {
            sm.stop((SoundInstance)snd);
        }
        ACTIVE.clear();
    }
}

