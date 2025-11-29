package dev.boredhuman.exceptions;

public class TrapException extends RuntimeException {
	public TrapException() {
		super("Wasm trap exception");
	}
}
