/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.handlers;

import net.neoforged.neoforge.transfer.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.TransferAction;
import org.jetbrains.annotations.Range;

// Primer Notes:
// - Changed name from IEnergyStorage -> IEnergyHandler
// > Renames to handler to be consistent with the new changes to IResourceHandler as well as communicate more that this 'handles' energy not necessarily stores.
// That is driven by the individual implementation.
//
// - Adds index parameters to methods to match IResourceHandler.
// > This allows the ability to control individual "slots" or buffers of a given handler.
// > This does NOT force a mod author to use the indexed variants for their handler, but rather provide a way to expose them if desired. EnergyBuffer is an example where both can co-exist
// Simple containers can just use a size of 1 if desired, the same as FluidTank has done for a decade (not exaggerating).
// > The methods renamed are marked below with `Formerly`. Ensure to remove these notes as well as the markings below

// Soaryn PR Notes: There is notably some technical debt by adding the indices/sub-buffers, but it is very minor and this is in favor of providing new opportunities for devs,
// as well as maintain consistency with the other handlers. This is mostly an opt-in given the logic remains the same for the most part on single buffer energy handlers.

/**
 * Formerly `IEnergyStorage`
 * <p>
 * A capability interface providing the methods such as insert/extract energy from a handler.<br>
 * To use Neo's energy capability see the {@link net.neoforged.neoforge.capabilities.Capabilities.EnergyHandler#BLOCK EnergyCapability} in {@link net.neoforged.neoforge.capabilities.Capabilities Capabilities}.
 * <br>
 * To make your own energy system using this interface, you can register a new capability using something like the following.
 * 
 * <pre>
 * {@code public static final BlockCapability<IEnergyHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath([MOD_ID], [CUSTOM_ENERGY_NAME]), IEnergyHandler.class);}
 * </pre>
 * 
 * This would effectively create a new capability that other mods could utilize so long as they create a new capability with the same id without needing any extra API provided by you.
 */
public interface IEnergyHandler {
    /**
     * <b>PRIMER: New</b> - Required for the indexed methods below to be inquired correctly.
     *
     * @return The number of indices this handler manages.
     */
    @Range(from = 1, to = Integer.MAX_VALUE)
    int size();

    /**
     * <b>PRIMER: Formerly</b> `getEnergyStored`. Now needs an index.
     * <p>
     * To know the total amount of energy stored in a {@link IEnergyHandler} consider using {@link EnergyHandlerUtil#getAmount}
     *
     * @param index The index to get the amount from.
     * @return The amount of energy stored at the given index. This should be non-negative.
     */
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
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
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
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
     * <p>
     * An estimation of or hint if {@link IEnergyHandler#extract} would result in any energy. This should not be used to determine if something is empty, nor should it return as such.
     * A typical use case is identifying which implementations of {@link IEnergyHandler} in a group would be able to be extractable.
     * <p>
     * <b>IMPORTANT:</b> This doesn't add any control, this is merely a guide for things like pipes to know ahead of time if it can be ever extracted from.
     *
     * @return True if the handler can be extracted from in its configuration, false otherwise. If using indices, this should return true if any index allows insertion. It is advised to <b>not</b> return false when the buffer is empty.
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
     * <p>
     * An estimation of or hint if {@link IEnergyHandler#insert} would result in any energy. This should not be used to determine if something is full, nor should it return as such.
     * A typical use case is identifying which implementations of {@link IEnergyHandler} in a group would be able to be extractable.
     * <p>
     * <b>IMPORTANT:</b> This doesn't add any control, this is merely a guide for things like pipes to know ahead of time if it can be ever extracted from.
     *
     * @param index The index to check
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
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    int insert(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action);

    /**
     * <b>PRIMER: Formerly</b> `receiveEnergy(int toReceive, bool simulate)`
     * <p>
     * Inserts a given amount into the handler. Distribution is up to the handler.
     * <p>
     * When implementing, it is advised to not make this call {@link IEnergyHandler#insert(int, int, TransferAction) insert(index, ...)} for each index directly,
     * but rather reuse the logic already checked. See {@link net.neoforged.neoforge.transfer.energy.EnergyBuffer#insertCommon(int, int, TransferAction) EnergyBuffer.insertCommon} for a reference of an implementation.
     *
     * @param amount The amount to insert.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) inserted. This should be non-negative.
     */
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    int insert(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action);

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
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    int extract(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action); //

    /**
     * <b>PRIMER: Formerly</b> `extractEnergy(int toReceive, bool simulate)`
     * <p>
     * Extracts a given amount from the handler. Distribution is up to the handler.
     * <p>
     * When implementing, it is advised to not make this call {@link IEnergyHandler#extract(int, int, TransferAction) extract(index, ...)} for each index directly,
     * but rather reuse the logic already checked. See {@link net.neoforged.neoforge.transfer.energy.EnergyBuffer#extractCommon(int, int, TransferAction) EnergyBuffer.extractCommon} for a reference of an implementation.
     *
     * @param amount The amount of the resource to extract.
     * @param action The kind of action being performed. {@link TransferAction#SIMULATE} will simulate the action
     *               while {@link TransferAction#EXECUTE} will actually perform the action.
     * @return The amount that was (or would have been, if simulated) extracted. This should be non-negative.
     */
    @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE)
    int extract(@Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount, TransferAction action);

    /**
     * <b>PRIMER: New</b>
     * <p>
     * An energy handler extension when wanting to expose direct energy value mutations of a given index.
     * This is purely optional, and is mainly provided to allow mod authors to expose a direct `set` for
     * operations such as "emptying" or "filling" an energy container, non-explicitly
     */
    interface Modifiable extends IEnergyHandler {
        /**
         * Sets the amount at the given index to the supplied value.
         *
         * @param index  The index for what value to set.
         * @param amount The value to set. This should be non-negative.
         */
        void set(int index, @Range(from = 0, to = EnergyHandlerUtil.MAX_VALUE) int amount);
    }
}
