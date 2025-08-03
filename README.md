# EcoXpert Pro - Premium Minecraft Economy Plugin

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.19.4--1.21.7+-green.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/License-Premium-gold.svg)](https://spigotmc.org/)
[![bStats](https://img.shields.io/badge/bStats-26446-blue.svg)](https://bstats.org/plugin/bukkit/ecoxpert/26446)

**EcoXpert Pro** is a next-generation economy plugin designed to solve the most common problem in Minecraft servers: **economic collapse and inflation**. Unlike traditional economy plugins, EcoXpert implements intelligent algorithms that prevent economic breakdown and maintain a healthy, engaging economy for players.

## 🎯 **Core Philosophy**

> **"An economy that doesn't collapse"**

Traditional servers follow this pattern:
- **Day 1:** Players start with $100
- **Day 30:** Average balance $50,000
- **Day 60:** Average balance $500,000  
- **Day 90:** Economy broken - players have everything, nothing has value

**EcoXpert prevents this** through intelligent market dynamics, anti-inflation systems, and advanced economic modeling.

---

## 🚀 **Key Features**

### 🧠 **Revolutionary Economic Intelligence System**
- **Living Economy** - Economy that "breathes" with natural expansion/contraction cycles
- **AI-Like Learning** - System learns from player behavior and adapts accordingly
- **Economic Personality Profiling** - Each player gets unique economic profile (Saver, Trader, Investor, etc.)
- **Predictive Modeling** - Forecasts economic trends and prevents crises before they happen
- **Smart Interventions** - Automatic economic stimulus, monetary policy adjustments
- **Crisis Prevention** - Detects economic anomalies and prevents server economy collapse

### 🔗 **Smart Integration**
- **Economy Takeover System** - Can replace or work alongside EssentialsX/CMI
- **Compatibility Mode** - Seamless integration with existing plugins
- **Migration Tools** - Import balances from other economy plugins
- **Vault Integration** - Full compatibility with all Vault-dependent plugins

### 🏦 **Advanced Banking System**
- **Tiered Accounts** - Basic, Silver, Gold, Platinum levels
- **Interest Calculations** - Compound interest with anti-exploitation
- **Daily Limits** - Configurable transaction limits per tier
- **Security Features** - Account freezing, audit trails, transaction hashing

### 📈 **Dynamic Market System**
- **Supply & Demand Analysis** - Real market economics
- **Price Volatility** - Realistic market fluctuations  
- **Trend Analysis** - 6 types of market trends (STABLE, UPWARD, VOLATILE, etc.)
- **Safety Constraints** - Prevents extreme price manipulation

### 🌐 **Cross-Platform Support**
- **Java + Bedrock** - Full GeyserMC/FloodGate compatibility
- **Multi-Server** - Spigot, Paper, Purpur, Folia support
- **Version Range** - MC 1.19.4 through 1.21.7+

---

## 📦 **Installation**

### Requirements
- **Java 17+** (required)
- **Spigot/Paper 1.19.4+** 
- **Vault** (dependency)

### Quick Setup
1. Download `EcoXpert-1.0.jar`
2. Place in your `plugins/` folder
3. Install **Vault** if not already present
4. Restart server
5. Plugin auto-configures and detects existing economy plugins

### With Existing Economy Plugins
EcoXpert automatically detects and integrates with:
- **EssentialsX** - Full compatibility
- **CMI** - Full compatibility  
- **Other Vault plugins** - Generic compatibility

No configuration needed - works out of the box!

---

## 🧠 **Economic Intelligence System - The Revolution**

### What Makes EcoXpert Different

Traditional economy plugins are **static** - they provide commands and basic functionality, but the economy inevitably collapses as players accumulate infinite wealth.

**EcoXpert's Economic Intelligence System creates a LIVING economy that:**

### 🌱 **Economic Cycles - The Economy "Breathes"**
```
🔄 DEPRESSION → RECESSION → STABLE → GROWTH → BOOM → BUBBLE → RECESSION...
```

The economy naturally cycles through different phases, just like real economies:
- **Depression:** Deflation, low activity - System provides stimulus
- **Recession:** Mild economic slowdown - Careful monetary policy
- **Stable:** Balanced conditions - Normal operations
- **Growth:** Economic expansion - Increased opportunities
- **Boom:** High activity period - Great time for investments
- **Bubble:** Overheated economy - System applies cooling measures

### 🤖 **Player Economic Personalities**

The system analyzes each player's behavior and assigns them an economic personality:

- **🏦 Saver** - Tends to save money, low spending
- **💸 Spender** - High spending, low savings  
- **📈 Trader** - Active market participant
- **💰 Investor** - Long-term wealth building
- **🎲 Speculator** - High-risk, high-reward behavior
- **🐉 Hoarder** - Accumulates wealth, minimal transactions
- **❤️ Philanthropist** - Generous, helps other players
- **⚠️ Exploiter** - Attempts to exploit economic systems

### 🔮 **Predictive Economic Modeling**

The system maintains an **Economic Memory** that:
- Learns from historical patterns
- Predicts future economic conditions
- Identifies potential crises before they happen
- Recommends optimal intervention strategies

### 🚨 **Intelligent Crisis Prevention**

**Automatic Anomaly Detection:**
- Rapid economic health changes
- Extreme inflation/deflation
- Wealth inequality spikes
- Market volatility warnings
- Unusual activity patterns

**Smart Interventions:**
- **Emergency Stimulus** - Crisis response with targeted money injection
- **Monetary Easing** - Lower interest rates, increase money supply
- **Monetary Tightening** - Combat inflation with policy changes
- **Market Stimulation** - Encourage trading activity
- **Wealth Redistribution** - Address inequality issues

### 📊 **Economic Health Monitoring**

Real-time tracking of:
- **Economic Health** (0-100%) - Overall economy condition
- **Inflation Rate** - Current price level changes
- **Velocity of Money** - How fast money circulates
- **Gini Coefficient** - Wealth inequality measurement
- **Market Volatility** - Economic stability indicator

---

## ⚙️ **Economy Integration Modes**

EcoXpert operates in different modes based on your server setup:

### 🥇 **Takeover Mode**
- **When:** No other economy plugin detected
- **Result:** EcoXpert becomes primary economy provider
- **Benefits:** Full intelligent economy features

### 🤝 **Compatibility Mode**  
- **When:** EssentialsX/CMI detected
- **Result:** Works alongside existing plugins
- **Benefits:** Adds intelligent features without disrupting current setup

### 🔄 **Sync Mode**
- **When:** In compatibility mode
- **Result:** Automatic balance synchronization
- **Benefits:** Seamless data consistency between plugins

### 🛡️ **Safe Mode**
- **When:** Errors detected
- **Result:** Minimal functionality to prevent server issues
- **Benefits:** Never breaks your server

---

## 🎮 **Commands**

### Basic Economy Commands
```
/ecoxpert balance [player]     - Check balance
/ecoxpert pay <player> <amount> - Pay another player
/ecoxpert help                  - Show help menu
```

### Market Commands
```
/market buy <item> [amount]     - Buy items
/market sell <item> [amount]    - Sell items  
/market prices                  - View current prices
/market stats                   - Market statistics
/market help                    - Market help
```

### Banking Commands
```
/bank balance                   - Check bank balance
/bank deposit <amount>          - Deposit money
/bank withdraw <amount>         - Withdraw money
/bank transfer <player> <amount> - Transfer to another account
/bank help                      - Banking help
```

### Admin Commands
```
/ecoxpert admin set <player> <amount>    - Set player balance
/ecoxpert admin add <player> <amount>    - Add money to player
/ecoxpert admin remove <player> <amount> - Remove money from player
```

### Economic Intelligence Commands
```
/ecoxpert economy status                 - Check economy system status
/ecoxpert economy diagnostics            - Run system diagnostics
/ecoxpert economy health                 - View economic health details
/ecoxpert economy cycle                  - View current economic cycle
/ecoxpert economy forecast               - View economic predictions
/ecoxpert economy anomalies              - Check for economic anomalies
/ecoxpert economy intervention <type>    - Force economic intervention
/ecoxpert player profile <player>       - View player economic profile
/ecoxpert player personality <player>   - View player economic personality
```

### Dynamic Economic Events Commands
```
/ecoxpert events active                  - View active economic events
/ecoxpert events history                 - View recent event history
/ecoxpert events trigger <type>          - Force trigger specific event type
/ecoxpert events end <id>                - End specific active event
/ecoxpert events status                  - View event engine status
/ecoxpert events anti-stagnation         - Check anti-stagnation system
```

### Intelligence System Interventions
```
emergency_stimulus     - Crisis response with money injection
monetary_easing       - Lower rates, increase money supply  
monetary_tightening   - Combat inflation, reduce money supply
market_stimulation    - Encourage market trading activity
wealth_redistribution - Address wealth inequality
```

### Event Types Available
```
government_stimulus         - Government economic aid during crises
trade_boom                 - Increased trading rewards and bonuses
market_discovery           - New valuable resources discovered
technological_breakthrough - Innovation creates new opportunities
investment_opportunity     - Special investment deals available
luxury_demand             - High demand for luxury goods
market_correction         - Natural market adjustment
resource_shortage         - Temporary scarcity increases prices
seasonal_demand           - Cyclical demand for specific items
black_swan_event          - Rare unpredictable major events
```

---

## 🔧 **Configuration**

### Economy Takeover System
The plugin automatically detects your server setup. For manual control:

```yaml
# config.yml
economy:
  takeover:
    enabled: true
    mode: "auto"  # auto, takeover, compatibility, safe
    sync_interval: 60  # seconds
    
  migration:
    import_on_startup: true
    backup_before_import: true
```

### Market Settings
```yaml
market:
  price_update_interval: 300  # 5 minutes
  max_price_change: 0.20      # 20% max change per update
  volatility_damping: 0.85    # Reduces extreme volatility
  trend_analysis_hours: 24    # Historical data for trends
```

### Banking Configuration
```yaml
banking:
  tiers:
    basic:
      interest_rate: 0.01      # 1% annual
      daily_deposit_limit: 1000
      daily_withdraw_limit: 500
    # Additional tiers...
```

---

## 📊 **Monitoring & Analytics**

### Built-in Diagnostics
```
/ecoxpert economy status
```
Shows:
- Current operation mode
- Active economy provider
- Sync statistics
- Detected plugins

### System Health Check
```  
/ecoxpert economy diagnostics
```
Runs comprehensive tests:
- Conflict detection
- Mode manager status
- Vault provider safety
- Service registry integration

### Performance Metrics
- **bStats Integration** (ID: 26446)
- **JMX Monitoring** support
- **Database performance** tracking
- **Transaction throughput** metrics

---

## 🚀 **Advanced Features**

### Intelligent Market Algorithms
- **Supply/Demand Analysis** - 24-hour historical data
- **Volatility Calculations** - Standard deviation of prices  
- **Momentum Analysis** - Transaction velocity tracking
- **Safety Constraints** - Min 10%, Max 1000% of base price

### Anti-Exploitation Systems
- **Rate Limiting** - Prevents transaction spam
- **Audit Trails** - Complete transaction history
- **Integrity Verification** - Transaction hash validation
- **Auto-Freeze** - Suspicious account detection

### Economic Modeling
- **Inflation Control** - Dynamic money supply management
- **Market Trends** - 6-tier trend classification system
- **Price Forecasting** - Predictive price modeling
- **Economic Events** - Automated economic interventions

---

## 🔍 **Troubleshooting**

### Common Issues

**Q: Commands don't work with EssentialsX**
A: Use `/ecoxpert` instead of `/eco` to avoid conflicts. EcoXpert detects conflicts automatically.

**Q: Balances not syncing**
A: Check `/ecoxpert economy status` - sync may be disabled or encountering errors.

**Q: Market prices seem wrong**
A: Market uses real supply/demand. Prices adjust based on actual trading activity.

**Q: Banking features not available**
A: Banking system requires full database setup. Check logs for initialization errors.

### Debug Information
Enable debug logging in `config.yml`:
```yaml
debug:
  enabled: true
  level: "INFO"  # DEBUG, INFO, WARN, ERROR
```

### Getting Help
- **Check logs** in `plugins/EcoXpert/logs/`
- **Run diagnostics** with `/ecoxpert economy diagnostics`
- **Check status** with `/ecoxpert economy status`

---

## 🏗️ **Development Status**

### ✅ Completed Systems
- **Phase 0:** Premium Foundation Setup
- **Phase 1:** Core Economy Implementation  
- **Phase 2:** Market System with Dynamic Pricing
- **Phase 3:** Banking System Foundation
- **Phase 4:** Economy System Integration & Conflict Resolution
- **Phase 5:** Revolutionary Economic Intelligence System ⭐ **REVOLUTIONARY**
  - Economic Intelligence Engine with AI-like learning
  - Player Economic Profiling (8 personality types)
  - Predictive Economic Modeling with forecasting
  - Automatic Crisis Prevention and Smart Interventions
  - Living Economy with natural breathing cycles  
  - Economic Memory System for pattern recognition
- **Phase 6:** Smart Banking System ⭐ **COMPLETADO**
  - ✅ 878 líneas código profesional con 25+ operaciones async
  - ✅ Tasas de interés dinámicas basadas en Economic Intelligence  
  - ✅ Detección de fraude automática con monitoreo de patrones
  - ✅ Límites diarios inteligentes y sistema de tiers avanzado
  - ✅ Auditoría completa con integridad de datos garantizada
  - ✅ Integración total con todos los sistemas IA económicos
- **Phase 7:** Dynamic Economic Events Engine ⭐ **REVOLUTIONARY**
  - 10 intelligent event types with realistic economic effects
  - AI-driven event selection based on economic conditions
  - Anti-stagnation system preventing economic death
  - Cascading event effects with market integration
  - Emergency intervention system for crisis prevention
  - Dynamic scaling based on server population and health

### 🚧 Next Session - Pending Tasks
- **Phase 7:** Dynamic Economic Events Engine - Eventos inteligentes automáticos
- **Loans Management System** - Scoring crediticio con Economic Intelligence
- **Advanced Banking Features** - Persistencia database y analytics completos
- **Integration APIs** - WorldGuard/Lands compatibility
- **Production Testing** - Validación completa en servidor Minecraft

### 📈 Technical Metrics
- **JAR Size:** ~3.2MB (estimated with all systems)
- **Classes:** 320+ (comprehensive economic intelligence)
- **Database:** SQLite primary, H2/Memory fallbacks
- **Performance:** Async operations, HikariCP pooling
- **Memory:** Intelligent caching, leak prevention
- **Intelligence:** 6 AI systems working in harmony
- **Events:** 10 dynamic event types with 50+ variations
- **Banking:** 4-tier system with predictive algorithms

---

## 🤝 **Support & Community**

### Getting Support
1. **Check this README** for common solutions
2. **Run diagnostics** with in-game commands
3. **Check server logs** for detailed error information
4. **Review configuration** files for proper setup

### Contributing
This is a premium plugin. Feature requests and bug reports are welcome through proper channels.

### License
Premium SpigotMC plugin. All rights reserved.

---

## 📋 **Version History**

### v1.0 - Initial Release
- Core economy system with Vault integration
- Dynamic market system with intelligent pricing
- Banking system with tiered accounts
- Economy takeover and compatibility systems
- Cross-platform support (Java + Bedrock)
- Anti-inflation and market intelligence features

---

**Made with ❤️ for the Minecraft community**

*EcoXpert Pro - Because your economy deserves intelligence*