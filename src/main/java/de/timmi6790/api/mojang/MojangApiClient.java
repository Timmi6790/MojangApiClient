package de.timmi6790.api.mojang;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import de.timmi6790.api.mojang.deserializers.NameEntryDeserializer;
import de.timmi6790.api.mojang.deserializers.PlayerInfoDeserializer;
import de.timmi6790.api.mojang.deserializers.PlayerProfileDeserializer;
import de.timmi6790.api.mojang.deserializers.StatusMapDeserializer;
import de.timmi6790.api.mojang.models.NameEntry;
import de.timmi6790.api.mojang.models.PlayerInfo;
import de.timmi6790.api.mojang.models.PlayerProfile;
import de.timmi6790.api.mojang.models.Status;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MojangApiClient {
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";

    private static MojangApiClient instance;

    public static MojangApiClient getInstance() {
        if (instance == null) {
            instance = new MojangApiClient();
        }
        return instance;
    }

    private final Cache<UUID, PlayerProfile> playerProfileCache = Caffeine
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    private final Cache<String, PlayerInfo> playerInfoCache = Caffeine
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    private final OkHttpClient httpClient;

    private final Moshi moshi = new Moshi.Builder()
            .add(NameEntry.class, new NameEntryDeserializer())
            .build();
    private final StatusMapDeserializer statusDeserializer = new StatusMapDeserializer();
    private final PlayerInfoDeserializer playerInfoDeserializer = new PlayerInfoDeserializer();
    private final PlayerProfileDeserializer playerProfileDeserializer = new PlayerProfileDeserializer();
    private final JsonAdapter<List<NameEntry>> nameEntriesDeserializer = this.moshi.adapter(Types.newParameterizedType(List.class, NameEntry.class));

    private MojangApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    final Request originalRequest = chain.request();
                    final Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .build();
    }

    protected Request getGetRequest(final String url) {
        final HttpUrl httpUrl = HttpUrl.parse(url);
        return new Request.Builder()
                .url(httpUrl)
                .build();
    }

    @SneakyThrows
    public Optional<Map<String, Status>> getStatus() {
        final Request request = this.getGetRequest("https://status.mojang.com/check");
        try (final Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            return Optional.ofNullable(this.statusDeserializer.fromJson(response.body().source()));
        }
    }

    public Optional<List<String>> getBlockedServers() {
        final Request request = this.getGetRequest("https://sessionserver.mojang.com/blockedservers");
        try (final Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            final String body = response.body().string();
            final String[] lines = body.split("\n");
            final List<String> formattedLines = new ArrayList<>(lines.length);
            for (final String line : lines) {
                formattedLines.add(line.trim());
            }
            return Optional.of(formattedLines);
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    public Optional<PlayerInfo> getPlayerInfo(final String playerName) {
        return this.getPlayerInfo(playerName, LocalDateTime.now());
    }

    public Optional<PlayerInfo> getPlayerInfo(final String playerName, final LocalDateTime atTime) {
        // Cache check
        // We can do this for now without caring for the timestamp, it is currently broken
        final PlayerInfo cacheEntry = this.playerInfoCache.getIfPresent(playerName);
        if (cacheEntry != null) {
            return Optional.of(cacheEntry);
        }

        final long unixTime = atTime.toEpochSecond(ZoneOffset.UTC);
        final Request request = this.getGetRequest("https://api.mojang.com/users/profiles/minecraft/" + playerName + "?at=" + unixTime);
        try (final Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            final PlayerInfo playerInfo = this.playerInfoDeserializer.fromJson(response.body().source());
            if (playerInfo != null) {
                this.playerInfoCache.put(playerName, playerInfo);
            }
            return Optional.ofNullable(playerInfo);
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    public Optional<List<NameEntry>> getPlayerNameHistory(final UUID playerUUID) {
        final Request request = this.getGetRequest("https://api.mojang.com/user/profiles/" + playerUUID.toString() + "/names");
        try (final Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            return Optional.ofNullable(this.nameEntriesDeserializer.fromJson(response.body().source()));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    public Optional<PlayerProfile> getPlayerProfiler(final UUID playerUUID) {
        // Cache check
        final PlayerProfile cacheEntry = this.playerProfileCache.getIfPresent(playerUUID);
        if (cacheEntry != null) {
            return Optional.of(cacheEntry);
        }

        final Request request = this.getGetRequest("https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID);
        try (final Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            final PlayerProfile playerProfile = this.playerProfileDeserializer.fromJson(response.body().source());
            if (playerProfile != null) {
                this.playerProfileCache.put(playerUUID, playerProfile);
            }
            return Optional.ofNullable(playerProfile);
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getPlayerName(final UUID playerUUID) {
        return this.getPlayerProfiler(playerUUID).map(PlayerProfile::getName);
    }

    public Optional<UUID> getPlayerUUID(final String playerName) {
        return this.getPlayerInfo(playerName).map(PlayerInfo::getUuid);
    }
}
