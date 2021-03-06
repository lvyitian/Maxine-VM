/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.jvmti;

import static com.oracle.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * Efficient state storage for thread related JVMTI information.
 *
 * In {@link #JVMTI_STATE}:
 * Bits 0-7 are status bits.
 *
 * {@link JVMTI_THREADLOCAL} supports {@link JVMTIFunctions#GetThreadLocalStorage}.
 *
 */
public class JVMTIVmThreadLocal {
    public static final VmThreadLocal JVMTI_STATE = new VmThreadLocal(
                    "JVMTI", false, "JVMTI state", Nature.Triple);
    public static final VmThreadLocal JVMTI_THREADLOCAL = new VmThreadLocal(
                    "JVMTI_TL", false, "JVMTI thread local storage", Nature.Triple);

    /**
     * Bit is set when a {@link JVMTIRawMonitor#notify()} has occurred on this thread.
     */
    static final int JVMTI_RAW_NOTIFY = 1;

    /**
     * Bit set while thread is executing a JVMTI/JNI upcall.
     */
    public static final int IN_UPCALL = 4;

    private static final int STATUS_BIT_MASK = 0xFF;

    /**
     * Checks if given bit is set in given threadlocals area.
     * @param tla
     * @param bit
     * @return {@code true} iff the bit is set.
     */
    @INLINE
    static boolean bitIsSet(Pointer tla, int bit) {
        return JVMTI_STATE.load(tla).and(bit).isNotZero();
    }

    /**
     * Checks if given bit is set in current thread's threadlocals area.
     * @param tla
     * @param bit
     * @return {@code true} iff the bit is set.
     */
    @INLINE
    public static boolean bitIsSet(int bit) {
        return bitIsSet(ETLA.load(currentTLA()), bit);
    }

    @INLINE
    public static void setBit(Pointer tla, int bit) {
        JVMTI_STATE.store3(tla, JVMTI_STATE.load(tla).or(bit));
    }

    @INLINE
    public static void unsetBit(Pointer tla, int bit) {
        JVMTI_STATE.store3(tla, JVMTI_STATE.load(tla).and(~bit));
    }

    static int setThreadLocalStorage(Thread thread, Pointer data) {
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        JVMTI_THREADLOCAL.store3(vmThread.tla(), data);
        return JVMTI_ERROR_NONE;
    }

    static int getThreadLocalStorage(Thread thread, Pointer dataPtr) {
        VmThread vmThread = JVMTIThreadFunctions.checkVmThread(thread);
        if (vmThread == null) {
            return JVMTI_ERROR_THREAD_NOT_ALIVE;
        }
        dataPtr.setWord(JVMTI_THREADLOCAL.load(vmThread.tla()));
        return JVMTI_ERROR_NONE;
    }



}
