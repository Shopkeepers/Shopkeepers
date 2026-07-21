package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.book.BookPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.offers.SKBookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.BookItems;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKBookPlayerShopkeeper
		extends AbstractPlayerShopkeeper implements BookPlayerShopkeeper {

	// Contains only one offer for a specific book (book title):
	private final List<BookOffer> offers = new ArrayList<>();
	private final List<? extends BookOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a new and not yet initialized {@link SKBookPlayerShopkeeper}.
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 */
	protected SKBookPlayerShopkeeper() {
	}

	@Override
	protected void setup() {
		this.registerViewProviderIfMissing(DefaultUITypes.EDITOR(), () -> {
			return new BookPlayerShopEditorViewProvider(this);
		});
		this.registerViewProviderIfMissing(DefaultUITypes.TRADING(), () -> {
			return new BookPlayerShopTradingViewProvider(this);
		});
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOffers(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		super.saveDynamicState(shopkeeperData, saveAll);
		this.saveOffers(shopkeeperData);
	}

	// ITEM UPDATES

	@Override
	protected int updateItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		// Note: Book offers have no items to update.
		return super.updateItems(logPrefix, shopkeeperData);
	}

	//

	@Override
	public BookPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BOOK();
	}

	@Override
	public boolean hasTradingRecipes(@Nullable Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(@Nullable Player player) {
		Map<? extends String, ? extends ItemStack> containerBooksByTitle
				= this.getCopyableBooksFromStockContainers();
		boolean hasBlankBooks = this.hasContainerBlankBooks();
		List<? extends BookOffer> offers = this.getOffers();
		List<TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(bookOffer -> {
			String bookTitle = bookOffer.getBookTitle();
			ItemStack bookItem = containerBooksByTitle.get(bookTitle);
			boolean outOfStock = !hasBlankBooks;
			if (bookItem == null) {
				outOfStock = true;
				bookItem = this.createDummyBook(bookTitle);
			} else {
				// Create a copy of the book from the stock containers:
				assert BookItems.isCopyableBook(bookItem);
				bookItem = BookItems.copyBook(bookItem);
			}
			assert bookItem != null;
			// Assert: bookItem is a copy.

			UnmodifiableItemStack unmodifiableBookItem = UnmodifiableItemStack.ofNonNull(bookItem);
			TradingRecipe recipe = this.createSellingRecipe(
					unmodifiableBookItem,
					bookOffer.getPrice(),
					outOfStock
			);
			if (recipe != null) {
				recipes.add(recipe);
			} // Else: Price is invalid (cannot be represented by currency items).
		});
		return Collections.unmodifiableList(recipes);
	}

	/**
	 * Gets the {@link BookItems#isCopyableBook(ItemStack) copyable}
	 * {@link BookItems#isWrittenBook(ItemStack) written book} items from the shopkeeper's stock
	 * containers.
	 * <p>
	 * Book items without title are omitted. If multiple book items share the same title, only the
	 * first encountered book item with that title is returned.
	 * 
	 * @return the book items mapped by their title, or an empty Map if no stock containers are
	 *         found
	 */
	protected Map<? extends String, ? extends ItemStack> getCopyableBooksFromStockContainers() {
		// Linked Map: Preserves the order of encountered items.
		Map<String, ItemStack> booksByTitle = new LinkedHashMap<>();
		// Empty if no stock containers are found:
		@Nullable ItemStack[] stockContainerContents = this.getStockContainerContents();
		for (ItemStack itemStack : stockContainerContents) {
			if (itemStack == null) continue;
			BookMeta bookMeta = BookItems.getBookMeta(itemStack);
			if (bookMeta == null) continue; // Not a written book
			if (!BookItems.isCopyable(bookMeta)) continue;
			String title = BookItems.getTitle(bookMeta);
			if (title == null) continue;

			// The item is ignored if we already encountered another book item with the same title
			// before:
			booksByTitle.putIfAbsent(title, itemStack);
		}
		return booksByTitle;
	}

	/**
	 * Checks if any of the shopkeeper's stock containers contains blank books (i.e. items of type
	 * {@link Material#WRITABLE_BOOK}).
	 * 
	 * @return <code>true</code> if the stock containers contain blank books
	 */
	protected boolean hasContainerBlankBooks() {
		for (var container : this.getContainers()) {
			if (!container.getType().isStock()) {
				continue;
			}

			Inventory containerInventory = container.getInventory();
			if (containerInventory == null) continue; // Container not found

			if (containerInventory.contains(Material.WRITABLE_BOOK)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Creates a dummy book {@link ItemStack} that acts as substitute representation of the book
	 * item with the given title.
	 * <p>
	 * This dummy book item is used as a replacement in the shopkeeper editor and trading interface
	 * if no actual book item with the given title is found in the shopkeeper's containers.
	 * 
	 * @param title
	 *            the book title
	 * @return the dummy book item
	 */
	protected ItemStack createDummyBook(String title) {
		ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bookMeta = Unsafe.castNonNull(bookItem.getItemMeta());
		bookMeta.setTitle(title);
		bookMeta.setAuthor(Messages.unknownBookAuthor);
		bookMeta.setGeneration(Generation.TATTERED);
		bookItem.setItemMeta(bookMeta);
		return bookItem;
	}

	/**
	 * Checks if the given {@link BookMeta} corresponds to a {@link #createDummyBook(String) dummy
	 * book item}.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return <code>true</code> if the book meta corresponds to a dummy book item
	 */
	protected static boolean isDummyBook(@ReadOnly BookMeta bookMeta) {
		assert bookMeta != null;
		Generation generation = BookItems.getGeneration(bookMeta);
		return (generation == Generation.TATTERED);
	}

	// OFFERS

	private static final String DATA_KEY_OFFERS = "offers";
	public static final Property<List<? extends BookOffer>> OFFERS = new BasicProperty<List<? extends BookOffer>>()
			.dataKeyAccessor(DATA_KEY_OFFERS, SKBookOffer.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"book-offers",
				MigrationPhase.ofShopkeeperClass(SKBookPlayerShopkeeper.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				return SKBookOffer.migrateOffers(
						shopkeeperData.getDataValue(DATA_KEY_OFFERS),
						logPrefix
				);
			}
		});
	}

	private void loadOffers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setOffers(shopkeeperData.get(OFFERS));
	}

	private void saveOffers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(OFFERS, this.getOffers());
	}

	@Override
	public List<? extends BookOffer> getOffers() {
		return offersView;
	}

	@Override
	public @Nullable BookOffer getOffer(@ReadOnly ItemStack bookItem) {
		Validate.notNull(bookItem, "bookItem is null");
		String bookTitle = BookItems.getBookTitle(bookItem);
		if (bookTitle == null) return null; // Not a written book, or has no title
		return this.getOffer(bookTitle);
	}

	@Override
	public @Nullable BookOffer getOffer(UnmodifiableItemStack bookItem) {
		Validate.notNull(bookItem, "bookItem is null");
		return this.getOffer(ItemUtils.asItemStack(bookItem));
	}

	@Override
	public @Nullable BookOffer getOffer(String bookTitle) {
		Validate.notNull(bookTitle, "bookTitle is null");
		for (BookOffer offer : this.getOffers()) {
			if (offer.getBookTitle().equals(bookTitle)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public void removeOffer(String bookTitle) {
		Validate.notNull(bookTitle, "bookTitle is null");
		Iterator<BookOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getBookTitle().equals(bookTitle)) {
				iterator.remove();
				this.markDirty();
				break;
			}
		}
	}

	@Override
	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	private void _clearOffers() {
		offers.clear();
	}

	@Override
	public void setOffers(@ReadOnly List<? extends BookOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(@ReadOnly List<? extends BookOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(BookOffer offer) {
		Validate.notNull(offer, "offer is null");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(BookOffer offer) {
		assert offer != null;
		Validate.isTrue(offer instanceof SKBookOffer, "offer is not of type SKBookOffer");
		SKBookOffer skOffer = (SKBookOffer) offer;

		// Remove any previous offer for the same book:
		String bookTitle = offer.getBookTitle();
		this.removeOffer(bookTitle);

		// Add the new offer:
		offers.add(skOffer);
	}

	@Override
	public void addOffers(@ReadOnly List<? extends BookOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(@ReadOnly List<? extends BookOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
		// This replaces any previous offers for the same books:
		offers.forEach(this::_addOffer);
	}
}
