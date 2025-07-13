Documento de Diseño y Planificación del Plugin: EcoXpert
Fecha de Creación: 2 de junio de 2025
Versión del Documento: 1.0

1. Introducción
   Nombre del Plugin: EcoXpert
   Slogan: "The ultimate intelligent economy plugin for Minecraft servers."
   Objetivo Principal: Desarrollar un plugin de economía premium, modular, avanzado y altamente configurable para servidores de Minecraft, compatible con Spigot, PaperMC y sus forks, ofreciendo una experiencia económica dinámica y robusta.
2. Requisitos y Características Generales
   2.1. Compatibilidad
   Versiones de Minecraft: 1.16.5 – 1.21.5 (y versiones futuras según sea posible). Se priorizará el uso de APIs estables en este rango.
   Software de Servidor:
   API Principal: Spigot API.
   Compatibilidad Extendida: PaperMC API y forks derivados (ej. Purpur) para aprovechar optimizaciones y características adicionales, manteniendo siempre la funcionalidad base en Spigot.
   2.2. Estándares de Calidad y Diseño
   Desarrollo Profesional:
   Código fuente optimizado para rendimiento y bajo consumo de recursos.
   Prevención activa de memory leaks y otros problemas comunes.
   Código robusto con manejo adecuado de errores y excepciones.
   Modularidad Extrema:
   Estructura de código dividida en módulos funcionales independientes y cohesivos.
   Archivos de configuración separados por módulo cuando sea apropiado para mayor claridad.
   Organización lógica de carpetas y paquetes para facilitar la escalabilidad y el mantenimiento.
   Documentación y Lenguaje:
   Todo el código fuente (clases, métodos, variables) y comentarios internos se escribirán en inglés.
   Se mantendrá una documentación clara de la API pública y de las configuraciones para los administradores.
   2.3. Integraciones y Características Fundamentales
   API de Integración:
   Diseño "API-first" para todos los módulos, facilitando la comunicación interna.
   Implementación de una API pública (Módulo 7) para que otros plugins puedan interactuar y extender EcoXpert.
   bStats:
   Implementación de bStats para recolectar métricas anónimas de uso del plugin, ayudando a entender su adopción y las características más utilizadas.
   Notificador de Actualizaciones:
   Sistema que informará a los administradores del servidor (vía consola al inicio) sobre nuevas versiones disponibles de EcoXpert, con opción de configuración para activarlo/desactivarlo.
   Actualización de Archivos de Configuración:
   Mecanismo para actualizar automáticamente los archivos de configuración (config.yml, messages_xx.yml, etc.) en el servidor cuando se instala una nueva versión del plugin. Este sistema buscará preservar las personalizaciones del usuario y añadir nuevas opciones por defecto.
   Proveedor de Economía Principal:
   EcoXpert actuará como el sistema económico central del servidor.
   Implementará la interfaz Economy de Vault, permitiendo que otros plugins utilicen EcoXpert para gestionar transacciones y saldos.
   2.4. Herramientas de Desarrollo
   Build System: Maven.
   Entorno de Desarrollo (IDE): IntelliJ IDEA.
   Control de Versiones: Git (se recomienda usar plataformas como GitHub/GitLab).
3. Módulos del Plugin (Funciones Detalladas)
   3.1. Módulo 1 – Mercado Dinámico (Dynamic Market System)
   Objetivo: Ajustar automáticamente los precios de los ítems según la oferta y demanda real en el servidor.
   Funciones Clave:
   Precios variables por ítem (compra y venta) que fluctúan según la actividad económica.
   Monitor de actividad del mercado (ventas por día, volumen total por ítem, etc.).
   Límite de compra/venta diario por jugador (configurable) para prevenir manipulación.
   API específica para integración con tiendas de otros plugins o tiendas personalizadas.
   3.2. Módulo 2 – Banco e Intereses (Banking System)
   Objetivo: Permitir a los jugadores administrar su economía personal de forma segura y avanzada.
   Funciones Clave:
   Cuentas bancarias individuales para cada jugador.
   Sistema de interés diario sobre los saldos (positivo/negativo).
   Intereses dinámicos que pueden ajustarse según la inflación del servidor (Módulo 6).
   Comisiones configurables por mantenimiento de cuenta o transacciones.
   Transacciones seguras entre jugadores (transferencias).
   Medidas de protección contra explotación y abuso del sistema bancario.
   3.3. Módulo 3 – Préstamos y Deudas (Loan System)
   Objetivo: Ofrecer un sistema de préstamos con condiciones claras y consecuencias económicas reales por impago.
   Funciones Clave:
   Solicitud de préstamos al sistema (banco del servidor) con tasas de interés configurables.
   Sistema de cobro automático diario de las cuotas del préstamo.
   Penalizaciones progresivas por impago (confiscación de balance, limitaciones de comercio, embargo de bienes – esto último a definir).
   Score de crédito persistente por jugador, afectando condiciones de futuros préstamos.
   Préstamos entre jugadores (P2P) con posibilidad de aval y gestión de riesgo.
   3.4. Módulo 4 – Eventos Económicos (Economic Events Engine)
   Objetivo: Dinamizar la economía del servidor mediante eventos aleatorios o reactivos a condiciones específicas.
   Funciones Clave:
   Eventos predefinidos y personalizables: crisis financieras, inflación súbita, escasez de recursos, boom económico, etc.
   Activación automática de eventos basada en condiciones del servidor (ej. farmeo excesivo de un ítem, inflación general alta, baja actividad comercial).
   Los eventos afectarán temporalmente precios del mercado, tasas de impuestos, condiciones de préstamos, etc.
   Anuncios automáticos a los jugadores sobre el inicio, desarrollo y fin de los eventos.
   Duración limitada y configurable para cada evento.
   3.5. Módulo 5 – Especialidades o Roles Económicos (Economic Professions)
   Objetivo: Fomentar la interdependencia económica entre jugadores y crear nichos de especialización.
   Funciones Clave:
   Roles económicos únicos (minero, agricultor, banquero, comerciante, artesano, etc.).
   Cada rol con beneficios específicos (ej. mejores precios de venta para sus productos, acceso a recetas especiales) y posibles restricciones (ej. no poder realizar ciertas acciones).
   Cambio de rol limitado (cooldown, coste económico, posible pérdida de progreso en el rol anterior).
   Bonificaciones únicas y restricciones comerciales basadas en el rol.
   (Opcional) Sistema de progresión o niveles dentro de cada rol.
   3.6. Módulo 6 – Control de Inflación (Inflation & Anti-Exploitation Engine)
   Objetivo: Estabilizar la economía del servidor y contrarrestar comportamientos abusivos o de explotación.
   Funciones Clave:
   Monitoreo constante del balance global de dinero en el servidor y por jugador.
   Impuestos automáticos y progresivos basados en niveles de inflación detectados.
   Desactivación temporal (o ajuste drástico de precios) de ítems que estén siendo farmeados masivamente y desestabilizando la economía.
   Advertencias automáticas a jugadores y administradores sobre posibles explotaciones o desequilibrios.
   Integración con el sistema de Eventos Económicos (Módulo 4) para activar medidas correctivas.
   3.7. Módulo 7 – API Pública y Extensiones
   Objetivo: Permitir una alta extensibilidad e integración de EcoXpert con otros plugins y herramientas.
   Funciones Clave:
   API pública bien documentada (métodos como getBalance, getPlayerRole, triggerEconomicEvent, etc.).
   Soporte completo para PlaceholderAPI, permitiendo mostrar información de EcoXpert en otros plugins.
   Hooks específicos para plugins populares (Jobs, Towny, Lands, etc.) para una integración más profunda.
   Soporte para persistencia de datos en MySQL además de SQLite, para servidores más grandes o redes.
   (Fase Futura Premium Pro) Posibilidad de un Web Dashboard para visualizar estadísticas y gestionar aspectos de la economía.
4. Fases de Desarrollo del Proyecto
   4.1. Fase 0: Preparación y Núcleo del Plugin
   Estado: En curso / Prácticamente completado.
   Tareas:
   Configuración del entorno de desarrollo: IntelliJ IDEA, JDK, Maven.
   Creación de la estructura inicial del proyecto Maven (pom.xml).
   Definición del archivo descriptor plugin.yml.
   Implementación de la clase principal EcoXpert.java (extendiendo JavaPlugin).
   Configuración e inicialización de bStats.
   Integración con Vault: EcoXpert se registra como proveedor Economy. Creación de VaultEconomyProvider.java (esqueleto inicial).
   Diseño y placeholders para gestores centrales: ConfigManager, DataManager (con SQLite como opción inicial).
   Diseño conceptual del Notificador de Actualizaciones y del sistema de Actualización de Configs.
   4.2. Fases Globales Modulares (basadas en el Roadmap Técnico)
   Fase 1: Mercado Dinámico Funcional (Implementación del Módulo 1)

[M1-F1] Desarrollo del motor de precios: almacenamiento de historial de precios, logs de transacciones.
[M1-F2] Implementación del algoritmo de cálculo automático de precios en tiempo real (basado en oferta/demanda).
[M1-F3] Adición de soporte para categorías de ítems y configuraciones de precios base/mínimos/máximos.
[M1-F4] Creación de una interfaz (comando/GUI) para visualizar precios actuales y tendencias del mercado.
(+) Desarrollo de la API para integración con tiendas externas.
Fase 2: Banco e Intereses (Implementación del Módulo 2)

[M2-F1] Desarrollo del sistema base de cuentas bancarias, integrado con VaultEconomyProvider para reflejar saldos.
[M2-F2] Implementación del cálculo de intereses (positivos/negativos) y su vinculación con la inflación (M6).
[M2-F3] Creación de comisiones configurables (mantenimiento, transacciones).
[M2-F4] Diseño e implementación de una GUI bancaria intuitiva.
[M2-F5] Integración con PlaceholderAPI para mostrar saldos y otra información bancaria.
(+) Implementación de transferencias seguras entre jugadores.
(+) Adición de medidas de protección contra explotación.
Fase 3: Préstamos Seguros con Penalización (Implementación del Módulo 3)

[M3-F1] Desarrollo del sistema de préstamos desde el servidor/banco.
[M3-F2] Creación de un registro detallado de deudas y planes de pago.
[M3-F3] Implementación de penalizaciones configurables por impago.
[M3-F4] Diseño y cálculo del score crediticio del jugador.
[M3-F5] Desarrollo del sistema de préstamos entre jugadores (P2P).
Fase 4: Eventos Automáticos y Configurables (Implementación del Módulo 4)

[M4-F1] Creación del sistema de activación de eventos por condiciones.
[M4-F2] Implementación de los efectos de los eventos sobre la economía.
[M4-F3] Diseño de la estructura de configuración en YAML para eventos.
[M4-F4] Sistema de anuncios con variables y placeholders.
[M4-F5] Interfaz (comando/GUI) para visualizar el evento activo.
Fase 5: Sistema de Roles Económicos Funcionales (Implementación del Módulo 5)

[M5-F1] Desarrollo del sistema base para la gestión de roles.
[M5-F2] Implementación de bonificaciones y restricciones específicas por rol.
[M5-F3] Sistema para el cambio de rol, incluyendo penalizaciones.
[M5-F4] Creación de una GUI para la selección y visualización de roles.
[M5-F5] (Opcional) Implementación de progresión y niveles por rol.
Fase 6: Motor de Control de Inflación (Implementación del Módulo 6)

[M6-F1] Desarrollo del sistema de registro de riqueza y estadísticas económicas.
[M6-F2] Definición de umbrales configurables para la detección de inflación/deflación.
[M6-F3] Implementación de auto-impuestos y otros sumideros de dinero.
[M6-F4] Sistema de bloqueo o ajuste de ítems por explotación.
[M6-F5] Mecanismo de alerta a administradores.
Fase 7: API Pública + Placeholders y Extensiones (Implementación del Módulo 7)

[M7-F1] Diseño y desarrollo de la API pública base.
[M7-F2] Creación de documentación exhaustiva para la API.
[M7-F3] Desarrollo de placeholders personalizables y profundización de la integración con PlaceholderAPI.
(+) Implementación de hooks para plugins populares (Jobs, Towny, Lands).
(+) Adición de soporte formal para MySQL.
[M7-F4] (Consideración futura) Planificación para un Web dashboard.
5. Estructura General de Archivos y Carpetas del Proyecto (Propuesta Inicial)
   Plaintext

EcoXpert/
├── pom.xml                      # Archivo de configuración de Maven
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecoXpert/      # Paquete raíz (GroupId)
│   │   │       ├── EcoXpert.java          # Clase principal del plugin
│   │   │       ├── api/                   # API pública del plugin
│   │   │       │   ├── EcoXpertAPI.java     # Interfaz principal de la API
│   │   │       │   └── events/            # Eventos personalizados de la API (CustomBukkitEvents)
│   │   │       ├── core/                  # Componentes centrales y utilidades
│   │   │       │   ├── ConfigManager.java   # Gestor de configuraciones (config.yml, messages.yml, etc.)
│   │   │       │   ├── ConfigUpdater.java   # Lógica para actualizar archivos de configuración
│   │   │       │   ├── DataManager.java     # Interfaz/Clase abstracta para persistencia de datos
│   │   │       │   ├── persistence/       # Implementaciones de DataManager
│   │   │       │   │   ├── SQLiteManager.java
│   │   │       │   │   └── MySQLManager.java # (Futuro)
│   │   │       │   ├── UpdateChecker.java   # Verificador de actualizaciones del plugin
│   │   │       │   └── utils/               # Clases de utilidad (formateo, paginación, etc.)
│   │   │       ├── economy/               # Lógica de la economía base y Vault
│   │   │       │   ├── VaultEconomyProvider.java # Implementación de la API de Vault
│   │   │       │   └── EconomyManager.java  # Gestión interna de saldos, transacciones (base para M2)
│   │   │       ├── modules/               # Directorio raíz para todos los módulos funcionales
│   │   │       │   ├── market/            # Módulo 1: Mercado Dinámico
│   │   │       │   │   ├── MarketManager.java
│   │   │       │   │   ├── PriceEngine.java
│   │   │       │   │   ├── ItemData.java    # Representación de ítems en el mercado
│   │   │       │   │   ├── commands/
│   │   │       │   │   ├── listeners/
│   │   │       │   │   └── gui/
│   │   │       │   ├── bank/              # Módulo 2: Banco e Intereses
│   │   │       │   │   ├── BankManager.java
│   │   │       │   │   ├── PlayerAccount.java
│   │   │       │   │   ├── InterestService.java
│   │   │       │   │   ├── commands/
│   │   │       │   │   ├── listeners/
│   │   │       │   │   └── gui/
│   │   │       │   ├── loans/             # Módulo 3: Préstamos y Deudas
│   │   │       │   │   ├── LoanManager.java
│   │   │       │   │   ├── CreditScoreService.java
│   │   │       │   │   ├── commands/
│   │   │       │   │   └── listeners/
│   │   │       │   ├── economic_events/   # Módulo 4: Eventos Económicos
│   │   │       │   │   ├── EventEngine.java
│   │   │       │   │   ├── type/            # Tipos de eventos (Crisis, Boom, etc.)
│   │   │       │   │   ├── conditions/
│   │   │       │   │   └── effects/
│   │   │       │   ├── professions/       # Módulo 5: Especialidades Económicas
│   │   │       │   │   ├── ProfessionManager.java
│   │   │       │   │   ├── Role.java
│   │   │       │   │   ├── commands/
│   │   │       │   │   └── gui/
│   │   │       │   ├── inflation_control/ # Módulo 6: Control de Inflación
│   │   │       │   │   ├── InflationMonitor.java
│   │   │       │   │   └── TaxEngine.java
│   │   │       │   └── integrations/      # Módulo 7 (parcial): Hooks e integraciones específicas
│   │   │       │       └── PlaceholderProvider.java # Para PlaceholderAPI
│   │   │       └── lib/                   # Bibliotecas incluidas (ej. bStats)
│   │   │           └── bstats/            # bStats reubicado por Maven Shade
│   │   └── resources/
│   │       ├── plugin.yml                 # Descriptor del plugin
│   │       ├── config.yml                 # Configuración principal global
│   │       ├── messages_en.yml            # Mensajes por defecto en inglés
│   │       ├── messages_es.yml            # Ejemplo para localización en español
│   │       ├── modules/                   # Configuraciones específicas por módulo
│   │       │   ├── market.yml
│   │       │   ├── bank.yml
│   │       │   └── (otros archivos de config por módulo)
│   ├── test/                        # Directorio para pruebas unitarias (JUnit)
│       └── java/
│           └── com/ecoXpert/
├── target/                        # Carpeta de salida de la compilación (contiene el .jar)
├── README.md                      # Información del proyecto, instrucciones de compilación/instalación
└──LICENSE                          # Archivo de licencia (importante para un plugin premium)
Este documento debería servir como una referencia centralizada. Podemos actualizarlo a medida que el proyecto evolucione y se tomen nuevas decisiones.