package com.nisovin.shopkeepers.storage.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Utility for generating SQL queries, with sub-classes for the different dialects.
 */
public abstract class SQL {

	public static final String DEFAULT_COLUMN_ROLE_DELIMITER = "_";

	protected SQL() {
	}

	public String quoteId(String identifier) {
		return "`" + identifier + "`";
	}

	public String qualifiedColumn(String tableName, String columnName) {
		return quoteId(tableName) + "." + quoteId(columnName);
	}

	public String primaryKey() {
		return "PRIMARY KEY";
	}

	public abstract String ignore();

	public abstract String autoIncrement();

	public String doubleType() {
		return "DOUBLE";
	}

	public abstract String dateTimeType();

	public abstract String currentTime();

	public abstract String unixTimeMillis(String dateTimeColumn);

	public abstract int getJoinLimit();

	public interface CreatableDBObject {

		public boolean isValid();

		public String name();

		public String toCreateSQL();

		public String toDropSQL();
	}

	public class Column {

		private final Table table; // not null
		private final String name; // not null or empty

		protected String type; // not null or empty, normalized to upper case
		// TODO support COLLATE? sqlite default is binary, mysql default can be specified via extra
		protected boolean primaryKey;
		protected boolean autoIncrement;
		protected boolean notNull;
		protected Optional<String> defaultValue = null; // null if not specified, "NULL" is normalized to null

		private boolean built = false; // can't be modified anymore once built

		protected Column(Table table, String name) {
			Validate.notNull(table, "Table is null!");
			Validate.notEmpty(name, "Name is empty!");
			this.table = table;
			this.name = name;
		}

		public Table table() {
			return table;
		}

		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		// fully qualified name
		public String fqn() {
			return qualifiedColumn(this.table().name(), this.name());
		}

		public String type() {
			return type;
		}

		public boolean isPrimaryKey() {
			return primaryKey;
		}

		public boolean isAutoIncrement() {
			return autoIncrement;
		}

		public boolean isNotNull() {
			return notNull;
		}

		public Optional<String> defaultValue() {
			return defaultValue;
		}

		// BUILDER

		public boolean isValid() {
			return StringUtils.isEmpty(type);
		}

		public final boolean isBuilt() {
			return built;
		}

		protected final void validateNotBuilt() {
			Validate.State.isTrue(!built, "Already built!");
		}

		public final Column build() {
			this.validateNotBuilt();
			this.built = true;
			return this;
		}

		//

		public Column type(String type) {
			this.validateNotBuilt();
			Validate.notEmpty(type);
			this.type = type.toUpperCase(Locale.ROOT);
			return this;
		}

		public Column primaryKey() {
			this.validateNotBuilt();
			this.primaryKey = true;
			return this;
		}

		public Column autoIncrement() {
			this.validateNotBuilt();
			this.autoIncrement = true;
			return this;
		}

		public Column notNull() {
			this.validateNotBuilt();
			this.notNull = true;
			return this;
		}

		public Column defaultValue(String defaultValue) {
			this.validateNotBuilt();
			this.defaultValue = Optional.ofNullable("NULL".equalsIgnoreCase(defaultValue) ? null : defaultValue);
			return this;
		}

		// SQL

		public String toSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.qn()).append(' ').append(type);
			if (primaryKey) {
				sb.append(' ').append(SQL.this.primaryKey());
			}
			if (autoIncrement) {
				sb.append(' ').append(SQL.this.autoIncrement());
			}
			if (notNull) {
				sb.append(' ').append("NOT NULL");
			}
			// default value is always explicitly specified, since some DBMS don't differentiate between implicit and
			// explicit default value:
			// TODO look into the reasoning of this again. Might play a role when checking if a table already exists /
			// automated migrations / updating
			if (true || defaultValue != null) { // TODO
				String defaultVal = defaultValue == null ? null : defaultValue.orElse(null);
				sb.append(' ').append("DEFAULT");
				sb.append(' ').append(defaultVal == null ? "NULL" : defaultVal);
			}
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Column [table=");
			builder.append(table.name());
			builder.append(", name=");
			builder.append(name);
			builder.append(", type=");
			builder.append(type);
			builder.append(", primaryKey=");
			builder.append(primaryKey);
			builder.append(", autoIncrement=");
			builder.append(autoIncrement);
			builder.append(", notNull=");
			builder.append(notNull);
			builder.append(", defaultValue=");
			builder.append(defaultValue);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + table.hashCode();
			result = prime * result + (autoIncrement ? 1231 : 1237);
			result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + (notNull ? 1231 : 1237);
			result = prime * result + (primaryKey ? 1231 : 1237);
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof Column)) return false;
			Column other = (Column) obj;
			if (table.equals(other.table)) return false;
			if (autoIncrement != other.autoIncrement) return false;
			if (defaultValue == null) {
				if (other.defaultValue != null) return false;
			} else if (!defaultValue.equals(other.defaultValue)) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (notNull != other.notNull) return false;
			if (primaryKey != other.primaryKey) return false;
			if (type == null) {
				if (other.type != null) return false;
			} else if (!type.equals(other.type)) return false;
			return true;
		}
	}

	public class ForeignKey {

		protected String column; // not null or empty
		protected String referencedTable; // not null or empty
		protected String referencedColumn; // not null or empty
		protected boolean cascadeDelete;

		private boolean built = false; // can't be modified anymore once built

		protected ForeignKey() {
		}

		public String column() {
			return column;
		}

		public String referencedTable() {
			return referencedTable;
		}

		public String referencedColumn() {
			return referencedColumn;
		}

		public boolean isCascadeDelete() {
			return cascadeDelete;
		}

		// BUILDER

		public boolean isValid() {
			return !StringUtils.isEmpty(column) && !StringUtils.isEmpty(referencedTable) && !StringUtils.isEmpty(referencedColumn);
		}

		public final boolean isBuilt() {
			return built;
		}

		protected final void validateNotBuilt() {
			Validate.State.isTrue(!built, "Already built!");
		}

		public final ForeignKey build() {
			this.validateNotBuilt();
			this.built = true;
			return this;
		}

		//

		public ForeignKey column(Column column) {
			Validate.notNull(column);
			return this.column(column.name());
		}

		public ForeignKey column(String column) {
			this.validateNotBuilt();
			Validate.notEmpty(column);
			this.column = column;
			return this;
		}

		public ForeignKey referencedTable(Table table) {
			Validate.notNull(table);
			return this.referencedTable(table.name());
		}

		public ForeignKey referencedTable(String referencedTable) {
			this.validateNotBuilt();
			Validate.notEmpty(referencedTable);
			this.referencedTable = referencedTable;
			return this;
		}

		public ForeignKey referencedColumn(Column column) {
			Validate.notNull(column);
			return this.referencedColumn(column.name());
		}

		public ForeignKey referencedColumn(String referencedColumn) {
			this.validateNotBuilt();
			Validate.notEmpty(referencedColumn);
			this.referencedColumn = referencedColumn;
			return this;
		}

		public ForeignKey cascadeDelete() {
			this.validateNotBuilt();
			this.cascadeDelete = true;
			return this;
		}

		// SQL

		public String toSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("FOREIGN KEY").append('(').append(quoteId(column)).append(')')
					.append(' ').append("REFERENCES")
					.append(' ').append(quoteId(referencedTable))
					.append('(').append(quoteId(referencedColumn)).append(')');
			if (cascadeDelete) {
				sb.append(' ').append("ON DELETE CASCADE");
			}
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ForeignKey [column=");
			builder.append(column);
			builder.append(", referencedTable=");
			builder.append(referencedTable);
			builder.append(", referencedColumn=");
			builder.append(referencedColumn);
			builder.append(", cascadeDelete=");
			builder.append(cascadeDelete);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cascadeDelete ? 1231 : 1237);
			result = prime * result + ((column == null) ? 0 : column.hashCode());
			result = prime * result + ((referencedColumn == null) ? 0 : referencedColumn.hashCode());
			result = prime * result + ((referencedTable == null) ? 0 : referencedTable.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof ForeignKey)) return false;
			ForeignKey other = (ForeignKey) obj;
			if (cascadeDelete != other.cascadeDelete) return false;
			if (column == null) {
				if (other.column != null) return false;
			} else if (!column.equals(other.column)) return false;
			if (referencedColumn == null) {
				if (other.referencedColumn != null) return false;
			} else if (!referencedColumn.equals(other.referencedColumn)) return false;
			if (referencedTable == null) {
				if (other.referencedTable != null) return false;
			} else if (!referencedTable.equals(other.referencedTable)) return false;
			return true;
		}
	}

	public class Table implements CreatableDBObject {

		private final String name; // not null or empty

		protected final List<Column> columns = new ArrayList<>(); // not null or empty
		protected final List<ForeignKey> foreignKeys = new ArrayList<>(); // not null, but can be empty
		// additional attributes (eg. charset for MySQL), not supported by all DBMS
		protected String extra = ""; // not null

		private boolean built = false; // can't be modified anymore once built

		protected Table(String name) {
			Validate.notEmpty(name, "Name is empty!");
			this.name = name;
		}

		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		public List<Column> columns() {
			return Collections.unmodifiableList(columns);
		}

		public List<ForeignKey> foreignKeys() {
			return Collections.unmodifiableList(foreignKeys);
		}

		public ForeignKey getForeignKey(String column) {
			for (ForeignKey foreignKey : foreignKeys) {
				if (foreignKey.column.equals(column)) {
					return foreignKey;
				}
			}
			return null;
		}

		public boolean supportsExtra() {
			return false;
		}

		// BUILDER

		@Override
		public boolean isValid() {
			return !columns.isEmpty();
		}

		public final boolean isBuilt() {
			return built;
		}

		protected final void validateNotBuilt() {
			Validate.State.isTrue(!built, "Already built!");
		}

		public final Table build() {
			this.validateNotBuilt();
			this.built = true;
			return this;
		}

		//

		public Column column(String name) {
			this.validateNotBuilt();
			Column column = new Column(this, name);
			this.columns.add(column);
			return column;
		}

		// TODO move into Column?
		public ForeignKey foreignKey() {
			this.validateNotBuilt();
			ForeignKey foreignKey = new ForeignKey();
			this.foreignKeys.add(foreignKey);
			return foreignKey;
		}

		public Table extra(String extra) {
			this.validateNotBuilt();
			Validate.notNull(extra);
			this.extra = extra;
			return this;
		}

		// SQL

		@Override
		public String toCreateSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS").append(' ').append(this.qn());
			sb.append('(').append(columns.stream().map(c -> c.toSQL()).collect(Collectors.joining(",")));
			if (foreignKeys != null && !foreignKeys.isEmpty()) {
				sb.append(',').append(foreignKeys.stream().map(fk -> fk.toSQL()).collect(Collectors.joining(",")));
			}
			sb.append(")");
			if (this.supportsExtra() && !extra.isEmpty()) {
				sb.append(" ").append(extra);
			}
			sb.append(";");
			return sb.toString();
		}

		@Override
		public String toDropSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("DROP TABLE IF EXISTS ").append(this.qn());
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Table [name=");
			builder.append(name);
			builder.append(", columns=");
			builder.append(columns);
			builder.append(", foreignKeys=");
			builder.append(foreignKeys);
			builder.append(", extra=");
			builder.append(extra);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + columns.hashCode();
			result = prime * result + foreignKeys.hashCode();
			result = prime * result + name.hashCode();
			result = prime * result + extra.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof Table)) return false;
			Table other = (Table) obj;
			if (!columns.equals(other.columns)) return false;
			if (!foreignKeys.equals(other.foreignKeys)) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (!extra.equals(other.extra)) return false;
			return true;
		}
	}

	public Table table(String name) {
		return new Table(name);
	}

	public class View implements CreatableDBObject {

		private final String name; // not null or empty
		// TODO column list?
		protected String selectSQL;

		private boolean built = false; // can't be modified anymore once built

		protected View(String name) {
			Validate.notEmpty(name, "Name is empty!");
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		// BUILDER

		@Override
		public boolean isValid() {
			return !StringUtils.isEmpty(selectSQL);
		}

		public final boolean isBuilt() {
			return built;
		}

		protected final void validateNotBuilt() {
			Validate.State.isTrue(!built, "Already built!");
		}

		public View build() {
			this.validateNotBuilt();
			this.built = true;
			return this;
		}

		//

		public View select(String selectSQL) {
			this.validateNotBuilt();
			Validate.notEmpty(selectSQL);
			this.selectSQL = selectSQL;
			return this;
		}

		// SQL

		// undefined: if the view already exists, it may either ignore or replace it
		@Override
		public String toCreateSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE VIEW IF NOT EXISTS ").append(this.qn());
			sb.append(" AS ").append(selectSQL).append(';');
			return sb.toString();
		}

		@Override
		public String toDropSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("DROP VIEW IF EXISTS ").append(this.qn()).append(';');
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("View [name=");
			builder.append(name);
			builder.append(", selectSQL=");
			builder.append(selectSQL);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((selectSQL == null) ? 0 : selectSQL.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof View)) return false;
			View other = (View) obj;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (selectSQL == null) {
				if (other.selectSQL != null) return false;
			} else if (!selectSQL.equals(other.selectSQL)) return false;
			return true;
		}
	}

	public View view(String name) {
		return new View(name);
	}

	public class CombinedView implements CreatableDBObject {

		private final String name; // not null or empty
		private Table table;
		private final List<ForeignKeyJoin> joins = new ArrayList<>();
		private boolean omitReferencedColumns = false;
		private String roleDelimiter = DEFAULT_COLUMN_ROLE_DELIMITER;

		private boolean built = false; // can't be modified anymore once built
		private View view = null;

		protected CombinedView(String name) {
			Validate.notEmpty(name, "Name is empty!");
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		public Table table() {
			return table;
		}

		public List<ForeignKeyJoin> joins() {
			return Collections.unmodifiableList(joins);
		}

		public boolean isOmitReferencedColumns() {
			return omitReferencedColumns;
		}

		public String roleDelimiter() {
			return roleDelimiter;
		}

		public ViewColumn getColumn(String column, String... roles) {
			String columnName = column;
			if (roles != null && roles.length > 0) {
				columnName = Arrays.asList(roles).stream().collect(Collectors.joining(roleDelimiter)) + roleDelimiter + column;
			}
			return SQL.this.viewColumn(view, columnName);
		}

		public View getView() {
			return view;
		}

		// BUILDER

		@Override
		public boolean isValid() {
			return table != null;
		}

		public final boolean isBuilt() {
			return built;
		}

		protected final void validateNotBuilt() {
			Validate.State.isTrue(!built, "Already built!");
		}

		protected final void validateBuilt() {
			Validate.State.isTrue(built, "Not yet built!");
		}

		public CombinedView build() {
			this.validateNotBuilt();
			this.built = true;
			String selectSQL = SQL.this.selectCombinedSQL(table, joins, roleDelimiter, omitReferencedColumns);
			this.view = SQL.this.view(name).select(selectSQL).build();
			return this;
		}

		//

		public CombinedView table(Table table) {
			this.validateNotBuilt();
			Validate.notNull(table);
			this.table = table;
			return this;
		}

		public CombinedView join(ForeignKeyJoin join) {
			this.validateNotBuilt();
			Validate.notNull(join);
			this.joins.add(join);
			return this;
		}

		public CombinedView omitReferencedColumns(boolean omitReferencedColumns) {
			this.validateNotBuilt();
			this.omitReferencedColumns = omitReferencedColumns;
			return this;
		}

		public CombinedView roleDelimiter(String roleDelimiter) {
			this.validateNotBuilt();
			Validate.notNull(roleDelimiter);
			this.roleDelimiter = roleDelimiter;
			return this;
		}

		// SQL

		@Override
		public String toCreateSQL() {
			this.validateBuilt();
			return view.toCreateSQL();
		}

		@Override
		public String toDropSQL() {
			this.validateBuilt();
			return view.toDropSQL();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CombinedView [name=");
			builder.append(name);
			builder.append(", table=");
			builder.append(table.name());
			builder.append(", joins=");
			builder.append(joins);
			builder.append(", roleDelimiter=");
			builder.append(roleDelimiter);
			builder.append(", omitReferencedColumns=");
			builder.append(omitReferencedColumns);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + joins.hashCode();
			result = prime * result + name.hashCode();
			result = prime * result + (omitReferencedColumns ? 1231 : 1237);
			result = prime * result + roleDelimiter.hashCode();
			result = prime * result + ((table == null) ? 0 : table.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof CombinedView)) return false;
			CombinedView other = (CombinedView) obj;
			if (!joins.equals(other.joins)) return false;
			if (!name.equals(other.name)) return false;
			if (omitReferencedColumns != other.omitReferencedColumns) return false;
			if (!roleDelimiter.equals(other.roleDelimiter)) return false;
			if (table == null) {
				if (other.table != null) return false;
			} else if (!table.equals(other.table)) return false;
			return true;
		}
	}

	public CombinedView combinedView(String name) {
		return new CombinedView(name);
	}

	public class ViewColumn {

		private final View view;
		private final String name;

		protected ViewColumn(View view, String name) {
			Validate.notNull(view);
			Validate.notEmpty(name);
			this.view = view;
			this.name = name;
		}

		public View view() {
			return view;
		}

		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		// fully qualified name
		public String fqn() {
			return qualifiedColumn(this.view().name(), this.name());
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ViewColumn [view=");
			builder.append(view.name());
			builder.append(", name=");
			builder.append(name);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + name.hashCode();
			result = prime * result + view.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof ViewColumn)) return false;
			ViewColumn other = (ViewColumn) obj;
			if (!name.equals(other.name)) return false;
			if (!view.equals(other.view)) return false;
			return true;
		}
	}

	protected ViewColumn viewColumn(View view, String columnName) {
		return new ViewColumn(view, columnName);
	}

	public class Index implements CreatableDBObject {

		protected String table; // not null or empty
		protected String name; // not null or empty
		protected boolean unique;
		protected final List<String> columns = new ArrayList<>(); // not null or empty

		protected Index() {
		}

		public String table() {
			return table;
		}

		@Override
		public String name() {
			return name;
		}

		// quoted name
		public String qn() {
			return quoteId(this.name());
		}

		public boolean isUnique() {
			return unique;
		}

		public List<String> columns() {
			return Collections.unmodifiableList(columns);
		}

		// BUILDER

		@Override
		public boolean isValid() {
			return !StringUtils.isEmpty(table) && !StringUtils.isEmpty(name) && !columns.isEmpty();
		}

		//

		public Index table(Table table) {
			Validate.notNull(table);
			return this.table(table.name());
		}

		public Index table(String table) {
			Validate.notEmpty(table);
			this.table = table;
			// default name if none is specified yet:
			if (name == null && columns.size() == 1) {
				name = table + "_" + columns.get(0);
			}
			return this;
		}

		public Index name(String name) {
			Validate.notEmpty(name);
			this.name = name;
			return this;
		}

		public Index unique() {
			this.unique = true;
			return this;
		}

		public Index column(Column column) {
			Validate.notNull(column);
			return this.column(column.name());
		}

		public Index column(String column) {
			Validate.notEmpty(column);
			this.columns.add(column);
			// default name if none is specified yet:
			if (name == null && table != null && columns.size() == 1) {
				name = table + "_" + column;
			}
			return this;
		}

		// SQL

		public boolean supportsIfNotExists() {
			return true;
		}

		@Override
		public String toCreateSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE");
			if (unique) {
				sb.append(' ').append("UNIQUE");
			}
			sb.append(' ').append("INDEX");
			if (this.supportsIfNotExists()) {
				sb.append(" IF NOT EXISTS");
			}
			sb.append(' ').append(quoteId(name));
			sb.append(' ').append("ON").append(' ').append(quoteId(table));
			sb.append(' ').append('(').append(columns.stream().map(c -> quoteId(c)).collect(Collectors.joining(","))).append(");");
			return sb.toString();
		}

		@Override
		public String toDropSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("DROP INDEX IF EXISTS ").append(this.qn());
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Index [table=");
			builder.append(table);
			builder.append(", name=");
			builder.append(name);
			builder.append(", unique=");
			builder.append(unique);
			builder.append(", columns=");
			builder.append(columns);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((columns == null) ? 0 : columns.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((table == null) ? 0 : table.hashCode());
			result = prime * result + (unique ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof Index)) return false;
			Index other = (Index) obj;
			if (columns == null) {
				if (other.columns != null) return false;
			} else if (!columns.equals(other.columns)) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (table == null) {
				if (other.table != null) return false;
			} else if (!table.equals(other.table)) return false;
			if (unique != other.unique) return false;
			return true;
		}
	}

	public Index index() {
		return new Index();
	}

	public enum TriggerEvent {
		DELETE,
		INSERT,
		UPDATE;
	}

	public class Trigger implements CreatableDBObject {

		protected String table; // not null or empty
		protected String name; // not null or empty
		protected boolean after = true; // default: after
		protected TriggerEvent event; // not null
		protected final List<String> columns = new ArrayList<>(); // only used for update, can be empty
		protected boolean forEachRow = false;
		protected String whenExpr = null;
		protected String reaction; // not null or empty

		protected Trigger() {
		}

		public String table() {
			return table;
		}

		@Override
		public String name() {
			return name;
		}

		public boolean isAfter() {
			return after;
		}

		public boolean isBefore() {
			return !after;
		}

		public TriggerEvent event() {
			return event;
		}

		public List<String> columns() {
			return Collections.unmodifiableList(columns);
		}

		public boolean isForEachRows() {
			return forEachRow;
		}

		public String whenExpression() {
			return whenExpr;
		}

		public String reaction() {
			return reaction;
		}

		// BUILDER

		@Override
		public boolean isValid() {
			return !StringUtils.isEmpty(table) && !StringUtils.isEmpty(name) && event != null && !StringUtils.isEmpty(reaction);
		}

		//

		public Trigger table(Table table) {
			Validate.notNull(table);
			return this.table(table.name());
		}

		public Trigger table(String table) {
			Validate.notEmpty(table);
			this.table = table;
			return this;
		}

		public Trigger name(String name) {
			Validate.notEmpty(name);
			this.name = name;
			return this;
		}

		public Trigger after() {
			this.after = true;
			return this;
		}

		public Trigger before() {
			this.after = false;
			return this;
		}

		public Trigger event(TriggerEvent event) {
			Validate.notNull(event);
			this.event = event;
			return this;
		}

		public Trigger column(Column column) {
			Validate.notNull(column);
			return this.column(column.name());
		}

		public Trigger column(String column) {
			Validate.notEmpty(column);
			columns.add(column);
			return this;
		}

		public Trigger forEachRow() {
			this.forEachRow = true;
			return this;
		}

		public Trigger when(String whenExpr) {
			Validate.notEmpty(whenExpr);
			this.whenExpr = whenExpr;
			return this;
		}

		public Trigger reaction(String reaction) {
			Validate.notEmpty(reaction);
			this.reaction = reaction;
			return this;
		}

		// SQL

		public boolean supportsIfNotExists() {
			return true;
		}

		protected String reactionSQL() {
			return reaction;
		}

		@Override
		public String toCreateSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TRIGGER");
			if (this.supportsIfNotExists()) {
				sb.append(" IF NOT EXISTS");
			}
			sb.append(' ').append(quoteId(name));
			if (after) {
				sb.append(' ').append("AFTER");
			} else {
				sb.append(' ').append("BEFORE");
			}
			sb.append(' ').append(event.name());
			if (event == TriggerEvent.UPDATE && !columns.isEmpty()) {
				sb.append(' ').append("OF").append(' ');
				sb.append(columns.stream().map(c -> quoteId(c)).collect(Collectors.joining(",")));

			}
			sb.append(' ').append("ON").append(' ').append(quoteId(table));
			if (forEachRow) {
				sb.append(' ').append("FOR EACH ROW");
			}
			if (whenExpr != null) {
				sb.append(' ').append("WHEN").append(' ').append(whenExpr);
			}
			sb.append(' ').append(this.reactionSQL()).append(';');
			return sb.toString();
		}

		@Override
		public String toDropSQL() {
			StringBuilder sb = new StringBuilder();
			sb.append("DROP TRIGGER IF EXISTS");
			sb.append(' ').append(quoteId(name)).append(';');
			return sb.toString();
		}

		//

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Trigger [table=");
			builder.append(table);
			builder.append(", name=");
			builder.append(name);
			builder.append(", after=");
			builder.append(after);
			builder.append(", event=");
			builder.append(event);
			builder.append(", columns=");
			builder.append(columns);
			builder.append(", forEachRow=");
			builder.append(forEachRow);
			builder.append(", whenExpr=");
			builder.append(whenExpr);
			builder.append(", reaction=");
			builder.append(reaction);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (after ? 1231 : 1237);
			result = prime * result + ((columns == null) ? 0 : columns.hashCode());
			result = prime * result + ((event == null) ? 0 : event.hashCode());
			result = prime * result + (forEachRow ? 1231 : 1237);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((reaction == null) ? 0 : reaction.hashCode());
			result = prime * result + ((table == null) ? 0 : table.hashCode());
			result = prime * result + ((whenExpr == null) ? 0 : whenExpr.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof Trigger)) return false;
			Trigger other = (Trigger) obj;
			if (after != other.after) return false;
			if (columns == null) {
				if (other.columns != null) return false;
			} else if (!columns.equals(other.columns)) return false;
			if (event != other.event) return false;
			if (forEachRow != other.forEachRow) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (reaction == null) {
				if (other.reaction != null) return false;
			} else if (!reaction.equals(other.reaction)) return false;
			if (table == null) {
				if (other.table != null) return false;
			} else if (!table.equals(other.table)) return false;
			if (whenExpr == null) {
				if (other.whenExpr != null) return false;
			} else if (!whenExpr.equals(other.whenExpr)) return false;
			return true;
		}
	}

	public Trigger trigger() {
		return new Trigger();
	}

	public abstract Trigger triggerLastModified(String eventTable, String reactionTable, String lastModifiedColumn, String idColumn);

	public static class ForeignKeyJoin {

		public final Table table;
		public final String role; // not null, can be empty
		public final Table joinedTable;
		public final String joinedRole; // not null, can be empty
		public final ForeignKey foreignKey;

		// in case the table is the top-level table involved in the join, no role is required for it
		public ForeignKeyJoin(Table table, Table joinedTable, String joinedRole, ForeignKey foreignKey) {
			this(table, "", joinedTable, joinedRole, foreignKey);
		}

		public ForeignKeyJoin(Table table, String role, Table joinedTable, String joinedRole, ForeignKey foreignKey) {
			Validate.notNull(table);
			Validate.notNull(joinedTable);
			Validate.notNull(foreignKey);
			Validate.isTrue(joinedTable.name().equals(foreignKey.referencedTable()));
			// assert: table contains foreignKey.column() and joinedTable contains foreignKey.referencedColumn()
			this.table = table;
			this.role = (role == null) ? "" : role; // normalize null to empty
			this.joinedTable = joinedTable;
			this.joinedRole = (joinedRole == null) ? "" : joinedRole; // normalize null to empty
			this.foreignKey = foreignKey;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ForeignKeyJoin [table=");
			builder.append(table.name());
			builder.append(", role=");
			builder.append(role);
			builder.append(", joinedTable=");
			builder.append(joinedTable.name());
			builder.append(", joinedRole=");
			builder.append(joinedRole);
			builder.append(", foreignKey=");
			builder.append(foreignKey);
			builder.append("]");
			return builder.toString();
		}
	}

	// recursively replaces any foreign key columns with the columns of the referenced table
	protected Stream<String> _expandForeignKeyColumns(Column column, String role, List<ForeignKeyJoin> joins, String roleDelimiter, boolean omitReferencedColumns) {
		String tableName = column.table.name();
		String columnName = column.name();
		ForeignKeyJoin join = null;
		for (ForeignKeyJoin _join : joins) {
			if (tableName.equals(_join.table.name()) && columnName.equals(_join.foreignKey.column())) {
				join = _join;
				break;
			}
		}

		if (join != null) {
			// return the joined columns:
			ForeignKeyJoin joinFinal = join;
			return join.joinedTable.columns().stream()
					// skip the referenced column in the output:
					.filter(joinedColumn -> !omitReferencedColumns || !joinedColumn.name().equals(joinFinal.foreignKey.referencedColumn))
					// recursively expand foreign key columns:
					.flatMap(joinedColumn -> {
						// combine parent and joined role names:
						String joinedRole;
						if (role.isEmpty() || joinFinal.joinedRole.isEmpty()) {
							// omit delimiter:
							joinedRole = role + joinFinal.joinedRole;
						} else {
							// both not empty -> combine with delimiter:
							joinedRole = role + roleDelimiter + joinFinal.joinedRole;
						}
						return this._expandForeignKeyColumns(joinedColumn, joinedRole, joins, roleDelimiter, omitReferencedColumns);
					});
		} else {
			String rolePrefix;
			String roleName;
			if (role.isEmpty()) {
				rolePrefix = "";
				roleName = tableName;
			} else {
				rolePrefix = role + roleDelimiter;
				roleName = role;
			}
			return Stream.of(qualifiedColumn(roleName, columnName) + " AS " + quoteId(rolePrefix + column.name()));
		}
	}

	// omitReferencedColumns: whether to omit or include the referenced id columns in the output
	// Note: The returned query is not ended with ';' to allow for extension or embedding.
	public String selectCombinedSQL(Table table, List<ForeignKeyJoin> joins, String roleDelimiter, boolean omitReferencedColumns) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		// recursively expand foreign key columns:
		sb.append(table.columns().stream().flatMap(column -> {
			return this._expandForeignKeyColumns(column, "", joins, roleDelimiter, omitReferencedColumns);
		}).collect(Collectors.joining(",")));
		sb.append(" FROM ").append(table.qn());
		sb.append(' ').append(joins.stream().map(join -> {
			StringBuilder sbJoin = new StringBuilder();
			sbJoin.append("LEFT JOIN ").append(join.joinedTable.qn());

			// role name of the join target table:
			String role;
			if (join.role.isEmpty()) {
				role = join.table.name();
			} else {
				role = join.role;
			}

			// role name of the joined table:
			ForeignKey fk = join.foreignKey;
			String joinedRole;
			if (join.joinedRole.isEmpty()) {
				joinedRole = fk.referencedTable();
			} else {
				// combine with parent role name:
				if (!join.role.isEmpty()) {
					joinedRole = join.role + roleDelimiter + join.joinedRole;
				} else {
					joinedRole = join.joinedRole;
				}
				sbJoin.append(' ').append(quoteId(joinedRole));
			}

			sbJoin.append(" ON ").append(qualifiedColumn(joinedRole, fk.referencedColumn()));
			sbJoin.append('=').append(qualifiedColumn(role, fk.column()));
			return sbJoin.toString();
		}).collect(Collectors.joining(" ")));
		return sb.toString();
	}
}
