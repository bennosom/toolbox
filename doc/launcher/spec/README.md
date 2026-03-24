# spec/

This directory contains technical documentation for the Launcher:

- **ADRs (Architecture Decision Records)** — numbered `ADR-NNN-title.md` — capture significant
  architectural choices, the options considered, and the rationale for the decision made.
  Once recorded, ADRs are immutable; superseded decisions get a new ADR that references the old one.

- **Technical Design Docs** — free-form `DESIGN-NNN-title.md` — describe the intended design of
  a feature or subsystem before or during implementation. They are living documents and may be
  updated as the design evolves.

## Naming convention

| Type | Pattern | Example |
|------|---------|---------|
| ADR | `ADR-NNN-short-title.md` | `ADR-001-jetpack-datastore.md` |
| Design doc | `DESIGN-NNN-short-title.md` | `DESIGN-001-dependency-injection.md` |

## Index

| Document | Topic |
|----------|-------|
| [DESIGN-001](DESIGN-001-dependency-injection.md) | Dependency Injection |
| [DESIGN-002](DESIGN-002-architecture.md) | Architecture (MVI) |
| [DESIGN-003](DESIGN-003-file-size.md) | File Size Limit |
| [DESIGN-004](DESIGN-004-compose-conventions.md) | Compose Conventions |
| [DESIGN-005](DESIGN-005-code-quality.md) | Code Quality (SOLID) |
| [DESIGN-006](DESIGN-006-error-handling.md) | Error Handling |
| [DESIGN-007](DESIGN-007-logging.md) | Logging |
| [DESIGN-008](DESIGN-008-testing.md) | Testing |
| [DESIGN-009](DESIGN-009-cold-start.md) | Cold Start |
| [DESIGN-010](DESIGN-010-resource-efficiency.md) | Resource Efficiency |
| [DESIGN-011](DESIGN-011-ui-thread.md) | UI Thread |
| [DESIGN-012](DESIGN-012-progress-and-cancellation.md) | Progress & Cancellation |
