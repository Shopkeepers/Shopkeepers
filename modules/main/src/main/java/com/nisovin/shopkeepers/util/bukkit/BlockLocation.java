package com.nisovin.shopkeepers.util.bukkit;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Combination of a world name with block coordinates.
 * <p>
 * The world name can also be {@link #hasWorldName() unset}.
 * <p>
 * This type is immutable, but has mutable subclasses such as {@link MutableBlockLocation}. Use
 * {@link #immutable()} to get a block location that is guaranteed to be immutable.
 */
public class BlockLocation {

	private static final Vector VECTOR_ZERO = new Vector();
	private static final Vector BLOCK_CENTER_OFFSET = new Vector(0.5D, 0.5D, 0.5D);

	/**
	 * An immutable block location with unset world name and all coordinates being zero.
	 */
	public static final BlockLocation EMPTY = new BlockLocation();

	/**
	 * Creates a new {@link BlockLocation} for the given {@link Block}
	 * 
	 * @param block
	 *            the block, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 */
	public static BlockLocation of(Block block) {
		Validate.notNull(block, "block is null");
		return new BlockLocation(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	/**
	 * Creates a new {@link BlockLocation} for the given {@link Location}.
	 * <p>
	 * If the given location stores no world, this returns a block location with unset world name.
	 * If the given location stores a world, but the world has been unloaded by now, this throws an
	 * exception.
	 * 
	 * @param location
	 *            the location, not <code>null</code>
	 * @return the block location, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the location references a world that has been unloaded by now
	 */
	public static BlockLocation of(Location location) {
		Validate.notNull(location, "location is null");
		// Note: We do not check Location#isWorldLoaded here, because if the location references a
		// world that has been unloaded by now, this is likely an error that we want to inform about
		// via an exception.
		// This throws an exception if the world has been unloaded:
		World world = location.getWorld();
		String worldName = (world != null) ? world.getName() : null;
		return new BlockLocation(
				worldName,
				location.getBlockX(),
				location.getBlockY(),
				location.getBlockZ()
		);
	}

	/**
	 * Converts a precise double coordinate to an integer block coordinate.
	 * 
	 * @param coordinate
	 *            the double coordinate
	 * @return the block coordinate
	 */
	public static int toBlock(double coordinate) {
		return Location.locToBlock(coordinate);
	}

	/////

	private @Nullable String worldName; // Not empty, but can be null
	private int x;
	private int y;
	private int z;

	/**
	 * Creates a new {@link BlockLocation} with unset world name and all coordinates being zero.
	 */
	public BlockLocation() {
		this(null, 0, 0, 0);
	}

	/**
	 * Creates a new {@link BlockLocation} with unset world name.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public BlockLocation(int x, int y, int z) {
		this(null, x, y, z);
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 */
	public BlockLocation(@Nullable String worldName, int x, int y, int z) {
		validateWorldName(worldName);
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Creates a new {@link BlockLocation} with unset world name.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using
	 * {@link #toBlock(double)}.
	 * 
	 * @param x
	 *            the precise x coordinate
	 * @param y
	 *            the precise y coordinate
	 * @param z
	 *            the precise z coordinate
	 */
	public BlockLocation(double x, double y, double z) {
		this(null, x, y, z);
	}

	/**
	 * Creates a new {@link BlockLocation}.
	 * <p>
	 * The given precise coordinates are converted to block coordinates using
	 * {@link #toBlock(double)}.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 * @param x
	 *            the precise x coordinate
	 * @param y
	 *            the precise y coordinate
	 * @param z
	 *            the precise z coordinate
	 */
	public BlockLocation(@Nullable String worldName, double x, double y, double z) {
		this(worldName, toBlock(x), toBlock(y), toBlock(z));
	}

	private static void validateWorldName(@Nullable String worldName) {
		Validate.isTrue(worldName == null || !worldName.isEmpty(), "worldName is empty");
	}

	/**
	 * Gets the world name.
	 * 
	 * @return the non-empty world name, or <code>null</code> if the world name is not set
	 */
	public final @Nullable String getWorldName() {
		return worldName;
	}

	/**
	 * Checks if the world name is set (i.e. is not <code>null</code>).
	 * 
	 * @return <code>true</code> if the world name is set
	 */
	public final boolean hasWorldName() {
		return (worldName != null);
	}

	/**
	 * Sets the world name.
	 * 
	 * @param worldName
	 *            the non-empty world name, or <code>null</code>
	 */
	protected void setWorldName(@Nullable String worldName) {
		validateWorldName(worldName);
		this.worldName = worldName;
	}

	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public final int getX() {
		return x;
	}

	/**
	 * Sets the x coordinate.
	 * 
	 * @param x
	 *            the x coordinate
	 */
	protected void setX(int x) {
		this.x = x;
	}

	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public final int getY() {
		return y;
	}

	/**
	 * Sets the y coordinate.
	 * 
	 * @param y
	 *            the y coordinate
	 */
	protected void setY(int y) {
		this.y = y;
	}

	/**
	 * Gets the z coordinate.
	 * 
	 * @return the block's z coordinate
	 */
	public final int getZ() {
		return z;
	}

	/**
	 * Sets the z coordinate.
	 * 
	 * @param z
	 *            the z coordinate
	 */
	protected void setZ(int z) {
		this.z = z;
	}

	/**
	 * Checks if this location is empty, i.e. if it has no world name and all its coordinates are
	 * zero.
	 * 
	 * @return <code>true</code> if this location is empty
	 */
	public final boolean isEmpty() {
		return !this.hasWorldName() && x == 0 && y == 0 && z == 0;
	}

	/**
	 * Gets the {@link World} this block location is located in.
	 * 
	 * @return the world, or <code>null</code> if the world name of this block location is unset or
	 *         if the world is not loaded currently
	 */
	public final @Nullable World getWorld() {
		if (!this.hasWorldName()) return null;
		return Bukkit.getWorld(Unsafe.assertNonNull(worldName));
	}

	/**
	 * Gets the {@link Block} that corresponds to this location.
	 * 
	 * @return the block, or <code>null</code> if the world name of this block location is unset or
	 *         if the world is not loaded currently
	 */
	public final @Nullable Block getBlock() {
		World world = this.getWorld();
		if (world == null) return null;
		return this.getBlock(world);
	}

	/**
	 * Gets the {@link Block} in the given {@link World} that corresponds to this location.
	 * 
	 * @param world
	 *            the world, not <code>null</code>
	 * @return the block, not <code>null</code>
	 */
	public final Block getBlock(World world) {
		return this.getBlockAtOffset(world, 0, 0, 0);
	}

	/**
	 * Gets the {@link Block} in the given {@link World} that corresponds to this location with the
	 * given offset applied to it.
	 * 
	 * @param world
	 *            the world, not <code>null</code>
	 * @param offsetX
	 *            the x offset
	 * @param offsetY
	 *            the y offset
	 * @param offsetZ
	 *            the z offset
	 * @return the block, not <code>null</code>
	 */
	public final Block getBlockAtOffset(World world, int offsetX, int offsetY, int offsetZ) {
		Validate.notNull(world, "world");
		// Not null (even for coordinates outside the world's bounds):
		return world.getBlockAt(x + offsetX, y + offsetY, z + offsetZ);
	}

	/**
	 * Gets the {@link ChunkCoords} of this location.
	 * 
	 * @return the {@link ChunkCoords}, or <code>null</code> if this location has no
	 *         {@link #hasWorldName() world name}
	 */
	public final @Nullable ChunkCoords getChunkCoords() {
		if (!this.hasWorldName()) return null;
		return ChunkCoords.fromBlock(Unsafe.assertNonNull(worldName), x, z);
	}

	/**
	 * Checks if this {@link BlockLocation} matches the specified world name and coordinates.
	 * 
	 * @param worldName
	 *            the world name
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param z
	 *            the z coordinate
	 * @return <code>true</code> if the world name and coordinates match
	 */
	public final boolean matches(@Nullable String worldName, int x, int y, int z) {
		if (this.x != x) return false;
		if (this.y != y) return false;
		if (this.z != z) return false;
		// Checked last for performance reasons:
		if (!Objects.equals(this.worldName, worldName)) return false;
		return true;
	}

	/**
	 * Checks if this {@link BlockLocation} matches the location of the given {@link Block}
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the locations match
	 */
	public final boolean matches(@Nullable Block block) {
		if (block == null) return false;
		return this.matches(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	/**
	 * Checks if the {@link #getWorldName() world name} of this {@link BlockLocation} matches the
	 * world name of the given other {@link BlockLocation}.
	 * <p>
	 * If both locations specify no world, this returns <code>true</code>. If one of the locations
	 * specifies no world but the other does, this returns <code>false</code>.
	 * 
	 * @param other
	 *            the other {@link BlockLocation} to compare with, not <code>null</code>
	 * @return <code>true</code> if the world names match
	 */
	public final boolean isSameWorld(BlockLocation other) {
		Validate.notNull(other, "other is null");
		return Objects.equals(this.worldName, other.worldName);
	}

	/**
	 * Checks if the {@link #getWorldName() world name} of this {@link BlockLocation} matches the
	 * world name of the given {@link Location}.
	 * <p>
	 * If both locations specify no world, this returns <code>true</code>. If one of the locations
	 * specifies no world but the other does, this returns <code>false</code>.
	 * 
	 * @param location
	 *            the location to compare with, not <code>null</code>
	 * @return <code>true</code> if the world names match
	 * @throws IllegalArgumentException
	 *             if the given location contains a world but it is no longer loaded
	 */
	public final boolean isSameWorld(Location location) {
		Validate.notNull(location, "location is null");
		World world = location.getWorld(); // Throws an exception if the world is no longer loaded
		String otherWorldName = world != null ? world.getName() : null;
		return Objects.equals(this.worldName, otherWorldName);
	}

	/**
	 * Gets the squared distance between this and the given location.
	 * <p>
	 * If the locations are located in different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param location
	 *            the other location, not <code>null</code>
	 * @return the squared distance
	 */
	public final double getDistanceSquared(BlockLocation location) {
		return this.getDistanceSquared(VECTOR_ZERO, location);
	}

	/**
	 * Gets the squared distance between this and the given location.
	 * <p>
	 * If the locations are located in different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param location
	 *            the other location, not <code>null</code>
	 * @return the squared distance
	 */
	public final double getDistanceSquared(Location location) {
		return this.getDistanceSquared(VECTOR_ZERO, location);
	}

	/**
	 * Gets the squared distance between this and the given location, with an offset added to the
	 * coordinates of this location.
	 * <p>
	 * If the locations are located in different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param offset
	 *            the offset to add to the coordinates of this location when calculating the
	 *            distance, not <code>null</code>
	 * @param location
	 *            the other location, not <code>null</code>
	 * @return the squared distance
	 */
	public final double getDistanceSquared(Vector offset, BlockLocation location) {
		Validate.notNull(offset, "offset is null");
		if (!this.isSameWorld(location)) return Double.MAX_VALUE;

		double dx = this.getX() + offset.getX() - location.getX();
		double dy = this.getY() + offset.getY() - location.getY();
		double dz = this.getZ() + offset.getZ() - location.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the squared distance between this and the given location, with an offset added to the
	 * coordinates of this location.
	 * <p>
	 * If the locations are located in different worlds, this returns {@link Double#MAX_VALUE}.
	 * 
	 * @param offset
	 *            the offset to add to the coordinates of this location when calculating the
	 *            distance, not <code>null</code>
	 * @param location
	 *            the other location, not <code>null</code>
	 * @return the squared distance
	 */
	public final double getDistanceSquared(Vector offset, Location location) {
		Validate.notNull(offset, "offset is null");
		if (!this.isSameWorld(location)) return Double.MAX_VALUE;

		double dx = this.getX() + offset.getX() - location.getX();
		double dy = this.getY() + offset.getY() - location.getY();
		double dz = this.getZ() + offset.getZ() - location.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the squared distance between the block center of this block location and the given
	 * location.
	 * 
	 * @param location
	 *            the other location, not <code>null</code>
	 * @return the squared distance
	 */
	public final double getBlockCenterDistanceSquared(Location location) {
		return this.getDistanceSquared(BLOCK_CENTER_OFFSET, location);
	}

	/**
	 * Creates a mutable copy of this block location.
	 * 
	 * @return the mutable copy, not <code>null</code>
	 */
	public final MutableBlockLocation mutableCopy() {
		return new MutableBlockLocation(worldName, x, y, z);
	}

	/**
	 * Gets an immutable instance of this block location.
	 * <p>
	 * If this block location is already immutable, this returns the block location itself.
	 * Otherwise, an immutable copy is returned.
	 * 
	 * @return the immutable block location, not <code>null</code>
	 */
	public BlockLocation immutable() {
		// This type is assumed to be immutable. Mutable subclasses need to override this method.
		return this;
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [worldName=");
		builder.append(worldName);
		builder.append(", x=");
		builder.append(x);
		builder.append(", y=");
		builder.append(y);
		builder.append(", z=");
		builder.append(z);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (worldName != null ? worldName.hashCode() : 0);
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BlockLocation)) return false;
		BlockLocation other = (BlockLocation) obj;
		return this.matches(other.worldName, other.x, other.y, other.z);
	}
}
