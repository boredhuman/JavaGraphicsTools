package dev.boredhuman.glslang;

public class GlslangImportsImpl implements GlslangImports {

	private final GlslangInterface wasmInterface;

	public GlslangImportsImpl(GlslangInterface wasmInterface) {
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

	@Override
	public int environ_sizes_get(int envVarsCount, int envVarsSize) {
		// zero environment variables
		this.wasmInterface.store_i32(envVarsCount, 0, 0);
		this.wasmInterface.store_i32(envVarsSize, 0, 0);
		return 0;
	}

	@Override
	public int environ_get(int i, int i1) {
		// no environment variables
		return 0;
	}

	@Override
	public int fd_read(int i, int i1, int i2, int i3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int __syscall_getcwd(int i, int i1) {
		throw new UnsupportedOperationException();
	}
}
