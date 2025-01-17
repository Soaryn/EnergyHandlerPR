/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

public interface IAbstractBoatExtension {
    private AbstractBoat self() {
        return (AbstractBoat) this;
    }

    /**
     * Returns whether the boat can be used on the fluid.
     *
     * @param state the state of the fluid
     * @return {@code true} if the boat can be used, {@code false} otherwise
     */
    default boolean canBoatInFluid(FluidState state) {
        return state.supportsBoating(self());
    }

    /**
     * Returns whether the boat can be used on the fluid.
     *
     * @param type the type of the fluid
     * @return {@code true} if the boat can be used, {@code false} otherwise
     */
    default boolean canBoatInFluid(FluidType type) {
        return type.supportsBoating(self());
    }
}
