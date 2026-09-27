# TuxGuitar 17EDO

This fork of [TuxGuitar](https://github.com/helge17/tuxguitar) (based on release `2.1.0`) is a 17-EDO
tablature editor. 17 equal divisions of the octave is the only tuning it supports: there is no 12-EDO mode
and no other EDO to pick. Tablature, the score staff, the fretboard, playback, exports and the default
tunings are all 17-EDO.

- **Tablature** shows dot-relative fret labels (`0.1`, `3`, `12.2`, ...), so positions read like the ones on a
  12-EDO neck.
- **The score staff** uses 17-EDO Superpyth notation: the seven naturals plus five sharps and five flats name
  all 17 tones.
- **Playback** is tuned to 17-EDO, anchored to A4 = 440 Hz.
- **New songs, templates and new tracks** get a 17-EDO tuning (`62,55,49,42,35,28` for a 6-string) and 34 frets.

The song data stays a plain integer fret per note and a plain integer per string, so the file formats are
unchanged. Those integers are 17-EDO steps, not semitones.

## Install

### macOS (Apple Silicon)

1. Download `TuxGuitar-17EDO-<version>-macos-arm64.dmg` from the
   [releases page](https://github.com/tylerhenthorn/tuxguitar-17edo/releases).
2. Open the DMG and drag **TuxGuitar 17EDO** onto **Applications**.
3. The app is not signed or notarized with an Apple Developer ID, and it will not be. macOS refuses to open it
   ("damaged" or "cannot be verified") until the quarantine flag is removed. Do that once, in Terminal:

   ```sh
   xattr -cr "/Applications/TuxGuitar 17EDO.app"
   ```

   Alternative without Terminal: try to open the app, then go to System Settings > Privacy & Security and
   click **Open Anyway**.
4. Start **TuxGuitar 17EDO** from Applications.

Java is bundled in the app. Intel Macs are not covered by the DMG: build from source (below).

### Linux

There is no binary package for Linux: build from source (below). On Debian/Ubuntu the build gives a `.deb`
to install; on other distributions it gives a folder to run in place.

## Build from source

Prerequisites on every platform: JDK 17+, Maven 3.3+, git, and SWT 4.37 installed into the local Maven repo.
`INSTALL.md` has the upstream instructions for the other platforms (Windows, FreeBSD, Android); the fork
changes no platform-specific code.

```sh
git clone -b 17edo https://github.com/tylerhenthorn/tuxguitar-17edo.git
cd tuxguitar-17edo
```

### macOS

```sh
brew install openjdk@17 maven wget

# SWT, once
TUX_ARCH=`uname -m | sed 's/arm64/aarch64/'`
wget https://archive.eclipse.org/eclipse/downloads/drops4/R-4.37-202509050730/swt-4.37-cocoa-macosx-${TUX_ARCH}.zip
mkdir swt && cd swt && unzip ../swt-4.37-cocoa-macosx-${TUX_ARCH}.zip
mvn install:install-file -Dfile=swt.jar -DgroupId=org.eclipse.swt -DartifactId=org.eclipse.swt.cocoa.macosx -Dpackaging=jar -Dversion=4.37
cd ..

# build
cd desktop/build-scripts/tuxguitar-macosx-swt-cocoa
mvn -e clean verify -P native-modules            # -DskipTests to skip unit tests

# bundle a Java runtime: the app starts ./jre/bin/java and Maven does not add it
"$(brew --prefix openjdk@17)/bin/jlink" --add-modules java.desktop \
  --output target/tuxguitar-9.99-SNAPSHOT-macosx-swt-cocoa.app/Contents/MacOS/jre
```

The app is `target/tuxguitar-9.99-SNAPSHOT-macosx-swt-cocoa.app`. Copy it to `/Applications` under any name
(`cp -R target/tuxguitar-9.99-SNAPSHOT-macosx-swt-cocoa.app "/Applications/TuxGuitar 17EDO.app"`) or
double-click it where it is. An app built on the same Mac is not quarantined, so no `xattr` step is needed.

To make the DMG:

```sh
mkdir dmg && cp -R target/tuxguitar-9.99-SNAPSHOT-macosx-swt-cocoa.app "dmg/TuxGuitar 17EDO.app"
ln -s /Applications dmg/Applications
hdiutil create -volname "TuxGuitar 17EDO" -srcfolder dmg -format UDZO TuxGuitar-17EDO-macos-`uname -m`.dmg
```

### Linux

Prerequisites on Debian/Ubuntu:

```sh
sudo apt install wget unzip git build-essential default-jdk maven libwebkit2gtk-4.1-0 libfluidsynth-dev libjack-jackd2-dev libasound2-dev liblilv-dev libsuil-dev qtbase5-dev
```

On other distributions install the same with your package manager: JDK, Maven, GCC, and the dev headers for
fluidsynth, jack, alsa, lilv and suil.

SWT, once:

```sh
wget https://archive.eclipse.org/eclipse/downloads/drops4/R-4.37-202509050730/swt-4.37-gtk-linux-`uname -m`.zip
mkdir swt && cd swt && unzip ../swt-4.37-gtk-linux-`uname -m`.zip
mvn install:install-file -Dfile=swt.jar -DgroupId=org.eclipse.swt -DartifactId=org.eclipse.swt.gtk.linux -Dpackaging=jar -Dversion=4.37
cd ..
```

Debian/Ubuntu, installed as a package:

```sh
cd desktop/build-scripts/tuxguitar-linux-swt-deb
mvn -e clean verify -P native-modules
sudo dpkg -i target/tuxguitar-*.deb
tuxguitar
```

This package is named `tuxguitar`, so it replaces a stock TuxGuitar installed from a `.deb`.

Any distribution, run in place:

```sh
cd desktop/build-scripts/tuxguitar-linux-swt
mvn -e clean verify -P native-modules            # -DskipTests to skip unit tests
cd target/tuxguitar-9.99-SNAPSHOT-linux-swt
./tuxguitar.sh
```

The folder is self-contained apart from Java (`java` must be on the `PATH`): move it anywhere, such as
`~/opt/tuxguitar-17edo`, and start `tuxguitar.sh` from there.

### Unit tests

Run only the formatter and tuning unit tests:

```sh
cd common/TuxGuitar-lib && mvn -q test -Dtest='TestFretLabelFormatter,TestMidiTuning'
```

## The 17 tones (Superpyth notation)

17-EDO is notated by its chain of fifths, as in Pythagorean and Superpyth tuning. One step is 70.59 cents.

| Interval | Steps | Cents |
|---|---|---|
| Octave | 17 | 1200 |
| Perfect fifth | 10 | 705.88 |
| Perfect fourth | 7 | 494.12 |
| Whole tone (C-D) | 3 | 211.76 |
| Major third (C-E) | 6 | 423.53 |
| Minor third (D-F) | 4 | 282.35 |
| Diatonic semitone (E-F, B-C) | 1 | 70.59 |
| Sharp or flat (C-C#, D-Db) | 2 | 141.18 |

A sharp raises a note by 2 steps and a flat lowers it by 2 steps, while E-F and B-C are 1 step apart.
A sharp is therefore *higher* than the flat of the note above it: C# and Db are two different tones, one
step apart. Using both sharps and flats covers every step of the octave:

| Step | Name | Step | Name | Step | Name |
|---|---|---|---|---|---|
| 0 | C | 6 | E | 12 | G# |
| 1 | Db | 7 | F | 13 | A |
| 2 | C# | 8 | Gb | 14 | Bb |
| 3 | D | 9 | F# | 15 | A# |
| 4 | Eb | 10 | G | 16 | B |
| 5 | D# | 11 | Ab | | |

Enharmonic equivalents: E# = Gb, Fb = D#, B# = Db, Cb = A#. No double sharps or double flats are needed.

The staff works as in standard notation: seven staff positions per octave, the usual clefs, key signatures
and sharp, flat and natural signs. Only the size of the accidentals differs. The same names are used wherever
a pitch is named (tuning dialogs, fretboard, matrix editor, print header, ASCII export).

- A note with two names is written with the name of the table above, unless the key signature holds the other
  one: E# from 6 sharps, B# with 7 sharps, Cb from 6 flats, Fb with 7 flats. The "alternative enharmonic"
  toggle of a note switches between its two names.
- Clicking on the staff enters the note of that line or space, altered as the key signature says. In
  sharp/flat mode a natural position gets a sharp (2 steps up) in sharp keys and a flat (2 steps down) in flat
  keys. Other notes are entered on the tablature.
- Tools > Transpose moves notes by 17-EDO steps.

## Tablature: dot-relative fret labels

- Every fret that carries a fretboard dot is labelled with the number that dot has on a 12-EDO neck:
  3, 5, 7, 9, 12, 15, 17, 19, 21, 24.
- Frets past a dot are labelled `<dot>.<k>`, where `k` is the number of steps past that dot.
  Frets past the open string are `0.1`, `0.2`, ...; the open string is `0`.
- The dots sit on frets 4, 7, 10, 13, 17, 21, 24, 27, 30, 34 (`round(c * 17 / 12)` for each 12-EDO dot `c`).

| Fret | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Label | 0 | 0.1 | 0.2 | 0.3 | 3 | 3.1 | 3.2 | 5 | 5.1 | 5.2 | 7 | 7.1 | 7.2 | 9 | 9.1 | 9.2 | 9.3 | 12 |

C major (C E G C E; raw frets 0, 1, 0, 3, 4 from high e down to A):

```
e |-----0------|
B |-----0.1----|
G |-----0------|
D |-----0.3----|
A |-----3------|
E |------------|
```

Also included:

- A **"raw fret = label"** readout next to the caret, so the number you type stays visible.
- The **fretboard view** draws two octaves (34 frets) with real 17-EDO fret geometry and dots on the dot frets.
- Wider labels such as `24.2` get extra horizontal room so neighbouring notes do not overlap.
- Every visual export uses the same labels: print, PDF, PNG/JPG image, SVG and ASCII tab.
- Chord diagrams (in the tab and in the chord editor) label their starting fret the same way.
- The per-track **max fret** spinner (Track > Properties) goes up to 99 (stock: 39).

## Tunings

String values are 17-EDO steps, anchored to A4 = 440 Hz. Every A string is an exact 110/220/440 Hz and
the other strings follow in whole steps.

| Tuning | String values (high to low) | Steps between strings (low to high) |
|---|---|---|
| 6-string, E standard | `62,55,49,42,35,28` | 7-7-7-6-7 |
| 6-string, D standard | `59,52,46,39,32,25` | 7-7-7-6-7 |
| 7-string | `62,55,49,42,35,28,21` | 7-7-7-7-6-7 |
| 4-string bass | `32,25,18,11` | 7-7-7 |

- **Defaults**: File > New (any template), Track > Add track, a string-count change and a switch from a
  percussion channel all give the track its 17-EDO tuning and a max fret of 34 (the "24th fret").
- **Track > Tuning**: every stock preset (E standard, D standard, Drop D, 7-string, bass, ...) is offered as
  its nearest 17-EDO steps. The strings table shows the note names. Adjust a string with
  **-1 step / +1 step** (select it first) or double-click it to pick a note or type the step value
  (69 = A4). A warning appears when the highest string plus the track's frets pass MIDI key 127.
- **Custom presets** you save are stored as step values.
- **Files from stock TuxGuitar or Guitar Pro** are not converted when opened: their string values and fret
  numbers are read as 17-EDO steps, so a 12-EDO tab will not sound as written. Set the track's tuning in
  Track > Tuning and re-enter the frets.

## Editing additions

- **Sticky note duration**: the selected duration no longer follows rests. Typing sixteenths and moving into the
  next (empty) measure keeps sixteenths selected instead of jumping to the whole rest's duration. Moving onto
  an existing *note* still picks up that note's duration, as in stock.
- **Move notes in time**: select beats (drag, or Shift + click / Shift + arrows), then hold **Ctrl** and drag
  the selection; the caret shows the drop position, releasing the button moves the beats there (same track or
  another non-percussion track). The source beats become rests, the destination beats are replaced, as with
  cut + paste, but the clipboard is untouched. One Undo step. Releasing without dragging does nothing.
- **Del removes rests and empty measures**: on a rest that has notes after it in the measure, Del removes the
  rest and pulls those notes left (the measure is refilled with rests at its end). On a measure that holds only
  rests in *every* track, Del removes the measure; with a selection, every empty measure it touches is removed.
  Measures with notes, text or chords in any track are kept, and the song always keeps one measure. Undo restores them.

## Configuration

The tuning is not configurable. The one setting is the list of fretboard dots, in
Tools > Settings > **Fret labels** (`edo.labels.dots` in `~/.config/tuxguitar/config/tuxguitar.cfg`), a
comma-separated list of `fret:name` pairs. Empty means the default:

```
4:3, 7:5, 10:7, 13:9, 17:12, 21:15, 24:17, 27:19, 30:21, 34:24
```

Names are free text (they are only displayed). If two entries land on the same fret the first one wins and a warning is logged.
Malformed entries are skipped with a warning. Changes apply to open tabs as soon as the settings dialog is saved.
The dot list is global, not per track or per file.

## Playback

TuxGuitar plays a note as MIDI number `string value + fret`. The fork keeps that and retunes the synth so
those numbers sound as 17-EDO steps: MIDI key `k` sounds at `440 * 2^((k - 69) / 17)` Hz. Key 69 is A4, and
the 128 MIDI keys cover a 6-string guitar and a 4-string bass with 40 frets.

Mechanics: on every channel update the player sends a MIDI Tuning Standard *bulk tuning dump*
(sysex `F0 7E 7F 08 01 00 ...`) with all 128 keys, then selects tuning program 0 on every melodic channel
with RPN 3. Percussion channels are left alone.

Supported output ports: **Gervill** (Java Sound), **TuxGuitar Synth** (Gervill processor) and **FluidSynth**
(native `fluid_synth_sysex`). ALSA, JACK, WinMM, AudioUnit and other external ports do not get the sysex;
tune those synths yourself.

External synths: map MIDI note `n` to `440 * 2^((n - 69) / 17)` Hz. Load a Scala `.scl` + `.kbm` pair into a
synth that supports them (Surge XT, Pianoteq, Kontakt, ZynAddSubFX), or run an MTS-ESP master
(ODDSound MTS-ESP) with the 17-EDO scale and use an MTS-ESP-aware synth. A 17-EDO `.scl`:

```
! 17edo.scl
17-EDO
 17
 70.58824
 141.17647
 ... (multiples of 1200/17 cents, ending in 2/1)
```

## Removed tools

These stock tools only know 12 notes per octave and are not offered:

- Chord editor (Insert chord), with its chord finder and chord recognition. Chords saved earlier stay in
  the Chord menu and can still be inserted.
- Scale tool, and the scale shown on the fretboard.
- Piano view.
- Tuner, LilyPond export and MusicXML export plugins (not packaged; their sources are still in the tree).

## Known limitations

- Natural harmonics, the tonic colour in chord diagrams and chord names from imported files still assume 12 notes per octave.
- Two different accidentals on the same staff position in one measure (C# then Cb) are not told apart: the second one gets no sign.
- The chord diagram first-fret label uses the chord fret font (5 pt by default), so a label like `12.1` is hard to read at 100% zoom; raise "Chord fret font" in Styles or zoom in.
- The fretboard view draws at most 48 frets.
- Label width during layout is estimated from the note font size (the exact width is only known at paint time). Very long labels at tiny font sizes may sit closer together than stock 2-digit frets.
- Only the SWT desktop build has been tested. Android shares the library, so its tablature labels and staff are 17-EDO too, but its playback is not retuned.
- Synth retune covers Gervill, TuxGuitar Synth and FluidSynth only; other ports need an externally tuned synth.
- LilyPond and MusicXML exports have no 17-EDO pitch support: LilyPond computes its own fret numbers from pitch and tuning, and MusicXML carries no fret labels.
- 12-EDO files are not converted on open (see "Tunings").

## Contribute

The upstream contribution guide is in [CONTRIBUTING.md](docs/CONTRIBUTING.md).

## License

TuxGuitar is released under the GNU Lesser General Public License.

Copyright (C) 2005-2022 Julián Casadesús
              2023-2025 guiv42, helge17

## Third party products

TuxGuitar includes the following third party products:

* SWT version: SWT (Standard Widget Toolkit): https://www.eclipse.org/swt/
* JFX version: JavaFX (Java client application platform): https://openjfx.io/
* Gervill (Java Software Synthesizer)
* iText (Free Java-PDF library): https://itextpdf.com/
* Magic Sound Font v2.0 - Contributed by Dennis Deutschmann
