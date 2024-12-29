/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.energy;

import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import org.jetbrains.annotations.Range;

public final class EnergyHandlerUtil {
    /**
     * A near max int value intended to be easier to view in normal gameplay. (2E9)
     */
    public static final int MAX_VALUE = 2000000000;

    /**
     * A non-null {@link IEnergyHandler} that performs no operations.
     */
    public static final IEnergyHandler EMPTY = new IEnergyHandler() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
        public int getAmount(int index) {
            return 0;
        }

        @Override
        @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
        public int getCapacity(int index) {
            return 0;
        }

        @Override
        public boolean allowsInsertion(int index) {
            return false;
        }

        @Override
        public boolean allowsExtraction(int index) {
            return false;
        }

        @Override
        public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int insert(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }

        @Override
        public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int insert(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }

        @Override
        public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int extract(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }

        @Override
        public @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int extract(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }
    };

    /**
     * @param handler Energy Handler to iterate
     * @return Total energy stored across all of its sub-buffers. Returns {@link Integer#MAX_VALUE} if it would have otherwise overflowed.
     */
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    public static int getAmount(IEnergyHandler handler) {
        var sum = 0;
        for (var i = 0; i < handler.size(); i++) {
            var amount = handler.getAmount(i);
            //If we overflow, we can stop and return max value.
            if (sum + amount < sum) return EnergyHandlerUtil.MAX_VALUE;
            sum += amount;
        }
        return sum;
    }

    /**
     * @param handler Energy Handler to iterate
     * @return Total capacity across all of its sub-buffers. Returns {@link Integer#MAX_VALUE} if it would have otherwise overflowed.
     */
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    public static int getCapacity(IEnergyHandler handler) {
        var sum = 0;
        for (var i = 0; i < handler.size(); i++) {
            var amount = handler.getCapacity(i);
            //If we overflow, we can stop and return max value.
            if (sum + amount < sum) return EnergyHandlerUtil.MAX_VALUE;
            sum += amount;
        }
        return sum;
    }

    private EnergyHandlerUtil() {}
}
