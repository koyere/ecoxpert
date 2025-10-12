package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;

/** Loan offer information */
public class LoanOfferInfo {
    private final BigDecimal maxAmount;
    private final double interestRate;
    private final int termDays;
    private final int creditScore;
    private final String riskLevel;

    public LoanOfferInfo(BigDecimal maxAmount, double interestRate, int termDays, int creditScore, String riskLevel) {
        this.maxAmount = maxAmount;
        this.interestRate = interestRate;
        this.termDays = termDays;
        this.creditScore = creditScore;
        this.riskLevel = riskLevel;
    }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public double getInterestRate() { return interestRate; }
    public int getTermDays() { return termDays; }
    public int getCreditScore() { return creditScore; }
    public String getRiskLevel() { return riskLevel; }
}
