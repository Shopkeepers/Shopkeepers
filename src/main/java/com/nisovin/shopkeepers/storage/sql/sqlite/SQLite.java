package com.nisovin.shopkeepers.storage.sql.sqlite;

import com.nisovin.shopkeepers.storage.sql.SQL;

public class SQLite extends SQL {

	public static final SQLite INSTANCE = new SQLite();

	protected SQLite() {
	}

	@Override
	public String ignore() {
		return "OR IGNORE";
	}

	@Override
	public String autoIncrement() {
		return "AUTOINCREMENT";
	}

	@Override
	public String dateTimeType() {
		return "TEXT"; // stored as text
	}

	@Override
	public String currentTime() {
		return "strftime('%Y-%m-%d %H:%M:%f','now','localtime')";
	}

	@Override
	public String unixTimeMillis(String dateTimeColumn) {
		// 2440587.5 = julianday('1970-01-01')
		// 86400000 = millis per day
		return "CAST((strftime('%J', " + this.quoteId(dateTimeColumn) + ") - 2440587.5)*86400000 AS INTEGER)";
	}

	@Override
	public int getJoinLimit() {
		// https://www.sqlite.org/limits.html
		return 64;
	}

	public class SQLiteTrigger extends Trigger {

		protected SQLiteTrigger() {
			super();
		}

		@Override
		protected String reactionSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("BEGIN").append(' ');
			sb.append(reaction).append(';');
			sb.append(' ').append("END");
			return sb.toString();
		}
	}

	@Override
	public Trigger trigger() {
		return new SQLiteTrigger();
	}

	@Override
	public Trigger triggerLastModified(String eventTable, String reactionTable, String lastModifiedColumn, String idColumn) {
		String reaction = "UPDATE " + quoteId(reactionTable)
				+ " SET " + quoteId(lastModifiedColumn) + "=" + this.currentTime()
				+ " WHERE " + quoteId(idColumn) + "=NEW." + quoteId(idColumn);
		return this.trigger().table(eventTable).name(eventTable + "_lastModified")
				.after().event(TriggerEvent.UPDATE).forEachRow()
				.reaction(reaction);
	}
}
