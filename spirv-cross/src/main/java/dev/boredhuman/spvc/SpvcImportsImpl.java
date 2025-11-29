package dev.boredhuman.spvc;

public class SpvcImportsImpl implements SpvcImports {

	private final SpvcInterface wasmInterface;

	public SpvcImportsImpl(SpvcInterface wasmInterface) {
		this.wasmInterface = wasmInterface;
	}

	@Override
	public int fd_close(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int fd_write(int i, int i1, int i2, int i3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int fd_seek(int i, long l, int i1, int i2) {
		throw new UnsupportedOperationException();
	}
}
