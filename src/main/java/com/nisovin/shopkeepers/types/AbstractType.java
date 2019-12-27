package com.nisovin.shopkeepers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.types.Type;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractType implements Type {

	/**
	 * An unique identifier.
	 * <p>
	 * Make sure that the identifier (and its {@link StringUtils#normalize(String) normalization}) are unique among all
	 * other {@link Type types} of the same context.
	 * <p>
	 * This could for example be used inside save/configuration files, so it should not contain any characters which
	 * could cause problems with that.
	 */
	protected final String identifier; // not null or empty
	protected final List<String> aliases; // unmodifiable, not null, can be empty, normalized
	protected final String permission; // can be null

	protected AbstractType(String identifier) {
		this(identifier, null, null);
	}

	protected AbstractType(String identifier, List<String> aliases) {
		this(identifier, aliases, null);
	}

	protected AbstractType(String identifier, String permission) {
		this(identifier, null, permission);
	}

	protected AbstractType(String identifier, List<String> aliases, String permission) {
		this.identifier = StringUtils.normalize(identifier);
		Validate.notEmpty(this.identifier, "Empty identifier!");
		if (aliases == null || aliases.isEmpty()) {
			this.aliases = Collections.emptyList();
		} else {
			List<String> normalizedAliases = new ArrayList<>(aliases.size());
			for (String alias : aliases) {
				Validate.notEmpty(alias, "Empty alias!");
				normalizedAliases.add(StringUtils.normalize(alias));
			}
			this.aliases = Collections.unmodifiableList(normalizedAliases);
		}
		this.permission = StringUtils.isEmpty(permission) ? null : permission;
	}

	@Override
	public final String getIdentifier() {
		return identifier;
	}

	@Override
	public Collection<String> getAliases() {
		return aliases;
	}

	@Override
	public String getPermission() {
		return permission;
	}

	@Override
	public boolean hasPermission(Player player) {
		return (permission == null || PermissionUtils.hasPermission(player, permission));
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean matches(String identifier) {
		if (StringUtils.isEmpty(identifier)) return false;
		identifier = StringUtils.normalize(identifier);
		if (identifier.equals(this.identifier)) return true;
		if (this.aliases.contains(identifier)) return true;
		String displayName = StringUtils.normalize(this.getDisplayName());
		return identifier.equals(displayName);
	}

	// not overriding equals and hashCode: only the exact same type instance is considered equal
}
