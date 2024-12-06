/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.energy.templates;

import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import org.jetbrains.annotations.Range;

/**
 * A buffer of energy that accepts any and all energy inserted into it, but never has an extractable amount.
 * If you need custom behaviour, then a new implementation is required rather than extending {@link VoidEnergyBuffer}
 */
public final class VoidEnergyBuffer implements IEnergyHandler {
    public static final VoidEnergyBuffer INSTANCE = new VoidEnergyBuffer();

    //There is 1 valid sub-buffer that accepts all energy.
    @Override
    public int size() {
        return 1;
    }

    //Never stores energy
    @Override
    public int getAmount(int index) {
        return 0;
    }

    //Holds "infinite" energy
    @Override
    public int getCapacity(int index) {
        return EnergyHandlerUtil.MAX_VALUE;
    }

    //Always
    @Override
    public boolean allowsInsertion(int index) {
        return true;
    }

    //Voids do not provide energy
    @Override
    public boolean allowsExtraction(int index) {
        return false;
    }

    //Accepts as much as is inserted at an index which in this case is ignored
    @Override
    public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int insert(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
        return amount;
    }

    //Accepts as much as is inserted, but rather than calling the above method, it is just simpler to return the amount.
    @Override
    public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int insert(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
        return amount;
    }

    //Never has anything to extract so we return 0
    @Override
    public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int extract(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
        return 0;
    }

    //Never has anything to extract so we return 0
    @Override
    public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int extract(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
        return 0;
    }

    /**
     * Any custom implementations are expected to make their own full implementation rather than extend {@link VoidEnergyBuffer}
     */
    private VoidEnergyBuffer() {}
}
