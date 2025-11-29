package dev.boredhuman.glslang;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class Util {

	public static final int FALSE = 0;

	public static int writeString(GlslangInterface glslangInterface, String str) {
		byte[] strData = str.getBytes(StandardCharsets.UTF_8);
		int memoryDst = glslangInterface.malloc(strData.length + 1);

		for (int i = 0; i < strData.length; i++) {
			glslangInterface.store_i8(memoryDst, strData[i], i);
		}

		glslangInterface.store_i8(memoryDst, 0, strData.length);

		return memoryDst;
	}

	public static String readCString(GlslangInterface wasmInterface, int addr) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(16);

		int data;
		int i = 0;
		while ((data = wasmInterface.load_s8(addr, i)) != 0) {
			bos.write(data);
			i++;
		}

		return bos.toString();
	}
}
