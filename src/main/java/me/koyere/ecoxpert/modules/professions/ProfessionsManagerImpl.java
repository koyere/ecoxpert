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
        String sql = "INSERT INTO ecoxpert_professions (player_uuid, role, level, selected_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT(player_uuid) DO UPDATE SET role = excluded.role, selected_at = CURRENT_TIMESTAMP";
        return dataManager.executeUpdate(sql, player.toString(), role.name())
            .thenApply(rows -> rows > 0);
    }

    @Override
    public List<ProfessionRole> getAvailableRoles() {
        return Arrays.asList(ProfessionRole.values());
    }
}

