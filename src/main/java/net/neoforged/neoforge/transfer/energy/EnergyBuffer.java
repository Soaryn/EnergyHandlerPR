/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;

import java.util.Objects;
import java.util.stream.IntStream;

//PRIMER: NBT Serialization was removed in favor of either Codecs in data attachments, or alternate means of writing to nbt.

/**
 * A simple reference implementation of {@link IEnergyHandler}. Use/extend this or implement your own. This has multiple "slots" or sub-buffers for energy to be inserted or extracted from.
 * It is recommended to make your own implementation of {@link IEnergyHandler}, especially if you need multiple "sub buffers".
 * <p>
 * It is recommended to use the {@link EnergyBuffer.Builder} to construct an {@link EnergyBuffer} such as:
 * <pre>
 * {@code ComplexEnergyBuffer.Builder.create(3, 1000).maxTransfer(10).build();}
 * </pre>
 */
public class EnergyBuffer implements IEnergyHandler {
    public static Codec<EnergyBuffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("size").forGetter(data -> data.size),
            Codec.INT.fieldOf("capacity").forGetter(data -> data.capacity),
            Codec.INT.fieldOf("max_insertion").forGetter(data -> data.maxInsert),
            Codec.INT.fieldOf("max_extraction").forGetter(data -> data.maxExtract),
            Codec.INT_STREAM.fieldOf("energy").xmap(IntStream::toArray, IntStream::of).forGetter(data -> data.energy)
    ).apply(instance, EnergyBuffer::new));

    /**
     * Number of sub-buffers
     */
    private final int size;

    /**
     * Current amount of energy stored in the buffer
     */
    private final int[] energy;

    /**
     * How much energy can be stored in the buffer
     */
    private final int capacity;

    /**
     * How much energy can be inserted in a single call of `insert`. Note, if you need to limit how much can be inserted in a single tick, then you will need to make your own implementation of {@link IEnergyHandler} that has the required information.
     */
    private final int maxInsert;

    /**
     * How much energy can be inserted in a single call of `extract`.
     * Note, if you need to limit how much can be extracted in a single tick,
     * then you will need to make your own implementation of {@link IEnergyHandler} that has the required information.
     */
    private final int maxExtract;

    /**
     * An {@link IEnergyHandler} with a variable, but constant, amount of sub-buffers.
     * This will give more options to what is routing the energy to the handler. If you don't need more than one sub-buffer, the builder can be used with a size of `1`, or more advisable, create your own {@link IEnergyHandler} implementation.
     * <p>
     * Use of constructor is allowed, but it is HIGHLY recommended to use the builder.
     * <p>
     * Example:
     * <pre>
     * {@code
     *      //Creates a buffer that has 3 sub-buffers each with a capacity of 1000,
     *      // and a max insert and extraction rate of 10
     *      ComplexEnergyBuffer.Builder.create(3, 1000).maxTransfer(10).build();
     * }
     * </pre>
     * @param size       How many sub-buffers should be available.
     * @param capacity   Amount of energy that can be stored in any sub-buffer. Note, this value is not unique per sub-buffer.
     * @param maxInsert  How much energy can be inserted in a single {@link IEnergyHandler#insert} call.
     * @param maxExtract How much energy can be extracted in a single  {@link IEnergyHandler#extract} call.
     * @param energy     An array of initial or serialized energy sub-buffer amounts.
     */
    public EnergyBuffer(int size, int capacity, int maxInsert, int maxExtract, int... energy) {
        if (size < energy.length) {
            throw new IllegalArgumentException("An EnergyBuffer must have a size (" + size + ") larger than energy array length (" + energy.length + ") passed in.");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("The capacity in an EnergyBuffer must not be less than zero.");
        }
        this.size = size;
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.energy = new int[size];
        for (int i = 0; i < size; i++) {
            this.energy[i] = Math.max(0, Math.min(capacity, energy[i]));
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int insert(int amount, TransferAction action) {
        amount = Math.min(maxInsert, amount);
        if (amount <= 0) return 0;

        var handled = 0;
        for (var index = 0; index < size(); index++) {
            if (handled == amount) break;
            handled += commonInsert(index, amount - handled, action);
        }
        return handled;
    }

    @Override
    public int insert(int index, int amount, TransferAction action) {
        Objects.checkIndex(index, size());
        amount = Math.min(maxInsert, amount);
        if (amount <= 0) return 0;
        return commonInsert(index, amount, action);
    }

    //TODO at the time of writing this PR, IResourceHandler was still in development and not available to link in java doc.
    /**
     * This was chosen to be separate from {@link EnergyBuffer#insert(int, int, TransferAction)} to provide both parity with the {@link IResourceHandler} as well as allow more accurate index checks when doing the loop variant.
     */
    private int commonInsert(int index, int amount, TransferAction action) {
        if (!allowsInsertion(index)) return 0;
        var capacity = getCapacity(index);
        int inserted = Math.min(capacity - energy[index], amount);
        if (action.isExecuting())
            energy[index] += inserted;
        return inserted;
    }

    @Override
    public int extract(int index, int amount, TransferAction action) {
        //Do this check outside the common method as the check does not need to be done in the manual loop below.
        Objects.checkIndex(index, size());
        amount = Math.min(maxExtract, amount);
        if (amount <= 0) return 0;

        return extractCommon(index, amount, action);
    }

    @Override
    public int extract(int amount, TransferAction action) {
        amount = Math.min(maxExtract, amount);
        if (amount <= 0) return 0;

        var handled = 0;
        for (var index = 0; index < size(); index++) {
            if (handled == amount) break;

            handled += extractCommon(index, amount - handled, action);
        }
        return handled;
    }

    private int extractCommon(int index, int amount, TransferAction action) {
        if (amount <= 0) return 0;
        if (!allowsExtraction(index)) return 0;
        if (energy[index] == 0) return 0;

        int handledAmount = Math.min(energy[index], amount);
        if (action.isExecuting())
            energy[index] = energy[index] - handledAmount;
        return handledAmount;
    }

    @Override
    public int getAmount(int index) {
        Objects.checkIndex(index, size());
        return this.energy[index];
    }

    @Override
    public int getCapacity(int index) {
        return this.capacity;
    }

    @Override
    public boolean allowsInsertion(int index) {
        Objects.checkIndex(index, size());
        return this.maxInsert > 0;
    }

    @Override
    public boolean allowsExtraction(int index) {
        Objects.checkIndex(index, size());
        return this.maxExtract > 0;
    }


    public static class Builder {
        protected int size = 1;
        protected int[] energy;
        protected int capacity;
        protected int maxInsert;
        protected int maxExtract;

        private Builder() { }

        public static Builder create(int size, int capacity) {
            return new Builder().size(size).capacity(capacity).maxTransferRate(capacity);
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder maxInsertRate(int maxInsert) {
            this.maxInsert = maxInsert;
            return this;
        }

        public Builder maxExtractRate(int maxExtract) {
            this.maxExtract = maxExtract;
            return this;
        }

        public Builder maxTransferRate(int maxTransfer) {
            return maxExtractRate(maxTransfer).maxInsertRate(maxTransfer);
        }

        /**
         * @param amount Amount to set the initial energy buffer to at the specified index
         * @return the builder instance to allow chaining commands.
         */
        public Builder energy(int index, int amount) {
            Objects.checkIndex(index, size);
            energy[index] = amount;
            return this;
        }

        /**
         * @param amount Amount to set the initial energy buffer to all indices
         * @return the builder instance to allow chaining commands.
         */
        public Builder energy(int amount) {
            for (int index = 0; index < size; index++) {
                Objects.checkIndex(index, size);
                energy[index] = amount;
            }
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            energy = new int[size];
            return this;
        }

        public EnergyBuffer build() {
            return new EnergyBuffer(size, capacity, maxInsert, maxExtract, energy);
        }
    }

}
