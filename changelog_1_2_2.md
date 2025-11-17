# EcoXpert 1.2.2 — Market Safety & Integrity

- Fixed: hard blockers on market buys/sells/orders when unit price or totals are zero/negative; cache and price calculator now auto-heal corrupt prices back to safe base values so items can’t be bought for free.
- Improved: economy operations now require strictly positive amounts, tightening validation across transfers/debits.
- Compatibility: Java 17+, Spigot/Paper/Purpur/Folia 1.19.4–1.21.9+ (no API changes).
- Update: build or download `EcoXpert-1.2.2.jar`, replace the old JAR in `plugins/`, and restart your server. No config changes required.
