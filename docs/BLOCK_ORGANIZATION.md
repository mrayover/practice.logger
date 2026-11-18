# Block Organization — Category, Source and Library asset groups

The organization candidate started on `block-organization-stage1` from development commit `66665ec5c0dc208519e96db415d003ad1363dbca`. See [current status](../CURRENT_STATE.md) for integration and delivery evidence.

## Principles
- Block remains the one canonical practice object. Every screen uses its identity, timing, goals, recording and reward relationships.
- A Block has zero or one Category, zero or one primary Source, plus optional chapter/page/location text. These are independent from primary Goal and existing Notes.
- Categories are topical headings, not folders. Sources are named references (book, video, lesson, recording, website, original material, other).
- A Source can be associated with multiple Blocks and multiple canonical media assets (PDF, image, audio). A media asset can be associated with several Sources. Original assets and master audio markers remain stored once in the Media Library.
- No Source landing page in this stage; the Source metadata and media associations are durable so a landing page can be added later without changing identity.
- Search ignores collapsed groups: when query is nonempty, the matching items are displayed flat. Source names and Category names are part of Block search; Source names are part of Media Library search.
- Goal colors, Block pill expansion, This Week repetition indicators, ordered session/routine entries, timer, recording and Playground do not change.

## UI placement
- **Block Library:** All / Category / Source group selection and collapsible headers, existing Goal and archived filters, existing BlockPill actions, organization management entry point.
- **Edit Block:** optional Category, Source and source-location selectors; the existing Notes / Source text remains untouched.
- **Practice session builder / Prepare Week:** retain current search, Goal filters and recommendation selection, optionally group available Blocks. Assigned weeks and current practice lineups remain in their meaningful sequence.
- **Routine editor:** category/source filters are added to the existing search and Goal picker; saved routine order remains unchanged.
- **Media Library:** retains all category/type search, import, rename, preview, annotation, and delete behaviors. Optional grouping by Source shows every file in each associated Source or Unassigned, and a file's Sources action manages links. Source organization is accessible from the Library.
- **Other Block contexts:** shared BlockPill shows subdued organization labels wherever that component is used. Goal details and recording do not become nested file trees.

## Database and backup behavior
- Room 28→29 migration adds nullable indexed categoryId/sourceId and a sourceLocation text column (default empty) to Blocks.
- New `block_categories`, `study_sources`, `study_source_media` records; source-media links have FKs and cascading link removal but never delete media originals.
- Category and Source deletion removes the association and retains Blocks, takes, annotations, media and practice history.
- Portable ZIP records version 14 and Room schema 29 preserve these relationships. Existing 13 and earlier archives restore with absent associations. Historical JSON backups continue to exclude Library media and organization additions; use the portable ZIP for full fidelity.
- The existing database and media originals are not moved or rewritten by this migration.

## Deferred
- Source-specific landing pages, bibliography metadata, cover art, advanced tags, nesting, inferred recommendations, and automatically accepted recommendations.
- No expansion of unrelated practice, timer, recording or reward logic.

## Verification requirements
- Compile and run scoped organization, repository and media tests.
- Confirm Room 28→29 migration is accepted and the generated schema 29 is retained.
- Confirm portable ZIP 14 round-trip, old ZIP compatibility, and safe handling of missing references.
- Confirm navigation back from Library organization and editor cancellation; phone acceptance of grouped headers, search and associations.
