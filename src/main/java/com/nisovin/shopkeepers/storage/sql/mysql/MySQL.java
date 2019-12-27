package com.nisovin.shopkeepers.storage.sql.mysql;

import com.nisovin.shopkeepers.storage.sql.SQL;

public class MySQL extends SQL {

	public static final MySQL INSTANCE = new MySQL();

	protected MySQL() {
	}

	@Override
	public String ignore() {
		return "IGNORE";
	}

	@Override
	public String autoIncrement() {
		return "AUTO_INCREMENT";
	}

	@Override
	public String dateTimeType() {
		return "DATETIME(3)"; // milliseconds precision
	}

	@Override
	public String currentTime() {
		return "NOW(3)"; // milliseconds precision
	}

	@Override
	public String unixTimeMillis(String dateTimeColumn) {
		return "CAST((UNIX_TIMESTAMP(" + quoteId(dateTimeColumn) + ") * 1000) AS UNSIGNED INTEGER)";
	}

	@Override
	public int getJoinLimit() {
		// https://dev.mysql.com/doc/refman/8.0/en/joins-limits.html
		return 61;
	}

	public class MySQLTable extends Table {

		protected MySQLTable(String name) {
			super(name);
		}

		@Override
		public boolean supportsExtra() {
			return true;
		}
	}

	@Override
	public Table table(String name) {
		return new MySQLTable(name);
	}

	public class MySQLView extends View {

		protected MySQLView(String name) {
			super(name);
		}

		@Override
		public String toCreateSQL() {
			StringBuilder sb = new StringBuilder();
			// MySQL doesn't support "IF NOT EXISTS", instead it offers "OR REPLACE"
			sb.append("CREATE OR REPLACE VIEW ").append(this.qn());
			sb.append(selectSQL).append(';');
			return sb.toString();
		}
	}

	@Override
	public View view(String name) {
		return new MySQLView(name);
	}

	public class MySQLIndex extends Index {

		protected MySQLIndex() {
			super();
		}

		@Override
		public boolean supportsIfNotExists() {
			// MySQL doesn't support IF NOT EXISTS; existence needs to be checked separately
			return false;
		}

		// does not support "IF EXISTS"; existence needs to be checked separately
		@Override
		public String toDropSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("DROP INDEX ").append(quoteId(name)).append(" ON ").append(quoteId(name)).append(';');
			return sb.toString();
		}
	}

	@Override
	public Index index() {
		return new MySQLIndex();
	}

	public class MySQLTrigger extends Trigger {

		protected MySQLTrigger() {
			super();
		}

		@Override
		public boolean supportsIfNotExists() {
			// MySQL doesn't support IF NOT EXISTS; existence needs to be checked separately
			return false;
		}
	}

	@Override
	public Trigger trigger() {
		return new MySQLTrigger();
	}

	@Override
	public Trigger triggerLastModified(String eventTable, String reactionTable, String lastModifiedColumn, String idColumn) {
		String reaction;
		if (eventTable.equals(reactionTable)) {
			reaction = "SET NEW." + quoteId(lastModifiedColumn) + "=" + this.currentTime();
		} else {
			reaction = "UPDATE " + quoteId(reactionTable)
					+ " SET " + quoteId(lastModifiedColumn) + "=" + this.currentTime()
					+ " WHERE " + quoteId(idColumn) + "=NEW." + quoteId(idColumn);
		}
		// only before trigger can modify the event table
		return this.trigger().table(eventTable).name(eventTable + "_lastModified")
				.before().event(TriggerEvent.UPDATE).forEachRow()
				.reaction(reaction);
	}
}
