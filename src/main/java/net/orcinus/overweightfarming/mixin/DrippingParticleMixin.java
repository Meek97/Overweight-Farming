package net.orcinus.overweightfarming.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.orcinus.overweightfarming.util.BlockLeakParticleDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(targets = "net/minecraft/client/particle/BlockLeakParticle$Dripping")
public abstract class DrippingParticleMixin extends Particle implements BlockLeakParticleDuck {
    @Unique
    private ParticleEffect customNextParticle;

    @Inject(method = "updateAge()V", at = @At("HEAD"), cancellable = true)
    public void spawnCustomNextParticle(CallbackInfo ci) {
        if(customNextParticle != null && this.maxAge - 1 <= 0) {
            this.markDead();
            this.world.addParticle(this.customNextParticle, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
            ci.cancel();
        }
    }

    @Override
    public void setNextParticle(ParticleEffect effect) {
        customNextParticle = effect;
    }

    protected DrippingParticleMixin(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
        throw new AssertionError("dummy constructor called");
    }
}