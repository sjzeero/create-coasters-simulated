/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.particle.SimpleAnimatedParticle
 *  net.minecraft.client.particle.SpriteSet
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.util.Mth
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
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

@OnlyIn(value=Dist.CLIENT)
public class TrackSnapSparkParticle
extends SimpleAnimatedParticle {
    private static final float BOUNCE = 0.72f;
    private static final float SPIN_DAMPING = 0.94f;
    private static final float SHRINK_START_FRACTION = 0.6f;
    private static final float SHRINK_END_SCALE = 0.35f;
    private static final float END_R = 0.92156863f;
    private static final float END_G = 0.84313726f;
    private static final float END_B = 0.47058824f;
    private static final int LIFETIME_TICKS = 24;
    private static final int LIFETIME_JITTER = 10;
    private final float baseQuadSize;
    private float rotSpeed;

    protected TrackSnapSparkParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, sprites, 1.0f);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize *= 0.75f;
        this.baseQuadSize = this.quadSize;
        this.lifetime = 14 + this.random.nextInt(21);
        this.rotSpeed = (this.random.nextFloat() - 0.5f) * 0.44f;
        this.roll = this.random.nextFloat() * ((float)Math.PI * 2);
        this.setSpriteFromAge(sprites);
        this.setAlpha(0.99f);
        this.setColor(1.0f, 1.0f, 1.0f);
    }

    public int getLightColor(float partialTick) {
        float f = ((float)this.age + partialTick) / (float)this.lifetime;
        f = Mth.clamp((float)f, (float)0.0f, (float)1.0f);
        int i = super.getLightColor(partialTick);
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        if ((j += (int)(f * 15.0f * 16.0f)) > 240) {
            j = 240;
        }
        return j | k << 16;
    }

    public float getQuadSize(float scaleFactor) {
        float ageFrac = ((float)this.age + scaleFactor) / (float)this.lifetime;
        if (ageFrac <= 0.6f) {
            return this.baseQuadSize;
        }
        float shrinkT = (ageFrac - 0.6f) / 0.39999998f;
        return this.baseQuadSize * Mth.lerp((float)shrinkT, (float)1.0f, (float)0.35f);
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.oRoll = this.roll;
        this.roll += this.rotSpeed;
        this.rotSpeed *= 0.94f;
        this.yd -= 0.04 * (double)this.gravity;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            if (this.yd < 0.0) {
                this.yd = -this.yd * (double)0.72f;
            }
            this.xd *= (double)0.82f;
            this.zd *= (double)0.82f;
        }
        this.xd *= (double)this.friction;
        this.yd *= (double)this.friction;
        this.zd *= (double)this.friction;
        this.setSpriteFromAge(this.sprites);
        this.updateColorFromAge();
        float ageFrac = (float)this.age / (float)this.lifetime;
        if (ageFrac > 0.6f) {
            this.setAlpha(Mth.lerp((float)((ageFrac - 0.6f) / 0.39999998f), (float)0.99f, (float)0.0f));
        }
    }

    private void updateColorFromAge() {
        float t = Mth.clamp((float)((float)this.age / (float)this.lifetime * 1.75f), (float)0.0f, (float)1.0f);
        this.rCol = Mth.lerp((float)t, (float)1.0f, (float)0.92156863f);
        this.gCol = Mth.lerp((float)t, (float)1.0f, (float)0.84313726f);
        this.bCol = Mth.lerp((float)t, (float)1.0f, (float)0.47058824f);
    }

    @OnlyIn(value=Dist.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new TrackSnapSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}

