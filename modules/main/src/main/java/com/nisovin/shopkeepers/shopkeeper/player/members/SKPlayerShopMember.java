package com.nisovin.shopkeepers.shopkeeper.player.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopMember;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.user.SKUser;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.value.DataValue;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

public final class SKPlayerShopMember implements PlayerShopMember {

	/**
	 * A {@link PlayerShopMember} with {@link SKUser#EMPTY} and
	 * {@link DefaultPlayerShopAccessLevels#getNone()}.
	 * <p>
	 * This can for example be used as a non-<code>null</code> placeholder for a missing, unknown or
	 * unset member.
	 */
	public static final PlayerShopMember EMPTY
			= new SKPlayerShopMember(SKUser.EMPTY, false, DefaultPlayerShopAccessLevels.NONE());

	private final User user;
	private boolean isOwner;
	private final PlayerShopAccessLevel accessLevel;

	public SKPlayerShopMember(User user, boolean isOwner, PlayerShopAccessLevel accessLevel) {
		Validate.notNull(user, "user is null");
		Validate.notNull(accessLevel, "accessLevel is null");

		this.user = user;
		this.isOwner = isOwner;
		this.accessLevel = accessLevel;
	}

	@Override
	public User getUser() {
		return user;
	}

	public boolean isOwner() {
		return isOwner;
	}

	@Override
	public PlayerShopAccessLevel getAccessLevel() {
		// If full access is disabled: Dynamically downgrade the "full" access level to "container"
		// access.
		// Note: Doing this here instead of during member loading ensures that this downgrading also
		// works on dynamic config updates.
		if (!isOwner
				&& accessLevel == DefaultPlayerShopAccessLevels.FULL()
				&& !Settings.allowMembersWithFullAccess) {
			return DefaultPlayerShopAccessLevels.EDIT();
		}

		return accessLevel;
	}

	@Override
	public String toString() {
		return "SKPlayerShopMember[user=" + user.getName()
				+ ", accessLevel=" + getAccessLevel().getIdentifier() + "]";
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	private static final Property<UUID> UNIQUE_ID = new BasicProperty<UUID>()
			.dataKeyAccessor("uuid", UUIDSerializers.LENIENT)
			.build();
	private static final Property<String> NAME = new BasicProperty<String>()
			.dataKeyAccessor("name", StringSerializers.SCALAR)
			.validator(StringValidators.NON_EMPTY)
			.build();
	private static final Property<PlayerShopAccessLevel> ACCESS_LEVEL = new BasicProperty<PlayerShopAccessLevel>()
			.dataKeyAccessor("access", new DataSerializer<PlayerShopAccessLevel>() {
				@Override
				public @Nullable Object serialize(PlayerShopAccessLevel value) {
					Validate.notNull(value, "value is null");
					return value.getIdentifier();
				}

				@Override
				public PlayerShopAccessLevel deserialize(Object data) throws InvalidDataException {
					String accessLevelId = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
					var registry = SKShopkeepersPlugin.getInstance().getPlayerShopAccessLevelRegistry();
					var accessLevel = registry.get(accessLevelId);
					if (accessLevel == null) {
						throw new InvalidDataException("Unknown player shop access level: "
								+ accessLevelId);
					}
					return accessLevel;
				}
			})
			.build();

	/**
	 * A {@link DataSerializer} for values of type {@link PlayerShopMember}.
	 */
	public static final DataSerializer<PlayerShopMember> SERIALIZER = new DataSerializer<PlayerShopMember>() {
		@Override
		public @Nullable Object serialize(PlayerShopMember value) {
			Validate.notNull(value, "value is null");
			DataContainer memberData = DataContainer.create();
			memberData.set(UNIQUE_ID, value.getUser().getUniqueId());
			memberData.set(NAME, value.getUser().getName());
			memberData.set(ACCESS_LEVEL, value.getAccessLevel());
			return memberData.serialize();
		}

		@Override
		public PlayerShopMember deserialize(Object data) throws InvalidDataException {
			DataContainer memberData = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				var userUniqueId = memberData.get(UNIQUE_ID);
				var userName = memberData.get(NAME);
				var accessLevel = memberData.get(ACCESS_LEVEL);

				var user = SKUser.of(userUniqueId, userName);
				return new SKPlayerShopMember(user, false, accessLevel);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link PlayerShopMember}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<List<? extends PlayerShopMember>> LIST_SERIALIZER
			= new DataSerializer<List<? extends PlayerShopMember>>() {
				@Override
				public @Nullable Object serialize(@ReadOnly List<? extends PlayerShopMember> value) {
					Validate.notNull(value, "value is null");
					DataContainer memberListData = DataContainer.create();
					int id = 1;
					for (PlayerShopMember member : value) {
						Validate.notNull(member, "list of members contains null");
						memberListData.set(String.valueOf(id), SERIALIZER.serialize(member));
						id++;
					}
					return memberListData.serialize();
				}

				@Override
				public List<? extends PlayerShopMember> deserialize(Object data)
						throws InvalidDataException {
					DataContainer memberListData = DataContainerSerializers.DEFAULT.deserialize(data);
					Set<? extends String> keys = memberListData.getKeys();
					List<PlayerShopMember> members = new ArrayList<>(keys.size());
					for (String id : keys) {
						Object memberData = Unsafe.assertNonNull(memberListData.get(id));
						PlayerShopMember member;
						try {
							member = SERIALIZER.deserialize(memberData);
						} catch (InvalidDataException e) {
							throw new InvalidDataException(
									"Invalid shop member " + id + ": " + e.getMessage(),
									e
							);
						}

						// Check for duplicate members:
						var userUuid = member.getUser().getUniqueId();
						for (var otherMember : members) {
							if (otherMember.getUser().getUniqueId().equals(userUuid)) {
								throw new InvalidDataException(
										"Invalid shop member " + id + ": Duplicate member!"
								);
							}
						}

						members.add(member);
					}
					return members;
				}
			};

	public static void saveMembers(
			DataValue dataValue,
			@ReadOnly @Nullable List<? extends PlayerShopMember> members
	) {
		Validate.notNull(dataValue, "dataValue is null");
		if (members == null) {
			dataValue.clear();
			return;
		}

		Object memberListData = LIST_SERIALIZER.serialize(members);
		dataValue.set(memberListData);
	}

	public static List<? extends PlayerShopMember> loadMembers(DataValue dataValue)
			throws InvalidDataException {
		Validate.notNull(dataValue, "dataValue is null");
		Object memberListData = dataValue.get();
		if (memberListData == null) {
			// No data. -> Return an empty list.
			return Collections.emptyList();
		}
		return LIST_SERIALIZER.deserialize(memberListData);
	}
}
