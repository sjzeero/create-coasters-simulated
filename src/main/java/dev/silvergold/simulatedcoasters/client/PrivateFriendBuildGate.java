package dev.silvergold.simulatedcoasters.client;

import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PrivateFriendBuildGate {
   private static final Set<String> ALLOWED = Set.of("silvergold", "redunes", "rawrf0rlife");

   private PrivateFriendBuildGate() {
   }

   public static void tick(Minecraft mc) {
      if (!Boolean.getBoolean("simulatedcoasters.skipFriendGate")) {
         if (!isAuthorized(mc)) {
            if (!(mc.screen instanceof PrivateFriendBuildGate.PrivateFriendBuildBlockedScreen)) {
               mc.setScreen(new PrivateFriendBuildGate.PrivateFriendBuildBlockedScreen());
            }
         }
      }
   }

   private static boolean isAuthorized(Minecraft mc) {
      if (mc.getUser() == null) {
         return false;
      } else {
         String name = mc.getUser().getName();
         return name != null && !name.isBlank() ? ALLOWED.contains(name.toLowerCase(Locale.ROOT)) : false;
      }
   }

   public static final class PrivateFriendBuildBlockedScreen extends Screen {
      public PrivateFriendBuildBlockedScreen() {
         super(Component.literal("Simulated Coasters — private build"));
      }

      protected void init() {
         super.init();
         int cx = this.width / 2;
         int cy = this.height / 2;
         this.addRenderableWidget(Button.builder(Component.literal("Quit Game"), b -> {
            if (this.minecraft != null) {
               this.minecraft.stop();
            }
         }).bounds(cx - 100, cy + 72, 200, 20).build());
      }

      public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         super.render(graphics, mouseX, mouseY, partialTick);
         int cx = this.width / 2;
         int y = this.height / 2 - 72;
         graphics.drawCenteredString(this.font, Component.literal("This Simulated Coasters build is not for you."), cx, y, 16733525);
         y += 14;
         graphics.drawCenteredString(this.font, Component.literal("Nice try though — tragic, isn't it?"), cx, y, 13421772);
         y += 28;
         graphics.drawCenteredString(this.font, Component.literal("Only whitelisted accounts may play with this JAR."), cx, y, 11184810);
         y += 14;
         graphics.drawCenteredString(this.font, Component.literal("If you were given this by mistake, delete it."), cx, y, 8947848);
      }

      public boolean shouldCloseOnEsc() {
         return false;
      }

      public boolean isPauseScreen() {
         return true;
      }
   }
}
