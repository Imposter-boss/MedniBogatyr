package com.example.bogatyr;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolItem;

public class CopperBogatyrHandler {
    public static boolean isCopperBogatyr(PlayerEntity player) {
        return player.getScoreboardTags().contains("bogatyr") || player.getName().getString().equalsIgnoreCase("Bogatyr");
    }
    public static void tick(PlayerEntity player) {
        if (!isCopperBogatyr(player)) return;
        ItemStack mainHand = player.getMainHandStack();
        Item item = mainHand.getItem();
        if (item instanceof ToolItem) {
            boolean isCopper = item.getTranslationKey().contains("copper");
            boolean isPickaxe = item instanceof PickaxeItem;
            if (!isCopper && !isPickaxe) {
                player.dropItem(mainHand.copy(), false);
                mainHand.setCount(0);
            }
        }
    }
}