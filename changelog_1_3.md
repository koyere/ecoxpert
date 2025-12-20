# EcoXpert - Changelog v1.3.0

## Version 1.3.0 - Balance Leaderboard & Discord Integration

**Release Date:** December 19, 2025  
**Compatibility:** Minecraft 1.19.4 - 1.21.8+  
**Java Version:** 17+

---

## 🎯 **What's New**

### ⭐ **Balance Leaderboard System (Baltop)**
Complete leaderboard system with cross-platform support for Java and Bedrock editions.

**Commands:**
```
/baltop [page]           - View leaderboard in chat
/baltop gui [page]       - Open GUI leaderboard
```
**Aliases:** `/balancetop`, `/topbalances`

**Features:**
- 📊 Console view: 10 entries per page with pagination
- 🖥️ GUI view: 45 entries per page with player skulls
- 📱 Bedrock Forms: Native Geyser Forms for mobile/console players
- 🏆 Self-highlighting: Your position highlighted in gold
- 🔄 Real-time data with 30-second smart caching
- 📍 Rank display: See your position even when not on current page

### 📱 **Discord Integration** (NEW)
Full DiscordSRV integration for economy notifications and commands.

**Discord Commands:**
```
!balance <player>        - Check player balance
!market <item>           - Check item prices
!top [count]             - View top players
!inflation               - Current inflation rate
!stats                   - Economy statistics
```

**Notifications:**
- 💰 Large transactions (configurable threshold)
- 📈 Market price changes (>20% by default)
- ⚠️ Inflation alerts (>5% by default)
- 📊 Daily economy reports

---

## 🔧 **Technical Improvements**

### **Performance & Compatibility**
- Smart caching system reduces database queries
- Async operations prevent server lag
- 100% Bedrock Edition compatibility maintained
- Automatic Geyser Forms detection and fallback

### **Cross-Platform Support**
- **Java Edition:** Full chest GUI support
- **Bedrock Edition:** Native Geyser Forms when available
- **Automatic Fallback:** Chest GUI when Geyser unavailable

---

## 🎮 **Commands & Permissions**

### **New Commands**
| Command | Permission | Description |
|---------|------------|-------------|
| `/baltop [page]` | `ecoxpert.baltop` | View balance leaderboard |
| `/baltop gui [page]` | `ecoxpert.baltop.gui` | Open GUI leaderboard |

### **New Permissions**
- `ecoxpert.baltop` - View balance leaderboard (default: true)
- `ecoxpert.baltop.gui` - Open GUI leaderboard (default: true)

---

## 📊 **PlaceholderAPI Integration**

### **New Placeholders**
```
%ecox_baltop_balance_<rank>%              - Balance at specific rank
%ecox_baltop_balance_formatted_<rank>%    - Formatted balance with currency
%ecox_baltop_player_<rank>%               - Player name at rank
%ecox_baltop_rank%                        - Your current rank
```

**Example:** `%ecox_baltop_player_1%` returns the richest player's name

---

## 🔧 **Configuration**

### **Discord Setup**
1. Install DiscordSRV plugin
2. Configure channels in `modules/discord.yml`:
   ```yaml
   discord:
     enabled: true
     channels:
       economy: "CHANNEL_ID"
       alerts: "CHANNEL_ID"
   ```
3. Reload: `/ecoxpert reload`

### **Baltop Settings**
- **Console:** 10 entries per page
- **GUI:** 45 entries per page  
- **Cache:** 30 seconds TTL
- **No configuration required** - works out of the box

---

## 📦 **Installation & Update**

### **New Installation**
1. Download `EcoXpert-1.3.0.jar`
2. Place in `plugins/` folder
3. Install Vault (required) and DiscordSRV (optional)
4. Restart server

### **Update from v1.2.x**
1. Stop server
2. Replace JAR file
3. Start server
4. **No database changes** - fully backward compatible

---

## 🐛 **Bug Fixes**

- Fixed potential memory leak in leaderboard queries
- Improved error handling for empty result sets
- Enhanced GUI pagination edge cases
- Better Discord integration error handling

---

## 🔗 **Integration Status**

### **Fully Supported**
- ✅ **Vault** - Complete economy integration
- ✅ **PlaceholderAPI** - 50+ placeholders available
- ✅ **DiscordSRV** - Full notification and command support
- ✅ **GeyserMC** - Native Bedrock Forms support
- ✅ **WorldGuard/Lands** - Territory-based bonuses
- ✅ **Jobs/Towny/Slimefun** - Economic adjustments

---

## 📞 **Support**

- **Discord:** https://discord.gg/xKUjn3EJzR
- **Issues:** GitHub Issues
- **Documentation:** README.md + in-game help

---

*EcoXpert Pro - Because your economy deserves intelligence*
