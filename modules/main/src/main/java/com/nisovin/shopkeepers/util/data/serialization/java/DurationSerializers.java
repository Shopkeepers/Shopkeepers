package com.nisovin.shopkeepers.util.data.serialization.java;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Duration} values.
 */
public final class DurationSerializers {

	/**
	 * A {@link DataSerializer} for {@link Duration} values.
	 * <p>
	 * This uses the ISO-8601 seconds-based representation (for example {@code "PT10M"}) to
	 * represent and reconstruct the duration.
	 */
	public static final DataSerializer<Duration> ISO = new DataSerializer<Duration>() {
		@Override
		public @Nullable Object serialize(Duration value) {
			Validate.notNull(value, "value is null");
			return value.toString();
		}

		@Override
		public Duration deserialize(Object data) throws InvalidDataException {
			String durationString = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
			try {
				return Duration.parse(durationString);
			} catch (DateTimeParseException e) {
				throw new InvalidDataException("Failed to parse duration from '" + durationString
						+ "'!", e);
			}
		}
	};

	private DurationSerializers() {
	}
}
