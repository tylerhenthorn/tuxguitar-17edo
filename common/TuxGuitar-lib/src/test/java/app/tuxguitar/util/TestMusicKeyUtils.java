package app.tuxguitar.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGScale;

// notes are 17-EDO steps: 56 = C4, 69 = A4
public class TestMusicKeyUtils {

	private static final int C4 = 56;
	private static final String[] NAMES = {"C", "Db", "C#", "D", "Eb", "D#", "E", "F", "Gb", "F#", "G", "Ab", "G#", "A", "Bb", "A#", "B"};
	private static final int[] INDEXES = {0, 1, 0, 1, 2, 1, 2, 3, 4, 3, 4, 5, 4, 5, 6, 5, 6};

	@Test
	public void testNotesNames() {
		for (int i = 0; i < 17; i++) {
			assertEquals(NAMES[i], TGMusicKeyUtils.sharpNoteName(C4 + i), "step " + i);
			assertEquals(NAMES[i], TGMusicKeyUtils.flatNoteName(C4 + i), "step " + i);
			assertEquals(NAMES[i], TGMusicKeyUtils.noteName(C4 + i, 0), "step " + i);
			assertEquals(NAMES[i], TGMusicKeyUtils.noteName(C4 + i - 34, 0), "step " + i);
			assertEquals(NAMES[i] + "4", TGMusicKeyUtils.sharpNoteFullName(C4 + i), "step " + i);
			assertEquals(NAMES[i].substring(0, 1), TGMusicKeyUtils.noteShortName(C4 + i, 0), "step " + i);
		}
		assertEquals("A", TGMusicKeyUtils.sharpNoteName(69));
		assertEquals("A#", TGMusicKeyUtils.sharpNoteName(71));
		assertEquals("Bb", TGMusicKeyUtils.sharpNoteName(70));
		assertEquals("A", TGMusicKeyUtils.sharpNoteName(86));
		assertEquals("G#0", TGMusicKeyUtils.sharpNoteFullName(0));
		assertEquals("D8", TGMusicKeyUtils.sharpNoteFullName(127));
		assertNull(TGMusicKeyUtils.sharpNoteName(-1));
		assertNull(TGMusicKeyUtils.sharpNoteName(128));
		assertNull(TGMusicKeyUtils.flatNoteName(-1));
		assertNull(TGMusicKeyUtils.noteShortName(128, 3));
		assertNull(TGMusicKeyUtils.noteFullName(128, 3));
		assertNull(TGMusicKeyUtils.noteName(60, 15));
	}

	@Test
	public void testOpenStrings() {
		int[] strings = {28, 35, 42, 49, 55, 62};
		String[] expected = {"E2", "A2", "D3", "G3", "B3", "E4"};
		for (int i = 0; i < strings.length; i++) {
			assertEquals(expected[i], TGMusicKeyUtils.sharpNoteFullName(strings[i]));
			assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(strings[i], 0));
		}
		// C major, raw frets 0 1 0 3 4 from high e down to A
		int[] chord = {62 + 0, 55 + 1, 49 + 0, 42 + 3, 35 + 4};
		String[] chordNames = {"E", "C", "G", "E", "C"};
		for (int i = 0; i < chord.length; i++) {
			assertEquals(chordNames[i], TGMusicKeyUtils.noteName(chord[i], 0));
			assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(chord[i], 0));
		}
		// fret 2 of the D string is D#
		assertEquals("D#", TGMusicKeyUtils.noteName(42 + 2, 0));
		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(42 + 2, 0));
		// 4-string bass
		assertEquals("E1", TGMusicKeyUtils.sharpNoteFullName(11));
		assertEquals("G2", TGMusicKeyUtils.sharpNoteFullName(32));
	}

	@Test
	public void testNotesOctave() {
		assertEquals(3, TGMusicKeyUtils.noteOctave(55));
		assertEquals(4, TGMusicKeyUtils.noteOctave(56));
		assertEquals(4, TGMusicKeyUtils.noteOctave(69));
		assertEquals(4, TGMusicKeyUtils.noteOctave(72));
		assertEquals(5, TGMusicKeyUtils.noteOctave(73));
		assertEquals(0, TGMusicKeyUtils.noteOctave(0));
		assertEquals(8, TGMusicKeyUtils.noteOctave(127));
		assertEquals(0, TGMusicKeyUtils.noteOctave(-1));
		assertEquals(0, TGMusicKeyUtils.noteOctave(128));

		// Db4 == B#3
		assertEquals(4, TGMusicKeyUtils.noteOctave(57, 6));
		assertEquals(3, TGMusicKeyUtils.noteOctave(57, 7));
		assertEquals(4, TGMusicKeyUtils.noteOctave(57, 8));
		// A#3 == Cb4
		assertEquals(3, TGMusicKeyUtils.noteOctave(54, 12));
		assertEquals(4, TGMusicKeyUtils.noteOctave(54, 13));
		assertEquals(4, TGMusicKeyUtils.noteOctave(54, 14));
	}

	@Test
	public void testNoteIsNatural() {
		for (int i = 0; i < 17; i++) {
			assertEquals(NAMES[i].length() == 1, TGMusicKeyUtils.isNaturalNote(C4 + i), "step " + i);
		}
		assertTrue(TGMusicKeyUtils.isNaturalNote(28));
		assertTrue(TGMusicKeyUtils.isNaturalNote(29));	// F2
		assertFalse(TGMusicKeyUtils.isNaturalNote(30));	// Gb2
	}

	@Test
	public void testNotesPosition() {
		// without key signature: every name of the table
		for (int i = 0; i < 17; i++) {
			int expected = (NAMES[i].endsWith("#") ? TGMusicKeyUtils.SHARP : (NAMES[i].endsWith("b") ? TGMusicKeyUtils.FLAT : TGMusicKeyUtils.NATURAL));
			assertEquals(INDEXES[i], TGMusicKeyUtils.noteIndex(C4 + i, 0), "step " + i);
			assertEquals(expected, TGMusicKeyUtils.noteAlteration(C4 + i, 0), "step " + i);
			assertEquals(expected == TGMusicKeyUtils.NATURAL ? TGMusicKeyUtils.NONE : expected, TGMusicKeyUtils.noteAccidental(C4 + i, 0), "step " + i);
		}

		// C# and Db are different notes, whatever the key signature
		for (int key = 0; key <= 14; key++) {
			assertEquals(0, TGMusicKeyUtils.noteIndex(C4 + 2, key));
			assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAlteration(C4 + 2, key));
			assertEquals(2, TGMusicKeyUtils.noteIndex(C4 + 4, key));
			assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAlteration(C4 + 4, key));
		}

		// F# (step 9): accidental until the key signature holds it
		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(C4 + 9, 0));
		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(C4 + 9, 8));
		for (int key = 1; key <= 7; key++) {
			assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 9, key));
			// natural F needs a natural sign
			assertEquals(3, TGMusicKeyUtils.noteIndex(C4 + 7, key));
			assertEquals(TGMusicKeyUtils.NATURAL, TGMusicKeyUtils.noteAccidental(C4 + 7, key));
		}
		// C# (step 2): in key signature from 2 sharps
		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(C4 + 2, 1));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 2, 2));
		assertEquals(TGMusicKeyUtils.NATURAL, TGMusicKeyUtils.noteAccidental(C4, 2));
		// Bb (step 14): in key signature from 1 flat, Eb (step 4) from 2 flats
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 14, 0));
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 14, 3));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 14, 8));
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 4, 8));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 4, 9));
		assertEquals(TGMusicKeyUtils.NATURAL, TGMusicKeyUtils.noteAccidental(C4 + 16, 8));
		// a sharp in a flat key
		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(C4 + 15, 8));
	}

	@Test
	public void testSecondNames() {
		// Gb or E#?
		assertEquals("Gb", TGMusicKeyUtils.noteName(C4 + 8, 5));
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 8, 5));
		assertEquals("E#", TGMusicKeyUtils.noteName(C4 + 8, 6));
		assertEquals(2, TGMusicKeyUtils.noteIndex(C4 + 8, 6));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 8, 6));
		assertEquals("E#4", TGMusicKeyUtils.noteFullName(C4 + 8, 7));
		assertEquals("Gb", TGMusicKeyUtils.noteName(C4 + 8, 8));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 8, 12));

		// Db or B#?
		assertEquals("Db", TGMusicKeyUtils.noteName(C4 + 1, 6));
		assertEquals("B#", TGMusicKeyUtils.noteName(C4 + 1, 7));
		assertEquals(6, TGMusicKeyUtils.noteIndex(C4 + 1, 7));
		assertEquals("B#3", TGMusicKeyUtils.noteFullName(C4 + 1, 7));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 1, 7));

		// A# or Cb?
		assertEquals("A#", TGMusicKeyUtils.noteName(C4 + 15, 12));
		assertEquals("Cb", TGMusicKeyUtils.noteName(C4 + 15, 13));
		assertEquals(0, TGMusicKeyUtils.noteIndex(C4 + 15, 14));
		assertEquals("Cb5", TGMusicKeyUtils.noteFullName(C4 + 15, 13));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 15, 13));

		// D# or Fb?
		assertEquals("D#", TGMusicKeyUtils.noteName(C4 + 5, 13));
		assertEquals("Fb", TGMusicKeyUtils.noteName(C4 + 5, 14));
		assertEquals(3, TGMusicKeyUtils.noteIndex(C4 + 5, 14));
		assertEquals("Fb4", TGMusicKeyUtils.noteFullName(C4 + 5, 14));
		assertEquals(TGMusicKeyUtils.NONE, TGMusicKeyUtils.noteAccidental(C4 + 5, 14));
	}

	@Test
	public void testNoteIndexToMidi(){
		assertEquals(69, TGMusicKeyUtils.midiNote(5,4));
		assertEquals(56, TGMusicKeyUtils.midiNote(0,4));
		assertEquals(55, TGMusicKeyUtils.midiNote(6,3));
		assertEquals(28, TGMusicKeyUtils.midiNote(2,2));
		assertEquals(49, TGMusicKeyUtils.midiNote(4,3));
		// round trip, all naturals
		for (int octave = 1; octave <= 7; octave++) {
			for (int index = 0; index < 7; index++) {
				int midiNote = TGMusicKeyUtils.midiNote(index, octave);
				assertEquals(index, TGMusicKeyUtils.noteIndex(midiNote, 0));
				assertEquals(octave, TGMusicKeyUtils.noteOctave(midiNote, 0));
				assertTrue(TGMusicKeyUtils.isNaturalNote(midiNote));
				// altered note of the same staff position
				assertEquals(index, TGMusicKeyUtils.noteIndex(midiNote + TGMusicKeyUtils.ALTERATION_STEPS, 7));
				assertEquals(index, TGMusicKeyUtils.noteIndex(midiNote - TGMusicKeyUtils.ALTERATION_STEPS, 14));
				assertEquals(octave, TGMusicKeyUtils.noteOctave(midiNote + TGMusicKeyUtils.ALTERATION_STEPS, 7));
				assertEquals(octave, TGMusicKeyUtils.noteOctave(midiNote - TGMusicKeyUtils.ALTERATION_STEPS, 14));
			}
		}
	}

	@Test
	public void testNoteIndexAlteration() {
		// all naturals, all sharps, all flats
		for (int i=0; i<7; i++) {
			assertEquals(TGMusicKeyUtils.NATURAL, TGMusicKeyUtils.noteIndexAlteration(i, 0));
			assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteIndexAlteration(i, 7));
			assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteIndexAlteration(i, 14));
		}
		// 1 sharp/flat
		for (int i=0; i<7; i++) {
			assertEquals((i==3 ? TGMusicKeyUtils.SHARP: TGMusicKeyUtils.NATURAL), TGMusicKeyUtils.noteIndexAlteration(i, 1));
			assertEquals((i==6 ? TGMusicKeyUtils.FLAT : TGMusicKeyUtils.NATURAL), TGMusicKeyUtils.noteIndexAlteration(i, 8));
		}
		// 6 sharp/flat
		for (int i=0; i<7; i++) {
			assertEquals((i!=6 ? TGMusicKeyUtils.SHARP: TGMusicKeyUtils.NATURAL), TGMusicKeyUtils.noteIndexAlteration(i, 6));
			assertEquals((i!=3 ? TGMusicKeyUtils.FLAT : TGMusicKeyUtils.NATURAL), TGMusicKeyUtils.noteIndexAlteration(i, 13));
		}
	}

	@Test
	public void testAddInterval() {
		// notes indexes (i.e. C, D, ... B)
		assertEquals(2, TGMusicKeyUtils.noteIndexAddInterval(0, 2)); // C+2 = E
		assertEquals(0, TGMusicKeyUtils.noteIndexAddInterval(0, 7)); // C+7 = C
		assertEquals(6, TGMusicKeyUtils.noteIndexAddInterval(0, -1)); // C-1 = B
		assertEquals(5, TGMusicKeyUtils.noteIndexAddInterval(6, -8)); // C-8 = A
		// notes octaves
		assertEquals(3, TGMusicKeyUtils.noteOctaveAddInterval(0, 4, -1)); // C4-1 = octave 3
		assertEquals(5, TGMusicKeyUtils.noteOctaveAddInterval(5, 4, 2)); //  A4+2 = octave 5
		assertEquals(4, TGMusicKeyUtils.noteOctaveAddInterval(3, 4, -2)); // F4-8 = octave 4
		assertEquals(3, TGMusicKeyUtils.noteOctaveAddInterval(3, 4, -8)); // F4-8 = octave 3
	}

	// alternative enharmonic representation: only Db/B#, D#/Fb, Gb/E#, A#/Cb
	@Test
	public void testAltName() {
		assertEquals("E#", TGMusicKeyUtils.noteName(C4 + 8, 0, true));
		assertEquals("E", TGMusicKeyUtils.noteShortName(C4 + 8, 0, true));
		assertEquals("Gb", TGMusicKeyUtils.noteName(C4 + 8, 6, true));
		assertEquals("B#", TGMusicKeyUtils.noteName(C4 + 1, 0, true));
		assertEquals("Db", TGMusicKeyUtils.noteName(C4 + 1, 7, true));
		assertEquals("Cb", TGMusicKeyUtils.noteName(C4 + 15, 0, true));
		assertEquals("A#", TGMusicKeyUtils.noteName(C4 + 15, 13, true));
		assertEquals("Fb", TGMusicKeyUtils.noteName(C4 + 5, 0, true));
		assertEquals("D#", TGMusicKeyUtils.noteName(C4 + 5, 14, true));

		assertEquals(TGMusicKeyUtils.SHARP, TGMusicKeyUtils.noteAccidental(C4 + 8, 0, true));
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 8, 6, true));
		assertEquals(TGMusicKeyUtils.FLAT, TGMusicKeyUtils.noteAccidental(C4 + 15, 0, true));

		// other notes have no alternative
		for (int keySignature = 0; keySignature <= 14; keySignature++) {
			for (int step : new int[] {0, 2, 3, 4, 6, 7, 9, 10, 11, 12, 13, 14, 16}) {
				assertEquals(TGMusicKeyUtils.noteName(C4 + step, keySignature), TGMusicKeyUtils.noteName(C4 + step, keySignature, true));
				assertEquals(TGMusicKeyUtils.noteIndex(C4 + step, keySignature), TGMusicKeyUtils.noteIndex(C4 + step, keySignature, true));
				assertEquals(TGMusicKeyUtils.noteOctave(C4 + step, keySignature), TGMusicKeyUtils.noteOctave(C4 + step, keySignature, true));
				assertEquals(TGMusicKeyUtils.noteAccidental(C4 + step, keySignature), TGMusicKeyUtils.noteAccidental(C4 + step, keySignature, true));
			}
		}
	}

	@Test
	public void testAltOctave() {
		// Db4 == B#3
		assertEquals(3, TGMusicKeyUtils.noteOctave(57,6,true));
		assertEquals(4, TGMusicKeyUtils.noteOctave(57,7,true));
		assertEquals(3, TGMusicKeyUtils.noteOctave(57,8,true));
		// A#3 == Cb4
		assertEquals(4, TGMusicKeyUtils.noteOctave(54,12,true));
		assertEquals(3, TGMusicKeyUtils.noteOctave(54,13,true));
		assertEquals(3, TGMusicKeyUtils.noteOctave(54,14,true));
	}

	// scales are 12-EDO
	@Test
	public void testScaleKeySignature() {
		TGFactory factory = new TGFactory();
		// major scale
		TGScale scale = factory.newScale();
		scale.setNote(0, true);
		scale.setNote(2, true);
		scale.setNote(4, true);
		scale.setNote(5, true);
		scale.setNote(7, true);
		scale.setNote(9, true);
		scale.setNote(11, true);

		scale.setKeyName("C");
		assertEquals(0, TGMusicKeyUtils.getKeySignature(scale));

		scale.setKeyName("D");
		assertEquals(2, TGMusicKeyUtils.getKeySignature(scale));

		scale.setKeyName("Eb");
		assertEquals(7 + 3, TGMusicKeyUtils.getKeySignature(scale));

		scale.setKeyName("F");
		assertEquals(7 + 1, TGMusicKeyUtils.getKeySignature(scale));

		scale.setKeyName("C#");
		assertEquals(7, TGMusicKeyUtils.getKeySignature(scale));

		// minor scale
		scale.clear();
		scale.setNote(0, true);
		scale.setNote(2, true);
		scale.setNote(3, true);
		scale.setNote(5, true);
		scale.setNote(7, true);
		scale.setNote(8, true);
		scale.setNote(10, true);

		scale.setKeyName("A");
		assertEquals(0, TGMusicKeyUtils.getKeySignature(scale));

		scale.setKeyName("C");
		assertEquals(7+3, TGMusicKeyUtils.getKeySignature(scale));

	}

}
