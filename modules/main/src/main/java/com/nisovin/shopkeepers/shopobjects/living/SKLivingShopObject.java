package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Steerable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.items.ItemUpdates;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObject;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.ui.equipmentEditor.EquipmentEditorUI;
import com.nisovin.shopkeepers.ui.equipmentEditor.EquipmentEditorUIState;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.EquipmentUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.inventory.PotionUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKLivingShopObject<E extends LivingEntity>
		extends BaseEntityShopObject<E> implements LivingShopObject {

	public static final Property<LivingShopEquipment> EQUIPMENT = new BasicProperty<LivingShopEquipment>()
			.dataKeyAccessor("equipment", SKLivingShopEquipment.SERIALIZER)
			.defaultValueSupplier(SKLivingShopEquipment::new)
			.omitIfDefault()
			.build();

	private final PropertyValue<LivingShopEquipment> equipmentProperty = new PropertyValue<>(EQUIPMENT)
			.onValueChanged(Unsafe.initialized(this)::onEquipmentPropertyChanged)
			.build(properties);

	protected SKLivingShopObject(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<?> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
		Validate.isTrue(context instanceof LivingShopObjectCreationContext,
				"Expecting LivingShopObjectCreationContext!");
		Validate.isTrue(shopObjectType instanceof SKLivingShopObjectType,
				"Expecting SKLivingShopObjectType!");

		// Setup the equipment changed listener for the initial default value:
		this.setEquipmentChangedListener();
	}

	@Override
	protected LivingShopObjectCreationContext getContext() {
		return (LivingShopObjectCreationContext) context;
	}

	@Override
	public SKLivingShopObjectType<?> getType() {
		return (SKLivingShopObjectType<?>) super.getType();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		equipmentProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		equipmentProperty.save(shopObjectData);
	}

	// ITEM UPDATES

	@Override
	public int updateItems(String logPrefix, @ReadWrite ShopObjectData shopObjectData) {
		int updatedItems = super.updateItems(logPrefix, shopObjectData);
		updatedItems += updateEquipmentItems(logPrefix, shopObjectData);
		return updatedItems;
	}

	private static int updateEquipmentItems(
			String logPrefix,
			@ReadWrite ShopObjectData shopObjectData
	) {
		LivingShopEquipment shopEquipment;
		try {
			shopEquipment = shopObjectData.get(EQUIPMENT);
		} catch (InvalidDataException e) {
			Log.warning(logPrefix + "Failed to load '" + EQUIPMENT.getName() + "'!", e);
			return 0;
		}

		int updatedItems = 0;

		for (EquipmentSlot slot : EquipmentUtils.EQUIPMENT_SLOTS) {
			var item = shopEquipment.getItem(slot);
			if (item == null) continue;

			var newItem = ItemUpdates.updateItem(item);
			if (newItem == item) continue; // Not changed

			Log.debug(DebugOptions.itemUpdates, logPrefix + "Updated equipment item for slot "
					+ slot.name());
			shopEquipment.setItem(slot, newItem);
			updatedItems += 1;
		}

		if (updatedItems > 0) {
			shopObjectData.set(EQUIPMENT, shopEquipment);
		}

		return updatedItems;
	}

	// ACTIVATION

	@Override
	protected void prepareEntity(@NonNull E entity) {
		super.prepareEntity(entity);

		// Clear equipment:
		// Doing this during entity preparation resolves some issue with the equipment not getting
		// cleared (at least not visually).
		EntityEquipment equipment = entity.getEquipment();
		// Currently, there is no type of living entity without equipment. But since the API
		// specifies this as nullable, we check for this here just in case this changes in the
		// future.
		if (equipment != null) {
			equipment.clear();
		}

		// Some entities (e.g. striders) may randomly spawn with a saddle that does not count as
		// equipment:
		// Note: Since MC 1.21.5+, this might no longer be necessary, because the saddle is part of
		// the equipment now. However, we still call this method to account for unexpected
		// differences in future API or Minecraft versions.
		if (entity instanceof Steerable) {
			Steerable steerable = (Steerable) entity;
			steerable.setSaddle(false);
		}
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();

		E entity = Unsafe.assertNonNull(this.getEntity());

		entity.setRemoveWhenFarAway(false);
		entity.setCanPickupItems(false);

		// Disable aging and breeding:
		if (entity instanceof Ageable ageable) {
			ageable.setAdult();
		}

		if (entity instanceof Breedable breedable) {
			breedable.setBreed(false);
			breedable.setAgeLock(true);
		}

		// Prevent raider shopkeepers from participating in nearby raids:
		if (entity instanceof Raider raider) {
			raider.setCanJoinRaid(false);
		}

		this.updatePotionEffects();
		this.applyEquipment();
	}

	@Override
	protected void overwriteAI() {
		super.overwriteAI();

		E entity = Unsafe.assertNonNull(this.getEntity());

		// Setting the entity non-collidable:
		entity.setCollidable(false);
		// TODO Only required to handle the 'look-at-nearby-player' behavior. Maybe replace this
		// with something own?
		Compat.getProvider().overwriteLivingEntityAI(entity);

		// Disable AI (also disables gravity) and replace it with our own handling:
		this.setNoAI(entity);
	}

	protected final void setNoAI(@NonNull E entity) {
		entity.setAI(false);
		// Note on Bukkit's 'isAware' flag added in MC 1.15: Disables the AI logic similarly to
		// NoAI, but the mob can still move when being pushed around or due to gravity.
		// The collidable API has been reworked to actually work now. Together with the isAware flag
		// this could be an alternative to using NoAI and then having to handle gravity on our own.
		// However, for now we prefer using NoAI. This might be safer in regards to potential future
		// issues and also automatically handles other cases, like players pushing entities around
		// by hitting them.

		// Minor optimization: Make sure that Spigot's entity activation range does not keep this
		// entity ticking, because it assumes that it is currently falling:
		// Note: For flying mobs, the EntityAI will reset this flag back to false every few ticks to
		// play their flying animation.
		// TODO This can be removed once Spigot ignores NoAI entities.
		Compat.getProvider().setOnGround(entity, true);
	}

	// TICKING

	@Override
	protected void checkActive() {
		super.checkActive();

		this.updatePotionEffects();
	}

	// AI

	@Override
	public void tickAI() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Unexpected

		// Look at nearby players: Implemented by manually running the vanilla AI goal.
		// In order to compensate for a reduced tick rate, we invoke the AI multiple times.
		// Otherwise, the entity would turn its head more slowly and track the player for an
		// increased duration.
		Compat.getProvider().tickAI(entity, Settings.entityBehaviorTickPeriod);
	}

	// POTION EFFECTS

	/**
	 * The default {@link PotionEffect}s to apply to the spawned entity.
	 * <p>
	 * These effects are added to any newly spawned entity, and periodically re-added if missing or
	 * nearly expired. Any potion effects not included here are prevented from being added to the
	 * spawned entity, and also periodically removed if detected.
	 * <p>
	 * This can for example be used if a certain mob type requires a certain potion effect to
	 * properly function as a shopkeeper.
	 * <p>
	 * This might be called relatively frequently to check if a given effect equals one of the
	 * default effects. It is therefore recommended that this returns a cached collection, instead
	 * of creating a new collection on each invocation.
	 * 
	 * @return an unmodifiable view on the entity's default potion effects, not <code>null</code>
	 */
	protected Collection<? extends PotionEffect> getDefaultPotionEffects() {
		// None by default:
		return Collections.emptySet();
	}

	private void updatePotionEffects() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return;

		Collection<? extends PotionEffect> defaultPotionEffects = this.getDefaultPotionEffects();
		Collection<PotionEffect> activePotionEffects = Unsafe.castNonNull(entity.getActivePotionEffects());

		// Re-add missing and nearly expired default potion effects:
		defaultPotionEffects.forEach(effect -> {
			@Nullable PotionEffect activeEffect = PotionUtils.findIgnoreDuration(activePotionEffects, effect);
			if (activeEffect != null
					&& (activeEffect.getDuration() == PotionEffect.INFINITE_DURATION
							|| activeEffect.getDuration() > CHECK_PERIOD_TICKS)) {
				return;
			}

			if (activeEffect != null) {
				// Remove the nearly expired effect:
				entity.removePotionEffect(effect.getType());
			}

			entity.addPotionEffect(effect);
		});

		// Remove non-default potion effects:
		activePotionEffects.forEach(effect -> {
			// No duration check here: If the effect matches a default effect and is nearly expired,
			// we already replaced it above.
			// Note: Doing these two operations in this order avoids having to refetch or update the
			// activePotionEffects list for the subsequent 'add-missing-default-effects' operation.
			if (PotionUtils.findIgnoreDuration(defaultPotionEffects, effect) != null) {
				return;
			}

			entity.removePotionEffect(effect.getType());
		});
	}

	// EDITOR ACTIONS

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		if (!this.getEditableEquipmentSlots().isEmpty()) {
			editorButtons.add(this.getEquipmentEditorButton());
		}
		return editorButtons;
	}

	// EQUIPMENT

	/**
	 * The editable {@link EquipmentSlot}s.
	 * <p>
	 * Limits which equipment slots can be edited inside the equipment editor. If empty, the
	 * equipment editor button is completely omitted from the shopkeeper editor.
	 * <p>
	 * This only controls whether users can edit the equipment in the editor. Arbitrary equipment
	 * can still be applied programmatically via the API.
	 * <p>
	 * Can be overridden by sub-types.
	 * 
	 * @return An unmodifiable view on the editable equipment slots. Not <code>null</code>, but can
	 *         be empty. The order of the returned slots defines the order in the editor.
	 */
	protected List<? extends EquipmentSlot> getEditableEquipmentSlots() {
		if (Settings.enableAllEquipmentEditorSlots) {
			return EquipmentUtils.EQUIPMENT_SLOTS;
		}

		switch (this.getEntityType().name()) {
		case "PIG": // Dedicated button for saddle
		case "STRIDER": // Dedicated button for saddle
		case "NAUTILUS": // Dedicated buttons for saddle and armor
		case "ZOMBIE_NAUTILUS": // Dedicated buttons for saddle and armor
		case "LLAMA": // Dedicated button for carpet (armor slot)
		case "TRADER_LLAMA": // Dedicated button for carpet (armor slot)
		case "HORSE": // Dedicated button for horse armor (armor slot) and saddle
		case "MULE": // Dedicated button for saddle
		case "DONKEY": // Dedicated button for saddle
		case "CAMEL": // Dedicated button for saddle
		case "CAMEL_HUSK": // Dedicated button for saddle
		case "ZOMBIE_HORSE": // Dedicated button for saddle
		case "SKELETON_HORSE": // Dedicated button for saddle
			return Collections.emptyList();
		case "VINDICATOR": // The main hand item is only visible during a chase.
			return EquipmentUtils.EQUIPMENT_SLOTS_HEAD;
		case "ENDERMAN": // Item in hand is mapped to the carried block
			return EquipmentUtils.EQUIPMENT_SLOTS_MAINHAND;
		default:
			return EquipmentUtils.getSupportedEquipmentSlots(this.getEntityType());
		}
	}

	@Override
	public LivingShopEquipment getEquipment() {
		return equipmentProperty.getValue();
	}

	private void onEquipmentPropertyChanged() {
		this.setEquipmentChangedListener();

		// Apply the new equipment and inform sub-types, but don't forcefully mark the shopkeeper as
		// dirty here: This is already handled by the equipment property itself, if necessary.
		this.onEquipmentChanged();
	}

	private void setEquipmentChangedListener() {
		((SKLivingShopEquipment) this.getEquipment()).setChangedListener(this::handleEquipmentChanged);
	}

	private void handleEquipmentChanged() {
		shopkeeper.markDirty();
		this.onEquipmentChanged();
	}

	// Can be overridden in sub-types.
	protected void onEquipmentChanged() {
		this.applyEquipment();
	}

	private void applyEquipment() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return; // Not spawned

		@Nullable EntityEquipment entityEquipment = entity.getEquipment();
		if (entityEquipment == null) return;

		LivingShopEquipment shopEquipment = this.getEquipment();

		// Iterate over all equipment slots, to also clear any no longer equipped slots:
		for (EquipmentSlot slot : EquipmentUtils.EQUIPMENT_SLOTS) {
			// No item copy required: Setting the equipment copies the item internally.
			@Nullable ItemStack item = ItemUtils.asItemStackOrNull(shopEquipment.getItem(slot));
			this.setEquipment(entityEquipment, slot, item);
		}
	}

	// Can be overridden by sub-types to for example enforce specific equipment, or apply default
	// equipment.
	protected void setEquipment(
			EntityEquipment entityEquipment,
			EquipmentSlot slot,
			@ReadOnly @Nullable ItemStack item
	) {
		assert entityEquipment != null && slot != null;

		@Nullable ItemStack itemToSet = item;

		// We give entities which would usually burn in sunlight an indestructible item as helmet.
		// This results in less EntityCombustEvents that need to be processed.
		// Note: The fire resistance potion effect does not avoid the EntityCombustEvent.
		// Note: Phantoms also burn in sunlight, but setting a helmet has no effect for them.
		EntityType entityType = this.getEntityType();
		if (slot == EquipmentSlot.HEAD
				&& entityType != EntityType.PHANTOM
				&& EntityUtils.burnsInSunlight(entityType)) {
			if (ItemUtils.isEmpty(itemToSet)) {
				// Buttons are unbreakable and small enough to not be visible inside the entity's
				// head (even for their baby variants).
				itemToSet = new ItemStack(Material.STONE_BUTTON);
			} else {
				assert itemToSet != null;
				// Make the given item indestructible.
				// "Destructible": Has max damage, has damage component, and not unbreakable.
				itemToSet = ItemUtils.setUnbreakable(itemToSet.clone());
			}
		}

		// This copies the item internally:
		entityEquipment.setItem(slot, itemToSet);
	}

	private ItemStack getEquipmentEditorItem() {
		return ItemUtils.setDisplayNameAndLore(
				new ItemStack(Material.ARMOR_STAND),
				Messages.buttonEquipment,
				Messages.buttonEquipmentLore
		);
	}

	private Button getEquipmentEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getEquipmentEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				editorView.closeDelayedAndRunTask(() -> {
					openEquipmentEditor(editorView.getPlayer(), false);
				});
				return true;
			}

			@Override
			protected void onActionSuccess(EditorView editorView, InventoryClickEvent clickEvent) {
				// The button only opens the equipment editor: Skip the ShopkeeperEditedEvent and
				// saving.
			}
		};
	}

	@Override
	public boolean openEquipmentEditor(Player player, boolean editAllSlots) {
		var config = new EquipmentEditorUIState(
				editAllSlots ? EquipmentUtils.EQUIPMENT_SLOTS : this.getEditableEquipmentSlots(),
				this.getEquipment().getItems(),
				(equipmentSlot, item) -> {
					this.getEquipment().setItem(equipmentSlot, item);

					// Call shopkeeper edited event:
					Shopkeeper shopkeeper = this.getShopkeeper();
					Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

					// Save:
					shopkeeper.save();
				}
		);
		return EquipmentEditorUI.request(shopkeeper, player, config);
	}
}
