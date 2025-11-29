package dev.boredhuman.glslang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GlslangBinary {
	private final byte[] binary;

	public GlslangBinary(byte[] binary) {
		this.binary = binary;
	}

	public byte[] getBinary() {
		return this.binary;
	}

	public static GlslangBinary create() throws IOException {
		InputStream inputStream = GlslangFactory.class.getResourceAsStream("/dev/boredhuman/glslang/CompiledModule.class.bin");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int read;
		byte[] buf = new byte[4096];
		while ((read = inputStream.read(buf)) != -1) {
			bos.write(buf, 0, read);
		}

		inputStream.close();

		return new GlslangBinary(bos.toByteArray());
	}
}
