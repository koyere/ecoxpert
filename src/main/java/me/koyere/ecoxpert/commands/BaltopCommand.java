package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Balance Top (Leaderboard) Command
 *
 * Displays top balances with pagination support.
 * Provides both console and GUI views for player leaderboards.
 *
 * Usage:
 * /baltop [page] - Show leaderboard (page 1 if not specified)
 * /baltop gui [page] - Open GUI view (players only)
 */
public class BaltopCommand extends BaseCommand {

    private static final int ENTRIES_PER_PAGE = 10;

    private final EcoXpertPlugin plugin;

    public BaltopCommand(EcoXpertPlugin plugin, EconomyManager economyManager, TranslationManager translationManager,
            ConfigManager configManager) {
        super(economyManager, translationManager, configManager);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        handleCommandSafely(sender, "baltop", () -> {
            debug("BaltopCommand started for: " + sender.getName());

            if (!hasPermission(sender, "ecoxpert.baltop")) {
                debug("Permission denied for: " + sender.getName());
                return;
            }

            // Check if GUI requested
            if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
                handleGUIRequest(sender, args);
                return;
            }

            // Parse page number
            int page = 1;
            if (args.length > 0) {
                try {
                    page = Integer.parseInt(args[0]);
                    if (page < 1)
                        page = 1;
                } catch (NumberFormatException e) {
                    sendMessage(sender, "baltop.invalid-page", args[0]);
                    return;
                }
            }

            displayLeaderboard(sender, page);
        });

        return true;
    }

    /**
     * Handle GUI request (player only)
     */
    private void handleGUIRequest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "error.player_only");
            return;
        }

        if (!hasPermission(sender, "ecoxpert.baltop.gui")) {
            return;
        }

        Player player = (Player) sender;

        // Parse page for GUI
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1)
                    page = 1;
            } catch (NumberFormatException e) {
                sendMessage(sender, "baltop.invalid-page", args[1]);
                return;
            }
        }

        // Open GUI
        openGUI(player, page);
    }

    /**
     * Display console leaderboard
     */
    private void displayLeaderboard(CommandSender sender, int page) {
        final int finalPage = page;
        final int limit = ENTRIES_PER_PAGE * page + 10; // Fetch extra to know if there's a next page

        economyManager.getTopBalances(limit).thenAccept(entries -> {
            if (entries.isEmpty()) {
                sendMessage(sender, "baltop.no-data");
                return;
            }

            // Calculate pagination
            int totalEntries = entries.size();
            int totalPages = (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE);
            int startIndex = (finalPage - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalEntries);

            // Validate page
            if (startIndex >= totalEntries) {
                sendMessage(sender, "baltop.page-out-of-range", finalPage, totalPages);
                return;
            }

            // Header
            sendMessage(sender, "baltop.header", finalPage, totalPages);

            // Display entries
            for (int i = startIndex; i < endIndex; i++) {
                EconomyManager.TopBalanceEntry entry = entries.get(i);
                int rank = i + 1;
                String playerName = getPlayerName(entry.playerUuid());
                String formattedBalance = economyManager.formatMoney(entry.balance());

                // Check if current player
                boolean isSelf = sender instanceof Player && ((Player) sender).getUniqueId().equals(entry.playerUuid());

                if (isSelf) {
                    sendMessage(sender, "baltop.entry-self", rank, playerName, formattedBalance);
                } else {
                    sendMessage(sender, "baltop.entry", rank, playerName, formattedBalance);
                }
            }

            // Footer with navigation hints
            if (finalPage > 1 || endIndex < totalEntries) {
                StringBuilder footer = new StringBuilder();

                if (finalPage > 1) {
                    footer.append(translationManager.getMessage("baltop.footer-prev", finalPage - 1));
                }

                if (endIndex < totalEntries) {
                    if (footer.length() > 0)
                        footer.append(" §8| ");
                    footer.append(translationManager.getMessage("baltop.footer-next", finalPage + 1));
                }

                sender.sendMessage(footer.toString());
            }

            // Show player's own rank if not in current page
            if (sender instanceof Player) {
                Player player = (Player) sender;
                economyManager.getBalanceRank(player.getUniqueId()).thenAccept(playerRank -> {
                    if (playerRank > 0 && (playerRank < startIndex + 1 || playerRank > endIndex)) {
                        economyManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
                            sendMessage(sender, "baltop.your-rank", playerRank, economyManager.formatMoney(balance));
                        });
                    }
                });
            }

        }).exceptionally(throwable -> {
            logger.log(java.util.logging.Level.SEVERE, "ECOXPERT ERROR - baltop failed", throwable);
            sendMessage(sender, "error.database_error");
            return null;
        });
    }

    /**
     * Open GUI leaderboard
     */
    private void openGUI(Player player, int page) {
        // Get or create GUI instance
        var gui = new me.koyere.ecoxpert.modules.baltop.BaltopGUI(plugin, economyManager, translationManager);
        gui.open(player, page);
    }

    /**
     * Get player name from UUID with fallback
     */
    private String getPlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : "§7Unknown";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest "gui" and page numbers
            if (sender instanceof Player && sender.hasPermission("ecoxpert.baltop.gui")) {
                if ("gui".startsWith(args[0].toLowerCase())) {
                    completions.add("gui");
                }
            }

            // Suggest page numbers 1-5
            for (int i = 1; i <= 5; i++) {
                String pageStr = String.valueOf(i);
                if (pageStr.startsWith(args[0])) {
                    completions.add(pageStr);
                }
            }
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
