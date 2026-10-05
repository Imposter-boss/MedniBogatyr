package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ExampleMod implements ModInitializer {

	// Класс игрока хранится в данных игрока (сохраняется в мире, переносится при смерти)
	public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath("bogatyr", "class"),
			builder -> builder.persistent(Codec.STRING).copyOnDeath()
	);

	private static final String COPPER = "COPPER_BOGATYR";
	private static final String DONKEY = "DONKEY_RIDER";
	private static final String MAXIM = "MAXIM";
	private static final String SENATOR = "SENATOR";
	private static final String[] CLASSES = {COPPER, DONKEY, MAXIM, SENATOR};

	private static final Random RANDOM = new Random();

	private static final Identifier HEALTH_MOD_ID = Identifier.fromNamespaceAndPath("bogatyr", "health_bonus");
	private static final Identifier DAMAGE_MOD_ID = Identifier.fromNamespaceAndPath("bogatyr", "damage_bonus");
	private static final Identifier SPEED_MOD_ID = Identifier.fromNamespaceAndPath("bogatyr", "speed_bonus");
	private static final Identifier DONKEY_SPEED_ID = Identifier.fromNamespaceAndPath("bogatyr", "donkey_speed");

	// Какого осла сейчас оседлал каждый игрок (чтобы снять бонус скорости с осла)
	private final Map<UUID, Donkey> ridden = new HashMap<>();

	@Override
	public void onInitialize() {
		registerEvents();
		registerCommands();
	}

	private static String getClassOf(ServerPlayer player) {
		return player.getAttached(PLAYER_CLASS);
	}

	private static String displayName(String cls) {
		if (cls == null) return "нет класса";
		return switch (cls) {
			case COPPER -> "Медный богатырь";
			case DONKEY -> "Всадник на белом осле";
			case MAXIM -> "Максим";
			case SENATOR -> "Сенатор";
			default -> cls;
		};
	}

	private static String description(String cls) {
		if (cls == null) return "";
		return switch (cls) {
			case COPPER -> "+10 здоровья, каждую минуту получаешь 1 медный слиток.";
			case DONKEY -> "-10% скорости пешком, +35% скорости верхом на осле. Ослы приручаются сами, без еды.";
			case MAXIM -> "Раз в 10 минут: тошнота, слабость и слепота на 1 минуту. "
					+ "Все игроки в радиусе 4 блоков получают тошноту и отравление.";
			case SENATOR -> "+10 здоровья, +10 урона, но ты медлительный.";
			default -> "";
		};
	}

	private void registerEvents() {
		// Вход в мир: выдаём класс, если его ещё нет, и применяем бонусы
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			String cls = getClassOf(player);
			boolean isNew = false;

			if (cls == null) {
				cls = CLASSES[RANDOM.nextInt(CLASSES.length)];
				player.setAttached(PLAYER_CLASS, cls);
				isNew = true;
			}

			applyStaticModifiers(player, cls, isNew);

			if (isNew) {
				player.sendSystemMessage(Component.literal("Твой класс: " + displayName(cls))
						.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
				player.sendSystemMessage(Component.literal(description(cls))
						.withStyle(ChatFormatting.GRAY));
			}
		});

		// После смерти: класс копируется (copyOnDeath), бонусы применяем заново
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			String cls = getClassOf(newPlayer);
			if (cls != null) {
				applyStaticModifiers(newPlayer, cls, true);
			}
		});

		// Игрок вышел: снимаем бонус скорости с осла
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			releaseDonkey(handler.getPlayer().getUUID());
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				String cls = getClassOf(player);
				if (cls == null) continue;

				int t = player.tickCount;

				switch (cls) {
					case COPPER -> {
						// Каждую минуту (1200 тиков) 1 медный слиток
						if (t > 0 && t % 1200 == 0) {
							player.getInventory().placeItemBackInInventory(new ItemStack(Items.COPPER_INGOT));
						}
					}
					case MAXIM -> {
						// Раз в 10 минут (12000 тиков): тошнота, слабость, слепота на 1 минуту (1200 тиков)
						if (t > 0 && t % 12000 == 0) {
							player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
							player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
							player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
						}
						// Аура: раз в секунду всем игрокам в радиусе 4 блоков
						if (t % 20 == 0) {
							for (ServerPlayer other : server.getPlayerList().getPlayers()) {
								if (other == player || other.isSpectator()) continue;
								if (other.level() != player.level()) continue;
								if (other.distanceToSqr(player) <= 16.0) {
									other.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
									other.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
								}
							}
						}
					}
					case DONKEY -> tickDonkeyRider(player);
					default -> { }
				}
			}
		});

		// Правый клик по ослу: автоприручение без еды. Дальше обычная посадка.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer sp) {
				if (DONKEY.equals(getClassOf(sp)) && entity instanceof Donkey donkey) {
					if (!donkey.isTamed()) {
						donkey.tameWithName(sp);
						sp.sendSystemMessage(Component.literal("Осёл послушно склонил голову.")
								.withStyle(ChatFormatting.YELLOW));
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	private void tickDonkeyRider(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;

		UUID id = player.getUUID();
		Donkey prev = ridden.get(id);

		if (player.getVehicle() instanceof Donkey donkey) {
			// Верхом: с игрока снимаем -10%, ослу даём +35%
			if (speed.hasModifier(SPEED_MOD_ID)) {
				speed.removeModifier(SPEED_MOD_ID);
			}
			if (prev != null && prev != donkey) {
				removeDonkeySpeed(prev);
			}
			AttributeInstance donkeySpeed = donkey.getAttribute(Attributes.MOVEMENT_SPEED);
			if (donkeySpeed != null && !donkeySpeed.hasModifier(DONKEY_SPEED_ID)) {
				donkeySpeed.addTransientModifier(new AttributeModifier(
						DONKEY_SPEED_ID, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
			ridden.put(id, donkey);
		} else {
			// Пешком: -10% скорости, бонус с осла снимаем
			releaseDonkey(id);
			if (!speed.hasModifier(SPEED_MOD_ID)) {
				speed.addTransientModifier(new AttributeModifier(
						SPEED_MOD_ID, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
	}

	private void releaseDonkey(UUID playerId) {
		Donkey prev = ridden.remove(playerId);
		if (prev != null) {
			removeDonkeySpeed(prev);
		}
	}

	private static void removeDonkeySpeed(Donkey donkey) {
		AttributeInstance a = donkey.getAttribute(Attributes.MOVEMENT_SPEED);
		if (a != null && a.hasModifier(DONKEY_SPEED_ID)) {
			a.removeModifier(DONKEY_SPEED_ID);
		}
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Команды в Майнкрафте чувствительны к регистру, поэтому регистрируем оба варианта
			for (String name : new String[]{"bogatyr", "Bogatyr"}) {
				dispatcher.register(Commands.literal(name)
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							String cls = getClassOf(player);
							player.sendSystemMessage(Component.literal("Твой класс: " + displayName(cls))
									.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
							player.sendSystemMessage(Component.literal(description(cls))
									.withStyle(ChatFormatting.GRAY));
							return 1;
						})
				);
			}
		});
	}

	// Постоянные бонусы здоровья и урона, постоянное замедление сенатора
	private void applyStaticModifiers(ServerPlayer player, String cls, boolean fullHeal) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);

		if (health != null) health.removeModifier(HEALTH_MOD_ID);
		if (damage != null) damage.removeModifier(DAMAGE_MOD_ID);
		if (speed != null) speed.removeModifier(SPEED_MOD_ID);

		switch (cls) {
			case COPPER -> {
				if (health != null) {
					health.addPermanentModifier(new AttributeModifier(
							HEALTH_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
				}
			}
			case SENATOR -> {
				if (health != null) {
					health.addPermanentModifier(new AttributeModifier(
							HEALTH_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
				}
				if (damage != null) {
					damage.addPermanentModifier(new AttributeModifier(
							DAMAGE_MOD_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
				}
				if (speed != null) {
					speed.addPermanentModifier(new AttributeModifier(
							SPEED_MOD_ID, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
				}
			}
			default -> { }
		}

		if (fullHeal) {
			player.setHealth(player.getMaxHealth());
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}
}
