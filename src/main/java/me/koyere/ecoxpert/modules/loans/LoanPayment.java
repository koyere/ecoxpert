package me.koyere.ecoxpert.modules.loans;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanPayment(long id, long loanId, int installmentNo, LocalDate dueDate,
                          BigDecimal amountDue, BigDecimal paidAmount, String status) {
}

