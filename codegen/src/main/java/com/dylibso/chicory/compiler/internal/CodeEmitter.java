package com.dylibso.chicory.compiler.internal;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.ValType;
import dev.boredhuman.exceptions.TrapException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckMethodAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeEmitter {
	static void emit(CompilerFast.EmitContext emitContext, InstructionAdapter instructionAdapter, CompilerInstruction instruction) {
		CompilerOpCode opcode = instruction.opcode();
		switch (opcode) {
			case I32_CONST:
				instructionAdapter.iconst((int) instruction.operand(0));
				break;
			case I64_CONST:
				instructionAdapter.lconst(instruction.operand(0));
				break;
			case F32_CONST:
				instructionAdapter.fconst(Float.intBitsToFloat((int) instruction.operand(0)));
				break;
			case F64_CONST:
				instructionAdapter.dconst(Double.longBitsToDouble(instruction.operand(0)));
				break;
			case I32_ADD:
				instructionAdapter.add(Type.INT_TYPE);
				break;
			case I64_ADD:
				instructionAdapter.add(Type.LONG_TYPE);
				break;
			case F32_ADD:
				instructionAdapter.add(Type.FLOAT_TYPE);
				break;
			case F64_ADD:
				instructionAdapter.add(Type.DOUBLE_TYPE);
				break;
			case I32_SUB:
				instructionAdapter.sub(Type.INT_TYPE);
				break;
			case I64_SUB:
				instructionAdapter.sub(Type.LONG_TYPE);
				break;
			case F32_SUB:
				instructionAdapter.sub(Type.FLOAT_TYPE);
				break;
			case F64_SUB:
				instructionAdapter.sub(Type.DOUBLE_TYPE);
				break;
			case I32_MUL:
				instructionAdapter.mul(Type.INT_TYPE);
				break;
			case I64_MUL:
				instructionAdapter.mul(Type.LONG_TYPE);
				break;
			case F32_MUL:
				instructionAdapter.mul(Type.FLOAT_TYPE);
				break;
			case F64_MUL:
				instructionAdapter.mul(Type.DOUBLE_TYPE);
				break;
			case I32_AND:
				instructionAdapter.and(Type.INT_TYPE);
				break;
			case I64_AND:
				instructionAdapter.and(Type.LONG_TYPE);
				break;
			case I32_OR:
				instructionAdapter.or(Type.INT_TYPE);
				break;
			case I64_OR:
				instructionAdapter.or(Type.LONG_TYPE);
				break;
			case I32_XOR:
				instructionAdapter.xor(Type.INT_TYPE);
				break;
			case I64_XOR:
				instructionAdapter.xor(Type.LONG_TYPE);
				break;
			case GLOBAL_GET: {
				int globalIndex = (int) instruction.operand(0);
				Global global = emitContext.globalSection.getGlobal(globalIndex);
				instructionAdapter.getstatic(emitContext.ownKlass, "global_" + globalIndex, CompilerFast.getType(global.valueType()).getDescriptor());
			}
			break;
			case GLOBAL_SET: {
				int globalIndex = (int) instruction.operand(0);
				Global global = emitContext.globalSection.getGlobal(globalIndex);
				instructionAdapter.putstatic(emitContext.ownKlass, "global_" + globalIndex, CompilerFast.getType(global.valueType()).getDescriptor());
			}
			break;
			case TRAP:
				instructionAdapter.anew(Type.getType(TrapException.class));
				instructionAdapter.dup();
				instructionAdapter.invokespecial(Type.getInternalName(TrapException.class), "<init>", "()V", false);
				instructionAdapter.athrow();
				break;
			case LOCAL_GET: {
				int localIndex = (int) instruction.operand(0);
				ValType localType = CompilerFast.getLocalValType(emitContext, instruction);
				int localOffset = emitContext.localOffsets[localIndex];
				instructionAdapter.load(localOffset, CompilerFast.getType(localType));
			}
			break;
			case LOCAL_TEE:
			case LOCAL_SET: {
				int localIndex = (int) instruction.operand(0);
				ValType localType = CompilerFast.getLocalValType(emitContext, instruction);
				if (opcode == CompilerOpCode.LOCAL_TEE) {
					if (CompilerFast.valTypeSize(localType) == 2) {
						instructionAdapter.dup2();
					} else {
						instructionAdapter.dup();
					}
				}
				int localOffset = emitContext.localOffsets[localIndex];
				instructionAdapter.store(localOffset, CompilerFast.getType(localType));
			}
			break;
			case F32_ABS:
				instructionAdapter.invokestatic("java/lang/Math", "abs", "(F)F", false);
				break;
			case F64_ABS:
				instructionAdapter.invokestatic("java/lang/Math", "abs", "(D)D", false);
				break;
			case F32_DIV:
				instructionAdapter.div(Type.FLOAT_TYPE);
				break;
			case F64_DIV:
				instructionAdapter.div(Type.DOUBLE_TYPE);
				break;
			case I32_DIV_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_DIV_U", "(II)I", false);
				break;
			case I32_DIV_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_DIV_S", "(II)I", false);
				break;
			case I64_DIV_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_DIV_U", "(JJ)J", false);
				break;
			case I64_DIV_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_DIV_S", "(JJ)J", false);
				break;
			case F32_CONVERT_I32_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F32_CONVERT_I32_U", "(I)F", false);
				break;
			case F64_CONVERT_I32_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_CONVERT_I32_U", "(I)D", false);
				break;
			case F64_CONVERT_I64_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_CONVERT_I64_U", "(J)D", false);
				break;
			case F64_CONVERT_I32_S:
				instructionAdapter.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
				break;
			case F32_CONVERT_I32_S:
				instructionAdapter.cast(Type.INT_TYPE, Type.FLOAT_TYPE);
				break;
			case F64_CONVERT_I64_S:
				instructionAdapter.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
				break;
			case F32_DEMOTE_F64:
				instructionAdapter.cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
				break;
			case F64_PROMOTE_F32:
				instructionAdapter.cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
				break;
			case RETURN:
				Type retType = emitContext.methodDescriptor.returns().stream().findFirst().map(CompilerFast::getType).orElse(Type.VOID_TYPE);
				instructionAdapter.areturn(retType);
				break;
			case F32_CEIL:
				instructionAdapter.cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
				instructionAdapter.invokestatic("java/lang/Math", "ceil", "(D)D", false);
				instructionAdapter.cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
				break;
			case F64_CEIL:
				instructionAdapter.invokestatic("java/lang/Math", "ceil", "(D)D", false);
				break;
			case F64_SQRT:
				instructionAdapter.invokestatic("java/lang/Math", "sqrt", "(D)D", false);
				break;
			case F64_FLOOR:
				instructionAdapter.invokestatic("java/lang/Math", "floor", "(D)D", false);
				break;
			case I32_TRUNC_SAT_F32_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_TRUNC_SAT_F32_U", "(F)I", false);
				break;
			case I32_TRUNC_SAT_F64_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_TRUNC_SAT_F64_U", "(D)I", false);
				break;
			case I32_TRUNC_SAT_F64_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_TRUNC_SAT_F64_S", "(D)I", false);
				break;
			case I64_TRUNC_SAT_F64_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_TRUNC_SAT_F64_U", "(D)J", false);
				break;
			case I64_TRUNC_SAT_F64_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_TRUNC_SAT_F64_S", "(D)J", false);
				break;
			case I32_POPCNT:
				instructionAdapter.invokestatic("java/lang/Integer", "bitCount", "(I)I", false);
				break;
			case I32_REINTERPRET_F32:
				instructionAdapter.invokestatic("java/lang/Float", "floatToRawIntBits", "(F)I", false);
				break;
			case F32_REINTERPRET_I32:
				instructionAdapter.invokestatic("java/lang/Float", "intBitsToFloat", "(I)F", false);
				break;
			case I64_REINTERPRET_F64:
				instructionAdapter.invokestatic("java/lang/Double", "doubleToRawLongBits", "(D)J", false);
				break;
			case F64_REINTERPRET_I64:
				instructionAdapter.invokestatic("java/lang/Double", "longBitsToDouble", "(J)D", false);
				break;
			case F64_NEG:
				instructionAdapter.neg(Type.DOUBLE_TYPE);
				break;
			case I32_CLZ:
				instructionAdapter.invokestatic("java/lang/Integer", "numberOfLeadingZeros", "(I)I", false);
				break;
			case I64_CLZ:
				instructionAdapter.invokestatic("java/lang/Long", "numberOfLeadingZeros", "(J)J", false);
				break;
			case I32_CTZ:
				instructionAdapter.invokestatic("java/lang/Integer", "numberOfTrailingZeros", "(I)I", false);
				break;
			case I64_CTZ:
				instructionAdapter.invokestatic("java/lang/Long", "numberOfTrailingZeros", "(J)I", false);
				instructionAdapter.cast(Type.INT_TYPE, Type.LONG_TYPE);
				break;
			case I32_ROTL:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_ROTL", "(II)I", false);
				break;
			case I64_ROTL:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_ROTL", "(JJ)J", false);
				break;
			case I32_SHR_U:
				instructionAdapter.ushr(Type.INT_TYPE);
				break;
			case I64_SHR_U:
				instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE);
				instructionAdapter.ushr(Type.LONG_TYPE);
				break;
			case I32_SHR_S:
				instructionAdapter.shr(Type.INT_TYPE);
				break;
			case I64_SHR_S:
				instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE);
				instructionAdapter.shr(Type.LONG_TYPE);
				break;
			case I32_SHL:
				instructionAdapter.shl(Type.INT_TYPE);
				break;
			case I64_SHL:
				instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE);
				instructionAdapter.shl(Type.LONG_TYPE);
				break;
			case I32_EXTEND_8_S:
				instructionAdapter.cast(Type.INT_TYPE, Type.BYTE_TYPE);
				instructionAdapter.cast(Type.BYTE_TYPE, Type.INT_TYPE);
				break;
			case I32_EXTEND_16_S:
				instructionAdapter.cast(Type.INT_TYPE, Type.SHORT_TYPE);
				instructionAdapter.cast(Type.SHORT_TYPE, Type.INT_TYPE);
				break;
			case I64_EXTEND_I32_U:
				instructionAdapter.invokestatic("java/lang/Integer", "toUnsignedLong", "(I)J", false);
				break;
			case I64_EXTEND_32_S:
				instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE);
				instructionAdapter.cast(Type.INT_TYPE, Type.LONG_TYPE);
				break;
			case I64_EXTEND_I32_S:
				instructionAdapter.cast(Type.INT_TYPE, Type.LONG_TYPE);
				break;
			case F64_COPYSIGN:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_COPYSIGN", "(DD)D", false);
				break;
			case I32_REM_S:
				instructionAdapter.rem(Type.INT_TYPE);
				break;
			case I32_REM_U:
				instructionAdapter.invokestatic("java/lang/Integer", "remainderUnsigned", "(II)I", false);
				break;
			case I64_REM_S:
				instructionAdapter.rem(Type.LONG_TYPE);
				break;
			case I64_REM_U:
				instructionAdapter.invokestatic("java/lang/Long", "remainderUnsigned", "(JJ)J", false);
				break;
			case I32_LOAD: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_i32", "(I)I", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_i32", "(II)I", false);
				}
			}
				break;
			case I64_LOAD: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_i64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_i64", "(II)J", false);
				}
			}
				break;
			case F32_LOAD: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_f32", "(I)F", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_f32", "(II)F", false);
				}
			}
				break;
			case F64_LOAD: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_f64", "(I)D", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_f64", "(II)D", false);
				}
			}
				break;
			case I32_LOAD8_S: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s8", "(I)I", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s8", "(II)I", false);
				}
			}
				break;
			case I32_LOAD8_U: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u8", "(I)I", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u8", "(II)I", false);
				}
			}
				break;
			case I32_LOAD16_S: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s16", "(I)I", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s16", "(II)I", false);
				}
			}
				break;
			case I32_LOAD16_U: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u16", "(I)I", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u16", "(II)I", false);
				}
			}
				break;
			case I64_LOAD8_S: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s8_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s8_64", "(II)J", false);
				}
			}
				break;
			case I64_LOAD8_U: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u8_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u8_64", "(II)J", false);
				}
			}
				break;
			case I64_LOAD16_S: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s16_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s16_64", "(II)J", false);
				}
			}
				break;
			case I64_LOAD16_U: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u16_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u16_64", "(II)J", false);
				}
			}
				break;
			case I64_LOAD32_S: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s32_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_s32_64", "(II)J", false);
				}
			}
				break;
			case I64_LOAD32_U: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u32_64", "(I)J", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "load_u32_64", "(II)J", false);
				}
			}
				break;
			case F32_STORE: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_f32", "(IF)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_f32", "(IFI)V", false);
				}
			}
				break;
			case F64_STORE: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_f64", "(ID)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_f64", "(IDI)V", false);
				}
			}
				break;
			case I32_STORE8: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i8", "(II)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i8", "(III)V", false);
				}
			}
				break;
			case I32_STORE16: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i16", "(II)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i16", "(III)V", false);
				}
			}
				break;
			case I32_STORE: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i32", "(II)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i32", "(III)V", false);
				}
			}
				break;
			case I64_STORE8: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i8", "(IJ)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i8", "(IJI)V", false);
				}
			}
				break;
			case I64_STORE16: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i16", "(IJ)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i16", "(IJI)V", false);
				}
			}
				break;
			case I64_STORE32: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i32", "(IJ)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i32", "(IJI)V", false);
				}
			}
				break;
			case I64_STORE: {
				int offset = (int) instruction.operand(1);
				if (offset == 0) {
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i64", "(IJ)V", false);
				} else {
					instructionAdapter.iconst(offset);
					instructionAdapter.invokestatic(emitContext.ownKlass, "store_i64", "(IJI)V", false);
				}
			}
				break;
			case I32_WRAP_I64:
				instructionAdapter.cast(Type.LONG_TYPE, Type.INT_TYPE);
				break;
			case MEMORY_GROW:
				instructionAdapter.invokestatic(emitContext.ownKlass, "memory_grow", "(I)I", false);
				break;
			case MEMORY_SIZE:
				instructionAdapter.invokestatic(emitContext.ownKlass, "memory_size", "()I", false);
				break;
			case MEMORY_FILL:
				instructionAdapter.invokestatic(emitContext.ownKlass, "memory_fill", "(III)V", false);
				break;
			case MEMORY_COPY:
				instructionAdapter.invokestatic(emitContext.ownKlass, "memory_copy", "(III)V", false);
				break;
			case DROP: {
				int operandOpcode = (int) instruction.operand(0);
				if (CompilerFast.valTypeSize(operandOpcode) == 2) {
					instructionAdapter.pop2();
				} else {
					instructionAdapter.pop();
				}
			}
				break;
			case DROP_KEEP: {
				int keepStart = (int) instruction.operand(0) + 1;

				// save result values
				int slot = emitContext.calculateFirstUnusedSlot();
				for (int i = instruction.operandCount() - 1; i >= keepStart; i--) {
					int operandOpcode = (int) instruction.operand(i);
					instructionAdapter.store(slot, CompilerFast.getType(operandOpcode));
					slot += CompilerFast.valTypeSize(operandOpcode);
				}

				// drop intervening values
				for (int i = keepStart - 1; i >= 1; i--) {
					int operandOpcode = (int) instruction.operand(i);
					if (CompilerFast.valTypeSize(operandOpcode) == 2) {
						instructionAdapter.pop2();
					} else {
						instructionAdapter.pop();
					}
				}

				// restore result values
				for (int i = keepStart; i < instruction.operandCount(); i++) {
					int operandOpcode = (int) instruction.operand(i);
					slot -= CompilerFast.valTypeSize(operandOpcode);
					instructionAdapter.load(slot, CompilerFast.getType(operandOpcode));
				}
			}
				break;
			case F32_GT:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F32_GT", "(FF)I", false);
				break;
			case F32_EQ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F32_EQ", "(FF)I", false);
				break;
			case F64_EQ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_EQ", "(DD)I", false);
				break;
			case F64_GE:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_GE", "(DD)I", false);
				break;
			case F64_GT:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_GT", "(DD)I", false);
				break;
			case F64_LT:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_LT", "(DD)I", false);
				break;
			case F32_NE:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F32_NE", "(FF)I", false);
				break;
			case F64_NE:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F64_NE", "(DD)I", false);
				break;
			case I32_EQ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_EQ", "(II)I", false);
				break;
			case I32_EQZ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_EQZ", "(I)I", false);
				break;
			case I32_GE_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_GE_S", "(II)I", false);
				break;
			case I32_GE_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_GE_U", "(II)I", false);
				break;
			case I32_GT_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_GT_S", "(II)I", false);
				break;
			case I32_GT_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_GT_U", "(II)I", false);
				break;
			case I32_LE_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_LE_S", "(II)I", false);
				break;
			case I32_LE_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_LE_U", "(II)I", false);
				break;
			case I32_LT_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_LT_S", "(II)I", false);
				break;
			case I32_LT_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_LT_U", "(II)I", false);
				break;
			case F32_LT:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "F32_LT", "(FF)I", false);
				break;
			case I32_NE:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I32_NE", "(II)I", false);
				break;
			case I64_EQ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_EQ", "(JJ)I", false);
				break;
			case I64_EQZ:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_EQZ", "(J)I", false);
				break;
			case I64_GE_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_GE_S", "(JJ)I", false);
				break;
			case I64_GE_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_GE_U", "(JJ)I", false);
				break;
			case I64_GT_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_GT_S", "(JJ)I", false);
				break;
			case I64_GT_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_GT_U", "(JJ)I", false);
				break;
			case I64_LE_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_LE_S", "(JJ)I", false);
				break;
			case I64_LE_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_LE_U", "(JJ)I", false);
				break;
			case I64_LT_S:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_LT_S", "(JJ)I", false);
				break;
			case I64_LT_U:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_LT_U", "(JJ)I", false);
				break;
			case I64_NE:
				instructionAdapter.invokestatic("com/dylibso/chicory/runtime/OpcodeImpl", "I64_NE", "(JJ)I", false);
				break;
			case SELECT:
				int instructionOpcode = (int) instruction.operand(0);
				Label endLabel = new Label();
				instructionAdapter.ifne(endLabel);
				if (CompilerFast.valTypeSize(instructionOpcode) == 1) {
					instructionAdapter.swap();
				} else {
					instructionAdapter.dup2X2();
					instructionAdapter.pop2();
				}
				instructionAdapter.mark(endLabel);
				if (CompilerFast.valTypeSize(instructionOpcode) == 2) {
					instructionAdapter.pop2();
				} else {
					instructionAdapter.pop();
				}
				break;
			case CALL: {
				int funcId = (int) instruction.operand(0);
				FunctionType functionType = emitContext.functionTypes.get(funcId);

				String methodDescriptor = CompilerFast.getMethodDescriptor(functionType);
				instructionAdapter.invokestatic(emitContext.ownKlass, "func_" + funcId, methodDescriptor, false);
			}
			break;
			case CALL_INDIRECT: {
				int tableIndex = (int) instruction.operand(1);
				if (tableIndex > 0) {
					throw new RuntimeException("Support multi table");
				}
				int typeIndex = (int) instruction.operand(0);
				FunctionType functionType = emitContext.types[typeIndex];

				String methodDescriptor = CompilerFast.getMethodDescriptor(functionType);

				Type methodType = Type.getMethodType(methodDescriptor);
				Type[] args = Arrays.copyOf(methodType.getArgumentTypes(), methodType.getArgumentCount() + 1);
				// signature + table index
				args[args.length - 1] = Type.INT_TYPE;

				instructionAdapter.invokestatic(emitContext.ownKlass, "call_indirect_" + typeIndex, Type.getMethodDescriptor(methodType.getReturnType(), args), false);
			}
			break;
			default:
				CompilerFast.codes.add(opcode);
				break;
		}
	}

	static MethodNode emitFunction(List<FunctionType> functionTypes, FunctionType[] types, FunctionBody functionBody, FunctionType methodDescriptor, int index,
								   GlobalSection globalSection, List<CompilerInstruction> compilerInstructions) {
		String ownKlass = CompilerFast.COMPILED_MODULE;

		List<ValType> localTypes = functionBody.localTypes();

		MethodNode methodNode = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "func_" + index, CompilerFast.getMethodDescriptor(methodDescriptor), null, null);

		MethodVisitor mv = new CheckMethodAdapter(methodNode);

		InstructionAdapter instructionAdapter = new InstructionAdapter(mv);
		instructionAdapter.visitCode();

		CompilerFast.EmitContext emitContext = new CompilerFast.EmitContext();
		emitContext.functionTypes = functionTypes;
		emitContext.types = types;
		emitContext.ownKlass = ownKlass;
		emitContext.globalSection = globalSection;
		emitContext.methodDescriptor = methodDescriptor;
		emitContext.localTypes = localTypes;
		emitContext.localOffsets = CompilerFast.calculateLocalOffsets(methodDescriptor, localTypes);

		// populate locals
		int parameterCount = methodDescriptor.params().size();
		for (int i = 0, len = localTypes.size(); i < len; i++) {
			int localSlot = emitContext.localOffsets[i + parameterCount];
			ValType localType = localTypes.get(i);

			Type type = CompilerFast.getType(localType);

			switch (type.getSort()) {
				case Type.INT:
					instructionAdapter.iconst(0);
					break;
				case Type.LONG:
					instructionAdapter.lconst(0);
					break;
				case Type.FLOAT:
					instructionAdapter.fconst(0);
					break;
				case Type.DOUBLE:
					instructionAdapter.dconst(0);
					break;
				default:
					throw new RuntimeException("Unsupported type: " + type.getSort());
			}
			instructionAdapter.store(localSlot, type);
		}

		Map<Long, Label> labels = new HashMap<>();
		for (CompilerInstruction compilerInstruction : compilerInstructions) {
			for (long target : compilerInstruction.labelTargets()) {
				labels.put(target, new Label());
			}
		}

		Set<Long> visitedTargets = new HashSet<>();

		for (CompilerInstruction compilerInstruction : compilerInstructions) {
			switch (compilerInstruction.opcode()) {
				case LABEL:
					Label label = labels.get(compilerInstruction.operand(0));
					if (label != null) {
						instructionAdapter.mark(label);
						visitedTargets.add(compilerInstruction.operand(0));
					}
					break;
				case GOTO:
					instructionAdapter.goTo(labels.get(compilerInstruction.operand(0)));
					break;
				case IFEQ:
					instructionAdapter.ifeq(labels.get(compilerInstruction.operand(0)));
					break;
				case IFNE:
					if (visitedTargets.contains(compilerInstruction.operand(0))) {
						Label skip = new Label();
						instructionAdapter.ifeq(skip);
						instructionAdapter.goTo(labels.get(compilerInstruction.operand(0)));
						instructionAdapter.mark(skip);
					} else {
						instructionAdapter.ifne(labels.get(compilerInstruction.operand(0)));
					}
					break;
				case SWITCH:
					// table switch using the last entry of the table as the default
					Label[] table = new Label[compilerInstruction.operandCount() - 1];
					for (int i = 0; i < table.length; i++) {
						table[i] = labels.get(compilerInstruction.operand(i));
					}
					Label defaultLabel = labels.get(compilerInstruction.operand(table.length));
					instructionAdapter.tableswitch(0, table.length - 1, defaultLabel, table);
					break;
				case TRY_CATCH_BLOCK:
					throw new RuntimeException("Add try-catch-block support");
				default:
					CodeEmitter.emit(emitContext, instructionAdapter, compilerInstruction);
			}
		}

		return methodNode;
	}
}
