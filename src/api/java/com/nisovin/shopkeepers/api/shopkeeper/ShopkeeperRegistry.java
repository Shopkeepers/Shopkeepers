package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * Keeps track of all loaded shopkeepers and handles their activation and deactivation.
 */
public interface ShopkeeperRegistry {

	// SHOPKEEPER CREATION

	/**
	 * Creates a shopkeeper from the given creation data and spawns it into the world.
	 * 
	 * @param id
	 *            the shopkeepers id
	 * @param creationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, owner, etc.)
	 *            for creating the shopkeeper
	 * @return the created Shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created
	 */
	public Shopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException;

	/**
	 * Recreates a shopkeeper by loading its previously saved data from the given config section.
	 * 
	 * @param shopType
	 *            the shop type
	 * @param id
	 *            the shopkeeper id
	 * @param configSection
	 *            the config section to load the shopkeeper data from
	 * @return the loaded shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	public Shopkeeper loadShopkeeper(ShopType<?> shopType, int id, ConfigurationSection configSection) throws ShopkeeperCreateException;

	// QUERYING

	/**
	 * Gets all shopkeepers.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends Shopkeeper> getAllShopkeepers();

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getUniqueId() unique id}.
	 * 
	 * @param shopkeeperUniqueId
	 *            the shopkeeper's unique id
	 * @return the shopkeeper for the given unique id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId);

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getId() id}.
	 * 
	 * @param shopkeeperId
	 *            the shopkeeper's id
	 * @return the shopkeeper for the given id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperById(int shopkeeperId);

	// VIRTUAL SHOPS

	/**
	 * Gets all {@link Shopkeeper#isVirtual() virtual} shopkeepers.
	 * 
	 * @return an unmodifiable view on the virtual shopkeepers
	 */
	public Collection<? extends Shopkeeper> getVirtualShopkeepers();

	// ADMIN SHOPS

	/**
	 * Gets all {@link AdminShopkeeper admin shopkeepers}.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends AdminShopkeeper> getAllAdminShopkeepers();

	// PLAYER SHOPS

	/**
	 * Gets all {@link PlayerShopkeeper player shopkeepers}.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends PlayerShopkeeper> getAllPlayerShopkeepers();

	/**
	 * Gets the {@link PlayerShopkeeper player shopkeepers} owned by the specified player.
	 * 
	 * @param ownerUUID
	 *            the owner uuid
	 * @return an unmodifiable view on the player's shopkeepers
	 */
	public Collection<? extends PlayerShopkeeper> getPlayerShopkeepersByOwner(UUID ownerUUID);

	/**
	 * Gets the {@link PlayerShopkeeper player shopkeepers} owned by the specified {@link User}.
	 * <p>
	 * This behaves just like {@link #getPlayerShopkeepersByOwner(UUID)} but uses the {@link User#getUniqueId() User's
	 * unique id}.
	 * 
	 * @param owner
	 *            the owner
	 * @return an unmodifiable view on the player's shopkeepers
	 */
	public Collection<? extends PlayerShopkeeper> getPlayerShopkeepersByOwner(User owner);

	// BY NAME

	/**
	 * Searches for shopkeepers whose names match the given name.
	 * <p>
	 * The comparison of shop names ignores case, colors and normalizes whitespace.
	 * <p>
	 * Note that shopkeeper names are not unique.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching shopkeepers as required.
	 * 
	 * @param shopName
	 *            the shop name
	 * @return a stream over the matching shopkeepers
	 */
	public Stream<? extends Shopkeeper> getShopkeepersByName(String shopName);

	/**
	 * Searches for shopkeepers whose names start with the specified prefix.
	 * <p>
	 * The comparison of shop names ignores case, colors and normalizes whitespace.
	 * <p>
	 * Note that shopkeeper names are not unique.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching shopkeepers as required.
	 * 
	 * @param shopNamePrefix
	 *            the shop name prefix
	 * @return a stream over the matching shopkeepers
	 */
	public Stream<? extends Shopkeeper> getShopkeepersByNamePrefix(String shopNamePrefix);

	// BY WORLD

	/**
	 * Gets the names of all worlds that contain shopkeepers.
	 * 
	 * @return an unmodifiable view on the world names
	 */
	public Collection<String> getWorldsWithShopkeepers();

	/**
	 * Gets all shopkeepers in the specified world.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	public Collection<? extends Shopkeeper> getShopkeepersInWorld(String worldName);

	/**
	 * Gets all shopkeepers in the specified world grouped by the chunks they are in.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the shopkeepers grouped by chunks
	 */
	public Map<ChunkCoords, ? extends Collection<? extends Shopkeeper>> getShopkeepersByChunks(String worldName);

	// ACTIVE CHUNKS

	/**
	 * Gets the currently active chunks in the specified world.
	 * <p>
	 * Chunks get activated and deactivated when they get loaded and unloaded. During activation, the shopkeepers
	 * located inside the chunk get spawned. However, to not spawn shopkeepers for chunks that stay loaded only briefly,
	 * the activation of chunks may be deferred. Consequently this may not return chunks even if they are currently
	 * already loaded.
	 * 
	 * @param worldName
	 *            the world name
	 * @return the active chunks
	 */
	public Collection<ChunkCoords> getActiveChunks(String worldName);

	/**
	 * Checks if the specified chunk is active.
	 * 
	 * @param chunkCoords
	 *            the chunk
	 * @return <code>true</code> if the chunk is active
	 * @see #getActiveChunks(String)
	 */
	public boolean isChunkActive(ChunkCoords chunkCoords);

	/**
	 * Gets all shopkeepers in currently <b>active chunks</b> in the specified world.
	 * <p>
	 * Note: Since chunk activation may be deferred from chunk loading, this may not return shopkeepers even if their
	 * chunk is currently already loaded.
	 * <p>
	 * Also note that the activation of some {@link ShopObject shop objects} may fail (eg. if spawning fails), while
	 * others may not even need to be activated and rather stay active all the time. This is not reflected by this
	 * method, which only considers the activation state of chunks as a whole. The actual activation state of the
	 * individual {@link ShopObject shop objects} can be determined by {@link ShopObject#isActive()}.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 * @see #getActiveChunks(String)
	 */
	public Collection<? extends Shopkeeper> getShopkeepersInActiveChunks(String worldName);

	// BY CHUNK

	/**
	 * Gets all shopkeepers in the specified chunk.
	 * 
	 * @param chunkCoords
	 *            the chunk
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	public Collection<? extends Shopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords);

	// BY LOCATION

	/**
	 * Gets all shopkeepers at the specified location.
	 * 
	 * @param location
	 *            the location
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	public Collection<? extends Shopkeeper> getShopkeepersAtLocation(Location location);

	// BY SHOP OBJECT

	/**
	 * Gets all active shopkeepers. Some shopkeeper types might be always active (like sign shops),
	 * others are only active as long as the chunk they are in is loaded.
	 * 
	 * @return an unmodifiable view on all active shopkeepers
	 */
	public Collection<? extends Shopkeeper> getActiveShopkeepers();

	public Shopkeeper getActiveShopkeeper(String objectId);

	/**
	 * Gets the shopkeeper that is being represented by the given entity.
	 * 
	 * @param entity
	 *            the entity
	 * @return the shopkeeper, or <code>null</code> if the given entity is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntity(Entity entity);

	/**
	 * Checks if the given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity
	 * @return <code>true</code> if the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity);

	/**
	 * Gets the shopkeeper that is represented by the given block (ex: sign shops).
	 * <p>
	 * In order to get the shopkeepers at a specific location use {@link #getShopkeepersAtLocation(Location)} instead.
	 * 
	 * @param block
	 *            the block
	 * @return the shopkeeper, or <code>null</code> if the given block is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByBlock(Block block);

	/**
	 * Checks if the given block is a Shopkeeper.
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the block is a Shopkeeper
	 */
	public boolean isShopkeeper(Block block);
}
