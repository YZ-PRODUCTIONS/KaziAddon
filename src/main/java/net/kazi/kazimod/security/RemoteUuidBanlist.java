package net.kazi.kazimod.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kazi.kazimod.KaziMod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Downloads and caches the UUID banlist maintained in the Kaziaddon GitHub repository.
 * Network failures retain the last valid cached or bundled list.
 */
public final class RemoteUuidBanlist {

    public static final String REMOTE_URL =
            "https://raw.githubusercontent.com/YZ-PRODUCTIONS/banlist/refs/heads/main/blacklist.json";
    public static final long REFRESH_INTERVAL_MINUTES = 30L;

    private static final String BUNDLED_RESOURCE = "/kazimod-banlist.json";
    private static final String CACHE_FILE_NAME = "kazimod-banlist-cache.json";
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 5000;
    private static final int MAX_RESPONSE_CHARACTERS = 1024 * 1024;

    private static final AtomicReference<Map<UUID, BanEntry>> ENTRIES =
            new AtomicReference<>(Collections.emptyMap());
    private static ScheduledExecutorService refreshExecutor;

    private RemoteUuidBanlist() {
    }

    public static synchronized void start() {
        if (refreshExecutor != null) {
            return;
        }

        Map<UUID, BanEntry> bundled = loadBundled();
        if (bundled != null) {
            ENTRIES.set(bundled);
        }

        Map<UUID, BanEntry> cached = loadCache();
        if (cached != null) {
            ENTRIES.set(cached);
        }

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "KaziMod Remote Banlist");
            thread.setDaemon(true);
            return thread;
        };
        refreshExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        refreshExecutor.scheduleWithFixedDelay(
                RemoteUuidBanlist::refreshFromGitHub,
                0L,
                REFRESH_INTERVAL_MINUTES,
                TimeUnit.MINUTES);

        KaziMod.LOGGER.info("Loaded {} UUID entries from the local KaziMod banlist", ENTRIES.get().size());
    }

    public static synchronized void stop() {
        if (refreshExecutor != null) {
            refreshExecutor.shutdownNow();
            refreshExecutor = null;
        }
    }

    @Nullable
    public static BanEntry find(UUID uuid) {
        return uuid == null ? null : ENTRIES.get().get(uuid);
    }

    private static void refreshFromGitHub() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(REMOTE_URL).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "KaziAdditions-Banlist/3.3");
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("GitHub returned HTTP " + responseCode);
            }

            String json;
            try (InputStream stream = connection.getInputStream()) {
                json = readUtf8(stream);
            }

            Map<UUID, BanEntry> downloaded = parse(json, "GitHub");
            ENTRIES.set(downloaded);
            saveCache(json);
            KaziMod.LOGGER.info("Refreshed KaziMod UUID banlist from GitHub: {} entries", downloaded.size());
        } catch (Exception exception) {
            KaziMod.LOGGER.warn(
                    "Could not refresh KaziMod UUID banlist; retaining {} cached entries: {}",
                    ENTRIES.get().size(), exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private static Map<UUID, BanEntry> loadBundled() {
        try (InputStream stream = RemoteUuidBanlist.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                KaziMod.LOGGER.error("Bundled KaziMod UUID banlist is missing");
                return null;
            }
            return parse(readUtf8(stream), "bundled resource");
        } catch (Exception exception) {
            KaziMod.LOGGER.error("Could not load bundled KaziMod UUID banlist", exception);
            return null;
        }
    }

    @Nullable
    private static Map<UUID, BanEntry> loadCache() {
        Path cache = getCachePath();
        if (!Files.isRegularFile(cache)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(cache);
            if (bytes.length > MAX_RESPONSE_CHARACTERS) {
                throw new IOException("cache exceeds size limit");
            }
            return parse(new String(bytes, StandardCharsets.UTF_8), "cache");
        } catch (Exception exception) {
            KaziMod.LOGGER.warn("Ignoring invalid KaziMod UUID banlist cache: {}", exception.getMessage());
            return null;
        }
    }

    private static Map<UUID, BanEntry> parse(String json, String source) throws IOException {
        JsonElement root;
        try {
            root = new JsonParser().parse(json);
        } catch (RuntimeException exception) {
            throw new IOException(source + " banlist is not valid JSON", exception);
        }

        if (!root.isJsonObject()) {
            throw new IOException(source + " banlist root must be an object");
        }

        JsonElement blacklistElement = root.getAsJsonObject().get("blacklist");
        if (blacklistElement == null || !blacklistElement.isJsonArray()) {
            throw new IOException(source + " banlist must contain a blacklist array");
        }

        JsonArray blacklist = blacklistElement.getAsJsonArray();
        Map<UUID, BanEntry> parsed = new LinkedHashMap<>();
        for (JsonElement element : blacklist) {
            if (!element.isJsonObject()) {
                KaziMod.LOGGER.warn("Skipping non-object entry in {} UUID banlist", source);
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            JsonElement uuidElement = object.get("uuid");
            if (uuidElement == null || !uuidElement.isJsonPrimitive()) {
                KaziMod.LOGGER.warn("Skipping UUID banlist entry without a UUID in {}", source);
                continue;
            }

            try {
                UUID uuid = UUID.fromString(uuidElement.getAsString().trim());
                String reason = readReason(object);
                parsed.put(uuid, new BanEntry(uuid, reason));
            } catch (IllegalArgumentException exception) {
                KaziMod.LOGGER.warn("Skipping invalid UUID '{}' in {} banlist", uuidElement, source);
            }
        }

        return Collections.unmodifiableMap(parsed);
    }

    private static String readReason(JsonObject object) {
        JsonElement reasonElement = object.get("reason");
        String reason = reasonElement != null && reasonElement.isJsonPrimitive()
                ? reasonElement.getAsString().trim()
                : "Access revoked";
        reason = reason.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        if (reason.isEmpty()) {
            reason = "Access revoked";
        }
        return reason.length() > 256 ? reason.substring(0, 256) : reason;
    }

    private static String readUtf8(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                if (result.length() + count > MAX_RESPONSE_CHARACTERS) {
                    throw new IOException("banlist exceeds size limit");
                }
                result.append(buffer, 0, count);
            }
        }
        return result.toString();
    }

    private static void saveCache(String json) throws IOException {
        Path cache = getCachePath();
        Files.createDirectories(cache.getParent());
        Path temporary = cache.resolveSibling(cache.getFileName() + ".tmp");
        Files.write(
                temporary,
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        try {
            Files.move(temporary, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path getCachePath() {
        return FMLPaths.CONFIGDIR.get().resolve(CACHE_FILE_NAME);
    }

    public static final class BanEntry {
        private final UUID uuid;
        private final String reason;

        private BanEntry(UUID uuid, String reason) {
            this.uuid = uuid;
            this.reason = reason;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public String getReason() {
            return this.reason;
        }
    }
}
