package me.koyere.ecoxpert.modules.integrations;

import me.koyere.ecoxpert.EcoXpertPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class IntegrationsManagerImpl implements IntegrationsManager {

    private final EcoXpertPlugin plugin;

    public IntegrationsManagerImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @Override
    public boolean hasLands() {
        return Bukkit.getPluginManager().getPlugin("Lands") != null;
    }

    @Override
    public String getWorldGuardRegions(Player player) {
        if (!hasWorldGuard()) return "";
        try {
            Location loc = player.getLocation();
            // WorldGuard 7+ API via reflection
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = wgClass.getMethod("getInstance");
            Object wg = getInstance.invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wg);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            // BukkitAdapter.adapt
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object weWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class).invoke(null, loc.getWorld());
            Object weLoc = bukkitAdapter.getMethod("adapt", org.bukkit.Location.class).invoke(null, loc);

            // RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);
            Object applicable = query.getClass().getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.util.Location")).invoke(query, weLoc);

            // Iterate regions: for (ProtectedRegion r : applicable)
            Iterable<?> it = (Iterable<?>) applicable;
            List<String> ids = new ArrayList<>();
            for (Object region : it) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (id != null && !id.isEmpty()) ids.add(id);
            }
            return String.join(",", ids);
        } catch (Throwable ignored) {
            return "";
        }
    }

    @Override
    public String getLandsLand(Player player) {
        if (!hasLands()) return "";
        try {
            Location loc = player.getLocation();
            Class<?> landsApiClass = Class.forName("me.angeschossen.lands.api.LandsAPI");
            Object api = landsApiClass.getMethod("getInstance").invoke(null);
            Object land = landsApiClass.getMethod("getLandByLocation", org.bukkit.Location.class).invoke(api, loc);
            if (land == null) return "";
            String name = (String) land.getClass().getMethod("getName").invoke(land);
            return name != null ? name : "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}

