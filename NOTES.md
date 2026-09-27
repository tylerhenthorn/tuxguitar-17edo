# EDO fret labels fork: working notes

## Phase 0: setup

- OS: Linux (Manjaro, x86_64). Build profile: `desktop/build-scripts/tuxguitar-linux-swt`.
- Upstream: https://github.com/helge17/tuxguitar (this repo is a fork of it).
- Base: release tag **`2.1.0`** (commit `2c46e2a1cc`, 2026-07-20). Branch `edo-labels` created from it.
- Toolchain found on the machine: OpenJDK 26.0.2.1, Maven 3.9.16, GCC 16.2, fluidsynth 2.6, pipewire-jack, lilv, suil, alsa headers.
- No `CONTRIBUTING.md` at repo root; it lives at `docs/CONTRIBUTING.md`.

### SWT install (one time)

```sh
curl -sSL -o swt.zip https://archive.eclipse.org/eclipse/downloads/drops4/R-4.37-202509050730/swt-4.37-gtk-linux-x86_64.zip
mkdir swt && cd swt && unzip ../swt.zip
mvn install:install-file -Dfile=swt.jar -DgroupId=org.eclipse.swt -DartifactId=org.eclipse.swt.gtk.linux -Dpackaging=jar -Dversion=4.37
```

### Build

```sh
cd desktop/build-scripts/tuxguitar-linux-swt
mvn -e clean verify -P native-modules        # add -DskipTests to skip unit tests
```

Result lands in `desktop/build-scripts/tuxguitar-linux-swt/target/tuxguitar-9.99-SNAPSHOT-linux-swt/`.

### Run

```sh
cd desktop/build-scripts/tuxguitar-linux-swt/target/tuxguitar-9.99-SNAPSHOT-linux-swt
./tuxguitar.sh [file.tg]
```

Gotcha: TuxGuitar is a singleton. It keeps `/tmp/tuxguitar-$USER/tuxguitar.lock` (5 s timeout) and hands a file
passed on the command line to the running instance. After killing an instance, wait 6 s or delete the lock
before launching again, or the new process just forwards the URL and exits silently.

### Run only the lib unit tests (fast)

```sh
cd common/TuxGuitar-lib && mvn -q test
```

Unmodified 2.1.0 built and launched fine before any change (all 63 upstream unit tests pass).

## Phase 1: where fret numbers become text

Everything in the tab staff goes through one class in `common/TuxGuitar-lib`:

| Location | What it does | Changed? |
|---|---|---|
| `graphics/control/TGNoteImpl.getNoteLabel(TGLayout, TGNote)` | builds the string painted in the tab: `L` for tied, `X` for dead, `(..)` for ghost, else the fret | **yes**, routed through the formatter |
| `TGNoteImpl.paintTablatureNoteValueTextMode` | font renderer (the one actually used: `tabNotePathRendererEnabled` is never set true anywhere in desktop/android) | uses the label width for margins already |
| `TGNoteImpl.paintTablatureNoteValuePathMode` + `painters/TGNumberPainter` | vector digit renderer (0-9 glyphs only) | **yes**, added `paintLabel`/`getLabelWidth` with a dot glyph |
| `TGNoteImpl.getEffectWidth` -> `TGVoiceImpl.getEffectWidth` -> `TGMeasureImpl` | the only hook where a note can ask for extra horizontal room (used by bends) | **yes**, adds room when a label is wider than its duration slot |
| `TGMeasureImpl.makeVoice/updateComponents` | beat spacing = `layout.getDurationWidth(duration)` + effect width; note text width is *not* considered upstream | no (uses the hook above) |
| `TGLayoutStyles` / `TGLayout.loadStyles` | style bag copied into the layout | **yes**, carries the formatter |
| `TGChordImpl.paintDiagram` | chord diagram: paints only the *first fret* number ("5fr" style) | **yes**, label from the formatter, spacing widened to fit |
| `desktop/.../dialog/chord/TGChordEditor` line ~180 | chord editor: paints the first-fret number | **yes**, label from config; MAX_FRET 24 -> 99 |
| `desktop/.../dialog/fretboard/TGFretBoard` | fretboard dots at (fret % 12) in {0,3,5,7,9}; fixed 24 frets; linear-ish spacing | **yes** (Phase 4 stretch goal done) |
| `desktop/.../printer/PrintLayoutStyles`, `TuxGuitar-pdf-ui/PDFLayoutStylesUI` | print / PDF reuse `TGNoteImpl`, only the style bag differs | **yes**, formatter set from config |
| `common/TuxGuitar-pdf/PDFLayoutStyles` | headless PDF export (no desktop config) | **yes**, reads `TGFretLabelFormatter.getInstance(context)` |
| `android/.../TGSongViewStyles` | Android style bag | no, inherits the disabled default |
| `desktop/TuxGuitar-svg/SVGSongWriter` | SVG export, own default `TGLayoutStyles` | **yes**, formatter from the context |
| `common/TuxGuitar-ascii/ASCIIOutputStream.drawNote` | ASCII tab export, prints `note.getValue()` itself | **yes**, formatter from the context |
| `desktop/TuxGuitar-image-swt/ImageExporterStream` | PNG/JPG export, uses `PrintController` -> `PrintLayoutStyles` | covered by the print change |
| `common/TuxGuitar-lilypond` | LilyPond writes pitches; LilyPond derives fret numbers itself | no (limitation) |

Other facts found:

- Numeric entry: `common/TuxGuitar-editor-utils/.../TGSetNoteFretNumberAction` already combines two digits typed within 1 s and clamps to `track.getMaxFret()`. Nothing to change for input.
- Max fret is **per track** and already stored in `.tg` files (`maxFret` attribute, default `TGTrack.DEFAULT_MAX_FRET = 29`). The only limit was the spinner in `TGTrackPropertiesDialog` (`MAX_MAXFRET_NUMBER = 39`).
- Settings: `desktop/.../dialog/settings/items/*Option.java`, registered in `TGSettingsEditor`; saving runs `TGReloadSettingsAction` -> `TGReloadStylesAction`, which rebuilds `TablatureStyles` from `TGConfigManager` and repaints. Config keys in `TGConfigKeys`, defaults in `TGConfigDefaults`, strings in `common/resources/lang/messages.properties`.
- Exporters without config access get the formatter from the application context: `TGFretLabelConfig.createFormatter` publishes it with `TGFretLabelFormatter.setInstance(context, ...)` every time the styles are (re)loaded, and `TGFretLabelFormatter.getInstance(context)` reads it (DISABLED when absent). Verified headless with a scratch `Export.java` driving the ASCII, SVG and PDF writers.
- Tuning presets: data file `common/resources/tunings/tunings.xml` (groups of `<tuning name notes="hi..lo"/>`), loaded by `TuningManager`. No code needed for a preset.
- There is no status bar in TuxGuitar 2.x; the caret is painted by `desktop/.../component/tab/Caret.paintCaret`.

## Phase 2: formatter

- `common/TuxGuitar-lib/.../graphics/control/TGFretLabelFormatter.java`: immutable (enabled, edo, dots). `format(int)`, `defaultDots(edo)`, `parseDots(String)`, `getDotsAsString()`. Duplicate frets keep the first entry and log a `java.util.logging` warning; malformed entries are skipped with a warning.
- `TGLayoutStyles.fretLabelFormatter` defaults to `TGFretLabelFormatter.DISABLED`, so every existing styles subclass (print, PDF, Android) behaves exactly as before unless it opts in.
- Layout width (Phase 2.4): no painter exists while `TGMeasureImpl` computes widths, so the exact text width is unknown. `TGLayout.getTabNoteLabelWidthEstimate` estimates `0.9 * noteFontHeight * fontScale` per character (half for `.`); `TGNoteImpl.getTabLabelExtraWidth` returns `max(0, estimate + 6*scale - durationWidth)` and feeds it into the existing effect-width mechanism. Returns 0 when the formatter is disabled, which keeps stock spacing bit-identical. The factor was tuned once against a screenshot of sixteenth notes labelled `24.4`.
- Tests: `common/TuxGuitar-lib/src/test/java/app/tuxguitar/graphics/control/TestFretLabelFormatter.java` (11 tests): frets 0-40 in 17-EDO, C major example, disabled pass-through, 12-EDO dot frets read as plain integers, custom list, duplicates, parsing, round trip, clamping (12..72).

## Phase 3: settings

- Keys `edo.labels.enabled`, `edo.labels.edo`, `edo.labels.dots` (`TGConfigKeys`), defaults `false`, `17`, `""` (`TGConfigDefaults`).
- `TGFretLabelConfig.createFormatter(TGConfigManager)` is the single place that turns config into a formatter; used by `TablatureStyles`, `PrintLayoutStyles`, `PDFLayoutStylesUI` and the fretboard.
- `EdoLabelsOption` page (Tools > Settings > "EDO labels"): enable toggle, EDO spinner, dot list, "Reset to formula", live preview of labels 0..EDO+2. On save the dot list is normalized (sorted, duplicates dropped) and stored; blank means formula.
- Live apply works through the existing reload path; verified by saving and re-capturing the tab.
- Per-track override: **not added**. It would need a new track attribute in the `.tg` format (and readers in every importer/exporter) or an abuse of the track name. Setting is global.

## Phase 4: editing

- Track > Properties max fret spinner now goes to 99 (`TGTrackPropertiesDialog.MAX_MAXFRET_NUMBER`). New tracks still default to 29; raise it per track (stored in the file, stock TuxGuitar reads it fine).
- Numeric entry unchanged; typing `2` then `1` within a second gives fret 21 if the track's max fret allows it.
- Caret readout: `Caret.paintFretStatus` draws `"<raw> = <label>"` to the right of the caret box (only when labels are enabled and a note is selected).
- Fretboard (stretch goal, done): `TGFretBoard.getFretCount()` = `min(2*EDO, 48)` when enabled, else 24; `initFrets` uses `position = L - L / 2^(n/EDO)` scaled so the last fret sits at the right edge; `paintFretPoints` uses the formatter's dot frets, double dots at multiples of the EDO. Hit-testing (`getFretIndex`) already works off the `frets[]` array, so clicks map correctly.

## Phase 5: playback

- Presets in `tunings.xml` (group "Guitar / 17-EDO 6-String"), values are 17-EDO steps counted from A4 = key 69: E standard `28,35,42,49,55,62` and D standard `25,32,39,46,52,59` (both 7-7-7-6-7 steps; D standard = E standard minus 3 steps). G-B = 6 steps (423 c) confirmed by the user on 2026-09-26; the 5-step neutral-third variant was dropped. History: the first preset was `40..74` with reference key 40 (E2); the user then asked for A = 440 Hz as concert pitch, which is impossible with an E anchor (a 17-EDO fourth is 494 c, so the A string sat at 109.6 Hz), hence the move to reference key 69 on 2026-09-26. Both presets re-verified acoustically through the Java Sound port (`StringCheck69.java`, scratch): A strings at 110.06 / 220.12 Hz, all strings on integer steps from A4.
- Synth setup is documented in `README-EDO.md` (Scala `.scl`/`.kbm` or MTS-ESP) for external synths.
- **Built-in synth retune (added later at the user's request):** `MidiTuning` (lib, `player.base`) builds a MIDI Tuning Standard bulk dump (`F0 7E 7F 08 01 tt name[16] 128x(xx yy zz) checksum F7`) mapping key k to 12-EDO pitch `ref + (k - ref) * 12 / EDO`; published on the context by `TGFretLabelConfig` (keys `edo.tuning.enabled`, `edo.tuning.reference-key`, default 40). `MidiPlayer.updateTuning()` (called at the end of `updateChannels()`) sends it through a new `MidiSynthesizer.sendSysex` default method, then RPN 3 = program 0 on every non-percussion channel and re-selects RPN 0. Plumbing: `GMReceiver/GMReceiverProxy/GMSynthesizer` (gm-utils), jsa `MidiReceiverImpl` (javax `SysexMessage`), fluidsynth `MidiSynth.sysex` JNI -> `fluid_synth_sysex` (payload without F0/F7), TuxGuitar-synth `TGMidiProcessor.sendSysex` -> `GervillProcessor`. Other ports keep the no-op default. Verified by rendering audio from the bundled Gervill (`PitchCheck.java`, scratch): key 57 = 164.9 Hz, key 47 = 109.7 Hz, key 40 = 82.4 Hz, standard tuning unchanged.
- Open-string intervals of the preset verified acoustically through the port (`StringCheck.java`, scratch): 82.45, 109.70, 145.91, 194.11, 247.89, 329.81 Hz = 7, 7, 7, 6, 7 steps of 17-EDO. The tuning dialogs (`TGTrackTuningDialog`, `TGTrackTuningChooserDialog`) name strings through `app.tuxguitar.app.util.TGEdoNoteNames`: stock names with the retune off, otherwise "E2 +7 steps ≈ A2 -6c" (short form "~A"), so the 17-EDO preset no longer reads as D5 G4 C#4 F#3 B2 E2.
- Gotcha found on first live test: the Java Sound plugin has two port classes. `MidiPortOut` (external MIDI devices, `Receiver`) and `MidiPortSynthesizer` (javax `Synthesizer` devices, drives channels through the `MidiChannel` API). The "Gervill" port is the latter, and it needs `Synthesizer.getReceiver()` for sysex; `MidiPortSynthesizerReceiver.sendSysex` now does that (receiver cached). Inside TuxGuitar "Gervill" is the bundled `media.sound.SoftSynthesizer` from `gervill.jar` (it registers as a Java Sound provider), i.e. the exact code `PitchCheck` exercised. Re-verified end to end through `MidiPortSynthesizer` (`PortPitchCheck.java`, scratch): same pitches.

## Phase 6: verification (done on 2026-09-26)

- Full build with tests: `BUILD SUCCESS`, 73 unit tests, 0 failures (63 upstream + 11 new, minus 1 upstream test counted per class).
- Test song generated by `gen_song.py` (scratch) as a hand-written `.tg` (zip of `version.txt` + `content.xml`; note the first beat must start at precise time 2882880). Tuning `74,67,61,54,47,40`, max fret 40, measure 1 = the C major example, measure 2 = eighths at 17,18,20,34,35,12,13,1, measure 3 = sixteenths at frets 20-35 / 30-45 / 38.
- Screen: measure 1 shows `0 / 0.1 / 0 / 0.2 / 3` exactly as in the plan; caret shows `0 = 0`. Screenshots in `docs/images/edo/`.
- Dense spacing: sixteenths labelled `24.4` do not overlap after the width estimate was raised to 0.9 (they touched at 0.8).
- Stock comparison: with labels disabled, the fork and a fresh stock 2.1.0 build (built in a git worktree) rendering the same `.gp5` gave **0 differing pixels** over the whole window (ImageMagick `compare -metric AE`).
- Round trip: a `.tg` written by the fork's `TGSongWriterImpl` contains `value="0"`, `value="1"`, ... plain integers, no dotted values; stock 2.1.0 opens it and shows `0 1 0 2 4`. The fork changes no file under `io/`.
- Settings page: opens, previews, "Reset to formula" fills the list, OK repaints the open tab.

## Phase 7: packaging

- Commits on `edo-labels`, in order: formatter + tests; painter integration; settings page + config; max fret; caret readout; fretboard; tuning presets; docs; patches.
- The fork is one commit on top of tag `2.1.0` (branch `17edo`); the `patches/` series was removed. To re-apply on a newer upstream: `git checkout -b 17edo-new <tag> && git cherry-pick 17edo`.
- `README-EDO.md` is the user-facing document.

## Phase 8: arbitrary EDO tunings (branch `edo-tuning-ui`, 2026-09-26)

- Problem: string values must be EDO steps, and only two hand-made 17-EDO presets existed. Decision: no per-EDO presets; Track > Tuning converts stock 12-EDO presets.
- Semantics change in `MidiTuning`: the reference key is now **the MIDI key that plays A4 = 440 Hz** (`getPitch(k) = 69 + (k - ref) * 12 / edo`), no longer "the key that keeps its 12-EDO pitch". Identical for ref 69 (all existing 17-EDO tracks unchanged), but for other refs A4 stays exact. Conversion `toStepValue(p) = ref + round((p - 69) * edo / 12)` rounds relative to A4, so A strings are exact and intervals do not depend on the ref.
- Range: each unit of ref moves every step by one key, so low strings need ref high enough (`minimumReferenceKeyFor(lowest)`), high strings plus the track's max fret need it low enough (`maximumReferenceKeyFor(highest, maxFret)`). 31-EDO guitar: 75..100 with 40 frets. 53-EDO and up: a guitar never fits 128 keys. The tuning dialog (`warnIfPresetOutOfRange`) shows the feasible range (`tuning.edo.range-error`) or says it is impossible (`tuning.edo.range-impossible`) when a picked preset clamps.
- Dialog (`TGTrackTuningDialog`): checkbox `edoConvert` (Options panel, only with retune on, default on), hint label, `presetValues()` converts built-in presets (never custom ones: they are stored raw), used for the table, preset labels and preset matching (also fixed the `findTuningInGroup` recursion that passed `this.tuning` instead of the parameter); `-1 step`/`+1 step` buttons (`nudgeString`). Chooser (`TGTrackTuningChooserDialog`): in EDO mode the list has 12-EDO names mapped to converted steps (deduplicated), the spinner edits the raw step (0..127), OK takes the last edited value. `TGTrackPropertiesDialog` tuning summary goes through `TGEdoNoteNames`. `tunings.xml` is back to stock.
- Verified: `TestMidiTuning` (10 tests: presets, other EDOs, round trip, range helpers); headless audio through `MidiPortSynthesizer` (`ConvertCheck2.java`, scratch): 19-EDO E standard at ref 69 = 8-8-8-6-8 steps with A2 = 110.06 Hz; 31-EDO at ref 80 = 13-13-13-10-13 steps, A2 = 110.06 Hz, E2 = 82.29 Hz. Feasible-range table (`Table.java`, scratch) is in README-EDO.md.
- Not automated: the dialog itself (checkbox, nudge buttons, chooser) was only compile-checked; see the manual checklist in the summary.
- Follow-up after the first live test: (1) the user's config still had `edo.tuning.reference-key=40` from the E2-anchored days, which under the A4 semantics put the low E below key 0. Fixed by making the A4 key automatic: `MidiTuning.defaultReferenceKey(edo)` = max(69, key needed for bass low E) capped by the key that keeps a 24-fret guitar under 127 (12..20 -> 69, 22 -> 75, 31 -> 106, 36 -> 118); config `edo.tuning.reference-key.auto` (default true) with the manual spinner as an override (settings page greys the spinner and shows the derived value). (2) The range warning was a modal error opened from inside the preset drop-down's selection event, which froze the SWT dialog; it is now an inline label (`edoWarning`) in the Options panel, with three messages: strings do not fit (names the manual key range), impossible at all, or only the frets overflow (names the playable fret count).

## Phase 9: 17-EDO branch (`17edo` off `edo-tuning-ui`, 2026-09-26)

- Defaults: `TGConfigDefaults` has `edo.labels.enabled` and `edo.tuning.enabled` = true (EDO 17 was already the default).
- Default tuning: `MidiTuning.convertDefaultTuning(TGTrack)` (strings to nearest step, max fret >= round(24 * edo / 12)); called from `TGNewSongAction`, `TGLoadTemplateAction` (the `.tg` templates stay stock 12-EDO files), `TGAddNewTrackAction`, `TGSetTrackStringCountAction` (only when the count changed) and `TGSetTrackChannelAction` (percussion -> melodic). Opened files are not touched. `TGSongManager.DEFAULT_TUNING_VALUES` is unchanged because the GTP/compat writers also use it.
- Sticky duration: `Caret.updateDuration` copies the beat's duration only when the voice has notes. Stock copied it always, so landing on the whole rest of a new measure reset the selection to whole.
- Move tool: `UIMouseEvent.isControlDown()` (SWT: `SWT.MOD1`; JFX keeps the old constructor = false). `MouseKit` enters move mode on Ctrl + button 1 with an active selection: drag runs `TGUpdateDragMoveAction` (caret follows, selection kept), release runs `TGMoveSelectionAction` (extends `TGPasteAction`, whose beat-pasting body was extracted to `pasteBeats`): store the selected beats, clear them (notes, chord, text), `replaceBeats` at the drop beat's precise start, reselect. Mapped `UNDOABLE_SONG_GENERIC`.
- Verified: full build + unit tests (`testConvertDefaultTuning` added); move logic run headless against the lib (`MoveCheck.java`, scratch): 4 sixteenths from measure 1 to measure 3, and an overlapping one-beat-left move, both correct. **Not verified in the GUI** (a user instance was running; mouse-driven test not run): Ctrl + drag gesture, caret feedback, sticky duration while typing.

- Del on rests / empty measures (same day): `TGDeleteNoteOrRestAction` removes the measures touched by the caret beat or the selection when they hold only rests in every track (no text, no chord), never the last remaining measure; sets `ATTRIBUTE_MEASURES_REMOVED`. Rests in measures that have notes keep the stock behaviour (`removeVoice(voice, true)` pulls the following beats left). Trailing rests cannot disappear: the measure must stay full, so they are refilled. Desktop mapping changed to `TGUpdateDeletedBeatsController` (song update, selection cleared, caret clamped to the last measure) and `UNDOABLE_SONG_GENERIC` (a beat-range undo cannot restore a removed measure). Tests: `TestDeleteRestOrMeasure` (5). Not verified in the GUI.

## Phase 10: 17-EDO only, Superpyth staff (2026-09-26)

- Decision: the fork supports 17-EDO and nothing else. README-EDO.md was rewritten first and the code was brought in line with it.
- Staff and names: `TGMusicKeyUtils` is 17-EDO. Key 56 = C4, 69 = A4, `pc = floorMod(k - 56, 17)`, naturals 0 3 6 7 10 13 16, sharp/flat = 2 steps (`ALTERATION_STEPS`). Two 17-entry tables give the default spelling (C Db C# D Eb D# E F Gb F# G Ab G# A Bb A# B). Four pitch classes have a second name (B#, Fb, E#, Cb), used when the key signature holds it or through `altEnharmonic`. Public signatures are unchanged, so `TGNotation`, `TGMeasureImpl.getNoteAccidental`, `TGFretBoard`, `TGMatrixEditor` and `TGPrintLayout` needed no change. `MIN_MIDI_NOTE` is 0 (G#0). `getKeySignature(TGScale)` keeps private 12-EDO tables (`scaleNoteIndex`), scales are still 12-EDO.
- Staff click (`EditorKit.fillAddOrRemoveBeat`): alterations add or subtract `ALTERATION_STEPS` instead of 1.
- Fixed tuning: `MidiTuning` has no fields (constants `EDO`, `A4_KEY`), `getInstance` returns one shared instance, `MidiPlayer.updateTuning` always sends the dump. `TGFretLabelFormatter` takes the dot list only (`EDO` constant, `DEFAULT` instance). Removed: enable flags, EDO parameter, reference key and its helpers, the 12-EDO fallback dump. Config keeps `edo.labels.dots` only; the settings page is the dot list with preview.
- Tuning dialogs: stock presets always converted, custom presets raw, step buttons always present, warning only when the highest string plus the frets pass key 127. `TGEdoNoteNames` returns Superpyth names. ASCII export string names come from `TGMusicKeyUtils`. Transpose dialog range is +-17 steps.
- Hidden: chord editor entry (Chord menu, edit toolbar, main toolbar map), scale tool (Tools menu, fretboard button and overlay), piano view (View menu, main toolbar map). The actions stay registered. Tuner, LilyPond and MusicXML plugins removed from every `desktop/build-scripts/tuxguitar-*/pom.xml` (modules and artifact items); sources untouched. A saved toolbar layout that names a removed item logs "toolBarItem name not found (ignored)".
- Android compiles the same lib sources: labels and staff are 17-EDO there as well, playback is not (no port implements `sendSysex`). Not built, not tested.
- Verified: lib tests (`TestMusicKeyUtils` rewritten, 12 tests; `TestMidiTuning` 5; `TestFretLabelFormatter` 9), full `tuxguitar-linux-swt` build with `-P native-modules`. **Not verified in the GUI**: staff rendering, key signatures, staff click entry, settings page, tuning dialogs, menus. `docs/images/edo/*.png` still show the old build.

