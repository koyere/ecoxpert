package me.koyere.ecoxpert.core.failsafe;

import java.util.List;
import java.util.UUID;

/**
 * Transaction history for a player
 */
public class TransactionHistory {
    private final UUID playerUuid;
    private final List<TransactionRecord> transactions;
    private final int totalCount;
    private final boolean hasMore;
    
    public TransactionHistory(UUID playerUuid, List<TransactionRecord> transactions, 
                            int totalCount, boolean hasMore) {
        this.playerUuid = playerUuid;
        this.transactions = List.copyOf(transactions);
        this.totalCount = totalCount;
        this.hasMore = hasMore;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public List<TransactionRecord> getTransactions() {
        return transactions;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public boolean hasMore() {
        return hasMore;
    }
    
    public int getReturnedCount() {
        return transactions.size();
    }
}