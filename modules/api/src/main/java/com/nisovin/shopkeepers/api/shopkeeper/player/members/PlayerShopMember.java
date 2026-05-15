package com.nisovin.shopkeepers.api.shopkeeper.player.members;

import com.nisovin.shopkeepers.api.user.User;

/**
 * Information about a player shop member.
 * <p>
 * This may be a snapshot of a shop member's properties at a particular time. Do not assume that
 * this information is dynamically updated. For example, any dynamic changes to the player's name or
 * access level might not be reflected by this object.
 * <p>
 * Do not compare shop members by their object identity. Instead, compare them by their
 * {@link User#getUniqueId()}.
 */
public interface PlayerShopMember {

	/**
	 * Gets the {@link User}.
	 * 
	 * @return the user
	 */
	public User getUser();

	/**
	 * Gets the {@link PlayerShopAccessLevel}. Not {@link DefaultPlayerShopAccessLevels#getNone()}
	 * for valid members.
	 * 
	 * @return the shop member's access level
	 */
	public PlayerShopAccessLevel getAccessLevel();
}
