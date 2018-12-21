/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.profilers.allocation;

import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.NO_SAFEPOINT_POLLS;
import com.sun.max.vm.Log;

import java.io.PrintWriter;

public class ProfiledObjects {

    /**
     * Each profiling cycle is predefined to track 10000 objects.
     * TODO: auto-configurable
     */
    public int SIZE = 500000;

    /**
     * The following 4 variables compose the stored information for each object.
     *
     * Index: unique for each object to make it distinguishable. [1-inf] index = 0 for empty cells
     * Type: the object's type/class
     * Size: the object's size. might exist objects with the same type but with different size.
     * Address: the object's address in the heap
     */
    public int[] index;
    public String[] type;
    public int[] size;
    public long[] address;

    public int currentIndex;

    public ProfiledObjects() {
        index = new int[SIZE];
        type = new String[SIZE];
        size = new int[SIZE];
        address = new long[SIZE];

        for (int i = 0; i < SIZE; i++) {
            index[i] = 0;
            type[i] = "null";
            size[i] = 0;
            address[i] = 0;
        }

        currentIndex = 0;
    }

    @NO_SAFEPOINT_POLLS("allocation profiler call chain must be atomic")
    @NEVER_INLINE
    public void record(int index, String type, int size, long address) {
        /*if( !(currentIndex < SIZE) ) {
            int newSIZE = SIZE+1000;
            int[] newIndex = new int[newSIZE];
            String[] newType = new String[newSIZE];
            int[] newSize = new int[newSIZE];
            long[] newAddress = new long[newSIZE];

            for (int i = 0; i < SIZE; i++) {
                newIndex[i] = this.index[i];
                newType[i] = this.type[i];
                newSize[i] = this.size[i];
                newAddress[i] = this.address[i];
            }
            for (int i = SIZE; i < newSIZE; i++) {
                newIndex[i] = 0;
                newType[i] = "null";
                newSize[i] = 0;
                newAddress[i] = 0;
            }
            this.index = newIndex;
            this.type = newType;
            this.size = newSize;
            this.address = newAddress;
            SIZE = newSIZE;

        }*/
        this.index[currentIndex] = index;
        this.type[currentIndex] = type;
        this.size[currentIndex] = size;
        this.address[currentIndex] = address;
        currentIndex++;
    }

    public void dumpToStdOut(int cycle) {
        Log.print("==== Profiling Cycle ");
        Log.print(cycle);
        Log.println(" Start ====");
        for (int i = 0; i < currentIndex; i++) {
            Log.print(index[i]);
            Log.print(" ");
            Log.print(type[i]);
            Log.print(" ");
            Log.print(size[i]);
            Log.print(" ");
            Log.println(address[i]);
        }
        Log.print("Buffer usage = ");
        Log.print(currentIndex);
        Log.print(" / ");
        Log.println(address.length);
    }

    public void print(int cycle) {
        dumpToStdOut(cycle);
    }

    public void resetCycle() {
        currentIndex = 0;
    }
}
