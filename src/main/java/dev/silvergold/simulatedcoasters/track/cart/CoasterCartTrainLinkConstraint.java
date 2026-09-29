package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
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
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class CoasterCartTrainLinkConstraint {
   private static final Set<ConstraintJointAxis> TRAIN_LINK_LOCKED_AXES = EnumSet.of(ConstraintJointAxis.LINEAR_X);
   private static final double MIN_LINK_DISTANCE = 0.05;
   private static final double TENSION_BREAK_STRETCH_BLOCKS = 0.5;
   private static final double TENSION_BREAK_STRETCH_FOR_FORCE_BLOCKS = 0.07;
   private static final double TENSION_BREAK_FORCE = 1800.0;
   private static final double TENSION_SNAP_VELOCITY_RETAIN = 0.08;
   private static final Map<String, CoasterCartTrainLinkConstraint.ActiveLink> ACTIVE_LINKS = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, Long> LAST_LINK_RESTORE_GAME_TIME = new ConcurrentHashMap<>();

   private CoasterCartTrainLinkConstraint() {
   }

   public static void tryLinkFromItemSelection(ServerLevel level, ServerSubLevel newCart, ItemStack stack) {
      tryLinkFromItemSelection(level, newCart, stack, null);
   }

   public static void tryLinkFromItemSelection(ServerLevel level, ServerSubLevel newCart, ItemStack stack, @Nullable ServerPlayer player) {
      UUID partnerId = CoasterCartLinkSelection.readLinkTarget(stack);
      if (partnerId != null) {
         ServerSubLevel partner = CoasterCartResolve.findCartById(level, partnerId);
         if (partner != null && partner != newCart && !partner.isRemoved()) {
            pruneInvalidLinksForCoupling(level, partner);
            if (linkCarts(level, partner, newCart)) {
               CoasterCartLinkSelection.clearLinkTarget(stack);
            }
         } else {
            CoasterCartLinkSelection.clearLinkTarget(stack);
            SimulatedCoasters.LOGGER.info("[cart-link] selection {} could not be resolved to a live cart; not linking", partnerId);
         }
      }
   }

   public static boolean linkCarts(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB) {
      Vector3d centerA = CoasterCartResolve.cartCenterWorld(cartA);
      Vector3d centerB = CoasterCartResolve.cartCenterWorld(cartB);
      if (centerA != null && centerB != null) {
         double distance = Math.max(centerA.distance(centerB), 0.05);
         return linkCarts(level, cartA, cartB, distance);
      } else {
         SimulatedCoasters.LOGGER.warn("[cart-link] missing bearing cell on one of the carts; not linking");
         return false;
      }
   }

   public static boolean linkCarts(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB, double centerDistance) {
      if (cartA == cartB) {
         return false;
      } else {
         pruneInvalidLinksForCoupling(level, cartA);
         pruneInvalidLinksForCoupling(level, cartB);
         if (CoasterCartTrainLinkData.hasLink(cartA, cartB.getUniqueId())) {
            return false;
         } else if (!CoasterCartTrainLinkData.canAddLink(cartA, cartB.getUniqueId()) || !CoasterCartTrainLinkData.canAddLink(cartB, cartA.getUniqueId())) {
            SimulatedCoasters.LOGGER
               .info("[cart-link] link refused — cart {} and/or {} at {}-link limit", new Object[]{cartA.getUniqueId(), cartB.getUniqueId(), 2});
            return false;
         } else if (wouldFormLoop(cartA, cartB)) {
            SimulatedCoasters.LOGGER.info("[cart-link] link refused — carts {} and {} are already in the same train", cartA.getUniqueId(), cartB.getUniqueId());
            return false;
         } else {
            double distance = Math.max(centerDistance, 0.05);
            CoasterCartTrainLinkData.addLink(cartA, cartB.getUniqueId(), distance);
            CoasterCartTrainLinkData.addLink(cartB, cartA.getUniqueId(), distance);
            CoasterCartTrainLinkSync.broadcastCart(cartA);
            CoasterCartTrainLinkSync.broadcastCart(cartB);
            ACTIVE_LINKS.put(
               pairKey(cartA.getUniqueId(), cartB.getUniqueId()),
               new CoasterCartTrainLinkConstraint.ActiveLink(level.dimension(), cartA.getUniqueId(), cartB.getUniqueId(), distance)
            );
            SimulatedCoasters.LOGGER
               .info(
                  "[cart-link] linked carts {} <-> {} at distance {}", new Object[]{cartA.getUniqueId(), cartB.getUniqueId(), String.format("%.3f", distance)}
               );
            Vector3d centerA = CoasterCartResolve.cartCenterWorld(cartA);
            Vector3d centerB = CoasterCartResolve.cartCenterWorld(cartB);
            if (centerA != null && centerB != null) {
               Vector3d mid = new Vector3d(centerA).add(centerB).mul(0.5);
               level.playSound(null, BlockPos.containing(mid.x, mid.y, mid.z), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.15F);
            }

            return true;
         }
      }
   }

   public static boolean adjustEditModeLinkDistances(ServerLevel level, ServerSubLevel hostCart, boolean shorten) {
      List<CoasterCartLinkDistanceEdit.AdjustedLink> adjustments = CoasterCartLinkDistanceEdit.computeAdjustments(hostCart, shorten);
      if (adjustments.isEmpty()) {
         return false;
      } else {
         for (CoasterCartLinkDistanceEdit.AdjustedLink adjustment : adjustments) {
            setPairLinkDistance(level, hostCart, adjustment.partner(), adjustment.newDistance());
         }

         Vector3d center = CoasterCartResolve.cartCenterWorld(hostCart);
         if (center != null) {
            level.playSound(
               null, BlockPos.containing(center.x, center.y, center.z), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.75F, shorten ? 0.95F : 1.1F
            );
         }

         return true;
      }
   }

   private static void setPairLinkDistance(ServerLevel level, ServerSubLevel cartA, ServerSubLevel cartB, double distance) {
      UUID idA = cartA.getUniqueId();
      UUID idB = cartB.getUniqueId();
      CoasterCartTrainLinkData.setLinkDistance(cartA, idB, distance);
      CoasterCartTrainLinkData.setLinkDistance(cartB, idA, distance);
      CoasterCartTrainLinkSync.broadcastCart(cartA);
      CoasterCartTrainLinkSync.broadcastCart(cartB);
      String key = pairKey(idA, idB);
      CoasterCartTrainLinkConstraint.ActiveLink existing = ACTIVE_LINKS.remove(key);
      if (existing != null && existing.handle != null && existing.handle.isValid()) {
         existing.handle.remove();
      }

      ACTIVE_LINKS.put(key, new CoasterCartTrainLinkConstraint.ActiveLink(level.dimension(), idA, idB, distance));
      SimulatedCoasters.LOGGER.info("[cart-link] set distance {} <-> {} to {}", new Object[]{idA, idB, String.format("%.3f", distance)});
   }

   public static void tryRestoreLink(ServerSubLevel cart, ServerLevel level) {
      for (CoasterCartTrainLinkData.LinkRecord data : CoasterCartTrainLinkData.readAll(cart)) {
         String key = pairKey(cart.getUniqueId(), data.partnerId());
         if (!ACTIVE_LINKS.containsKey(key)) {
            ServerSubLevel partner = CoasterCartResolve.findCartById(level, data.partnerId());
            if (partner != null && !partner.isRemoved()) {
               ACTIVE_LINKS.put(
                  key, new CoasterCartTrainLinkConstraint.ActiveLink(level.dimension(), cart.getUniqueId(), data.partnerId(), data.centerDistance())
               );
               SimulatedCoasters.LOGGER
                  .info("[cart-link] restored link {} <-> {} (distance {})", new Object[]{cart.getUniqueId(), data.partnerId(), data.centerDistance()});
            } else if (isDefunctLinkPartner(level, data.partnerId())) {
               unlinkPair(level, cart.getUniqueId(), data.partnerId());
            }
         }
      }
   }

   public static boolean clearAllLinks(ServerLevel level, ServerSubLevel cart) {
      if (cart.isRemoved()) {
         return false;
      } else {
         List<CoasterCartTrainLinkData.LinkRecord> links = CoasterCartTrainLinkData.readAll(cart);
         if (links.isEmpty()) {
            return false;
         } else {
            UUID cartId = cart.getUniqueId();

            for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(links)) {
               unlinkPair(level, cartId, link.partnerId());
            }

            Vector3d center = CoasterCartResolve.cartCenterWorld(cart);
            if (center != null) {
               BlockPos pos = BlockPos.containing(center.x, center.y, center.z);
               level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.9F, 1.0F);
               level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.85F, 0.95F);
            }

            return true;
         }
      }
   }

   public static void onCartRemoved(ServerSubLevel cart) {
      ServerLevel level = cart.getLevel();
      UUID cartId = cart.getUniqueId();
      CoasterCartBearingTickCache.remove(cart);
      CoasterCartIdIndex.removeCart(level, cart);

      for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
         unlinkPair(level, cartId, link.partnerId());
      }

      CoasterCartTrainLinkData.clearAll(cart);
      CoasterCartTrainLinkSync.broadcastCart(level, cartId, null);
   }

   public static void pruneDefunctLinks(ServerLevel level, ServerSubLevel cart) {
      for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
         if (isDefunctLinkPartner(level, link.partnerId())) {
            unlinkPair(level, cart.getUniqueId(), link.partnerId());
         }
      }
   }

   public static void pruneInvalidLinksForCoupling(ServerLevel level, ServerSubLevel cart) {
      for (CoasterCartTrainLinkData.LinkRecord link : List.copyOf(CoasterCartTrainLinkData.readAll(cart))) {
         if (isDefunctLinkPartner(level, link.partnerId()) || CoasterCartResolve.findCartById(level, link.partnerId()) == null) {
            unlinkPair(level, cart.getUniqueId(), link.partnerId());
         }
      }
   }

   public static void tick(SubLevelPhysicsSystem physics, ServerLevel level) {
      maybeRestorePersistedLinks(level);
      if (!ACTIVE_LINKS.isEmpty()) {
         for (CoasterCartTrainLinkConstraint.ActiveLink link : List.copyOf(ACTIVE_LINKS.values())) {
            if (link.dimension.equals(level.dimension())) {
               ServerSubLevel cartA = CoasterCartResolve.findCartById(level, link.cartA);
               ServerSubLevel cartB = CoasterCartResolve.findCartById(level, link.cartB);
               if (cartA != null && cartB != null) {
                  if (!cartA.isRemoved() && !cartB.isRemoved()) {
                     updateConstraint(physics, link, cartA, cartB);
                  } else {
                     unlinkPair(level, link.cartA, link.cartB);
                  }
               } else if (!isDefunctLinkPartner(level, link.cartA) && !isDefunctLinkPartner(level, link.cartB)) {
                  suspendActiveLink(link);
               } else {
                  unlinkPair(level, link.cartA, link.cartB);
               }
            }
         }
      }
   }

   public static void restorePersistedLinks(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (CoasterCartResolve.isCartSubLevel(sub)) {
               pruneDefunctLinks(level, sub);
               tryRestoreLink(sub, level);
            }
         }
      }
   }

   private static void maybeRestorePersistedLinks(ServerLevel level) {
      long gameTime = level.getGameTime();
      ResourceKey<Level> dimension = level.dimension();
      Long last = LAST_LINK_RESTORE_GAME_TIME.get(dimension);
      if (last == null || (gameTime - last >= 60L || gameTime < last)) {
         LAST_LINK_RESTORE_GAME_TIME.put(dimension, gameTime);
         restorePersistedLinks(level);
      }
   }

   public static void clearDimension(ResourceKey<Level> dimension) {
      ACTIVE_LINKS.entrySet().removeIf(e -> e.getValue().dimension.equals(dimension));
      LAST_LINK_RESTORE_GAME_TIME.remove(dimension);
   }

   public static boolean areLinkedCarts(SubLevel a, SubLevel b) {
      if (a == null || b == null || a == b) {
         return a == b;
      } else if (CoasterCartResolve.isCartSubLevel(a) && CoasterCartResolve.isCartSubLevel(b)) {
         UUID aId = a.getUniqueId();
         UUID bId = b.getUniqueId();
         if (a instanceof ServerSubLevel serverA && CoasterCartTrainLinkData.hasLink(serverA, bId)) {
            return true;
         }

         if (b instanceof ServerSubLevel serverB && CoasterCartTrainLinkData.hasLink(serverB, aId)) {
            return true;
         }

         if (a.getLevel() == null) {
            return false;
         } else {
            return a.getLevel().isClientSide()
               ? CoasterCartTrainLinkQueries.clientHasLinkBetween(a.getLevel().dimension(), aId, bId)
               : activeLinkBetween(aId, bId, a.getLevel().dimension());
         }
      } else {
         return false;
      }
   }

   private static boolean activeLinkBetween(UUID aId, UUID bId, ResourceKey<Level> dimension) {
      for (CoasterCartTrainLinkConstraint.ActiveLink link : ACTIVE_LINKS.values()) {
         if (link.dimension.equals(dimension) && (link.cartA.equals(aId) && link.cartB.equals(bId) || link.cartA.equals(bId) && link.cartB.equals(aId))) {
            return true;
         }
      }

      return false;
   }

   public static boolean areAlreadyLinked(SubLevel cartA, SubLevel cartB) {
      if (cartA != null && cartB != null && cartA != cartB) {
         UUID aId = cartA.getUniqueId();
         UUID bId = cartB.getUniqueId();
         return partnerIdsFor(cartA).contains(bId) || partnerIdsFor(cartB).contains(aId);
      } else {
         return false;
      }
   }

   public static boolean wouldFormLoop(SubLevel cartA, SubLevel cartB) {
      if (cartA != null && cartB != null && cartA != cartB) {
         UUID aId = cartA.getUniqueId();
         UUID bId = cartB.getUniqueId();
         return areAlreadyLinked(cartA, cartB) ? false : getTrainLinkedCartIds(cartA).contains(bId);
      } else {
         return false;
      }
   }

   public static boolean wouldExceedLinkLimit(SubLevel cartA, SubLevel cartB) {
      if (cartA == null || cartB == null || cartA == cartB) {
         return false;
      } else {
         return areAlreadyLinked(cartA, cartB) ? false : livePartnerCount(cartA) >= 2 || livePartnerCount(cartB) >= 2;
      }
   }

   public static boolean isAtLinkLimit(SubLevel cart) {
      return cart != null && livePartnerCount(cart) >= 2;
   }

   private static int livePartnerCount(SubLevel cart) {
      if (cart.getLevel() == null) {
         return 0;
      } else {
         int count = 0;

         for (UUID partnerId : partnerIdsFor(cart)) {
            SubLevel partner = CoasterCartResolve.findCartById(cart.getLevel(), partnerId);
            if (partner != null && !partner.isRemoved()) {
               count++;
            }
         }

         return count;
      }
   }

   @Nullable
   public static String coupleRejectLangKey(SubLevel cartA, SubLevel cartB) {
      if (areAlreadyLinked(cartA, cartB)) {
         return "simulatedcoasters.cart.link_already";
      } else if (wouldFormLoop(cartA, cartB)) {
         return "simulatedcoasters.cart.link_loop";
      } else {
         return wouldExceedLinkLimit(cartA, cartB) ? "simulatedcoasters.cart.link_full" : null;
      }
   }

   public static Set<UUID> getTrainLinkedCartIds(SubLevel hostCart) {
      Set<UUID> visited = new ObjectOpenHashSet();
      if (CoasterCartResolve.isCartSubLevel(hostCart) && hostCart.getLevel() != null) {
         ArrayDeque<UUID> queue = new ArrayDeque<>();
         UUID hostId = hostCart.getUniqueId();
         visited.add(hostId);
         queue.add(hostId);

         while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            SubLevel current = currentId.equals(hostId) ? hostCart : CoasterCartResolve.findCartById(hostCart.getLevel(), currentId);

            for (UUID partnerId : partnerIdsFor(current)) {
               if (visited.add(partnerId)) {
                  queue.add(partnerId);
               }
            }
         }

         return visited;
      } else {
         return visited;
      }
   }

   public static UUID trainSoundLeaderId(SubLevel cart) {
      Set<UUID> train = getTrainLinkedCartIds(cart);
      if (train.isEmpty()) {
         return cart.getUniqueId();
      } else {
         UUID leader = null;

         for (UUID id : train) {
            if (leader == null || id.compareTo(leader) < 0) {
               leader = id;
            }
         }

         return leader;
      }
   }

   public static boolean isTrainSoundLeader(SubLevel cart) {
      return cart.getUniqueId().equals(trainSoundLeaderId(cart));
   }

   private static List<UUID> partnerIdsFor(@Nullable SubLevel cart) {
      if (cart == null || cart.getLevel() == null) {
         return List.of();
      } else if (cart instanceof ServerSubLevel server) {
         return CoasterCartTrainLinkData.readAll(server).stream().map(CoasterCartTrainLinkData.LinkRecord::partnerId).toList();
      } else {
         return cart.getLevel().isClientSide()
            ? CoasterCartTrainLinkQueries.clientPartnerIds(cart.getLevel().dimension(), cart.getUniqueId())
            : activePartnerIds(cart.getUniqueId(), cart.getLevel().dimension());
      }
   }

   private static List<UUID> activePartnerIds(UUID cartId, ResourceKey<Level> dimension) {
      List<UUID> partners = new ArrayList<>(2);

      for (CoasterCartTrainLinkConstraint.ActiveLink link : ACTIVE_LINKS.values()) {
         if (link.dimension.equals(dimension)) {
            if (link.cartA.equals(cartId)) {
               partners.add(link.cartB);
            } else if (link.cartB.equals(cartId)) {
               partners.add(link.cartA);
            }
         }
      }

      return partners;
   }

   public static void augmentLoadingDependencyChain(Collection<ServerSubLevel> chain) {
      boolean changed = true;

      while (changed) {
         changed = false;

         for (ServerSubLevel current : new ArrayList<>(chain)) {
            if (CoasterCartResolve.isCartSubLevel(current)) {
               for (CoasterCartTrainLinkData.LinkRecord record : CoasterCartTrainLinkData.readAll(current)) {
                  ServerSubLevel partner = CoasterCartResolve.findCartById(current.getLevel(), record.partnerId());
                  if (partner != null && chain.add(partner)) {
                     changed = true;
                  }
               }
            }
         }
      }
   }

   public static void onPostPhysicsTick(ForgeSablePostPhysicsTickEvent event) {
      if (!ACTIVE_LINKS.isEmpty()) {
         SubLevelPhysicsSystem physics = event.getPhysicsSystem();
         ServerLevel level = physics.getLevel();
         double timeStep = event.getTimeStep();
         List<CoasterCartTrainLinkConstraint.ActiveLink> toBreak = new ArrayList<>();

         for (CoasterCartTrainLinkConstraint.ActiveLink link : ACTIVE_LINKS.values()) {
            if (link.dimension.equals(level.dimension())) {
               ServerSubLevel cartA = CoasterCartResolve.findCartById(level, link.cartA);
               ServerSubLevel cartB = CoasterCartResolve.findCartById(level, link.cartB);
               if (cartA != null && cartB != null && !cartA.isRemoved() && !cartB.isRemoved() && isUnderExtremeTension(link, cartA, cartB, timeStep)) {
                  toBreak.add(link);
               }
            }
         }

         for (CoasterCartTrainLinkConstraint.ActiveLink linkx : toBreak) {
            ServerSubLevel cartA = CoasterCartResolve.findCartById(level, linkx.cartA);
            ServerSubLevel cartB = CoasterCartResolve.findCartById(level, linkx.cartB);
            if (cartA != null && cartB != null) {
               breakLinkUnderTension(level, physics, linkx, cartA, cartB);
            }
         }
      }
   }

   private static boolean isUnderExtremeTension(CoasterCartTrainLinkConstraint.ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB, double timeStep) {
      Vector3d centerA = CoasterCartResolve.cartCenterWorld(cartA);
      Vector3d centerB = CoasterCartResolve.cartCenterWorld(cartB);
      if (centerA != null && centerB != null) {
         double stretch = centerA.distance(centerB) - link.centerDistance;
         if (stretch <= 0.0) {
            return false;
         } else if (stretch >= 0.5) {
            return true;
         } else {
            GenericConstraintHandle handle = link.handle;
            if (handle != null && handle.isValid() && !(timeStep <= 0.0)) {
               Vector3d linear = new Vector3d();
               handle.getJointImpulses(linear, new Vector3d());
               double linkAxisForce = Math.abs(linear.x) / timeStep;
               return stretch >= 0.07 && linkAxisForce >= 1800.0;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static void breakLinkUnderTension(
      ServerLevel level, SubLevelPhysicsSystem physics, CoasterCartTrainLinkConstraint.ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB
   ) {
      Vector3d centerA = CoasterCartResolve.cartCenterWorld(cartA);
      Vector3d centerB = CoasterCartResolve.cartCenterWorld(cartB);
      unlinkPair(level, link.cartA, link.cartB);
      dampVelocitiesAfterSnapBreak(physics, cartA);
      dampVelocitiesAfterSnapBreak(physics, cartB);
      if (centerA != null && centerB != null) {
         Vector3d mid = new Vector3d(centerA).add(centerB).mul(0.5);
         level.playSound(null, BlockPos.containing(mid.x, mid.y, mid.z), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.85F, 0.95F);
         level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 6, 0.12, 0.12, 0.12, 0.04);
         SimulatedCoasters.LOGGER.info("[cart-link] coupler snapped under tension {} <-> {}", link.cartA, link.cartB);
      }
   }

   private static void dampVelocitiesAfterSnapBreak(SubLevelPhysicsSystem physics, ServerSubLevel cart) {
      PhysicsPipeline pipeline = physics.getPipeline();
      Vector3d v = pipeline.getLinearVelocity(cart, new Vector3d());
      Vector3d w = pipeline.getAngularVelocity(cart, new Vector3d());
      double scale = -0.92;
      if (v.lengthSquared() >= 1.0E-18 || w.lengthSquared() >= 1.0E-18) {
         pipeline.addLinearAndAngularVelocity(cart, new Vector3d(v).mul(scale), new Vector3d(w).mul(scale));
         pipeline.wakeUp(cart);
      }
   }

   private static void updateConstraint(
      SubLevelPhysicsSystem physics, CoasterCartTrainLinkConstraint.ActiveLink link, ServerSubLevel cartA, ServerSubLevel cartB
   ) {
      Vector3d bearingALocal = CoasterCartResolve.bearingPlotCenter(cartA);
      Vector3d bearingBLocal = CoasterCartResolve.bearingPlotCenter(cartB);
      if (bearingALocal != null && bearingBLocal != null) {
         Vector3d centerA = cartA.logicalPose().transformPosition(bearingALocal, new Vector3d());
         Vector3d centerB = cartB.logicalPose().transformPosition(bearingBLocal, new Vector3d());
         Vector3d aToBWorld = new Vector3d(centerB).sub(centerA);
         if (aToBWorld.lengthSquared() < 1.0E-12) {
            aToBWorld.set(1.0, 0.0, 0.0);
         }

         aToBWorld.normalize();
         Vector3d targetWorldOnB = new Vector3d(aToBWorld).mul(-link.centerDistance).add(centerB);
         Vector3d anchorBLocal = cartB.logicalPose().transformPositionInverse(targetWorldOnB, new Vector3d());
         Quaterniond qJointWorld = quaternionWithPositiveXAxis(aToBWorld);
         Quaterniond qA = localJointOrientation(cartA, qJointWorld);
         Quaterniond qB = localJointOrientation(cartB, qJointWorld);
         GenericConstraintHandle handle = link.handle;
         if (handle != null && handle.isValid()) {
            handle.setFrame1(bearingALocal, qA);
            handle.setFrame2(anchorBLocal, qB);
         } else {
            GenericConstraintConfiguration cfg = new GenericConstraintConfiguration(new Vector3d(bearingALocal), anchorBLocal, qA, qB, TRAIN_LINK_LOCKED_AXES);
            handle = (GenericConstraintHandle)physics.getPipeline().addConstraint(cartA, cartB, cfg);
            if (handle == null) {
               SimulatedCoasters.LOGGER.warn("[cart-link] pipeline refused constraint for {} <-> {}", link.cartA, link.cartB);
               return;
            }

            handle.setContactsEnabled(false);
            link.handle = handle;
            physics.getPipeline().wakeUp(cartA);
            physics.getPipeline().wakeUp(cartB);
         }
      }
   }

   private static void suspendActiveLink(CoasterCartTrainLinkConstraint.ActiveLink link) {
      CoasterCartTrainLinkConstraint.ActiveLink removed = ACTIVE_LINKS.remove(pairKey(link.cartA, link.cartB));
      if (removed != null && removed.handle != null && removed.handle.isValid()) {
         removed.handle.remove();
      }
   }

   private static boolean isDefunctLinkPartner(ServerLevel level, UUID partnerId) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         SubLevel sub = container.getSubLevel(partnerId);
         return sub == null ? false : sub.isRemoved() || !CoasterCartResolve.isCartSubLevel(sub);
      } else {
         return false;
      }
   }

   private static void unlinkPair(ServerLevel level, UUID cartIdA, UUID cartIdB) {
      CoasterCartTrainLinkConstraint.ActiveLink link = ACTIVE_LINKS.remove(pairKey(cartIdA, cartIdB));
      if (link != null && link.handle != null && link.handle.isValid()) {
         link.handle.remove();
      }

      ServerSubLevel cartA = subLevelById(level, cartIdA);
      ServerSubLevel cartB = subLevelById(level, cartIdB);
      if (cartA != null) {
         CoasterCartTrainLinkData.removeLink(cartA, cartIdB);
      }

      if (cartB != null) {
         CoasterCartTrainLinkData.removeLink(cartB, cartIdA);
      }

      CoasterCartTrainLinkSync.broadcastCart(level, cartIdA, liveCartForSync(cartA));
      CoasterCartTrainLinkSync.broadcastCart(level, cartIdB, liveCartForSync(cartB));
      SimulatedCoasters.LOGGER.info("[cart-link] unlinked {} <-> {}", cartIdA, cartIdB);
   }

   @Nullable
   private static ServerSubLevel subLevelById(ServerLevel level, UUID id) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         if (container.getSubLevel(id) instanceof ServerSubLevel server && !server.isRemoved()) {
            return server;
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   private static ServerSubLevel liveCartForSync(@Nullable ServerSubLevel sub) {
      return sub != null && CoasterCartResolve.isCartSubLevel(sub) ? sub : null;
   }

   private static Quaterniond localJointOrientation(ServerSubLevel cart, Quaterniond qJointWorld) {
      return new Quaterniond(cart.logicalPose().orientation()).conjugate().mul(qJointWorld).normalize();
   }

   private static Quaterniond quaternionWithPositiveXAxis(Vector3d xAxisUnit) {
      Vector3d ref = Math.abs(xAxisUnit.y) < 0.99 ? new Vector3d(0.0, 1.0, 0.0) : new Vector3d(1.0, 0.0, 0.0);
      Vector3d z = new Vector3d(xAxisUnit).cross(ref).normalize();
      Vector3d y = new Vector3d(z).cross(xAxisUnit).normalize();
      Matrix3d m = new Matrix3d();
      m.setColumn(0, xAxisUnit);
      m.setColumn(1, y);
      m.setColumn(2, z);
      return new Quaterniond().setFromNormalized(m);
   }

   private static String pairKey(UUID a, UUID b) {
      return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
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
