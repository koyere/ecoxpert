package me.koyere.ecoxpert.modules.integrations.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.time.Instant;
import java.util.logging.Level;

/**
 * Integration with DiscordSRV plugin
 * 
 * Provides Discord notifications for economic events, market updates,
 * inflation alerts, and Discord commands for economy queries.
 */
public class DiscordSRVIntegration {

    private final EcoXpertPlugin plugin;
    private final ConfigManager configManager;
    private boolean enabled;
    private boolean available;

    // Channel IDs from config
    private String economyChannelId;
    private String marketChannelId;
    private String alertsChannelId;

    // Notification settings
    private boolean largeTransactionsEnabled;
    private double largeTransactionThreshold;
    private int largeTransactionCooldown;
    private long lastLargeTransactionNotification;

    private boolean marketChangesEnabled;
    private double marketChangeThreshold;

    private boolean inflationAlertsEnabled;
    private double inflationAlertThreshold;

    private boolean dailyReportEnabled;
    private String dailyReportTime;

    // Embed colors
    private Color colorPositive;
    private Color colorNegative;
    private Color colorWarning;
    private Color colorInfo;
    private String footerText;
    private String thumbnailUrl;

    public DiscordSRVIntegration(EcoXpertPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.lastLargeTransactionNotification = 0;

        loadConfiguration();
        checkAvailability();
        
        // Initialize Discord integration if available
        if (isAvailable()) {
            initializeIntegration();
        }
    }

    /**
     * Load configuration from discord.yml
     */
    private void loadConfiguration() {
        try {
            var config = configManager.getModuleConfig("discord");

            this.enabled = config.getBoolean("discord.enabled", true);

            // Channel IDs
            ConfigurationSection channels = config.getConfigurationSection("discord.channels");
            if (channels != null) {
                this.economyChannelId = channels.getString("economy", "");
                this.marketChannelId = channels.getString("market", "");
                this.alertsChannelId = channels.getString("alerts", "");
            }

            // Notification settings
            ConfigurationSection notifications = config.getConfigurationSection("discord.notifications");
            if (notifications != null) {
                ConfigurationSection largeTx = notifications.getConfigurationSection("large_transactions");
                if (largeTx != null) {
                    this.largeTransactionsEnabled = largeTx.getBoolean("enabled", true);
                    this.largeTransactionThreshold = largeTx.getDouble("threshold", 10000.0);
                    this.largeTransactionCooldown = largeTx.getInt("cooldown", 300);
                }

                ConfigurationSection marketChanges = notifications.getConfigurationSection("market_changes");
                if (marketChanges != null) {
                    this.marketChangesEnabled = marketChanges.getBoolean("enabled", true);
                    this.marketChangeThreshold = marketChanges.getDouble("threshold_percent", 20.0);
                }

                ConfigurationSection inflationAlerts = notifications.getConfigurationSection("inflation_alerts");
                if (inflationAlerts != null) {
                    this.inflationAlertsEnabled = inflationAlerts.getBoolean("enabled", true);
                    this.inflationAlertThreshold = inflationAlerts.getDouble("threshold_percent", 5.0);
                }

                ConfigurationSection dailyReport = notifications.getConfigurationSection("daily_report");
                if (dailyReport != null) {
                    this.dailyReportEnabled = dailyReport.getBoolean("enabled", true);
                    this.dailyReportTime = dailyReport.getString("time", "20:00");
                }
            }

            // Embed colors
            ConfigurationSection embeds = config.getConfigurationSection("discord.embeds");
            if (embeds != null) {
                this.colorPositive = Color.decode(embeds.getString("color_positive", "#00FF00"));
                this.colorNegative = Color.decode(embeds.getString("color_negative", "#FF0000"));
                this.colorWarning = Color.decode(embeds.getString("color_warning", "#FFA500"));
                this.colorInfo = Color.decode(embeds.getString("color_info", "#0099FF"));
                this.footerText = embeds.getString("footer_text", "EcoXpert Economy System");
                this.thumbnailUrl = embeds.getString("thumbnail_url", "");
            } else {
                // Defaults
                this.colorPositive = Color.decode("#00FF00");
                this.colorNegative = Color.decode("#FF0000");
                this.colorWarning = Color.decode("#FFA500");
                this.colorInfo = Color.decode("#0099FF");
                this.footerText = "EcoXpert Economy System";
                this.thumbnailUrl = "";
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load Discord configuration", e);
            this.enabled = false;
        }
    }

    /**
     * Check if DiscordSRV is available
     */
    private void checkAvailability() {
        if (!enabled) {
            this.available = false;
            return;
        }

        try {
            this.available = Bukkit.getPluginManager().getPlugin("DiscordSRV") != null
                    && DiscordSRV.getPlugin() != null
                    && DiscordSRV.getPlugin().isEnabled();

            if (available) {
                plugin.getLogger().info("Discord integration enabled - connected to DiscordSRV");
            } else {
                plugin.getLogger().info("DiscordSRV not found - Discord integration disabled");
            }
        } catch (Throwable e) {
            this.available = false;
            plugin.getLogger().log(Level.WARNING, "Failed to initialize DiscordSRV integration", e);
        }
    }

    /**
     * Check if DiscordSRV integration is available
     */
    public boolean isAvailable() {
        return enabled && available;
    }

    /**
     * Send a large transaction notification
     */
    public void sendLargeTransactionNotification(String playerName, double amount, String type) {
        if (!isAvailable() || !largeTransactionsEnabled)
            return;
        if (amount < largeTransactionThreshold)
            return;

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastLargeTransactionNotification < largeTransactionCooldown * 1000L) {
            return;
        }
        lastLargeTransactionNotification = now;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💰 Large Transaction Detected")
                .setColor(colorPositive)
                .addField("Player", playerName, true)
                .addField("Amount", String.format("$%,.2f", amount), true)
                .addField("Type", type, true)
                .setFooter(footerText, null)
                .setTimestamp(Instant.now());

        if (!thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        sendToChannel(economyChannelId, embed.build());
    }

    /**
     * Send a market change notification
     */
    public void sendMarketChangeNotification(String itemName, double oldPrice, double newPrice, double changePercent) {
        if (!isAvailable() || !marketChangesEnabled)
            return;
        if (Math.abs(changePercent) < marketChangeThreshold)
            return;

        Color color = changePercent > 0 ? colorPositive : colorNegative;
        String emoji = changePercent > 0 ? "📈" : "📉";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " Market Price Change")
                .setColor(color)
                .addField("Item", itemName, true)
                .addField("Old Price", String.format("$%,.2f", oldPrice), true)
                .addField("New Price", String.format("$%,.2f", newPrice), true)
                .addField("Change", String.format("%.2f%%", changePercent), false)
                .setFooter(footerText, null)
                .setTimestamp(Instant.now());

        if (!thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        sendToChannel(marketChannelId, embed.build());
    }

    /**
     * Send an inflation alert
     */
    public void sendInflationAlert(double inflationRate, String severity) {
        if (!isAvailable() || !inflationAlertsEnabled)
            return;
        if (inflationRate < inflationAlertThreshold)
            return;

        Color color = inflationRate > 10.0 ? colorNegative : colorWarning;
        String emoji = inflationRate > 10.0 ? "🚨" : "⚠️";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " Inflation Alert")
                .setColor(color)
                .addField("Current Rate", String.format("%.2f%%", inflationRate), true)
                .addField("Severity", severity, true)
                .addField("Status", "Economic intervention may be triggered", false)
                .setFooter(footerText, null)
                .setTimestamp(Instant.now());

        if (!thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        sendToChannel(alertsChannelId, embed.build());
    }

    /**
     * Send daily economic report
     */
    public void sendDailyReport(int totalTransactions, double totalVolume, double avgBalance,
            double inflationRate, String topPlayer, double topBalance) {
        if (!isAvailable() || !dailyReportEnabled)
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Daily Economic Report")
                .setColor(colorInfo)
                .addField("Total Transactions", String.valueOf(totalTransactions), true)
                .addField("Total Volume", String.format("$%,.2f", totalVolume), true)
                .addField("Average Balance", String.format("$%,.2f", avgBalance), true)
                .addField("Inflation Rate", String.format("%.2f%%", inflationRate), true)
                .addField("Richest Player", topPlayer, true)
                .addField("Top Balance", String.format("$%,.2f", topBalance), true)
                .setFooter(footerText, null)
                .setTimestamp(Instant.now());

        if (!thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        sendToChannel(economyChannelId, embed.build());
    }

    /**
     * Send economic crisis alert
     */
    public void sendCrisisAlert(String crisisType, String description, String recommendation) {
        if (!isAvailable())
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🚨 Economic Crisis Detected")
                .setColor(colorNegative)
                .addField("Crisis Type", crisisType, false)
                .addField("Description", description, false)
                .addField("Recommendation", recommendation, false)
                .setFooter(footerText, null)
                .setTimestamp(Instant.now());

        if (!thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        sendToChannel(alertsChannelId, embed.build());
    }

    /**
     * Send embed to specific Discord channel
     */
    private void sendToChannel(String channelId, MessageEmbed embed) {
        if (channelId == null || channelId.isEmpty())
            return;

        try {
            TextChannel channel = DiscordUtil.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(embed).queue();
            } else {
                plugin.getLogger().warning("Discord channel not found: " + channelId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send Discord message", e);
        }
    }

    /**
     * Reload configuration
     */
    public void reload() {
        loadConfiguration();
        checkAvailability();
        
        if (isAvailable()) {
            initializeIntegration();
        }
    }

    /**
     * Initialize Discord integration components
     */
    private void initializeIntegration() {
        try {
            // Initialize command handler
            new DiscordCommandHandler(plugin, this);
            
            // Initialize event listener
            new DiscordEventListener(plugin, this);
            
            plugin.getLogger().info("Discord integration components initialized successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize Discord integration components", e);
        }
    }
}
