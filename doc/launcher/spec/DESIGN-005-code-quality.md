# Code Quality

Apply the **SOLID principles**:

- *Single Responsibility* — each class/function has exactly one reason to change.
- *Open/Closed* — extend behaviour through new types or parameters, not by editing existing logic.
- *Liskov Substitution* — subtypes are substitutable for their base type without behavioural surprises.
- *Interface Segregation* — keep interfaces narrow; callers must not depend on methods they do not use.
- *Dependency Inversion* — high-level modules depend on abstractions, not concrete implementations.

Use **meaningful, fully spelled-out names** — no abbreviations, no single-letter variables outside
trivial loop indices. Names must reveal intent.

Do **not** treat the existing implementation as the source of truth. Refactor structure and quality
whenever a cleaner design is available; incremental improvement is expected, not optional.
