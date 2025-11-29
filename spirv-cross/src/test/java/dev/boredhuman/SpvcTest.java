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
import dev.boredhuman.spvc.SpvcBinary;
import dev.boredhuman.spvc.SpvcFactory;
import dev.boredhuman.spvc.SpvcImportsImpl;
import dev.boredhuman.spvc.SpvcInterface;
import dev.boredhuman.spvc.wrapper.SpvcBackend;
import dev.boredhuman.spvc.wrapper.SpvcCaptureMode;
import dev.boredhuman.spvc.wrapper.SpvcCompilerOption;

public class SpvcTest {
	public static String BASIC_SHADER = "#version 150\n" +
		"\n" +
		"uniform mat4 projMatrix;\n" +
		"uniform mat4 modelViewMatrix;\n" +
		"\n" +
		"vec3 pos;\n" +
		"\n" +
		"void main() {\n" +
		"	gl_Position = projMatrix * modelViewMatrix * vec4(pos, 1.0);;\n" +
		"}\n";

	public static void main(String[] args) throws Throwable {
		SpvcBinary spvcBinary = SpvcBinary.create();

		SpvcInterface spvcInterface = SpvcFactory.createInstance(spvcBinary);

		spvcInterface.setWasmImports(new SpvcImportsImpl(spvcInterface));

		spvcInterface._initialize();

		int[] spirvBinary = SpvcTest.createSpirvBinary();

		int spirvBinaryMemory = spvcInterface.malloc(spirvBinary.length * 4);
		for (int i = 0; i < spirvBinary.length; i++) {
			spvcInterface.store_i32(spirvBinaryMemory, spirvBinary[i], i * 4);
		}

		int contextPtr = spvcInterface.malloc(4);
		spvcInterface.spvc_context_create(contextPtr);

		int irPtr = spvcInterface.malloc(4);
		spvcInterface.spvc_context_parse_spirv(spvcInterface.load_i32(contextPtr), spirvBinaryMemory, spirvBinary.length, irPtr);

		int compilerPtr = spvcInterface.malloc(4);
		spvcInterface.spvc_context_create_compiler(
			spvcInterface.load_i32(contextPtr), SpvcBackend.SPVC_BACKEND_HLSL.ordinal(), spvcInterface.load_i32(irPtr), SpvcCaptureMode.SPVC_CAPTURE_MODE_TAKE_OWNERSHIP.ordinal(), compilerPtr
		);

		int optionsPtr = spvcInterface.malloc(4);
		spvcInterface.spvc_compiler_create_compiler_options(spvcInterface.load_i32(compilerPtr), optionsPtr);

		spvcInterface.spvc_compiler_options_set_uint(spvcInterface.load_i32(optionsPtr), SpvcCompilerOption.SPVC_COMPILER_OPTION_HLSL_SHADER_MODEL.value, 50);
		spvcInterface.spvc_compiler_options_set_bool(spvcInterface.load_i32(optionsPtr), SpvcCompilerOption.SPVC_COMPILER_OPTION_FIXUP_DEPTH_CONVENTION.value, 1);

		spvcInterface.spvc_compiler_install_compiler_options(spvcInterface.load_i32(compilerPtr), spvcInterface.load_i32(optionsPtr));

		int outputPtr = spvcInterface.malloc(4);
		spvcInterface.spvc_compiler_compile(spvcInterface.load_i32(compilerPtr), outputPtr);

		String hlslVertShader = dev.boredhuman.spvc.Util.readCString(spvcInterface, spvcInterface.load_i32(outputPtr));

		System.out.println("Successfully compiled to GLSL to HLSL");
		System.out.println(hlslVertShader);

		spvcInterface.spvc_context_destroy(spvcInterface.load_i32(contextPtr));

		spvcInterface.free(outputPtr);
		spvcInterface.free(optionsPtr);
		spvcInterface.free(compilerPtr);
		spvcInterface.free(irPtr);
		spvcInterface.free(contextPtr);
		spvcInterface.free(spirvBinaryMemory);

		spvcInterface.heap_free();
	}

	public static int[] createSpirvBinary() throws Throwable {
		GlslangBinary glslangBinary = GlslangBinary.create();

		GlslangInterface glslangInterface = GlslangFactory.createInstance(glslangBinary);

		glslangInterface.setWasmImports(new GlslangImportsImpl(glslangInterface));

		glslangInterface._initialize();

		return SpvcTest.parse(glslangInterface, SpvcTest.BASIC_SHADER, GlslangStage.GLSLANG_STAGE_VERTEX);
	}

	public static int[] parse(GlslangInterface glslangInterface, String shader, GlslangStage stage) {
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
			return null;
		}

		if (glslangInterface.glslang_shader_parse(glslangShader, glslangInputMemory) == Util.FALSE) {
			System.out.printf("failed to parse shader %s\n", Util.readCString(glslangInterface, glslangInterface.glslang_shader_get_info_log(glslangShader)));
			return null;
		}

		int program = glslangInterface.glslang_program_create();

		glslangInterface.glslang_program_add_shader(program, glslangShader);

		if (glslangInterface.glslang_program_link(program, GlslangMessages.GLSLANG_MSG_DEFAULT_BIT) == Util.FALSE) {
			System.out.printf("failed to link %s\n", Util.readCString(glslangInterface, glslangInterface.glslang_program_get_info_log(program)));
			return null;
		}

		glslangInterface.glslang_program_SPIRV_generate(program, stage.ordinal());

		int wordCount = glslangInterface.glslang_program_SPIRV_get_size(program);
		int ptr = glslangInterface.glslang_program_SPIRV_get_ptr(program);

		int[] spirv = new int[wordCount];

		for (int i = 0; i < wordCount; i++) {
			spirv[i] = glslangInterface.load_i32(ptr, i * 4);
		}

		glslangInterface.free(glslangInput.code);
		glslangInterface.free(resourceLimitsMemory);
		glslangInterface.free(glslangInputMemory);
		glslangInterface.glslang_shader_delete(glslangShader);
		glslangInterface.glslang_program_delete(program);

		return spirv;
	}
}
