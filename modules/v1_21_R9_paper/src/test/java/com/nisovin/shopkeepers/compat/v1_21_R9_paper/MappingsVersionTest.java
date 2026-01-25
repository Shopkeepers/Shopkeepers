package com.nisovin.shopkeepers.compat.v1_21_R9_paper;

import static org.junit.Assert.*;

import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.compat.CompatProvider;
import com.nisovin.shopkeepers.compat.CompatVersion;

import net.minecraft.SharedConstants;

public class MappingsVersionTest {

	private static String getMinecraftVersion() {
		SharedConstants.tryDetectVersion();
		return Unsafe.assertNonNull(SharedConstants.getCurrentVersion().id());
	}

	@Test
	public void testMappingsVersion() throws Exception {
		CompatVersion compatVersion = CompatProvider.getCompatVersion(CompatProviderImpl.VERSION_ID);
		String expectedMappingsVersion = compatVersion.getFirstMappingsVersion();
		var actualMappingsVersion = getMinecraftVersion();
		assertEquals("Unexpected mappings version!",
				expectedMappingsVersion,
				actualMappingsVersion
		);
	}
}
