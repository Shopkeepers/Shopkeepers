package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link NamespacedKey} values.
 */
public final class NamespacedKeySerializers {

	private static abstract class NamespacedKeySerializer implements DataSerializer<@NonNull NamespacedKey> {

		@Override
		public @Nullable Object serialize(NamespacedKey value) {
			Validate.notNull(value, "value is null");
			return value.toString();
		}
	}

	/**
	 * A {@link DataSerializer} for {@link NamespacedKey} values.
	 */
	public static final DataSerializer<@NonNull NamespacedKey> DEFAULT = new NamespacedKeySerializer() {
		@Override
		public NamespacedKey deserialize(Object data) throws InvalidDataException {
			String keyString = StringSerializers.STRICT.deserialize(data);
			// NamespacedKeyUtils.parse instead of NamespacedKey.fromString to also accept old
			// uppercase enum names and interpret them as namespaced keys.
			@Nullable NamespacedKey key = NamespacedKeyUtils.parse(keyString);
			if (key == null) {
				throw new InvalidDataException("Invalid namespaced key: '" + keyString + "'");
			}
			return key;
		}
	};

	private NamespacedKeySerializers() {
	}
}
