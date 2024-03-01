# NameMC Server Like API
This is a simple little plugin that retrieves the UUID list of players that have liked your Minecraft Server on NameMC

This plugin will pull data from *https://api.namemc.com/server/play.earthpol.com/likes* which can be configured in config.yml.

An example of the JSON Output from  looks like this:
```JSON
[
  "1e453074-8de5-4194-8d82-70574aad18cf",
  "355e900b-8806-49d8-9b72-93cd37bc5612",
  "d550441b-5fcf-448c-a756-5fa391b89a46"
]
```
This URL is fetched every 60 seconds, which can be configured in config.yml

## How to use the API
Simple add the plugin.jar as a depedency to your plugin

Implentment the method like so:
```JAVA
import com.earthpol.namemc.api.UUIDFetcherAPI

 public void useUUIDFetcherAPI() {
        RegisteredServiceProvider<UUIDFetcherAPI> provider = Bukkit.getServicesManager().getRegistration(UUIDFetcherAPI.class);
        if (provider != null) {
            UUIDFetcherAPI api = provider.getProvider();
            // Now you can call methods on the API
            List<UUID> uuids = api.getFetchedUUIDs();
            // Do something with the UUIDs...
        }
    }
```

Now you can check against the List as many times as you want without getting rate limited by NameMC

## Disclaimer
This plugin is not associated, sponsored or supported by NameMC, please do not bother them with support or questions about this plugin. This plugin is maintained by EarthPol who has no affiliation to NameMC or any of NameMC services.
