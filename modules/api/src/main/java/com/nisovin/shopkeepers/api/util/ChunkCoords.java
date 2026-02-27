package com.nisovin.shopkeepers.api.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * Stores positional information about a chunk, i.e. its world name and coordinates.
 */
public class ChunkCoords {

	/**
	 * Converts a block coordinate to a chunk coordinate.
	 * 
	 * @param blockCoord
	 *            the block coordinate
	 * @return the chunk coordinate
	 */
	public static int fromBlock(int blockCoord) {
		return blockCoord >> 4;
	}

	/**
	 * Checks if the given {@link Location locations} are located inside the same chunk.
	 * <p>
	 * This returns <code>false</code> if either of the given locations is <code>null</code>.
	 * 
	 * @param location1
	 *            the first location
	 * @param location2
	 *            the second location
	 * @return <code>true</code> if both locations are located within the same chunk
	 */
	public static boolean isSameChunk(@Nullable Location location1, @Nullable Location location2) {
		if (location1 == null || location2 == null) return false;
		World world1 = location1.getWorld();
		World world2 = location2.getWorld();
		if (world1 == null || world2 == null) return false;
		if (!world1.getName().equals(world2.getName())) return false;

		int chunkX1 = fromBlock(location1.getBlockX());
		int chunkX2 = fromBlock(location2.getBlockX());
		if (chunkX1 != chunkX2) return false;

		int chunkZ1 = fromBlock(location1.getBlockZ());
		int chunkZ2 = fromBlock(location2.getBlockZ());
		if (chunkZ1 != chunkZ2) return false;
		return true;
	}

	/**
	 * Checks if the chunk at the given {@link Location} is loaded.
	 * <p>
	 * This returns <code>false</code> if the given location is <code>null</code>.
	 * 
	 * @param location
	 *            the location
	 * @return <code>true</code> if the chunk is loaded
	 */
	public static boolean isChunkLoaded(@Nullable Location location) {
		if (location == null) return false;
		World world;
		try {
			// Can return null if this location contains no world:
			world = location.getWorld();
		} catch (IllegalArgumentException e) {
			// The location's world has been unloaded.
			return false;
		}
		if (world == null) return false;
		return world.isChunkLoaded(
				fromBlock(location.getBlockX()),
				fromBlock(location.getBlockZ())
		);
	}

	/**
	 * Gets a {@link ChunkCoords} for the specified block location.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param blockX
	 *            the block's x coordinate
	 * @param blockZ
	 *            the block's z coordinate
	 * @return the ChunkCoords
	 */
	public static ChunkCoords fromBlock(String worldName, int blockX, int blockZ) {
		return new ChunkCoords(worldName, fromBlock(blockX), fromBlock(blockZ));
	}

	private String worldName; // Not null or empty
	private int chunkX;
	private int chunkZ;

	/**
	 * Creates a new {@link ChunkCoords}.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 * @param chunkX
	 *            the chunk's x coordinate
	 * @param chunkZ
	 *            the chunk's z coordinate
	 */
	public ChunkCoords(String worldName, int chunkX, int chunkZ) {
		Preconditions.checkNotNull(worldName, "worldName is null");
		Preconditions.checkArgument(!worldName.isEmpty(), "worldName is empty");
		this.worldName = worldName;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	/**
	 * Creates a new {@link ChunkCoords}.
	 * 
	 * @param chunk
	 *            the chunk, not <code>null</code>
	 */
	public ChunkCoords(Chunk chunk) {
		this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	/**
	 * Creates a new {@link ChunkCoords}.
	 * 
	 * @param chunkCoords
	 *            the chunk coordinates to copy, not <code>null</code>
	 */
	public ChunkCoords(ChunkCoords chunkCoords) {
		this(chunkCoords.getWorldName(), chunkCoords.getChunkX(), chunkCoords.getChunkZ());
	}

	/**
	 * Creates a new {@link ChunkCoords}.
	 * <p>
	 * The given {@link Location} is expected to provide a {@link World}.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 */
	public ChunkCoords(Location location) {
		this(
				getWorldName(location),
				fromBlock(location.getBlockX()),
				fromBlock(location.getBlockZ())
		);
	}

	private static String getWorldName(Location location) {
		Preconditions.checkNotNull(location, "location is null");
		World world = location.getWorld(); // Throws an exception if the world is no longer loaded
		Preconditions.checkNotNull(world, "location's world is null");
		return Unsafe.assertNonNull(world).getName();
	}

	/**
	 * Creates a new {@link ChunkCoords}.
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 */
	public ChunkCoords(Block block) {
		this(block.getWorld().getName(), fromBlock(block.getX()), fromBlock(block.getZ()));
	}

	/**
	 * Gets the chunk's world name.
	 * 
	 * @return the chunk's world name, not <code>null</code> or empty
	 */
	public String getWorldName() {
		return worldName;
	}

	/**
	 * Sets the world name.
	 * 
	 * @param worldName
	 *            the world name, not <code>null</code> or empty
	 */
	protected void setWorldName(String worldName) {
		Preconditions.checkNotNull(worldName, "worldName is null");
		Preconditions.checkArgument(!worldName.isEmpty(), "worldName is empty");
		this.worldName = worldName;
	}

	/**
	 * Gets the chunk's x-coordinate
	 * 
	 * @return the chunk's x-coordinate
	 */
	public int getChunkX() {
		return chunkX;
	}

	/**
	 * Sets the chunk's x-coordinate
	 * 
	 * @param chunkX
	 *            the chunk's x-coordinate
	 */
	protected void setChunkX(int chunkX) {
		this.chunkX = chunkX;
	}

	/**
	 * Gets the chunk's z-coordinate
	 * 
	 * @return the chunk's z-coordinate
	 */
	public int getChunkZ() {
		return chunkZ;
	}

	/**
	 * Sets the chunk's z-coordinate
	 * 
	 * @param chunkZ
	 *            the chunk's z-coordinate
	 */
	protected void setChunkZ(int chunkZ) {
		this.chunkZ = chunkZ;
	}

	/**
	 * Gets the {@link World} this {@link ChunkCoords} is located in, or <code>null</code> if the
	 * world is not loaded.
	 * 
	 * @return the world, or <code>null</code> if it is not loaded
	 */
	public @Nullable World getWorld() {
		return Bukkit.getWorld(worldName);
	}

	/**
	 * Checks if the chunk is loaded.
	 * 
	 * @return <code>true</code> if loaded
	 */
	public boolean isChunkLoaded() {
		World world = this.getWorld();
		if (world != null) {
			return world.isChunkLoaded(chunkX, chunkZ);
		}
		return false;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldName.hashCode();
		result = prime * result + chunkX;
		result = prime * result + chunkZ;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof ChunkCoords)) return false;
		ChunkCoords other = (ChunkCoords) obj;
		return this.matches(other.worldName, other.chunkX, other.chunkZ);
	}

	/**
	 * Checks if this {@link ChunkCoords} matches the specified world name and chunk coordinates.
	 * 
	 * @param worldName
	 *            the world name
	 * @param chunkX
	 *            the chunk's X-coordinate
	 * @param chunkZ
	 *            the chunk's z-coordinate
	 * @return <code>true</code> if the world name and chunk coordinates match
	 */
	public boolean matches(@Nullable String worldName, int chunkX, int chunkZ) {
		if (this.chunkX != chunkX) return false;
		if (this.chunkZ != chunkZ) return false;
		// Checked last for performance reasons:
		if (!this.worldName.equals(worldName)) return false;
		return true;
	}

	/**
	 * Checks if this {@link ChunkCoords} matches the given {@link Chunk}.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return <code>true</code> if the world name and chunk coordinates match
	 */
	public boolean matches(@Nullable Chunk chunk) {
		if (chunk == null) return false;
		return this.matches(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [worldName=");
		builder.append(worldName);
		builder.append(", chunkX=");
		builder.append(chunkX);
		builder.append(", chunkZ=");
		builder.append(chunkZ);
		builder.append("]");
		return builder.toString();
	}
}
