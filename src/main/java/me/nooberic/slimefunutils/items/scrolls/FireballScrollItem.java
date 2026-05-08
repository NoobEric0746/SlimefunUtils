package me.nooberic.slimefunutils.items.scrolls;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

public class FireballScrollItem extends SimpleSlimefunItem<ItemUseHandler> {

    private final JavaPlugin plugin;

    public FireballScrollItem(JavaPlugin plugin, ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        this.plugin = plugin;
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return event -> {
            event.cancel();

            Player player = event.getPlayer();
            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeItem(event.getItem());
            }

            for (int i = 0; i < 3; i++) {
                int delay = i * 4;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> launchFireball(player), delay);
            }
        };
    }

    private void launchFireball(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        Vector direction = player.getEyeLocation().getDirection().normalize();
        SmallFireball fireball = player.launchProjectile(SmallFireball.class, direction.multiply(0.9));
        fireball.setShooter(player);
        fireball.setIsIncendiary(true);
        fireball.setYield(0F);
    }

    private void consumeItem(ItemStack item) {
        int amount = item.getAmount();

        if (amount <= 1) {
            item.setAmount(0);
            return;
        }

        item.setAmount(amount - 1);
    }
}
