package com.nisovin.shopkeepers.container.protection;

/**
 * The kind of action that a container protection is checked for.
 */
public enum ContainerProtectionCheck {

	/**
	 * Checks all container protections.
	 */
	DEFAULT,
	/**
	 * Only checks the protections that are relevant for accessing a container.
	 * <p>
	 * For example: Unlike for normally owned shops, owners and members of hired player shops can
	 * only access the shop containers, but are restricted from deleting or otherwise messing with
	 * the shop containers (blocked by {@link #DEFAULT}).
	 */
	ACCESS
}
