/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.material.MapColor
 *  net.neoforged.neoforge.registries.DeferredBlock
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Blocks
 */
package dev.silvergold.simulatedcoasters;

import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.track.CoasterTrackMaterialBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SimulatedCoastersBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks((String)"simulatedcoasters");
    public static final DeferredBlock<CoasterTrackMaterialBlock> COASTER_TRACK_MATERIAL = BLOCKS.register("coaster_track_material", () -> new CoasterTrackMaterialBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8f).sound(SoundType.METAL).noOcclusion().forceSolidOn(), CoasterTrackMaterials.COASTER));
    public static final DeferredBlock<CoasterCartBlock> COASTER_CART_BLOCK = BLOCKS.register("coaster_cart", () -> new CoasterCartBlock(CoasterCartBlock.defaultProperties()));
    public static final DeferredBlock<CoasterAnchorpointBlock> COASTER_ANCHORPOINT = BLOCKS.register("coaster_anchorpoint", () -> new CoasterAnchorpointBlock(CoasterAnchorpointBlock.defaultProperties()));
    public static final DeferredBlock<RivetBlock> RIVET = BLOCKS.register("rivet", () -> new RivetBlock(RivetBlock.defaultProperties()));
    public static final DeferredBlock<RedBalloonBlock> RED_BALLOON = BLOCKS.register("red_balloon", () -> new RedBalloonBlock(RedBalloonBlock.defaultProperties()));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create((ResourceKey)Registries.BLOCK_ENTITY_TYPE, (String)"simulatedcoasters");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RivetBlockEntity>> RIVET_BE = BLOCK_ENTITY_TYPES.register("rivet", () -> BlockEntityType.Builder.of(RivetBlockEntity::new, (Block[])new Block[]{(Block)RIVET.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BalloonBlockEntity>> BALLOON_BE = BLOCK_ENTITY_TYPES.register("red_balloon", () -> BlockEntityType.Builder.of(BalloonBlockEntity::new, (Block[])new Block[]{(Block)RED_BALLOON.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoasterAnchorpointBlockEntity>> COASTER_ANCHORPOINT_BE = BLOCK_ENTITY_TYPES.register("coaster_anchorpoint", () -> BlockEntityType.Builder.of(CoasterAnchorpointBlockEntity::new, (Block[])new Block[]{(Block)COASTER_ANCHORPOINT.get()}).build(null));

    private SimulatedCoastersBlocks() {
    }
}

