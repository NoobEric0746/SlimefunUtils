package me.nooberic.slimefunutils.items.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;

public class VoodooDollItem extends SlimefunItem implements Listener {

    private static final String DISPLAY_NAME = "&6巫毒玩偶";
    private static final String UNBOUND_STATUS = "&8状态: &7未绑定";
    private static final String BOUND_STATUS = "&8状态: &c已绑定";
    private static final String BOUND_NAME_PREFIX = "&8目标: &f";
    private static final String BIND_HINT = "&7右键绑定生物";
    private static final String USE_HINT = "&7下蹲右键对绑定生物造成伤害";
    private static final String COST_HINT = "&8可能有一点小小的代价";
    private static final String CURSE_DEATH_METADATA = "voodoo_doll_curse_death";
    private static final String BACKLASH_DEATH_METADATA = "voodoo_doll_backlash_death";

    private NamespacedKey boundUuidKey;
    private NamespacedKey boundNameKey;

    public VoodooDollItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyDisplay(meta, null);
            item.setItemMeta(meta);
        }
    }

    @Override
    public void postRegister() {
        JavaPlugin plugin = getAddon().getJavaPlugin();
        this.boundUuidKey = new NamespacedKey(plugin, "voodoo_doll_bound_uuid");
        this.boundNameKey = new NamespacedKey(plugin, "voodoo_doll_bound_name");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!isItem(item)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!canUse(player, true)) {
            return;
        }

        if (player.isSneaking()) {
            triggerBoundDamage(player, item);
            return;
        }

        if (!(event.getRightClicked() instanceof LivingEntity livingEntity)) {
            return;
        }

        bindTarget(item, livingEntity);
        sendActionBar(player, "&6已绑定目标: &e" + getEntityDisplayName(livingEntity));
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isItem(item)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!canUse(player, true)) {
            return;
        }

        if (player.isSneaking()) {
            triggerBoundDamage(player, item);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.hasMetadata(CURSE_DEATH_METADATA)) {
            event.setDeathMessage(player.getName() + "受诅咒而死");
            player.removeMetadata(CURSE_DEATH_METADATA, getAddon().getJavaPlugin());
        }

        if (player.hasMetadata(BACKLASH_DEATH_METADATA)) {
            event.setDeathMessage(player.getName() + "受诅咒反噬而死");
            player.removeMetadata(BACKLASH_DEATH_METADATA, getAddon().getJavaPlugin());
        }
    }

    private void bindTarget(ItemStack item, LivingEntity target) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(boundUuidKey, PersistentDataType.STRING, target.getUniqueId().toString());
        container.set(boundNameKey, PersistentDataType.STRING, getEntityDisplayName(target));
        applyDisplay(meta, getEntityDisplayName(target));
        item.setItemMeta(meta);
    }

    private void triggerBoundDamage(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String uuidString = container.get(boundUuidKey, PersistentDataType.STRING);

        if (uuidString == null || uuidString.isEmpty()) {
            sendActionBar(player, "&c该巫毒玩偶尚未绑定目标");
            return;
        }

        Entity entity;
        try {
            entity = Bukkit.getEntity(UUID.fromString(uuidString));
        } catch (IllegalArgumentException ex) {
            clearBinding(item, meta, container);
            sendActionBar(player, "&c巫毒玩偶的绑定已失效");
            return;
        }

        if (!(entity instanceof LivingEntity target) || !entity.isValid() || entity.isDead()) {
            clearBinding(item, meta, container);
            sendActionBar(player, "&c未找到绑定目标，已解除绑定");
            return;
        }

        String displayName = getEntityDisplayName(target);
        container.set(boundNameKey, PersistentDataType.STRING, displayName);
        applyDisplay(meta, displayName);
        item.setItemMeta(meta);

        applyMagicDamage(player, 19.0D, EntityDamageEvent.DamageCause.VOID, BACKLASH_DEATH_METADATA);
        applyMagicDamage(target, 10.0D, EntityDamageEvent.DamageCause.VOID, CURSE_DEATH_METADATA);
        spawnInkParticles(player);
        spawnInkParticles(target);
        sendActionBar(player, "&7你消耗生命诅咒了 &f" + displayName);
    }

    private void applyMagicDamage(LivingEntity entity, double amount, EntityDamageEvent.DamageCause cause, String deathMetadataKey) {
        if (entity.isDead()) {
            return;
        }

        boolean marked = false;
        if (entity instanceof Player player && player.getHealth() <= amount) {
            player.setMetadata(deathMetadataKey, new FixedMetadataValue(getAddon().getJavaPlugin(), true));
            marked = true;
        }

        entity.damage(amount);
        entity.setLastDamageCause(new EntityDamageEvent(entity, cause, amount));

        if (marked && entity instanceof Player player && !player.isDead() && player.getHealth() > 0.0D) {
            player.removeMetadata(deathMetadataKey, getAddon().getJavaPlugin());
        }
    }

    private void applyDisplay(ItemMeta meta, String boundName) {
        meta.setDisplayName(colorize(DISPLAY_NAME));

        List<String> lore = new ArrayList<>();
        if (boundName == null || boundName.isEmpty()) {
            lore.add(colorize(UNBOUND_STATUS));
        } else {
            lore.add(colorize(BOUND_STATUS));
            lore.add(colorize(BOUND_NAME_PREFIX + boundName));
        }

        lore.add(colorize(BIND_HINT));
        lore.add(colorize(USE_HINT));
        lore.add(colorize(COST_HINT));
        meta.setLore(lore);
    }

    private void clearBinding(ItemStack item, ItemMeta meta, PersistentDataContainer container) {
        container.remove(boundUuidKey);
        container.remove(boundNameKey);
        applyDisplay(meta, null);
        item.setItemMeta(meta);
    }

    private String getEntityDisplayName(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getName();
        }

        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return ChatColor.stripColor(entity.getCustomName());
        }

        return entity.getName();
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
    }

    private void spawnInkParticles(LivingEntity entity) {
        if (entity == null || entity.getWorld() == null) {
            return;
        }

        entity.getWorld().spawnParticle(
            Particle.SQUID_INK,
            entity.getLocation().add(0.0D, entity.getHeight() * 0.6D, 0.0D),
            20,
            0.4D,
            0.35D,
            0.4D,
            0.02D
        );
    }
}
