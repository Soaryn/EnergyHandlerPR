package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import org.jetbrains.annotations.Range;

public final class EnergyHandlerUtil {
    /**
     * An empty instance of {@link IEnergyHandler} to have no operation performed and just accept all. This will not store, receive, or provide any energy, but will accept the instruction to do so.
     */
    public static final IEnergyHandler EMPTY = new IEnergyHandler() {
        @Override
        public int size() {
            return 0;
        }
        @Override
        public int getAmount(int index) {
            return 0;
        }
        @Override
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
        public @Range(from = 0, to = Integer.MAX_VALUE) int insert(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }
        @Override
        public @Range(from = 0, to = Integer.MAX_VALUE) int extract(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }
        @Override
        public @Range(from = 0, to = Integer.MAX_VALUE) int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }
        @Override
        public @Range(from = 0, to = Integer.MAX_VALUE) int extract(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
            return 0;
        }
    };

    /**
     * @param handler Energy Handler to iterate
     * @return Total energy stored across all of its sub-buffers. Returns {@link Integer#MAX_VALUE} if it would have otherwise overflowed.
     */
    public static int getAmount(IEnergyHandler handler) {
        var sum = 0;
        for (var i = 0; i < handler.size(); i++) {
            var amount = handler.getAmount(i);
            //If we overflow, we can stop and return the Maximum and int can be.
            if (sum + amount < sum) return Integer.MAX_VALUE;
            sum += amount;
        }
        return sum;
    }

    /**
     * @param handler Energy Handler to iterate
     * @return Total capacity across all of its sub-buffers. Returns {@link Integer#MAX_VALUE} if it would have otherwise overflowed.
     */
    public static int getCapacity(IEnergyHandler handler) {
        var sum = 0;
        for (var i = 0; i < handler.size(); i++) {
            var amount = handler.getCapacity(i);
            //If we overflow, we can stop and return the Maximum and int can be.
            if (sum + amount < sum) return Integer.MAX_VALUE;
            sum += amount;
        }
        return sum;
    }

    private EnergyHandlerUtil() { }
}
