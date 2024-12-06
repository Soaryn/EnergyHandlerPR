/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.energy;

import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;

/**
 * An energy storage is the unit of interaction with Energy inventories.
 * <p>
 * A reference implementation can be found at {@link EnergyStorage}.
 * <p>
 * Derived from the Redstone Flux power system designed by King Lemming and originally utilized in Thermal Expansion and related mods.
 * Created with consent and permission of King Lemming and Team CoFH. Released with permission under LGPL 2.1 when bundled with Forge.
 * 
 * @deprecated {@link IEnergyHandler}
 */
@Deprecated(since = "1.21.4", forRemoval = true)
public interface IEnergyStorage extends IEnergyHandler {
    /**
     * Adds energy to the storage. Returns the amount of energy that was accepted.
     *
     * @param toReceive The amount of energy being received.
     * @param simulate  If true, the insertion will only be simulated, meaning {@link #getEnergyStored()} will not change.
     * @return Amount of energy that was (or would have been, if simulated) accepted by the storage.
     */
    int receiveEnergy(int toReceive, boolean simulate);

    /**
     * Removes energy from the storage. Returns the amount of energy that was removed.
     *
     * @param toExtract The amount of energy being extracted.
     * @param simulate  If true, the extraction will only be simulated, meaning {@link #getEnergyStored()} will not change.
     * @return Amount of energy that was (or would have been, if simulated) extracted from the storage.
     */
    int extractEnergy(int toExtract, boolean simulate);

    /**
     * Returns the amount of energy currently stored.
     */
    int getEnergyStored();

    /**
     * Returns the maximum amount of energy that can be stored.
     */
    int getMaxEnergyStored();

    /**
     * Returns if this storage can have energy extracted.
     * If this is false, then any calls to extractEnergy will return 0.
     */
    boolean canExtract();

    /**
     * Used to determine if this storage can receive energy.
     * If this is false, then any calls to receiveEnergy will return 0.
     */
    boolean canReceive();

    //These are only added to allow easier developer ease into IEnergyHandler.
    // It is not advised to use these overrides, and rather look at EnergyBuffer as a reference
    @Override
    default int size() {
        return 1;
    }

    @Override
    default int getAmount(int index) {
        return getEnergyStored();
    }

    @Override
    default int getCapacity(int index) {
        return getMaxEnergyStored();
    }

    @Override
    default boolean allowsInsertion(int index) {
        return canReceive();
    }

    @Override
    default boolean allowsExtraction(int index) {
        return canExtract();
    }

    @Override
    default boolean allowsInsertion() {
        return allowsInsertion(0);
    }

    @Override
    default boolean allowsExtraction() {
        return allowsExtraction(0);
    }

    @Override
    default int insert(int index, int amount, TransferAction action) {
        return receiveEnergy(amount, action.isSimulating());
    }

    @Override
    default int extract(int index, int amount, TransferAction action) {
        return extractEnergy(amount, action.isSimulating());
    }

    @Override
    default int insert(int amount, TransferAction action) {
        return insert(0, amount, action);
    }

    @Override
    default int extract(int amount, TransferAction action) {
        return extract(0, amount, action);
    }
}
