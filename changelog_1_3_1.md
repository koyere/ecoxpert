# EcoXpert - Changelog v1.3.1

## Version 1.3.1 - Bug Fixes & Stability

**Release Date:** January 8, 2026
**Compatibility:** Minecraft 1.19.4 - 1.21.11
**Java Version:** 17+

---

## 🐛 **Bug Fixes**

### **SQLite Database Lock Fix**
- **Problem:** `SQLITE_BUSY - database is locked` error under concurrent operations
- **Solution:** SQLite pool size reduced to 1 connection to serialize writes
- **Details:**
  - SQLite only allows one writer at a time
  - HikariCP pool was configured with 10 connections causing conflicts
  - `busy_timeout` increased to 30 seconds as safety net
  - MySQL pool remains at 10 connections (handles concurrency properly)

### **SQL Compatibility Fix**
- **Problem:** `INSERT OR IGNORE` syntax only works in SQLite, not MySQL
- **Solution:** Database-specific SQL syntax detection
- **Details:**
  - SQLite: `INSERT OR IGNORE INTO...`
  - MySQL: `INSERT IGNORE INTO...`
  - Affected files: `EconomyManagerImpl.java`, `MarketManagerImpl.java`

### **Market GUI Click Handler Fix**
- **Problem:** Clicking "Sell Hand by $" button sent "hand" to chat instead of opening sub-GUI
- **Solution:** Reordered event handlers to check sub-GUI title before `marketInv == null`
- **Details:**
  - Sub-GUI was not tracked in `openGUIs` map
  - Handler returned early without cancelling the event
  - Same fix applied to `MarketOrdersGUI.java`

### **Missing Translation Keys Fix**
- **Problem:** Translation keys displayed as raw text (e.g., `market.gui.sell-hand.lore1`)
- **Solution:** Added missing keys to correct YAML paths
- **Added keys:**
  - `market.gui.sell-hand.*` (title, amount, lore1, lore2)
  - `market.gui.bedrock.action.*` (19 keys for Bedrock action GUI)
- **Files:** `messages_en.yml`, `messages_es.yml`

### **Translation System Fallback Improvement**
- **Problem:** New translation keys not available if user has old language files
- **Solution:** Improved fallback to always check embedded JAR resources for missing keys
- **Details:**
  - Local files are checked first (preserves user customizations)
  - If key not found, automatically loads from JAR embedded resources
  - Added UTF-8 encoding for proper character support
  - Debug logging for troubleshooting translation issues

### **GUI Item Theft Prevention**
- **Problem:** Items in GUIs could be moved to player inventory when translation keys failed
- **Solution:** All GUI click handlers now check both translated title AND raw key
- **Affected GUIs:**
  - MarketGUI: List GUI, Sell Hand sub-GUI
  - MarketOrdersGUI: Orders list, Select quantity
  - LoansGUI: Offer preview, Schedule

---

## 📦 **Update Instructions**

1. Stop server
2. Replace `EcoXpert-1.3.0.jar` with `EcoXpert-1.3.1.jar`
3. Start server
4. **No configuration changes required**

---

*EcoXpert Pro - Because your economy deserves intelligence*
