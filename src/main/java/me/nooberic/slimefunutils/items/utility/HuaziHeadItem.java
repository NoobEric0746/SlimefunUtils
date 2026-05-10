package me.nooberic.slimefunutils.items.utility;

import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

public class HuaziHeadItem extends SimpleSlimefunItem<BlockPlaceHandler> {

    public HuaziHeadItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler(onBreak());
    }

    @Override
    public BlockPlaceHandler getItemHandler() {
        return new BlockPlaceHandler(true) {

            @Override
            public void onPlayerPlace(@Nonnull BlockPlaceEvent e) {
                // Slimefun's main listener already stores the block ID.
            }
        };
    }

    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(true, true) {

            @Override
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack item, @Nonnull List<ItemStack> drops) {
                e.setDropItems(false);
                drops.clear();
                drops.add(getItem().clone());
                
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), getItem().clone());
            }
        };
    }

}