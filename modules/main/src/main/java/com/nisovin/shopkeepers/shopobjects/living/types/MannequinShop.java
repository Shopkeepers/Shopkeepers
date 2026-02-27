package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.profile.PlayerProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.input.InputRequest;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ConfigSerializableSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

//TODO Use the actual entity type once we only support 1.21.9+
public class MannequinShop extends SKLivingShopObject<LivingEntity> {

	public static final Property<MainHand> MAIN_HAND = new BasicProperty<MainHand>()
			.dataKeyAccessor("mainHand", EnumSerializers.lenient(MainHand.class))
			.useDefaultIfMissing()
			.defaultValue(MainHand.RIGHT)
			.build();

	public static final Property<Pose> POSE = new BasicProperty<Pose>()
			.dataKeyAccessor("pose", EnumSerializers.lenient(Pose.class))
			.validator(pose -> {
				Validate.isTrue(
						EntityUtils.isValidMannequinPose(pose),
						"pose is invalid for mannequin: " + pose.name()
				);
			})
			.useDefaultIfMissing()
			.defaultValue(Pose.STANDING)
			.build();

	public static final Property<@Nullable PlayerProfile> PROFILE = new BasicProperty<@Nullable PlayerProfile>()
			.dataKeyAccessor("profile", ConfigSerializableSerializers.strict(PlayerProfile.class))
			.nullable()
			.build();

	private final PropertyValue<MainHand> mainHandProperty = new PropertyValue<>(MAIN_HAND)
			.onValueChanged(Unsafe.initialized(this)::applyMainHand)
			.build(properties);

	private final PropertyValue<Pose> poseProperty = new PropertyValue<>(POSE)
			.onValueChanged(Unsafe.initialized(this)::applyPose)
			.build(properties);

	private final PropertyValue<@Nullable PlayerProfile> profileProperty = new PropertyValue<>(PROFILE)
			.onValueChanged(Unsafe.initialized(this)::applyProfile)
			.build(properties);

	public MannequinShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<MannequinShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		mainHandProperty.load(shopObjectData);
		poseProperty.load(shopObjectData);
		profileProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		mainHandProperty.save(shopObjectData);
		poseProperty.save(shopObjectData);
		profileProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyDescription();
		this.applyMainHand();
		this.applyPose();
		this.applyProfile();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getMainHandEditorButton());
		editorButtons.add(this.getPoseEditorButton());
		editorButtons.add(this.getProfileEditorButton());
		return editorButtons;
	}

	// DESCRIPTION

	private void applyDescription() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setMannequinHideDescription(entity, true);
	}

	// MAIN HAND

	public MainHand getMainHand() {
		return mainHandProperty.getValue();
	}

	public void setMainHand(MainHand mainHand) {
		mainHandProperty.setValue(mainHand);
	}

	public void cycleMainHand(boolean backwards) {
		this.setMainHand(EnumUtils.cycleEnumConstant(
				MainHand.class,
				this.getMainHand(),
				backwards
		));
	}

	private void applyMainHand() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setMannequinMainHand(entity, this.getMainHand());
	}

	private ItemStack getMainHandEditorItem() {
		ItemStack iconItem = this.getMainHand() == MainHand.LEFT
				? ItemUtils.getSkull_MHF_ArrowLeft()
				: ItemUtils.getSkull_MHF_ArrowRight();
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonMannequinMainHand,
				Messages.buttonMannequinMainHandLore
		);
		return iconItem;
	}

	private Button getMainHandEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getMainHandEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleMainHand(backwards);
				return true;
			}
		};
	}

	// POSE

	public Pose getPose() {
		return poseProperty.getValue();
	}

	public void setPose(Pose pose) {
		poseProperty.setValue(pose);
	}

	public void cyclePose(boolean backwards) {
		this.setPose(EnumUtils.cycleEnumConstant(
				Pose.class,
				this.getPose(),
				backwards,
				EntityUtils::isValidMannequinPose
		));
	}

	private void applyPose() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setMannequinPose(entity, this.getPose());
	}

	private ItemStack getPoseEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_BOOTS);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonMannequinPose,
				Messages.buttonMannequinPoseLore
		);
		return iconItem;
	}

	private Button getPoseEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getPoseEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cyclePose(backwards);
				return true;
			}
		};
	}

	// PROFILE

	public @Nullable PlayerProfile getProfile() {
		return profileProperty.getValue();
	}

	public void setProfile(@Nullable PlayerProfile profile) {
		profileProperty.setValue(profile);
	}

	private void applyProfile() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setMannequinProfile(entity, this.getProfile());
	}

	private ItemStack getProfileEditorItem() {
		ItemStack iconItem = new ItemStack(Material.PLAYER_HEAD);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonMannequinProfile,
				Messages.buttonMannequinProfileLore
		);
		return iconItem;
	}

	private Button getProfileEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return getProfileEditorItem();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				// Also triggers a save:
				editorView.closeDelayed();

				// TODO Allow alternatively specifying the profile name via a nametag item in case
				// chat input does not work on the server.
				var player = editorView.getPlayer();
				var chatInput = SKShopkeepersPlugin.getInstance().getChatInput();
				chatInput.request(player, new InputRequest<String>() {
					@Override
					public void onInput(String input) {
						handleProfileInput(player, input);
					}
				});
				TextUtils.sendMessage(player, Messages.mannequinEnterProfile);
				return true;
			}
		};
	}

	private void handleProfileInput(Player player, String input) {
		// Prepare the input:
		String preparedInput = input.trim();

		if (!shopkeeper.isValid() || preparedInput.isEmpty() || preparedInput.equals("!")) {
			// Abort:
			TextUtils.sendMessage(player, Messages.mannequinEnterProfileCanceled);
			return;
		}

		if (preparedInput.equals("-")) {
			// Clear the profile:
			this.updateProfile(player, preparedInput, null);
			return;
		}

		// Create and apply the new profile:

		// We support both name and uuid lookups:
		var uuid = ConversionUtils.parseUUID(preparedInput);
		if (uuid == null && !Compat.getProvider().getCompatVersion().isPaper()) {
			// TODO Workaround for SPIGOT-8088: Lookup the player id via OfflinePlayer (blocking!)
			var offlinePlayer = Bukkit.getOfflinePlayer(preparedInput);
			uuid = offlinePlayer.getUniqueId(); // Potentially a non-existent offline uuid
		}

		var profile = uuid != null
				// TODO Empty name: Workaround for SPIGOT-8088
				? Bukkit.createPlayerProfile(uuid, "")
				: Bukkit.createPlayerProfile(preparedInput);
		var plugin = SKShopkeepersPlugin.getInstance();
		profile.update().whenComplete((updatedProfile, e) -> {
			if (e != null) {
				// Unexpected: If the lookup "fails", we expect to just get back the incomplete
				// profile. This might be reached if there is some bug in the server.
				Log.debug("Failed to update player profile: " + preparedInput, e);
				return;
			}

			SchedulerUtils.runOnMainThreadOrOmit(player.getLocation(), () -> {
				this.updateProfile(player, preparedInput, updatedProfile);
			});
		});
	}

	private void updateProfile(Player player, String input, @Nullable PlayerProfile profile) {
		if (!shopkeeper.isValid()) {
			TextUtils.sendMessage(player, Messages.mannequinEnterProfileCanceled);
			return;
		}

		// Validate the profile:
		if (profile != null && !profile.isComplete()) {
			TextUtils.sendMessage(player, Messages.mannequinProfileInvalid, "input", input);
			return;
		}

		// Apply the profile:
		this.setProfile(profile);

		// Inform player:
		if (profile == null) {
			TextUtils.sendMessage(player, Messages.mannequinProfileCleared);
		} else {
			var profileName = Unsafe.assertNonNull(profile.getName());
			TextUtils.sendMessage(player, Messages.mannequinProfileSet, "profileName", profileName);
		}

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();
	}
}
