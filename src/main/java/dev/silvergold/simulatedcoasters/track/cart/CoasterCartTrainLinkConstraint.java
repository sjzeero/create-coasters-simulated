/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipeline
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis
 *  dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration
 *  dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle
 *  dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3d
 *  org.joml.Matrix3dc
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartIdIndex;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkDistanceEdit;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkData;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkQueries;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartTrainLinkConstraint {
    private static final Set<ConstraintJointAxis> TRAIN_LINK_LOCKED_AXES = EnumSet.of(ConstraintJointAxis.LINEAR_X);
    private static final double MIN_LINK_DISTANCE = 0.05;
    private static final double TENSION_BREAK_STRETCH_BLOCKS = 0.5;
    private static final double TENSION_BREAK_STRETCH_FOR_FORCE_BLOCKS = 0.07;
    private static final double TENSION_BREAK_FORCE = 1800.0;
    private static final double TENSION_SNAP_VELOCITY_RETAIN = 0.08;
    private static final Map<String, ActiveLink> ACTIVE_LINKS = new ConcurrentHashMap<String, ActiveLink>();
    private static final Map<ResourceKey<Level>, Long> LAST_LINK_RESTORE_GAME_TIME = new ConcurrentHashMap<ResourceKey<Level>, Long>();

    private CoasterCartTrainLinkConstraint() {
    }

    public static void tryLinkFromItemSelection(ServerLevel level, ServerSubLevel newCart, ItemStack stack) {
        CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(level, newCart, stack, null);
    }

    public static void tryLinkFromItemSelection(ServerLevel level, ServerSubLevel newCart, ItemStack stack, @Nullable ServerPlayer player) {
        UUID partnerId = CoasterCartLinkSelection.readLinkTarget(stack);
        if (partnerId == null) {
            return;
        }
        ServerSubLevel partner = CoasterCartResolve.findCartById(level, partnerId);
        if (partner == null || partner == newCart || partner.isRemoved()) {
            CoasterCartLinkSelection.clearLinkTarget(stack);
            SimulatedCoasters.LOGGER.info("[cart-link] selection {} could not be resolved to a live cart; not linking", (Object)partnerId);
            return;
        }
        CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, partner);
        if (!CoasterCartTrainLinkConstraint.linkCarts(level, partner, newCart)) {
            return;
        }
        CoasterCartLinkSelection.clearLinkTarget(stack);
    }

    public static boolean linkCarts(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB) {
        Vector3d centerA = CoasterCartResolve.cartCenterWorld((SubLevel)cartA);
        Vector3d centerB = CoasterCartResolve.cartCenterWorld((SubLevel)cartB);
        if (centerA == null || centerB == null) {
            SimulatedCoasters.LOGGER.warn("[cart-link] missing bearing cell on one of the carts; not linking");
            return false;
        }
        double distance = Math.max(centerA.distance((Vector3dc)centerB), 0.05);
        return CoasterCartTrainLinkConstraint.linkCarts(level, cartA, cartB, distance);
    }

    public static boolean linkCarts(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB, double centerDistance) {
        if (cartA == cartB) {
            return false;
        }
        CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, cartA);
        CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, cartB);
        if (CoasterCartTrainLinkData.hasLink(cartA, cartB.getUniqueId())) {
            return false;
        }
        if (!CoasterCartTrainLinkData.canAddLink(cartA, cartB.getUniqueId()) || !CoasterCartTrainLinkData.canAddLink(cartB, cartA.getUniqueId())) {
            SimulatedCoasters.LOGGER.info("[cart-link] link refused \u2014 cart {} and/or {} at {}-link limit", new Object[]{cartA.getUniqueId(), cartB.getUniqueId(), 2});
            return false;
        }
        if (CoasterCartTrainLinkConstraint.wouldFormLoop((SubLevel)cartA, (SubLevel)cartB)) {
            SimulatedCoasters.LOGGER.info("[cart-link] link refused \u2014 carts {} and {} are already in the same train", (Object)cartA.getUniqueId(), (Object)cartB.getUniqueId());
            return false;
        }
        double distance = Math.max(centerDistance, 0.05);
        CoasterCartTrainLinkData.addLink(cartA, cartB.getUniqueId(), distance);
        CoasterCartTrainLinkData.addLink(cartB, cartA.getUniqueId(), distance);
        CoasterCartTrainLinkSync.broadcastCart(cartA);
        CoasterCartTrainLinkSync.broadcastCart(cartB);
        ACTIVE_LINKS.put(CoasterCartTrainLinkConstraint.pairKey(cartA.getUniqueId(), cartB.getUniqueId()), new ActiveLink((ResourceKey<Level>)level.dimension(), cartA.getUniqueId(), cartB.getUniqueId(), distance));
        SimulatedCoasters.LOGGER.info("[cart-link] linked carts {} <-> {} at distance {}", new Object[]{cartA.getUniqueId(), cartB.getUniqueId(), String.format("%.3f", distance)});
        Vector3d centerA = CoasterCartResolve.cartCenterWorld((SubLevel)cartA);
        Vector3d centerB = CoasterCartResolve.cartCenterWorld((SubLevel)cartB);
        if (centerA != null && centerB != null) {
            Vector3d mid = new Vector3d((Vector3dc)centerA).add((Vector3dc)centerB).mul(0.5);
            level.playSound(null, BlockPos.containing((double)mid.x, (double)mid.y, (double)mid.z), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.15f);
        }
        return true;
    }

    public static boolean adjustEditModeLinkDistances(ServerLevel level, ServerSubLevel hostCart, boolean shorten) {
        List<CoasterCartLinkDistanceEdit.AdjustedLink> adjustments = CoasterCartLinkDistanceEdit.computeAdjustments(hostCart, shorten);
        if (adjustments.isEmpty()) {
            return false;
        }
        for (CoasterCartLinkDistanceEdit.AdjustedLink adjustment : adjustments) {
            CoasterCartTrainLinkConstraint.setPairLinkDistance(level, hostCart, adjustment.partner(), adjustment.newDistance());
        }
        Vector3d center = CoasterCartResolve.cartCenterWorld((SubLevel)hostCart);
        if (center != null) {
            level.playSound(null, BlockPos.containing((double)center.x, (double)center.y, (double)center.z), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.75f, shorten ? 0.95f : 1.1f);
        }
        return true;
    }

    private static void setPairLinkDistance(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB, double distance) {
        UUID idA = cartA.getUniqueId();
        UUID idB = cartB.getUniqueId();
        CoasterCartTrainLinkData.setLinkDistance(cartA, idB, distance);
        CoasterCartTrainLinkData.setLinkDistance(cartB, idA, distance);
        CoasterCartTrainLinkSync.broadcastCart(cartA);
        CoasterCartTrainLinkSync.broadcastCart(cartB);
        String key = CoasterCartTrainLinkConstraint.pairKey(idA, idB);
        ActiveLink existing = ACTIVE_LINKS.remove(key);
        if (existing != null && existing.handle != null && existing.handle.isValid()) {
            existing.handle.remove();
        }
        ACTIVE_LINKS.put(key, new ActiveLink((ResourceKey<Level>)level.dimension(), idA, idB, distance));
        SimulatedCoasters.LOGGER.info("[cart-link] set distance {} <-> {} to {}", new Object[]{idA, idB, String.format("%.3f", distance)});
    }

    public static void tryRestoreLink(ServerSubLevel cart, ServerLevel level) {
        for (CoasterCartTrainLinkData.LinkRecord data : CoasterCartTrainLinkData.readAll(cart)) {
            String key = CoasterCartTrainLinkConstraint.pairKey(cart.getUniqueId(), data.partnerId());
            if (ACTIVE_LINKS.containsKey(key)) continue;
            ServerSubLevel partner = CoasterCartResolve.findCartById(level, data.partnerId());
            if (partner == null || partner.isRemoved()) {
                if (!CoasterCartTrainLinkConstraint.isDefunctLinkPartner(level, data.partnerId())) continue;
                CoasterCartTrainLinkConstraint.unlinkPair(level, cart.getUniqueId(), data.partnerId());
                continue;
            }
            ACTIVE_LINKS.put(key, new ActiveLink((ResourceKey<Level>)level.dimension(), cart.getUniqueId(), data.partnerId(), data.centerDistance()));
            SimulatedCoasters.LOGGER.info("[cart-link] restored link {} <-> {} (distance {})", new Object[]{cart.getUniqueId(), data.partnerId(), data.centerDistance()});
        }
    }

    public static boolean clearAllLinks(ServerLevel level, ServerSubLevel cart) {
        if (cart.isRemoved()) {
            return false;
        }
        List<CoasterCartTrainLinkData.LinkRecord> links = CoasterCartTrainLinkData.readAll(cart);
        if (links.isEmpty()) {
            return false;
        }
        UUID cartId = cart.getUniqueId();
        for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(links)) {
            CoasterCartTrainLinkConstraint.unlinkPair(level, cartId, link.partnerId());
        }
        Vector3d center = CoasterCartResolve.cartCenterWorld((SubLevel)cart);
        if (center != null) {
            BlockPos pos = BlockPos.containing((double)center.x, (double)center.y, (double)center.z);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.9f, 1.0f);
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.85f, 0.95f);
        }
        return true;
    }

    public static void onCartRemoved(ServerSubLevel cart) {
        ServerLevel level = cart.getLevel();
        UUID cartId = cart.getUniqueId();
        CoasterCartBearingTickCache.remove(cart);
        CoasterCartIdIndex.removeCart(level, cart);
        for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
            CoasterCartTrainLinkConstraint.unlinkPair(level, cartId, link.partnerId());
        }
        CoasterCartTrainLinkData.clearAll(cart);
        CoasterCartTrainLinkSync.broadcastCart(level, cartId, null);
    }

    public static void pruneDefunctLinks(ServerLevel level, ServerSubLevel cart) {
        for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
            if (!CoasterCartTrainLinkConstraint.isDefunctLinkPartner(level, link.partnerId())) continue;
            CoasterCartTrainLinkConstraint.unlinkPair(level, cart.getUniqueId(), link.partnerId());
        }
    }

    public static void pruneInvalidLinksForCoupling(ServerLevel level, ServerSubLevel cart) {
        for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
            if (!CoasterCartTrainLinkConstraint.isDefunctLinkPartner(level, link.partnerId()) && CoasterCartResolve.findCartById(level, link.partnerId()) != null) continue;
            CoasterCartTrainLinkConstraint.unlinkPair(level, cart.getUniqueId(), link.partnerId());
        }
    }

    public static void tick(SubLevelPhysicsSystem physics, ServerLevel level) {
        CoasterCartTrainLinkConstraint.maybeRestorePersistedLinks(level);
        if (ACTIVE_LINKS.isEmpty()) {
            return;
        }
        for (ActiveLink link : List.copyOf(ACTIVE_LINKS.values())) {
            if (!link.dimension.equals((Object)level.dimension())) continue;
            ServerSubLevel cartA = CoasterCartResolve.findCartById(level, link.cartA);
            ServerSubLevel cartB = CoasterCartResolve.findCartById(level, link.cartB);
            if (cartA == null || cartB == null) {
                if (CoasterCartTrainLinkConstraint.isDefunctLinkPartner(level, link.cartA) || CoasterCartTrainLinkConstraint.isDefunctLinkPartner(level, link.cartB)) {
                    CoasterCartTrainLinkConstraint.unlinkPair(level, link.cartA, link.cartB);
                    continue;
                }
                CoasterCartTrainLinkConstraint.suspendActiveLink(link);
                continue;
            }
            if (cartA.isRemoved() || cartB.isRemoved()) {
                CoasterCartTrainLinkConstraint.unlinkPair(level, link.cartA, link.cartB);
                continue;
            }
            CoasterCartTrainLinkConstraint.updateConstraint(physics, link, cartA, cartB);
        }
    }

    public static void restorePersistedLinks(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (!CoasterCartResolve.isCartSubLevel((SubLevel)sub)) continue;
            CoasterCartTrainLinkConstraint.pruneDefunctLinks(level, sub);
            CoasterCartTrainLinkConstraint.tryRestoreLink(sub, level);
        }
    }

    private static void maybeRestorePersistedLinks(ServerLevel level) {
        long gameTime = level.getGameTime();
        ResourceKey dimension = level.dimension();
        Long last = LAST_LINK_RESTORE_GAME_TIME.get(dimension);
        if (last != null && last == gameTime) {
            return;
        }
        LAST_LINK_RESTORE_GAME_TIME.put((ResourceKey<Level>)dimension, gameTime);
        CoasterCartTrainLinkConstraint.restorePersistedLinks(level);
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        ACTIVE_LINKS.entrySet().removeIf(e -> ((ActiveLink)e.getValue()).dimension.equals((Object)dimension));
        LAST_LINK_RESTORE_GAME_TIME.remove(dimension);
    }

    public static boolean areLinkedCarts(SubLevel a, SubLevel b) {
        ServerSubLevel serverB;
        ServerSubLevel serverA;
        if (a == null || b == null || a == b) {
            return a == b;
        }
        if (!CoasterCartResolve.isCartSubLevel(a) || !CoasterCartResolve.isCartSubLevel(b)) {
            return false;
        }
        UUID aId = a.getUniqueId();
        UUID bId = b.getUniqueId();
        if (a instanceof ServerSubLevel && CoasterCartTrainLinkData.hasLink(serverA = (ServerSubLevel)a, bId)) {
            return true;
        }
        if (b instanceof ServerSubLevel && CoasterCartTrainLinkData.hasLink(serverB = (ServerSubLevel)b, aId)) {
            return true;
        }
        if (a.getLevel() == null) {
            return false;
        }
        if (a.getLevel().isClientSide()) {
            return CoasterCartTrainLinkQueries.clientHasLinkBetween((ResourceKey<Level>)a.getLevel().dimension(), aId, bId);
        }
        return CoasterCartTrainLinkConstraint.activeLinkBetween(aId, bId, (ResourceKey<Level>)a.getLevel().dimension());
    }

    private static boolean activeLinkBetween(UUID aId, UUID bId, ResourceKey<Level> dimension) {
        for (ActiveLink link : ACTIVE_LINKS.values()) {
            if (!link.dimension.equals(dimension) || (!link.cartA.equals(aId) || !link.cartB.equals(bId)) && (!link.cartA.equals(bId) || !link.cartB.equals(aId))) continue;
            return true;
        }
        return false;
    }

    public static boolean areAlreadyLinked(SubLevel cartA, SubLevel cartB) {
        if (cartA == null || cartB == null || cartA == cartB) {
            return false;
        }
        UUID aId = cartA.getUniqueId();
        UUID bId = cartB.getUniqueId();
        return CoasterCartTrainLinkConstraint.partnerIdsFor(cartA).contains(bId) || CoasterCartTrainLinkConstraint.partnerIdsFor(cartB).contains(aId);
    }

    public static boolean wouldFormLoop(SubLevel cartA, SubLevel cartB) {
        if (cartA == null || cartB == null || cartA == cartB) {
            return false;
        }
        UUID aId = cartA.getUniqueId();
        UUID bId = cartB.getUniqueId();
        if (CoasterCartTrainLinkConstraint.areAlreadyLinked(cartA, cartB)) {
            return false;
        }
        return CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(cartA).contains(bId);
    }

    public static boolean wouldExceedLinkLimit(SubLevel cartA, SubLevel cartB) {
        if (cartA == null || cartB == null || cartA == cartB) {
            return false;
        }
        if (CoasterCartTrainLinkConstraint.areAlreadyLinked(cartA, cartB)) {
            return false;
        }
        return CoasterCartTrainLinkConstraint.livePartnerCount(cartA) >= 2 || CoasterCartTrainLinkConstraint.livePartnerCount(cartB) >= 2;
    }

    public static boolean isAtLinkLimit(SubLevel cart) {
        return cart != null && CoasterCartTrainLinkConstraint.livePartnerCount(cart) >= 2;
    }

    private static int livePartnerCount(SubLevel cart) {
        if (cart.getLevel() == null) {
            return 0;
        }
        int count = 0;
        for (UUID partnerId : CoasterCartTrainLinkConstraint.partnerIdsFor(cart)) {
            SubLevel partner = CoasterCartResolve.findCartById(cart.getLevel(), partnerId);
            if (partner == null || partner.isRemoved()) continue;
            ++count;
        }
        return count;
    }

    @Nullable
    public static String coupleRejectLangKey(SubLevel cartA, SubLevel cartB) {
        if (CoasterCartTrainLinkConstraint.areAlreadyLinked(cartA, cartB)) {
            return "simulatedcoasters.cart.link_already";
        }
        if (CoasterCartTrainLinkConstraint.wouldFormLoop(cartA, cartB)) {
            return "simulatedcoasters.cart.link_loop";
        }
        if (CoasterCartTrainLinkConstraint.wouldExceedLinkLimit(cartA, cartB)) {
            return "simulatedcoasters.cart.link_full";
        }
        return null;
    }

    public static Set<UUID> getTrainLinkedCartIds(SubLevel hostCart) {
        ObjectOpenHashSet visited = new ObjectOpenHashSet();
        if (!CoasterCartResolve.isCartSubLevel(hostCart) || hostCart.getLevel() == null) {
            return visited;
        }
        ArrayDeque<UUID> queue = new ArrayDeque<UUID>();
        UUID hostId = hostCart.getUniqueId();
        visited.add(hostId);
        queue.add(hostId);
        while (!queue.isEmpty()) {
            UUID currentId = (UUID)queue.poll();
            SubLevel current = currentId.equals(hostId) ? hostCart : CoasterCartResolve.findCartById(hostCart.getLevel(), currentId);
            for (UUID partnerId : CoasterCartTrainLinkConstraint.partnerIdsFor(current)) {
                if (!visited.add(partnerId)) continue;
                queue.add(partnerId);
            }
        }
        return visited;
    }

    public static UUID trainSoundLeaderId(SubLevel cart) {
        Set<UUID> train = CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(cart);
        if (train.isEmpty()) {
            return cart.getUniqueId();
        }
        UUID leader = null;
        for (UUID id : train) {
            if (leader != null && id.compareTo(leader) >= 0) continue;
            leader = id;
        }
        return leader;
    }

    public static boolean isTrainSoundLeader(SubLevel cart) {
        return cart.getUniqueId().equals(CoasterCartTrainLinkConstraint.trainSoundLeaderId(cart));
    }

    private static List<UUID> partnerIdsFor(@Nullable SubLevel cart) {
        if (cart == null || cart.getLevel() == null) {
            return List.of();
        }
        if (cart instanceof ServerSubLevel) {
            ServerSubLevel server = (ServerSubLevel)cart;
            return CoasterCartTrainLinkData.readAll(server).stream().map(CoasterCartTrainLinkData.LinkRecord::partnerId).toList();
        }
        if (cart.getLevel().isClientSide()) {
            return CoasterCartTrainLinkQueries.clientPartnerIds((ResourceKey<Level>)cart.getLevel().dimension(), cart.getUniqueId());
        }
        return CoasterCartTrainLinkConstraint.activePartnerIds(cart.getUniqueId(), (ResourceKey<Level>)cart.getLevel().dimension());
    }

    private static List<UUID> activePartnerIds(UUID cartId, ResourceKey<Level> dimension) {
        ArrayList<UUID> partners = new ArrayList<UUID>(2);
        for (ActiveLink link : ACTIVE_LINKS.values()) {
            if (!link.dimension.equals(dimension)) continue;
            if (link.cartA.equals(cartId)) {
                partners.add(link.cartB);
                continue;
            }
            if (!link.cartB.equals(cartId)) continue;
            partners.add(link.cartA);
        }
        return partners;
    }

    public static void augmentLoadingDependencyChain(Collection<ServerSubLevel> chain) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ServerSubLevel current : new ArrayList<ServerSubLevel>(chain)) {
                if (!CoasterCartResolve.isCartSubLevel((SubLevel)current)) continue;
                for (CoasterCartTrainLinkData.LinkRecord record : CoasterCartTrainLinkData.readAll(current)) {
                    ServerSubLevel partner = CoasterCartResolve.findCartById(current.getLevel(), record.partnerId());
                    if (partner == null || !chain.add(partner)) continue;
                    changed = true;
                }
            }
        }
    }

    public static void onPostPhysicsTick(ForgeSablePostPhysicsTickEvent event) {
        ServerSubLevel cartB;
        ServerSubLevel cartA;
        if (ACTIVE_LINKS.isEmpty()) {
            return;
        }
        SubLevelPhysicsSystem physics = event.getPhysicsSystem();
        ServerLevel level = physics.getLevel();
        double timeStep = event.getTimeStep();
        ArrayList<ActiveLink> toBreak = new ArrayList<ActiveLink>();
        for (ActiveLink link : ACTIVE_LINKS.values()) {
            if (!link.dimension.equals((Object)level.dimension())) continue;
            cartA = CoasterCartResolve.findCartById(level, link.cartA);
            cartB = CoasterCartResolve.findCartById(level, link.cartB);
            if (cartA == null || cartB == null || cartA.isRemoved() || cartB.isRemoved() || !CoasterCartTrainLinkConstraint.isUnderExtremeTension(link, cartA, cartB, timeStep)) continue;
            toBreak.add(link);
        }
        for (ActiveLink link : toBreak) {
            cartA = CoasterCartResolve.findCartById(level, link.cartA);
            cartB = CoasterCartResolve.findCartById(level, link.cartB);
            if (cartA == null || cartB == null) continue;
            CoasterCartTrainLinkConstraint.breakLinkUnderTension(level, physics, link, cartA, cartB);
        }
    }

    private static boolean isUnderExtremeTension(ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB, double timeStep) {
        Vector3d centerA = CoasterCartResolve.cartCenterWorld((SubLevel)cartA);
        Vector3d centerB = CoasterCartResolve.cartCenterWorld((SubLevel)cartB);
        if (centerA == null || centerB == null) {
            return false;
        }
        double stretch = centerA.distance((Vector3dc)centerB) - link.centerDistance;
        if (stretch <= 0.0) {
            return false;
        }
        if (stretch >= 0.5) {
            return true;
        }
        GenericConstraintHandle handle = link.handle;
        if (handle == null || !handle.isValid() || timeStep <= 0.0) {
            return false;
        }
        Vector3d linear = new Vector3d();
        handle.getJointImpulses(linear, new Vector3d());
        double linkAxisForce = Math.abs(linear.x) / timeStep;
        return stretch >= 0.07 && linkAxisForce >= 1800.0;
    }

    private static void breakLinkUnderTension(ServerLevel level, SubLevelPhysicsSystem physics, ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB) {
        Vector3d centerA = CoasterCartResolve.cartCenterWorld((SubLevel)cartA);
        Vector3d centerB = CoasterCartResolve.cartCenterWorld((SubLevel)cartB);
        CoasterCartTrainLinkConstraint.unlinkPair(level, link.cartA, link.cartB);
        CoasterCartTrainLinkConstraint.dampVelocitiesAfterSnapBreak(physics, cartA);
        CoasterCartTrainLinkConstraint.dampVelocitiesAfterSnapBreak(physics, cartB);
        if (centerA == null || centerB == null) {
            return;
        }
        Vector3d mid = new Vector3d((Vector3dc)centerA).add((Vector3dc)centerB).mul(0.5);
        level.playSound(null, BlockPos.containing((double)mid.x, (double)mid.y, (double)mid.z), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.85f, 0.95f);
        level.sendParticles((ParticleOptions)ParticleTypes.CRIT, mid.x, mid.y, mid.z, 6, 0.12, 0.12, 0.12, 0.04);
        SimulatedCoasters.LOGGER.info("[cart-link] coupler snapped under tension {} <-> {}", (Object)link.cartA, (Object)link.cartB);
    }

    private static void dampVelocitiesAfterSnapBreak(SubLevelPhysicsSystem physics, ServerSubLevel cart) {
        PhysicsPipeline pipeline = physics.getPipeline();
        Vector3d v = pipeline.getLinearVelocity((PhysicsPipelineBody)cart, new Vector3d());
        Vector3d w = pipeline.getAngularVelocity((PhysicsPipelineBody)cart, new Vector3d());
        double scale = -0.92;
        if (v.lengthSquared() >= 1.0E-18 || w.lengthSquared() >= 1.0E-18) {
            pipeline.addLinearAndAngularVelocity((PhysicsPipelineBody)cart, (Vector3dc)new Vector3d((Vector3dc)v).mul(scale), (Vector3dc)new Vector3d((Vector3dc)w).mul(scale));
            pipeline.wakeUp((PhysicsPipelineBody)cart);
        }
    }

    private static void updateConstraint(SubLevelPhysicsSystem physics, ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB) {
        Vector3d bearingALocal = CoasterCartResolve.bearingPlotCenter((SubLevel)cartA);
        Vector3d bearingBLocal = CoasterCartResolve.bearingPlotCenter((SubLevel)cartB);
        if (bearingALocal == null || bearingBLocal == null) {
            return;
        }
        Vector3d centerA = cartA.logicalPose().transformPosition((Vector3dc)bearingALocal, new Vector3d());
        Vector3d centerB = cartB.logicalPose().transformPosition((Vector3dc)bearingBLocal, new Vector3d());
        Vector3d aToBWorld = new Vector3d((Vector3dc)centerB).sub((Vector3dc)centerA);
        if (aToBWorld.lengthSquared() < 1.0E-12) {
            aToBWorld.set(1.0, 0.0, 0.0);
        }
        aToBWorld.normalize();
        Vector3d targetWorldOnB = new Vector3d((Vector3dc)aToBWorld).mul(-link.centerDistance).add((Vector3dc)centerB);
        Vector3d anchorBLocal = cartB.logicalPose().transformPositionInverse((Vector3dc)targetWorldOnB, new Vector3d());
        Quaterniond qJointWorld = CoasterCartTrainLinkConstraint.quaternionWithPositiveXAxis(aToBWorld);
        Quaterniond qA = CoasterCartTrainLinkConstraint.localJointOrientation(cartA, qJointWorld);
        Quaterniond qB = CoasterCartTrainLinkConstraint.localJointOrientation(cartB, qJointWorld);
        GenericConstraintHandle handle = link.handle;
        if (handle == null || !handle.isValid()) {
            GenericConstraintConfiguration cfg = new GenericConstraintConfiguration((Vector3dc)new Vector3d((Vector3dc)bearingALocal), (Vector3dc)anchorBLocal, (Quaterniondc)qA, (Quaterniondc)qB, TRAIN_LINK_LOCKED_AXES);
            handle = (GenericConstraintHandle)physics.getPipeline().addConstraint((PhysicsPipelineBody)cartA, (PhysicsPipelineBody)cartB, (PhysicsConstraintConfiguration)cfg);
            if (handle == null) {
                SimulatedCoasters.LOGGER.warn("[cart-link] pipeline refused constraint for {} <-> {}", (Object)link.cartA, (Object)link.cartB);
                return;
            }
            handle.setContactsEnabled(false);
            link.handle = handle;
            physics.getPipeline().wakeUp((PhysicsPipelineBody)cartA);
            physics.getPipeline().wakeUp((PhysicsPipelineBody)cartB);
        } else {
            handle.setFrame1((Vector3dc)bearingALocal, (Quaterniondc)qA);
            handle.setFrame2((Vector3dc)anchorBLocal, (Quaterniondc)qB);
        }
    }

    private static void suspendActiveLink(ActiveLink link) {
        ActiveLink removed = ACTIVE_LINKS.remove(CoasterCartTrainLinkConstraint.pairKey(link.cartA, link.cartB));
        if (removed != null && removed.handle != null && removed.handle.isValid()) {
            removed.handle.remove();
        }
    }

    private static boolean isDefunctLinkPartner(ServerLevel level, UUID partnerId) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return false;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(partnerId);
        if (sub == null) {
            return false;
        }
        return sub.isRemoved() || !CoasterCartResolve.isCartSubLevel(sub);
    }

    private static void unlinkPair(ServerLevel level, UUID cartIdA, UUID cartIdB) {
        ActiveLink link = ACTIVE_LINKS.remove(CoasterCartTrainLinkConstraint.pairKey(cartIdA, cartIdB));
        if (link != null && link.handle != null && link.handle.isValid()) {
            link.handle.remove();
        }
        ServerSubLevel cartA = CoasterCartTrainLinkConstraint.subLevelById(level, cartIdA);
        ServerSubLevel cartB = CoasterCartTrainLinkConstraint.subLevelById(level, cartIdB);
        if (cartA != null) {
            CoasterCartTrainLinkData.removeLink(cartA, cartIdB);
        }
        if (cartB != null) {
            CoasterCartTrainLinkData.removeLink(cartB, cartIdA);
        }
        CoasterCartTrainLinkSync.broadcastCart(level, cartIdA, CoasterCartTrainLinkConstraint.liveCartForSync(cartA));
        CoasterCartTrainLinkSync.broadcastCart(level, cartIdB, CoasterCartTrainLinkConstraint.liveCartForSync(cartB));
        SimulatedCoasters.LOGGER.info("[cart-link] unlinked {} <-> {}", (Object)cartIdA, (Object)cartIdB);
    }

    @Nullable
    private static ServerSubLevel subLevelById(ServerLevel level, UUID id) {
        ServerSubLevel server;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(id);
        if (!(sub instanceof ServerSubLevel) || (server = (ServerSubLevel)sub).isRemoved()) {
            return null;
        }
        return server;
    }

    @Nullable
    private static ServerSubLevel liveCartForSync(@Nullable ServerSubLevel sub) {
        return sub != null && CoasterCartResolve.isCartSubLevel((SubLevel)sub) ? sub : null;
    }

    private static Quaterniond localJointOrientation(ServerSubLevel cart, Quaterniond qJointWorld) {
        return new Quaterniond((Quaterniondc)cart.logicalPose().orientation()).conjugate().mul((Quaterniondc)qJointWorld).normalize();
    }

    private static Quaterniond quaternionWithPositiveXAxis(Vector3d xAxisUnit) {
        Vector3d ref = Math.abs(xAxisUnit.y) < 0.99 ? new Vector3d(0.0, 1.0, 0.0) : new Vector3d(1.0, 0.0, 0.0);
        Vector3d z = new Vector3d((Vector3dc)xAxisUnit).cross((Vector3dc)ref).normalize();
        Vector3d y = new Vector3d((Vector3dc)z).cross((Vector3dc)xAxisUnit).normalize();
        Matrix3d m = new Matrix3d();
        m.setColumn(0, (Vector3dc)xAxisUnit);
        m.setColumn(1, (Vector3dc)y);
        m.setColumn(2, (Vector3dc)z);
        return new Quaterniond().setFromNormalized((Matrix3dc)m);
    }

    private static String pairKey(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? String.valueOf(a) + "|" + String.valueOf(b) : String.valueOf(b) + "|" + String.valueOf(a);
    }

    private static final class ActiveLink {
        final ResourceKey<Level> dimension;
        final UUID cartA;
        final UUID cartB;
        final double centerDistance;
        @Nullable
        GenericConstraintHandle handle;

        ActiveLink(ResourceKey<Level> dimension, UUID cartA, UUID cartB, double centerDistance) {
            this.dimension = dimension;
            this.cartA = cartA;
            this.cartB = cartB;
            this.centerDistance = centerDistance;
        }
    }
}

