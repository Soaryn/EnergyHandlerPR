package net.neoforged.neoforge.transfer.energy;

import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import org.jetbrains.annotations.Range;

public final class VoidEnergyBuffer implements IEnergyHandler {
    @Override
    public int size() {
        return 1;
    }
    @Override
    public int getAmount(int index) {
        return 0;
    }
    @Override
    public int getCapacity(int index) {
        //Essentially max int, but cleaner when printing
        return 2000000000;
    }
    @Override
    public boolean allowsInsertion(int index) {
        return true;
    }
    @Override
    public boolean allowsExtraction(int index) {
        return false;
    }
    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int insert(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
        return amount;
    }
    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
        return amount;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int extract(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
        return 0;
    }
    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int extract(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action) {
        return 0;
    }
}
