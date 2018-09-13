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

package com.sun.max.asm.gen.risc.arm;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.arm.*;
import com.sun.max.asm.gen.risc.*;

/**
 * The program entry point for the ARM assembler generator.
 */

public final class ARMAssemblerGenerator extends RiscAssemblerGenerator<RiscTemplate> {

    private ARMAssemblerGenerator() {
        super(ARMAssembly.ASSEMBLY);
    }

    @Override
    protected String getJavadocManualReference(RiscTemplate template) {
        final String section = template.instructionDescription().architectureManualSection();
        return "\"ARM Architecture Reference Manual ARMv7-A and ARMv7-R edition Issue C - Section " + section + "\"";
    }

    public static void main(String[] programArguments) {
        final ARMAssemblerGenerator generator = new ARMAssemblerGenerator();
        generator.options.parseArguments(programArguments);
        generator.generate();
    }

    @Override
    protected DisassembledInstruction generateExampleInstruction(RiscTemplate template, List<Argument> arguments) throws AssemblyException {
        return new DisassembledInstruction(new ARMDisassembler(0, null), 0, new byte[] {0, 0, 0, 0}, template, arguments);
    }

}
