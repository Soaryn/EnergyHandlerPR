/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.debug.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.TransferAction;
import net.neoforged.neoforge.transfer.energy.DefaultEnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyBuffer;
import net.neoforged.neoforge.transfer.energy.VoidEnergyBuffer;
import net.neoforged.neoforge.transfer.handlers.IEnergyHandler;
import net.neoforged.testframework.TestFramework;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.OnInit;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import net.neoforged.testframework.registration.DeferredBlocks;
import net.neoforged.testframework.registration.DeferredItems;
import net.neoforged.testframework.registration.RegistrationHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ForEachTest(groups = "handlers.energy_handler")
public class EnergyHandlerTests {
    public static final int SUB_BUFFER_COUNT = 3;
    public static final int MAX_INSERTION = 100;
    public static final int MAX_CAPACITY = 1000;

    private static final RegistrationHelper HELPER = RegistrationHelper.create("energy_handler_tests");
    private static final DeferredItems ITEMS = HELPER.items();
    private static final DeferredBlocks BLOCKS = HELPER.blocks();
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = HELPER.registrar(Registries.BLOCK_ENTITY_TYPE);


    private static final DeferredBlock<Block> ENERGY_BLOCK = BLOCKS.registerBlock("energy_block", EnergyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    private static final DeferredItem<BlockItem> ENERGY_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ENERGY_BLOCK);
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyBlock.Entity>> ENERGY_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "energy",
            () -> new BlockEntityType<>(EnergyBlock.Entity::new, ENERGY_BLOCK.get())
    );


    @OnInit
    static void init(final TestFramework framework) {
        var bus = framework.modEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);

        bus.<RegisterCapabilitiesEvent>addListener(e -> {
            e.registerBlock(
                    Capabilities.EnergyHandler.BLOCK, (level, pos, state, blockEntity, context) -> {
                        if (context == null || !(blockEntity instanceof EnergyBlock.Entity energyEntity)) return null;
                        return switch (context) {
                            case Direction.UP, Direction.DOWN -> energyEntity.simpleEnergyBuffer;
                            case Direction.EAST -> energyEntity.complexEnergyBuffer;
                            default -> null;
                        };
                    }, ENERGY_BLOCK.get()
            );
        });
    }

    /**
     * Uses {@link IEnergyHandler}, {@link EnergyBuffer}
     */
    @GameTest
    @EmptyTemplate
    @TestHolder(description = "Tests IEnergyHandler extraction/insertion using the reference EnergyBuffer.")
    public static void testEnergyHandler(ExtendedGameTestHelper helper) {
        var blockPos = new BlockPos(1, 1, 1);
        helper.setBlock(blockPos, ENERGY_BLOCK.value());

        var generalAccessCap = helper.getCapability(Capabilities.EnergyHandler.BLOCK, blockPos, null);

        helper.assertNull(generalAccessCap, "Energy Capability was not properly assigned and was accessible without a direction.");
        var simpleBufferUpCap = helper.requireCapability(Capabilities.EnergyHandler.BLOCK, blockPos, Direction.UP);
        var simpleBufferDownCap = helper.requireCapability(Capabilities.EnergyHandler.BLOCK, blockPos, Direction.DOWN);
        var complexBufferCap = helper.requireCapability(Capabilities.EnergyHandler.BLOCK, blockPos, Direction.EAST);

        //Validates the sub-buffer count
        helper.assertValueEqual(complexBufferCap.size(), SUB_BUFFER_COUNT, "The number of sub-buffers should be " + SUB_BUFFER_COUNT);
        helper.assertValueEqual(simpleBufferUpCap.size(), 1, "The number of sub-buffers should be " + 1);

        //Validates whether we are able to over extract
        helper.assertValueEqual(simpleBufferUpCap.extract(MAX_CAPACITY + 100, TransferAction.EXECUTE), MAX_CAPACITY, "Extracted energy should be equal to the %d value.".formatted(MAX_CAPACITY));
        helper.assertValueEqual(EnergyHandlerUtil.getAmount(simpleBufferDownCap), 0, "The remaining energy should be 0");
        helper.assertValueEqual(simpleBufferUpCap.insert(MAX_CAPACITY, TransferAction.EXECUTE), MAX_INSERTION, "Inserted energy should be equal to %d".formatted(MAX_INSERTION));
        helper.assertValueNotEqual(simpleBufferUpCap.insert(MAX_INSERTION, TransferAction.SIMULATE), MAX_INSERTION + 10, "The input value should NOT be more than what it could be");
        //This should show that simulate didn't commit its value.
        helper.assertValueEqual(EnergyHandlerUtil.getAmount(simpleBufferDownCap), MAX_INSERTION, "The amount stored should be " + MAX_INSERTION);

        //Validating sub-buffer usage
        //0 + 100 + 0 = 100
        helper.assertValueEqual(EnergyHandlerUtil.getAmount(complexBufferCap), 100, "The amount stored should be 100");
        helper.assertValueEqual(complexBufferCap.getAmount(0), 0, "The amount stored should be 0");
        helper.assertValueEqual(complexBufferCap.getAmount(1), 100, "The amount stored should be 100");
        helper.assertValueEqual(complexBufferCap.getAmount(2), 0, "The amount stored should be 0");

        complexBufferCap.insert(1, 100, TransferAction.EXECUTE);
        helper.assertValueEqual(EnergyHandlerUtil.getAmount(complexBufferCap), 200, "The amount stored should be 200");
        helper.assertValueEqual(complexBufferCap.getAmount(0), 0, "The amount stored should be 0");
        helper.assertValueEqual(complexBufferCap.getAmount(1), 200, "The amount stored should be 200");
        helper.assertValueEqual(complexBufferCap.getAmount(2), 0, "The amount stored should be 0");

        helper.assertValueEqual(complexBufferCap.insert(MAX_CAPACITY, TransferAction.EXECUTE), MAX_INSERTION, "Should have accepted all");
        helper.assertValueEqual(complexBufferCap.extract(MAX_CAPACITY, TransferAction.EXECUTE), 0, "Should have not allowed any");
        helper.assertFalse(complexBufferCap.allowsExtraction(), "Should now allow extraction");

        helper.assertFalse(DefaultEnergyBuffer.INSTANCE.allowsInsertion(), "Empty should no-op");
        helper.assertFalse(DefaultEnergyBuffer.INSTANCE.allowsExtraction(), "Empty should no-op");
        helper.assertValueEqual(DefaultEnergyBuffer.INSTANCE.getAmount(0), 0, "Empty should no-op");
        helper.assertValueEqual(DefaultEnergyBuffer.INSTANCE.getCapacity(0), 0, "Empty should no-op");

        var voidBuffer = new VoidEnergyBuffer();
        helper.assertTrue(voidBuffer.allowsInsertion(), "Void should allow insertion");
        helper.assertFalse(voidBuffer.allowsExtraction(), "Void should not allow extraction");
        helper.assertValueEqual(voidBuffer.getAmount(0), 0, "Void should have none");
        helper.assertValueEqual(voidBuffer.getCapacity(0), 2000000000, "Void should have near max int");


        helper.succeed();
    }


    private static class EnergyBlock extends Block implements EntityBlock {
        public EnergyBlock(Properties properties) {
            super(properties);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
            return new Entity(pos, state);
        }
        private static class Entity extends BlockEntity {
            private final IEnergyHandler simpleEnergyBuffer = EnergyBuffer.Builder.create(1, MAX_CAPACITY).energy(MAX_CAPACITY).maxInsertRate(MAX_INSERTION).build();
            private final IEnergyHandler complexEnergyBuffer = EnergyBuffer.Builder.create(3, MAX_CAPACITY).maxInsertRate(MAX_INSERTION).maxExtractRate(0).energy(1, 100).build();

            public Entity(BlockPos pos, BlockState state) {
                super(ENERGY_BLOCK_ENTITY.get(), pos, state);
            }
        }
    }

}
