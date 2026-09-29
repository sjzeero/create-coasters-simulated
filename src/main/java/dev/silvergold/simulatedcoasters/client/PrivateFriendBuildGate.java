/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package dev.silvergold.simulatedcoasters.client;

import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PrivateFriendBuildGate {
    private static final Set<String> ALLOWED = Set.of("silvergold", "redunes", "rawrf0rlife");

    private PrivateFriendBuildGate() {
    }

    public static void tick(Minecraft mc) {
        if (Boolean.getBoolean("simulatedcoasters.skipFriendGate")) {
            return;
        }
        if (PrivateFriendBuildGate.isAuthorized(mc)) {
            return;
        }
        if (mc.screen instanceof PrivateFriendBuildBlockedScreen) {
            return;
        }
        mc.setScreen((Screen)new PrivateFriendBuildBlockedScreen());
    }

    private static boolean isAuthorized(Minecraft mc) {
        if (mc.getUser() == null) {
            return false;
        }
        String name = mc.getUser().getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        return ALLOWED.contains(name.toLowerCase(Locale.ROOT));
    }

    public static final class PrivateFriendBuildBlockedScreen
    extends Screen {
        public PrivateFriendBuildBlockedScreen() {
            super((Component)Component.literal((String)"Simulated Coasters \u2014 private build"));
        }

        protected void init() {
            super.init();
            int cx = this.width / 2;
            int cy = this.height / 2;
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"Quit Game"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.stop();
                }
            }).bounds(cx - 100, cy + 72, 200, 20).build());
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            int cx = this.width / 2;
            int y = this.height / 2 - 72;
            graphics.drawCenteredString(this.font, (Component)Component.literal((String)"This Simulated Coasters build is not for you."), cx, y, 0xFF5555);
            graphics.drawCenteredString(this.font, (Component)Component.literal((String)"Nice try though \u2014 tragic, isn't it?"), cx, y += 14, 0xCCCCCC);
            graphics.drawCenteredString(this.font, (Component)Component.literal((String)"Only whitelisted accounts may play with this JAR."), cx, y += 28, 0xAAAAAA);
            graphics.drawCenteredString(this.font, (Component)Component.literal((String)"If you were given this by mistake, delete it."), cx, y += 14, 0x888888);
        }

        public boolean shouldCloseOnEsc() {
            return false;
        }

        public boolean isPauseScreen() {
            return true;
        }
    }
}

