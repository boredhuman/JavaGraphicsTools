package dev.boredhuman.glslang.wrapper;

import dev.boredhuman.glslang.GlslangInterface;

public class GlslangInput {
	public GlslangSource language;
	public GlslangStage stage;
	public GlslangClient client;
	public GlslangTargetClientVersion clientVersion;
	public GlslangTargetLanguage targetLanguage;
	public GlslangTargetLanguageVersion targetLanguageVersion;
	public int code;
	public int defaultVersion;
	public GlslangProfile defaultProfile;
	public boolean forceDefaultVersionAndProfile;
	public boolean forwardCompatible;
	/**
	 * {@link GlslangMessages}
	 */
	public int messages;
	/**
	 * {@link ResourceLimits}
	 */
	public int resource;
	public GlslIncludeCallbacks callbacks;
	public int callbacksCtx;

	public static int sizeof() {
		return 68;
	}

	public void write(GlslangInterface glslangInterface, int dst) {
		glslangInterface.store_i32(dst, this.language.ordinal());
		glslangInterface.store_i32(dst, this.stage.ordinal(), 4);
		glslangInterface.store_i32(dst, this.client.ordinal(), 8);
		glslangInterface.store_i32(dst, this.clientVersion.value, 12);
		glslangInterface.store_i32(dst, this.targetLanguage.ordinal(), 16);
		glslangInterface.store_i32(dst, this.targetLanguageVersion.value, 20);
		glslangInterface.store_i32(dst, this.code, 24);
		glslangInterface.store_i32(dst, this.defaultVersion, 28);
		glslangInterface.store_i32(dst, this.defaultProfile.value, 32);
		glslangInterface.store_i32(dst, this.forceDefaultVersionAndProfile ? 1 : 0, 36);
		glslangInterface.store_i32(dst, this.forwardCompatible ? 1 : 0, 40);
		glslangInterface.store_i32(dst, this.messages, 44);
		glslangInterface.store_i32(dst, this.resource, 48);
		if (this.callbacks != null) {
			glslangInterface.store_i32(dst, this.callbacks.includeSystem, 52);
			glslangInterface.store_i32(dst, this.callbacks.includeLocal, 56);
			glslangInterface.store_i32(dst, this.callbacks.freeIncludeResult, 60);
		} else {
			glslangInterface.store_i32(dst, 0, 52);
			glslangInterface.store_i32(dst, 0, 56);
			glslangInterface.store_i32(dst, 0, 60);
		}
		glslangInterface.store_i32(dst, this.callbacksCtx, 64);
	}
}
