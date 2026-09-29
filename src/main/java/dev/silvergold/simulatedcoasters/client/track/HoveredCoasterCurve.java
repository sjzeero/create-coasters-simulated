package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.world.phys.Vec3;

record HoveredCoasterCurve(BezierConnection bc, Vec3 hoverWorld) {
}
