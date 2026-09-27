package app.tuxguitar.util;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.song.models.TGScale;

/*
 * This class provides generic static methods for notes manipulation:
 * - conversion from note pitch to note name, octave, alteration, accidental
 * - conversion from note name and octave to note pitch
 * - evaluation of presence of alterations, accidentals
 * - addition of interval to note
 *
 * Pitches are 17-EDO steps, named in Superpyth notation (chain of fifths):
 * - a whole tone (C-D) is 3 steps, a diatonic semitone (E-F, B-C) is 1 step
 * - a sharp raises by 2 steps, a flat lowers by 2 steps, so C# is one step above Db
 * - the 17 pitch classes are C Db C# D Eb D# E F Gb F# G Ab G# A Bb A# B
 * - 4 pitch classes have a second name: Db = B#, D# = Fb, Gb = E#, A# = Cb
 *   the second name is used when it belongs to the key signature, or as the "alternative enharmonic" of a note
 *
 * Conventions:
 * - note "index" is an integer in the range [0..6], 0=C, 1=D, 2=E, 3=F, 4=G, 5=A, 6=B
 * - "midiNote" is the midi key of the note, one key per 17-EDO step, midiNote 69 = A4 = 440 Hz, midiNote 56 = C4
 * - octave number follows general midi convention: in octave 4, A = 440Hz
 * - keySignature is encoded as everywhere in TuxGuitar: 0 = all naturals, 1 to 7 = 1 to 7 sharps, 8 to 15 = 1 to 7 flats
 * - alteration can be NATURAL, SHARP, FLAT
 *   e.g. alteration(C#) = SHARP, alteration(Db) = FLAT, alteration(F) = NATURAL
 * - accidental can be NONE, NATURAL, SHARP, FLAT
 *   e.g. with keySignature 2 sharps, accidental(D#) = SHARP, accidental(natural F) = NATURAL, accidental(C#) = NONE
 */


public class TGMusicKeyUtils {

	public static final int STEPS_PER_OCTAVE = 17;
	// number of steps of a sharp or a flat
	public static final int ALTERATION_STEPS = 2;
	public static final int C4_KEY = 56;

	public static final int MIN_MIDI_NOTE = 0;		// G#0
	public static final int MAX_MIDI_NOTE = 127;	// D8, 7-bits limitation

	// 12-EDO names, only used by tools which are still 12-EDO (chords, scales)
	public static final String sharpKeyNames[] = new String[] {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	public static final String flatKeyNames[] = new String[] {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

	// steps from C of the note indexes
	private static final int[] naturalSteps = {0, 3, 6, 7, 10, 13, 16};
	// default spelling of the 17 pitch classes: C Db C# D Eb D# E F Gb F# G Ab G# A Bb A# B
	private static final int[] indexes =     {0, 1, 0, 1, 2, 1, 2, 3, 4, 3, 4, 5, 4, 5, 6, 5, 6};
	private static final int[] alterations = {0,-1, 1, 0,-1, 1, 0, 0,-1, 1, 0,-1, 1, 0,-1, 1, 0};
	// pitch classes with a second name: B#, Fb, E#, Cb
	private static final int PITCH_CLASS_B_SHARP = 1;
	private static final int PITCH_CLASS_F_FLAT = 5;
	private static final int PITCH_CLASS_E_SHARP = 8;
	private static final int PITCH_CLASS_C_FLAT = 15;

	// 12-EDO notes indexes, only used to guess a key signature from a (12-EDO) scale
	private static final int[] indexesSharp =  {0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6};
	private static final int[] indexes6Sharp = {0, 0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6};	// E#
	private static final int[] indexes7Sharp = {6, 0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6};	// E#, B#
	private static final int[] indexesFlat =   {0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6};
	private static final int[] indexes6Flat =  {0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 0};	// Cb
	private static final int[] indexes7Flat =  {0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6, 0};	// Cb, Fb
	private static final int[][] tableIndex = {indexesSharp, indexes6Sharp, indexes7Sharp, indexesFlat, indexes6Flat, indexes7Flat};
	private static final int[] indexKeySignature = {0,0,0,0,0,0,1,2,3,3,3,3,3,4,5};
	// order of sharps (note indexes) FCGDAEB
	private static final int[] sharps = {3,0,4,1,5,2,6};
	// notes names per index
	private static final String[] names = {"C","D","E","F","G","A","B"};

	// accidentals
	public static final int NONE = 0;
	// accidentals, alterations
	public static final int NATURAL = 1;
	public static final int SHARP = 2;
	public static final int FLAT = 3;

	private static boolean isValidNote(int midiNote) {
		return (midiNote >= MIN_MIDI_NOTE && midiNote <= MAX_MIDI_NOTE);
	}

	private static boolean isValidKeySignature(int keySignature) {
		return (keySignature >= 0 && keySignature <= 14);
	}

	private static int pitchClass(int midiNote) {
		return Math.floorMod(midiNote - C4_KEY, STEPS_PER_OCTAVE);
	}

	// true if the second name of the pitch class belongs to the key signature
	private static boolean isSecondNameInKeySignature(int pitchClass, int keySignature) {
		switch (pitchClass) {
			case PITCH_CLASS_E_SHARP:
				return (keySignature == 6 || keySignature == 7);
			case PITCH_CLASS_B_SHARP:
				return (keySignature == 7);
			case PITCH_CLASS_C_FLAT:
				return (keySignature == 13 || keySignature == 14);
			case PITCH_CLASS_F_FLAT:
				return (keySignature == 14);
			default:
				return false;
		}
	}

	private static boolean hasSecondName(int pitchClass) {
		return (pitchClass == PITCH_CLASS_B_SHARP || pitchClass == PITCH_CLASS_F_FLAT
			|| pitchClass == PITCH_CLASS_E_SHARP || pitchClass == PITCH_CLASS_C_FLAT);
	}

	private static boolean isSecondName(int midiNote, int keySignature, boolean altEnharmonic) {
		int pitchClass = pitchClass(midiNote);
		return (hasSecondName(pitchClass) && (isSecondNameInKeySignature(pitchClass, keySignature) != altEnharmonic));
	}

	// ----- "default" note name and octave: without considering keySignature -------

	// midi note name, without octave
	// e.g. 71 -> "A#", 70 -> "Bb"
	public static String sharpNoteName(int midiNote) {
		return noteName(midiNote, 0);
	}

	// midi note name, without octave
	// in 17-EDO sharps and flats are different pitches, so this is the same name as sharpNoteName
	public static String flatNoteName(int midiNote) {
		return noteName(midiNote, 0);
	}

	// note octave
	// e.g. 70 -> 4
	public static int noteOctave(int midiNote) {
		return noteOctave(midiNote,0);
	}

	// midi note name, with octave
	// e.g. 71 -> "A#4"
	public static String sharpNoteFullName(int midiNote) {
		return noteFullName(midiNote, 0);
	}

	// returns true if midi note in [A,B,C,D,E,F,G], else false (if A#, Bb, etc)
	// don't use this method if keySignature needs to be considered (e.g. "Db" could in fact be "B#")
	public static boolean isNaturalNote(int midiNote) {
		return (alterations[pitchClass(midiNote)] == 0);
	}

	// ----- note name and octave: considering keySignature -------

	// midi note name, with octave, considering key signature
	// e.g. 64 -> "Gb4", or "E#4" if key signature = 6 or 7 sharps
	public static String noteFullName(int midiNote, int keySignature) {
		if (!isValidNote(midiNote) || !isValidKeySignature(keySignature)) return null;
		return noteName(midiNote, keySignature) + String.valueOf(noteOctave(midiNote, keySignature));
	}

	// midi note name, without octave, considering key signature
	// e.g. 64 -> "Gb", or "E#" if key signature = 6 or 7 sharps
	public static String noteName(int midiNote, int keySignature) {
		return noteName(midiNote, keySignature, false);
	}

	// midi note short name, without alteration, without octave, considering key signature
	// e.g. 64 -> "G", or "E" if key signature = 6 or 7 sharps
	public static String noteShortName(int midiNote, int keySignature) {
		return noteShortName(midiNote, keySignature, false);
	}

	// midi note octave, considering keySignature
	// because B#3 is one step above C4, and Cb4 is one step below B3
	public static int noteOctave(int midiNote, int keySignature) {
		return noteOctave(midiNote, keySignature, false);
	}

	// midi note index, considering keySignature
	public static int noteIndex(int midiNote, int keySignature) {
		return noteIndex(midiNote, keySignature, false);
	}

	// midi note alteration: flat, natural, sharp
	public static int noteAlteration(int midiNote, int keySignature) {
		return noteAlteration(midiNote, keySignature, false);
	}

	// midi note accidental, considering keySignature: none, flat, natural, sharp
	// returns none if note is altered with an alteration present in keySignature
	public static int noteAccidental(int midiNote, int keySignature) {
		return noteAccidental(midiNote, keySignature, false);
	}

	// alteration of note index, considering key signature
	// e.g. with keySignature==2, alteration of C (index 0) -> SHARP
	public static int noteIndexAlteration(int noteIndex, int keySignature) {
		if (keySignature == 0) {
			return NATURAL;
		}
		if (keySignature <= 7) {
			for (int i=1; i<=keySignature; i++) {
				if (noteIndex == sharps[i-1]) {
					return(SHARP);
				}
			}
		} else {
			for (int i=8; i<=keySignature; i++) {
				if (noteIndex == sharps[14-i]) {
					return(FLAT);
				}
			}
		}
		return NATURAL;
	}

	// ----- note name and octave: considering keySignature and alternative enharmonic representation -------
	// only B#, Fb, E#, Cb have an alternative, other notes are not changed

	public static String noteName(int midiNote, int keySignature, boolean altEnharmonic) {
		if (!isValidNote(midiNote) || !isValidKeySignature(keySignature)) return null;
		int alteration = noteAlteration(midiNote, keySignature, altEnharmonic);
		String alterationString = alteration==SHARP ? "#" : (alteration == FLAT ? "b" : "");
		return noteShortName(midiNote, keySignature, altEnharmonic) + alterationString;
	}
	public static String noteShortName(int midiNote, int keySignature, boolean altEnharmonic) {
		if (!isValidNote(midiNote) || !isValidKeySignature(keySignature)) return null;
		return names[noteIndex(midiNote, keySignature, altEnharmonic)];
	}
	public static int noteOctave(int midiNote, int keySignature, boolean altEnharmonic) {
		if (!isValidNote(midiNote) || !isValidKeySignature(keySignature)) return 0;
		int octave = Math.floorDiv(midiNote - C4_KEY, STEPS_PER_OCTAVE) + 4;
		if (isSecondName(midiNote, keySignature, altEnharmonic)) {
			int pitchClass = pitchClass(midiNote);
			if (pitchClass == PITCH_CLASS_B_SHARP) {
				octave--;
			}
			else if (pitchClass == PITCH_CLASS_C_FLAT) {
				octave++;
			}
		}
		return octave;
	}
	public static int noteIndex(int midiNote, int keySignature, boolean altEnharmonic) {
		if (!isValidKeySignature(keySignature)) return 0;
		int pitchClass = pitchClass(midiNote);
		if (isSecondName(midiNote, keySignature, altEnharmonic)) {
			switch (pitchClass) {
				case PITCH_CLASS_B_SHARP: return 6;
				case PITCH_CLASS_F_FLAT: return 3;
				case PITCH_CLASS_E_SHARP: return 2;
				default: return 0;	// Cb
			}
		}
		return indexes[pitchClass];
	}
	public static int noteAlteration(int midiNote, int keySignature, boolean altEnharmonic) {
		int pitchClass = pitchClass(midiNote);
		if (isSecondName(midiNote, keySignature, altEnharmonic)) {
			return ((pitchClass == PITCH_CLASS_B_SHARP || pitchClass == PITCH_CLASS_E_SHARP) ? SHARP : FLAT);
		}
		int alteration = alterations[pitchClass];
		return (alteration > 0 ? SHARP : (alteration < 0 ? FLAT : NATURAL));
	}
	public static int noteAccidental(int midiNote, int keySignature, boolean altEnharmonic) {
		int alteration = noteAlteration(midiNote, keySignature, altEnharmonic);
		// compare with expected alteration considering keySignature
		int index = noteIndex(midiNote, keySignature, altEnharmonic);
		return (alteration == noteIndexAlteration(index, keySignature)) ? NONE : alteration;
	}

		// ----- additions of offset -------
	// addition of offset to note index, ex: C-1->B, C+1->D
	public static int noteIndexAddInterval(int noteIndex, int offset) {
		int index = (noteIndex + offset) % 7;
		if (index<0) {
			index += 7;
		}
		return index;
	}
	// octave after addition of offset to note index, ex: C4-1->3, C4+1->4
	public static int noteOctaveAddInterval(int noteIndex, int octave, int offset) {
		return octave + (int)Math.floor((float)(noteIndex + offset)/7);
	}

	// ----- midi note from note index -------

	// (noteIndex=5 (A), octave=4) -> midi note 69
	public static int midiNote(int noteIndex, int octave) {
		return C4_KEY + STEPS_PER_OCTAVE*(octave - 4) + naturalSteps[noteIndex];
	}

	// ----- key signature of a scale (scales are 12-EDO) -------

	private static int scaleNoteIndex(int value, int keySignature) {
		return tableIndex[indexKeySignature[keySignature]][value % 12];
	}

	private static int scaleNoteAlteration(int value, int keySignature) {
		int index = scaleNoteIndex(value, keySignature);
		if ((7 + scaleNoteIndex(value, 8) - index) % 7 == 1) {
			return SHARP;
		}
		if ((7 + scaleNoteIndex(value, 0) - index) % 7 == 6) {
			return FLAT;
		}
		return NATURAL;
	}

	// returns -1 if invalid:
	// - if scale base note is sharp and key signature holds only flats (and vice-versa)
	// - if several notes in the scale have the same name (e.g. A and A# in the scale -> invalid)
	private static int getNbAccidentals(TGScale scale, int keySignature) {
		if ((scale.getAlteration()==SHARP) && (keySignature>7)) return -1;
		if ((scale.getAlteration()==FLAT) && (keySignature<=7)) return -1;

		List<String> notesNames = new ArrayList<String>();
		int nbAccidentals = 0;

		for (int i=0; i<12; i++) {
			int value = 12 + scale.getKey() + i;
			if (scale.getNote(value)) {
				int index = scaleNoteIndex(value, keySignature);
				String shortName = names[index];
				if (notesNames.contains(shortName)) {
					return -1;
				}
				notesNames.add(shortName);
				if ( scaleNoteAlteration(value, keySignature) != noteIndexAlteration(index, keySignature) ) {
					nbAccidentals++;
				}
			}
		}
		return nbAccidentals;
	}

	// try to guess key signature from scale
	public static int getKeySignature(TGScale scale) {
		// 0 ?
		if (getNbAccidentals(scale, 0) == 0) {
			return 0;
		}

		int bestKeySignature = 0;	// default
		int nbAccidentalsMin = 8;	// max +1
		for (int i = 7; i>0; i--) {
			// flat / sharp
			for (int j=7; j>=0; j-=7) {
				int keySignature = i + j;
				int nbAccidentals = getNbAccidentals(scale, keySignature);
				if ((nbAccidentals >= 0) && (nbAccidentals <= nbAccidentalsMin)) {
					nbAccidentalsMin = nbAccidentals;
					bestKeySignature = keySignature;
				}
			}
		}
		return bestKeySignature;
	}

}
