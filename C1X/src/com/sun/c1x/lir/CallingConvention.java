/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;

/**
 * This class represents a calling convention instance for a particular method invocation and describes the ABI for
 * outgoing arguments and the return value, both runtime calls and Java calls.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class CallingConvention {

    public final int overflowArgumentSize;
    public final CiLocation[] locations;
    public final LIROperand[] operands;

    CallingConvention(CiLocation[] locations) {
        this.locations = locations;
        this.operands = new LIROperand[locations.length];
        int outgoing = 0;
        for (int i = 0; i < locations.length; i++) {
            CiLocation l = locations[i];
            operands[i] = locationToOperand(l);
            if (l.isStack()) {
                outgoing = Math.max(outgoing, l.stackOffset() + l.stackSize());
            }
        }

        overflowArgumentSize = outgoing;
    }

    public static LIROperand locationToOperand(CiLocation location) {
        if (location.isStack()) {
            int stackOffset = location.stackOffset();
            if (location.isCallerFrame()) {
                return LIROperand.forAddress(LIROperand.forRegister(CiKind.Int, CiRegister.CallerStack), stackOffset, location.kind);
            } else {
                return LIROperand.forAddress(LIROperand.forRegister(CiKind.Int, CiRegister.Stack), stackOffset, location.kind);
            }
        } else {
            assert location.register() != null;
            return new LIRLocation(location.kind, location.register());
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("CallingConvention[");
        for (LIROperand op : operands) {
            result.append(op.toString()).append(" ");
        }
        result.append("]");
        return result.toString();
    }

}
