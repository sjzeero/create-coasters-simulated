package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCartLinkDistanceEditPayload(UUID cartId, boolean shorten) implements CustomPacketPayload {
   public static final Type<CoasterCartLinkDistanceEditPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "cart_link_distance_edit")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartLinkDistanceEditPayload> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      CoasterCartLinkDistanceEditPayload::cartId,
      ByteBufCodecs.BOOL,
      CoasterCartLinkDistanceEditPayload::shorten,
      CoasterCartLinkDistanceEditPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterCartLinkDistanceEditPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            if (AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem())) {
               ServerLevel level = player.serverLevel();
               ServerSubLevel cart = CoasterCartResolve.findCartById(level, payload.cartId());
               if (cart != null && !cart.isRemoved()) {
                  Vec3 center = CoasterCartLinkPlacement.partnerBearingCenterWorld(cart, null);
                  if (center != null) {
                     double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
                     if (!(player.getEyePosition().distanceToSqr(center) > reach * reach)) {
                        CoasterCartTrainLinkConstraint.adjustEditModeLinkDistances(level, cart, payload.shorten());
                     }
                  }
               }
            }
         }
      });
   }
}
