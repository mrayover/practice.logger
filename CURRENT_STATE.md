# Current state — practice.logger 0.15.0, build30 candidate

Updated October29,2026 (America/Los_Angeles). Increment3 PDF and recording-sheet references are implemented. Build30 is signed/installable and focused checks pass; full release and200% gates are in progress. Earlier candidates28/29 are retained and superseded.

## Delivery and provenance

- Signed APK source:`dce8b3241d8dfa59270fbb3fdee4054d29d56082`; version0.15.0/code30; `com.mrayover.practicelogger.app`,minAPI26/target36.
- [Signed APK](dist/practice.logger-0.15.0-b30-dce8b32/practice.logger-0.15.0-b30-dce8b32.apk),[ZIP](dist/practice.logger-0.15.0-b30-dce8b32.zip),[build info](dist/practice.logger-0.15.0-b30-dce8b32/BUILD_INFO.json),[verification status](dist/practice.logger-0.15.0-b30-dce8b32/VERIFICATION_STATUS.txt),[phone checklist](dist/practice.logger-0.15.0-b30-dce8b32/PHONE_CHECKLIST.txt),[latest pointer](dist/LATEST.txt).
- APK SHA256:`e9747a15e929505b98df76c235061b9ca1596218818df270265f376e01ccc559`;ZIP SHA256:`56b5de2f31ffba71f8247335ce16854cd662a026ac09a8081f826002fea2a4f3`.
- Permanent certificate:`f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- Publisher verified signature,certificate,package/version,checksum and ZIP integrity. All100 pre-session packages and candidates28/29 are preserved; source/tests/records remain in the original Windows checkout and GitHub. Dist/signing are local and ignored;synced sources untouched.

Install **over the permanent-key app without uninstalling or clearing data**. No physical-phone upgrade is claimed.

## Implemented behavior and data

One image OR PDF per saved Block; existing MIME/dimension columns in block_images preserve Room10 and old rows/files without migration. Stable archived ownership,safe validated private copy/replacement,confirmed removal,RESTRICT deletion protection and conservative cleanup are retained. Original picker sources are never modified; no persistent URI dependency.

PDFs:20MiB,1–100pages,1–14400-point page dimensions. Bounded standard startxref/EOF and table/stream preflight avoids the observed API26 renderer failure after truncated input. Encryption markers(including owner-encrypted PDFs accepted by API35)are rejected conservatively. Native PdfRenderer validates every page with one256px temporary bitmap and renders only the requested page at2048px on IO;all page/document/descriptor resources close. Show/Hide,page count,Previous/Next,aspect ratio,pinch/pan/reset are supported. Existing EXIF orientation,full safe-width Focused Practice,180–640dp bounds and image zoom/panning remain.

The existing recording sheet contains the same viewer during free/phrase capture/count-in/loop playback,with active status and Stop/Cancel above scrolling content. Viewer state never calls sheet leave/enter or recording/practice/metronome engines. Genuine backgrounding and deliberate dismissal retain existing foreground-only safety. Standalone Free recording has no Block reference.

Room10,migrations/schemas1–10 and JSON writer6/readers1–6 are retained. JSON excludes every image/PDF byte,attachment metadata/path/URI and take/audio metadata. Keep originals separately and export wanted takes individually;restore creates no broken media rows. Atomic empty-target restore and all other structured data remain intact. No new runtime dependency or permission;no multi-attachment,annotation/OCR,media-inclusive backup or background recording.

## Actual verification and current gate status

- [Build40949598879](https://github.com/mrayover/practice.logger/actions/runs/40949598879):81unit/Robolectric tests,0failures/errors/skips;debug/release compilation;lint0errors/13warnings;all compiler schemas1–10 byte-identical.
- [API26 focused40949603306](https://github.com/mrayover/practice.logger/actions/runs/40949603306),job123713913507:actual encrypted/corrupt import rejection with intact-original byte checks and immediate rendering;picker,page/zoom,practice/timer/BPM/notes,free/phrase recording continuity,playback,dismissal,archive and cold reopen pass.
- API35 test keyboard correction passed in[40948857925](https://github.com/mrayover/practice.logger/actions/runs/40948857925). Previous build29 enlarged PDF/image workflows passed both APIs;build30’s actual reruns are pending below.
- Full release:[40950300706](https://github.com/mrayover/practice.logger/actions/runs/40950300706),in progress.
-200% PDF/image recording/reopen:[API26 40950305578](https://github.com/mrayover/practice.logger/actions/runs/40950305578),[API35 40950309794](https://github.com/mrayover/practice.logger/actions/runs/40950309794),in progress.
- Prior full40946722042 passed actual build27 upgrade retention and3 real PDF storage tests on both APIs,but did not complete:API26 renderer state failed after corrupt replacement(bytes unchanged),API35 test restored the notes keyboard. Build30 preflight and explicit test focus handling address those observed failures. Earlier test/framework/setup failures and exact evidence remain in WORK_LOG;no full matrix pass claimed yet.

Normal and200% reference screenshots were reviewed;final build30 visual evidence will be reconciled after its gates. Deep unsupported PDF parse failures may require reopening the app before retrying;bytes/metadata are preserved. Physical-phone permanent-key update,owner’s original photo,vendor pickers,device landscape,TalkBack and acoustic/loop checks remain pending. Stage3 detailed acceptance pending;Stage4 remains0/10documented sessions.

## One next task

Complete the running build30 release/200% gates and reconcile the release record,then perform the Increment3 owner phone checklist in [MANUAL_TESTS.md](MANUAL_TESTS.md). Do not uninstall or clear data.
