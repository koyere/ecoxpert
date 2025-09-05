package me.koyere.ecoxpert.modules.professions;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfessionsManagerImpl implements ProfessionsManager {

    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;

    public ProfessionsManagerImpl(EcoXpertPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public CompletableFuture<Optional<ProfessionRole>> getRole(UUID player) {
        return dataManager.executeQuery("SELECT role FROM ecoxpert_professions WHERE player_uuid = ?", player.toString())
            .thenApply(result -> mapRole(result));
    }

    private Optional<ProfessionRole> mapRole(QueryResult result) {
        try (result) {
            if (!result.next()) return Optional.empty();
            String r = result.getString("role");
            if (r == null || r.isBlank()) return Optional.empty();
            try { return Optional.of(ProfessionRole.fromString(r)); } catch (Exception e) { return Optional.empty(); }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to map profession role: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> setRole(UUID player, ProfessionRole role) {
        return canChange(player).thenCompose(allowed -> {
            if (!allowed) return CompletableFuture.completedFuture(false);
            String sql = "INSERT INTO ecoxpert_professions (player_uuid, role, level, selected_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
                         "ON CONFLICT(player_uuid) DO UPDATE SET role = excluded.role, selected_at = CURRENT_TIMESTAMP, level = 1";
            return dataManager.executeUpdate(sql, player.toString(), role.name())
                .thenApply(rows -> rows > 0);
        });
    }

    @Override
    public List<ProfessionRole> getAvailableRoles() {
        return Arrays.asList(ProfessionRole.values());
    }

    @Override
    public CompletableFuture<Integer> getLevel(UUID player) {
        return dataManager.executeQuery("SELECT level FROM ecoxpert_professions WHERE player_uuid = ?", player.toString())
            .thenApply(result -> {
                try (result) { if (result.next()) return Math.max(1, result.getInt("level")); } catch (Exception ignored) {}
                return 1;
            });
    }

    @Override
    public CompletableFuture<Boolean> setLevel(UUID player, int level) {
        int lvl = Math.max(1, level);
        return dataManager.executeUpdate(
            "INSERT INTO ecoxpert_professions (player_uuid, role, level, selected_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(player_uuid) DO UPDATE SET level = excluded.level",
            player.toString(), ProfessionRole.SAVER.name(), lvl
        ).thenApply(rows -> rows > 0);
    }

    @Override
    public CompletableFuture<Boolean> canChange(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try (QueryResult qr = dataManager.executeQuery(
                "SELECT selected_at FROM ecoxpert_professions WHERE player_uuid = ?",
                player.toString()).join()) {
                // If never selected, can change
                if (!qr.next()) return true;
                java.sql.Timestamp ts = qr.getTimestamp("selected_at");
                if (ts == null) return true;
                var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class).getModuleConfig("professions");
                int cooldown = cfg.getInt("cooldown_minutes", 1440); // default 24h
                long elapsedMin = java.time.Duration.between(ts.toInstant(), java.time.Instant.now()).toMinutes();
                return elapsedMin >= cooldown;
            } catch (Exception ignored) { return true; }
        });
    }
}
