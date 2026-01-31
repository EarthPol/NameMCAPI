# NameMCAPI (EarthPol)

NameMCAPI is a lightweight Paper/Folia-safe plugin that exposes several of NameMC’s **undocumented public API endpoints** as a shared **Bukkit service**.

It is designed to be used by other plugins in the EarthPol ecosystem (or elsewhere) without scraping HTML or blocking the main server thread.

All requests are asynchronous and optionally cached.

---

## Features

- Async, non-blocking HTTP requests
- Bukkit Services API integration
- Optional TTL-based response caching
- Cache bypass for “fetch latest” calls
- Paper & Folia compatible
- Clean separation between API and implementation

---

## Supported NameMC Endpoints

The following NameMC endpoints are currently supported:

- `GET /profile/{uuid}/friends`
- `GET /profile/{uuid}/friends?with={uuid}`
- `GET /server/{domain}/likes`
- `GET /server/{domain}/likes?profile={uuid}`

---

## Installation

1. Place `NameMCAPI.jar` into your `/plugins` directory
2. Restart the server
3. The API service will register automatically

No commands or permissions are required.

---

## Configuration

```yml
namemc:
  base-url: "https://api.namemc.com"
  http-timeout-ms: 6000
  cache-ttl-ms: 60000
```

### Configuration Options

- `base-url`
  - Defaults to `https://api.namemc.com`

- `http-timeout-ms`
  - HTTP request timeout in milliseconds

- `cache-ttl-ms`
  - Time-to-live for cached responses
  - Set to `0` to disable caching entirely

---

## Using NameMCAPI in Other Plugins

NameMCAPI exposes a Bukkit service called `NameMCAPIService`.

### 1. Obtaining the Service

```java
import com.earthpol.namemc.api.NameMCAPIService;
import org.bukkit.Bukkit;

NameMCAPIService nameMC =
        Bukkit.getServicesManager().load(NameMCAPIService.class);

if (nameMC == null) {
    getLogger().severe("NameMCAPI not found! Is the plugin installed?");
    return;
}
```

This should typically be done once in `onEnable`.

---

### 2. Fetching a Player’s Friends List

```java
UUID uuid = UUID.fromString("d550441b-5fcf-448c-a756-5fa391b89a46");

// false = fetch latest, true = use cache
nameMC.getFriends(uuid, false).thenAccept(friends -> {
    getLogger().info("Friend count: " + friends.size());
});
```

---

### 3. Checking if Two Profiles Are Friends

```java
UUID a = UUID.fromString("403e6cb7-a6ca-440a-8041-7fb1e579b5a5");
UUID b = UUID.fromString("ac62cc72-ce09-4fd4-9018-38b92ef4d619");

nameMC.areFriends(a, b, true).thenAccept(areFriends -> {
    if (areFriends) {
        getLogger().info("They are friends on NameMC");
    }
});
```

---

### 4. Getting Server Like Count

```java
nameMC.getServerLikes("play.earthpol.com", true)
      .thenAccept(likes ->
          getLogger().info("Server likes: " + likes)
      );
```

---

### 5. Checking if a Player Liked a Server

```java
UUID profile = UUID.fromString("3bd5b91a-7532-4acf-bd2a-2a739206865b");

nameMC.hasProfileLikedServer("play.earthpol.com", profile, false)
      .thenAccept(liked -> {
          if (liked) {
              getLogger().info("Player has liked the server");
          }
      });
```

---

## Threading Notes (IMPORTANT)

All callbacks run **off the main thread**.

If you need to interact with Bukkit objects (players, worlds, scoreboards, etc.), you must switch back to the main thread:

```java
nameMC.getFriends(uuid, true).thenAccept(friends -> {
    Bukkit.getScheduler().runTask(this, () -> {
        player.sendMessage("You have " + friends.size() + " NameMC friends");
    });
});
```

---

## Maven Dependency (BitworksMC Nexus)

NameMCAPI is published to the BitworksMC Nexus repository and can be consumed directly via Maven.

### 1. Add the repository

```xml
<repositories>
    <repository>
        <id>bitworksmc-releases</id>
        <url>https://nexus.bitworksmc.com/repository/maven-releases/</url>
    </repository>
</repositories>
```

### 2. Add the dependency

```xml
<dependency>
    <groupId>com.earthpol</groupId>
    <artifactId>NameMCAPI</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```


---

## Design Notes

- NameMCAPI does **not** scrape HTML
- Uses NameMC’s publicly accessible JSON endpoints
- Responses and endpoints are undocumented and may change
- Failures are not cached
- Cache deduplicates in-flight requests to avoid stampedes

---

## License / Disclaimer

This project is **not affiliated with NameMC** or Mojang/Microsoft.

NameMC endpoints are undocumented and may change or disappear at any time.

Use at your own risk.

