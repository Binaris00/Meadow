package net.satisfy.meadow.block.entity;

import de.cristelknight.doapi.common.world.ImplementedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.meadow.block.CookingCauldronBlock;
import net.satisfy.meadow.client.gui.handler.CookingCauldronGuiHandler;
import net.satisfy.meadow.recipes.CookingCauldronRecipe;
import net.satisfy.meadow.registry.BlockEntityRegistry;
import net.satisfy.meadow.registry.RecipeRegistry;
import net.satisfy.meadow.registry.TagRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("deprecation, unused")
public class CookingCauldronBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider {
    private static final int MAX_CAPACITY = 7;
    private static final int MAX_COOKING_TIME = 200;
    private static final int OUTPUT_SLOT = 0;
    private static final int INGREDIENTS_AREA = 6;
    private static final int[] SLOTS_FOR_REST = {1, 2, 3, 4, 5, 6};
    private static final int[] SLOTS_FOR_DOWN = {0};
    public static int getMaxCookingTime() {
        return MAX_COOKING_TIME;
    }

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
    private int cookingTime;
    private boolean isBeingBurned;
    private final ContainerData delegate = new ContainerData() {
        public int get(int index) {
            if (index == 0) return cookingTime;
            else if (index == 1) return isBeingBurned ? 1 : 0;
            return 0;
        }

        public void set(int index, int value) {
            if (index == 0) cookingTime = value;
            else if (index == 1) isBeingBurned = value != 0;
        }

        public int getCount() {
            return 2;
        }
    };

    public CookingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COOKING_CAULDRON.get(), pos, state);
    }

    public int @NotNull [] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_REST;
    }


    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        ContainerHelper.loadAllItems(compoundTag, inventory, provider);
        cookingTime = compoundTag.getInt("CookingTime");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, inventory, provider);
        compoundTag.putInt("CookingTime", cookingTime);
    }

    public boolean isBeingBurned() {
        if (getLevel() == null) throw new NullPointerException("Null world invoked");
        if (getBlockState().getValue(CookingCauldronBlock.HANGING)) return true;
        var optionalList = BuiltInRegistries.BLOCK.getTag(TagRegistry.ALLOWS_COOKING);
        var entryList = optionalList.orElse(null);
        return entryList != null && entryList.contains(getLevel().getBlockState(getBlockPos().below()).getBlock().builtInRegistryHolder());
    }

    private boolean canCraft(CookingCauldronRecipe recipe) {
        if (recipe == null || recipe.getResultItem().isEmpty()) return false;
        ItemStack outputSlotStack = getItem(OUTPUT_SLOT);
        return outputSlotStack.isEmpty() || (ItemStack.isSameItem(outputSlotStack, recipe.getResultItem()) && outputSlotStack.getCount() < outputSlotStack.getMaxStackSize());
    }

    private void craft(CookingCauldronRecipe recipe) {
        if (!canCraft(recipe)) return;

        ItemStack recipeOutput = recipe.assemble();
        ItemStack outputSlotStack = getItem(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            setItem(OUTPUT_SLOT, recipeOutput.copy());
        } else if (ItemStack.isSameItem(outputSlotStack, recipe.getResultItem()) && outputSlotStack.getCount() + recipeOutput.getCount() <= outputSlotStack.getMaxStackSize()) {
            outputSlotStack.grow(recipeOutput.getCount());
        }

        boolean[] ingredientUsed = new boolean[INGREDIENTS_AREA + 1];

        for (Ingredient ingredient : recipe.getIngredients()) {
            for (int slotIndex = 1; slotIndex <= INGREDIENTS_AREA; slotIndex++) {
                if (!ingredientUsed[slotIndex] && ingredient.test(getItem(slotIndex))) {
                    ingredientUsed[slotIndex] = true;
                    ItemStack stackInSlot = getItem(slotIndex);
                    ItemStack remainderStack = getRemainderItem(stackInSlot);
                    stackInSlot.shrink(1);
                    if (!remainderStack.isEmpty()) {
                        if (stackInSlot.isEmpty()) {
                            setItem(slotIndex, remainderStack);
                        } else {
                            handleRemainder(remainderStack, slotIndex);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void handleRemainder(ItemStack remainderStack, int originalSlot) {
        boolean added = false;
        for (int i = 1; i <= INGREDIENTS_AREA; i++) {
            ItemStack is = getItem(i);
            if (is.isEmpty()) {
                setItem(i, remainderStack.copy());
                added = true;
                break;
            } else if (ItemStack.isSameItem(is, remainderStack) && is.getCount() + remainderStack.getCount() <= is.getMaxStackSize()) {
                is.grow(remainderStack.getCount());
                added = true;
                break;
            }
        }
        if (!added) {
            dropItemIntoWorld(remainderStack, worldPosition);
        }
    }

    private ItemStack getRemainderItem(ItemStack stack) {
        if (stack.getItem().hasCraftingRemainingItem()) {
            return new ItemStack(Objects.requireNonNull(stack.getItem().getCraftingRemainingItem()));
        }
        return ItemStack.EMPTY;
    }

    private void dropItemIntoWorld(ItemStack itemStack, BlockPos pos) {
        if (level != null && !level.isClientSide()) {
            double offsetX = level.random.nextDouble() * 0.7 + 0.15;
            double offsetY = level.random.nextDouble() * 0.5 + 0.1;
            double offsetZ = level.random.nextDouble() * 0.7 + 0.15;
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ, itemStack);
            level.addFreshEntity(itemEntity);
        }
    }

    public void tick(Level world, BlockPos pos, BlockState state) {
        if (world.isClientSide()) return;

        isBeingBurned = isBeingBurned();
        if (!isBeingBurned && state.getValue(CookingCauldronBlock.LIT)) {
            world.setBlock(pos, state.setValue(CookingCauldronBlock.LIT, false), Block.UPDATE_ALL);
            return;
        }

        RecipeManager recipeManager = world.getRecipeManager();
        List<RecipeHolder<CookingCauldronRecipe>> recipes = recipeManager.getAllRecipesFor(RecipeRegistry.COOKING.get());
        Optional<CookingCauldronRecipe> recipe = Optional.ofNullable(getRecipe(recipes));


        if (recipe.isPresent() && canCraft(recipe.get())) {
            cookingTime++;
            if (cookingTime >= MAX_COOKING_TIME) {
                cookingTime = 0;
                craft(recipe.get());
            }
            if (!state.getValue(CookingCauldronBlock.COOKING)) {
                world.setBlock(pos, state.setValue(CookingCauldronBlock.COOKING, true).setValue(CookingCauldronBlock.LIT, true), Block.UPDATE_ALL);
            }
        } else {
            cookingTime = 0;
            if (state.getValue(CookingCauldronBlock.COOKING)) {
                world.setBlock(pos, state.setValue(CookingCauldronBlock.COOKING, false).setValue(CookingCauldronBlock.LIT, true), Block.UPDATE_ALL);
            }
        }
    }

    public CookingCauldronRecipe getRecipe(List<RecipeHolder<CookingCauldronRecipe>> recipes) {
        recipeLoop:
        for (RecipeHolder<CookingCauldronRecipe> recipeHolder : recipes) {
            CookingCauldronRecipe recipe = recipeHolder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                boolean ingredientFound = false;
                for (int slotIndex = 1; slotIndex <= INGREDIENTS_AREA; slotIndex++) {
                    ItemStack slotItem = inventory.get(slotIndex);
                    if (ingredient.test(slotItem)) {
                        ingredientFound = true;
                        break;
                    }
                }
                if (!ingredientFound) {
                    continue recipeLoop;
                }
            }
            return recipe;
        }
        return null; // No matching recipe found
    }

    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    public boolean stillValid(Player player) {
        return player.distanceToSqr((double) worldPosition.getX() + 0.5, (double) worldPosition.getY() + 0.5, (double) worldPosition.getZ() + 0.5) <= 64.0;
    }

    public @NotNull Component getDisplayName() {
        return Component.nullToEmpty("");
    }

    public @Nullable AbstractContainerMenu createMenu(int syncId, @NotNull Inventory inv, @NotNull Player player) {
        return new CookingCauldronGuiHandler(syncId, inv, this, delegate);
    }
}