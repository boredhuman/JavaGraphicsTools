package dev.boredhuman.spvc;

public class SpvcFactory {

	public static SpvcInterface createInstance(SpvcBinary binary) {
		Loader loader = new Loader();
		Class<?> klass = loader.createKlass(binary.getBinary());
		return new SpvcInterface(klass);
	}

	private static class Loader extends ClassLoader {
		public Loader() {
			super(SpvcFactory.class.getClassLoader());
		}

		public Class<?> createKlass(byte[] data) {
			return this.defineClass(null, data, 0,data.length);
		}
	}
}
