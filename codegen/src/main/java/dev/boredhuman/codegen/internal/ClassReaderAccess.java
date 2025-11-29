package dev.boredhuman.codegen.internal;

import org.objectweb.asm.ClassReader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class ClassReaderAccess {
	private static final MethodHandle constantUtf8ValuesHandle;

	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ClassReader.class, MethodHandles.lookup());
			constantUtf8ValuesHandle = lookup.findGetter(ClassReader.class, "constantUtf8Values", String[].class);
		} catch (Throwable err) {
			throw new RuntimeException(err);
		}
	}

	public static String[] getConstantUtf8ValuesHandle(ClassReader classReader) {
		try {
			return (String[]) ClassReaderAccess.constantUtf8ValuesHandle.invokeExact(classReader);
		} catch (Throwable err) {
			throw new RuntimeException(err);
		}
	}
}
