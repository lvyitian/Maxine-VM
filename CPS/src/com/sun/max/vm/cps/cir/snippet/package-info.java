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
/**
 * CIR subroutines that the compiler can pick up to implement Java features such as byte codes.
 * Each snippet is actually generated by translating a Java method to CIR.
 * Thus we do not have to concern ourselves here with programming in CIR.
 * 
 * This is a core feature of the Maxine architecture.
 * Instead of repetitively constructing more and more compounds of primitives
 * for all kinds of compilers and their layers by manually weaving IR,
 * we rather express the desired functionality,
 * no matter how low-level as long as it's not a true "builtin" primitive,
 * in a Java "snippet" and have the compiler du jour translate that to the IR at hand.
 * 
 * This means drastically reduced implementation burden for compiler writers.

 * @author Bernd Mathiske
 */
package com.sun.max.vm.cps.cir.snippet;
