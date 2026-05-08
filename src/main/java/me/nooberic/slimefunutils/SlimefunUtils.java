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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public class SlimefunUtils extends JavaPlugin implements SlimefunAddon {

    private static final Gson GSON = new Gson();

    @Override
    public void onEnable() {
        // 从 config.yml 中读取插件配置
        Config cfg = new Config(this);

        boolean autoUpdate = cfg.getBoolean("options.auto-update");
        boolean checkUpdate = cfg.getBoolean("options.check-update");

        if (autoUpdate || checkUpdate) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> checkForUpdates(cfg, autoUpdate, checkUpdate));
        }

        /*
         * 1. 创建分类
         * 分类的显示物品将使用以下物品
         */
        ItemStack itemGroupItem = new CustomItemStack(Material.DIAMOND, "&4附属分类");

        // 给你的分类提供一个独一无二的ID
        NamespacedKey itemGroupId = new NamespacedKey(this, "addon_category");
        ItemGroup itemGroup = new ItemGroup(itemGroupId, itemGroupItem);

        /*
         * 2. 创建一个 SlimefunItemStack
         * 这个类是 ItemStack 的扩展，拥有多个构造函数
         * 重要：每个物品都得有一个独一无二的ID
         */
        SlimefunItemStack slimefunItem = new SlimefunItemStack("COOL_DIAMOND", Material.DIAMOND, "&4炫酷的钻石", "&c+20% 炫酷");

        /*
         * 3. 创建配方
         * 这个配方是一个拥有9个ItemStack的数组。
         * 它代表了一个3x3的有序合成配方。
         * 该配方所需的机器将在后面通过RecipeType指定。
         */
        ItemStack[] recipe = { new ItemStack(Material.EMERALD), null, new ItemStack(Material.EMERALD), null, new ItemStack(Material.DIAMOND), null, new ItemStack(Material.EMERALD), null, new ItemStack(Material.EMERALD) };

        /*
         * 4. 注册物品
         * 现在，你只需要注册物品
         * RecipeType.ENHANCED_CRAFTING_TABLE 代表
         * 该物品将在增强型工作台中合成。
         * 来自粘液科技本体的配方类型将会自动将配方添加到对应的机器中。
         */
        SlimefunItem item = new SlimefunItem(itemGroup, slimefunItem, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
        item.register(this);
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

    private record ReleaseInfo(String version, String downloadUrl, String fileName) {
    }

    private record PluginJarMetadata(String name, String version) {
    }

}
