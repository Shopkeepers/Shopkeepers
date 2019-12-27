package com.nisovin.shopkeepers.history;

import com.nisovin.shopkeepers.util.Validate;

public class HistoryRequest {

	public static interface Range {

		/**
		 * Gets the index of the first returned record (inclusive), starting at <code>0</code>.
		 * <p>
		 * The range may depend on the actually available number of total entries.
		 * 
		 * @param totalEntries
		 *            the total number of available entries
		 * @return the start index
		 */
		public int getStartIndex(int totalEntries);

		/**
		 * Gets the upper index limit for the last returned record (exclusive), has to be greater than
		 * <code>startIndex</code>.
		 * <p>
		 * The range may depend on the actually available number of total entries.
		 * 
		 * @param totalEntries
		 *            the total number of available entries
		 * @return the end index
		 */
		public int getEndIndex(int totalEntries);

		public static class ExplicitRange implements Range {

			private final int startIndex;
			private final int endIndex;

			/**
			 * Creates a range with explicit bounds.
			 * 
			 * @param startIndex
			 *            index of the first returned record (inclusive), starting at <code>0</code>
			 * @param endIndex
			 *            upper index limit for the last returned record (exclusive), has to be greater than
			 *            <code>startIndex</code>
			 */
			public ExplicitRange(int startIndex, int endIndex) {
				Validate.isTrue(startIndex >= 0, "startIndex cannot be negative!");
				Validate.isTrue(endIndex >= 0, "endIndex cannot be negative!");
				Validate.isTrue(endIndex > startIndex, "endIndex has to be greater than startIndex!");
				this.startIndex = startIndex;
				this.endIndex = endIndex;
			}

			@Override
			public int getStartIndex(int totalEntries) {
				return startIndex;
			}

			@Override
			public int getEndIndex(int totalEntries) {
				return endIndex;
			}

			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append("ExplicitRange [startIndex=");
				builder.append(startIndex);
				builder.append(", endIndex=");
				builder.append(endIndex);
				builder.append("]");
				return builder.toString();
			}
		}

		public static class PageRange implements Range {

			// Gets trimmed to max page
			private final int page;
			private final int entriesPerPage;

			public PageRange(int page, int entriesPerPage) {
				Validate.isTrue(page >= 1, "Page has to be positive!");
				Validate.isTrue(entriesPerPage >= 1, "Entries per page has to be positive!");
				this.page = page;
				this.entriesPerPage = entriesPerPage;
			}

			public int getMaxPage(int totalEntries) {
				return Math.max(1, (int) Math.ceil((double) totalEntries / entriesPerPage));
			}

			public int getActualPage(int totalEntries) {
				int maxPage = this.getMaxPage(totalEntries);
				return Math.max(1, Math.min(page, maxPage));
			}

			@Override
			public int getStartIndex(int totalEntries) {
				int actualPage = this.getActualPage(totalEntries);
				return (actualPage - 1) * entriesPerPage;
			}

			@Override
			public int getEndIndex(int totalEntries) {
				int actualPage = this.getActualPage(totalEntries);
				return actualPage * entriesPerPage;
			}

			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append("PageRange [page=");
				builder.append(page);
				builder.append(", entriesPerPage=");
				builder.append(entriesPerPage);
				builder.append("]");
				return builder.toString();
			}
		}
	}

	public final PlayerSelector playerSelector; // not null
	public final ShopSelector shopSelector; // not null
	public final Range range; // not null

	/**
	 * Creates a {@link HistoryRequest} for logged trades in the specified range matching the given criteria.
	 * 
	 * @param playerSelector
	 *            specifies the involved trading player(s), not <code>null</code>
	 * @param shopSelector
	 *            specifies the involved shop(s), not <code>null</code>
	 * @param startIndex
	 *            index of the first returned record (inclusive), starting at <code>0</code>
	 * @param endIndex
	 *            upper index limit for the last returned record (exclusive), has to be greater than
	 *            <code>startIndex</code>
	 */
	public HistoryRequest(PlayerSelector playerSelector, ShopSelector shopSelector, Range range) {
		Validate.notNull(playerSelector, "PlayerSelector is null!");
		Validate.notNull(shopSelector, "ShopSelector is null!");
		Validate.notNull(range, "Range is null!");
		this.playerSelector = playerSelector;
		this.shopSelector = shopSelector;
		this.range = range;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HistoryRequest [playerSelector=");
		builder.append(playerSelector);
		builder.append(", shopSelector=");
		builder.append(shopSelector);
		builder.append(", range=");
		builder.append(range);
		builder.append("]");
		return builder.toString();
	}
}
