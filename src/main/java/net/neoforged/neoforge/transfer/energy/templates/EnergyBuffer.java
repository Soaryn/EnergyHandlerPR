/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.energy.templates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import org.jetbrains.annotations.Range;

// PRIMER: NBT Serialization was removed in favor of either Codecs in data attachments, or alternate means of writing to nbt.

/**
 * A simple reference implementation of {@link IEnergyHandler}. Use/extend this or implement your own. This has multiple "slots" or sub-buffers for energy to be inserted or extracted from.
 * It is recommended to make your own implementation of {@link IEnergyHandler}, especially if you need multiple "sub buffers".
 * <p>
 * It is recommended to use the {@link EnergyBuffer.Builder} to construct an {@link EnergyBuffer} such as:
 * 
 * <pre>
 * {@code
 * ComplexEnergyBuffer.Builder.create(3, 1000).maxTransfer(10).build();
 * }
 * </pre>
 */
public class EnergyBuffer implements IEnergyHandler.Modifiable {
    public static Codec<EnergyBuffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("size").forGetter(data -> data.size),
            Codec.INT.fieldOf("capacity").forGetter(data -> data.capacity),
            Codec.INT.fieldOf("max_insertion").forGetter(data -> data.maxInsert),
            Codec.INT.fieldOf("max_extraction").forGetter(data -> data.maxExtract),
            Codec.INT_STREAM.fieldOf("energy").xmap(IntStream::toArray, IntStream::of).forGetter(data -> data.energy)).apply(instance, EnergyBuffer::new));

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
     * How much energy can be inserted in a single call of `insert`.
     * Note, if you need to limit how much can be inserted in a single tick,
     * then you will need to make your own implementation of {@link IEnergyHandler} that has the required information.
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
     * This will give more options to what is routing the energy to the handler. If you don't need more than one sub-buffer, the builder can be used with a size of `1`, but it is more advisable, create your own {@link IEnergyHandler} implementation to match your exact needs.
     * <p>
     * Use of constructor is allowed, but it is HIGHLY recommended to use the builder.
     * <p>
     * Example:
     * 
     * <pre>
     * {@code
     * //Creates a buffer that has 3 sub-buffers each with a capacity of 1000,
     * // and a max insert and extraction rate of 10
     * ComplexEnergyBuffer.Builder.create(3, 1000).maxTransfer(10).build();
     * }
     * </pre>
     *
     * @param size              How many sub-buffers should be available.
     * @param capacity          Amount of energy that can be stored in any sub-buffer. Note, this value is not unique per sub-buffer.
     * @param maxInsertionRate  How much energy can be inserted in a single {@link IEnergyHandler#insert} call.
     * @param maxExtractionRate How much energy can be extracted in a single {@link IEnergyHandler#extract} call.
     * @param energy            An array of initial or serialized energy sub-buffer amounts.
     */
    public EnergyBuffer(int size, int capacity, int maxInsertionRate, int maxExtractionRate, int... energy) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        if (size < energy.length) {
            throw new IllegalArgumentException("An EnergyBuffer must have a size (" + size + ") larger than energy array length (" + energy.length + ") passed in.");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("The capacity in an EnergyBuffer must not be less than zero.");
        }
        if (maxInsertionRate < 0) {
            throw new IllegalArgumentException("The maximum insertion rate in an EnergyBuffer must not be less than zero.");
        }
        if (maxExtractionRate < 0) {
            throw new IllegalArgumentException("The maximum extraction rate in an EnergyBuffer must not be less than zero.");
        }
        this.size = size;
        this.capacity = capacity;
        this.maxInsert = maxInsertionRate;
        this.maxExtract = maxExtractionRate;
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

            //We don't need to check if the index is valid in this case since we already know our index is within bounds
            handled += insertCommon(index, amount - handled, action);
        }
        return handled;
    }

    @Override
    public int insert(int index, int amount, TransferAction action) {
        //This check is done per external index call
        Objects.checkIndex(index, size());
        amount = Math.min(maxInsert, amount);
        if (amount <= 0) return 0;

        return insertCommon(index, amount, action);
    }

    //TODO at the time of writing this PR, IResourceHandler was still in development and not available to link in java doc.
    /**
     * This was chosen to be separate from {@link EnergyBuffer#insert(int, int, TransferAction)} to provide both parity with the {@link IResourceHandler} as well as allow more accurate index checks when doing the loop variant.
     * <p>
     * The added benefit is less double-checking in runtime on data we already know
     */
    private int insertCommon(int index, int amount, TransferAction action) {
        if (!allowsInsertion(index)) return 0;
        var currentAmount = energy[index];
        int inserted = Math.min(capacity - currentAmount, amount);
        if (action.isExecuting())
            energy[index] = currentAmount + inserted;
        return inserted;
    }

    @Override
    public int extract(int amount, TransferAction action) {
        amount = Math.min(maxExtract, amount);
        if (amount <= 0) return 0;

        var handled = 0;
        for (var index = 0; index < size(); index++) {
            if (handled == amount) break;
            //We don't need to check if the index is valid in this case since we already know our index is within bounds
            handled += extractCommon(index, amount - handled, action);
        }
        return handled;
    }

    @Override
    public int extract(int index, int amount, TransferAction action) {
        //This check is done per external index call
        Objects.checkIndex(index, size());
        amount = Math.min(maxExtract, amount);
        if (amount <= 0) return 0;

        return extractCommon(index, amount, action);
    }

    /**
     * Common method for extraction, but allowing the index-less and the indexed methods to have their
     * own validations for their respective calls. Avoids double-checking certain validations
     */
    private int extractCommon(int index, int amount, TransferAction action) {
        if (amount <= 0) return 0;
        if (!allowsExtraction(index)) return 0;
        var currentAmount = energy[index];
        if (currentAmount == 0) return 0;

        int handledAmount = Math.min(currentAmount, amount);
        if (action.isExecuting())
            energy[index] = currentAmount - handledAmount;
        return handledAmount;
    }

    @Override
    public int getAmount(int index) {
        Objects.checkIndex(index, size());
        return this.energy[index];
    }

    @Override
    public int getCapacity(int index) {
        Objects.checkIndex(index, size());
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

    @Override
    public void set(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount) {
        Objects.checkIndex(index, size());
        energy[index] = Mth.clamp(amount, 0, this.capacity);
    }

    public static class Builder {
        protected int size = 1;
        protected int[] energy = new int[1];
        protected int capacity;
        protected int maxInsertRate;
        protected int maxExtractRate;

        private Builder() {}

        /**
         * Creates a builder of a specified size, and capacity. This is the advised way to make an {@link EnergyBuffer}.
         * An important note, is by default the transfer rate is 1% of the capacity (but never less than 1).
         * This it to help make a simple feeling of something charging
         *
         * @param size     How many sub-buffers the {@link EnergyBuffer} should have. The typical amount is 1, but more advanced usages can have more.
         * @param capacity How much energy the sub-buffers are set to be able to hold individually. If you desire separate capacities per buffer, then you will need to implement your own variant.
         * @return Chainable builder to allow creation of a new {@link EnergyBuffer}
         */
        public static Builder create(@Range(from = 1, to = EnergyHandlerUtil.MAX_VALUE) int size, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int capacity) {
            return new Builder().size(size).capacity(capacity).maxTransferRate(Mth.ceil(capacity * 0.01f));
        }

        /**
         * @param capacity How much energy each sub-buffer can hold.
         */
        public Builder capacity(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int capacity) {
            this.capacity = capacity;
            return this;
        }

        /**
         * @param rate How much energy the buffer can insert in a single call.
         */
        public Builder maxInsertRate(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int rate) {
            this.maxInsertRate = rate;
            return this;
        }

        /**
         * @param rate How much energy the buffer can extract in a single call.
         */
        public Builder maxExtractRate(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int rate) {
            this.maxExtractRate = rate;
            return this;
        }

        /**
         * @param rate How much energy the buffer can insert or extract in a single call.
         */
        public Builder maxTransferRate(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int rate) {
            return maxExtractRate(rate).maxInsertRate(rate);
        }

        /**
         * @param amount Amount to set the initial energy buffer to at the specified index
         * @return the builder instance to allow chaining commands.
         */
        public Builder energy(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount) {
            Objects.checkIndex(index, size);
            energy[index] = amount;
            return this;
        }

        /**
         * @param amount Amount to set the initial energy buffer to all indices
         * @return the builder instance to allow chaining commands.
         */
        public Builder energy(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount) {
            for (int index = 0; index < size; index++) {
                energy[index] = amount;
            }
            return this;
        }

        /**
         * @param size Number of sub-buffers.
         */
        public Builder size(@Range(from = 1, to = EnergyHandlerUtil.MAX_VALUE) int size) {
            this.size = size;
            energy = new int[size];
            return this;
        }

        /**
         * Constructs a new {@link EnergyBuffer} to use.
         */
        public EnergyBuffer build() {
            return new EnergyBuffer(size, capacity, maxInsertRate, maxExtractRate, energy);
        }
    }
}
