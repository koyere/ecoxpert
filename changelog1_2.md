# EcoXpert 1.2 — MySQL Reliability Update

- **Added:** full MySQL configuration support driven by `config.yml`, including optional pool tuning.
- **Fixed:** removed hardcoded credentials to stop "Access denied" errors during MySQL startup.
- **Improved:** MySQL connections now enforce `utf8mb4` and validate parameters before building the pool for safer boot.
