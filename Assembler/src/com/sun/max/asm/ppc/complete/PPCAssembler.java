/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.asm.ppc.complete;

import com.sun.max.asm.*;

/**
 * The base class for the 32-bit and 64-bit PowerPC assemblers. This class also defines
 * the more complex synthetic PowerPC instructions.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class PPCAssembler extends PPCLabelAssembler {

    @Override
    protected void emitPadding(int numberOfBytes) throws AssemblyException {
        if ((numberOfBytes % 4) != 0) {
            throw new AssemblyException("Cannot pad instruction stream with a number of bytes not divisble by 4");
        }
        for (int i = 0; i < numberOfBytes / 4; i++) {
            nop();
        }
    }
}
