package com.example;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

public class ExampleMod implements ModInitializer {

    public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.createPersistent(
            ResourceLocation.fromNamespaceAndPath("bogatyr", "class"), Codec.STRING
    );

    private static final String[] CLASSES = {"COPPER_BOGATYR", "DONKEY_RIDER", "MAXIM", "SENATOR"};
    private static final Random RANDOM = new Random();

    private static final ResourceLocation HEALTH_MOD_ID = ResourceLocation.fromNamespaceAndPath("bogatyr", "health_bonus");
    private static final ResourceLocation DAMAGE_MOD_ID = ResourceLocation.fromNamespaceAndPath("bogatyr", "damage_bonus");
    private static final ResourceLocation SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath("bogatyr", "speed_bonus");

    @Override
    public void onInitialize() {
        registerEvents();
        registerCommands();
    }

    private void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            String currentClass = player.getAttachedOrCreate(PLAYER_CLASS, () -> "NONE");

            if ("NONE".equals(currentClass)) {
                currentClass = CLASSES[RANDOM.nextInt(CLASSES.length)];
                player.setAttached(PLAYER_CLASS, currentClass);
                
                player.sendSystemMessage(Component.literal("Твой класс: " + currentClass)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                
                applyStaticModifiers(player, currentClass);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String playerClass = player.getAttachedOrCreate(PLAYER_CLASS, () -> "NONE");

                switch (playerClass) {
                    case "COPPER_BOGATYR" -> {
                        if (player.tickCount % 1200 == 0) {
                            player.getInventory().placeItemBackInInventory(new ItemStack(Items.COPPER_INGOT));
                        }
                    }
                    case "MAXIM" -> {
                        if (player.tickCount % 12000 == 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
                        }
                        if (player.tickCount % 20 == 0) {
                            List<ServerPlayer> nearby = player.level().getEntitiesOfClass(
                                    ServerPlayer.class, 
                                    player.getBoundingBox().inflate(4.0), 
                                    p -> p != player
                            );
                            for (ServerPlayer target : nearby) {
                                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                            }
                        }
                    }
                    case "DONKEY_RIDER" -> {
                        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                        if (speedAttr != null) {
                            if (player.getVehicle() instanceof Donkey donkey) {
                                if (speedAttr.hasModifier(SPEED_MOD_ID)) {
                                    speedAttr.removeModifier(SPEED_MOD_ID);
                                }
                                donkey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
                            } else {
                                if (!speedAttr.hasModifier(SPEED_MOD_ID)) {
                                    speedAttr.addTransientModifier(new AttributeModifier(SPEED_MOD_ID, -0.05, AttributeModifier.Operation.ADD_VALUE));
                                }
                            }
                        }
                    }
                }
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayer serverPlayer) {
                String pClass = serverPlayer.getAttachedOrCreate(PLAYER_CLASS, () -> "NONE");
                if ("DONKEY_RIDER".equals(pClass) && entity instanceof Donkey donkey) {
                    if (!donkey.isTamed()) {
                        donkey.tameWithName(player);
                        player.sendSystemMessage(Component.literal("Осёл послушно склонил голову.")
                                .withStyle(ChatFormatting.YELLOW));
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("bogatyr")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String pClass = player.getAttachedOrCreate(PLAYER_CLASS, () -> "NONE");
                        player.sendSystemMessage(Component.literal("Твой текущий класс: " + pClass)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                        return 1;
                    })
            );
        });
    }

    private void applyStaticModifiers(ServerPlayer player, String playerClass) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (health != null) health.removeModifier(HEALTH_MOD_ID);
        if (damage != null) damage.removeModifier(DAMAGE_MOD_ID);
        if (speed != null) speed.removeModifier(SPEED_MOD_ID);

        switch (playerClass) {
            case "COPPER_BOGATYR" -> {
                if (health != null) health.addPermanentModifier(new AttributeModifier(HEALTH_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
            }
            case "SENATOR" -> {
                if (health != null) health.addPermanentModifier(new AttributeModifier(HEALTH_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
                if (damage != null) damage.addPermanentModifier(new AttributeModifier(DAMAGE_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
                if (speed != null) speed.addPermanentModifier(new AttributeModifier(SPEED_MOD_ID, -0.05, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        player.setHealth(player.getMaxHealth());
    }
}
