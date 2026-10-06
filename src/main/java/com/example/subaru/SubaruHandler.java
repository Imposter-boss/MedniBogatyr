package com.example.subaru;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import java.util.List;

public class SubaruHandler {
    public static boolean isSubaru(PlayerEntity player) {
        return player.getScoreboardTags().contains("subaru") || player.getName().getString().equalsIgnoreCase("Subaru");
    }
    public static void tickSubaru(PlayerEntity player) {
        if (!isSubaru(player)) return;
        List<StatusEffectInstance> effects = player.getStatusEffects().stream().toList();
        for (StatusEffectInstance effect : effects) {
            player.removeStatusEffect(effect.getEffectType());
        }
    }
}