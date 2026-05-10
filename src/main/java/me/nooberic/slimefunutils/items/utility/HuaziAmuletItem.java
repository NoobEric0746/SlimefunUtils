package me.nooberic.slimefunutils.items.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class HuaziAmuletItem extends SlimefunItem {

    private static final int HEALTH_PER_PLAYER = 2;
    private static final double BASE_MAX_HEALTH = 20.0D;

    private final Map<UUID, Double> lastAppliedHealth = new HashMap<>();

    public HuaziAmuletItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void postRegister() {
        getAddon().getJavaPlugin().getServer().getScheduler().runTaskTimer(
            getAddon().getJavaPlugin(),
            this::updatePlayers,
            20L,
            20L
        );
    }

    private void updatePlayers() {
        int bonusHealth = Math.max(0, getAddon().getJavaPlugin().getServer().getOnlinePlayers().size() - 1) * HEALTH_PER_PLAYER;

        for (Player player : getAddon().getJavaPlugin().getServer().getOnlinePlayers()) {
            double targetMaxHealth = hasAmulet(player) && canUse(player, false) ? BASE_MAX_HEALTH + bonusHealth : BASE_MAX_HEALTH;
            applyMaxHealth(player, targetMaxHealth);
        }
    }

    private void applyMaxHealth(Player player, double targetMaxHealth) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        Double lastApplied = lastAppliedHealth.get(player.getUniqueId());
        if (lastApplied != null && Double.compare(lastApplied, targetMaxHealth) == 0) {
            return;
        }

        attribute.setBaseValue(targetMaxHealth);
        lastAppliedHealth.put(player.getUniqueId(), targetMaxHealth);
    }

    private boolean hasAmulet(Player player) {
        PlayerInventory inventory = player.getInventory();
        return contains(inventory.getStorageContents())
            || contains(inventory.getArmorContents())
            || contains(inventory.getExtraContents());
    }

    private boolean contains(ItemStack[] items) {
        if (items == null) {
            return false;
        }

        for (ItemStack item : items) {
            if (item != null && isItem(item)) {
                return true;
            }
        }

        return false;
    }
}
