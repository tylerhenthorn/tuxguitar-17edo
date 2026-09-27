package app.tuxguitar.app.util;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMusicKeyUtils;

/**
 * Names for string values in the tuning dialogs. The value is a 17-EDO step index
 * (MIDI key, 69 = A4), named in Superpyth notation.
 */
public class TGEdoNoteNames {

	/** Short name for tables and preset lists, e.g. "E", "Bb". */
	public static String shortName(TGContext context, int value) {
		return TGMusicKeyUtils.sharpNoteName(value);
	}

	/** Name with octave, e.g. "A2". */
	public static String fullName(TGContext context, int value) {
		return TGMusicKeyUtils.sharpNoteFullName(value);
	}
}
