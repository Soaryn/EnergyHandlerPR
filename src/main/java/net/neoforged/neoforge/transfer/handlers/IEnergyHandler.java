/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.handlers;

import net.neoforged.neoforge.transfer.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.TransferAction;
import org.jetbrains.annotations.Range;

//Primer Notes:
// - Changed name from IEnergyStorage -> IEnergyHandler
//      > Renames to handler to be consistent with the new changes to IResourceHandler.
//
// - Adds index parameters to methods to match IResourceHandler.
//      > This allows not only consistency, but also the ability to control individual "slots" or buffers of a given handler.
//      > This does NOT force a mod author to use the indexed variants for their handler, but rather provide a way to expose them if desired.
//        Simple containers can just use a size of 1 if desired, the same as FluidTank has done for a decade (not exaggerating).
//      > The methods renamed are marked below. Ensure to remove these notes as well as the markings below

// Soaryn PR Notes: There is notably some technical debt by adding the indices, but this is in favor of providing new opportunities for devs,
//   as well as maintain consistency with IResourceHandler. This is mostly an opt-in given the logic remains the same for the most part on single buffer energy handlers.

/**
 * Formerly `IEnergyStorage`
 * <p>
 * A capability interface providing the methods such as insert/extract energy from a handler.<br>
 * To use Neo's energy capability see the {@link net.neoforged.neoforge.capabilities.Capabilities.EnergyHandler#BLOCK EnergyCapability} in {@link net.neoforged.neoforge.capabilities.Capabilities Capabilities}.
 * <br>
 * To make your own energy system using this interface, you can register a new capability using something like the following.
 * <pre>
 * {@code public static final BlockCapability<IEnergyHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath([MOD_ID], [CUSTOM_ENERGY_NAME]), IEnergyHandler.class);}
 * </pre>
 * This would effectively create a new capability that other mods could utilize so long as they create a new capability with the same id without needing any extra API provided by you.
 */
public interface IEnergyHandler {
    /**
     * <b>PRIMER: New</b> - Required for the indexed methods below to be inquired correctly.
     *
     * @return The number of indices this handler manages.
     */
    int size();

    /**
     * <b>PRIMER: Formerly</b> `getEnergyStored`. Now needs an index.
     * <p>
     * To know the total amount of energy stored in a {@link IEnergyHandler} consider using {@link EnergyHandlerUtil#getAmount}
     *
     * @param index The index to get the amount from.
     * @return The amount of energy stored at the given index. This should be non-negative.
     */
    int getAmount(int index);

    /**
     * <b>PRIMER: Formerly</b> `getMaxEnergyStored`. Now has an index similar to IResourceHandler.
     * <p>
     * Gets the capacity that index can hold.
     * To know the total capacity of energy in a {@link IEnergyHandler} consider using {@link EnergyHandlerUtil#getCapacity}
     *
     * @param index The index to get the limit from.
     * @return The capacity at the given index. This should be non-negative.
     */
    int getCapacity(int index);

    /**
     * <b>PRIMER: Formerly</b> `canReceive`
     *
     * @return True if the handler can be inserted into at this time, false otherwise. If using indices, this should return true if any index allows insertion
     */
    default boolean allowsInsertion() {
        var indices = size();
        for (var index = 0; index < indices; index++) {
            if (allowsInsertion(index)) return true;
        }
        return false;
    }

    /**
     * <b>PRIMER: Formerly</b> `canExtract`
     *
     * @return True if the handler can be extracted from at this time, false otherwise. If using indices, this should return true if any index allows extraction
     */
    default boolean allowsExtraction() {
        var indices = size();
        for (var index = 0; index < indices; index++) {
            if (allowsExtraction(index)) return true;
        }
        return false;
    }

    /**
     * <b>PRIMER: New</b>
     *
     * @param index The index to check
     * @return True if at the given index, the handler can be inserted into, false otherwise.
     */
    boolean allowsInsertion(int index);

    /**
     * <b>PRIMER: New</b>
     *
     * @param index The index to check
     * @return True if at the given index, the handler can be extracted from, false otherwise.
     */
    boolean allowsExtraction(int index);

    /**
     * <b>PRIMER: New</b>
     * <p>
     * Inserts a given amount of energy into the handler at the target index. If the intent is to just arbitrarily send power to the handler, consider using {@link IEnergyHandler#insert(int, TransferAction)} instead.
     *
     * @param index  The index to insert into.
     * @param amount The value to insert.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) inserted. This should be non-negative.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int insert(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action);

    /**
     * <b>PRIMER: Formerly</b> `receiveEnergy(int toReceive, bool simulate)`
     * <p>
     * Inserts a given amount into the handler. Distribution is up to the handler. When implementing, it is advised to not make this call insert for each index directly,
     * with {@link IEnergyHandler#insert(int, int, TransferAction)}, but rather reuse the logic already checked.
     * <p>
     * [[TODO TO DOC, the code is already available at time of writing this, but it is scattered in different projects.
     * The generic IResourceHandler has an example that we can use for this. We ultimately want to encourage clean use of something commonly shared here and for extract]]
     *
     * @param amount The amount to insert.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) inserted. This should be non-negative.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action);

    /**
     * <b>PRIMER: New</b>
     * <p>
     * Extracts a given amount of energy from the handler at the given index. If the intent is to arbitrarily extract power from the handler, consider using {@link IEnergyHandler#extract(int, TransferAction)} instead.
     *
     * @param index  The index to extract from.
     * @param amount The amount to extract.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) extracted. This should be non-negative.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int extract(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action); //

    /**
     * <b>PRIMER: Formerly</b> `extractEnergy(int toReceive, bool simulate)`
     * <p>
     * Extracts a given amount from the handler. Distribution is up to the handler.
     *
     * @param amount The amount of the resource to extract.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) extracted. This should be non-negative.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    int extract(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransferAction action);

    /**
     * <b>PRIMER: New</b>
     * <p>
     * A energy handler extension when wanting to expose direct energy value mutations of a given index.
     * This is purely optional, and is mainly provided to allow mod authors to expose a direct `set` for
     * operations such as "emptying" or "filling" an energy container, non-explicitly
     */
    interface IEnergyHandlerModifiable extends IEnergyHandler {
        /**
         * Sets the amount at the given index to the supplied value.
         *
         * @param index  The index for what value to set.
         * @param amount The value to set. This should be non-negative.
         */
        void set(int index, @Range(from = 0, to = Integer.MAX_VALUE) int amount);
    }
}