package dev.silvergold.simulatedcoasters.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackSnapSparkParticle extends SimpleAnimatedParticle {
   private static final float BOUNCE = 0.72F;
   private static final float SPIN_DAMPING = 0.94F;
   private static final float SHRINK_START_FRACTION = 0.6F;
   private static final float SHRINK_END_SCALE = 0.35F;
   private static final float END_R = 0.92156863F;
   private static final float END_G = 0.84313726F;
   private static final float END_B = 0.47058824F;
   private static final int LIFETIME_TICKS = 24;
   private static final int LIFETIME_JITTER = 10;
   private final float baseQuadSize;
   private float rotSpeed;

   protected TrackSnapSparkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
      super(level, x, y, z, sprites, 1.0F);
      this.xd = xSpeed;
      this.yd = ySpeed;
      this.zd = zSpeed;
      this.quadSize *= 0.75F;
      this.baseQuadSize = this.quadSize;
      this.lifetime = 14 + this.random.nextInt(21);
      this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.44F;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
      this.setSpriteFromAge(sprites);
      this.setAlpha(0.99F);
      this.setColor(1.0F, 1.0F, 1.0F);
   }

   public int getLightColor(float partialTick) {
      float f = ((float)this.age + partialTick) / (float)this.lifetime;
      f = Mth.clamp(f, 0.0F, 1.0F);
      int i = super.getLightColor(partialTick);
      int j = i & 0xFF;
      int k = i >> 16 & 0xFF;
      j += (int)(f * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   public float getQuadSize(float scaleFactor) {
      float ageFrac = ((float)this.age + scaleFactor) / (float)this.lifetime;
      if (ageFrac <= 0.6F) {
         return this.baseQuadSize;
      } else {
         float shrinkT = (ageFrac - 0.6F) / 0.39999998F;
         return this.baseQuadSize * Mth.lerp(shrinkT, 1.0F, 0.35F);
      }
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.oRoll = this.roll;
         this.roll = this.roll + this.rotSpeed;
         this.rotSpeed *= 0.94F;
         this.yd = this.yd - 0.04 * (double)this.gravity;
         this.move(this.xd, this.yd, this.zd);
         if (this.onGround) {
            if (this.yd < 0.0) {
               this.yd = -this.yd * 0.72F;
            }

            this.xd *= 0.82F;
            this.zd *= 0.82F;
         }

         this.xd = this.xd * (double)this.friction;
         this.yd = this.yd * (double)this.friction;
         this.zd = this.zd * (double)this.friction;
         this.setSpriteFromAge(this.sprites);
         this.updateColorFromAge();
         float ageFrac = (float)this.age / (float)this.lifetime;
         if (ageFrac > 0.6F) {
            this.setAlpha(Mth.lerp((ageFrac - 0.6F) / 0.39999998F, 0.99F, 0.0F));
         }
      }
   }

   private void updateColorFromAge() {
      float t = Mth.clamp((float)this.age / (float)this.lifetime * 1.75F, 0.0F, 1.0F);
      this.rCol = Mth.lerp(t, 1.0F, 0.92156863F);
      this.gCol = Mth.lerp(t, 1.0F, 0.84313726F);
      this.bCol = Mth.lerp(t, 1.0F, 0.47058824F);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new TrackSnapSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
      }
   }
}
