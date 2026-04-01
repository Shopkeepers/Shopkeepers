package com.nisovin.shopkeepers.shopkeeper.activation;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.util.java.Validate;

final class ChunkData {

	private final ChunkCoords chunkCoords;
	// This flag differs from the chunk's current activation state during the processing of a
	// request to de-/activate a chunk and its actual de-/activation. It does NOT indicate a pending
	// delayed activation.
	private boolean shouldBeActive;
	private boolean active;
	// TODO Use one task (or a small number of tasks) for all pending delayed chunk activations,
	// instead of one task per chunk?
	private @Nullable WrappedTask delayedActivationTask = null;

	ChunkData(ChunkCoords chunkCoords) {
		Validate.notNull(chunkCoords, "chunkCoords is null");
		this.chunkCoords = chunkCoords;
		// The chunk entry is initialized as active if the chunk is currently loaded:
		this.setActive(chunkCoords.isChunkLoaded());
	}

	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	boolean isShouldBeActive() {
		return shouldBeActive;
	}

	void setShouldBeActive(boolean shouldBeActive) {
		this.shouldBeActive = shouldBeActive;
	}

	public boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
		this.setShouldBeActive(active);
	}

	public boolean isActivationDelayed() {
		return (delayedActivationTask != null);
	}

	void setDelayedActivationTask(@Nullable WrappedTask delayedActivationTask) {
		this.delayedActivationTask = delayedActivationTask;
	}

	void cancelDelayedActivation() {
		if (delayedActivationTask != null) {
			delayedActivationTask.cancel();
			delayedActivationTask = null;
		}
	}

	// Checks if the chunk is loaded, but not yet active or pending activation:
	boolean needsActivation() {
		// Check if the chunk is already active or pending activation (avoids unnecessary
		// isChunkLoaded calls):
		if (this.isActive() || this.isShouldBeActive() || this.isActivationDelayed()) {
			return false;
		}

		// Check if the chunk is loaded:
		return chunkCoords.isChunkLoaded();
	}

	void cleanUp() {
		this.setShouldBeActive(false);
		this.cancelDelayedActivation();
	}
}
