package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Interest calculation engine
 * 
 * Professional interest calculation system with compound interest,
 * precise mathematics, and anti-exploitation measures.
 */
public class InterestCalculator {

    // Constants for precise calculations
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Minimum balance for interest calculation (prevents micro-spam)
    private static final BigDecimal MINIMUM_INTEREST_BALANCE = new BigDecimal("100");

    // Maximum interest per calculation (anti-exploitation)
    private static final BigDecimal MAXIMUM_DAILY_INTEREST = new BigDecimal("10000");

    /**
     * Calculate daily compound interest for account
     */
    public static BigDecimal calculateDailyInterest(BankAccount account) {
        if (account == null || account.isFrozen()) {
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();

        // Skip calculation for small balances
        if (balance.compareTo(MINIMUM_INTEREST_BALANCE) < 0) {
            return BigDecimal.ZERO;
        }

        BankAccountTier tier = account.getTier();
        BigDecimal dailyRate = tier.getDailyInterestRate();

        // Calculate compound interest: Principal * (1 + rate)^days - Principal
        BigDecimal onePlusRate = BigDecimal.ONE.add(dailyRate);
        BigDecimal interestAmount = balance.multiply(onePlusRate.subtract(BigDecimal.ONE));

        // Apply anti-exploitation cap
        if (interestAmount.compareTo(MAXIMUM_DAILY_INTEREST) > 0) {
            interestAmount = MAXIMUM_DAILY_INTEREST;
        }

        // Round to 2 decimal places
        return interestAmount.setScale(2, ROUNDING_MODE);
    }

    /**
     * Calculate interest for specific time period
     */
    public static BigDecimal calculateInterestForPeriod(BigDecimal principal,
            BankAccountTier tier,
            int days) {
        if (principal == null || principal.compareTo(MINIMUM_INTEREST_BALANCE) < 0 || days <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyRate = tier.getDailyInterestRate();

        // Compound interest formula: P * (1 + r)^n - P
        BigDecimal onePlusRate = BigDecimal.ONE.add(dailyRate);
        BigDecimal compound = onePlusRate.pow(days);
        BigDecimal futureValue = principal.multiply(compound);
        BigDecimal interestEarned = futureValue.subtract(principal);

        // Apply daily cap per day
        BigDecimal maxInterest = MAXIMUM_DAILY_INTEREST.multiply(new BigDecimal(days));
        if (interestEarned.compareTo(maxInterest) > 0) {
            interestEarned = maxInterest;
        }

        return interestEarned.setScale(2, ROUNDING_MODE);
    }

    /**
     * Calculate projected interest for future period
     */
    public static BigDecimal calculateProjectedInterest(BankAccount account, int days) {
        if (account == null || account.isFrozen() || days <= 0) {
            return BigDecimal.ZERO;
        }

        return calculateInterestForPeriod(account.getBalance(), account.getTier(), days);
    }

    /**
     * Calculate interest since last calculation
     */
    public static BigDecimal calculateAccruedInterest(BankAccount account) {
        if (account == null || account.isFrozen()) {
            return BigDecimal.ZERO;
        }

        LocalDateTime lastCalculation = account.getLastInterestCalculation();
        LocalDateTime now = LocalDateTime.now();

        // Calculate days since last interest calculation
        long daysSinceLastCalculation = ChronoUnit.DAYS.between(lastCalculation, now);

        if (daysSinceLastCalculation <= 0) {
            return BigDecimal.ZERO;
        }

        // Limit to prevent abuse from very old accounts
        if (daysSinceLastCalculation > 30) {
            daysSinceLastCalculation = 30;
        }

        return calculateInterestForPeriod(account.getBalance(), account.getTier(),
                (int) daysSinceLastCalculation);
    }

    /**
     * Calculate annual percentage yield (APY) for tier
     */
    public static BigDecimal calculateAPY(BankAccountTier tier) {
        BigDecimal dailyRate = tier.getDailyInterestRate();

        // APY = (1 + daily_rate)^365 - 1
        BigDecimal onePlusRate = BigDecimal.ONE.add(dailyRate);
        BigDecimal compoundAnnual = onePlusRate.pow(365);
        BigDecimal apy = compoundAnnual.subtract(BigDecimal.ONE);

        return apy.multiply(new BigDecimal("100")).setScale(3, ROUNDING_MODE); // Convert to percentage
    }

    /**
     * Calculate time to reach target balance
     */
    public static int calculateDaysToReachBalance(BigDecimal currentBalance,
            BigDecimal targetBalance,
            BankAccountTier tier) {
        if (currentBalance.compareTo(targetBalance) >= 0) {
            return 0;
        }

        if (currentBalance.compareTo(MINIMUM_INTEREST_BALANCE) < 0) {
            return -1; // Cannot reach target with interest alone
        }

        BigDecimal dailyRate = tier.getDailyInterestRate();

        // Use logarithmic formula: days = ln(target/current) / ln(1 + rate)
        double current = currentBalance.doubleValue();
        double target = targetBalance.doubleValue();
        double rate = dailyRate.doubleValue();

        if (rate <= 0) {
            return -1; // No interest rate
        }

        double days = Math.log(target / current) / Math.log(1 + rate);

        // Cap at reasonable maximum
        return Math.min((int) Math.ceil(days), 36500); // 100 years max
    }

    /**
     * Validate interest calculation parameters
     */
    public static boolean validateInterestCalculation(BigDecimal principal,
            BankAccountTier tier,
            int days) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        if (tier == null) {
            return false;
        }

        if (days < 0 || days > 36500) { // Max 100 years
            return false;
        }

        BigDecimal dailyRate = tier.getDailyInterestRate();
        if (dailyRate.compareTo(BigDecimal.ZERO) < 0 ||
                dailyRate.compareTo(new BigDecimal("0.1")) > 0) { // Max 10% daily rate
            return false;
        }

        return true;
    }

    /**
     * Get minimum balance for interest calculation
     */
    public static BigDecimal getMinimumInterestBalance() {
        return MINIMUM_INTEREST_BALANCE;
    }

    /**
     * Get maximum daily interest cap
     */
    public static BigDecimal getMaximumDailyInterest() {
        return MAXIMUM_DAILY_INTEREST;
    }

    /**
     * Format interest rate for display
     */
    public static String formatInterestRate(BigDecimal rate) {
        BigDecimal percentage = rate.multiply(new BigDecimal("100"));
        return percentage.setScale(3, ROUNDING_MODE) + "%";
    }

    /**
     * Format APY for display
     */
    public static String formatAPY(BankAccountTier tier) {
        BigDecimal apy = calculateAPY(tier);
        return apy.setScale(2, ROUNDING_MODE) + "%";
    }
}