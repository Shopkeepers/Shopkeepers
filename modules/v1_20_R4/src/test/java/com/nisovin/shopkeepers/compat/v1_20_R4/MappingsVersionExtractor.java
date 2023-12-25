package com.nisovin.shopkeepers.compat.v1_20_R4;

import java.io.IOException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.nisovin.shopkeepers.util.java.Box;

/**
 * Extracts the mappings version from the byte code of the CraftMagicNumbers class.
 * <p>
 * During tests, we cannot instantiate the CraftMagicNumbers class to then retrieve the mappings
 * version like normal, because the static initializers of this class access Minecraft registries,
 * which check whether the server has performed its usual startup routines (which is not the case
 * during tests). If we attempt to access the class anyways, these static initializers will trigger
 * a 'bootstrap exception'.
 */
public final class MappingsVersionExtractor {

	// Loading and passing the class is fine, as long as we don't access any members, which then
	// results in the static initializers to be executed.
	public static String getMappingsVersion(Class<?> craftMagicNumbersClass) throws IOException {
		Box<@Nullable String> result = new Box<>();
		ClassReader reader = new ClassReader(craftMagicNumbersClass.getName());
		reader.accept(new ClassVisitor(Opcodes.ASM9) {
			@SuppressWarnings("override.return")
			@Override
			public @Nullable MethodVisitor visitMethod(
					int access,
					String name,
					String desc, String signature,
					String[] exceptions
			) {
				if (name.equals("getMappingsVersion")) {
					return new MethodVisitor(api) {
						@Override
						public void visitLdcInsn(final Object value) {
							result.setValue((String) value);
						}
					};
				}
				return null;
			}
		}, 0);

		String mappingsVersion = result.getValue();
		if (mappingsVersion == null) {
			throw new RuntimeException("Could not extract the mappings version!");
		}
		return mappingsVersion;
	}

	private MappingsVersionExtractor() {
	}
}
