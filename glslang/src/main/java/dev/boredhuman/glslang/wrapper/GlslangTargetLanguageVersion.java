package dev.boredhuman.glslang.wrapper;

public enum GlslangTargetLanguageVersion {
	GLSLANG_TARGET_SPV_1_0(1 << 16),
	GLSLANG_TARGET_SPV_1_1((1 << 16) | (1 << 8)),
	GLSLANG_TARGET_SPV_1_2((1 << 16) | (2 << 8)),
	GLSLANG_TARGET_SPV_1_3((1 << 16) | (3 << 8)),
	GLSLANG_TARGET_SPV_1_4((1 << 16) | (4 << 8)),
	GLSLANG_TARGET_SPV_1_5((1 << 16) | (5 << 8)),
	GLSLANG_TARGET_SPV_1_6((1 << 16) | (6 << 8));

	public final int value;

	GlslangTargetLanguageVersion(int value) {
		this.value = value;
	}
}
