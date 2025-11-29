package dev.boredhuman.codegen.internal;

import com.dylibso.chicory.compiler.internal.CompilerFast;

import java.io.File;

public class CodeGenMain {
	public static void main(String[] args) throws Throwable {
		if (args.length != 2) {
			throw new RuntimeException(String.format("Expected 2 arguments but instead got %d arguments", args.length));
		}

		File wasmFile = new File(args[0]);

		if (!wasmFile.exists()) {
			throw new RuntimeException(String.format("Failed to find wasm file at %s\n", wasmFile.getAbsolutePath()));
		}

		CompilerFast.generate(wasmFile, args[1]);
	}
}
