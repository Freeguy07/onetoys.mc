package com.orehall.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NetherLapisDripParticle {
    public static final float RED = 0.11F;
    public static final float GREEN = 0.26F;
    public static final float BLUE = 0.84F;

    @OnlyIn(Dist.CLIENT)
    public static class HangParticle extends DripParticle {
        private final ParticleOptions fallingParticle;

        public HangParticle(ClientLevel level, double x, double y, double z, ParticleOptions fallingParticle) {
            super(level, x, y, z, Fluids.EMPTY);
            this.fallingParticle = fallingParticle;
            this.isGlowing = true;
            this.gravity *= 0.01F;
            this.lifetime = 100;
            this.setColor(RED, GREEN, BLUE);
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FallAndLandParticle extends DripParticle {
        protected final ParticleOptions landParticle;

        public FallAndLandParticle(ClientLevel level, double x, double y, double z, ParticleOptions landParticle) {
            super(level, x, y, z, Fluids.EMPTY);
            this.landParticle = landParticle;
            this.isGlowing = true;
            this.gravity = 0.06F;
            this.lifetime = (int) (64.0 / (Math.random() * 0.8 + 0.2));
            this.setColor(RED, GREEN, BLUE);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LandParticle extends DripParticle {
        public LandParticle(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z, Fluids.EMPTY);
            this.isGlowing = true;
            this.gravity = 0.0F;
            this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
            this.setColor(RED, GREEN, BLUE);
        }
    }
}
