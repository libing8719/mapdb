/*
 *  Copyright (c) 2012 Jan Kotek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Adopted from Apache Harmony with following copyright:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */
package org.mapdb.atomic;

import org.mapdb.ser.Serializer;
import org.mapdb.ser.Serializers;
import org.mapdb.store.MutableStore;
import org.mapdb.store.Store;

import java.io.DataInput;
import java.io.IOException;

/**
 * <p>
 * A small toolkit of classes that support lock-free thread-safe
 * programming on single records.  In essence, the classes here
 * provide provide an atomic conditional update operation of the form:
 * </p>
 *
 * <pre>
 *   boolean compareAndSet(expectedValue, updateValue);
 * </pre>
 *
 * <p>
 * This method (which varies in argument types across different
 * classes) atomically sets a record to the {@code updateValue} if it
 * currently holds the {@code expectedValue}, reporting {@code true} on
 * success. Classes jere also contain methods to get and
 * unconditionally set values.
 * </p><p>
 *
 * The specifications of these methods enable to
 * employ more efficient internal DB locking. CompareAndSwap
 * operation is typically faster than using transactions, global lock or other
 * concurrent protection.
 *
 * </p><p>
 * Instances of classes
 * {@link Atomic.Boolean},
 * {@link Atomic.Integer},
 * {@link Atomic.Long},
 * {@link Atomic.String} and
 * {@link Atomic.Var}
 * each provide access and updates to a single record of the
 * corresponding type.  Each class also provides appropriate utility
 * methods for that type.  For example, classes {@code Atomic.Long} and
 * {@code Atomic.Integer} provide atomic increment methods.  One
 * application is to generate unique keys for Maps:
 * </p>
 * <pre>
 *    Atomic.Long id = Atomic.getLong("mapId");
 *    map.put(id.getAndIncrement(), "something");
 * </pre>
 *
 * <p>
 * Atomic classes are designed primarily as building blocks for
 * implementing non-blocking data structures and related infrastructure
 * classes.  The {@code compareAndSet} method is not a general
 * replacement for locking.  It applies only when critical updates for an
 * object are confined to a <em>single</em> record.
 *</p><p>
 *
 * Atomic classes are not general purpose replacements for
 * {@code java.lang.Integer} and related classes.  They do <em>not</em>
 * define methods such as {@code hashCode} and
 * {@code compareTo}.  (Because atomic records are expected to be
 * mutated, they are poor choices for hash table keys.)  Additionally,
 * classes are provided only for those types that are commonly useful in
 * intended applications. Other types has to be wrapped into general {@link Atomic.Var}
 * </p><p>
 *
 * You can also hold floats using
 * {@link java.lang.Float#floatToIntBits} and
 * {@link java.lang.Float#intBitsToFloat} conversions, and doubles using
 * {@link java.lang.Double#doubleToLongBits} and
 * {@link java.lang.Double#longBitsToDouble} conversions.
 * </p>
 *
 */
final public class Atomic {

    private Atomic(){}


    /**
     * An {@code int} record that may be updated atomically.  An
     * {@code Atomic@Integer} is used in applications such as atomically
     * incremented counters, and cannot be used as a replacement for an
     * {@link java.lang.Integer}. However, this class does extend
     * {@code Number} to allow uniform access by tools and utilities that
     * deal with numerically-based classes.
     */
    public final static class Integer extends Number {

		private static final long serialVersionUID = 4615119399830853054L;
		
		protected final MutableStore store;
        protected final long recid;

        public Integer(MutableStore store, long recid) {
            this.store = store;
            this.recid = recid;
        }

        /**
         * @return recid under which value is saved
         */
        public long getRecid(){
            return recid;
        }

        /**
         * Gets the current value.
         *
         * @return the current value
         */
        public final int get() {
            return store.get(recid, Serializers.INTEGER);
        }

        /**
         * Sets to the given value.
         *
         * @param newValue the new value
         */
        public final void set(int newValue) {
            store.update(recid, Serializers.INTEGER, newValue);
        }


        /**
         * Atomically sets to the given value and returns the old value.
         *
         * @param newValue the new value
         * @return the previous value
         */
        public final int getAndSet(int newValue) {
            //$DELAY$
            for (;;) {
                int current = get();
                //$DELAY$
                if (compareAndSet(current, newValue)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically sets the value to the given updated value
         * if the current value {@code ==} the expected value.
         *
         * @param expect the expected value
         * @param update the new value
         * @return true if successful. False return indicates that
         * the actual value was not equal to the expected value.
         */
        public final boolean compareAndSet(int expect, int update) {
            return store.compareAndUpdate(recid, Serializers.INTEGER, expect, update);
        }


        /**
         * Atomically increments by one the current value.
         *
         * @return the previous value
         */
        public final int getAndIncrement() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current + 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically decrements by one the current value.
         *
         * @return the previous value
         */
        public final int getAndDecrement() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current - 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically adds the given value to the current value.
         *
         * @param delta the value to add
         * @return the previous value
         */
        public final int getAndAdd(int delta) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current + delta;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically increments by one the current value.
         *
         * @return the updated value
         */
        public final int incrementAndGet() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current + 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Atomically decrements by one the current value.
         *
         * @return the updated value
         */
        public final int decrementAndGet() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current - 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Atomically adds the given value to the current value.
         *
         * @param delta the value to add
         * @return the updated value
         */
        public final int addAndGet(int delta) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                int current = get();
                int next = current + delta;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Returns the String representation of the current value.
         * @return the String representation of the current value.
         */
        public java.lang.String toString() {
            return java.lang.Integer.toString(get());
        }


        public int intValue() {
            return get();
        }

        public long longValue() {
            return (long)get();
        }

        public float floatValue() {
            return (float)get();
        }

        public double doubleValue() {
            return (double)get();
        }

    }


    /**
     * A {@code long} record that may be updated atomically.   An
     * {@code Atomic#Long} is used in applications such as atomically
     * incremented sequence numbers, and cannot be used as a replacement
     * for a {@link java.lang.Long}. However, this class does extend
     * {@code Number} to allow uniform access by tools and utilities that
     * deal with numerically-based classes.
     */
    public final static class Long extends Number{

		private static final long serialVersionUID = 2882620413591274781L;
		
		protected final MutableStore store;
        protected final long recid;

        public Long(MutableStore store, long recid) {
            this.store = store;
            this.recid = recid;
        }

        /**
         * @return recid under which value is saved
         */
        public long getRecid(){
            return recid;
        }


        /**
         * Gets the current value.
         *
         * @return the current value
         */
        public final long get() {
            return store.get(recid, Serializers.LONG);
        }

        /**
         * Sets to the given value.
         *
         * @param newValue the new value
         */
        public final void set(long newValue) {
            store.update(recid, Serializers.LONG, newValue);
        }


        /**
         * Atomically sets to the given value and returns the old value.
         *
         * @param newValue the new value
         * @return the previous value
         */
        public final long getAndSet(long newValue) {
            //$DELAY$
            while (true) {
                //$DELAY$
                long current = get();
                //$DELAY$
                if (compareAndSet(current, newValue)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically sets the value to the given updated value
         * if the current value {@code ==} the expected value.
         *
         * @param expect the expected value
         * @param update the new value
         * @return true if successful. False return indicates that
         * the actual value was not equal to the expected value.
         */
        public final boolean compareAndSet(long expect, long update) {
            return store.compareAndUpdate(recid, Serializers.LONG, expect, update);
        }


        /**
         * Atomically increments by one the current value.
         *
         * @return the previous value
         */
        public final long getAndIncrement() {
            //$DELAY$
            while (true) {
                //$DELAY$
                long current = get();
                long next = current + 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically decrements by one the current value.
         *
         * @return the previous value
         */
        public final long getAndDecrement() {
            //$DELAY$
            while (true) {
                //$DELAY$
                long current = get();
                long next = current - 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically adds the given value to the current value.
         *
         * @param delta the value to add
         * @return the previous value
         */
        public final long getAndAdd(long delta) {
            //$DELAY$
            while (true) {
                //$DELAY$
                long current = get();
                long next = current + delta;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Atomically increments by one the current value.
         *
         * @return the updated value
         */
        public final long incrementAndGet() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                long current = get();
                long next = current + 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Atomically decrements by one the current value.
         *
         * @return the updated value
         */
        public final long decrementAndGet() {
            //$DELAY$
            for (;;) {
                //$DELAY$
                long current = get();
                long next = current - 1;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Atomically adds the given value to the current value.
         *
         * @param delta the value to add
         * @return the updated value
         */
        public final long addAndGet(long delta) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                long current = get();
                long next = current + delta;
                //$DELAY$
                if (compareAndSet(current, next)) {
                    //$DELAY$
                    return next;
                }
            }
        }

        /**
         * Returns the String representation of the current value.
         * @return the String representation of the current value.
         */
        public java.lang.String toString() {
            return java.lang.Long.toString(get());
        }


        public int intValue() {
            return (int)get();
        }

        public long longValue() {
            return get();
        }

        public float floatValue() {
            return (float)get();
        }

        public double doubleValue() {
            return (double)get();
        }

    }


    /**
     * A {@code boolean} record that may be updated atomically.
     */
    public final static class Boolean {

        protected final MutableStore store;
        protected final long recid;

        public Boolean(MutableStore store, long recid) {
            this.store = store;
            this.recid = recid;
        }

        /**
         * @return recid under which value is saved
         */
        public long getRecid(){
            return recid;
        }


        /**
         * Returns the current value.
         *
         * @return the current value
         */
        public final boolean get() {
            return store.get(recid, Serializers.BOOLEAN);
        }

        /**
         * Atomically sets the value to the given updated value
         * if the current value {@code ==} the expected value.
         *
         * @param expect the expected value
         * @param update the new value
         * @return true if successful. False return indicates that
         * the actual value was not equal to the expected value.
         */
        public final boolean compareAndSet(boolean expect, boolean update) {
            return store.compareAndUpdate(recid, Serializers.BOOLEAN, expect, update);
        }


        /**
         * Unconditionally sets to the given value.
         *
         * @param newValue the new value
         */
        public final void set(boolean newValue) {
            store.update(recid, Serializers.BOOLEAN, newValue);
        }


        /**
         * Atomically sets to the given value and returns the previous value.
         *
         * @param newValue the new value
         * @return the previous value
         */
        public final boolean getAndSet(boolean newValue) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                boolean current = get();
                //$DELAY$
                if (compareAndSet(current, newValue)) {
                    //$DELAY$
                    return current;
                }
            }
        }

        /**
         * Returns the String representation of the current value.
         * @return the String representation of the current value.
         */
        public java.lang.String toString() {
            return java.lang.Boolean.toString(get());
        }

    }

    /**
    * A {@code String} record that may be updated atomically.
    */
    public final static class String{

        protected final MutableStore store;
        protected final long recid;

        public String(MutableStore store, long recid) {
            this.store = store;
            this.recid = recid;
        }


        /**
         * @return recid under which value is saved
         */
        public long getRecid(){
            return recid;
        }

        public java.lang.String toString() {
            return get();
        }

        /**
         * Returns the current value.
         *
         * @return the current value
         */
        public final java.lang.String get() {
            return store.get(recid, Serializers.STRING_NOSIZE);
        }

        /**
         * Atomically sets the value to the given updated value
         * if the current value equals the expected value.
         *
         * @param expect the expected value
         * @param update the new value
         * @return true if successful. False return indicates that
         * the actual value was not equal to the expected value.
         */
        public final boolean compareAndSet(java.lang.String expect, java.lang.String update) {
            return store.compareAndUpdate(recid, Serializers.STRING_NOSIZE, expect, update);
        }


        /**
         * Unconditionally sets to the given value.
         *
         * @param newValue the new value
         */
        public final void set(java.lang.String newValue) {
            store.update(recid, Serializers.STRING_NOSIZE, newValue);
        }


        /**
         * Atomically sets to the given value and returns the previous value.
         *
         * @param newValue the new value
         * @return the previous value
         */
        public final java.lang.String getAndSet(java.lang.String newValue) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                java.lang.String current = get();
                //$DELAY$
                if (compareAndSet(current, newValue)) {
                    //$DELAY$
                    return current;
                }
            }
        }

    }

    /**
     * Atomically updated variable which may contain any type of record.
     */
    public static final class Var<E> {

        protected final MutableStore store;
        protected final long recid;
        protected final Serializer<E> serializer;

        public Var(MutableStore store, long recid, Serializer<E> serializer) {
            this.store = store;
            this.recid = recid;
            this.serializer = serializer;
        }
//
//        /* used for deserialization */
//        protected Var(Store store, SerializerBase serializerBase, DataInput is, SerializerBase.FastArrayList<Object> objectStack) throws IOException {
//            objectStack.add(this);
//            this.store = store;
//            this.recid = DataIO.unpackLong(is);
//            this.serializer = (Serializer<E>) serializerBase.deserialize(is,objectStack);
//        }

        /**
         * @return recid under which value is saved
         */
        public long getRecid(){
            return recid;
        }


        public java.lang.String toString() {
            E v = get();
            return v==null? null : v.toString();
        }

        /**
         * Returns the current value.
         *
         * @return the current value
         */
        public final E get() {
            return store.get(recid, serializer);
        }

        /**
         * Atomically sets the value to the given updated value
         * if the current value equals the expected value.
         *
         * @param expect the expected value
         * @param update the new value
         * @return true if successful. False return indicates that
         * the actual value was not equal to the expected value.
         */
        public final boolean compareAndSet(E expect, E update) {
            return store.compareAndUpdate(recid, serializer, expect, update);
        }


        /**
         * Unconditionally sets to the given value.
         *
         * @param newValue the new value
         */
        public final void set(E newValue) {
            store.update(recid, serializer, newValue);
        }


        /**
         * Atomically sets to the given value and returns the previous value.
         *
         * @param newValue the new value
         * @return the previous value
         */
        public final E getAndSet(E newValue) {
            //$DELAY$
            for (;;) {
                //$DELAY$
                E current = get();
                //$DELAY$
                if (compareAndSet(current, newValue)) {
                    //$DELAY$
                    return current;
                }
            }
        }


    }

}
