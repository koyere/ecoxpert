package me.koyere.ecoxpert.modules.integrations.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.events.message.MessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles Discord commands for EcoXpert
 * 
 * Registers and processes commands like !balance, !market, !top, !inflation,
 * !stats
 */
public class DiscordCommandHandler extends ListenerAdapter {

    private final EcoXpertPlugin plugin;
    private final DiscordSRVIntegration integration;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final InflationManager inflationManager;

    private boolean enabled;
    private String commandPrefix;
    private List<String> allowedRoles;

    public DiscordCommandHandler(EcoXpertPlugin plugin, DiscordSRVIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
        this.economyManager = plugin.getServiceRegistry().getInstance(EconomyManager.class);
        this.marketManager = plugin.getServiceRegistry().getInstance(MarketManager.class);
        this.inflationManager = plugin.getServiceRegistry().getInstance(InflationManager.class);

        loadConfiguration();

        if (enabled && integration.isAvailable()) {
            registerCommands();
        }
    }

    /**
     * Load configuration
     */
    private void loadConfiguration() {
        try {
            ConfigManager configManager = plugin.getServiceRegistry().getInstance(ConfigManager.class);
            var config = configManager.getModuleConfig("discord");

            ConfigurationSection commands = config.getConfigurationSection("discord.commands");
            if (commands != null) {
                this.enabled = commands.getBoolean("enabled", true);
                this.commandPrefix = commands.getString("prefix", "!");
                this.allowedRoles = commands.getStringList("allowed_roles");
            } else {
                this.enabled = true;
                this.commandPrefix = "!";
                this.allowedRoles = List.of("Admin", "Moderator", "VIP");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load Discord commands configuration", e);
            this.enabled = false;
        }
    }

    /**
     * Register command listener with DiscordSRV
     */
    private void registerCommands() {
        try {
            DiscordSRV.api.subscribe(this);
            plugin.getLogger().info("Discord commands registered successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register Discord commands", e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        if (!enabled)
            return;

        String message = event.getMessage().getContentRaw();
        if (!message.startsWith(commandPrefix))
            return;

        // Check permissions
        if (!hasPermission(event)) {
            event.getChannel().sendMessage("❌ You don't have permission to use economy commands.").queue();
            return;
        }

        String[] args = message.substring(commandPrefix.length()).split("\\s+");
        String command = args[0].toLowerCase();

        switch (command) {
            case "balance" -> handleBalance(event, args);
            case "market" -> handleMarket(event, args);
            case "top" -> handleTop(event, args);
            case "inflation" -> handleInflation(event);
            case "stats" -> handleStats(event);
            default -> {
                // Ignore unknown commands
            }
        }
    }

    /**
     * Check if user has permission
     */
    private boolean hasPermission(MessageReceivedEvent event) {
        if (allowedRoles.isEmpty())
            return true;

        var member = event.getMember();
        if (member == null)
            return false;

        return member.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.getName()));
    }

    /**
     * Handle !balance command
     */
    private void handleBalance(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Usage: `" + commandPrefix + "balance <player>`").queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                @SuppressWarnings("deprecation")
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

                if (player == null || !player.hasPlayedBefore()) {
                    event.getChannel().sendMessage("❌ Player not found: " + playerName).queue();
                    return;
                }

                BigDecimal balance = economyManager.getBalance(player.getUniqueId()).join();
                String formatted = economyManager.formatMoney(balance);

                event.getChannel().sendMessage(
                        String.format("💰 **%s's Balance:** %s", player.getName(), formatted)).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error retrieving balance").queue();
                plugin.getLogger().log(Level.WARNING, "Error in Discord balance command", e);
            }
        });
    }

    /**
     * Handle !market command
     */
    private void handleMarket(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Usage: `" + commandPrefix + "market <item>`").queue();
            return;
        }

        String itemName = args[1].toUpperCase();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                org.bukkit.Material material = org.bukkit.Material.getMaterial(itemName);
                if (material == null) {
                    event.getChannel().sendMessage("❌ Invalid item: " + itemName).queue();
                    return;
                }

                var itemOpt = marketManager.getItem(material).join();
                if (itemOpt.isEmpty()) {
                    event.getChannel().sendMessage("❌ Item not available in market: " + itemName).queue();
                    return;
                }

                var item = itemOpt.get();
                String buyPrice = economyManager.formatMoney(item.getCurrentBuyPrice());
                String sellPrice = economyManager.formatMoney(item.getCurrentSellPrice());

                event.getChannel().sendMessage(
                        String.format("📊 **%s Market Prices**\n💵 Buy: %s\n💰 Sell: %s",
                                material.name(), buyPrice, sellPrice))
                        .queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error retrieving market data").queue();
                plugin.getLogger().log(Level.WARNING, "Error in Discord market command", e);
            }
        });
    }

    /**
     * Handle !top command
     */
    private void handleTop(MessageReceivedEvent event, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(25, limit)); // Limit between 1-25
            } catch (NumberFormatException ignored) {
            }
        }

        final int finalLimit = limit;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                var topBalances = economyManager.getTopBalances(finalLimit).join();

                if (topBalances.isEmpty()) {
                    event.getChannel().sendMessage("❌ No data available").queue();
                    return;
                }

                StringBuilder sb = new StringBuilder("🏆 **Top " + finalLimit + " Richest Players**\n\n");
                int rank = 1;
                for (var entry : topBalances) {
                    UUID uuid = entry.playerUuid();
                    BigDecimal balance = entry.balance();

                    @SuppressWarnings("deprecation")
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    String name = player.getName() != null ? player.getName() : "Unknown";
                    String formatted = economyManager.formatMoney(balance);

                    String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.format("%d.", rank);
                    sb.append(String.format("%s **%s** - %s\n", medal, name, formatted));
                    rank++;
                }

                event.getChannel().sendMessage(sb.toString()).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error retrieving top balances").queue();
                plugin.getLogger().log(Level.WARNING, "Error in Discord top command", e);
            }
        });
    }

    /**
     * Handle !inflation command
     */
    private void handleInflation(MessageReceivedEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                double inflationRate = inflationManager.getInflationRate();
                String status = getInflationStatus(inflationRate);
                String emoji = getInflationEmoji(inflationRate);

                event.getChannel().sendMessage(
                        String.format("%s **Current Inflation Rate:** %.2f%%\n📊 Status: %s",
                                emoji, inflationRate, status))
                        .queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error retrieving inflation data").queue();
                plugin.getLogger().log(Level.WARNING, "Error in Discord inflation command", e);
            }
        });
    }

    /**
     * Handle !stats command
     */
    private void handleStats(MessageReceivedEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                var stats = marketManager.getMarketStatistics().join();
                double inflationRate = inflationManager.getInflationRate();

                String totalItems = String.valueOf(stats.getTotalItems());
                String activeItems = String.valueOf(stats.getActiveItems());
                String totalTransactions = String.valueOf(stats.getTotalTransactions());
                String totalVolume = economyManager.formatMoney(stats.getTotalVolume());
                String avgPrice = economyManager.formatMoney(stats.getAveragePrice());

                event.getChannel().sendMessage(
                        String.format("📊 **Economy Statistics**\n\n" +
                                "🛒 Market Items: %s (%s active)\n" +
                                "💱 Total Transactions: %s\n" +
                                "💰 Total Volume: %s\n" +
                                "📈 Average Price: %s\n" +
                                "📉 Inflation Rate: %.2f%%",
                                totalItems, activeItems, totalTransactions, totalVolume, avgPrice, inflationRate))
                        .queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error retrieving statistics").queue();
                plugin.getLogger().log(Level.WARNING, "Error in Discord stats command", e);
            }
        });
    }

    /**
     * Get trend emoji
     */
    private String getTrendEmoji(me.koyere.ecoxpert.modules.market.MarketTrend.TrendDirection trend) {
        return switch (trend) {
            case STRONG_UPWARD -> "📈↑↑";
            case UPWARD -> "📈↑";
            case STABLE -> "→";
            case DOWNWARD -> "📉↓";
            case STRONG_DOWNWARD -> "📉↓↓";
            case VOLATILE -> "📊⚡";
        };
    }

    /**
     * Get inflation status
     */
    private String getInflationStatus(double rate) {
        if (rate < 0)
            return "Deflation";
        if (rate < 2)
            return "Healthy";
        if (rate < 5)
            return "Moderate";
        if (rate < 10)
            return "High";
        return "Critical";
    }

    /**
     * Get inflation emoji
     */
    private String getInflationEmoji(double rate) {
        if (rate < 0)
            return "📉";
        if (rate < 2)
            return "✅";
        if (rate < 5)
            return "⚠️";
        return "🚨";
    }

    /**
     * Unregister commands
     */
    public void unregister() {
        try {
            DiscordSRV.api.unsubscribe(this);
        } catch (Exception ignored) {
        }
    }
}
