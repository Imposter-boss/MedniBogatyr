package com.example.mixin;
import com.example.maxim.CleanMaximHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void preventTargetingMaxim(LivingEntity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity player && CleanMaximHandler.isCleanMaxim(player)) {
            ci.cancel();
        }
    }
}