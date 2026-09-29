package dev.silvergold.simulatedcoasters;

import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.track.CoasterTrackMaterialBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public final class SimulatedCoastersBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("simulatedcoasters");
   public static final DeferredBlock<CoasterTrackMaterialBlock> COASTER_TRACK_MATERIAL = BLOCKS.register(
      "coaster_track_material",
      () -> new CoasterTrackMaterialBlock(
            Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion().forceSolidOn(), CoasterTrackMaterials.COASTER
         )
   );
   public static final DeferredBlock<CoasterCartBlock> COASTER_CART_BLOCK = BLOCKS.register(
      "coaster_cart", () -> new CoasterCartBlock(CoasterCartBlock.defaultProperties())
   );
   public static final DeferredBlock<CoasterAnchorpointBlock> COASTER_ANCHORPOINT = BLOCKS.register(
      "coaster_anchorpoint", () -> new CoasterAnchorpointBlock(CoasterAnchorpointBlock.defaultProperties())
   );
   public static final DeferredBlock<RivetBlock> RIVET = BLOCKS.register("rivet", () -> new RivetBlock(RivetBlock.defaultProperties()));
   public static final DeferredBlock<RedBalloonBlock> RED_BALLOON = BLOCKS.register(
      "red_balloon", () -> new RedBalloonBlock(RedBalloonBlock.defaultProperties())
   );
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "simulatedcoasters");
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RivetBlockEntity>> RIVET_BE = BLOCK_ENTITY_TYPES.register(
      "rivet", () -> Builder.of(RivetBlockEntity::new, new Block[]{(Block)RIVET.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BalloonBlockEntity>> BALLOON_BE = BLOCK_ENTITY_TYPES.register(
      "red_balloon", () -> Builder.of(BalloonBlockEntity::new, new Block[]{(Block)RED_BALLOON.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoasterAnchorpointBlockEntity>> COASTER_ANCHORPOINT_BE = BLOCK_ENTITY_TYPES.register(
      "coaster_anchorpoint", () -> Builder.of(CoasterAnchorpointBlockEntity::new, new Block[]{(Block)COASTER_ANCHORPOINT.get()}).build(null)
   );

   private SimulatedCoastersBlocks() {
   }
}
