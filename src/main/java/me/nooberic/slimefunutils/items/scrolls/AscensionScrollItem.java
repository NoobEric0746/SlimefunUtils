package me.nooberic.slimefunutils.items.scrolls;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

public class AscensionScrollItem extends SimpleSlimefunItem<ItemUseHandler> {

    private static final int SLOW_DURATION_TICKS = 20;
    private static final int SLOW_AMPLIFIER = 4;
    private static final int ANIMATION_TICKS = 20;
    private static final double HELIX_RADIUS = 0.6;
    private static final double HELIX_ANGLE_STEP = Math.PI / 6;
    private static final double TELEPORT_Y_OFFSET = 0.1;

    private final JavaPlugin plugin;

    public AscensionScrollItem(JavaPlugin plugin, ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        this.plugin = plugin;
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return event -> {
            event.cancel();

            Player player = event.getPlayer();
            Location targetLocation = findSurfaceLocation(player);
            if (targetLocation == null) {
                return;
            }

            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeItem(event.getItem());
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, true, true));
            animateAscension(player, targetLocation);
        };
    }

    private Location findSurfaceLocation(Player player) {
        Location start = player.getLocation().getBlock().getLocation().add(0, 1, 0);
        World world = player.getWorld();
        int maxY = world.getMaxHeight();
        boolean foundSolid = false;
        Block targetBlock = null;

        for (int y = start.getBlockY(); y < maxY; y++) {
            Block block = world.getBlockAt(start.getBlockX(), y, start.getBlockZ());

            if (!foundSolid) {
                if (hasCollision(block)) {
                    foundSolid = true;
                }
                continue;
            }

            if (block.getType().isAir() && y + 1 < maxY && world.getBlockAt(start.getBlockX(), y + 1, start.getBlockZ()).getType().isAir()) {
                targetBlock = block;
                break;
            }
        }

        if (targetBlock == null) {
            return null;
        }

        Location target = targetBlock.getLocation().add(0.5, TELEPORT_Y_OFFSET, 0.5);
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        return target;
    }

    private boolean hasCollision(Block block) {
        return !block.isPassable() && !block.getCollisionShape().getBoundingBoxes().isEmpty();
    }

    private void animateAscension(Player player, Location targetLocation) {
        Location startLocation = player.getLocation().clone();
        World world = player.getWorld();
        double totalHeight = Math.max(0.5, targetLocation.getY() - startLocation.getY());

        new BukkitRunnable() {
            private int tick;
            private double angle;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                double progress = Math.min(1D, (tick + 1D) / ANIMATION_TICKS);
                double currentHeight = totalHeight * progress;
                spawnHelix(world, startLocation, currentHeight, angle);
                angle += HELIX_ANGLE_STEP;
                tick++;

                if (tick >= ANIMATION_TICKS) {
                    player.teleport(targetLocation);
                    world.playSound(targetLocation, Sound.ITEM_BONE_MEAL_USE, 1.0F, 1.1F);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnHelix(World world, Location origin, double height, double angleOffset) {
        double step = 0.25;
        for (double y = 0; y <= height; y += step) {
            double angle = angleOffset + y * 1.8;
            Vector first = helixOffset(angle, y);
            Vector second = helixOffset(angle + Math.PI, y);

            world.spawnParticle(Particle.HAPPY_VILLAGER, origin.clone().add(first), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.HAPPY_VILLAGER, origin.clone().add(second), 1, 0, 0, 0, 0);
        }
    }

    private Vector helixOffset(double angle, double y) {
        return new Vector(
            Math.cos(angle) * HELIX_RADIUS,
            y,
            Math.sin(angle) * HELIX_RADIUS
        );
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
