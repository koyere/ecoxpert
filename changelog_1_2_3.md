# EcoXpert 1.2.3 — Debug Calm & Baltop Placeholders

- Fixed: economy debug logs now honor `plugin.debug` and stop spamming console when disabled.
- Added: EssentialsX-style baltop placeholders (all accept optional `essentials_` prefix):
  - `%ecox_baltop_balance_<rank>%` → raw balance
  - `%ecox_baltop_balance_commas_<rank>%` → balance with thousand separators
  - `%ecox_baltop_balance_fixed_<rank>%` → balance fixed to 2 decimals
  - `%ecox_baltop_balance_formatted_<rank>%` → formatted with currency symbol
  - `%ecox_baltop_player_<rank>%` / `%ecox_baltop_player_stripped_<rank>%` → player name at rank
  - `%ecox_baltop_rank%` → requester’s rank (1-based)
- Added: Bank placeholders: `%ecox_bank_balance%`, `%ecox_bank_tier%`, `%ecox_bank_interest_rate%`, daily remaining limits `%ecox_bank_daily_deposit_remaining%` / `%ecox_bank_daily_withdraw_remaining%` / `%ecox_bank_daily_transfer_remaining%`.
- Added: `/ecoxpert reload` admin command to reload configs, translations, and bank tier limits (perms: `ecoxpert.admin` or `ecoxpert.admin.reload`).
- Added: Bank daily limits now configurable per tier in `modules/bank.yml`; limit errors show tier, limit, and remaining (resets daily, server time).
- Added: Loan placeholders: `%ecox_loan_status%`, `%ecox_loan_principal%`, `%ecox_loan_outstanding_raw%`, `%ecox_loan_interest_rate%`, `%ecox_loan_next_due_amount%`, `%ecox_loan_next_due_date%`, `%ecox_loan_next_due_in_days%`, `%ecox_loan_installments_left%`.
