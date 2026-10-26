# Work mode handoff — practice.logger, first development session

**Use this prompt with the starter files in the existing repository.**

## Mission

Initialize and start building **practice.logger**, a local-first Android practice app, inside the user's already created repository:

**GitHub:** https://github.com/mrayover/practice.logger  
**Preferred GitHub Desktop checkout:** `C:\Users\mrove\Projects\practice.logger` (verify; do not assume Work has access to the owner's Windows drive)

The repository was reported empty at planning time. **Inspect before changing anything. Never discard existing commits or replace it with a new repository.** The user uses GitHub Desktop for Git operations.

## Before coding

1. Confirm repository permissions and actual branch/contents, and identify whether you are working in the owner's Windows checkout or your own cloud clone.
2. Ensure these root files are present: `README.md`, `AGENTS.md`, `BUILD_PLAN.md`, `CURRENT_STATE.md`, `WORK_LOG.md`, `FIRST_WORK_HANDOFF.md`. Import them if absent; read `AGENTS.md`, `CURRENT_STATE.md`, `WORK_LOG.md` and `BUILD_PLAN.md` completely before implementation.
3. Preserve all work already committed, even if the planning documents describe the repository as empty.
4. Treat the first-release plan as authoritative. The full V1 specification is longer-term reference, not authorization to build every feature.

## Implementation target for this session

**First complete Stage 0**: initialize a clean, minimal Kotlin Android project; use Jetpack Compose and Room unless a better proven reason emerges; configure locally bundled **Lato Regular and Bold** as the application-wide typeface; retain licensing information; establish Git ignore/Gradle wrapper/testing and GitHub Actions CI that actually builds and uploads an installable **debug APK artifact**. Avoid a decorative but nonfunctional navigation shell.

**Then begin Stage 1 only if Stage 0 passes**: implement Home → Block Library → Block Editor with functional create/edit/search/archive and durable local persistence. Keep all visible controls working. Do not rush beyond what can be tested.

Do not implement Goals, saved Routines, metronome, tuner, recording, overdubs, PDFs, advanced statistics or other future-V1 features. Do not invent an elaborate visual style in place of unavailable reference drawings. Use simple accessible provisional layout; record what needs owner visual approval.

## Non-negotiable workflow

- Work in small, verifiable slices. Leave the project compiling.
- Confirm Gradle build/test result and GitHub Actions status. Provide actual run/artifact URL if available; never invent an APK link or say you've performed a phone test without evidence.
- **Before the session ends, append a complete factual entry to `WORK_LOG.md` and update `CURRENT_STATE.md`.** This applies even if Stage 1 is only partly completed.
- Commit all intended code/docs changes and push to GitHub when authorized; verify branch/commit remotely. If you cannot push, clearly label **NOT PUSHED** and provide a precise recovery path.
- Any future Work mode instance must do the same startup and shutdown process using `AGENTS.md`, `WORK_LOG.md`, and `CURRENT_STATE.md`; do not depend on AI conversation memory.
- Do not say the local Windows clone was updated unless you actually modified it. If you worked in a cloud clone, explicitly instruct: **"Open GitHub Desktop → Fetch origin → Pull origin"** after the remote work lands.

## Deliverables to report

- Exactly what is functional; what is not.
- Stage achieved and checklists passed/failed.
- Tests/build commands and outcomes.
- GitHub Actions APK artifact URL if produced.
- Commit SHA, branch, push/PR status, and what the owner must do in GitHub Desktop.
- Short manual test checklist for the user's Android phone.
- A clear, single next step from `CURRENT_STATE.md`.

**Priority:** Give the user a real, small practice tool—first working Block catalog, then usable practice flow—not a grand but unfinished application.
