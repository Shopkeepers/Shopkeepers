package com.nisovin.shopkeepers.container.protection;

/**
 * The result of a container protection check.
 */
public enum ContainerProtectionResult {

	/**
	 * The container is not protected, or the player is not affected by the protection.
	 */
	ALLOWED,
	/**
	 * The container is protected and the player has no access to any of the shopkeepers using it.
	 */
	PROTECTED,
	/**
	 * The container is protected and the player would usually have access to it, but the requested
	 * action is disallowed because one or more shopkeepers using the container can be hired.
	 */
	PROTECTED_HIREABLE;

	/**
	 * Checks whether the action is allowed.
	 * 
	 * @return <code>true</code> if the action is allowed
	 */
	public boolean isAllowed() {
		return this == ALLOWED;
	}
}
