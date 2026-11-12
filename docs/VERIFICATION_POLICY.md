## Owner policy — development verification and F1 regression (2026-11-12)

During ordinary development, compile the changed application and run only focused checks for the assigned feature, critical data integrity, and signed delivery. Comprehensive cross-feature regression, device matrices, historical migration/backup compatibility campaigns, broad UI/accessibility suites and baseline lint investigations are deferred to F1. Retain all tests and verification infrastructure. Investigate failures directly affecting the assigned change; document unrelated failures for F1.

Comprehensive workflows are manual and require explicit owner authorization for each campaign. G3 completion does not authorize F1 or feature freeze. The owner decides when remaining features, including future check-ins/revisit suggestions, Playground and other approved or pending work, justify F1. This directive supersedes older per-stage broad-regression requirements, including the full-suite wording in the efficiency procedure; its other rules remain applicable.

All existing verification scripts and tests remain available for F1. No replacement automatic test matrix is introduced. Repository protection configuration must be inspected independently; do not change required statuses or protection rules as part of this policy.
