package dev.boredhuman.support;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class CompiledModule {
	private static final Unsafe UNSAFE = CompiledModule.getUnsafe();
	private static final int BYTE_ARRAY_BASE_OFFSET = CompiledModule.UNSAFE.arrayBaseOffset(byte[].class);
	private static int MEMORY_SIZE = CompiledModule.memorySize();
	private static long MEMORY = CompiledModule.UNSAFE.allocateMemory(CompiledModule.MEMORY_SIZE);

	public static int memorySize() {
		return 0;
	}

	private static Unsafe getUnsafe() {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);

			return (Unsafe) theUnsafe.get(null);
		} catch (Throwable err) {
			throw new RuntimeException(err);
		}
	}

	public static int load_i32(int addr) {
		return CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr);
	}

	public static long load_i64(int addr) {
		return CompiledModule.UNSAFE.getLong(CompiledModule.MEMORY + addr);
	}

	public static float load_f32(int addr) {
		return CompiledModule.UNSAFE.getFloat(CompiledModule.MEMORY + addr);
	}

	public static double load_f64(int addr) {
		return CompiledModule.UNSAFE.getDouble(CompiledModule.MEMORY + addr);
	}

	public static int load_s8(int addr) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr);
	}

	public static int load_u8(int addr) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr) & 0xFF;
	}

	public static int load_s16(int addr) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr);
	}

	public static int load_u16(int addr) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr) & 0xFFFF;
	}

	public static long load_s8_64(int addr) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr);
	}

	public static long load_u8_64(int addr) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr) & 0xFF;
	}

	public static long load_s16_64(int addr) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr);
	}

	public static long load_u16_64(int addr) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr) & 0xFFFF;
	}

	public static long load_s32_64(int addr) {
		return CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr);
	}

	public static long load_u32_64(int addr) {
		return ((long) CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr)) & 0xFFFF_FFFFL;
	}

	public static void store_f32(int addr, float value) {
		CompiledModule.UNSAFE.putFloat(CompiledModule.MEMORY + addr, value);
	}

	public static void store_f64(int addr, double value) {
		CompiledModule.UNSAFE.putDouble(CompiledModule.MEMORY + addr, value);
	}

	public static void store_i8(int addr, int value) {
		CompiledModule.UNSAFE.putByte(CompiledModule.MEMORY + addr, (byte) value);
	}

	public static void store_i16(int addr, int value) {
		CompiledModule.UNSAFE.putShort(CompiledModule.MEMORY + addr, (short) value);
	}

	public static void store_i32(int addr, int value) {
		CompiledModule.UNSAFE.putInt(CompiledModule.MEMORY + addr, value);
	}

	public static void store_i8(int addr, long value) {
		CompiledModule.UNSAFE.putByte(CompiledModule.MEMORY + addr, (byte) value);
	}

	public static void store_i16(int addr, long value) {
		CompiledModule.UNSAFE.putShort(CompiledModule.MEMORY + addr, (short) value);
	}

	public static void store_i32(int addr, long value) {
		CompiledModule.UNSAFE.putInt(CompiledModule.MEMORY + addr, (int) value);
	}

	public static void store_i64(int addr, long value) {
		CompiledModule.UNSAFE.putLong(CompiledModule.MEMORY + addr, value);
	}

	public static int load_i32(int addr, int offset) {
		return CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr + offset);
	}

	public static long load_i64(int addr, int offset) {
		return CompiledModule.UNSAFE.getLong(CompiledModule.MEMORY + addr + offset);
	}

	public static float load_f32(int addr, int offset) {
		return CompiledModule.UNSAFE.getFloat(CompiledModule.MEMORY + addr + offset);
	}

	public static double load_f64(int addr, int offset) {
		return CompiledModule.UNSAFE.getDouble(CompiledModule.MEMORY + addr + offset);
	}

	public static int load_s8(int addr, int offset) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr + offset);
	}

	public static int load_u8(int addr, int offset) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr + offset) & 0xFF;
	}

	public static int load_s16(int addr, int offset) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr + offset);
	}

	public static int load_u16(int addr, int offset) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr + offset) & 0xFFFF;
	}

	public static long load_s8_64(int addr, int offset) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr + offset);
	}

	public static long load_u8_64(int addr, int offset) {
		return CompiledModule.UNSAFE.getByte(CompiledModule.MEMORY + addr + offset) & 0xFF;
	}

	public static long load_s16_64(int addr, int offset) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr + offset);
	}

	public static long load_u16_64(int addr, int offset) {
		return CompiledModule.UNSAFE.getShort(CompiledModule.MEMORY + addr + offset) & 0xFFFF;
	}

	public static long load_s32_64(int addr, int offset) {
		return CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr + offset);
	}

	public static long load_u32_64(int addr, int offset) {
		return ((long) CompiledModule.UNSAFE.getInt(CompiledModule.MEMORY + addr + offset)) & 0xFFFF_FFFFL;
	}

	public static void store_f32(int addr, float value, int offset) {
		CompiledModule.UNSAFE.putFloat(CompiledModule.MEMORY + addr + offset, value);
	}

	public static void store_f64(int addr, double value, int offset) {
		CompiledModule.UNSAFE.putDouble(CompiledModule.MEMORY + addr + offset, value);
	}

	public static void store_i8(int addr, int value, int offset) {
		CompiledModule.UNSAFE.putByte(CompiledModule.MEMORY + addr + offset, (byte) value);
	}

	public static void store_i16(int addr, int value, int offset) {
		CompiledModule.UNSAFE.putShort(CompiledModule.MEMORY + addr + offset, (short) value);
	}

	public static void store_i32(int addr, int value, int offset) {
		CompiledModule.UNSAFE.putInt(CompiledModule.MEMORY + addr + offset, value);
	}

	public static void store_i8(int addr, long value, int offset) {
		CompiledModule.UNSAFE.putByte(CompiledModule.MEMORY + addr + offset, (byte) value);
	}

	public static void store_i16(int addr, long value, int offset) {
		CompiledModule.UNSAFE.putShort(CompiledModule.MEMORY + addr + offset, (short) value);
	}

	public static void store_i32(int addr, long value, int offset) {
		CompiledModule.UNSAFE.putInt(CompiledModule.MEMORY + addr + offset, (int) value);
	}

	public static void store_i64(int addr, long value, int offset) {
		CompiledModule.UNSAFE.putLong(CompiledModule.MEMORY + addr + offset, value);
	}

	public static int memory_grow(int pages) {
		int currentPages = CompiledModule.MEMORY_SIZE / 65536;

		long newSize = (currentPages + pages) * 65536L;
		long newMemory = CompiledModule.UNSAFE.allocateMemory(newSize);

		CompiledModule.UNSAFE.copyMemory(CompiledModule.MEMORY, newMemory, CompiledModule.MEMORY_SIZE);
		CompiledModule.UNSAFE.freeMemory(CompiledModule.MEMORY);

		CompiledModule.MEMORY = newMemory;
		CompiledModule.MEMORY_SIZE = (int) newSize;


		return currentPages;
	}

	public static int memory_size() {
		return CompiledModule.MEMORY_SIZE / 65536;
	}

	public static void memory_fill(int offset, int value, int size) {
		CompiledModule.UNSAFE.setMemory(CompiledModule.MEMORY + offset, size, (byte) value);
	}

	public static void memory_copy(int dst, int src, int size) {
		CompiledModule.UNSAFE.copyMemory(CompiledModule.MEMORY + src, CompiledModule.MEMORY + dst, size);
	}

	public static void arraycopy(byte[] data, int dstOffset, int length) {
		CompiledModule.UNSAFE.copyMemory(data, CompiledModule.BYTE_ARRAY_BASE_OFFSET, null, CompiledModule.MEMORY + dstOffset, length);
	}

	public static void heap_free() {
		CompiledModule.UNSAFE.freeMemory(CompiledModule.MEMORY);
		CompiledModule.MEMORY_SIZE = 0;
		CompiledModule.MEMORY = 0;
	}
}
