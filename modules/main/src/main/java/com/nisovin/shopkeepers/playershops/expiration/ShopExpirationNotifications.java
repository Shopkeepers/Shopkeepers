package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DurationSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Tracks the last shop expiration notification threshold each shop member has been notified at.
 */
public class ShopExpirationNotifications {

	/**
	 * A {@link DataSerializer} for values of type {@link ShopExpirationNotifications}.
	 */
	public static final DataSerializer<ShopExpirationNotifications> SERIALIZER
			= new DataSerializer<ShopExpirationNotifications>() {
				@Override
				public @Nullable Object serialize(ShopExpirationNotifications value) {
					Validate.notNull(value, "value is null");
					DataContainer notificationsData = DataContainer.create();
					value.thresholds.forEach((playerId, threshold) -> {
						notificationsData.set(
								playerId.toString(),
								DurationSerializers.ISO.serialize(threshold)
						);
					});
					return notificationsData.serialize();
				}

				@Override
				public ShopExpirationNotifications deserialize(Object data) throws InvalidDataException {
					var notifications = new ShopExpirationNotifications();

					var notificationsData = DataContainerSerializers.DEFAULT.deserialize(data);
					for (String playerIdKey : notificationsData.getKeys()) {
						UUID playerId;
						try {
							playerId = UUIDSerializers.LENIENT.deserialize(playerIdKey);
						} catch (InvalidDataException e) {
							throw new InvalidDataException(
									"Invalid expiration notification player: " + e.getMessage(),
									e
							);
						}

						Object thresholdData = Unsafe.assertNonNull(notificationsData.get(playerIdKey));
						Duration threshold;
						try {
							threshold = DurationSerializers.ISO.deserialize(thresholdData);
						} catch (InvalidDataException e) {
							throw new InvalidDataException(
									"Invalid expiration notification threshold for player "
											+ playerIdKey + ": " + e.getMessage(),
									e
							);
						}

						notifications.set(playerId, threshold);
					}

					return notifications;
				}
			};

	// The last notification threshold of each player:
	// Insertion-ordered, so that the data remains stable across serialization.
	private final Map<UUID, Duration> thresholds = new LinkedHashMap<>();

	public ShopExpirationNotifications() {
	}

	/**
	 * Checks whether no player has been notified yet.
	 * 
	 * @return <code>true</code> if there are no notifications
	 */
	public boolean isEmpty() {
		return thresholds.isEmpty();
	}

	/**
	 * Gets the last notification threshold the specified player has been notified at.
	 * 
	 * @param playerId
	 *            the player's unique id, not <code>null</code>
	 * @return the last notification threshold, or <code>null</code> if the player has not been
	 *         notified yet
	 */
	public @Nullable Duration get(UUID playerId) {
		Validate.notNull(playerId, "playerId is null");
		return thresholds.get(playerId);
	}

	/**
	 * Sets the last notification threshold the specified player has been notified at.
	 * 
	 * @param playerId
	 *            the player's unique id, not <code>null</code>
	 * @param threshold
	 *            the notification threshold, or <code>null</code> to clear the last notification
	 *            threshold for the player
	 */
	public void set(UUID playerId, @Nullable Duration threshold) {
		Validate.notNull(playerId, "playerId is null");
		if (threshold == null) {
			thresholds.remove(playerId);
		} else {
			thresholds.put(playerId, threshold);
		}
	}

	/**
	 * Removes all notifications.
	 */
	public void clear() {
		thresholds.clear();
	}

	/**
	 * Removes the notifications of all players that match the given filter.
	 * 
	 * @param filter
	 *            the filter, not <code>null</code>
	 * @return <code>true</code> if any notifications were removed
	 */
	public boolean removeIf(Predicate<? super UUID> filter) {
		Validate.notNull(filter, "filter is null");
		return thresholds.keySet().removeIf(filter);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + thresholds.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof ShopExpirationNotifications)) return false;
		ShopExpirationNotifications other = (ShopExpirationNotifications) obj;
		if (!thresholds.equals(other.thresholds)) return false;
		return true;
	}
}
