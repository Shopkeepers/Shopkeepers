package com.nisovin.shopkeepers.api.shopkeeper.player.book;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * Sells copies of written books in exchange for currency items.
 * <p>
 * Books are identified solely based on their title: There can be at most one offer for books with a
 * certain title. If there are multiple books with the same title in the shop's containers, the
 * shopkeeper uses only the first book it finds.
 */
public interface BookPlayerShopkeeper extends PlayerShopkeeper {

	// OFFERS

	/**
	 * Gets the offers of this shopkeeper.
	 * <p>
	 * There can be at most one offer for books with a certain title.
	 * 
	 * @return an unmodifiable view on the shopkeeper's offers
	 */
	public List<? extends BookOffer> getOffers();

	/**
	 * Gets the offer for the given book item.
	 * 
	 * @param bookItem
	 *            the book item, not <code>null</code>
	 * @return the offer, or <code>null</code> if there is none for the given item
	 */
	public @Nullable BookOffer getOffer(ItemStack bookItem);

	/**
	 * Gets the offer for the given book item.
	 * 
	 * @param bookItem
	 *            the book item, not <code>null</code>
	 * @return the offer, or <code>null</code> if there is none for the given item
	 */
	public @Nullable BookOffer getOffer(UnmodifiableItemStack bookItem);

	/**
	 * Gets the offer for the book with the specified title.
	 * 
	 * @param bookTitle
	 *            the book title
	 * @return the offer, or <code>null</code> if there is no offer for books with the specified
	 *         title
	 */
	public @Nullable BookOffer getOffer(String bookTitle);

	/**
	 * Removes the offer for a specific book title.
	 * <p>
	 * This has no effect if no such offer exists.
	 * 
	 * @param bookTitle
	 *            the book title
	 */
	public void removeOffer(String bookTitle);

	/**
	 * Clears the shopkeeper's offers.
	 */
	public void clearOffers();

	/**
	 * Sets the shopkeeper's offers.
	 * <p>
	 * This replaces the shopkeeper's previous offers. Duplicate offers for the same book will
	 * replace each other.
	 * 
	 * @param offers
	 *            the new offers
	 */
	public void setOffers(List<? extends BookOffer> offers);

	/**
	 * Adds the given offer to the shopkeeper.
	 * <p>
	 * This will replace any previous offer for the same book.
	 * <p>
	 * The offer gets added to the end of the current offers. If you want to insert, replace or
	 * reorder offers, use {@link #setOffers(List)} instead.
	 * 
	 * @param offer
	 *            the offer to add
	 */
	public void addOffer(BookOffer offer);

	/**
	 * Adds the given offers to the shopkeeper.
	 * <p>
	 * For every offer, this will replace any previous offer for the same book. Duplicate offers for
	 * the same book will replace each other.
	 * <p>
	 * The offers get added to the end of the current offers. If you want to insert, replace or
	 * reorder offers, use {@link #setOffers(List)} instead.
	 * 
	 * @param offers
	 *            the offers to add
	 */
	public void addOffers(List<? extends BookOffer> offers);
}
