/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=10L; 1=11L; 2=12L; 3=13L; 4=14L; 5=15L; 6=16L; 7=17L
 */
public class Reduce_Long01 {
    public static long test(long arg) {
        if (arg == 0) {
            return add(10);
        }
        if (arg == 1) {
            return sub(11);
        }
        if (arg == 2) {
            return mul(12);
        }
        if (arg == 3) {
            return div(13);
        }
        if (arg == 4) {
            return mod();
        }
        if (arg == 5) {
            return and(15);
        }
        if (arg == 6) {
            return or(16);
        }
        if (arg == 7) {
            return xor(17);
        }
        return 0;
    }
    public static long add(long x) {
        return x + 0;
    }
    public static long sub(long x) {
        return x - 0;
    }
    public static long mul(long x) {
        return x * 1;
    }
    public static long div(long x) {
        return x / 1;
    }
    public static long mod() {
        return 14;
    }
    public static long and(long x) {
        return x & -1;
    }
    public static long or(long x) {
        return x | 0;
    }
    public static long xor(long x) {
        return x ^ 0;
    }
}
