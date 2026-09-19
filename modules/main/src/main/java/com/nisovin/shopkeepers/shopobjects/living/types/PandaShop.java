package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Panda;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

// TODO Pose (laying, sitting, eating, worried).
public class PandaShop extends BabyableShop<Panda> {

	public static final Property<Panda.Gene> GENE = new BasicProperty<Panda.Gene>()
			.dataKeyAccessor("gene", EnumSerializers.lenient(Panda.Gene.class))
			.defaultValue(Panda.Gene.NORMAL)
			.build();

	private final PropertyValue<Panda.Gene> geneProperty = new PropertyValue<>(GENE)
			.onValueChanged(Unsafe.initialized(this)::applyGene)
			.build(properties);

	public PandaShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<PandaShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		geneProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		geneProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyGene();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getGeneEditorButton());
		return editorButtons;
	}

	// GENE

	public Panda.Gene getGene() {
		return geneProperty.getValue();
	}

	public void setGene(Panda.Gene gene) {
		geneProperty.setValue(gene);
	}

	public void cycleGene(boolean backwards) {
		this.setGene(EnumUtils.cycleEnumConstant(Panda.Gene.class, this.getGene(), backwards));
	}

	private void applyGene() {
		Panda entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Panda.Gene gene = this.getGene();
		entity.setMainGene(gene);
		entity.setHiddenGene(gene);
	}

	private ItemStack getGeneEditorItem() {
		ItemStack iconItem = new ItemStack(Material.PANDA_SPAWN_EGG);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonPandaVariant,
				Messages.buttonPandaVariantLore
		);
		return iconItem;
	}

	private Button getGeneEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getGeneEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleGene(backwards);
				return true;
			}
		};
	}
}
