package com.earthpol.namemc;

import com.earthpol.namemc.api.UUIDFetcherAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
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

public final class NameMC extends JavaPlugin implements UUIDFetcherAPI {
    private List<UUID> uuidList = new CopyOnWriteArrayList<>();
    private String fetchUrl;
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        int interval = config.getInt("fetch-interval", 60) * 20; // Convert to ticks
        fetchUrl = config.getString("uuid-fetch-url", "");

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimerAsynchronously(this, this::fetchUUIDs, 0L, interval);

        Bukkit.getServicesManager().register(UUIDFetcherAPI.class, this, this, org.bukkit.plugin.ServicePriority.Normal);
    }

    private void fetchUUIDs() {
        try {
            URL url = new URL(fetchUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            List<UUID> fetchedUuids;
            try (InputStreamReader reader = new InputStreamReader(con.getInputStream())) {
                // Use Gson to parse the JSON array into a List<UUID>
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> uuidStrings = new Gson().fromJson(reader, listType);
                fetchedUuids = new ArrayList<>();
                for (String uuidString : uuidStrings) {
                    fetchedUuids.add(UUID.fromString(uuidString));
                }
            }
            // Clear and add all fetched UUIDs to the uuidList
            uuidList.clear();
            uuidList.addAll(fetchedUuids);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<UUID> getFetchedUUIDs() {
        return new ArrayList<>(uuidList); // Return a copy to avoid modification
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
