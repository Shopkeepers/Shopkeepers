package com.nisovin.shopkeepers.compat.v1_20_R4;

import static org.junit.Assert.*;

import org.junit.Test;

import com.nisovin.shopkeepers.compat.CompatVersion;

public class MappingsVersionTest {

	@Test
	public void testMappingsVersion() throws Exception {
		NMSHandler nmsHandler = new NMSHandler();
		CompatVersion compatVersion = nmsHandler.getCompatVersion();
		String expectedMappingsVersion = compatVersion.getMappingsVersion();
		String actualMappingsVersion = MappingsVersionExtractor.getMappingsVersion(
				nmsHandler.getCraftMagicNumbersClass()
		);
		assertEquals("Unexpected mappings version!",
				expectedMappingsVersion,
				actualMappingsVersion
		);
	}
}
