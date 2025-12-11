package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

/**
 * Monthly bank statement
 * 
 * Immutable monthly statement with comprehensive transaction
 * summary and account performance analytics.
 */
public class BankStatement {

    private final UUID accountId;
    private final int year;
    private final Month month;
    private final String accountNumber;
    private final BankAccountTier accountTier;

    // Balance information
    private final BigDecimal openingBalance;
    private final BigDecimal closingBalance;
    private final BigDecimal averageBalance;
    private final BigDecimal minimumBalance;
    private final BigDecimal maximumBalance;

    // Transaction summaries
    private final int totalTransactions;
    private final BigDecimal totalDeposits;
    private final BigDecimal totalWithdrawals;
    private final BigDecimal totalTransfersIn;
    private final BigDecimal totalTransfersOut;
    private final BigDecimal totalInterest;
    private final BigDecimal totalFees;
    private final BigDecimal netChange;

    // Transaction counts
    private final int depositCount;
    private final int withdrawalCount;
    private final int transferInCount;
    private final int transferOutCount;
    private final int interestPayments;
    private final int feeCharges;

    // Performance metrics
    private final BigDecimal interestRate;
    private final BigDecimal effectiveYield;
    private final int daysActive;

    private final LocalDateTime generatedAt;
    private final List<BankTransaction> transactions;

    private BankStatement(Builder builder) {
        this.accountId = builder.accountId;
        this.year = builder.year;
        this.month = builder.month;
        this.accountNumber = builder.accountNumber;
        this.accountTier = builder.accountTier;

        this.openingBalance = builder.openingBalance;
        this.closingBalance = builder.closingBalance;
        this.averageBalance = builder.averageBalance;
        this.minimumBalance = builder.minimumBalance;
        this.maximumBalance = builder.maximumBalance;

        this.totalTransactions = builder.totalTransactions;
        this.totalDeposits = builder.totalDeposits;
        this.totalWithdrawals = builder.totalWithdrawals;
        this.totalTransfersIn = builder.totalTransfersIn;
        this.totalTransfersOut = builder.totalTransfersOut;
        this.totalInterest = builder.totalInterest;
        this.totalFees = builder.totalFees;
        this.netChange = calculateNetChange();

        this.depositCount = builder.depositCount;
        this.withdrawalCount = builder.withdrawalCount;
        this.transferInCount = builder.transferInCount;
        this.transferOutCount = builder.transferOutCount;
        this.interestPayments = builder.interestPayments;
        this.feeCharges = builder.feeCharges;

        this.interestRate = builder.interestRate;
        this.effectiveYield = calculateEffectiveYield();
        this.daysActive = builder.daysActive;

        this.generatedAt = LocalDateTime.now();
        this.transactions = List.copyOf(builder.transactions);
    }

    /**
     * Calculate net change for the month
     */
    private BigDecimal calculateNetChange() {
        return closingBalance.subtract(openingBalance);
    }

    /**
     * Calculate effective yield for the month
     */
    private BigDecimal calculateEffectiveYield() {
        if (averageBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal yield = totalInterest.divide(averageBalance, 6, RoundingMode.HALF_UP);
        return yield.multiply(new BigDecimal("100")); // Convert to percentage
    }

    /**
     * Get statement period description
     */
    public String getStatementPeriod() {
        return month.name() + " " + year;
    }

    /**
     * Check if statement shows positive performance
     */
    public boolean isPositivePerformance() {
        return netChange.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get transaction activity level
     */
    public ActivityLevel getActivityLevel() {
        if (totalTransactions == 0)
            return ActivityLevel.INACTIVE;
        if (totalTransactions <= 5)
            return ActivityLevel.LOW;
        if (totalTransactions <= 20)
            return ActivityLevel.MODERATE;
        if (totalTransactions <= 50)
            return ActivityLevel.HIGH;
        return ActivityLevel.VERY_HIGH;
    }

    /**
     * Calculate average transaction amount
     */
    public BigDecimal getAverageTransactionAmount() {
        if (totalTransactions == 0)
            return BigDecimal.ZERO;

        BigDecimal totalVolume = totalDeposits.add(totalWithdrawals)
                .add(totalTransfersIn)
                .add(totalTransfersOut);

        return totalVolume.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get formatted summary text
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Statement for ").append(getStatementPeriod()).append("\n");
        summary.append("Opening Balance: $").append(openingBalance).append("\n");
        summary.append("Closing Balance: $").append(closingBalance).append("\n");
        summary.append("Net Change: $").append(netChange).append("\n");
        summary.append("Interest Earned: $").append(totalInterest).append("\n");
        summary.append("Total Transactions: ").append(totalTransactions).append("\n");
        summary.append("Activity Level: ").append(getActivityLevel().getDisplayName());

        return summary.toString();
    }

    // === Builder Pattern ===

    public static class Builder {
        private UUID accountId;
        private int year;
        private Month month;
        private String accountNumber;
        private BankAccountTier accountTier;

        private BigDecimal openingBalance = BigDecimal.ZERO;
        private BigDecimal closingBalance = BigDecimal.ZERO;
        private BigDecimal averageBalance = BigDecimal.ZERO;
        private BigDecimal minimumBalance = BigDecimal.ZERO;
        private BigDecimal maximumBalance = BigDecimal.ZERO;

        private int totalTransactions = 0;
        private BigDecimal totalDeposits = BigDecimal.ZERO;
        private BigDecimal totalWithdrawals = BigDecimal.ZERO;
        private BigDecimal totalTransfersIn = BigDecimal.ZERO;
        private BigDecimal totalTransfersOut = BigDecimal.ZERO;
        private BigDecimal totalInterest = BigDecimal.ZERO;
        private BigDecimal totalFees = BigDecimal.ZERO;

        private int depositCount = 0;
        private int withdrawalCount = 0;
        private int transferInCount = 0;
        private int transferOutCount = 0;
        private int interestPayments = 0;
        private int feeCharges = 0;

        private BigDecimal interestRate = BigDecimal.ZERO;
        private int daysActive = 0;

        private List<BankTransaction> transactions = List.of();

        public Builder setAccountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setYear(int year) {
            this.year = year;
            return this;
        }

        public Builder setMonth(Month month) {
            this.month = month;
            return this;
        }

        public Builder setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder setAccountTier(BankAccountTier accountTier) {
            this.accountTier = accountTier;
            return this;
        }

        public Builder setOpeningBalance(BigDecimal openingBalance) {
            this.openingBalance = openingBalance;
            return this;
        }

        public Builder setClosingBalance(BigDecimal closingBalance) {
            this.closingBalance = closingBalance;
            return this;
        }

        public Builder setAverageBalance(BigDecimal averageBalance) {
            this.averageBalance = averageBalance;
            return this;
        }

        public Builder setMinimumBalance(BigDecimal minimumBalance) {
            this.minimumBalance = minimumBalance;
            return this;
        }

        public Builder setMaximumBalance(BigDecimal maximumBalance) {
            this.maximumBalance = maximumBalance;
            return this;
        }

        public Builder setTotalTransactions(int totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public Builder setTotalDeposits(BigDecimal totalDeposits) {
            this.totalDeposits = totalDeposits;
            return this;
        }

        public Builder setTotalWithdrawals(BigDecimal totalWithdrawals) {
            this.totalWithdrawals = totalWithdrawals;
            return this;
        }

        public Builder setTotalTransfersIn(BigDecimal totalTransfersIn) {
            this.totalTransfersIn = totalTransfersIn;
            return this;
        }

        public Builder setTotalTransfersOut(BigDecimal totalTransfersOut) {
            this.totalTransfersOut = totalTransfersOut;
            return this;
        }

        public Builder setTotalInterest(BigDecimal totalInterest) {
            this.totalInterest = totalInterest;
            return this;
        }

        public Builder setTotalFees(BigDecimal totalFees) {
            this.totalFees = totalFees;
            return this;
        }

        public Builder setDepositCount(int depositCount) {
            this.depositCount = depositCount;
            return this;
        }

        public Builder setWithdrawalCount(int withdrawalCount) {
            this.withdrawalCount = withdrawalCount;
            return this;
        }

        public Builder setTransferInCount(int transferInCount) {
            this.transferInCount = transferInCount;
            return this;
        }

        public Builder setTransferOutCount(int transferOutCount) {
            this.transferOutCount = transferOutCount;
            return this;
        }

        public Builder setInterestPayments(int interestPayments) {
            this.interestPayments = interestPayments;
            return this;
        }

        public Builder setFeeCharges(int feeCharges) {
            this.feeCharges = feeCharges;
            return this;
        }

        public Builder setInterestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
            return this;
        }

        public Builder setDaysActive(int daysActive) {
            this.daysActive = daysActive;
            return this;
        }

        public Builder setTransactions(List<BankTransaction> transactions) {
            this.transactions = transactions;
            return this;
        }

        public BankStatement build() {
            if (accountId == null)
                throw new IllegalArgumentException("Account ID is required");
            if (accountNumber == null)
                throw new IllegalArgumentException("Account number is required");
            if (accountTier == null)
                throw new IllegalArgumentException("Account tier is required");

            return new BankStatement(this);
        }
    }

    /**
     * Activity level enumeration
     */
    public enum ActivityLevel {
        INACTIVE("Inactive"),
        LOW("Low Activity"),
        MODERATE("Moderate Activity"),
        HIGH("High Activity"),
        VERY_HIGH("Very High Activity");

        private final String displayName;

        ActivityLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === Getters ===

    public UUID getAccountId() {
        return accountId;
    }

    public int getYear() {
        return year;
    }

    public Month getMonth() {
        return month;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BankAccountTier getAccountTier() {
        return accountTier;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public BigDecimal getAverageBalance() {
        return averageBalance;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public BigDecimal getMaximumBalance() {
        return maximumBalance;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public BigDecimal getTotalTransfersIn() {
        return totalTransfersIn;
    }

    public BigDecimal getTotalTransfersOut() {
        return totalTransfersOut;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }

    public int getDepositCount() {
        return depositCount;
    }

    public int getWithdrawalCount() {
        return withdrawalCount;
    }

    public int getTransferInCount() {
        return transferInCount;
    }

    public int getTransferOutCount() {
        return transferOutCount;
    }

    public int getInterestPayments() {
        return interestPayments;
    }

    public int getFeeCharges() {
        return feeCharges;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public BigDecimal getEffectiveYield() {
        return effectiveYield;
    }

    public int getDaysActive() {
        return daysActive;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public List<BankTransaction> getTransactions() {
        return transactions;
    }
}