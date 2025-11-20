# EcoXpert 1.2.3 — Debug Calm, Baltop, Sync & Bank UX

## Bug fixes
- Economy debug logs now respect `plugin.debug` (no spam when disabled).

## New placeholders
- EssentialsX-style baltop (also accept `essentials_` prefix):  
  `%ecox_baltop_balance_<rank>%`, `%ecox_baltop_balance_commas_<rank>%`, `%ecox_baltop_balance_fixed_<rank>%`, `%ecox_baltop_balance_formatted_<rank>%`, `%ecox_baltop_player_<rank>%`, `%ecox_baltop_player_stripped_<rank>%`, `%ecox_baltop_rank%`.
- Bank: `%ecox_bank_balance%`, `%ecox_bank_tier%`, `%ecox_bank_interest_rate%`, `%ecox_bank_daily_deposit_remaining%`, `%ecox_bank_daily_withdraw_remaining%`, `%ecox_bank_daily_transfer_remaining%`.
- Loans: `%ecox_loan_status%`, `%ecox_loan_principal%`, `%ecox_loan_outstanding_raw%`, `%ecox_loan_interest_rate%`, `%ecox_loan_next_due_amount%`, `%ecox_loan_next_due_date%`, `%ecox_loan_next_due_in_days%`, `%ecox_loan_installments_left%`.

## EssentialsX sync
- Config: `economy.sync.mode` (`off|pull|push|bidirectional`), `economy.sync.interval-seconds`, `economy.sync.min-delta`.
- Commands: `/ecoxpert economy sync [player|all|status]` and `/ecoxpert migrate balances` now use the sync/import service (syncs balances only; bank/loans/market are untouched).

## Banking UX
- /bank and BankGUI now show amount + new bank balance on deposit/withdraw/transfer.
- Daily limits per tier raised in `modules/bank.yml` so users are not blocked by default (adjust to your policy).
- `/ecoxpert reload` reloads configs, translations, and bank tier limits.

## How to update/apply
1) Replace the JAR in `plugins/` with `EcoXpert-1.2.3.jar`.
2) Restart the server (recommended) or run `/ecoxpert reload` after restart for configs/lang refresh.
3) Optional: adjust `economy.sync.*` in `config.yml` and tier limits in `modules/bank.yml` to your desired caps, then `/ecoxpert reload`.
4) If you used `/eco` (Essentials) while EcoXpert was active, run `/ecoxpert migrate balances` or `/ecoxpert economy sync` (pull mode) to import balances.
