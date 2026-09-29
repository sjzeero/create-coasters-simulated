package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.BezierHandleEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierLiftEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierOpenEndExtensionEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierTiltEditPayload;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleDragManager {
   public static final double HANDLE_EXTRA_REACH_BLOCKS = 22.0;
   private static final double MIN_VIEW_FORWARD = 0.3;
   private static final double MAX_VIEW_FORWARD = 24.0;
   private static final double DEFAULT_VIEW_FORWARD = 3.0;
   private static double handleDragTickSoundAccum;
   @Nullable
   private static Vec3 lastHandleDragSoundWorldPos;
   private static double lastHandleDragSoundScalar = Double.NaN;
   private static final double HANDLE_DRAG_SOUND_BLOCKS_PER_TICK = 0.08;
   private static final double HANDLE_DRAG_SOUND_MIN_DELTA_BLOCKS = 0.002;
   private static boolean dragging;
   private static boolean draggingTilt;
   private static boolean draggingLift;
   private static boolean draggingOpenEndpoint;
   private static boolean useKeyWasDown;
   private static BezierHandleDragManager.HandlePick hoverPick;
   private static BezierHandleDragManager.TiltHover hoverTilt;
   private static BezierHandleDragManager.LiftHover hoverLift;
   private static BezierHandleDragManager.OpenEndpointHover hoverOpenEndpoint;
   private static BlockPos dragPrimaryHost;
   private static BlockPos dragRemoteEnd;
   private static int dragEndpointIndex;
   private static BlockPos dragEndpointPos;
   @Nullable
   private static String dragActiveEdgeKey;
   private static BlockPos dragTiltAnchorPos;
   private static BlockPos dragLiftAnchorPos;
   private static BlockPos dragOpenEndpointAnchorPos;
   private static float openEndpointPreviewExtensionBlocks = Float.NaN;
   private static double dragViewForwardDistance;
   private static double lastCursorX;
   private static double lastCursorY;
   private static CoasterBezierHandleEdit.PreviewResult preview;
   private static CoasterBezierHandleEdit.PreviewResult tiltPreview;
   private static CoasterBezierHandleEdit.PreviewResult liftPreview;
   private static boolean suppressHandleGrabUntilUseRelease;

   private BezierHandleDragManager() {
   }

   static void onEditModeActivated() {
      suppressHandleGrabUntilUseRelease = true;
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null || mc.level == null || mc.screen != null) {
         cancelInner(mc, true);
      } else if (!BezierHandleEditMode.isActive()) {
         cancelInner(mc, true);
      } else if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
         cancelInner(mc, true);
      } else {
         long window = mc.getWindow().getWindow();
         boolean useDown = mc.options.keyUse.isDown();
         double[] ax = new double[1];
         double[] ay = new double[1];
         GLFW.glfwGetCursorPos(window, ax, ay);
         double cx = ax[0];
         double cy = ay[0];
         if (suppressHandleGrabUntilUseRelease && !useDown) {
            suppressHandleGrabUntilUseRelease = false;
         }

         if (draggingOpenEndpoint) {
            lastCursorX = cx;
            lastCursorY = cy;
            updateOpenEndpointPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
               releaseCommit(mc);
            }

            useKeyWasDown = useDown;
         } else if (draggingTilt) {
            lastCursorX = cx;
            lastCursorY = cy;
            updateTiltPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
               releaseCommit(mc);
            }

            useKeyWasDown = useDown;
         } else if (draggingLift) {
            lastCursorX = cx;
            lastCursorY = cy;
            updateLiftPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
               releaseCommit(mc);
            }

            useKeyWasDown = useDown;
         } else if (dragging) {
            lastCursorX = cx;
            lastCursorY = cy;
            updatePreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
               releaseCommit(mc);
            }

            useKeyWasDown = useDown;
         } else {
            resolveHover(mc);
            if (useDown && !useKeyWasDown && !suppressHandleGrabUntilUseRelease) {
               tryBeginDrag(mc, cx, cy);
            }

            useKeyWasDown = useDown;
            lastCursorX = cx;
            lastCursorY = cy;
         }
      }
   }

   private static void resolveHover(Minecraft mc) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 rd = cameraLookDir(mc.gameRenderer.getMainCamera());
         BezierHandleDragManager.HandlePick hp = rayPickClosestTangentHandle(mc);
         BezierHandleDragManager.TiltHover th = rayPickClosestTiltHandle(mc);
         BezierHandleDragManager.LiftHover lh = rayPickClosestLiftHandle(mc);
         BezierHandleDragManager.OpenEndpointHover oe = rayPickOpenEndpointHandle(mc);
         Double tHp = hp != null ? raySphere(ro, rd, hp.handleTipWorld(), pickRadiusWorld(ro, hp.handleTipWorld())) : null;
         Double tTh = th != null ? raySphere(ro, rd, th.tipWorld(), pickRadiusWorld(ro, th.tipWorld())) : null;
         Double tLh = lh != null ? raySphere(ro, rd, lh.tipWorld(), pickRadiusWorld(ro, lh.tipWorld())) : null;
         Double tOe = oe != null ? raySphere(ro, rd, oe.tipWorld(), pickRadiusWorld(ro, oe.tipWorld())) : null;
         double best = Double.POSITIVE_INFINITY;
         int winner = -1;
         if (tHp != null && tHp >= 0.0 && tHp < best) {
            best = tHp;
            winner = 0;
         }

         if (tTh != null && tTh >= 0.0 && tTh < best) {
            best = tTh;
            winner = 1;
         }

         if (tLh != null && tLh >= 0.0 && tLh < best) {
            best = tLh;
            winner = 2;
         }

         if (tOe != null && tOe >= 0.0 && tOe < best) {
            best = tOe;
            winner = 3;
         }

         hoverPick = winner == 0 ? hp : null;
         hoverTilt = winner == 1 ? th : null;
         hoverLift = winner == 2 ? lh : null;
         hoverOpenEndpoint = winner == 3 ? oe : null;
      } else {
         hoverPick = null;
         hoverTilt = null;
         hoverLift = null;
         hoverOpenEndpoint = null;
      }
   }

   private static Vec3 cameraLookDir(Camera cam) {
      Vector3f lv = cam.getLookVector();
      return new Vec3((double)lv.x(), (double)lv.y(), (double)lv.z()).normalize();
   }

   private static double pickRadiusWorld(Vec3 eye, Vec3 tip) {
      double d = eye.distanceTo(tip);
      return Mth.clamp(Math.max(0.16, d * 0.052), 0.16, 2.25);
   }

   public static BezierHandleDragManager.HandlePick rayPickClosestHandle(Minecraft mc) {
      return rayPickClosestTangentHandle(mc);
   }

   public static BezierHandleDragManager.TiltHover getHoverTilt() {
      return hoverTilt;
   }

   public static BezierHandleDragManager.LiftHover getHoverLift() {
      return hoverLift;
   }

   @Nullable
   public static BezierHandleDragManager.LiftHover rayPickClosestLiftHandle(Minecraft mc) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         if (editAnchor == null) {
            return null;
         } else if (!CoasterBezierHandleEdit.anchorEligibleForLiftEdit(level, editAnchor)) {
            return null;
         } else if (level.getBlockEntity(editAnchor) instanceof CoasterAnchorpointBlockEntity be) {
            float var14 = AnimationTickHolder.getPartialTicks(level);
            Vec3 ro = mc.player.getEyePosition(var14);
            Vec3 rd = cameraLookDir(mc.gameRenderer.getMainCamera());
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
            if (incident.isEmpty()) {
               return null;
            } else {
               Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), editAnchor);
               if (t == null) {
                  return null;
               } else {
                  Vec3 center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(
                     level, editAnchor, CoasterAnchorpointBlockEntity.worldCenter(level, editAnchor)
                  );
                  Vec3 face = CoasterAnchorClientSpace.toRenderDirection(level, editAnchor, CoasterTrackPlacement.anchorFaceNormalForAxis(level, editAnchor, t));
                  Vec3 tip = center.add(face.scale(1.4F));
                  double tRay = raySphere(ro, rd, tip, pickRadiusWorld(ro, tip));
                  return tRay < 0.0 ? null : new BezierHandleDragManager.LiftHover(editAnchor.immutable(), tip);
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static BezierHandleDragManager.TiltHover rayPickClosestTiltHandle(Minecraft mc) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         if (editAnchor == null) {
            return null;
         } else if (!CoasterBezierHandleEdit.anchorEligibleForTiltEdit(level, editAnchor)) {
            return null;
         } else if (level.getBlockEntity(editAnchor) instanceof CoasterAnchorpointBlockEntity be) {
            float var14 = AnimationTickHolder.getPartialTicks(level);
            Vec3 ro = mc.player.getEyePosition(var14);
            Vec3 rd = cameraLookDir(mc.gameRenderer.getMainCamera());
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
            if (incident.isEmpty()) {
               return null;
            } else {
               Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), editAnchor);
               if (t == null) {
                  return null;
               } else {
                  Vec3 center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(
                     level, editAnchor, CoasterAnchorpointBlockEntity.worldCenter(level, editAnchor)
                  );
                  Vec3 face = CoasterAnchorClientSpace.toRenderDirection(level, editAnchor, CoasterTrackPlacement.anchorFaceNormalForAxis(level, editAnchor, t));
                  Vec3 tip = center.add(face.scale(0.75));
                  double tRay = raySphere(ro, rd, tip, pickRadiusWorld(ro, tip));
                  return tRay < 0.0 ? null : new BezierHandleDragManager.TiltHover(editAnchor.immutable(), tip);
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static BezierHandleDragManager.HandlePick rayPickClosestTangentHandle(Minecraft mc) {
      Camera cam = mc.gameRenderer.getMainCamera();
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 rd = cameraLookDir(cam);
         double bestT = Double.MAX_VALUE;
         BlockPos bestHost = null;
         BlockPos bestRemote = null;
         int bestEp = -1;
         Vec3 bestTip = null;
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         if (editAnchor == null) {
            return null;
         } else {
            for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
               if (anchor.getLevel() == level && !anchor.isRemoved()) {
                  BlockPos here = anchor.getBlockPos();
                  if (editAnchor.equals(here)) {
                     for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                        if (bcRaw != null) {
                           BezierConnection bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                           if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
                              Vec3 e1 = (Vec3)bc.starts.getFirst();
                              Vec3 e2 = (Vec3)bc.starts.getSecond();
                              Vec3 f1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
                              Vec3 f2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
                              BlockPos storageHost = (BlockPos)bc.bePositions.getFirst();
                              BlockPos remote = bc.getKey();
                              Vec3 f1Hit = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, here, f1);
                              Vec3 f2Hit = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, here, f2);
                              double tFirst = raySphere(ro, rd, f1Hit, pickRadiusWorld(ro, f1Hit));
                              if (tFirst >= 0.0 && tFirst < bestT) {
                                 bestT = tFirst;
                                 bestHost = storageHost;
                                 bestRemote = remote;
                                 bestEp = 0;
                                 bestTip = f1Hit;
                              }

                              double tSecond = raySphere(ro, rd, f2Hit, pickRadiusWorld(ro, f2Hit));
                              if (tSecond >= 0.0 && tSecond < bestT) {
                                 bestT = tSecond;
                                 bestHost = storageHost;
                                 bestRemote = remote;
                                 bestEp = 1;
                                 bestTip = f2Hit;
                              }
                           }
                        }
                     }
                  }
               }
            }

            return bestHost != null && bestEp >= 0 && bestTip != null
               ? new BezierHandleDragManager.HandlePick(bestHost.immutable(), bestRemote.immutable(), bestEp, bestTip)
               : null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static BezierHandleDragManager.HandlePick maybeSwitchEditAnchorForNeighborTangentHandleThenRefreshPick(
      Minecraft mc, Level level, @Nullable BezierHandleDragManager.HandlePick hp
   ) {
      if (hp != null && BezierHandleEditMode.isActive()) {
         BlockPos active = BezierHandleEditMode.getActiveAnchor();
         if (active == null) {
            return hp;
         } else {
            BezierConnection bc = loadPrimary(level, hp.primaryHost(), hp.remoteEnd());
            if (bc == null) {
               return hp;
            } else {
               int epi = hp.endpointIndex();
               BlockPos ep = epi == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
               if (active.equals(ep)) {
                  return hp;
               } else {
                  BezierHandleEditMode.switchActiveAnchorTo(ep, level);
                  BezierHandleDragManager.HandlePick hp2 = rayPickClosestTangentHandle(mc);
                  return hp2 != null ? hp2 : hp;
               }
            }
         }
      } else {
         return hp;
      }
   }

   private static void tryBeginDrag(Minecraft mc, double cx, double cy) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 rd = cameraLookDir(mc.gameRenderer.getMainCamera());
         BezierHandleDragManager.HandlePick hp = rayPickClosestTangentHandle(mc);
         hp = maybeSwitchEditAnchorForNeighborTangentHandleThenRefreshPick(mc, level, hp);
         BezierHandleDragManager.TiltHover th = rayPickClosestTiltHandle(mc);
         BezierHandleDragManager.LiftHover lh = rayPickClosestLiftHandle(mc);
         BezierHandleDragManager.OpenEndpointHover oe = rayPickOpenEndpointHandle(mc);
         Double tHp = hp != null ? raySphere(ro, rd, hp.handleTipWorld(), pickRadiusWorld(ro, hp.handleTipWorld())) : null;
         Double tTh = th != null ? raySphere(ro, rd, th.tipWorld(), pickRadiusWorld(ro, th.tipWorld())) : null;
         Double tLh = lh != null ? raySphere(ro, rd, lh.tipWorld(), pickRadiusWorld(ro, lh.tipWorld())) : null;
         Double tOe = oe != null ? raySphere(ro, rd, oe.tipWorld(), pickRadiusWorld(ro, oe.tipWorld())) : null;
         double best = Double.POSITIVE_INFINITY;
         int winner = -1;
         if (tHp != null && tHp >= 0.0 && tHp < best) {
            best = tHp;
            winner = 0;
         }

         if (tTh != null && tTh >= 0.0 && tTh < best) {
            best = tTh;
            winner = 1;
         }

         if (tLh != null && tLh >= 0.0 && tLh < best) {
            best = tLh;
            winner = 2;
         }

         if (tOe != null && tOe >= 0.0 && tOe < best) {
            best = tOe;
            winner = 3;
         }

         if (winner == 1 && th != null) {
            tryBeginTiltDrag(mc, th);
         } else if (winner == 2 && lh != null) {
            tryBeginLiftDrag(mc, lh);
         } else if (winner == 3 && oe != null) {
            tryBeginOpenEndpointDrag(mc, oe);
         } else if (hp != null) {
            BlockPos bestHost = hp.primaryHost();
            BlockPos bestRemote = hp.remoteEnd();
            int bestEp = hp.endpointIndex();
            BezierConnection bc = loadPrimary(level, bestHost, bestRemote);
            if (bc != null) {
               BlockPos ep = bestEp == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
               Vec3 fwd = mc.player.getViewVector(pt);
               if (fwd.lengthSqr() < 1.0E-12) {
                  fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
               } else {
                  fwd = fwd.normalize();
               }

               Vec3 handleTip = hp.handleTipWorld();
               double along = handleTip.subtract(ro).dot(fwd);
               if (along < 0.3) {
                  along = 3.0;
               }

               dragViewForwardDistance = Mth.clamp(along, 0.3, 24.0);
               dragging = true;
               hoverPick = null;
               hoverTilt = null;
               dragPrimaryHost = bestHost.immutable();
               dragRemoteEnd = bestRemote.immutable();
               dragEndpointIndex = bestEp;
               dragEndpointPos = ep.immutable();
               dragActiveEdgeKey = edgeKey(bc);
               resetHandleDragSoundSample();
               updatePreviewFromVirtualTarget(mc);
               lastCursorX = cx;
               lastCursorY = cy;
            }
         }
      }
   }

   private static void tryBeginTiltDrag(Minecraft mc, BezierHandleDragManager.TiltHover th) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         double along = th.tipWorld().subtract(ro).dot(fwd);
         if (along < 0.3) {
            along = 3.0;
         }

         dragViewForwardDistance = Mth.clamp(along, 0.3, 24.0);
         draggingTilt = true;
         hoverPick = null;
         hoverTilt = null;
         hoverLift = null;
         dragTiltAnchorPos = th.anchorPos().immutable();
         resetHandleDragSoundSample();
         updateTiltPreviewFromVirtualTarget(mc);
      }
   }

   private static void tryBeginLiftDrag(Minecraft mc, BezierHandleDragManager.LiftHover lh) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         double along = lh.tipWorld().subtract(ro).dot(fwd);
         if (along < 0.3) {
            along = 3.0;
         }

         dragViewForwardDistance = Mth.clamp(along, 0.3, 24.0);
         draggingLift = true;
         hoverPick = null;
         hoverTilt = null;
         hoverLift = null;
         hoverOpenEndpoint = null;
         dragLiftAnchorPos = lh.anchorPos().immutable();
         resetHandleDragSoundSample();
         updateLiftPreviewFromVirtualTarget(mc);
      }
   }

   private static void tryBeginOpenEndpointDrag(Minecraft mc, BezierHandleDragManager.OpenEndpointHover oe) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 ro = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         double along = oe.tipWorld().subtract(ro).dot(fwd);
         if (along < 0.3) {
            along = 3.0;
         }

         dragViewForwardDistance = Mth.clamp(along, 0.3, 24.0);
         draggingOpenEndpoint = true;
         hoverPick = null;
         hoverTilt = null;
         hoverLift = null;
         hoverOpenEndpoint = null;
         dragOpenEndpointAnchorPos = oe.anchorPos().immutable();
         openEndpointPreviewExtensionBlocks = Float.NaN;
         resetHandleDragSoundSample();
         updateOpenEndpointPreviewFromVirtualTarget(mc);
      }
   }

   @Nullable
   private static BezierHandleDragManager.OpenEndpointHover rayPickOpenEndpointHandle(Minecraft mc) {
      BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
      Level level = mc.level;
      if (editAnchor != null && level != null) {
         if (level.getBlockEntity(editAnchor) instanceof CoasterAnchorpointBlockEntity be) {
            if (be.legCount() != 1) {
               return null;
            } else {
               BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, editAnchor);
               if (bc == null) {
                  return null;
               } else {
                  String ek = edgeKey(bc);
                  BezierConnection draw = previewCurveOrWorld(ek, bc);
                  float pt = AnimationTickHolder.getPartialTicks(level);
                  Vec3 handle = openEndpointHandleRenderWorld(level, editAnchor, draw, pt);
                  return handle == null ? null : new BezierHandleDragManager.OpenEndpointHover(editAnchor.immutable(), handle);
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   static Vec3 openEndpointHandleRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
      return openEndpointTipRenderWorld(level, anchorPos, bc, partialTick);
   }

   @Nullable
   static Vec3 openEndpointHandleRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
      return openEndpointHandleRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks(level));
   }

   @Nullable
   static Vec3 openEndpointJunctionRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
      Vec3 junctionPlot = CoasterOpenEndExtension.junctionPlotLocal(bc, anchorPos);
      return CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, anchorPos, junctionPlot, partialTick);
   }

   @Nullable
   static Vec3 openEndpointJunctionRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
      return openEndpointJunctionRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks(level));
   }

   @Nullable
   static Vec3 openEndpointTipRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
      float ext = openEndExtensionPreviewBlocks(anchorPos) != null
         ? openEndExtensionPreviewBlocks(anchorPos)
         : CoasterOpenEndExtension.extensionBlocks(level, anchorPos);
      Vec3 tipPlot = CoasterOpenEndExtension.tipPlotLocal(bc, anchorPos, ext);
      return CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, anchorPos, tipPlot, partialTick);
   }

   @Nullable
   static Vec3 openEndpointTipRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
      return openEndpointTipRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks(level));
   }

   @Nullable
   public static Float openEndExtensionPreviewBlocks(BlockPos anchorPos) {
      if (draggingOpenEndpoint && dragOpenEndpointAnchorPos != null && dragOpenEndpointAnchorPos.equals(anchorPos)) {
         return Float.isNaN(openEndpointPreviewExtensionBlocks) ? null : openEndpointPreviewExtensionBlocks;
      } else {
         return null;
      }
   }

   @Nullable
   public static BlockPos openEndpointDragAnchor() {
      return draggingOpenEndpoint ? dragOpenEndpointAnchorPos : null;
   }

   @Nullable
   static Vec3 curveStartAtAnchor(BezierConnection bc, BlockPos anchorPos) {
      if (((BlockPos)bc.bePositions.getFirst()).equals(anchorPos)) {
         return (Vec3)bc.starts.getFirst();
      } else {
         return ((BlockPos)bc.bePositions.getSecond()).equals(anchorPos) ? (Vec3)bc.starts.getSecond() : null;
      }
   }

   private static BezierConnection loadPrimary(Level level, BlockPos host, BlockPos remote) {
      if (!(level.getBlockEntity(host) instanceof CoasterAnchorpointBlockEntity ape)) {
         return null;
      } else {
         BezierConnection c = ape.getAnchorPeerCurvesView().get(remote);
         return c != null && c.isPrimary() ? c : null;
      }
   }

   private static void updatePreviewFromVirtualTarget(Minecraft mc) {
      Level level = mc.level;
      if (level != null && dragEndpointPos != null && mc.player != null) {
         Vec3 raw = computeTangentDragRawEyeTarget(mc);
         if (raw != null) {
            CoasterBezierHandleEdit.TangentGuideSnapResult snap = computeTangentGuideSnap(mc, raw);
            Vec3 virtualTarget = snap.snappedVirtualTarget();
            if (mc.player != null) {
               showTangentGuideAlignmentActionBar(mc, snap.guideAlignmentAxes());
            }

            Vec3 handleRoot = CoasterAnchorpointBlockEntity.worldCenter(level, dragEndpointPos);
            Vec3 handleOffset = virtualTarget.subtract(handleRoot);
            preview = CoasterBezierHandleEdit.computePreview(level, dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, handleOffset);
            tickHandleDragSoundFromWorldPos(mc, tangentDragHandleTipRenderWorld(mc));
         }
      }
   }

   @Nullable
   private static Vec3 tangentDragHandleTipRenderWorld(Minecraft mc) {
      Level level = mc.level;
      if (level != null && preview != null && dragEndpointPos != null && dragActiveEdgeKey != null) {
         for (BezierConnection c : preview.allUpdated()) {
            if (edgeKey(c).equals(dragActiveEdgeKey)) {
               BezierConnection u = c.isPrimary() ? c : c.secondary();
               Vec3 start = dragEndpointIndex == 0 ? (Vec3)u.starts.getFirst() : (Vec3)u.starts.getSecond();
               Vec3 axis = dragEndpointIndex == 0 ? (Vec3)u.axes.getFirst() : (Vec3)u.axes.getSecond();
               double len = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(u, dragEndpointIndex);
               Vec3 tipPlot = axis.lengthSqr() < 1.0E-12 ? start : start.add(axis.normalize().scale(len));
               float pt = AnimationTickHolder.getPartialTicks(level);
               return CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, dragEndpointPos, tipPlot, pt);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   private static Vec3 computeTangentDragRawEyeTarget(Minecraft mc) {
      Level level = mc.level;
      if (level != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 eye = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         return eye.add(fwd.scale(dragViewForwardDistance));
      } else {
         return null;
      }
   }

   private static CoasterBezierHandleEdit.TangentGuideSnapResult computeTangentGuideSnap(Minecraft mc, Vec3 rawEyeTarget) {
      Level level = mc.level;
      if (level != null && dragPrimaryHost != null && dragRemoteEnd != null && dragEndpointPos != null) {
         Vec3 plotTarget = CoasterAnchorClientSpace.toPlotLocal(level, dragEndpointPos, rawEyeTarget);
         return CoasterBezierHandleEdit.snapBezierTangentVirtualTargetWithGuidePlanes(
            level, dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, plotTarget, !isCreateAltFineDown(), 0.3
         );
      } else {
         return new CoasterBezierHandleEdit.TangentGuideSnapResult(rawEyeTarget, 0);
      }
   }

   private static void updateTiltPreviewFromVirtualTarget(Minecraft mc) {
      Vec3 virtualTarget = tiltDragEyeVirtualTarget(mc);
      if (virtualTarget != null) {
         Level level = mc.level;
         tiltPreview = CoasterBezierHandleEdit.computeTiltPreview(level, dragTiltAnchorPos, virtualTarget);
         Double theta = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget(level, dragTiltAnchorPos, virtualTarget);
         if (theta != null) {
            tickHandleDragSoundFromScalar(mc, theta);
            if (mc.player != null && tiltPreview != null && tiltPreview.valid()) {
               showLiveTiltAngleReadout(mc, theta);
            }
         }
      }
   }

   private static void showLiveLiftReadout(Minecraft mc, float liftBlocksMeters) {
      if (mc.player != null) {
         mc.player
            .displayClientMessage(
               CoasterBezierHandleEdit.liftMetersComponent(liftBlocksMeters).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16777215))), true
            );
      }
   }

   private static void showLiveOpenEndExtensionReadout(Minecraft mc, float extensionBlocks) {
      if (mc.player != null) {
         mc.player
            .displayClientMessage(
               CoasterBezierHandleEdit.openEndExtensionBlocksComponent(extensionBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16777215))), true
            );
      }
   }

   private static void showLiveTiltAngleReadout(Minecraft mc, double jointBankRadians) {
      if (mc.player != null) {
         mc.player
            .displayClientMessage(
               CoasterBezierHandleEdit.jointBankDegreesComponent(jointBankRadians).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16777215))), true
            );
      }
   }

   private static void showTangentGuideAlignmentActionBar(Minecraft mc, int guideAlignmentAxes) {
      if (mc.player != null) {
         int blueRgb = 11781103;
         int whiteRgb = 16777215;
         String key;
         int rgb;
         if (guideAlignmentAxes >= 2) {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_aligned_2_axis";
            rgb = whiteRgb;
         } else if (guideAlignmentAxes == 1) {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_aligned_1_axis";
            rgb = whiteRgb;
         } else {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_unaligned";
            rgb = blueRgb;
         }

         mc.player.displayClientMessage(Component.translatable(key).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))), true);
      }
   }

   public static boolean shouldShowBezierSnapControlHint() {
      return draggingLift && liftPreview != null && liftPreview.valid()
         || draggingTilt && tiltPreview != null && tiltPreview.valid()
         || dragging && preview != null && preview.valid()
         || draggingOpenEndpoint && dragOpenEndpointAnchorPos != null && !Float.isNaN(openEndpointPreviewExtensionBlocks);
   }

   public static boolean shouldShowBezierEditAnchorStatusHud(Minecraft mc) {
      if (mc.player != null && !mc.options.hideGui && mc.screen == null) {
         if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
         } else if (!BezierHandleEditMode.isActive()) {
            return false;
         } else if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            return false;
         } else if (shouldShowBezierSnapControlHint()) {
            return false;
         } else if (!dragging && !draggingTilt && !draggingLift) {
            BlockPos ap = BezierHandleEditMode.getActiveAnchor();
            return ap != null && mc.level != null && mc.level.getBlockEntity(ap) instanceof CoasterAnchorpointBlockEntity;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void renderBezierEditAnchorStatusHud(Minecraft mc, GuiGraphics guiGraphics) {
      if (shouldShowBezierEditAnchorStatusHud(mc)) {
         BlockPos ap = BezierHandleEditMode.getActiveAnchor();
         if (ap != null && mc.level != null && mc.level.getBlockEntity(ap) instanceof CoasterAnchorpointBlockEntity be) {
            int var18 = 16777215;
            int yellowRgb = 16768341;
            Style styleWhite = Style.EMPTY.withColor(TextColor.fromRgb(var18));
            Style styleCurvature = Style.EMPTY.withColor(TextColor.fromRgb(hoverPick != null ? yellowRgb : var18));
            Style styleTilt = Style.EMPTY.withColor(TextColor.fromRgb(hoverTilt != null ? yellowRgb : var18));
            Style styleLift = Style.EMPTY.withColor(TextColor.fromRgb(hoverLift != null ? yellowRgb : var18));
            String tiltStr = CoasterBezierHandleEdit.formatJointBankDegreesReadout((double)be.getJointBankRadians());
            String liftStr = CoasterBezierHandleEdit.formatLiftMetersReadout(be.getLiftBlocks());
            MutableComponent line = Component.empty()
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_curvature").withStyle(styleCurvature))
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite))
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_tilt", new Object[]{tiltStr}).withStyle(styleTilt))
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite))
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_lift", new Object[]{liftStr}).withStyle(styleLift));
            Font font = mc.font;
            int w = font.width(line);
            int x = (guiGraphics.guiWidth() - w) / 2;
            int y = bezierEditHudRowAboveHotbarY(mc, guiGraphics);
            int backdropTint = ARGB32.color(255, var18);
            guiGraphics.drawStringWithBackdrop(font, line, x, y, w, backdropTint);
         }
      }
   }

   public static int editHudRowAboveHotbarY(Minecraft mc, GuiGraphics guiGraphics) {
      return bezierEditHudRowAboveHotbarY(mc, guiGraphics);
   }

   private static int bezierEditHudRowAboveHotbarY(Minecraft mc, GuiGraphics guiGraphics) {
      int yShift = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
      int y = guiGraphics.guiHeight() - Math.max(yShift, 59);
      y += 14;
      return y - 9 - 2;
   }

   public static void renderBezierSnapControlHintHud(Minecraft mc, GuiGraphics guiGraphics) {
      if (shouldShowBezierSnapControlHint() && mc.player != null && !mc.options.hideGui) {
         if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            int y = bezierEditHudRowAboveHotbarY(mc, guiGraphics);
            int blueRgb = 11781103;
            int keyRgb = isCreateAltFineDown() ? 16777215 : blueRgb;
            Style keyStyle = Style.EMPTY.withColor(TextColor.fromRgb(keyRgb));
            MutableComponent bracketedKey = Component.literal("[")
               .withStyle(keyStyle)
               .append(Component.literal(AllKeys.ALT_MODIFIER.getBoundKey()).withStyle(keyStyle))
               .append(Component.literal("]").withStyle(keyStyle));
            MutableComponent hint = Component.translatable("simulatedcoasters.track.bezier_edit_lift_snap_hint", new Object[]{bracketedKey})
               .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(blueRgb)));
            Font font = mc.font;
            int w = font.width(hint);
            int x = (guiGraphics.guiWidth() - w) / 2;
            int color = ARGB32.color(255, 11781103);
            guiGraphics.drawStringWithBackdrop(font, hint, x, y, w, color);
         }
      }
   }

   private static void updateLiftPreviewFromVirtualTarget(Minecraft mc) {
      Vec3 virtualTarget = liftDragEyeVirtualTarget(mc);
      if (virtualTarget != null) {
         Level level = mc.level;
         liftPreview = CoasterBezierHandleEdit.computeLiftPreview(level, dragLiftAnchorPos, virtualTarget);
         Float w = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget(level, dragLiftAnchorPos, virtualTarget);
         if (w != null) {
            tickHandleDragSoundFromScalar(mc, (double)w.floatValue());
            if (mc.player != null && liftPreview != null && liftPreview.valid()) {
               showLiveLiftReadout(mc, w);
            }
         }
      }
   }

   private static void updateOpenEndpointPreviewFromVirtualTarget(Minecraft mc) {
      Vec3 virtualTarget = openEndpointDragVirtualTarget(mc);
      if (virtualTarget != null) {
         Level level = mc.level;
         if (level != null && dragOpenEndpointAnchorPos != null) {
            CoasterBezierHandleEdit.OpenEndExtensionPreviewResult pr = CoasterBezierHandleEdit.computeOpenEndExtensionPreview(
               level, dragOpenEndpointAnchorPos, virtualTarget, false
            );
            if (pr.valid()) {
               openEndpointPreviewExtensionBlocks = pr.extensionBlocks();
               if (mc.player != null) {
                  showLiveOpenEndExtensionReadout(mc, pr.extensionBlocks());
               }

               tickHandleDragSoundFromScalar(mc, (double)pr.extensionBlocks());
            }
         }
      }
   }

   @Nullable
   private static Vec3 openEndpointDragVirtualTarget(Minecraft mc) {
      Vec3 eyeRayTarget = openEndpointDragEyeVirtualTargetPlotLocal(mc);
      return eyeRayTarget != null && mc.level != null && dragOpenEndpointAnchorPos != null
         ? openEndDragVirtualTarget(mc.level, dragOpenEndpointAnchorPos, eyeRayTarget)
         : null;
   }

   private static Vec3 openEndDragVirtualTarget(Level level, BlockPos anchorPos, Vec3 eyeRayTargetPlotLocal) {
      if (isCreateAltFineDown()) {
         return eyeRayTargetPlotLocal;
      } else {
         BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, anchorPos);
         if (bc == null) {
            return eyeRayTargetPlotLocal;
         } else {
            Float raw = CoasterOpenEndExtension.extensionBlocksFromVirtualTarget(level, anchorPos, bc, eyeRayTargetPlotLocal, false);
            if (raw == null) {
               return eyeRayTargetPlotLocal;
            } else {
               float snapped = CoasterOpenEndExtension.snapExtensionBlocksToCenter(raw);
               return CoasterOpenEndExtension.virtualTargetPlotLocalForExtensionBlocks(bc, anchorPos, eyeRayTargetPlotLocal, snapped);
            }
         }
      }
   }

   @Nullable
   private static Vec3 openEndpointDragEyeVirtualTargetPlotLocal(Minecraft mc) {
      Level level = mc.level;
      if (level != null && dragOpenEndpointAnchorPos != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 eye = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         Vec3 eyeRayTargetRender = eye.add(fwd.scale(dragViewForwardDistance));
         return CoasterAnchorClientSpace.toPlotLocal(level, dragOpenEndpointAnchorPos, eyeRayTargetRender);
      } else {
         return null;
      }
   }

   @Nullable
   private static Vec3 liftDragEyeVirtualTarget(Minecraft mc) {
      Level level = mc.level;
      if (level != null && dragLiftAnchorPos != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 eye = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
         return liftDragVirtualTarget(mc, level, eyeRayTarget);
      } else {
         return null;
      }
   }

   private static void resetHandleDragSoundSample() {
      lastHandleDragSoundWorldPos = null;
      lastHandleDragSoundScalar = Double.NaN;
      handleDragTickSoundAccum = 0.0;
   }

   private static void tickHandleDragSoundFromWorldPos(Minecraft mc, @Nullable Vec3 currentWorld) {
      if (mc.player != null && mc.level != null && currentWorld != null) {
         if (lastHandleDragSoundWorldPos == null) {
            lastHandleDragSoundWorldPos = currentWorld;
         } else {
            accumulateHandleDragSoundFromMovement(mc, lastHandleDragSoundWorldPos.distanceTo(currentWorld));
            lastHandleDragSoundWorldPos = currentWorld;
         }
      }
   }

   private static void tickHandleDragSoundFromScalar(Minecraft mc, double current) {
      if (mc.player != null && mc.level != null && !Double.isNaN(current)) {
         if (Double.isNaN(lastHandleDragSoundScalar)) {
            lastHandleDragSoundScalar = current;
         } else {
            accumulateHandleDragSoundFromMovement(mc, Math.abs(current - lastHandleDragSoundScalar));
            lastHandleDragSoundScalar = current;
         }
      }
   }

   private static void accumulateHandleDragSoundFromMovement(Minecraft mc, double deltaBlocks) {
      if (!(deltaBlocks < 0.002)) {
         handleDragTickSoundAccum += deltaBlocks;

         while (handleDragTickSoundAccum >= 0.08) {
            handleDragTickSoundAccum -= 0.08;
            float vol = Mth.clamp((float)(deltaBlocks * 0.85F), 0.035F, 0.1F);
            float pitch = 0.84F + mc.level.random.nextFloat() * 0.26F;
            BezierHandleClientSounds.playCreateUi(AllSoundEvents.SCROLL_VALUE.getMainEvent(), vol, pitch);
         }
      }
   }

   private static Vec3 liftDragVirtualTarget(Minecraft mc, Level level, Vec3 eyeRayTargetRender) {
      if (dragLiftAnchorPos == null) {
         return eyeRayTargetRender;
      } else {
         Vec3 eyeRayTarget = CoasterAnchorClientSpace.toPlotLocal(level, dragLiftAnchorPos, eyeRayTargetRender);
         if (isCreateAltFineDown()) {
            return eyeRayTarget;
         } else {
            Float rawLift = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget(level, dragLiftAnchorPos, eyeRayTarget);
            if (rawLift == null) {
               return eyeRayTarget;
            } else {
               float snapped = CoasterBezierHandleEdit.snapLiftBlocksToNearestHalf(rawLift);
               Vec3 adj = CoasterBezierHandleEdit.liftVirtualTargetWorldForStoredLift(level, dragLiftAnchorPos, eyeRayTarget, snapped);
               return adj != null ? adj : eyeRayTarget;
            }
         }
      }
   }

   private static boolean isCreateAltFineDown() {
      return AllKeys.altDown();
   }

   private static Vec3 tiltDragVirtualTarget(Minecraft mc, Level level, Vec3 eyeRayTargetRender) {
      if (dragTiltAnchorPos == null) {
         return eyeRayTargetRender;
      } else {
         Vec3 eyeRayTarget = CoasterAnchorClientSpace.toPlotLocal(level, dragTiltAnchorPos, eyeRayTargetRender);
         if (isCreateAltFineDown()) {
            return eyeRayTarget;
         } else {
            Double rawTheta = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget(level, dragTiltAnchorPos, eyeRayTarget);
            if (rawTheta == null) {
               return eyeRayTarget;
            } else {
               double snapped = CoasterBezierHandleEdit.snapJointBankRadiansToNearestTiltSnapStep(level, dragTiltAnchorPos, rawTheta);
               Vec3 adj = CoasterBezierHandleEdit.tiltVirtualTargetWorldForJointBankRadians(level, dragTiltAnchorPos, eyeRayTarget, snapped);
               return adj != null ? adj : eyeRayTarget;
            }
         }
      }
   }

   @Nullable
   private static Vec3 tiltDragEyeVirtualTarget(Minecraft mc) {
      Level level = mc.level;
      if (level != null && dragTiltAnchorPos != null && mc.player != null) {
         float pt = AnimationTickHolder.getPartialTicks(level);
         Vec3 eye = mc.player.getEyePosition(pt);
         Vec3 fwd = mc.player.getViewVector(pt);
         if (fwd.lengthSqr() < 1.0E-12) {
            fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
         } else {
            fwd = fwd.normalize();
         }

         Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
         return tiltDragVirtualTarget(mc, level, eyeRayTarget);
      } else {
         return null;
      }
   }

   private static void releaseCommit(Minecraft mc) {
      if (draggingTilt) {
         updateTiltPreviewFromVirtualTarget(mc);
         if (tiltPreview != null && tiltPreview.valid() && dragTiltAnchorPos != null && mc.level != null && mc.player != null) {
            Vec3 virtualTarget = tiltDragEyeVirtualTarget(mc);
            if (virtualTarget != null) {
               PacketDistributor.sendToServer(new BezierTiltEditPayload(dragTiltAnchorPos, virtualTarget), new CustomPacketPayload[0]);
               BezierHandleClientSounds.playCommitSuccessSound(mc);
               cancelInner(mc, false);
            } else {
               cancelInner(mc, true);
            }
         } else {
            cancelInner(mc, true);
         }

         useKeyWasDown = false;
      } else if (draggingLift) {
         updateLiftPreviewFromVirtualTarget(mc);
         if (liftPreview != null && liftPreview.valid() && dragLiftAnchorPos != null && mc.level != null && mc.player != null) {
            float pt = AnimationTickHolder.getPartialTicks(mc.level);
            Vec3 eye = mc.player.getEyePosition(pt);
            Vec3 fwd = mc.player.getViewVector(pt);
            if (fwd.lengthSqr() < 1.0E-12) {
               fwd = cameraLookDir(mc.gameRenderer.getMainCamera());
            } else {
               fwd = fwd.normalize();
            }

            Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
            Vec3 virtualTarget = liftDragVirtualTarget(mc, mc.level, eyeRayTarget);
            PacketDistributor.sendToServer(new BezierLiftEditPayload(dragLiftAnchorPos, virtualTarget), new CustomPacketPayload[0]);
            BezierHandleClientSounds.playCommitSuccessSound(mc);
            cancelInner(mc, false);
         } else {
            cancelInner(mc, true);
         }

         useKeyWasDown = false;
      } else if (!draggingOpenEndpoint) {
         updatePreviewFromVirtualTarget(mc);
         if (preview != null && preview.valid() && mc.level != null && mc.player != null && dragEndpointPos != null) {
            Vec3 raw = computeTangentDragRawEyeTarget(mc);
            if (raw != null) {
               Vec3 virtualTarget = computeTangentGuideSnap(mc, raw).snappedVirtualTarget();
               Vec3 handleRoot = CoasterAnchorpointBlockEntity.worldCenter(mc.level, dragEndpointPos);
               Vec3 handleOffset = virtualTarget.subtract(handleRoot);
               PacketDistributor.sendToServer(
                  new BezierHandleEditPayload(dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, handleOffset), new CustomPacketPayload[0]
               );
               BezierHandleClientSounds.playCommitSuccessSound(mc);
            }
         }

         cancelInner(mc, true);
         useKeyWasDown = false;
      } else {
         updateOpenEndpointPreviewFromVirtualTarget(mc);
         if (dragOpenEndpointAnchorPos != null && mc.level != null && mc.player != null && !Float.isNaN(openEndpointPreviewExtensionBlocks)) {
            Vec3 virtualTarget = openEndpointDragVirtualTarget(mc);
            if (virtualTarget != null) {
               PacketDistributor.sendToServer(new BezierOpenEndExtensionEditPayload(dragOpenEndpointAnchorPos, virtualTarget), new CustomPacketPayload[0]);
               BezierHandleClientSounds.playCommitSuccessSound(mc);
               cancelInner(mc, false);
            } else {
               cancelInner(mc, true);
            }
         } else {
            cancelInner(mc, true);
         }

         useKeyWasDown = false;
      }
   }

   private static void cancelInner() {
      cancelInner(Minecraft.getInstance(), true);
   }

   private static void cancelInner(Minecraft mc, boolean clearHandleHud) {
      boolean hadLiftDrag = draggingLift;
      boolean hadTiltDrag = draggingTilt;
      boolean hadTangentDrag = dragging;
      boolean hadOpenEndpointDrag = draggingOpenEndpoint;
      suppressHandleGrabUntilUseRelease = false;
      resetHandleDragSoundSample();
      dragging = false;
      draggingTilt = false;
      draggingLift = false;
      draggingOpenEndpoint = false;
      hoverPick = null;
      hoverTilt = null;
      hoverLift = null;
      hoverOpenEndpoint = null;
      dragPrimaryHost = null;
      dragRemoteEnd = null;
      dragEndpointPos = null;
      dragTiltAnchorPos = null;
      dragLiftAnchorPos = null;
      dragOpenEndpointAnchorPos = null;
      openEndpointPreviewExtensionBlocks = Float.NaN;
      dragActiveEdgeKey = null;
      dragViewForwardDistance = 0.0;
      preview = null;
      tiltPreview = null;
      liftPreview = null;
      if (clearHandleHud && (hadLiftDrag || hadTiltDrag || hadTangentDrag || hadOpenEndpointDrag) && mc != null && mc.player != null) {
         mc.player.displayClientMessage(Component.empty(), true);
      }
   }

   public static BezierHandleDragManager.HandlePick getHoverPick() {
      return hoverPick;
   }

   public static boolean isDraggingHandle() {
      return dragging || draggingTilt || draggingLift || draggingOpenEndpoint;
   }

   public static boolean isDraggingTangentHandle() {
      return dragging;
   }

   public static boolean isDraggingTangentOnAnchor(BlockPos anchorBlock) {
      return dragging && dragEndpointPos != null && dragEndpointPos.equals(anchorBlock);
   }

   public static boolean isDraggingOpenEndpointHandle() {
      return draggingOpenEndpoint;
   }

   public static boolean isHoveredOpenEndpointAnchor(BlockPos anchorBlock) {
      return !dragging
         && !draggingTilt
         && !draggingLift
         && !draggingOpenEndpoint
         && hoverOpenEndpoint != null
         && hoverOpenEndpoint.anchorPos().equals(anchorBlock);
   }

   public static boolean isDraggingTiltHandle() {
      return draggingTilt;
   }

   public static boolean isDraggingLiftHandle() {
      return draggingLift;
   }

   public static boolean isHoveringOrDraggingAnyHandle(Minecraft mc) {
      if (isDraggingHandle()) {
         return true;
      } else if (BezierHandleEditMode.isActive() && mc.player != null && mc.level != null) {
         resolveHover(mc);
         return hoverPick != null || hoverTilt != null || hoverLift != null || hoverOpenEndpoint != null;
      } else {
         return false;
      }
   }

   public static boolean shouldSuppressVanillaUse(Minecraft mc) {
      return mc.player != null && mc.level != null && mc.screen == null ? isHoveringOrDraggingAnyHandle(mc) : false;
   }

   @Nullable
   public static CoasterBezierHandleEdit.PreviewResult currentDragPreview() {
      if (draggingTilt) {
         return tiltPreview;
      } else {
         return draggingLift ? liftPreview : preview;
      }
   }

   @Nullable
   public static BezierHandleDragManager.AnchorFrameOverride livePreviewedAnchorFrame(BlockPos anchor) {
      CoasterBezierHandleEdit.PreviewResult pr = currentDragPreview();
      if (pr != null && pr.allUpdated() != null) {
         for (BezierConnection c : pr.allUpdated()) {
            if (((BlockPos)c.bePositions.getFirst()).equals(anchor)) {
               Vec3 tIn = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchor);
               if (tIn != null) {
                  return new BezierHandleDragManager.AnchorFrameOverride((Vec3)c.starts.getFirst(), (Vec3)c.normals.getFirst(), tIn);
               }
            }

            if (((BlockPos)c.bePositions.getSecond()).equals(anchor)) {
               Vec3 tIn = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchor);
               if (tIn != null) {
                  return new BezierHandleDragManager.AnchorFrameOverride((Vec3)c.starts.getSecond(), (Vec3)c.normals.getSecond(), tIn);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean isHoveredEndpoint(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
      if (!dragging && !draggingTilt && !draggingLift && mc.level != null) {
         BezierHandleDragManager.HandlePick h = hoverPick;
         if (h == null) {
            return false;
         } else {
            BezierConnection bc = loadPrimary(mc.level, h.primaryHost(), h.remoteEnd());
            if (bc != null && edgeKey(bc).equals(edgeKey)) {
               BlockPos ep = h.endpointIndex() == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
               return ep.equals(endpointBlock);
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isDraggingEndpoint(String edgeKey, BlockPos endpointBlock) {
      return dragging && dragActiveEdgeKey != null && dragEndpointPos != null && dragActiveEdgeKey.equals(edgeKey) && dragEndpointPos.equals(endpointBlock);
   }

   public static boolean isDraggingTiltOnAnchor(BlockPos anchorBlock) {
      return draggingTilt && dragTiltAnchorPos != null && dragTiltAnchorPos.equals(anchorBlock);
   }

   public static boolean isHoveredTiltAnchor(BlockPos anchorBlock) {
      return !dragging && !draggingTilt && !draggingLift && hoverTilt != null && hoverTilt.anchorPos().equals(anchorBlock);
   }

   public static boolean isDraggingLiftOnAnchor(BlockPos anchorBlock) {
      return draggingLift && dragLiftAnchorPos != null && dragLiftAnchorPos.equals(anchorBlock);
   }

   public static boolean isHoveredLiftAnchor(BlockPos anchorBlock) {
      return !dragging && !draggingTilt && !draggingLift && hoverLift != null && hoverLift.anchorPos().equals(anchorBlock);
   }

   private static double raySphere(Vec3 ro, Vec3 rd, Vec3 center, double radius) {
      Vec3 oc = ro.subtract(center);
      double b = oc.dot(rd);
      double c = oc.dot(oc) - radius * radius;
      double disc = b * b - c;
      if (disc < 0.0) {
         return -1.0;
      } else {
         double sqrt = Math.sqrt(disc);
         double t0 = -b - sqrt;
         double t1 = -b + sqrt;
         return t0 >= 1.0E-4 ? t0 : (t1 >= 1.0E-4 ? t1 : -1.0);
      }
   }

   public static String edgeKey(BezierConnection bc) {
      long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
      long b = ((BlockPos)bc.bePositions.getSecond()).asLong();
      return a <= b ? a + ":" + b : b + ":" + a;
   }

   public static BezierConnection previewCurveOrWorld(String edgeKey, BezierConnection worldBc) {
      if (draggingTilt && tiltPreview != null) {
         for (BezierConnection c : tiltPreview.allUpdated()) {
            if (edgeKey(c).equals(edgeKey)) {
               return c;
            }
         }

         return worldBc;
      } else if (draggingLift && liftPreview != null) {
         for (BezierConnection cx : liftPreview.allUpdated()) {
            if (edgeKey(cx).equals(edgeKey)) {
               return cx;
            }
         }

         return worldBc;
      } else if (dragging && preview != null) {
         for (BezierConnection cxx : preview.allUpdated()) {
            if (edgeKey(cxx).equals(edgeKey)) {
               return cxx;
            }
         }

         return worldBc;
      } else {
         return worldBc;
      }
   }

   public static boolean previewValid() {
      return draggingTilt && tiltPreview != null && tiltPreview.valid()
         || draggingLift && liftPreview != null && liftPreview.valid()
         || dragging && preview != null && preview.valid();
   }

   public static boolean previewInvalidDrag() {
      return draggingTilt && tiltPreview != null && !tiltPreview.valid()
         || draggingLift && liftPreview != null && !liftPreview.valid()
         || dragging && preview != null && !preview.valid();
   }

   public static boolean invalidTint(String edgeKey) {
      return previewInvalidDrag() && previewTouchesEdge(edgeKey);
   }

   public static boolean validDragTint(String edgeKey) {
      return previewValid() && previewTouchesEdge(edgeKey);
   }

   private static boolean previewTouchesEdge(String ek) {
      if (draggingTilt && tiltPreview != null) {
         for (BezierConnection c : tiltPreview.allUpdated()) {
            if (edgeKey(c).equals(ek)) {
               return true;
            }
         }
      }

      if (draggingLift && liftPreview != null) {
         for (BezierConnection cx : liftPreview.allUpdated()) {
            if (edgeKey(cx).equals(ek)) {
               return true;
            }
         }
      }

      if (dragging && preview != null) {
         for (BezierConnection cxx : preview.allUpdated()) {
            if (edgeKey(cxx).equals(ek)) {
               return true;
            }
         }
      }

      return false;
   }

   public static record AnchorFrameOverride(Vec3 center, Vec3 faceNormal, Vec3 tangentInto) {
   }

   public static record HandlePick(BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleTipWorld) {
   }

   public static record LiftHover(BlockPos anchorPos, Vec3 tipWorld) {
   }

   public static record OpenEndpointHover(BlockPos anchorPos, Vec3 tipWorld) {
   }

   public static record TiltHover(BlockPos anchorPos, Vec3 tipWorld) {
   }
}
