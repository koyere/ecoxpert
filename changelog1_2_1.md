# EcoXpert 1.2.1 — MySQL Schema Finalization

- **Fixed:** schema creation now generates valid MySQL DDL (AUTO_INCREMENT, booleans, compatible defaults) and uses dialect-specific upsert logic to avoid syntax errors during startup.
- **Improved:** database initialization chooses the correct SQL dialect (SQLite/MySQL/H2), verifies index existence via metadata, and handles meta-version updates without driver-specific queries.
