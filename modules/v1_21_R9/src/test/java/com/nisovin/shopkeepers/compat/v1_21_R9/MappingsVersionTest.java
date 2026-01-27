package com.nisovin.shopkeepers.compat.v1_21_R9;

import static org.junit.Assert.*;

import org.bukkit.craftbukkit.v1_21_R7.util.CraftMagicNumbers;
import org.junit.Test;

import com.nisovin.shopkeepers.compat.CompatProvider;
import com.nisovin.shopkeepers.compat.CompatVersion;

public class MappingsVersionTest {

	@Test
	public void testMappingsVersion() throws Exception {
		CompatVersion compatVersion = CompatProvider.getCompatVersion(CompatProviderImpl.VERSION_ID);
		String expectedMappingsVersion = compatVersion.getFirstMappingsVersion();
		String actualMappingsVersion = MappingsVersionExtractor.getMappingsVersion(
				CraftMagicNumbers.class
		);
		assertEquals("Unexpected mappings version!",
				expectedMappingsVersion,
				actualMappingsVersion
		);
	}
}
