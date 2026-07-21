package com.nisovin.shopkeepers.storage;

import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Represents the data versions used by the {@link ShopkeeperStorage}.
 * <p>
 * {@link DataVersion} combines the following components:
 * <ul>
 * <li>Shopkeeper storage version: This version indicates changes that affect the data of all
 * shopkeepers, such as for example changes to the storage format, and that therefore require a full
 * save of all shopkeepers.
 * <li>Shopkeeper data version: This version indicates changes to the data format of individual
 * shopkeepers, such as the renaming, removal, or the addition of attributes for specific types of
 * shopkeepers or shop objects. This version can for example be used to determine required
 * shopkeeper data migrations. Not all shopkeepers might be affected by a certain change, so changes
 * to this version do not necessarily trigger a full save of all shopkeepers.
 * <li>Minecraft data version: This data version is dictated by Minecraft and updates with every
 * server update, even minor ones. Minecraft sometimes updates or extends the data or data format of
 * game elements such as item stacks. These changes also affect the data of stored shopkeepers,
 * because shopkeepers can store game elements such as item stacks, for example as part of their
 * trade offers.
 * <p>
 * In order to avoid repeated data migrations, and to not end with up with very old save data whose
 * migration might not be supported by future versions of the Minecraft server or the Shopkeepers
 * plugin, the {@link ShopkeeperStorage} may trigger a full migration and save of all shopkeeper
 * data whenever the Minecraft data version has changed.
 * </ul>
 * <p>
 * Alternatively, a {@link DataVersion} can also represent a special state in form of a "named" data
 * version. An example is the {@link #MISSING "missing"} data version that represents the state of
 * no data version being available. Named data versions are {@link #isEmpty() empty}, i.e. all their
 * version components are <code>0</code>. They only provide a {@link #getName() name}.
 * <p>
 * To compactly represent, store, or compare a {@link DataVersion}, one can use its
 * {@link #toString() String representation}, which combines the individual version components. For
 * {@link #isEmpty() empty} data versions, the String representation only contains the data
 * version's {@link #getName() name}. Use {@link #parse(String)} to reconstruct a
 * {@link DataVersion} from such a String.
 */
public final class DataVersion {

	/**
	 * The current shopkeeper storage version.
	 * <p>
	 * Changes to this version trigger a full save of all shopkeepers.
	 */
	private static final int SHOPKEEPER_STORAGE_VERSION = 4;
	/**
	 * The current shopkeeper data version.
	 * <p>
	 * Changes to this version indicate that the data format of individual types of shopkeepers or
	 * shop objects might have changed. But they do not trigger a full save of all shopkeepers.
	 */
	private static final int SHOPKEEPER_DATA_VERSION = 3;

	private static @Nullable DataVersion current = null;

	/**
	 * Gets the current {@link DataVersion}.
	 * 
	 * @return the current data version, not <code>null</code>
	 */
	public static DataVersion current() {
		Validate.State.notNull(current, "Not yet initialized!");
		return Unsafe.assertNonNull(current);
	}

	/**
	 * Performs an initial setup of this {@link DataVersion} class.
	 * <p>
	 * This needs to be called early during plugin initialization.
	 */
	public static void init() {
		if (current != null) return; // Already initialized

		// This call can fail and then (intentionally) cause the plugin initialization to fail:
		current = new DataVersion(
				SHOPKEEPER_STORAGE_VERSION,
				SHOPKEEPER_DATA_VERSION,
				getCurrentMinecraftDataVersion()
		);
	}

	private static int getCurrentMinecraftDataVersion() {
		try {
			return ServerUtils.getDataVersion();
		} catch (Exception e) {
			// This case can for example be reached when the plugin runs on an unsupported type of
			// server, or when this is called in an unsupported context, such as for example during
			// tests when no actual server is running.
			throw new IllegalStateException(
					"Could not retrieve the server's current Minecraft data version!",
					e
			);
		}
	}

	/**
	 * Represents a "missing" data version.
	 */
	public static final DataVersion MISSING = new DataVersion("missing");

	private static final String SEPARATOR = "|";
	private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR, Pattern.LITERAL);

	private static final String NAMED_START = "<";
	private static final String NAMED_END = "<";

	/**
	 * Parses a {@link DataVersion} from the given String representation.
	 * <p>
	 * If the given String is <code>null</code> or empty, this returns {@link #MISSING}.
	 * 
	 * @param dataVersionString
	 *            the String representation, can be <code>null</code>
	 * @return the {@link DataVersion}, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the data version cannot be parsed
	 */
	public static DataVersion parse(@Nullable String dataVersionString) {
		// Check if the data version is missing:
		if (StringUtils.isEmpty(dataVersionString)) {
			return MISSING;
		}
		assert dataVersionString != null;

		// Check if the data version is a named data version:
		if (dataVersionString.startsWith(NAMED_START)) {
			Validate.isTrue(dataVersionString.endsWith(NAMED_END),
					() -> "Invalid data version: " + dataVersionString);
			String name = dataVersionString.substring(
					NAMED_START.length(),
					dataVersionString.length() - NAMED_END.length()
			);
			// The constructor applies additional validation and normalization:
			try {
				return new DataVersion(name);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid data version: " + dataVersionString, e);
			}
		}

		// Normal compound data version:
		// Legacy data versions only have 2 components: Shopkeeper storage and Minecraft data
		// version.
		@NonNull String[] components = SEPARATOR_PATTERN.split(dataVersionString);
		boolean legacy = (components.length == 2);
		Validate.isTrue(components.length == 3 || legacy,
				() -> "Invalid data version: " + dataVersionString);

		String skStorageVersionString = components[0];
		String skDataVersionString = legacy ? "0" : components[1];
		String mcDataVersionString = legacy ? components[1] : components[2];

		int skStorageVersion = parseVersionComponent(skStorageVersionString, dataVersionString);
		int skDataVersion = parseVersionComponent(skDataVersionString, dataVersionString);
		int mcDataVersion = parseVersionComponent(mcDataVersionString, dataVersionString);

		// The constructor applies additional validation:
		try {
			return new DataVersion(skStorageVersion, skDataVersion, mcDataVersion, legacy);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid data version: " + dataVersionString, e);
		}
	}

	private static int parseVersionComponent(String versionString, String dataVersionString) {
		Integer version = ConversionUtils.parseInt(versionString);
		Validate.notNull(version, () -> "Invalid data version: " + dataVersionString);
		return Unsafe.assertNonNull(version);
	}

	/////

	private final int shopkeeperStorageVersion;
	private final int shopkeeperDataVersion;
	private final int minecraftDataVersion;
	private final String name;

	public DataVersion(String name) {
		Validate.notEmpty(name, "name is null or empty");
		// This also catches most types of newlines:
		Validate.isTrue(!StringUtils.containsWhitespace(name), "name contains whitespace");
		this.shopkeeperStorageVersion = 0;
		this.shopkeeperDataVersion = 0;
		this.minecraftDataVersion = 0;
		this.name = StringUtils.normalize(name);
	}

	public DataVersion(
			int shopkeeperStorageVersion,
			int shopkeeperDataVersion,
			int minecraftDataVersion
	) {
		this(shopkeeperStorageVersion, shopkeeperDataVersion, minecraftDataVersion, false);
	}

	// Legacy data versions did not yet support the shopkeeperDataVersion. They use a value of 0 for
	// this version component, and their name and String representation omit it.
	private DataVersion(
			int shopkeeperStorageVersion,
			int shopkeeperDataVersion,
			int minecraftDataVersion,
			boolean legacy
	) {
		Validate.isTrue(shopkeeperStorageVersion > 0, "shopkeeperStorageVersion <= 0");
		if (legacy) {
			Validate.isTrue(shopkeeperDataVersion == 0, "legacy but shopkeeperDataVersion != 0");
		} else {
			Validate.isTrue(shopkeeperDataVersion > 0, "shopkeeperDataVersion <= 0");
		}
		Validate.isTrue(minecraftDataVersion > 0, "minecraftDataVersion <= 0");
		this.shopkeeperStorageVersion = shopkeeperStorageVersion;
		this.shopkeeperDataVersion = shopkeeperDataVersion;
		this.minecraftDataVersion = minecraftDataVersion;
		// The name combines the individual version components:
		this.name = shopkeeperStorageVersion
				+ (legacy ? "" : (SEPARATOR + shopkeeperDataVersion))
				+ SEPARATOR + minecraftDataVersion;
	}

	/**
	 * Gets the shopkeeper storage version.
	 * 
	 * @return the shopkeepers storage version, or <code>0</code> if not available
	 */
	public int getShopkeeperStorageVersion() {
		return shopkeeperStorageVersion;
	}

	/**
	 * Gets the shopkeeper data version.
	 * 
	 * @return the shopkeepers data version, or <code>0</code> if not available
	 */
	public int getShopkeeperDataVersion() {
		return shopkeeperDataVersion;
	}

	/**
	 * Gets the Minecraft data version.
	 * 
	 * @return the Minecraft data version, or <code>0</code> if not available
	 */
	public int getMinecraftDataVersion() {
		return minecraftDataVersion;
	}

	/**
	 * Checks if this {@link DataVersion} is empty.
	 * <p>
	 * A {@link DataVersion} is empty if all its version components are <code>0</code>. An empty
	 * data version only provides a {@link #getName() name}.
	 * <p>
	 * An example for an empty data version is the {@link #MISSING "missing"} data version that
	 * represents the state of no data version being available.
	 * 
	 * @return <code>true</code> if this data version is empty
	 */
	public boolean isEmpty() {
		return shopkeeperStorageVersion == 0
				&& shopkeeperDataVersion == 0
				&& minecraftDataVersion == 0;
	}

	/**
	 * Gets the name of this data version.
	 * <p>
	 * For {@link #isEmpty() non-empty} data versions this matches the {@link #toString() String
	 * representation}.
	 * 
	 * @return the name, not <code>null</code> or empty
	 */
	public String getName() {
		return name;
	}

	private boolean isVersionDowngrade(int thisVersion, DataVersion previous, int previousVersion) {
		assert previous != null;
		if (this.equals(MISSING)) {
			return !previous.isEmpty();
		}
		if (previous.isEmpty() || this.isEmpty()) return false;
		return (thisVersion < previousVersion);
	}

	/**
	 * Checks if this data version represents a shopkeeper storage downgrade compared to the given
	 * previous data version.
	 * <p>
	 * The {@link #MISSING} data version is considered to represent a downgrade compared to all
	 * other {@link #isEmpty() non-empty} data versions. Consequently, this returns
	 * <code>true</code> if this data version equals {@link #MISSING} and the given previous data
	 * version is not empty. Otherwise, if this or the given previous data version is
	 * {@link #isEmpty() empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a shopkeeper storage downgrade
	 *         compared to the given data version
	 */
	public boolean isShopkeeperStorageDowngrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return this.isVersionDowngrade(
				this.getShopkeeperStorageVersion(),
				previous,
				previous.getShopkeeperStorageVersion()
		);
	}

	/**
	 * Checks if this data version represents a shopkeeper storage upgrade compared to the given
	 * previous data version.
	 * <p>
	 * All {@link #isEmpty() non-empty} data versions are considered to represent an upgrade
	 * compared to the {@link #MISSING} data version. Consequently, this returns <code>true</code>
	 * if this data version is not empty and the given previous data version equals
	 * {@link #MISSING}. Otherwise, if this or the given previous data version is {@link #isEmpty()
	 * empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a shopkeeper storage upgrade
	 *         compared to the given data version
	 */
	public boolean isShopkeeperStorageUpgrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return previous.isShopkeeperStorageDowngrade(this);
	}

	/**
	 * Checks if this data version represents a shopkeeper data downgrade compared to the given
	 * previous data version.
	 * <p>
	 * The {@link #MISSING} data version is considered to represent a downgrade compared to all
	 * other {@link #isEmpty() non-empty} data versions. Consequently, this returns
	 * <code>true</code> if this data version equals {@link #MISSING} and the given previous data
	 * version is not empty. Otherwise, if this or the given previous data version is
	 * {@link #isEmpty() empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a shopkeeper data downgrade
	 *         compared to the given data version
	 */
	public boolean isShopkeeperDataDowngrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return this.isVersionDowngrade(
				this.getShopkeeperDataVersion(),
				previous,
				previous.getShopkeeperDataVersion()
		);
	}

	/**
	 * Checks if this data version represents a shopkeeper data upgrade compared to the given
	 * previous data version.
	 * <p>
	 * All {@link #isEmpty() non-empty} data versions are considered to represent an upgrade
	 * compared to the {@link #MISSING} data version. Consequently, this returns <code>true</code>
	 * if this data version is not empty and the given previous data version equals
	 * {@link #MISSING}. Otherwise, if this or the given previous data version is {@link #isEmpty()
	 * empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a shopkeeper data upgrade compared
	 *         to the given data version
	 */
	public boolean isShopkeeperDataUpgrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return previous.isShopkeeperDataDowngrade(this);
	}

	/**
	 * Checks if this data version represents a Minecraft server downgrade compared to the given
	 * previous data version.
	 * <p>
	 * The {@link #MISSING} data version is considered to represent a downgrade compared to all
	 * other {@link #isEmpty() non-empty} data versions. Consequently, this returns
	 * <code>true</code> if this data version equals {@link #MISSING} and the given previous data
	 * version is not empty. Otherwise, if this or the given previous data version is
	 * {@link #isEmpty() empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a Minecraft server downgrade
	 *         compared to the given data version
	 */
	public boolean isMinecraftDowngrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return this.isVersionDowngrade(
				this.getMinecraftDataVersion(),
				previous,
				previous.getMinecraftDataVersion()
		);
	}

	/**
	 * Checks if this data version represents a Minecraft server upgrade compared to the given
	 * previous data version.
	 * <p>
	 * All {@link #isEmpty() non-empty} data versions are considered to represent an upgrade
	 * compared to the {@link #MISSING} data version. Consequently, this returns <code>true</code>
	 * if this data version is not empty and the given previous data version equals
	 * {@link #MISSING}. Otherwise, if this or the given previous data version is {@link #isEmpty()
	 * empty}, this returns <code>false</code>.
	 * 
	 * @param previous
	 *            the previous data version, not <code>null</code>
	 * @return <code>true</code> if this data version represents a Minecraft server upgrade compared
	 *         to the given data version
	 */
	public boolean isMinecraftUpgrade(DataVersion previous) {
		Validate.notNull(previous, "previous is null");
		return previous.isMinecraftDowngrade(this);
	}

	@Override
	public String toString() {
		if (this.isEmpty()) {
			return NAMED_START + name + NAMED_END;
		} else {
			return name;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DataVersion)) return false;
		DataVersion other = (DataVersion) obj;
		if (!name.equals(other.name)) return false;
		return true;
	}
}
