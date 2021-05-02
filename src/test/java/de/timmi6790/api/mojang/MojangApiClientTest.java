package de.timmi6790.api.mojang;

import de.timmi6790.api.mojang.models.NameEntry;
import de.timmi6790.api.mojang.models.PlayerInfo;
import de.timmi6790.api.mojang.models.PlayerProfile;
import de.timmi6790.api.mojang.models.Status;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class MojangApiClientTest {
    @SneakyThrows
    protected String getContentFromFile(final String path) {
        final ClassLoader classLoader = MojangApiClientTest.class.getClassLoader();

        final URI uri = classLoader.getResource(path).toURI();
        final byte[] encoded = Files.readAllBytes(Paths.get(uri));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected void assertNameEntry(final NameEntry nameEntry,
                                   final String name,
                                   final long changeTimeInSeconds,
                                   final boolean isFirstName) {
        final LocalDateTime expectedTime = changeTimeInSeconds == 0 ? LocalDateTime.MIN : new Timestamp(changeTimeInSeconds).toLocalDateTime();

        assertThat(nameEntry.getName()).isEqualTo(name);
        assertThat(nameEntry.getChangedAt()).isEqualTo(expectedTime);
        assertThat(nameEntry.isOriginalName()).isEqualTo(isFirstName);
    }

    protected Request getMockedGetRequest(final HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .build();
    }

    protected MojangApiClient getMojangApiClient(final HttpUrl url) {
        final MojangApiClient apiClient = spy(MojangApiClient.class);
        doReturn(this.getMockedGetRequest(url)).when(apiClient).getGetRequest(any());

        return apiClient;
    }

    @Test
    @SneakyThrows
    void getStatus() {
        final Map<String, Status> expectedStatus = new HashMap<>();
        expectedStatus.put("minecraft.net", Status.GREEN);
        expectedStatus.put("session.minecraft.net", Status.RED);
        expectedStatus.put("account.mojang.com", Status.YELLOW);
        expectedStatus.put("authserver.mojang.com", Status.GREEN);
        expectedStatus.put("sessionserver.mojang.com", Status.RED);
        expectedStatus.put("api.mojang.com", Status.GREEN);
        expectedStatus.put("textures.minecraft.net", Status.GREEN);
        expectedStatus.put("mojang.com", Status.GREEN);

        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("status.json")));

            final HttpUrl url = server.url("/check");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<Map<String, Status>> foundStatusOpt = apiClient.getStatus();
            assertThat(foundStatusOpt).isPresent();
            assertThat(foundStatusOpt.get()).containsExactlyInAnyOrderEntriesOf(expectedStatus);
        }
    }

    @Test
    @SneakyThrows
    void getStatus_invalid_response_code() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            final HttpUrl url = server.url("/check");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<Map<String, Status>> foundStatusOpt = apiClient.getStatus();
            assertThat(foundStatusOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getBlockedServers() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("blocked_servers")));

            final HttpUrl url = server.url("/blockedservers");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<List<String>> blockedServersOpt = apiClient.getBlockedServers();
            assertThat(blockedServersOpt).isPresent();

            final List<String> blockedServers = blockedServersOpt.get();
            assertThat(blockedServers).hasSize(2271);
            assertThat(blockedServers.get(0)).isEqualTo("c5c03d9bad5c5ad25deb64600b9cd900312d4d74");
            assertThat(blockedServers.get(381)).isEqualTo("5d64ecee1e2494299dba4f5835e9e49254a3f6b4");
        }
    }

    @Test
    @SneakyThrows
    void getBlockedServers_invalid_response_code() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            final HttpUrl url = server.url("/blockedservers");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<List<String>> blockedServersOpt = apiClient.getBlockedServers();
            assertThat(blockedServersOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerInfo() {
        final String playerName = "Timmi6790";

        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_info.json")));

            final HttpUrl url = server.url("/users/profiles/minecraft");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerInfo> playerInfoOpt = apiClient.getPlayerInfo(playerName);

            assertThat(playerInfoOpt).isPresent();

            final PlayerInfo playerInfo = playerInfoOpt.get();
            assertThat(playerInfo.getName()).isEqualTo(playerName);
            assertThat(playerInfo.getUuid()).isEqualTo(UUID.fromString("9d59daad-6f62-4bd9-b13e-c961bf906750"));
        }
    }

    @Test
    @SneakyThrows
    void getPlayerInfo_invalid_response_code() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            final HttpUrl url = server.url("/users/profiles/minecraft");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerInfo> playerInfoOpt = apiClient.getPlayerInfo("Timmi67901");
            assertThat(playerInfoOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerInfo_invalid_response_name() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_info_missing_name.json")));

            final HttpUrl url = server.url("/users/profiles/minecraft");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerInfo> playerInfoOpt = apiClient.getPlayerInfo("MissingName");
            assertThat(playerInfoOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerInfo_invalid_response_uuid() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_info_missing_uuid.json")));

            final HttpUrl url = server.url("/users/profiles/minecraft");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerInfo> playerInfoOpt = apiClient.getPlayerInfo("MissingUUID");
            assertThat(playerInfoOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerNameHistory() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("name_history.json")));

            final HttpUrl url = server.url("/users/profiles/names");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<List<NameEntry>> nameHistoryOpt = apiClient.getPlayerNameHistory(UUID.fromString("05c02f83-c09e-4629-b35e-f17e061df8be"));
            assertThat(nameHistoryOpt).isPresent();

            final List<NameEntry> nameHistory = nameHistoryOpt.get();
            assertThat(nameHistory).hasSize(5);

            this.assertNameEntry(
                    nameHistory.get(0),
                    "HappyCat0406",
                    0L,
                    true
            );
            this.assertNameEntry(
                    nameHistory.get(1),
                    "Vansqn",
                    1584033056000L,
                    false
            );
            this.assertNameEntry(
                    nameHistory.get(2),
                    "King_BP",
                    1587135232000L,
                    false
            );
            this.assertNameEntry(
                    nameHistory.get(3),
                    "Vansqn",
                    1589732489000L,
                    false
            );
            this.assertNameEntry(
                    nameHistory.get(4),
                    "0hVanny",
                    1608983359000L,
                    false
            );
        }
    }

    @Test
    @SneakyThrows
    void getPlayerNameHistory_invalid_response_code() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("name_history_missing_name.json")));

            final HttpUrl url = server.url("/users/profiles/names");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<List<NameEntry>> nameHistoryOpt = apiClient.getPlayerNameHistory(UUID.randomUUID());
            assertThat(nameHistoryOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerNameHistory_invalid_response() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            final HttpUrl url = server.url("/users/profiles/names");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<List<NameEntry>> nameHistoryOpt = apiClient.getPlayerNameHistory(UUID.randomUUID());
            assertThat(nameHistoryOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerProfiler() {
        final UUID playerUUID = UUID.fromString("9d59daad-6f62-4bd9-b13e-c961bf906750");

        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_profile.json")));

            final HttpUrl url = server.url("/session/minecraft/profile/");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerProfile> playerProfilerOpt = apiClient.getPlayerProfiler(playerUUID);

            assertThat(playerProfilerOpt).isPresent();

            final PlayerProfile playerProfile = playerProfilerOpt.get();
            assertThat(playerProfile.getName()).isEqualTo("Timmi6790");
            assertThat(playerProfile.getUuid()).isEqualTo(playerUUID);
        }
    }

    @Test
    @SneakyThrows
    void getPlayerProfiler_invalid_response_code() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500));

            final HttpUrl url = server.url("/session/minecraft/profile/");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerProfile> playerProfilerOpt = apiClient.getPlayerProfiler(UUID.randomUUID());
            assertThat(playerProfilerOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerProfiler_invalid_response_name() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_profile_missing_name.json")));

            final HttpUrl url = server.url("/session/minecraft/profile/");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerProfile> playerProfilerOpt = apiClient.getPlayerProfiler(UUID.randomUUID());
            assertThat(playerProfilerOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerProfiler_invalid_response_uuid() {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_profile_missing_uuid.json")));

            final HttpUrl url = server.url("/session/minecraft/profile/");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<PlayerProfile> playerProfilerOpt = apiClient.getPlayerProfiler(UUID.randomUUID());
            assertThat(playerProfilerOpt).isNotPresent();
        }
    }

    @Test
    @SneakyThrows
    void getPlayerName() {
        final String expectedName = "mwmy";
        final UUID expectedUUID = UUID.fromString("5438ed1a-48ed-4086-a5a7-7912ca2bf1ee");

        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_name.json")));

            final HttpUrl url = server.url("/session/minecraft/profile/");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<String> playerNameOpt = apiClient.getPlayerName(expectedUUID);
            assertThat(playerNameOpt)
                    .isPresent()
                    .contains(expectedName);

            // Cache check
            final Optional<String> playerNameCacheOpt = apiClient.getPlayerName(expectedUUID);
            assertThat(playerNameCacheOpt)
                    .isPresent()
                    .contains(expectedName);
        }
    }

    @Test
    @SneakyThrows
    void getPlayerUUID() {
        final String expectedName = "mwmy";
        final UUID expectedUUID = UUID.fromString("5438ed1a-48ed-4086-a5a7-7912ca2bf1ee");

        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(this.getContentFromFile("player_uuid.json")));

            final HttpUrl url = server.url("/users/profiles/minecraft");
            final MojangApiClient apiClient = this.getMojangApiClient(url);

            final Optional<UUID> playerUUIDOpt = apiClient.getPlayerUUID(expectedName);
            assertThat(playerUUIDOpt)
                    .isPresent()
                    .contains(expectedUUID);

            // Cache check
            final Optional<UUID> playerUUIDCacheOpt = apiClient.getPlayerUUID(expectedName);
            assertThat(playerUUIDCacheOpt)
                    .isPresent()
                    .contains(expectedUUID);
        }
    }

    @Test
    void getInstance() {
        final MojangApiClient apiClient = MojangApiClient.getInstance();
        assertThat(apiClient).isNotNull();
    }
}