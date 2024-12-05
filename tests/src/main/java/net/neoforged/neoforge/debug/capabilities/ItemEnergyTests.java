/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.debug.capabilities;

import com.mojang.serialization.Codec;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.energy.ComponentEnergyBuffer;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import net.neoforged.testframework.DynamicTest;
import net.neoforged.testframework.TestFramework;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.OnInit;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.registration.DeferredItems;
import net.neoforged.testframework.registration.RegistrationHelper;

@ForEachTest(groups = "capabilities.itemenergy")
public class ItemEnergyTests {
    public static final int MAX_CAPACITY = 16384;

    private static final RegistrationHelper HELPER = RegistrationHelper.create("item_energy_tests");

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = HELPER.registrar(Registries.DATA_COMPONENT_TYPE);
    private static final Supplier<DataComponentType<Integer>> ENERGY_COMPONENT = COMPONENTS.register("test_energy", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, MAX_CAPACITY))
            .networkSynchronized(ByteBufCodecs.INT)
            .build());

    private static final DeferredItems ITEMS = HELPER.items();
    private static final DeferredItem<Item> BATTERY = ITEMS.registerItem("test_battery", props -> new Item(props.component(ENERGY_COMPONENT, MAX_CAPACITY)));

    @OnInit
    static void init(final TestFramework framework) {
        COMPONENTS.register(framework.modEventBus());
        ITEMS.register(framework.modEventBus());
        framework.modEventBus().<RegisterCapabilitiesEvent>addListener(e -> {
            e.registerItem(
                    Capabilities.EnergyHandler.ITEM, (stack, ctx) -> {
                return new ComponentEnergyBuffer(stack, ENERGY_COMPONENT.get(), MAX_CAPACITY);
            }, BATTERY);
        });
    }

    @GameTest
    @EmptyTemplate
    @TestHolder(description = "Tests that ComponentEnergyStorage can read and write from a data component")
    public static void testItemEnergy(DynamicTest test, RegistrationHelper reg) {
        test.onGameTest(helper -> {
            ItemStack stack = BATTERY.toStack();
            IEnergyHandler energy = stack.getCapability(Capabilities.EnergyHandler.ITEM);
            Objects.requireNonNull(energy,"EnergyHandler must not be null.");

            helper.assertValueEqual(EnergyHandlerUtil.getAmount(energy), MAX_CAPACITY, "Default stored energy should be equal to the max capacity.");

            helper.assertValueEqual(energy.extract(MAX_CAPACITY, TransferAction.EXECUTE), MAX_CAPACITY, "Extracted energy should be equal to the target value.");
            helper.assertValueEqual(EnergyHandlerUtil.getAmount(energy), 0, "Post-extraction energy stored should be zero.");

            // Sanity check the real component here
            var component = stack.get(ENERGY_COMPONENT);
            Objects.requireNonNull(component, "Energy Component must not be null.");
            helper.assertValueEqual(component, 0, "Post-extraction data component value should be zero.");

            helper.assertValueEqual(energy.insert(MAX_CAPACITY, TransferAction.EXECUTE), MAX_CAPACITY, "Received energy should be equal to the target value.");
            helper.assertValueEqual(EnergyHandlerUtil.getAmount(energy), MAX_CAPACITY, "Post-insertion energy stored should be max capacity.");

            helper.succeed();
        });
    }
}
