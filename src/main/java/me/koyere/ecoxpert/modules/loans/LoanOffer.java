package me.koyere.ecoxpert.modules.loans;

import java.math.BigDecimal;

public record LoanOffer(boolean approved, BigDecimal amount, BigDecimal interestRate, int termDays, int score,
                        String reason) {
}

