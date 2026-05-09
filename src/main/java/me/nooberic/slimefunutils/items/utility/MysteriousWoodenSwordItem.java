package me.nooberic.slimefunutils.items.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class MysteriousWoodenSwordItem extends SlimefunItem implements Listener {

    public MysteriousWoodenSwordItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void postRegister() {
        JavaPlugin plugin = getAddon().getJavaPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDirectDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isItem(item)) {
            return;
        }

        if (!canUse(player, true)) {
            event.setCancelled(true);
            return;
        }

        DamageCause cause = event.getCause();
        if (cause != DamageCause.ENTITY_ATTACK && cause != DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (player.getAttackCooldown() < 1.0F) {
            event.setCancelled(true);
            return;
        }

        double damage = event.getFinalDamage();
        event.setCancelled(true);

        if (damage <= 0.0D || target.isDead()) {
            return;
        }

        double newHealth = Math.max(0.0D, target.getHealth() - damage);
        target.setLastDamageCause(event);
        target.setHealth(newHealth);
        target.playHurtAnimation(target.getLocation().getYaw());
    }
}
