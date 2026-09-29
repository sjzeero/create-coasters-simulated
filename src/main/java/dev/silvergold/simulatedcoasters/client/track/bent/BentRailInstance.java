package dev.silvergold.simulatedcoasters.client.track.bent;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BentRailInstance extends ColoredLitOverlayInstance {
   public final Vector3f prevSpine = new Vector3f();
   public final Vector3f currSpine = new Vector3f();
   public final Vector3f prevTangent = new Vector3f();
   public final Vector3f currTangent = new Vector3f();
   public final Vector3f prevLateral = new Vector3f();
   public final Vector3f currLateral = new Vector3f();
   public float modelAnchorY = 0.0F;

   public BentRailInstance(InstanceType<? extends BentRailInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public BentRailInstance setSegment(Vec3 prevSpineV, Vec3 currSpineV, Vec3 prevTangentV, Vec3 currTangentV, Vec3 prevLateralV, Vec3 currLateralV) {
      this.prevSpine.set((float)prevSpineV.x, (float)prevSpineV.y, (float)prevSpineV.z);
      this.currSpine.set((float)currSpineV.x, (float)currSpineV.y, (float)currSpineV.z);
      this.prevTangent.set((float)prevTangentV.x, (float)prevTangentV.y, (float)prevTangentV.z);
      this.currTangent.set((float)currTangentV.x, (float)currTangentV.y, (float)currTangentV.z);
      this.prevLateral.set((float)prevLateralV.x, (float)prevLateralV.y, (float)prevLateralV.z);
      this.currLateral.set((float)currLateralV.x, (float)currLateralV.y, (float)currLateralV.z);
      return this;
   }

   public BentRailInstance setModelAnchorY(float modelAnchorY) {
      this.modelAnchorY = modelAnchorY;
      return this;
   }

   public BentRailInstance setZeroTransform() {
      this.prevSpine.zero();
      this.currSpine.zero();
      this.prevTangent.zero();
      this.currTangent.zero();
      this.prevLateral.zero();
      this.currLateral.zero();
      this.modelAnchorY = 0.0F;
      return this;
   }
}
