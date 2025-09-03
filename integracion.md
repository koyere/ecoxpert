Aquí un breve resumen de cómo integraremos cada punto:

Compatibilidad (1.16.5-1.21.5): Esto es crucial. Nos enfocaremos en usar la API de Spigot de manera que funcione en este rango. Si necesitamos funciones específicas de versiones más nuevas, discutiremos cómo implementarlas manteniendo la compatibilidad hacia atrás, posiblemente usando capas de abstracción o chequeos de versión. Evitaremos el uso de NMS (código interno del servidor) tanto como sea posible para maximizar la compatibilidad.
API de Spigot: Será nuestra base principal.
Compatibilidad con Forks (Paper, etc.): Diseñaremos con las mejores prácticas en mente, lo que generalmente asegura una buena compatibilidad. Si hay oportunidades de usar APIs de Paper para mejorar el rendimiento, lo consideraremos, pero siempre con un fallback para Spigot o asegurándonos de que no rompa la compatibilidad.
API de Integración: Perfecto. Diseñaremos cada módulo con una interfaz clara (API) desde el principio. Esto no solo facilitará la integración con otros plugins (Módulo 7), sino que también mejorará la estructura interna del propio EcoXpert.
Diseño Profesional (Inglés, Optimizado, Comentado): De acuerdo. Me esforzaré por ofrecerte estructuras de código y ejemplos que sigan estas pautas: código limpio, bien documentado (en inglés), modular y optimizado, prestando atención a la gestión de memoria y el rendimiento.
Modularidad (Archivos/Carpetas): Seguiremos una estructura de paquetes clara (com.ecoXpert.market, com.ecoXpert.bank, etc.) y organizaremos los recursos y configuraciones de forma lógica y escalable.
bStats: Incluiremos la clase bStats en el proyecto y la inicializaremos en el método onEnable para obtener estadísticas de uso valiosas.
Notificador de Actualizaciones: Implementaremos una función (configurable, para que los admins puedan desactivarla si lo desean) que revise si hay una nueva versión del plugin disponible (probablemente consultando la API de SpigotMC o una fuente que designes) y lo notifique en la consola al iniciar el servidor.
Actualización Automática de Configs: Implementaremos un sistema para manejar las actualizaciones de config.yml y messages.yml. Generalmente, esto implica verificar una versión en el archivo, respaldar el archivo antiguo y generar uno nuevo con las claves actualizadas, preservando los valores antiguos siempre que sea posible. Es un punto delicado que trataremos con cuidado.
Sugerencias: ¡Excelente! Si veo alguna oportunidad de mejora o una alternativa interesante, te la presentaré para que podamos discutirla y decidir juntos el mejor camino a seguir.

---

Notas de Integración Operativa (2025-09-03)

- Vault & Migración:
  - Al habilitar el plugin, si EssentialsX/CMI están presentes y `economy.migration.import_on_startup: true`, EcoXpert difiere ~3s la importación y crea un backup SQLite en `plugins/EcoXpert/backups/` si `backup_before_import: true`.
  - Luego importa saldos desde el provider activo de Vault y registra su propio provider.

- Eventos Dinámicos:
  - Configuración en `modules/events.yml`; nuevas secciones para `investment_opportunity`, `market_correction`, `technological_breakthrough` con `duration_minutes` e intensidades.
  - `cooldown_hours` por evento (fallback global).
  - Mensajes i18n (EN/ES) para banners y anuncios.
  - Pesos `weight` por evento para sesgo de selección.

- Traducciones:
  - Archivos creados automáticamente en `plugins/EcoXpert/languages/` si no existen.
  - Clave `prefix` se antepone a mensajes enviados a jugador/broadcast según contexto.

- Compatibilidad:
  - Evitamos NMS; integración vía Spigot API. Con GeyserMC detectado, se informa soporte Bedrock.

- Préstamos:
  - Config: `modules/loans.yml` (tasa, plazo, límites, pagos, penalizaciones, scheduler).
  - Scheduler de morosidad: marca PENDING vencidas como LATE, aplica penalización y notifica.
