package com.nisovin.shopkeepers.types;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.types.TypeRegistry;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractTypeRegistry<T extends AbstractType> implements TypeRegistry<T> {

	private final Map<String, @NonNull T> registeredTypes = new LinkedHashMap<>();
	private final Collection<? extends @NonNull T> registeredTypesView
			= Collections.unmodifiableCollection(registeredTypes.values());

	protected AbstractTypeRegistry() {
	}

	@Override
	public void register(@NonNull T type) {
		Validate.notNull(type, "type is null");
		String identifier = type.getIdentifier();
		assert identifier != null && !identifier.isEmpty();
		Validate.isTrue(!registeredTypes.containsKey(identifier),
				"A " + this.getTypeName() + " with this identifier is already registered: "
						+ identifier);
		registeredTypes.put(identifier, type);
	}

	@Override
	public void registerAll(Collection<? extends @NonNull T> types) {
		Validate.notNull(types, "types is null");
		types.forEach(this::register);
	}

	/**
	 * A name for the type this TypeRegistry is managing.
	 * <p>
	 * Used to print slightly more informative debug messages.
	 * <p>
	 * Examples: 'shop type', 'shop object type', 'UI type', etc.
	 * 
	 * @return a name for the type this class is handling
	 */
	protected abstract String getTypeName();

	@Override
	public Collection<? extends @NonNull T> getRegisteredTypes() {
		return registeredTypesView;
	}

	@Override
	public @Nullable T get(String identifier) {
		return registeredTypes.get(identifier);
	}

	@Override
	public @Nullable T match(String identifier) {
		Validate.notNull(identifier, "identifier is null");
		// Normalizing the identifier beforehand might slightly improve performance of this loop:
		String normalized = StringUtils.normalize(identifier);
		for (T type : registeredTypesView) {
			if (type.matches(normalized)) {
				return type;
			}
		}
		return null;
	}

	@Override
	public void clearAll() {
		registeredTypes.clear();
	}
}
