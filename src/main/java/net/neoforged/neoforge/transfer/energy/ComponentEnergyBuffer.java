/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.energy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;

/**
 * Variant of {@link EnergyBuffer} for use with data components.
 * <p>
 * The actual data storage is managed by a data component, and all changes will write back to that component.
 * <p>
 * To use this class, register a new {@link DataComponentType} which holds an {@link Integer} for your item.
 * Then reference that component from your {@link ICapabilityProvider} passed to {@link RegisterCapabilitiesEvent#registerItem} to create an instance of this class.
 */
public class ComponentEnergyBuffer implements IEnergyHandler.IEnergyHandlerModifiable {
    protected final MutableDataComponentHolder parent;
    protected final DataComponentType<Integer> energyComponent;
    protected final int capacity;
    protected final int maxReceive;
    protected final int maxExtract;

    /**
     * Creates a new ComponentEnergyStorage with a data component as the backing store for the energy value.
     *
     * @param parent          The parent component holder, such as an {@link ItemStack}
     * @param energyComponent The data component referencing the stored energy of the item stack
     * @param capacity        The max capacity of the energy being stored
     * @param maxReceive      The max per-transfer power input rate
     * @param maxExtract      The max per-transfer power output rate
     */
    public ComponentEnergyBuffer(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxReceive, int maxExtract) {
        this.parent = parent;
        this.energyComponent = energyComponent;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    /**
     * Creates a new ItemEnergyStorage with a unified receive / extract rate.
     */
    public ComponentEnergyBuffer(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
        this(parent, energyComponent, capacity, maxTransfer, maxTransfer);
    }

    /**
     * Creates a new ItemEnergyStorage with a transfer rate equivalent to the capacity.
     */
    public ComponentEnergyBuffer(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity) {
        this(parent, energyComponent, capacity, capacity);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public int insert(int index, int amount, TransferAction action) {
        if (!allowsInsertion(index) || amount <= 0) {
            return 0;
        }

        int energy = this.getAmount(index);
        int energyReceived = Mth.clamp(this.capacity - energy, 0, Math.min(this.maxReceive, amount));
        if (action.isExecuting() && energyReceived > 0) {
            set(index, energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public int insert(int amount, TransferAction action) {
        return insert(0, amount, action);
    }

    @Override
    public int extract(int index, int amount, TransferAction action) {
        if (!allowsExtraction() || amount <= 0) {
            return 0;
        }

        int energy = getAmount(index);
        int energyExtracted = Math.min(energy, Math.min(this.maxExtract, amount));
        if (action.isExecuting() && energyExtracted > 0) {
            set(index, energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public int extract(int amount, TransferAction action) {
        return extract(0, amount, action);
    }

    @Override
    public int getAmount(int index) {
        int rawEnergy = this.parent.getOrDefault(this.energyComponent, 0);
        return Mth.clamp(rawEnergy, 0, this.capacity);
    }


    @Override
    public int getCapacity(int index) {
        return this.capacity;
    }

    @Override
    public boolean allowsInsertion() {
        return allowsInsertion(0);
    }

    @Override
    public boolean allowsExtraction() {
        return allowsExtraction(0);
    }

    @Override
    public boolean allowsExtraction(int index) {
        return this.maxExtract > 0;
    }

    @Override
    public boolean allowsInsertion(int index) {
        return this.maxReceive > 0;
    }

    /**
     * Writes a new energy value to the data component. Clamps to [0, capacity]
     *
     * @param energy The new energy value
     */
    public void set(int index, int energy) {
        int realEnergy = Mth.clamp(energy, 0, this.capacity);
        this.parent.set(this.energyComponent, realEnergy);
    }
}
