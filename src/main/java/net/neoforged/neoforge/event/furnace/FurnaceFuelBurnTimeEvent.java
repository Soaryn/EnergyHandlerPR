/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event.furnace;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * {@link FurnaceFuelBurnTimeEvent} is fired when determining the fuel value for an ItemStack. <br>
 * <br>
 * To set the burn time of your own item, use {@link Item#getBurnTime(ItemStack, RecipeType, FuelValues)} instead.<br>
 * <br>
 * This event is fired from {@link EventHooks#getItemBurnTime(ItemStack, int, RecipeType, FuelValues)}.<br>
 * <br>
 * This event is {@link ICancellableEvent} to prevent later handlers from changing the value.<br>
 * <br>
 * This event is fired on the {@link NeoForge#EVENT_BUS}.
 **/
public class FurnaceFuelBurnTimeEvent extends Event implements ICancellableEvent {
    private final ItemStack itemStack;
    @Nullable
    private final RecipeType<?> recipeType;
    private final FuelValues fuelValues;
    private int burnTime;

    public FurnaceFuelBurnTimeEvent(ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        this.itemStack = itemStack;
        this.burnTime = burnTime;
        this.recipeType = recipeType;
        this.fuelValues = fuelValues;
    }

    /**
     * Get the ItemStack "fuel" in question.
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     *
     * Get the recipe type for which to obtain the burn time, if known.
     */
    @Nullable
    public RecipeType<?> getRecipeType() {
        return recipeType;
    }

    /**
     * Get the {@link FuelValues} populated from the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#FURNACE_FUELS data map}
     */
    public FuelValues getFuelValues() {
        return fuelValues;
    }

    /**
     * Set the burn time for the given ItemStack.
     * Setting it to 0 will prevent the item from being used as fuel, overriding vanilla's decision.
     */
    public void setBurnTime(int burnTime) {
        if (burnTime >= 0) {
            this.burnTime = burnTime;
            setCanceled(true);
        }
    }

    /**
     * The resulting value of this event, the burn time for the ItemStack.
     * A value of 0 will prevent the item from being used as fuel, overriding vanilla's decision.
     * <p>
     * The initial burn time can come from either the {@link net.neoforged.neoforge.common.extensions.IItemExtension#getBurnTime(ItemStack, RecipeType, FuelValues) extension method}
     * or the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#FURNACE_FUELS data map} through {@link FuelValues}.
     */
    public int getBurnTime() {
        return burnTime;
    }
}
