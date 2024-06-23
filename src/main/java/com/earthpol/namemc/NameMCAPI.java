package com.earthpol.namemc;

import com.earthpol.namemc.api.UUIDFetcherAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NameMCAPI extends JavaPlugin implements UUIDFetcherAPI {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<UUID> uuidList = new CopyOnWriteArrayList<>();
    private String fetchUrl;
    private long lastFetchTime = 0;
    private int fetchIntervalSeconds;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        fetchIntervalSeconds = config.getInt("fetch-interval", 60); // Fetch interval in seconds
        fetchUrl = config.getString("uuid-fetch-url", "");

        // Start fetching UUIDs asynchronously
        scheduler.scheduleAtFixedRate(this::fetchUUIDs, 0, fetchIntervalSeconds, TimeUnit.SECONDS);

        // Start the monitoring task
        scheduler.scheduleAtFixedRate(this::monitorFetch, fetchIntervalSeconds, fetchIntervalSeconds, TimeUnit.SECONDS);

        Bukkit.getServicesManager().register(UUIDFetcherAPI.class, this, this, org.bukkit.plugin.ServicePriority.Normal);
    }

    private void fetchUUIDs() {
        List<UUID> fetchedUuids = new ArrayList<>();
        try {
            URL url = new URL(fetchUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (InputStreamReader reader = new InputStreamReader(con.getInputStream())) {
                // Use Gson to parse the JSON array into a List<UUID>
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> uuidStrings = new Gson().fromJson(reader, listType);
                for (String uuidString : uuidStrings) {
                    fetchedUuids.add(UUID.fromString(uuidString));
                }
                // Only clear and update the uuidList if fetching was successful
                uuidList.clear();
                uuidList.addAll(fetchedUuids);
                lastFetchTime = System.currentTimeMillis(); // Update last fetch time
            }
        } catch (Exception e) {
            // Log the error but do not clear the uuidList
            e.printStackTrace();
            // Optionally, log a more specific message indicating the fetch was skipped due to an error
            getLogger().warning("Failed to fetch UUIDs. Will attempt again on the next scheduled trigger.");
        }
    }

    private void monitorFetch() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime > fetchIntervalSeconds * 2000) { // Allowing some buffer time
            getLogger().warning("Fetch task may not be running as expected. Last fetch was more than twice the interval ago.");
        }
    }

    @Override
    public List<UUID> getFetchedUUIDs() {
        return new ArrayList<>(uuidList); // Return a copy to avoid modification
    }

    @Override
    public void onDisable() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}