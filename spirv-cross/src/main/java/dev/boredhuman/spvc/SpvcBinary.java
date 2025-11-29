package dev.boredhuman.spvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpvcBinary {
	private final byte[] binary;

	public SpvcBinary(byte[] binary) {
		this.binary = binary;
	}

	public byte[] getBinary() {
		return this.binary;
	}

	public static SpvcBinary create() throws IOException {
		InputStream inputStream = SpvcBinary.class.getResourceAsStream("/dev/boredhuman/spvc/CompiledModule.class.bin");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int read;
		byte[] buf = new byte[4096];
		while ((read = inputStream.read(buf)) != -1) {
			bos.write(buf, 0, read);
		}

		inputStream.close();

		return new SpvcBinary(bos.toByteArray());
	}
}
