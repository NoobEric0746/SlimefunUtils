package me.nooberic.slimefunutils.items.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class GazhiAmuletItem extends SlimefunItem {

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final double MAX_DISTANCE = 64.0D;
    private static final double MIN_DISTANCE = 0.1D;
    private static final double LOOK_TOLERANCE = 0.5D;
    private static final String ACTIONBAR_TEMPLATE = "&e%viewer% &7正在看着你";

    public GazhiAmuletItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void postRegister() {
        JavaPlugin plugin = getAddon().getJavaPlugin();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkViewers, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private void checkViewers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return;
        }

        List<Player> players = new ArrayList<>(onlinePlayers);
        for (Player target : players) {
            if (!hasAmulet(target)) {
                continue;
            }

            if (!canUse(target, false)) {
                continue;
            }

            List<Player> viewers = findViewers(target, players);
            if (!viewers.isEmpty()) {
                sendActionBar(target, ACTIONBAR_TEMPLATE.replace("%viewer%", joinViewerNames(viewers)));
            }
        }
    }

    private List<Player> findViewers(Player target, List<Player> players) {
        List<Player> viewers = new ArrayList<>();

        for (Player viewer : players) {
            if (viewer.equals(target)) {
                continue;
            }

            if (!viewer.isOnline() || viewer.isDead()) {
                continue;
            }

            if (!viewer.getWorld().equals(target.getWorld())) {
                continue;
            }

            if (viewer.getLocation().distanceSquared(target.getLocation()) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }

            if (isLookingAt(viewer, target)) {
                viewers.add(viewer);
            }
        }

        return viewers;
    }

    private String joinViewerNames(List<Player> viewers) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < viewers.size(); i++) {
            if (i > 0) {
                builder.append(" , ");
            }

            builder.append(viewers.get(i).getName());
        }

        return builder.toString();
    }

    private boolean isLookingAt(Player viewer, Player target) {
        Vector toTarget = target.getEyeLocation().toVector().subtract(viewer.getEyeLocation().toVector());
        double distance = toTarget.length();
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
            return false;
        }

        Vector direction = viewer.getEyeLocation().getDirection().normalize();
        Vector toTargetDirection = toTarget.normalize();
        double threshold = 1.0D - LOOK_TOLERANCE / distance;

        return direction.dot(toTargetDirection) > threshold && viewer.hasLineOfSight(target);
    }

    private boolean hasAmulet(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }

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

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
