/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.jni;

import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;
import static com.sun.max.vm.classfile.constant.SymbolTable.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A utility class for generating bytecode that implements the transition
 * from Java to native code. Most of these transitions will made for calling a native function via JNI.
 * However, faster transitions to MaxineVM specific native code is also supported.
 * The steps performed by a generated stub are:
 * <p>
 * <ol>
 *   <li>Record the {@linkplain JniHandles#top() top} of {@linkplain VmThread#jniHandles() the current thread's JNI handle stack}.</li>
 *   <li>Push the pointer to the {@linkplain VmThread#jniEnv() current thread's native JNI environment data structure}.</li>
 *   <li>If the native method is static, {@linkplain JniHandles#createStackHandle(Object) handlize} and push the class reference
 *       otherwise handlize and push the receiver reference.</li>
 *   <li>Push the remaining parameters, handlizing non-null references before they are pushed.</li>
 *   <li>Save last Java frame info (stack, frame and instruction pointers) from thread local storage (TLS) to
 *       local variables and then update the TLS info to reflect the frame of the native stub.
 *   <li>Invoke the native function via a Maxine VM specific bytecode which also handles resolving the native function.
 *       The native function symbol is generated by {@linkplain Mangle mangling} the name and signature of the native method appropriately.</li>
 *   <li>Set the last Java instruction pointer in TLS to zero to indicate transition back into Java code.
 *   <li>If the native method returns a reference, {@linkplain JniHandle#unhand() unwrap} the returned handle.</li>
 *   <li>Restore the JNI frame as recorded in the first step.</li>
 *   <li>Throw any {@linkplain VmThread#throwJniException() pending exception} (if any) for the current thread.</li>
 *   <li>Return the result to the caller.</li>
 * </ol>
 * <p>
 */
public final class NativeStubGenerator extends BytecodeAssembler {

    public NativeStubGenerator(ConstantPoolEditor constantPoolEditor, MethodActor classMethodActor) {
        super(constantPoolEditor);
        this.classMethodActor = classMethodActor;
        allocateParameters(classMethodActor.isStatic(), classMethodActor.descriptor());
        generateCode(classMethodActor.isCFunction(), classMethodActor.isStatic(), classMethodActor.holder(), classMethodActor.descriptor());
    }

    private final SeekableByteArrayOutputStream codeStream = new SeekableByteArrayOutputStream();
    private final MethodActor classMethodActor;

    @Override
    public void writeByte(byte b) {
        codeStream.write(b);
    }

    @Override
    protected void setWriteBCI(int bci) {
        codeStream.seek(bci);
    }

    @Override
    public byte[] code() {
        fixup();
        return codeStream.toByteArray();
    }

    public CodeAttribute codeAttribute() {
        return new CodeAttribute(constantPool(),
                                 code(),
                                 (char) maxStack(),
                                 (char) maxLocals(),
                                 CodeAttribute.NO_EXCEPTION_HANDLER_TABLE,
                                 LineNumberTable.EMPTY,
                                 LocalVariableTable.EMPTY,
                                 null);
    }

    /**
     * These methods may be called from a generated native stub.
     */
    private static final ClassMethodRefConstant jniEnv = createClassMethodConstant(VmThread.class, makeSymbol("jniEnv"));
    private static final ClassMethodRefConstant currentThread = createClassMethodConstant(VmThread.class, makeSymbol("current"));
    private static final ClassMethodRefConstant traceCurrentThreadPrefix = createClassMethodConstant(NativeStubGenerator.class, makeSymbol("traceCurrentThreadPrefix"));
    private static final ClassMethodRefConstant throwJniException = createClassMethodConstant(VmThread.class, makeSymbol("throwJniException"));
    private static final ClassMethodRefConstant createStackHandle = createClassMethodConstant(JniHandles.class, makeSymbol("createStackHandle"), Object.class);
    private static final ClassMethodRefConstant unhandHandle = createClassMethodConstant(JniHandle.class, makeSymbol("unhand"));
    private static final ClassMethodRefConstant handlesTop = createClassMethodConstant(VmThread.class, makeSymbol("jniHandlesTop"));
    private static final ClassMethodRefConstant resetHandlesTop = createClassMethodConstant(VmThread.class, makeSymbol("resetJniHandlesTop"), int.class);
    private static final ClassMethodRefConstant logPrintln_String = createClassMethodConstant(Log.class, makeSymbol("println"), String.class);
    private static final ClassMethodRefConstant logPrint_String = createClassMethodConstant(Log.class, makeSymbol("print"), String.class);
    private static final FieldRefConstant traceJNI = createFieldConstant(ClassMethodActor.class, makeSymbol("TraceJNI"));
    private static final ClassMethodRefConstant link = createClassMethodConstant(NativeStubGenerator.class, makeSymbol("link"));
    private static final ClassMethodRefConstant nativeToJava = createClassMethodConstant(NativeStubGenerator.class, makeSymbol("nativeToJava"));
    private static final ClassMethodRefConstant javaToNative = createClassMethodConstant(NativeStubGenerator.class, makeSymbol("javaToNative"));
    private static final StringConstant threadLabelPrefix = PoolConstantFactory.createStringConstant("[Thread \"");

    private void generateCode(boolean isCFunction, boolean isStatic, ClassActor holder, SignatureDescriptor signatureDescriptor) {
        final TypeDescriptor resultDescriptor = signatureDescriptor.resultDescriptor();
        final Kind resultKind = resultDescriptor.toKind();
        final StringBuilder nativeFunctionDescriptor = new StringBuilder("(");
        int nativeFunctionArgSlots = 0;
        final TypeDescriptor nativeResultDescriptor = resultKind.isReference ? JavaTypeDescriptor.JNI_HANDLE : resultDescriptor;

        int top = 0;

        int currentThread = -1;

        int parameterLocalIndex = 0;

        if (!isCFunction) {

            // Cache current thread in a local variable
            invokestatic(NativeStubGenerator.currentThread, 0, 1);
            currentThread = allocateLocal(Kind.REFERENCE);
            astore(currentThread);

            verboseJniEntry();

            // Save current JNI frame.
            top = allocateLocal(Kind.INT);
            aload(currentThread);
            invokevirtual(handlesTop, 1, 1);
            istore(top);

            // Push the JNI environment variable
            invokestatic(jniEnv, 0, 1);

            final TypeDescriptor jniEnvDescriptor = jniEnv.signature(constantPool()).resultDescriptor();
            nativeFunctionDescriptor.append(jniEnvDescriptor);
            nativeFunctionArgSlots += jniEnvDescriptor.toKind().stackSlots;

            final TypeDescriptor stackHandleDescriptor = createStackHandle.signature(constantPool()).resultDescriptor();
            if (isStatic) {
                // Push the class for a static method
                ldc(createClassConstant(holder.toJava()));
            } else {
                // Push the receiver for a non-static method
                aload(parameterLocalIndex++);
            }
            invokestatic(createStackHandle, 1, 1);
            nativeFunctionDescriptor.append(stackHandleDescriptor);
            nativeFunctionArgSlots += stackHandleDescriptor.toKind().stackSlots;
        } else {
            assert isStatic;
        }

        // Push the remaining parameters, wrapping reference parameters in JNI handles
        for (int i = 0; i < signatureDescriptor.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = signatureDescriptor.parameterDescriptorAt(i);
            TypeDescriptor nativeParameterDescriptor = parameterDescriptor;
            switch (parameterDescriptor.toKind().asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT: {
                    iload(parameterLocalIndex);
                    break;
                }
                case FLOAT: {
                    fload(parameterLocalIndex);
                    break;
                }
                case LONG: {
                    lload(parameterLocalIndex);
                    ++parameterLocalIndex;
                    break;
                }
                case DOUBLE: {
                    dload(parameterLocalIndex);
                    ++parameterLocalIndex;
                    break;
                }
                case WORD: {
                    aload(parameterLocalIndex);
                    break;
                }
                case REFERENCE: {
                    assert !isCFunction;

                    aload(parameterLocalIndex);
                    invokestatic(createStackHandle, 1, 1);
                    nativeParameterDescriptor = JavaTypeDescriptor.JNI_HANDLE;
                    break;
                }
                case VOID: {
                    throw ProgramError.unexpected();
                }
            }
            nativeFunctionDescriptor.append(nativeParameterDescriptor);
            nativeFunctionArgSlots += nativeParameterDescriptor.toKind().stackSlots;
            ++parameterLocalIndex;
        }

        // Link native function
        invokestatic(link, 0, 1);

        invokestatic(javaToNative, 0, 0);

        // Invoke the native function
        callnative(SignatureDescriptor.create(nativeFunctionDescriptor.append(')').append(nativeResultDescriptor).toString()), nativeFunctionArgSlots, nativeResultDescriptor.toKind().stackSlots);

        invokestatic(nativeToJava, 0, 0);

        if (!isCFunction) {
            // Unwrap a reference result from its enclosing JNI handle. This must be done
            // *before* the JNI frame is restored.
            if (resultKind.isReference) {
                invokevirtual(unhandHandle, 1, 1);
            }

            // Restore JNI frame.
            aload(currentThread);
            iload(top);
            invokevirtual(resetHandlesTop, 2, 0);

            verboseJniExit();

            // throw (and clear) any pending exception
            aload(currentThread);
            invokevirtual(throwJniException, 1, 0);
        }

        // Return result
        if (resultKind.isReference) {
            assert !isCFunction;

            // Insert cast if return type is not java.lang.Object
            if (resultDescriptor != JavaTypeDescriptor.OBJECT) {
                checkcast(createClassConstant(resultDescriptor));
            }
        }

        return_(resultKind);
    }

    /**
     * Generates the code to trace a call to a native function from a native stub.
     */
    private void verboseJniEntry() {
        if (ClassMethodActor.TraceJNI) {
            if (MaxineVM.isHosted()) {
                // Stubs generated while bootstrapping need to test the "-XX:+TraceJNI" VM option
                getstatic(traceJNI);
                final Label noTracing = newLabel();
                ifeq(noTracing);
                traceJniEntry();
                noTracing.bind();
            } else {
                traceJniEntry();
            }
        }
    }

    private void traceJniEntry() {
        invokestatic(traceCurrentThreadPrefix, 0, 0);
        ldc(PoolConstantFactory.createStringConstant("\" --> JNI: " + classMethodActor.format("%H.%n(%P)") + "]"));
        invokestatic(logPrintln_String, 1, 0);
    }

    /**
     * Generates the code to trace a return to a native stub from a native function.
     */
    private void verboseJniExit() {
        if (ClassMethodActor.TraceJNI) {
            if (MaxineVM.isHosted()) {
                // Stubs generated while bootstrapping need to test the "-XX:+TraceJNI" VM option
                getstatic(traceJNI);
                final Label notVerbose = newLabel();
                ifeq(notVerbose);
                traceJniExit();
                notVerbose.bind();
            } else {
                traceJniExit();
            }
        }
    }

    @NEVER_INLINE
    private void traceJniExit() {
        invokestatic(traceCurrentThreadPrefix, 0, 0);
        ldc(PoolConstantFactory.createStringConstant("\" <-- JNI: " + classMethodActor.format("%H.%n(%P)") + "]"));
        invokestatic(logPrintln_String, 1, 0);
    }

    @NEVER_INLINE
    private static void traceCurrentThreadPrefix() {
        Log.print("[Thread \"");
        Log.print(VmThread.current().getName());
    }

    /**
     * @see MaxineIntrinsicIDs#JNI_LINK
     * @see #link
     */
    @INTRINSIC(JNI_LINK)
    public static native Address link();

    /**
     * @see MaxineIntrinsicIDs#JNI_N2J
     * @see #nativeToJava
     */
    @INTRINSIC(JNI_N2J)
    public static native void nativeToJava();

    /**
     * @see MaxineIntrinsicIDs#JNI_J2N
     * @see #javaToNative
     */
    @INTRINSIC(JNI_J2N)
    public static native void javaToNative();

}
