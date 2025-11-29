package dev.boredhuman.glslang.wrapper;

public enum GlslangProfile {
	GLSLANG_BAD_PROFILE(0),
	GLSLANG_NO_PROFILE(1 << 0),
	GLSLANG_CORE_PROFILE(1 << 1),
	GLSLANG_COMPATIBILITY_PROFILE(1 << 2),
	GLSLANG_ES_PROFILE(1 << 3);

	public final int value;

	GlslangProfile(int value) {
		this.value = value;
	}
}
