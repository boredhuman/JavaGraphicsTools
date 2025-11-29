package dev.boredhuman;

import dev.boredhuman.glslang.GlslangBinary;
import dev.boredhuman.glslang.GlslangFactory;
import dev.boredhuman.glslang.GlslangImportsImpl;
import dev.boredhuman.glslang.GlslangInterface;
import dev.boredhuman.glslang.Util;
import dev.boredhuman.glslang.wrapper.GlslangClient;
import dev.boredhuman.glslang.wrapper.GlslangInput;
import dev.boredhuman.glslang.wrapper.GlslangMessages;
import dev.boredhuman.glslang.wrapper.GlslangProfile;
import dev.boredhuman.glslang.wrapper.GlslangShaderOptions;
import dev.boredhuman.glslang.wrapper.GlslangSource;
import dev.boredhuman.glslang.wrapper.GlslangStage;
import dev.boredhuman.glslang.wrapper.GlslangTargetClientVersion;
import dev.boredhuman.glslang.wrapper.GlslangTargetLanguage;
import dev.boredhuman.glslang.wrapper.GlslangTargetLanguageVersion;
import dev.boredhuman.glslang.wrapper.ResourceLimits;

public class GlslangTest {

	public static String BASIC_SHADER = "#version 150\n" +
		"\n" +
		"void main() {\n" +
		"	gl_FragColor = vec4(1.0);\n" +
		"}\n";

	public static void main(String[] args) throws Throwable {
		GlslangBinary glslangBinary = GlslangBinary.create();

		GlslangInterface glslangInterface = GlslangFactory.createInstance(glslangBinary);

		glslangInterface.setWasmImports(new GlslangImportsImpl(glslangInterface));

		glslangInterface._initialize();

		if (!GlslangTest.parse(glslangInterface, GlslangTest.BASIC_SHADER, GlslangStage.GLSLANG_STAGE_FRAGMENT)) {
			throw new RuntimeException("Failed to parse!");
		}

		glslangInterface.heap_free();
	}

	public static boolean parse(GlslangInterface glslangInterface, String shader, GlslangStage stage) {
		GlslangInput glslangInput = new GlslangInput();
		glslangInput.language = GlslangSource.GLSLANG_SOURCE_GLSL;
		glslangInput.stage = stage;
		glslangInput.client = GlslangClient.GLSLANG_CLIENT_OPENGL;
		glslangInput.clientVersion = GlslangTargetClientVersion.GLSLANG_TARGET_OPENGL_450;
		glslangInput.targetLanguage = GlslangTargetLanguage.GLSLANG_TARGET_SPV;
		glslangInput.targetLanguageVersion = GlslangTargetLanguageVersion.GLSLANG_TARGET_SPV_1_0;
		glslangInput.code = Util.writeString(glslangInterface, shader);

		ResourceLimits resourceLimits = new ResourceLimits();
		int resourceLimitsMemory = glslangInterface.malloc(ResourceLimits.sizeof());
		resourceLimits.write(glslangInterface, resourceLimitsMemory);

		glslangInput.defaultVersion = 100;
		glslangInput.defaultProfile = GlslangProfile.GLSLANG_NO_PROFILE;
		glslangInput.forceDefaultVersionAndProfile = true;
		glslangInput.resource = resourceLimitsMemory;

		int glslangInputMemory = glslangInterface.malloc(GlslangInput.sizeof());
		glslangInput.write(glslangInterface, glslangInputMemory);

		int glslangShader = glslangInterface.glslang_shader_create(glslangInputMemory);

		glslangInterface.glslang_shader_set_options(glslangShader, GlslangShaderOptions.GLSLANG_SHADER_AUTO_MAP_LOCATIONS);
		glslangInterface.glslang_shader_set_glsl_version(glslangShader, 330);

		if (glslangInterface.glslang_shader_preprocess(glslangShader, glslangInputMemory) == Util.FALSE) {
			System.out.printf("failed to preprocess shader %s\n", Util.readCString(glslangInterface, glslangInterface.glslang_shader_get_info_log(glslangShader)));
			return false;
		}

		if (glslangInterface.glslang_shader_parse(glslangShader, glslangInputMemory) == Util.FALSE) {
			System.out.printf("failed to parse shader %s\n", Util.readCString(glslangInterface, glslangInterface.glslang_shader_get_info_log(glslangShader)));
			return false;
		}

		int program = glslangInterface.glslang_program_create();

		glslangInterface.glslang_program_add_shader(program, glslangShader);

		if (glslangInterface.glslang_program_link(program, GlslangMessages.GLSLANG_MSG_DEFAULT_BIT) == Util.FALSE) {
			System.out.printf("failed to link %s\n", Util.readCString(glslangInterface, glslangInterface.glslang_program_get_info_log(program)));
			return false;
		}

		glslangInterface.glslang_program_SPIRV_generate(program, stage.ordinal());

		int wordCount = glslangInterface.glslang_program_SPIRV_get_size(program);

		int i = glslangInterface.glslang_program_SPIRV_get_ptr(program);

		glslangInterface.free(glslangInput.code);
		glslangInterface.free(resourceLimitsMemory);
		glslangInterface.free(glslangInputMemory);
		glslangInterface.glslang_shader_delete(glslangShader);
		glslangInterface.glslang_program_delete(program);

		System.out.printf("word count: %d data ptr: %d\n", wordCount, i);

		return true;
	}
}
