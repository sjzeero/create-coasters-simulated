/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.foundation.ponder.CreateSceneBuilder
 *  net.createmod.catnip.math.Pointing
 *  net.createmod.ponder.api.element.ElementLink
 *  net.createmod.ponder.api.element.WorldSectionElement
 *  net.createmod.ponder.api.scene.SceneBuilder
 *  net.createmod.ponder.api.scene.SceneBuildingUtil
 *  net.createmod.ponder.api.scene.Selection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.ponder.scenes;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.ponder.CoasterCartPonderExtras;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderRestore;
import dev.silvergold.simulatedcoasters.ponder.elements.CoasterCartExtrasElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class CoasterCartScenes {
    private static final int TRACK_Y = 1;
    private static final int FIRST_CART_Y = 2;
    private static final int LINKED_CARTS_Y = 3;
    private static final int SHORT_LINK_CARTS_Y = 4;
    private static final int DECORATED_CARTS_Y = 5;
    private static final int CART_DISPLAY_Y = 2;
    private static final float TRACK_ALIGNED_YAW = 90.0f;
    private static final Vec3 OFFSCREEN = new Vec3(0.0, -100.0, 0.0);
    private static final BlockPos ANCHOR_LEFT = new BlockPos(3, 0, 7);
    private static final BlockPos ANCHOR_RIGHT = new BlockPos(11, 0, 7);
    private static final BlockPos FIRST_CART = new BlockPos(7, 0, 7);
    private static final BlockPos LINKED_CART_A = new BlockPos(4, 0, 7);
    private static final BlockPos LINKED_CART_B = new BlockPos(7, 0, 7);
    private static final BlockPos SHORT_LINK_CART_A = new BlockPos(5, 0, 7);
    private static final BlockPos SHORT_LINK_CART_B = new BlockPos(7, 0, 7);
    private static final BlockPos DECORATED_CART_A = new BlockPos(5, 0, 7);
    private static final BlockPos DECORATED_CART_B = new BlockPos(7, 0, 7);
    private static final BlockPos DECORATED_CART_C = new BlockPos(9, 0, 7);

    private CoasterCartScenes() {
    }

    public static void linking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ponder_cart", "Placing Coaster Carts");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.7f);
        scene.setSceneOffsetY(-1.0f);
        scene.showBasePlate();
        scene.idle(10);
        Selection trackLayer = util.select().fromTo(3, 1, 7, 11, 1, 7);
        scene.world().showIndependentSection(trackLayer, Direction.DOWN);
        CoasterPonderRestore.applyDisplayedAnchorLayer(scene, 1, 1, ANCHOR_LEFT, ANCHOR_RIGHT);
        scene.idle(20);
        ItemStack cartStack = new ItemStack((ItemLike)SimulatedCoasters.COASTER_CART.get());
        Vec3 trackFocus = util.vector().topOf(7, 1, 7);
        Vec3 firstCartHint = util.vector().topOf(FIRST_CART.atY(2));
        Vec3 secondCartHint = util.vector().topOf(LINKED_CART_A.atY(2));
        scene.overlay().showText(80).attachKeyFrame().text("Coaster carts can be placed on coaster track").placeNearTarget().pointAt(trackFocus);
        scene.idle(35);
        ElementLink firstCart = scene.world().showIndependentSection(util.select().position(FIRST_CART.atY(2)), Direction.DOWN);
        CoasterCartScenes.poseCartOnTrack(scene, util, (ElementLink<WorldSectionElement>)firstCart, FIRST_CART.atY(2), FIRST_CART.atY(2));
        ElementLink<CoasterCartExtrasElement> firstExtras = CoasterCartPonderExtras.show(scene, CoasterCartExtrasElement.carts(90.0f, FIRST_CART.atY(2)), Direction.DOWN);
        scene.overlay().showControls(firstCartHint, Pointing.DOWN, 45).withItem(cartStack).rightClick();
        scene.idle(70);
        scene.overlay().showText(70).attachKeyFrame().text("Right click a placed cart with the Coaster Cart item to create a link").placeNearTarget().pointAt(firstCartHint);
        scene.idle(20);
        scene.overlay().showControls(firstCartHint, Pointing.DOWN, 50).withItem(cartStack).rightClick();
        scene.idle(60);
        scene.overlay().showText(70).attachKeyFrame().text("Placing a linked cart couples the two carts together").placeNearTarget().pointAt(secondCartHint);
        scene.idle(15);
        scene.overlay().showControls(secondCartHint, Pointing.DOWN, 55).withItem(cartStack).rightClick();
        scene.idle(20);
        CoasterCartScenes.hideSectionImmediately(scene, (ElementLink<WorldSectionElement>)firstCart);
        CoasterCartPonderExtras.hideImmediately(scene, firstExtras);
        ElementLink<WorldSectionElement> linkedB = CoasterCartScenes.showCartImmediately(scene, util, LINKED_CART_B, 3);
        ElementLink linkedA = scene.world().showIndependentSection(util.select().position(LINKED_CART_A.atY(3)), Direction.DOWN);
        CoasterCartScenes.poseCartOnTrack(scene, util, (ElementLink<WorldSectionElement>)linkedA, LINKED_CART_A.atY(3), LINKED_CART_A.atY(2));
        ElementLink<CoasterCartExtrasElement> linkedExtras = CoasterCartPonderExtras.showImmediately(scene, CoasterCartExtrasElement.linkedCarts(90.0f, LINKED_CART_A.atY(2), LINKED_CART_B.atY(2)));
        scene.idle(60);
        Vec3 linkedCartAHint = util.vector().topOf(LINKED_CART_A.atY(2));
        Vec3 linkedCartBHint = util.vector().topOf(LINKED_CART_B.atY(2));
        ItemStack shears = new ItemStack((ItemLike)Items.SHEARS);
        ItemStack chain = new ItemStack((ItemLike)Items.CHAIN);
        scene.overlay().showText(70).attachKeyFrame().text("Shears can be used to remove connections").placeNearTarget().pointAt(linkedCartBHint);
        scene.idle(15);
        scene.overlay().showControls(linkedCartBHint, Pointing.DOWN, 50).withItem(shears).rightClick();
        scene.idle(30);
        CoasterCartPonderExtras.setCouplerVisible(scene, linkedExtras, false);
        scene.idle(30);
        scene.overlay().showText(80).attachKeyFrame().text("Chains can be used to make new connections").placeNearTarget().pointAt(linkedCartAHint);
        scene.idle(15);
        scene.overlay().showControls(linkedCartAHint, Pointing.DOWN, 65).withItem(chain).rightClick();
        scene.idle(25);
        scene.overlay().showControls(linkedCartBHint, Pointing.DOWN, 40).withItem(chain).rightClick();
        scene.idle(20);
        CoasterCartPonderExtras.setCouplerVisible(scene, linkedExtras, true);
        scene.idle(40);
        scene.overlay().showText(80).attachKeyFrame().text("A wrench can be used to adjust the length of a cart connection").placeNearTarget().pointAt(linkedCartAHint);
        scene.idle(20);
        scene.overlay().showControls(linkedCartAHint, Pointing.DOWN, 60).withItem(AllItems.WRENCH.asStack()).rightClick();
        scene.idle(30);
        CoasterCartScenes.hideSectionImmediately(scene, (ElementLink<WorldSectionElement>)linkedA);
        CoasterCartScenes.hideSectionImmediately(scene, linkedB);
        CoasterCartPonderExtras.hideImmediately(scene, linkedExtras);
        ElementLink<WorldSectionElement> shortA = CoasterCartScenes.showCartImmediately(scene, util, SHORT_LINK_CART_A, 4);
        ElementLink<WorldSectionElement> shortB = CoasterCartScenes.showCartImmediately(scene, util, SHORT_LINK_CART_B, 4);
        ElementLink<CoasterCartExtrasElement> shortExtras = CoasterCartPonderExtras.showImmediately(scene, CoasterCartExtrasElement.linkedCarts(90.0f, SHORT_LINK_CART_A.atY(2), SHORT_LINK_CART_B.atY(2)));
        scene.idle(40);
        scene.world().hideIndependentSection(shortA, Direction.UP);
        scene.world().hideIndependentSection(shortB, Direction.UP);
        CoasterCartPonderExtras.hide(scene, shortExtras, Direction.UP);
        scene.idle(20);
        CoasterCartScenes.showDecoratedCartColumn(scene, util, DECORATED_CART_A, 4, 5, true);
        CoasterCartScenes.showDecoratedCartColumn(scene, util, DECORATED_CART_B, 6, 7, true);
        CoasterCartScenes.showDecoratedCartColumn(scene, util, DECORATED_CART_C, 8, 9, true);
        CoasterCartPonderExtras.show(scene, CoasterCartExtrasElement.trainCarts(90.0f, DECORATED_CART_A.atY(2), DECORATED_CART_B.atY(2), DECORATED_CART_C.atY(2)), Direction.DOWN);
        Vec3 decoratedFocus = util.vector().blockSurface(DECORATED_CART_B.atY(3), Direction.UP);
        scene.overlay().showText(100).attachKeyFrame().text("Blocks can be placed on coaster carts").placeNearTarget().pointAt(decoratedFocus);
        scene.idle(100);
    }

    private static void showDecoratedCartColumn(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos cartXz, int minX, int maxX, boolean fade) {
        ElementLink cart = fade ? scene.world().showIndependentSection(util.select().position(cartXz.atY(5)), Direction.DOWN) : scene.world().showIndependentSectionImmediately(util.select().position(cartXz.atY(5)));
        CoasterCartScenes.poseCartOnTrack(scene, util, (ElementLink<WorldSectionElement>)cart, cartXz.atY(5), cartXz.atY(2));
        int decorSourceY = 6;
        int decorDisplayY = 3;
        Selection decor = util.select().fromTo(minX, decorSourceY, 6, maxX, decorSourceY, 8);
        ElementLink top = fade ? scene.world().showIndependentSection(decor, Direction.DOWN) : scene.world().showIndependentSectionImmediately(decor);
        scene.world().moveSection(top, util.vector().of(0.0, (double)(decorDisplayY - decorSourceY), 0.0), 0);
    }

    private static ElementLink<WorldSectionElement> showCartImmediately(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos cartXz, int sourceY) {
        ElementLink cart = scene.world().showIndependentSectionImmediately(util.select().position(cartXz.atY(sourceY)));
        CoasterCartScenes.poseCartOnTrack(scene, util, (ElementLink<WorldSectionElement>)cart, cartXz.atY(sourceY), cartXz.atY(2));
        return cart;
    }

    private static void hideSectionImmediately(CreateSceneBuilder scene, ElementLink<WorldSectionElement> section) {
        scene.world().moveSection(section, OFFSCREEN, 0);
        scene.world().hideIndependentSection(section, Direction.DOWN);
    }

    private static void poseCartOnTrack(CreateSceneBuilder scene, SceneBuildingUtil util, ElementLink<WorldSectionElement> cart, BlockPos sourceCorner, BlockPos displayCorner) {
        scene.world().moveSection(cart, util.vector().of(0.0, (double)(displayCorner.getY() - sourceCorner.getY()), 0.0), 0);
        scene.world().configureCenterOfRotation(cart, util.vector().centerOf(sourceCorner));
        scene.world().rotateSection(cart, 0.0, 90.0, 0.0, 0);
    }
}

