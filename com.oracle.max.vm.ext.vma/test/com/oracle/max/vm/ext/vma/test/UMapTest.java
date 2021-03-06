/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.test;

import java.util.*;

public class UMapTest {

    public static void main(String[] args) {
        System.out.println("main entered");
        new UMapTest().run();
        System.out.println("main exited");
    }

    private Map<Key, Value> map = new HashMap<Key, Value>();

    private void run() {
        System.out.println("run entered");
        for (int i = 0; i < 100; i++) {
            map.put(new Key(i), new Value(i));
        }

        for (Map.Entry<Key, Value> entry : map.entrySet()) {
            if (entry.getKey().i != entry.getValue().i) {
                throw new RuntimeException("key/value mismatch");
            }
        }
        System.out.println("run exited");
    }

    static class Key {
        private int i;

        Key(int i) {
            this.i = i;
        }

        @Override
        public int hashCode() {
            return i;
        }

        @Override
        public boolean equals(Object other) {
            return ((Key) other).i == i;
        }
    }

    static class Value {
        private int i;

        Value(int i) {
            this.i = i;
        }
    }
}
