package me.koyere.ecoxpert.modules.integrations;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class IntegrationsManagerImpl implements IntegrationsManager {

    private final EcoXpertPlugin plugin;
    private final boolean enabled;
    private final boolean detectJobs;
    private final boolean detectTowny;
    private final boolean detectLands;
    private final boolean detectSlimefun;
    private final boolean detectMcMMO;

    public IntegrationsManagerImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
        boolean en = true, dj = true, dt = true, dl = true, ds = true, dm = true;
        try {
            ConfigManager cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class);
            var c = cfg.getModuleConfig("integrations");
            en = c.getBoolean("enabled", true);
            var section = c.getConfigurationSection("detect");
            dj = section == null || section.getBoolean("jobs", true);
            dt = section == null || section.getBoolean("towny", true);
            dl = section == null || section.getBoolean("lands", true);
            ds = section == null || section.getBoolean("slimefun", true);
            dm = section == null || section.getBoolean("mcmmo", true);
        } catch (Throwable ignored) {}
        this.enabled = en;
        this.detectJobs = dj;
        this.detectTowny = dt;
        this.detectLands = dl;
        this.detectSlimefun = ds;
        this.detectMcMMO = dm;

        try {
            boolean wg = hasWorldGuard();
            boolean lands = hasLands();
            boolean jobs = hasJobs();
            boolean towny = hasTowny();
            boolean slimefun = hasSlimefun();
            boolean mcmmo = hasMcMMO();

            plugin.getLogger().info(String.format(
                "Integrations detected → WorldGuard=%s, Lands=%s, Jobs=%s, Towny=%s, Slimefun=%s, McMMO=%s",
                wg ? "yes" : "no",
                lands ? "yes" : "no",
                jobs ? "yes" : "no",
                towny ? "yes" : "no",
                slimefun ? "yes" : "no",
                mcmmo ? "yes" : "no"
            ));
        } catch (Throwable ignored) {
            // Avoid any hard failure if plugin manager is not ready
        }
    }

    @Override
    public boolean hasWorldGuard() {
        if (!enabled) return false;
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @Override
    public boolean hasLands() {
        if (!enabled || !detectLands) return false;
        return Bukkit.getPluginManager().getPlugin("Lands") != null;
    }

    @Override
    public boolean hasJobs() {
        if (!enabled || !detectJobs) return false;
        return Bukkit.getPluginManager().getPlugin("Jobs") != null
                || Bukkit.getPluginManager().getPlugin("JobsReborn") != null;
    }

    @Override
    public boolean hasTowny() {
        if (!enabled || !detectTowny) return false;
        return Bukkit.getPluginManager().getPlugin("Towny") != null;
    }

    @Override
    public boolean hasSlimefun() {
        if (!enabled || !detectSlimefun) return false;
        return Bukkit.getPluginManager().getPlugin("Slimefun") != null
                || Bukkit.getPluginManager().getPlugin("Slimefun4") != null;
    }

    @Override
    public boolean hasMcMMO() {
        if (!enabled || !detectMcMMO) return false;
        return Bukkit.getPluginManager().getPlugin("mcMMO") != null
                || Bukkit.getPluginManager().getPlugin("McMMO") != null;
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
