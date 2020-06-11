package com.nisovin.shopkeepers.shopobjects;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;

/**
 * Abstract base class for all shop object implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to call {@link Shopkeeper#markDirty()} on every change of data that might need to be persisted.
 * </ul>
 */
public abstract class AbstractShopObject implements ShopObject {

	protected final AbstractShopkeeper shopkeeper; // not null
	private String lastId = null;

	// fresh creation
	protected AbstractShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	@Override
	public abstract AbstractShopObjectType<?> getType();

	public void load(ConfigurationSection configSection) {
	}

	/**
	 * Saves the shop object's data to the specified configuration section.
	 * <p>
	 * Note: The serialization of the inserted data may happen asynchronously, so make sure that this is not a problem
	 * (ex. only insert immutable objects, or always create copies of the data you insert and/or make sure to not modify
	 * the inserted objects).
	 * 
	 * @param configSection
	 *            the config section
	 */
	public void save(ConfigurationSection configSection) {
		configSection.set("type", this.getType().getIdentifier());
	}

	/**
	 * This gets called at the end of shopkeeper construction, when the shopkeeper has been loaded and setup.
	 * <p>
	 * The shopkeeper has not yet been registered at this point!
	 * <p>
	 * This can be used to perform any remaining initial shop object setup.
	 */
	public void setup() {
	}

	// LIFE CYCLE

	/**
	 * This gets called when the {@link ShopObject} is meant to be removed.
	 * <p>
	 * This can for example be used to disable any active components (ex. listeners) for this shop object.
	 */
	public void remove() {
	}

	/**
	 * This gets called when the {@link ShopObject} is meant to be permanently deleted.
	 * <p>
	 * This gets called after {@link #remove()}.
	 * <p>
	 * This can for example be used to cleanup any persistent data corresponding to this shop object.
	 */
	public void delete() {
	}

	// ACTIVATION

	public void onChunkActivation() {
	}

	public void onChunkDeactivation() {
	}

	@Override
	public abstract boolean isActive();

	@Override
	public abstract String getId();

	/**
	 * Gets the object id the shopkeeper is currently stored by inside the shopkeeper registry.
	 * 
	 * @return the object id, or <code>null</code>
	 */
	public final String getLastId() {
		return lastId;
	}

	/**
	 * Sets the object id the shopkeeper is currently stored by inside the shopkeeper registry.
	 * 
	 * @param lastId
	 *            the object id, can be <code>null</code>
	 */
	public final void setLastId(String lastId) {
		this.lastId = lastId; // can be null
	}

	@Override
	public abstract boolean needsSpawning();

	/**
	 * Whether or not this shop object gets despawned right before world saves and respawned afterwards.
	 * 
	 * @return <code>true</code> if this shop object gets despawned during world saves
	 */
	public boolean despawnDuringWorldSaves() {
		return this.needsSpawning();
	}

	@Override
	public abstract boolean spawn();

	@Override
	public abstract void despawn();

	@Override
	public abstract Location getLocation();

	/**
	 * This is periodically called for active shopkeepers.
	 * <p>
	 * It makes sure that everything is still alright with the shop object.<br>
	 * Ex: Attempts to respawn shop entities, teleports them back into place, informs about their removal.
	 * 
	 * @return <code>true</code> to if the shop object might no longer be active or its id has changed
	 */
	public abstract boolean check();

	/**
	 * This is called whenever the {@link PlayerShopkeeper#getOwner() owner} of the corresponding
	 * {@link PlayerShopkeeper} has changed.
	 * TODO: Maybe have a general 'onShopkeeperChanged' callback, which also incorporates the shopkeeper name changes,
	 * any any other shopkeeper data the shop objects might be interested in.
	 */
	public abstract void onShopkeeperOwnerChanged();

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return AbstractShopkeeper.MAX_NAME_LENGTH;
	}

	@Override
	public String prepareName(String name) {
		if (name == null) return null;
		// trim to max name length:
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) name = name.substring(0, lengthLimit);
		return name;
	}

	@Override
	public abstract void setName(String name);

	@Override
	public abstract String getName();

	// EDITOR ACTIONS

	public List<EditorHandler.Button> getEditorButtons() {
		return Collections.emptyList(); // none by default
	}
}
