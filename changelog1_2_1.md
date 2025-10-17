# EcoXpert 1.2.1 — MySQL Schema Finalization

- **Fixed:** schema creation now generates valid MySQL DDL (AUTO_INCREMENT, booleans, compatible defaults) and uses dialect-specific upsert logic to avoid syntax errors during startup.
- **Improved:** database initialization chooses the correct SQL dialect (SQLite/MySQL/H2), verifies index existence via metadata, and handles meta-version updates without driver-specific queries.
- **Fixed:** MySQL charset compatibility — added automatic fallback from utf8mb4 to utf8 if server doesn't support utf8mb4. Database is now auto-created if it doesn't exist.
- **Improved:** config.yml now includes MySQL troubleshooting guide with solutions for charset, timeout, and access errors.
