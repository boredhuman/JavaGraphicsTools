package dev.boredhuman.glslang.wrapper;

public enum GlslangTargetClientVersion {
	GLSLANG_TARGET_VULKAN_1_0(1 << 22),
	GLSLANG_TARGET_VULKAN_1_1((1 << 22) | (1 << 12)),
	GLSLANG_TARGET_VULKAN_1_2((1 << 22) | (2 << 12)),
	GLSLANG_TARGET_VULKAN_1_3((1 << 22) | (3 << 12)),
	GLSLANG_TARGET_VULKAN_1_4((1 << 22) | (4 << 12)),
	GLSLANG_TARGET_OPENGL_450(450);

	public final int value;

	GlslangTargetClientVersion(int value) {
		this.value = value;
	}
}
