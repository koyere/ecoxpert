package me.koyere.ecoxpert.modules.loans;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoanNotificationListener implements Listener {
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final TranslationManager translationManager;

    public LoanNotificationListener(EcoXpertPlugin plugin, DataManager dataManager, TranslationManager translationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.translationManager = translationManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (QueryResult qr = dataManager.executeQuery(
                "SELECT COUNT(*) as c FROM ecoxpert_loan_schedules s JOIN ecoxpert_loans l ON l.id = s.loan_id " +
                    "WHERE l.player_uuid = ? AND s.status = 'LATE'",
                p.getUniqueId().toString()).join()) {
                if (qr.next()) {
                    Integer c = qr.getInt("c");
                    int late = c != null ? c : (qr.getLong("c") != null ? qr.getLong("c").intValue() : 0);
                    if (late > 0) {
                        p.sendMessage(translationManager.getMessage("prefix") + translationManager.getPlayerMessage(p, "loans.overdue-summary", late));
                    }
                }
            } catch (Exception ignored) {}
        });
    }
}

