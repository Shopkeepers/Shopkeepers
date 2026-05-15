package com.nisovin.shopkeepers.api.shopkeeper.player.members;

import java.util.UUID;

import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.types.Type;

/**
 * An access level a player can have for a particular player shop.
 * <p>
 * See {@link PlayerShopkeeper#getAccessLevel(UUID)} and {@link PlayerShopMember#getAccessLevel()}.
 */
public interface PlayerShopAccessLevel extends Type {
	/**
	 * Checks if this {@link PlayerShopAccessLevel} includes the specified access level.
	 * <p>
	 * For example, {@link DefaultPlayerShopAccessLevels#getEdit()} includes
	 * {@link DefaultPlayerShopAccessLevels#getContainer()}, and
	 * {@link DefaultPlayerShopAccessLevels#getFull()} includes all of the other access levels.
	 * 
	 * @param accessLevel
	 *            the access level to check for
	 * @return <code>true</code> if this permission includes the specified permission
	 */
	public boolean includes(PlayerShopAccessLevel accessLevel);
}
