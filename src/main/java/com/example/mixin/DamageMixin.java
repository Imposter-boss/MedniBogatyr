package com.example.mixin;
import com.example.subaru.SubaruHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class DamageMixin {
    @ModifyVariable(method = "attack", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float reduceSubaruDamage(float amount) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (SubaruHandler.isSubaru(player)) {
            return amount / 1.5f;
        }
        return amount;
    }
}