package com.nisovin.shopkeepers.storage.versioning.sql;

import com.nisovin.shopkeepers.storage.sql.SQLConnector;
import com.nisovin.shopkeepers.storage.sql.SQLStorageComponent;

public class SQLVersioning extends SQLStorageComponent {

	private static final String STORAGE_VERSION_KEY = "storage";

	public SQLVersioning(SQLConnector connector) {
		super("versioning", connector);
	}

	public int getVersion(String componentName) {
		return 0; // TODO
	}

	public void setVersion(String componentName, int newVersion) {
		// TODO
	}

	public int getStorageVersion() {
		return this.getVersion(STORAGE_VERSION_KEY);
	}

	public void setStorageVersion(int newVersion) {
		this.setVersion(STORAGE_VERSION_KEY, newVersion);
	}
}
