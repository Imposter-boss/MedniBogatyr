package com.example.maxim;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CleanMaximHandler {
    private static final int SEVEN_MINUTES_TICKS = 7 * 60 * 20;
    private static final Map<UUID, Integer> waterTimers = new HashMap<>();

    public static boolean isCleanMaxim(PlayerEntity player) {
        return player.getScoreboardTags().contains("maxim") || player.getName().getString().equalsIgnoreCase("Maxim");
    }
    public static void tick(PlayerEntity player) {
        if (!isCleanMaxim(player)) return;
        UUID id = player.getUuid();
        int currentTimer = waterTimers.getOrDefault(id, SEVEN_MINUTES_TICKS);

        for (StatusEffectInstance effect : player.getStatusEffects().stream().toList()) {
            if (effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                player.removeStatusEffect(effect.getEffectType());
            }
        }
        Box area = player.getBoundingBox().expand(15.0);
        player.getWorld().getEntitiesByClass(AnimalEntity.class, area, animal -> true).forEach(animal -> {
            animal.getNavigation().startMovingTo(player, 1.25D);
        });

        if (player.isTouchingWater()) {
            waterTimers.put(id, SEVEN_MINUTES_TICKS);
            return;
        }
        currentTimer--;
        waterTimers.put(id, currentTimer);

        if (currentTimer == 200) {
            player.sendMessage(Text.literal("ВНИМАНИЕ: Зайдите в воду! Осталось 10 секунд!").formatted(Formatting.RED, Formatting.BOLD), false);
        }
        if (currentTimer <= 0) {
            if (player.age % 20 == 0) player.damage(player.getDamageSources().drown(), 2.0f);
        }
    }
}