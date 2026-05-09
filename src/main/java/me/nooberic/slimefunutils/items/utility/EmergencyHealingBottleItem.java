package me.nooberic.slimefunutils.items.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class EmergencyHealingBottleItem extends SlimefunItem implements Listener {

    private static final double TRIGGER_HEALTH = 10.0D;
    private static final int HEAL_DURATION_TICKS = 1;
    private static final int HEAL_AMPLIFIER = 0;
    private static final String HEAL_ACTIONBAR = "&a应急血瓶已触发";

    public EmergencyHealingBottleItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void postRegister() {
        JavaPlugin plugin = getAddon().getJavaPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double remainingHealth = player.getHealth() - event.getFinalDamage();
        if (remainingHealth > TRIGGER_HEALTH || remainingHealth <= 0.0D) {
            return;
        }

        Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> tryApplyHealing(player));
    }

    private void tryApplyHealing(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        if (player.getHealth() > TRIGGER_HEALTH) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        int slot = findEmergencyBottleSlot(inventory);
        if (slot < 0) {
            return;
        }

        if (!canUse(player, true)) {
            return;
        }

        consumeItem(inventory.getItem(slot));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, HEAL_DURATION_TICKS, HEAL_AMPLIFIER));
        sendActionBar(player, HEAL_ACTIONBAR);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
    }

    private int findEmergencyBottleSlot(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            if (isItem(contents[i])) {
                return i;
            }
        }

        return -1;
    }

    private void consumeItem(ItemStack item) {
        if (item == null) {
            return;
        }

        int amount = item.getAmount();
        if (amount <= 1) {
            item.setAmount(0);
            return;
        }

        item.setAmount(amount - 1);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
