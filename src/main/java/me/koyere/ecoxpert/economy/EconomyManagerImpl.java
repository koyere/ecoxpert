package me.koyere.ecoxpert.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import org.bukkit.configuration.file.FileConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Implementation of core economy management
 * 
 * Provides enterprise-grade economy operations with database
 * persistence, transaction logging, and comprehensive validation.
 */
@Singleton
public class EconomyManagerImpl implements EconomyManager {
    
    private final EcoXpertPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    
    // Economy configuration
    private BigDecimal startingBalance;
    private BigDecimal maximumBalance;
    private String currencyNameSingular;
    private String currencyNamePlural;
    private String currencySymbol;
    private int decimalPlaces;
    private DecimalFormat moneyFormat;
    
    @Inject
    public EconomyManagerImpl(EcoXpertPlugin plugin, ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }
    
    private void debug(String message) {
        if (configManager != null && configManager.isDebugEnabled()) {
            plugin.getLogger().info("ECOXPERT DEBUG - " + message);
        }
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing economy system...");
        
        loadConfiguration();
        setupMoneyFormat();
        
        plugin.getLogger().info("Economy system initialized successfully");
    }
    
    @Override
    public void shutdown() {
        plugin.getLogger().info("Economy system shutdown complete");
    }
    
    @Override
    public CompletableFuture<Boolean> hasAccount(UUID playerUuid) {
        debug("hasAccount called for: " + playerUuid);
        return dataManager.executeQuery(
            "SELECT player_uuid FROM ecoxpert_accounts WHERE player_uuid = ? LIMIT 1",
            playerUuid.toString()
        ).thenApply(result -> {
            try (result) {
                boolean exists = result.next();
                debug("hasAccount result: " + exists + " for: " + playerUuid);
                return exists;
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "ECOXPERT ERROR - hasAccount failed for: " + playerUuid, throwable);
            return false;
        });
    }
    
    @Override
    public CompletableFuture<Void> createAccount(UUID playerUuid, BigDecimal startingBalance) {
        debug("createAccount called for: " + playerUuid + " with balance: " + startingBalance);
        return dataManager.executeUpdate(
            "INSERT OR IGNORE INTO ecoxpert_accounts (player_uuid, balance) VALUES (?, ?)",
            playerUuid.toString(), startingBalance
        ).thenCompose(rows -> {
            debug("createAccount affected " + rows + " rows for: " + playerUuid);
            if (rows > 0) {
                return logTransaction(null, playerUuid, startingBalance, "ACCOUNT_CREATION", 
                                    "Initial account creation");
            }
            return CompletableFuture.completedFuture(null);
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "ECOXPERT ERROR - createAccount failed for: " + playerUuid, throwable);
            return (Void) null;
        });
    }
    
    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID playerUuid) {
        debug("getBalance called for: " + playerUuid);
        return ensureAccountExists(playerUuid).thenCompose(v -> {
            debug("ensureAccountExists completed, executing balance query");
            return dataManager.executeQuery(
                "SELECT balance FROM ecoxpert_accounts WHERE player_uuid = ?",
                playerUuid.toString()
            ).thenApply(result -> {
                try (result) {
                    if (result.next()) {
                        BigDecimal balance = result.getBigDecimal("balance");
                        debug("getBalance found balance: " + balance + " for: " + playerUuid);
                        return balance;
                    }
                    debug("getBalance found no rows for: " + playerUuid);
                    return BigDecimal.ZERO;
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "ECOXPERT ERROR - getBalance failed for: " + playerUuid, throwable);
            return BigDecimal.ZERO;
        });
    }
    
    @Override
    public CompletableFuture<Void> setBalance(UUID playerUuid, BigDecimal balance, String reason) {
        validateAmount(balance);
        
        return ensureAccountExists(playerUuid).thenCompose(v -> {
            return dataManager.executeUpdate(
                "UPDATE ecoxpert_accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                balance, playerUuid.toString()
            ).thenCompose(rows -> {
                if (rows > 0) {
                    return logTransaction(null, playerUuid, balance, "BALANCE_SET", reason);
                }
                return CompletableFuture.completedFuture(null);
            });
        }).thenApply(v -> null);
    }
    
    @Override
    public CompletableFuture<Void> addMoney(UUID playerUuid, BigDecimal amount, String reason) {
        validateAmount(amount);
        plugin.getLogger().info("ECOXPERT DEBUG - addMoney called for: " + playerUuid + " amount: " + amount);
        
        return ensureAccountExists(playerUuid).thenCompose(v -> {
            plugin.getLogger().info("ECOXPERT DEBUG - ensureAccountExists completed, executing addMoney update");
            return dataManager.executeUpdate(
                "UPDATE ecoxpert_accounts SET balance = balance + ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                amount, playerUuid.toString()
            ).thenCompose(rows -> {
                plugin.getLogger().info("ECOXPERT DEBUG - addMoney update affected " + rows + " rows");
                if (rows > 0) {
                    return logTransaction(null, playerUuid, amount, "DEPOSIT", reason);
                }
                return CompletableFuture.completedFuture(null);
            });
        }).thenApply(v -> {
            plugin.getLogger().info("ECOXPERT DEBUG - addMoney completed for: " + playerUuid);
            return (Void) null;
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "ECOXPERT ERROR - addMoney failed for: " + playerUuid, throwable);
            return (Void) null;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> removeMoney(UUID playerUuid, BigDecimal amount, String reason) {
        validateAmount(amount);
        
        return ensureAccountExists(playerUuid).thenCompose(v -> {
            return hasSufficientFunds(playerUuid, amount).thenCompose(hasFunds -> {
                if (!hasFunds) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return dataManager.executeUpdate(
                    "UPDATE ecoxpert_accounts SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                    amount, playerUuid.toString()
                ).thenCompose(rows -> {
                    if (rows > 0) {
                        return logTransaction(playerUuid, null, amount, "WITHDRAWAL", reason)
                               .thenApply(result -> true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> transferMoney(UUID fromUuid, UUID toUuid, BigDecimal amount, String reason) {
        validateAmount(amount);
        
        if (fromUuid.equals(toUuid)) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }
        
        return ensureAccountExists(fromUuid).thenCompose(v -> ensureAccountExists(toUuid))
        .thenCompose(v -> {
            return dataManager.beginTransaction().thenCompose(transaction -> {
                // Check funds WITHIN transaction for atomicity
                return transaction.executeQuery(
                    "SELECT balance FROM ecoxpert_accounts WHERE player_uuid = ?",
                    fromUuid.toString()
                ).thenCompose(result -> {
                    try {
                        if (!result.next()) {
                            transaction.rollback();
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        BigDecimal currentBalance = result.getBigDecimal("balance");
                        if (currentBalance.compareTo(amount) < 0) {
                            transaction.rollback();
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // Remove from source
                        return transaction.executeUpdate(
                            "UPDATE ecoxpert_accounts SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                            amount, fromUuid.toString()
                        ).thenCompose(rows1 -> {
                        if (rows1 == 0) {
                            transaction.rollback();
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        // Add to target
                        return transaction.executeUpdate(
                            "UPDATE ecoxpert_accounts SET balance = balance + ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                            amount, toUuid.toString()
                        ).thenCompose(rows2 -> {
                            if (rows2 == 0) {
                                transaction.rollback();
                                return CompletableFuture.completedFuture(false);
                            }
                            
                            // Log transaction
                            return transaction.executeUpdate(
                                "INSERT INTO ecoxpert_transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)",
                                fromUuid.toString(), toUuid.toString(), amount, "TRANSFER", reason
                            ).thenCompose(rows3 -> {
                                return transaction.commit().thenApply(commitResult -> true);
                            });
                        });
                    });
                    } catch (Exception e) {
                        transaction.rollback();
                        throw new RuntimeException(e);
                    } finally {
                        result.close();
                    }
                }).exceptionally(throwable -> {
                    transaction.rollback();
                    return false;
                });
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> hasSufficientFunds(UUID playerUuid, BigDecimal amount) {
        return getBalance(playerUuid).thenApply(balance -> 
            balance.compareTo(amount) >= 0
        );
    }
    
    @Override
    public BigDecimal getStartingBalance() {
        return startingBalance;
    }
    
    @Override
    public BigDecimal getMaximumBalance() {
        return maximumBalance;
    }
    
    @Override
    public String formatMoney(BigDecimal amount) {
        return currencySymbol + moneyFormat.format(amount);
    }
    
    @Override
    public String getCurrencyNameSingular() {
        return currencyNameSingular;
    }
    
    @Override
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }
    
    @Override
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    @Override
    public CompletableFuture<Integer> applyWealthTax(BigDecimal rate, BigDecimal threshold, String reason) {
        // Validate inputs
        if (rate == null || rate.signum() <= 0 || threshold == null || threshold.signum() < 0) {
            return CompletableFuture.completedFuture(0);
        }

        // Reduce balances proportionally for accounts above threshold
        String sql = """
            UPDATE ecoxpert_accounts
            SET balance = balance - (balance * ?), updated_at = CURRENT_TIMESTAMP
            WHERE balance > ?
            """;

        return dataManager.executeUpdate(sql, rate, threshold).thenApply(rows -> {
            plugin.getLogger().info("Applied wealth tax at rate " + rate + ", threshold " + threshold + 
                ". Affected accounts: " + rows);
            return rows;
        });
    }
    
    /**
     * Load economy configuration from config files
     */
    private void loadConfiguration() {
        FileConfiguration config = configManager.getConfig();
        
        this.startingBalance = BigDecimal.valueOf(config.getDouble("economy.starting-balance", 1000.0));
        this.maximumBalance = config.getDouble("economy.maximum-balance", 0) > 0 
            ? BigDecimal.valueOf(config.getDouble("economy.maximum-balance"))
            : null;
        
        this.currencyNameSingular = config.getString("economy.currency.name-singular", "dollar");
        this.currencyNamePlural = config.getString("economy.currency.name-plural", "dollars");
        this.currencySymbol = config.getString("economy.currency.symbol", "$");
        this.decimalPlaces = config.getInt("economy.currency.decimal-places", 2);
    }
    
    /**
     * Setup money formatting
     */
    private void setupMoneyFormat() {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimalPlaces));
        }
        
        this.moneyFormat = new DecimalFormat(pattern.toString());
        this.moneyFormat.setRoundingMode(RoundingMode.HALF_UP);
    }
    
    /**
     * Ensure a player account exists, create if not
     */
    private CompletableFuture<Void> ensureAccountExists(UUID playerUuid) {
        plugin.getLogger().info("ECOXPERT DEBUG - ensureAccountExists called for: " + playerUuid);
        return hasAccount(playerUuid).thenCompose(exists -> {
            plugin.getLogger().info("ECOXPERT DEBUG - hasAccount returned: " + exists + " for: " + playerUuid);
            if (!exists) {
                plugin.getLogger().info("ECOXPERT DEBUG - Creating account with starting balance: " + startingBalance);
                return createAccount(playerUuid, startingBalance);
            }
            plugin.getLogger().info("ECOXPERT DEBUG - Account exists, no need to create");
            return CompletableFuture.completedFuture(null);
        });
    }
    
    /**
     * Log a transaction to the database
     */
    private CompletableFuture<Void> logTransaction(UUID fromUuid, UUID toUuid, BigDecimal amount, 
                                                  String type, String description) {
        return dataManager.executeUpdate(
            "INSERT INTO ecoxpert_transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)",
            fromUuid != null ? fromUuid.toString() : null,
            toUuid != null ? toUuid.toString() : null,
            amount, type, description
        ).thenApply(v -> null);
    }
    
    /**
     * Validate monetary amount
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (maximumBalance != null && amount.compareTo(maximumBalance) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum balance limit");
        }
        
        // Check decimal places
        if (amount.scale() > decimalPlaces) {
            throw new IllegalArgumentException("Amount has too many decimal places");
        }
    }
}
