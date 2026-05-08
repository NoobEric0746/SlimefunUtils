package me.nooberic.slimefunutils.items.scrolls;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

public class FreezeScrollItem extends SimpleSlimefunItem<ItemUseHandler> {

    private static final double EFFECT_RADIUS = 5.0;
    private static final int WATER_FREEZE_RADIUS = 4;
    private static final int EFFECT_DURATION_TICKS = 20 * 10;
    private static final int PLAYER_EFFECT_DURATION_TICKS = 20 * 5;
    private static final int EFFECT_AMPLIFIER = 4;

    private final JavaPlugin plugin;

    public FreezeScrollItem(JavaPlugin plugin, ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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

            applyFreezeEffect(player);
        };
    }

    private void applyFreezeEffect(Player player) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        freezeNearbyWater(player);

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)) {
            if (!(entity instanceof LivingEntity livingEntity) || livingEntity.equals(player)) {
                continue;
            }

            if (livingEntity.getLocation().distanceSquared(player.getLocation()) > EFFECT_RADIUS * EFFECT_RADIUS) {
                continue;
            }

            int duration = livingEntity instanceof Player ? PLAYER_EFFECT_DURATION_TICKS : EFFECT_DURATION_TICKS;
            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, EFFECT_AMPLIFIER));
            player.getWorld().spawnParticle(
                Particle.BLOCK_CRACK,
                livingEntity.getLocation().add(0, livingEntity.getHeight() * 0.5, 0),
                32,
                0.35,
                0.45,
                0.35,
                Material.ICE.createBlockData()
            );
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.8F);
        plugin.getLogger().fine("Applied freeze scroll effect for " + player.getName());
    }

    private void freezeNearbyWater(Player player) {
        Block center = player.getLocation().getBlock();
        int radiusSquared = WATER_FREEZE_RADIUS * WATER_FREEZE_RADIUS;

        for (int x = -WATER_FREEZE_RADIUS; x <= WATER_FREEZE_RADIUS; x++) {
            for (int z = -WATER_FREEZE_RADIUS; z <= WATER_FREEZE_RADIUS; z++) {
                if (x * x + z * z > radiusSquared) {
                    continue;
                }

                for (int y = -WATER_FREEZE_RADIUS; y <= WATER_FREEZE_RADIUS; y++) {
                    Block block = center.getRelative(x, y, z);
                    if (!isSurfaceWaterSource(block)) {
                        continue;
                    }

                    block.setType(Material.ICE);
                }
            }
        }
    }

    private boolean isSurfaceWaterSource(Block block) {
        if (block.getType() != Material.WATER) {
            return false;
        }

        if (!(block.getBlockData() instanceof Levelled levelled) || levelled.getLevel() != 0) {
            return false;
        }

        return block.getRelative(BlockFace.UP).getType().isAir();
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
