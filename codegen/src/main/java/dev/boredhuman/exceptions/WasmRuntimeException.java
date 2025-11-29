package dev.boredhuman.exceptions;

public class WasmRuntimeException extends RuntimeException {
	public WasmRuntimeException(String msg) {
		super(msg);
	}

	public WasmRuntimeException(Throwable cause) {
		super(cause);
	}

	public WasmRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
