package dev.boredhuman.glslang;

public class GlslangFactory {

	public static GlslangInterface createInstance(GlslangBinary binary) {
		Loader loader = new Loader();
		Class<?> klass = loader.createKlass(binary.getBinary());
		return new GlslangInterface(klass);
	}

	private static class Loader extends ClassLoader {
		public Loader() {
			super(GlslangFactory.class.getClassLoader());
		}

		public Class<?> createKlass(byte[] data) {
			return this.defineClass(null, data, 0,data.length);
		}
	}
}
