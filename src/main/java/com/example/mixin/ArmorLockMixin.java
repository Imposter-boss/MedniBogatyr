package com.example.mixin;
import com.example.bogatyr.CopperBogatyrHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class ArmorLockMixin {
    @Inject(method = "getEquippedStack", at = @At("HEAD"))
    private void checkArmorLock(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        // Implementation for locking armor
    }
}