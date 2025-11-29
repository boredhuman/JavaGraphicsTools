package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.OpcodeImpl;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.ValType;
import dev.boredhuman.codegen.internal.ClassReaderAccess;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.CheckMethodAdapter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CompilerFast {
	static Set<CompilerOpCode> codes = new HashSet<>();
	static String WASM_IMPORTS;
	static String WASM_IMPORTS_TYPE;
	static String WASM_INTERFACE;
	static String WASM_INTERFACE_TYPE;
	static String COMPILED_MODULE;

	private static void setupPackages(String name) {
		String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

		CompilerFast.WASM_IMPORTS = String.format("dev/boredhuman/%s/%sImports", name, capitalized);
		CompilerFast.WASM_IMPORTS_TYPE = "L" + CompilerFast.WASM_IMPORTS + ";";

		CompilerFast.WASM_INTERFACE = String.format("dev/boredhuman/%s/%sInterface", name, capitalized);
		CompilerFast.WASM_INTERFACE_TYPE = "L" + CompilerFast.WASM_INTERFACE + ";";

		CompilerFast.COMPILED_MODULE = String.format("dev/boredhuman/%s/CompiledModule", name);
	}

	public static void generate(File wasmFile, String domain) throws Throwable {
		CompilerFast.setupPackages(domain);

		WasmModule wasmModule = Parser.parse(wasmFile);

		WasiOptions options = WasiOptions.builder().build();
		WasiPreview1 wasi = WasiPreview1.builder().withOptions(options)
			.build();

		ImportValues.Builder importValuesBuilder = ImportValues.builder()
			.addFunction(wasi.toHostFunctions())
			.addFunction(
				new HostFunction(
					"env",
					"__syscall_getcwd",
					FunctionType.of(Arrays.asList(ValType.I32, ValType.I32), Collections.singletonList(ValType.I32)),
					(instance, methodArgs) -> new long[] { 0L }
				)
			);

		Instance instance = Instance.builder(wasmModule)
			.withImportValues(importValuesBuilder.build())
			.withInitialize(true)
			.withStart(false)
			.withMemoryFactory(ByteArrayMemory::new)
			.build();

		wasi.close();

		CodeSection codeSection = wasmModule.codeSection();
		FunctionSection functionSection = wasmModule.functionSection();
		TypeSection typeSection = wasmModule.typeSection();

		int functionImports = wasmModule.importSection().count(ExternalType.FUNCTION);

		WasmAnalyzer wasmAnalyzer = new WasmAnalyzer(wasmModule);
		List<FunctionType> functionTypes = wasmAnalyzer.functionTypes();

		InputStream baseKlass = CompilerFast.class.getResourceAsStream("/dev/boredhuman/support/CompiledModule.class");
		ClassReader classReader = new ClassReader(baseKlass);
		ClassNode classNode = new ClassNode();

		CompilerFast.remapOwner(classReader, classNode, "dev/boredhuman/support/CompiledModule", String.format("dev/boredhuman/%s/CompiledModule", domain));

		ClassNode baseKlassCopy = new ClassNode();

		classReader.accept(baseKlassCopy, ClassReader.SKIP_FRAMES);

		// create globals
		for (int i = 0, len = wasmModule.globalSection().globalCount(); i < len; i++) {
			GlobalInstance globalInstance = instance.global(i);
			Global global = wasmModule.globalSection().getGlobal(i);
			ValType valType = global.valueType();

			Object initialValue;
			if (valType.id() == ValType.I32.id()) {
				initialValue = (int) globalInstance.getValue();
			} else {
				throw new RuntimeException("Add support for more initialization types");
			}

			classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "global_" + i, CompilerFast.getType(valType).getDescriptor(), null, initialValue));
		}

		MemorySection memorySection = wasmModule.memorySection().orElseThrow(RuntimeException::new);

		if (memorySection.memoryCount() > 1) {
			throw new RuntimeException("Support multi memory");
		}

		MemoryLimits limits = memorySection.getMemory(0).limits();
		int memorySize = limits.initialPages() * 65536;

		MethodNode memory = classNode.methods.stream().filter(e -> e.name.equals("memorySize")).findFirst().orElseThrow(RuntimeException::new);

		InstructionAdapter memoryHelper = new InstructionAdapter(memory);
		memory.instructions.clear();

		memoryHelper.iconst(memorySize);
		memoryHelper.areturn(Type.INT_TYPE);

		MethodNode staticInitMethod = classNode.methods.stream().filter(e -> e.name.equals("<clinit>")).findFirst().orElseThrow(RuntimeException::new);
		staticInitMethod.instructions.remove(staticInitMethod.instructions.getLast());

		InstructionAdapter initHelper = new InstructionAdapter(staticInitMethod);

		Map<Integer, Integer> counts = new HashMap<>();

		// populate memory
		for (int i = 0, len = wasmModule.dataSection().dataSegmentCount(); i < len; i++)
		{
			DataSegment dataSegment = wasmModule.dataSection().getDataSegment(i);

			if (!(dataSegment instanceof ActiveDataSegment activeDataSegment)) {
				throw new RuntimeException("Support data segment type " + dataSegment.getClass());
			}

			List<Instruction> instructions = activeDataSegment.offsetInstructions();
			if (instructions.size() > 1) {
				throw new RuntimeException("Support multi instruction offsets");
			}

			Instruction instruction = instructions.get(0);
			if (instruction.opcode() != OpCode.I32_CONST) {
				throw new RuntimeException("Support non i32 const offsets");
			}

			boolean nativeMemory = true;

			if (i == 0) {
				initHelper.getstatic("java/nio/charset/StandardCharsets", "ISO_8859_1", "Ljava/nio/charset/Charset;");
				initHelper.store(0, Type.getType("Ljava/nio/charset/Charset;"));
				if (!nativeMemory) {
					initHelper.getstatic(CompilerFast.COMPILED_MODULE, "MEMORY", "[B");
					initHelper.store(1, Type.getType(byte[].class));
				}
			}

			int offset = (int) instruction.operands()[0];
			byte[] data = activeDataSegment.data();

			counts.compute(data.length, (k, v) -> {
				if (v == null) {
					return 1;
				} else {
					return v + 1;
				}
			});

			// Use strings to load data into memory

			int strOffset = 0;
			do {
				int start = strOffset;
				int strSize = CompilerFast.createString(data, strOffset);
				strOffset += strSize;

				String wrapped = new String(data, start, strSize, StandardCharsets.ISO_8859_1);
				initHelper.visitLdcInsn(wrapped);
				initHelper.load(0, Type.getType("Ljava/nio/charset/Charset;"));
				initHelper.invokevirtual("java/lang/String", "getBytes", "(Ljava/nio/charset/Charset;)[B", false);

				if (!nativeMemory) {
					initHelper.iconst(0);
					initHelper.load(1, Type.getType(byte[].class));
					initHelper.iconst(offset + start);
					initHelper.iconst(strSize);
					initHelper.invokestatic("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
				} else {
					initHelper.iconst(offset + start);
					initHelper.iconst(strSize);
					initHelper.invokestatic(CompilerFast.COMPILED_MODULE, "arraycopy", "([BII)V", false);
				}
			} while (data.length != strOffset);
		}

		initHelper.areturn(Type.VOID_TYPE);

		classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "WASM_IMPORTS", CompilerFast.WASM_IMPORTS_TYPE, null, null));

		for (int i = 0, len = wasmModule.importSection().importCount(); i < len; i++) {
			Import anImport = wasmModule.importSection().getImport(i);

			if (!(anImport instanceof FunctionImport functionImport)) {
				throw new RuntimeException("Added support for other function imports");
			}

			FunctionType type = wasmModule.typeSection().getType(functionImport.typeIndex());
			String methodDescripter = CompilerFast.getMethodDescriptor(type);

			MethodNode importMethodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "func_" + i, methodDescripter, null, null);

			InstructionAdapter importHelper = new InstructionAdapter(importMethodNode);

			Type methodType = Type.getMethodType(methodDescripter);

			importHelper.getstatic(CompilerFast.COMPILED_MODULE, "WASM_IMPORTS", CompilerFast.WASM_IMPORTS_TYPE);

			int localOffset = 0;
			for (Type argType : methodType.getArgumentTypes()) {
				importHelper.load(localOffset, argType);
				localOffset += argType == Type.DOUBLE_TYPE || argType == Type.LONG_TYPE ? 2 : 1;
			}

			importHelper.invokeinterface(CompilerFast.WASM_IMPORTS, functionImport.name(), methodDescripter);
			importHelper.areturn(methodType.getReturnType());

			classNode.methods.add(importMethodNode);
		}

		// build methods
		for (int i = 0, len = codeSection.functionBodyCount(); i < len; i++) {
			FunctionBody functionBody = codeSection.getFunctionBody(i);
			FunctionType functionType = functionSection.getFunctionType(i, typeSection);

			List<CompilerInstruction> compilerInstructions = wasmAnalyzer.analyze(functionImports + i);

			MethodNode methodNode = CodeEmitter.emitFunction(functionTypes, wasmModule.typeSection().types(), functionBody, functionType, functionImports + i, wasmModule.globalSection(), compilerInstructions);
			classNode.methods.add(methodNode);
		}

		if (!CompilerFast.codes.isEmpty()) {
			System.out.println("Unimplemented Opcodes: ");
			for (CompilerOpCode code : CompilerFast.codes) {
				System.out.println(code.opcode().name());
			}
			throw new RuntimeException("Some opcodes have not been implemented");
		}

		// table index -> function ref
		List<Map.Entry<Integer, Integer>> functionTable = new ArrayList<>();

		TableInstance table = instance.table(0);
		for (int i = 0, len = table.size(); i < len; i++) {
			int ref = table.ref(i);

			functionTable.add(new AbstractMap.SimpleEntry<>(i, ref));
		}

		// function type -> fuction
		Map<Integer, List<Map.Entry<Integer, Integer>>> functionTypeToFunctions = functionTable.stream()
			.filter(i -> i.getValue() >= 0)
			.collect(Collectors.groupingBy(functionRef -> wasmModule.functionSection().getFunctionType(functionRef.getValue() - functionImports)));

		// generate call indirect methods
		for (Map.Entry<Integer, List<Map.Entry<Integer, Integer>>> typeIndexFunctionId : functionTypeToFunctions.entrySet()) {
			int functionTypeIndex = typeIndexFunctionId.getKey();
			FunctionType functionType = wasmModule.typeSection().getType(functionTypeIndex);
			List<Map.Entry<Integer, Integer>> functions = typeIndexFunctionId.getValue();

			Type innerFunctionType = Type.getMethodType(CompilerFast.getMethodDescriptor(functionType));
			Type callIndirectMethodType = innerFunctionType;
			Type[] methodArgs = callIndirectMethodType.getArgumentTypes();
			methodArgs = Arrays.copyOf(methodArgs, methodArgs.length + 1);
			methodArgs[methodArgs.length - 1] = Type.INT_TYPE;
			callIndirectMethodType = Type.getMethodType(callIndirectMethodType.getReturnType(), methodArgs);

			MethodNode methodNode = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "call_indirect_" + functionTypeIndex, callIndirectMethodType.getDescriptor(), null, null);

			InstructionAdapter instructionAdapter = new InstructionAdapter(new CheckMethodAdapter(methodNode));

			instructionAdapter.visitCode();

			Type[] argumentTypes = callIndirectMethodType.getArgumentTypes();
			int[] paramOffsets = new int[argumentTypes.length];
			int offset = 0;
			for (int i = 0, len = paramOffsets.length; i < len; i++) {
				Type type = argumentTypes[i];
				int typeSize = type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
				paramOffsets[i] += offset;
				offset += typeSize;
			}

			for (int i = 0, len = argumentTypes.length; i < len; i++) {
				instructionAdapter.load(paramOffsets[i], argumentTypes[i]);
			}

			int min = functions.stream().mapToInt(Map.Entry::getKey).min().orElseThrow(RuntimeException::new);
			int max = functions.stream().mapToInt(Map.Entry::getKey).max().orElseThrow(RuntimeException::new);

			functions.sort(Comparator.comparingInt(Map.Entry::getKey));

			Map<Integer, Label> functionToLabel = new IdentityHashMap<>();
			for (Map.Entry<Integer, Integer> function : functions) {
				functionToLabel.put(function.getValue(), new Label());
			}

			Map<Integer, Integer> indexToFuncTable = functions.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			Label defaultLabel = new Label();
			Label end = new Label();
			Label[] labels = IntStream.range(min, max + 1)
				.mapToObj(i -> Optional.ofNullable(functionToLabel.get(indexToFuncTable.get(i))).orElse(defaultLabel))
				.toArray(Label[]::new);
			instructionAdapter.tableswitch(min, max, defaultLabel, labels);

			List<AbstractMap.SimpleEntry<Integer, Label>> functionToLabelArray = functionToLabel.entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()))
				.sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getKey))
				.toList();

			for (AbstractMap.SimpleEntry<Integer, Label> funcRefLabel : functionToLabelArray) {
				int funcIndex = funcRefLabel.getKey();

				instructionAdapter.mark(funcRefLabel.getValue());
				instructionAdapter.invokestatic(CompilerFast.COMPILED_MODULE, "func_" + funcIndex, innerFunctionType.getDescriptor(), false);
				instructionAdapter.goTo(end);
			}

			for (int i = argumentTypes.length - 2; i >= 0; i--) {
				Type argType = argumentTypes[i];

				if (argType == Type.DOUBLE_TYPE || argType == Type.LONG_TYPE) {
					instructionAdapter.pop2();
				} else {
					instructionAdapter.pop();
				}
			}

			instructionAdapter.mark(defaultLabel);
			instructionAdapter.anew(Type.getType(RuntimeException.class));
			instructionAdapter.dup();
			instructionAdapter.load(paramOffsets[paramOffsets.length - 1], Type.INT_TYPE); // load index
			instructionAdapter.invokestatic(Type.getInternalName(String.class), "valueOf", "(I)Ljava/lang/String;", false);
			instructionAdapter.invokespecial(Type.getInternalName(RuntimeException.class), "<init>", "(Ljava/lang/String;)V", false);
			instructionAdapter.athrow();

			instructionAdapter.mark(end);
			instructionAdapter.areturn(innerFunctionType.getReturnType());

			classNode.methods.add(methodNode);
		}

		// inline helper methods
		Set<String> outerMethods = new HashSet<>();

		for (MethodNode methodNode : classNode.methods) {
			for (AbstractInsnNode inst : methodNode.instructions) {
				if (inst instanceof MethodInsnNode methodInsnNode) {
					if (methodInsnNode.owner.equals("com/dylibso/chicory/runtime/OpcodeImpl")) {
						outerMethods.add(methodInsnNode.name + methodInsnNode.desc);
						methodInsnNode.owner = CompilerFast.COMPILED_MODULE;
					}
				}
			}
		}

		InputStream opcodesImplStream = OpcodeImpl.class.getResourceAsStream("/com/dylibso/chicory/runtime/OpcodeImpl.class");

		ClassReader opcodesImplClassReader = new ClassReader(opcodesImplStream);

		ClassNode opcodesImplClassNode = new ClassNode();

		opcodesImplClassReader.accept(opcodesImplClassNode, ClassReader.SKIP_FRAMES);

		for (String method : outerMethods) {
			String methodName = method.substring(0, method.indexOf('('));
			String desc = method.substring(method.indexOf('('));

			MethodNode methodNode = opcodesImplClassNode.methods.stream()
				.filter(e -> e.name.equals(methodName) && e.desc.equals(desc))
				.findFirst()
				.orElseThrow(RuntimeException::new);

			MethodNode copy = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, methodName, desc, null, null);

			for (AbstractInsnNode instruction : methodNode.instructions) {
				if (instruction instanceof MethodInsnNode methodInst) {

					if (methodInst.owner.equals("com/dylibso/chicory/runtime/WasmRuntimeException")) {
						methodInst = new MethodInsnNode(methodInst.getOpcode(), "dev/boredhuman/exceptions/WasmRuntimeException", methodInst.name, methodInst.desc, methodInst.itf);
					}

					copy.instructions.add(methodInst);
				} else if (instruction instanceof TypeInsnNode typeInsnNode) {

					// standalone jar with no dependencies
					if (typeInsnNode.desc.equals("com/dylibso/chicory/runtime/WasmRuntimeException")) {
						typeInsnNode = new TypeInsnNode(Opcodes.NEW, "dev/boredhuman/exceptions/WasmRuntimeException");
					}

					copy.instructions.add(typeInsnNode);
				} else {
					copy.instructions.add(instruction);
				}
			}

			classNode.methods.add(copy);
		}

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

		classNode.accept(classWriter);

		CompilerFast.write(classWriter.toByteArray(), new File(wasmFile.getParentFile().getParentFile(), "target/gen"));

		// test memory initialization
		// byte[] data = classWriter.toByteArray();
		// AtomicReference<Class<?>> klass = new AtomicReference<>();
		// ClassLoader temp = new ClassLoader() {
		// 	{
		// 		klass.set(this.defineClass(null, data, 0, data.length));
		// 	}
		// };
		//
		// Field memory1 = klass.get().getDeclaredField("MEMORY");
		// byte[] o = (byte[]) memory1.get(null);
		//
		// ByteArrayMemory byteArrayMemory = (ByteArrayMemory) instance.memory();
		// Field buffer = ByteArrayMemory.class.getDeclaredField("buffer");
		// buffer.setAccessible(true);
		// byte[] o1 = (byte[]) buffer.get(byteArrayMemory);
		//
		// if (!Arrays.equals(o, o1)) {
		// 	throw new RuntimeException("Bad memory initialization");
		// }

		ClassWriter importsWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

		ClassNode importsClass = CompilerFast.createImportsClass(wasmModule);

		importsClass.accept(importsWriter);

		File classesDirectory = new File(wasmFile.getParentFile().getParentFile(), "target/classes");
		CompilerFast.write(importsWriter.toByteArray(), classesDirectory);

		ClassWriter exportsWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

		ClassNode exportsClass = CompilerFast.createExportsInterface(wasmModule, baseKlassCopy);

		exportsClass.accept(exportsWriter);

		CompilerFast.write(exportsWriter.toByteArray(), classesDirectory);
	}

	public static void write(byte[] classBytes, File dst) throws Throwable {
		ClassReader classReader = new ClassReader(classBytes);
		String className = classReader.getClassName();

		File classFile = new File(dst, className + ".class");

		File parentDir = classFile.getParentFile();
		if (!parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new RuntimeException("Failed to make directory " + parentDir);
			}
		}

		try (FileOutputStream fos = new FileOutputStream(classFile)) {
			fos.write(classBytes);
		}
	}

	public static ClassNode createImportsClass(WasmModule wasmModule) {
		ClassNode classNode = new ClassNode();
		classNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, CompilerFast.WASM_IMPORTS, null, "java/lang/Object", null);

		ImportSection importSection = wasmModule.importSection();

		for (int i = 0, len = importSection.importCount(); i < len; i++) {
			Import anImport = importSection.getImport(i);

			if (!(anImport instanceof FunctionImport functionImport)) {
				throw new RuntimeException("Support non function import");
			}

			FunctionType type = wasmModule.typeSection().getType(functionImport.typeIndex());

			MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, functionImport.name(), CompilerFast.getMethodDescriptor(type), null, null);

			classNode.methods.add(methodNode);
		}

		return classNode;
	}

	public static ClassNode createExportsInterface(WasmModule wasmModule, ClassNode baseKlass) {
		List<ExportFunctionGenData> exportFunctionGenDataList = new ArrayList<>();

		for (int i = 0, len = wasmModule.exportSection().exportCount(); i < len; i++) {
			Export export = wasmModule.exportSection().getExport(i);

			if (export.exportType() != ExternalType.FUNCTION) {
				continue;
			}

			// ignore C++ mangled name exports
			if (export.name().startsWith("_ZN")) {
				continue;
			}

			int index = export.index();

			FunctionType functionType = wasmModule.functionSection().getFunctionType(index - wasmModule.importSection().importCount(), wasmModule.typeSection());
			Type methodType = Type.getMethodType(CompilerFast.getMethodDescriptor(functionType));
			String funcName = "func_" + index;

			exportFunctionGenDataList.add(new ExportFunctionGenData(export.name(), funcName, methodType, export.name() + "_handle"));
		}

		// export method store / load functions
		for (MethodNode methodNode : baseKlass.methods) {
			if (!methodNode.name.contains("store") && !methodNode.name.contains("load") && !methodNode.name.contains("heap_free")) {
				continue;
			}

			String fieldName = methodNode.name + "_" + methodNode.desc.substring(1, methodNode.desc.indexOf(")"));

			ExportFunctionGenData exportFunctionGenData = new ExportFunctionGenData(methodNode.name, methodNode.name, Type.getMethodType(methodNode.desc), fieldName);
			exportFunctionGenData.localVariableNodes = methodNode.localVariables;
			for (LocalVariableNode localVariableNode : exportFunctionGenData.localVariableNodes) {
				localVariableNode.start = new LabelNode();
				localVariableNode.end = new LabelNode();
				localVariableNode.index += 1; // move from static to virtual so need to offset by one because 0 is 'this'
			}
			exportFunctionGenDataList.add(exportFunctionGenData);
		}

		ClassNode classNode = new ClassNode();
		classNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, CompilerFast.WASM_INTERFACE, null, "java/lang/Object", null);

		MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Class;)V", null, null);

		classNode.methods.add(constructor);

		InstructionAdapter constructorHelper = new InstructionAdapter(constructor);

		constructorHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
		constructorHelper.invokespecial("java/lang/Object", "<init>", "()V", false);
		constructorHelper.invokestatic("java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
		constructorHelper.store(3, Type.getType(MethodHandles.Lookup.class));

		for (ExportFunctionGenData exportFunctionGenData : exportFunctionGenDataList) {
			String exportName = exportFunctionGenData.exportName;
			Type methodType = exportFunctionGenData.methodType;

			// field fetch
			constructorHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
			constructorHelper.load(3, Type.getType(MethodHandles.Lookup.class));

			constructorHelper.load(1, Type.getType(Class.class)); // push class
			constructorHelper.visitLdcInsn(exportFunctionGenData.functionName); // push function name

			CompilerFast.loadClassType(methodType.getReturnType(), constructorHelper);

			constructorHelper.iconst(methodType.getArgumentCount()); // create array of arg types
			constructorHelper.newarray(Type.getType(Class.class));
			constructorHelper.store(2, Type.getType(Class[].class)); // store array in local 2

			Type[] argTypes = methodType.getArgumentTypes();

			// populate array of arg types
			for (int j = 0, argCount = methodType.getArgumentCount(); j < argCount; j++) {
				Type argType = argTypes[j];

				constructorHelper.load(2, Type.getType(Class[].class)); // load array
				constructorHelper.iconst(j); // load store index

				CompilerFast.loadClassType(argType, constructorHelper);

				constructorHelper.astore(Type.getType(Class.class));
			}

			constructorHelper.load(2, Type.getType(Class[].class)); // push args array

			constructorHelper.invokestatic(
				"java/lang/invoke/MethodType", "methodType",
				Type.getMethodDescriptor(Type.getType(MethodType.class), Type.getType(Class.class), Type.getType(Class[].class)),
				false
			); // push methodtype

			constructorHelper.invokevirtual(
				"java/lang/invoke/MethodHandles$Lookup", "findStatic",
				Type.getMethodDescriptor(Type.getType(MethodHandle.class), Type.getType(Class.class), Type.getType(String.class), Type.getType(MethodType.class)),
				false
			);

			constructorHelper.putfield(CompilerFast.WASM_INTERFACE, exportFunctionGenData.fieldName, "Ljava/lang/invoke/MethodHandle;");

			// method

			classNode.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, exportFunctionGenData.fieldName, "Ljava/lang/invoke/MethodHandle;", null, null);

			MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, exportName, methodType.getDescriptor(), null, null);
			methodNode.localVariables = exportFunctionGenData.localVariableNodes;
			InstructionAdapter methodHelper = new InstructionAdapter(methodNode);

			methodHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
			methodHelper.getfield(CompilerFast.WASM_INTERFACE, exportFunctionGenData.fieldName, "Ljava/lang/invoke/MethodHandle;");

			int localOffset = 1;
			for (int j = 0, argCount = methodType.getArgumentCount(); j < argCount; j++) {
				Type argType = argTypes[j];
				methodHelper.load(localOffset, argType);
				localOffset += argType == Type.DOUBLE_TYPE || argType == Type.LONG_TYPE ? 2 : 1;
			}

			methodHelper.invokevirtual("java/lang/invoke/MethodHandle", "invokeExact", methodType.getDescriptor(), false);
			methodHelper.areturn(methodType.getReturnType());

			classNode.methods.add(methodNode);
		}

		constructorHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
		constructorHelper.load(3, Type.getType(MethodHandles.Lookup.class));
		constructorHelper.load(1, Type.getType(Class.class));
		constructorHelper.visitLdcInsn("WASM_IMPORTS");
		constructorHelper.visitLdcInsn(Type.getObjectType(CompilerFast.WASM_IMPORTS));

		Type findStaticGetter = Type.getMethodType(Type.getType(MethodHandle.class), Type.getType(Class.class), Type.getType(String.class), Type.getType(Class.class));
		constructorHelper.invokevirtual("java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", findStaticGetter.getDescriptor(), false);
		constructorHelper.putfield(CompilerFast.WASM_INTERFACE, "wasmImport_handle_getter", "Ljava/lang/invoke/MethodHandle;");

		constructorHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
		constructorHelper.load(3, Type.getType(MethodHandles.Lookup.class));
		constructorHelper.load(1, Type.getType(Class.class));
		constructorHelper.visitLdcInsn("WASM_IMPORTS");
		constructorHelper.visitLdcInsn(Type.getObjectType(CompilerFast.WASM_IMPORTS));

		constructorHelper.invokevirtual("java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", findStaticGetter.getDescriptor(), false);
		constructorHelper.putfield(CompilerFast.WASM_INTERFACE, "wasmImport_handle_setter", "Ljava/lang/invoke/MethodHandle;");

		constructorHelper.areturn(Type.VOID_TYPE);

		classNode.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "wasmImport_handle_getter", "Ljava/lang/invoke/MethodHandle;", null, null);

		// wasm import getter
		MethodNode wasmImportsGetter = new MethodNode(Opcodes.ACC_PUBLIC, "getWasmImports", "()" + CompilerFast.WASM_IMPORTS_TYPE, null, null);
		InstructionAdapter wasmImportsGetterHelper = new InstructionAdapter(wasmImportsGetter);

		wasmImportsGetterHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
		wasmImportsGetterHelper.getfield(CompilerFast.WASM_INTERFACE, "wasmImport_handle_getter", "Ljava/lang/invoke/MethodHandle;");
		wasmImportsGetterHelper.invokevirtual("java/lang/invoke/MethodHandle", "invokeExact", "()" + CompilerFast.WASM_IMPORTS_TYPE, false);
		wasmImportsGetterHelper.areturn(Type.getType(CompilerFast.WASM_IMPORTS_TYPE));

		classNode.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "wasmImport_handle_setter", "Ljava/lang/invoke/MethodHandle;", null, null);

		// wasm import setter
		MethodNode wasmImportsSetter = new MethodNode(Opcodes.ACC_PUBLIC, "setWasmImports", String.format("(%s)V", CompilerFast.WASM_IMPORTS_TYPE), null, null);
		InstructionAdapter wasmImportsSetterHelper = new InstructionAdapter(wasmImportsSetter);

		wasmImportsSetterHelper.load(0, Type.getType(CompilerFast.WASM_INTERFACE_TYPE));
		wasmImportsSetterHelper.getfield(CompilerFast.WASM_INTERFACE, "wasmImport_handle_setter", "Ljava/lang/invoke/MethodHandle;");
		wasmImportsSetterHelper.load(1, Type.getType(CompilerFast.WASM_IMPORTS_TYPE));
		wasmImportsSetterHelper.invokevirtual("java/lang/invoke/MethodHandle", "invokeExact", String.format("(%s)V", CompilerFast.WASM_IMPORTS_TYPE), false);

		wasmImportsSetterHelper.areturn(Type.VOID_TYPE);

		classNode.methods.add(wasmImportsGetter);
		classNode.methods.add(wasmImportsSetter);

		return classNode;
	}

	public static String getMethodDescriptor(FunctionType type) {
		if (type.returns().size() > 1) {
			throw new RuntimeException("Support multi return");
		}

		return Type.getMethodDescriptor(type.returns().stream().map(CompilerFast::getType).findFirst().orElse(Type.VOID_TYPE),
			type.params().stream().map(CompilerFast::getType).toArray(Type[]::new));
	}

	public static Type getType(int opcode) {
		if (opcode == ValType.I32.opcode()) {
			return Type.INT_TYPE;
		} else if (opcode == ValType.I64.opcode()) {
			return Type.LONG_TYPE;
		} else if (opcode == ValType.F32.opcode()) {
			return Type.FLOAT_TYPE;
		} else if (opcode == ValType.F64.opcode()) {
			return Type.DOUBLE_TYPE;
		} else {
			throw new RuntimeException("Unsupported opcode: " + opcode);
		}
	}

	public static Type getType(ValType type) {
		if (type == null) {
			return Type.VOID_TYPE;
		} else if (type.opcode() == ValType.I32.opcode()) {
			return Type.INT_TYPE;
		} else if (type.opcode() == ValType.I64.opcode()) {
			return Type.LONG_TYPE;
		} else if (type.opcode() == ValType.F32.opcode()) {
			return Type.FLOAT_TYPE;
		} else if (type.opcode() == ValType.F64.opcode()) {
			return Type.DOUBLE_TYPE;
		} else {
			throw new RuntimeException("Unsupported type: " + type.name());
		}
	}

	static ValType getLocalValType(EmitContext emitContext, CompilerInstruction instruction) {
		int localIndex = (int) instruction.operand(0);
		ValType localType;
		if (localIndex < emitContext.methodDescriptor.params().size()) {
			localType = emitContext.methodDescriptor.params().get(localIndex);
		} else {
			localType = emitContext.localTypes.get(localIndex - emitContext.methodDescriptor.params().size());
		}
		return localType;
	}

	static int[] calculateLocalOffsets(FunctionType methodDescriptor, List<ValType> localTypes) {
		List<ValType> params = methodDescriptor.params();

		int[] offsets = new int[params.size() + localTypes.size()];

		int offset = 0;
		for (int i = 0; i < params.size(); i++) {
			offsets[i] = offset;
			offset += CompilerFast.valTypeSize(params.get(i));
		}

		for (int i = 0; i < localTypes.size(); i++) {
			offsets[i + params.size()] = offset;
			offset += CompilerFast.valTypeSize(localTypes.get(i));
		}

		return offsets;
	}

	public static int valTypeSize(int opcode) {
		if (opcode == ValType.I64.opcode() || opcode == ValType.F64.opcode()) {
			return 2;
		} else {
			return 1;
		}
	}

	public static int valTypeSize(ValType valType) {
		if (valType.opcode() == ValType.I64.opcode() || valType.opcode() == ValType.F64.opcode()) {
			return 2;
		} else {
			return 1;
		}
	}

	private static void loadClassType(Type type, InstructionAdapter instructionAdapter) {
		if (type == Type.INT_TYPE) {
			instructionAdapter.getstatic(Type.getInternalName(Integer.class), "TYPE", Type.getDescriptor(Class.class));
		} else if (type == Type.LONG_TYPE) {
			instructionAdapter.getstatic(Type.getInternalName(Long.class), "TYPE", Type.getDescriptor(Class.class));
		} else if (type == Type.FLOAT_TYPE) {
			instructionAdapter.getstatic(Type.getInternalName(Float.class), "TYPE", Type.getDescriptor(Class.class));
		} else if (type == Type.DOUBLE_TYPE) {
			instructionAdapter.getstatic(Type.getInternalName(Double.class), "TYPE", Type.getDescriptor(Class.class));
		} else if (type == Type.VOID_TYPE) {
			instructionAdapter.getstatic(Type.getInternalName(Void.class), "TYPE", Type.getDescriptor(Class.class));
		} else {
			throw new RuntimeException("Type not supported " + type);
		}
	}

	private static int createString(byte[] data, int offset) {
		int remaining = data.length - offset;
		int size = Math.min(65535, remaining);
		while (true) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(bos);

			try {
				dataOutputStream.writeUTF(new String(data, offset, size, StandardCharsets.ISO_8859_1));
				if (bos.size() > 65535 + 2) {
					size--;
				} else {
					return size;
				}
			} catch (Throwable err) {
				size--;
			}
		}
	}

	private static void remapOwner(ClassReader classReader, ClassNode classNodeDst, String test, String replacement) {
		classReader.accept(new ClassNode(), 0);

		String[] constants = ClassReaderAccess.getConstantUtf8ValuesHandle(classReader);

		for (int i = 0, len = constants.length; i < len; i++) {
			String constant = constants[i];

			if (constant == null) {
				continue;
			}

			if (constant.contains(test)) {
				constants[i] = constant.replace(test, replacement);
			}
		}

		classReader.accept(classNodeDst, 0);
	}

	public static class EmitContext {
		public List<FunctionType> functionTypes;
		public FunctionType[] types;

		public String ownKlass;
		public GlobalSection globalSection;
		public FunctionType methodDescriptor;
		public List<ValType> localTypes;
		public int[] localOffsets;

		public int calculateFirstUnusedSlot() {
			int offset = 0;
			if (!this.methodDescriptor.params().isEmpty()) {
				offset = CompilerFast.valTypeSize(this.methodDescriptor.params().get(0));
			}
			return this.localOffsets[this.localOffsets.length - 1] + offset;
		}
	}

	public static class ExportFunctionGenData {
		String exportName;
		String functionName;
		Type methodType;
		String fieldName;
		List<LocalVariableNode> localVariableNodes;

		public ExportFunctionGenData(String exportName, String functionName, Type methodType, String fieldName) {
			this.exportName = exportName;
			this.functionName = functionName;
			this.methodType = methodType;
			this.fieldName = fieldName;
		}
	}
}
