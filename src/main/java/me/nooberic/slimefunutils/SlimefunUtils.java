package me.nooberic.slimefunutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.nooberic.slimefunutils.items.scrolls.AscensionScrollItem;
import me.nooberic.slimefunutils.items.scrolls.FireballScrollItem;
import me.nooberic.slimefunutils.items.scrolls.FreezeScrollItem;

public class SlimefunUtils extends JavaPlugin implements SlimefunAddon {

    private static final Gson GSON = new Gson();

    @Override
    public void onEnable() {
        Config cfg = new Config(this);

        boolean autoUpdate = cfg.getBoolean("options.auto-update");
        boolean checkUpdate = cfg.getBoolean("options.check-update");

        if (autoUpdate || checkUpdate) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> checkForUpdates(cfg, autoUpdate, checkUpdate));
        }

        ItemGroup itemGroup = new ItemGroup(
            new NamespacedKey(this, "magic_scrolls"),
            createEnchantedPaperIcon("&d魔法卷轴", "&7收纳各种神秘卷轴")
        );
        itemGroup.register(this);

        SlimefunItemStack blankScroll = new SlimefunItemStack(
            "BLANK_SCROLL",
            Material.PAPER,
            "&f空白卷轴",
            "&7魔法卷轴的基础"
        );

        ItemStack[] blankScrollRecipe = {
            null, createMagicCrystalIi(), null,
            createMagicCrystalIi(), new ItemStack(Material.PAPER), createMagicCrystalIi(),
            null, createMagicCrystalIi(), null
        };

        SlimefunItem blankScrollItem = new SlimefunItem(itemGroup, blankScroll, RecipeType.MAGIC_WORKBENCH, blankScrollRecipe);
        blankScrollItem.register(this);

        Research blankScrollResearch = new Research(
            new NamespacedKey(this, "blank_scroll"),
            9500,
            "便携式法术",
            10
        );
        blankScrollResearch.addItems(blankScrollItem);
        blankScrollResearch.register();

        SlimefunItemStack fireballScroll = new SlimefunItemStack(
            "SCROLL_FIREBALL",
            createEnchantedPaperIcon("&6卷轴:火球术", "&7发射三枚炽热火球")
        );

        ItemStack[] fireballScrollRecipe = {
            null, new ItemStack(Material.FIRE_CHARGE), null,
            new ItemStack(Material.FIRE_CHARGE), blankScroll.clone(), new ItemStack(Material.FIRE_CHARGE),
            null, new ItemStack(Material.FIRE_CHARGE), null
        };

        SlimefunItem fireballScrollItem = new FireballScrollItem(this, itemGroup, fireballScroll, RecipeType.MAGIC_WORKBENCH, fireballScrollRecipe);
        fireballScrollItem.register(this);

        Research fireballScrollResearch = new Research(
            new NamespacedKey(this, "scroll_fireball"),
            9501,
            "发射火球",
            15
        );
        fireballScrollResearch.addItems(fireballScrollItem);
        fireballScrollResearch.register();

        SlimefunItemStack freezeScroll = new SlimefunItemStack(
            "SCROLL_FREEZE",
            createEnchantedPaperIcon("&b卷轴:冰冻术", "&7冰冻周围生物")
        );

        ItemStack[] freezeScrollRecipe = {
            null, new ItemStack(Material.ICE), null,
            new ItemStack(Material.ICE), blankScroll.clone(), new ItemStack(Material.ICE),
            null, new ItemStack(Material.ICE), null
        };

        SlimefunItem freezeScrollItem = new FreezeScrollItem(this, itemGroup, freezeScroll, RecipeType.MAGIC_WORKBENCH, freezeScrollRecipe);
        freezeScrollItem.register(this);

        Research freezeScrollResearch = new Research(
            new NamespacedKey(this, "scroll_freeze"),
            9502,
            "天寒地冻",
            15
        );
        freezeScrollResearch.addItems(freezeScrollItem);
        freezeScrollResearch.register();

        SlimefunItemStack ascensionScroll = new SlimefunItemStack(
            "SCROLL_ASCENSION",
            createEnchantedPaperIcon("&a卷轴:通天术", "&7快速返回地表")
        );

        ItemStack[] ascensionScrollRecipe = {
            null, SlimefunItems.AIR_RUNE.clone(), null,
            new ItemStack(Material.FEATHER), blankScroll.clone(), new ItemStack(Material.FEATHER),
            null, null, null
        };

        SlimefunItem ascensionScrollItem = new AscensionScrollItem(this, itemGroup, ascensionScroll, RecipeType.MAGIC_WORKBENCH, ascensionScrollRecipe);
        ascensionScrollItem.register(this);

        Research ascensionScrollResearch = new Research(
            new NamespacedKey(this, "scroll_ascension"),
            9503,
            "快速返回地表",
            20
        );
        ascensionScrollResearch.addItems(ascensionScrollItem);
        ascensionScrollResearch.register();
    }

    @Override
    public void onDisable() {
        // 禁用插件的逻辑...
    }

    @Override
    public String getBugTrackerURL() {
        // 你可以在这里返回你的问题追踪器的网址，而不是 null
        return null;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        /*
         * 你需要返回对你插件的引用。
         * 如果这是你插件的主类，只需要返回 "this" 即可。
         */
        return this;
    }

    private void checkForUpdates(Config cfg, boolean autoUpdate, boolean checkUpdate) {
        String owner = cfg.getString("updater.owner");
        String repo = cfg.getString("updater.repo");
        String apiUrlTemplate = cfg.getString("updater.api-url");

        if (isBlank(owner) || isBlank(repo) || isBlank(apiUrlTemplate)) {
            getLogger().warning("自动更新已启用，但 updater 配置不完整，已跳过更新检查。");
            return;
        }

        String requestUrl = apiUrlTemplate.replace("%owner%", owner).replace("%repo%", repo);
        int connectTimeoutMs = cfg.getInt("updater.connect-timeout-ms");
        int readTimeoutMs = cfg.getInt("updater.read-timeout-ms");

        try {
            ReleaseInfo latest = fetchLatestRelease(requestUrl, connectTimeoutMs, readTimeoutMs);
            if (latest == null) {
                getLogger().info("未获取到可用的 GitHub Release，跳过自动更新。");
                return;
            }

            String currentVersion = normalizeVersion(getDescription().getVersion());
            String latestVersion = normalizeVersion(latest.version());

            if (compareVersions(latestVersion, currentVersion) <= 0) {
                if (checkUpdate) {
                    getLogger().info("当前已是最新版本: " + getDescription().getVersion());
                }
                return;
            }

            getLogger().info("检测到新版本: " + latest.version() + "，当前版本: " + getDescription().getVersion());

            if (!autoUpdate) {
                getLogger().info("自动下载已关闭，请前往 GitHub Releases 手动更新。");
                return;
            }

            Path updateFolder = getServer().getUpdateFolderFile().toPath();
            Files.createDirectories(updateFolder);

            String fileName = latest.fileName();
            if (isBlank(fileName)) {
                fileName = getName() + "-" + latest.version() + ".jar";
            }

            Path targetFile = updateFolder.resolve(fileName);
            cleanupStaleUpdateFiles(updateFolder, targetFile);
            downloadFile(latest.downloadUrl(), targetFile, connectTimeoutMs, readTimeoutMs);

            PluginJarMetadata metadata = readPluginMetadata(targetFile);
            if (metadata != null && !getName().equals(metadata.name())) {
                getLogger().warning("下载的更新包插件名为 " + metadata.name() + "，与当前插件名 " + getName() + " 不一致，Paper 可能不会应用此更新。");
            }

            String downloadedVersion = metadata != null && !isBlank(metadata.version()) ? metadata.version() : latest.version();
            getLogger().info("已下载更新到: " + targetFile + "，将在下一次完整重启后由 Paper 应用。下载版本: " + downloadedVersion);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "自动更新检查失败: " + e.getMessage(), e);
        }
    }

    private ReleaseInfo fetchLatestRelease(String requestUrl, int connectTimeoutMs, int readTimeoutMs) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
            .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 1000)))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", getName() + "/" + getDescription().getVersion())
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub API 响应异常: HTTP " + response.statusCode());
        }

        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        if (json == null || json.entrySet().isEmpty()) {
            return null;
        }

        String version = getAsString(json, "tag_name");
        JsonArray assets = json.has("assets") && json.get("assets").isJsonArray() ? json.getAsJsonArray("assets") : new JsonArray();

        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject asset = element.getAsJsonObject();
            String contentType = getAsString(asset, "content_type");
            String name = getAsString(asset, "name");
            String downloadUrl = getAsString(asset, "browser_download_url");

            if (isJarAsset(name, contentType, downloadUrl)) {
                return new ReleaseInfo(version, downloadUrl, name);
            }
        }

        return null;
    }

    private void downloadFile(String downloadUrl, Path targetFile, int connectTimeoutMs, int readTimeoutMs) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
            .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 1000)))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", getName() + "/" + getDescription().getVersion())
            .GET()
            .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("下载更新失败: HTTP " + response.statusCode());
        }

        Path tempFile = Files.createTempFile(getName().toLowerCase(Locale.ROOT) + "-update-", ".jar");
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(tempFile)) {
            in.transferTo(out);
        }

        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void cleanupStaleUpdateFiles(Path updateFolder, Path targetFile) throws IOException {
        try (var files = Files.list(updateFolder)) {
            files
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .filter(path -> !path.equals(targetFile))
                .forEach(path -> {
                    try {
                        PluginJarMetadata metadata = readPluginMetadata(path);
                        if (metadata != null && getName().equals(metadata.name())) {
                            Files.deleteIfExists(path);
                            getLogger().info("已移除旧的待更新文件: " + path.getFileName());
                        }
                    } catch (IOException e) {
                        getLogger().log(Level.WARNING, "清理旧更新文件失败: " + path.getFileName(), e);
                    }
                });
        }
    }

    private PluginJarMetadata readPluginMetadata(Path jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            var pluginEntry = jar.getJarEntry("plugin.yml");
            if (pluginEntry == null) {
                return null;
            }

            try (InputStream in = jar.getInputStream(pluginEntry)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                String name = yaml.getString("name");
                String version = yaml.getString("version");
                if (isBlank(name) && isBlank(version)) {
                    return null;
                }
                return new PluginJarMetadata(name, version);
            }
        }
    }

    private boolean isJarAsset(String name, String contentType, String downloadUrl) {
        String lowerName = Objects.toString(name, "").toLowerCase(Locale.ROOT);
        String lowerType = Objects.toString(contentType, "").toLowerCase(Locale.ROOT);
        String lowerUrl = Objects.toString(downloadUrl, "").toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".jar") || lowerType.contains("java-archive") || lowerUrl.endsWith(".jar");
    }

    private String getAsString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }

        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private int compareVersions(String left, String right) {
        List<Integer> leftParts = parseVersionParts(left);
        List<Integer> rightParts = parseVersionParts(right);
        int max = Math.max(leftParts.size(), rightParts.size());

        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.size() ? leftParts.get(i) : 0;
            int rightValue = i < rightParts.size() ? rightParts.get(i) : 0;

            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }

        return 0;
    }

    private List<Integer> parseVersionParts(String version) {
        List<Integer> parts = new ArrayList<>();
        if (isBlank(version)) {
            parts.add(0);
            return parts;
        }

        for (String token : version.split("[.-]")) {
            String digits = token.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                parts.add(0);
            } else {
                try {
                    parts.add(Integer.parseInt(digits));
                } catch (NumberFormatException ignored) {
                    parts.add(0);
                }
            }
        }

        return parts;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ItemStack createEnchantedPaperIcon(String name, String... lore) {
        ItemStack icon = new CustomItemStack(Material.PAPER, name, lore);
        ItemMeta meta = icon.getItemMeta();

        if (meta != null) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    private ItemStack createMagicCrystalIi() {
        return SlimefunItems.MAGIC_LUMP_2.clone();
    }

    private record ReleaseInfo(String version, String downloadUrl, String fileName) {
    }

    private record PluginJarMetadata(String name, String version) {
    }

}
