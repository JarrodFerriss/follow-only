package com.followonly;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import javax.inject.Inject;

@PluginDescriptor(
		name = "Follow Only Plugin",
		description = "Restrict independent movement, allow following only if adjacent."
)
public class FollowOnlyPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		// Get local player
		Player localPlayer = client.getLocalPlayer();
		WorldPoint localPlayerPos = localPlayer.getWorldLocation();

		// Prevent left-click and right-click "Walk here"
		if (event.getMenuOption().equals("Walk here")) {
			event.consume(); // Cancel the walk action
			return;
		}

		// Allow "Follow" only if the target player is on the same or adjacent tile
		if (event.getMenuOption().equals("Follow")) {
			Player targetPlayer = getPlayerFromMenu(event);
			if (targetPlayer != null) {
				WorldPoint targetPosition = targetPlayer.getWorldLocation();

				if (isWithinOneTile(localPlayerPos, targetPosition)) {
					return;
				} else {
					sendChatMessage("You are too far away to follow this player.");
				}
			} else {
				// Only check for adjacent players if name is correctly formatted
				targetPlayer = findPlayerByProximityAndName(localPlayerPos, event);
				if (targetPlayer != null) {
					return; // Allow the follow action by not consuming the event
				}
			}
			event.consume(); // Cancel follow if target is not adjacent
			return;
		}

		// Check if it's an attack and allow ranged/magic attacks from a distance
		if (event.getMenuOption().equals("Attack")) {
			// Check if the player is using a ranged or magic weapon
			if (isUsingRangedOrMagicWeapon()) {
				WorldPoint targetPosition = getTargetPosition(event);
				if (targetPosition != null && !isWithinOneTile(localPlayerPos, targetPosition)) {
					sendChatMessage("Attacking from a distance.");
					// Allow the attack from current position without moving closer
					return;
				}
			}
		}

		// General distance check for all interactions with a valid target position
		WorldPoint targetPosition = getTargetPosition(event);
		if (targetPosition != null && !isWithinOneTile(localPlayerPos, targetPosition)) {
			sendChatMessage("You are too far away to interact with this.");
			event.consume(); // Cancel interaction if target is more than one tile away
		}
	}

	private boolean isUsingRangedOrMagicWeapon() {
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null) {
			Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null) {
				int weaponId = weapon.getId();
				return WeaponData.isRangedWeapon(weaponId) || WeaponData.isMagicWeapon(weaponId);
			}
		}
		return false;
	}

	private Player findPlayerByProximityAndName(WorldPoint localPlayerPos, MenuOptionClicked event) {
		// Clean target name
		String targetName = Text.removeTags(event.getMenuTarget()).replaceAll("\\(.*?\\)", "").trim();

		// Find adjacent player by name and proximity
		return client.getPlayers().stream()
				.filter(p -> p.getName().equals(targetName) && isWithinOneTile(localPlayerPos, p.getWorldLocation()))
				.findFirst()
				.orElse(null);
	}

	private WorldPoint getTargetPosition(MenuOptionClicked event) {
		int targetId = event.getId();

		// Check if it's a game object (e.g., doors, chests)
		for (Tile[][] plane : client.getScene().getTiles()) {
			for (Tile[] row : plane) {
				for (Tile tile : row) {
					if (tile != null) {
						// Check each GameObject on the tile
						for (GameObject gameObject : tile.getGameObjects()) {
							if (gameObject != null && gameObject.getId() == targetId) {
								return gameObject.getWorldLocation(); // For GameObjects
							}
						}
						// Check GroundObject on the tile
						GroundObject groundObject = tile.getGroundObject();
						if (groundObject != null && groundObject.getId() == targetId) {
							return groundObject.getWorldLocation(); // For GroundObjects
						}
						// Check WallObject on the tile
						WallObject wallObject = tile.getWallObject();
						if (wallObject != null && wallObject.getId() == targetId) {
							return wallObject.getWorldLocation(); // For WallObjects
						}
						// Check DecorativeObject on the tile
						DecorativeObject decoObject = tile.getDecorativeObject();
						if (decoObject != null && decoObject.getId() == targetId) {
							return decoObject.getWorldLocation(); // For DecorativeObjects
						}
					}
				}
			}
		}

		// Check if it's an NPC
		NPC targetNpc = client.getNpcs().stream()
				.filter(npc -> npc.getIndex() == targetId)
				.findFirst().orElse(null);
		if (targetNpc != null) {
			return targetNpc.getWorldLocation();
		}

		return null; // No valid target position found
	}

	private boolean isWithinOneTile(WorldPoint pos1, WorldPoint pos2) {
		// Check if pos2 is within one tile of pos1
		return pos1.distanceTo(pos2) <= 1;
	}

	private Player getPlayerFromMenu(MenuOptionClicked event) {
		// Clean target name
		String targetName = Text.removeTags(event.getMenuTarget()).replaceAll("\\(.*?\\)", "").trim();

		// Find player based on the event target name, removing any color tags
		return client.getPlayers().stream()
				.filter(p -> p.getName().equals(targetName))
				.findFirst()
				.orElse(null);
	}

	private void sendChatMessage(String message) {
		final String chatMessage = new ChatMessageBuilder()
				.append(message)
				.build();

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
	}
}
