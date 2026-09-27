package app.tuxguitar.player.base;

import app.tuxguitar.song.models.TGString;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;

/**
 * 17-EDO tuning of the built-in synthesizers, sent as a
 * MIDI Tuning Standard bulk dump (Universal Non-Real-Time SysEx 08 01).
 *
 * MIDI key {@code k} is mapped to the 12-EDO pitch {@code 69 + (k - 69) * 12 / 17}:
 * key 69 plays A4 = 440 Hz and every other key is one 17-EDO step per unit of key number.
 * String values are step indices, so the note number TuxGuitar emits is exactly the step to play.
 */
public class MidiTuning {

	public static final int EDO = 17;
	public static final int A4_KEY = 69;

	private static final MidiTuning INSTANCE = new MidiTuning();
	private static final int TUNING_PROGRAM = 0;

	public MidiTuning() {
		super();
	}

	public static MidiTuning getInstance(TGContext context) {
		return INSTANCE;
	}

	/** The tuning program the dump is stored under; channels select it with RPN 3. */
	public int getTuningProgram() {
		return TUNING_PROGRAM;
	}

	/** 12-EDO pitch, in (fractional) semitones, that MIDI key {@code key} should sound at. */
	public double getPitch(int key) {
		return A4_KEY + ((key - A4_KEY) * 12.0 / EDO);
	}

	/**
	 * The step value (string value / MIDI key) closest to a 12-EDO pitch:
	 * 69 + round((midiNote - 69) * 17 / 12), i.e. rounded relative to A4 so that every A
	 * stays exact.
	 */
	public int toStepValue(int midiNote) {
		return (int) Math.max(0, Math.min(127, toStepValueUnclamped(midiNote)));
	}

	/** Same as {@link #toStepValue(int)} but without clamping, so callers can detect notes that fall outside the MIDI key range. */
	public long toStepValueUnclamped(int midiNote) {
		return A4_KEY + Math.round((midiNote - A4_KEY) * (double) EDO / 12.0);
	}

	/**
	 * Turns a track that holds a stock 12-EDO tuning (new song, new track, template) into its
	 * 17-EDO equivalent: every string becomes the nearest step and the max fret grows to the
	 * 24th-fret position (34). Nothing happens on percussion tracks.
	 */
	public void convertDefaultTuning(TGTrack track) {
		if (track != null && !track.isPercussion()) {
			for (TGString string : track.getStrings()) {
				string.setValue(toStepValue(string.getValue()));
			}
			track.setMaxFret(Math.max(track.getMaxFret(), (int) Math.round(24 * (double) EDO / 12.0)));
		}
	}

	/** Frequency in Hz for a MIDI key, with A4 (key 69) at {@code a4}. */
	public double getFrequency(int key, double a4) {
		return a4 * Math.pow(2.0, (getPitch(key) - 69.0) / 12.0);
	}

	/**
	 * Builds a MIDI Tuning Standard bulk tuning dump for all 128 keys:
	 * F0 7E 7F 08 01 tt &lt;16-char name&gt; &lt;128 x (xx yy zz)&gt; checksum F7.
	 * xx is the 12-EDO semitone, yy zz the 14-bit fraction of a semitone.
	 * Keys whose pitch falls outside 0..127.99 are marked "no change" (7F 7F 7F).
	 */
	public byte[] createBulkTuningDump() {
		byte[] data = new byte[8 + 16 + (128 * 3)];
		int i = 0;
		data[i++] = (byte) 0xF0;
		data[i++] = (byte) 0x7E;
		data[i++] = (byte) 0x7F; // all devices
		data[i++] = (byte) 0x08;
		data[i++] = (byte) 0x01;
		data[i++] = (byte) TUNING_PROGRAM;

		String name = (EDO + "-EDO");
		for (int c = 0; c < 16; c++) {
			data[i++] = (byte) (c < name.length() ? (name.charAt(c) & 0x7F) : 0x20);
		}

		for (int key = 0; key < 128; key++) {
			double pitch = getPitch(key);
			int semitone = (int) Math.floor(pitch);
			int fraction = (int) Math.round((pitch - semitone) * 16384.0);
			if (fraction >= 16384) {
				semitone++;
				fraction = 0;
			}
			if (semitone < 0 || semitone > 127) {
				data[i++] = (byte) 0x7F;
				data[i++] = (byte) 0x7F;
				data[i++] = (byte) 0x7F;
			} else {
				data[i++] = (byte) semitone;
				data[i++] = (byte) ((fraction >> 7) & 0x7F);
				data[i++] = (byte) (fraction & 0x7F);
			}
		}

		int checksum = 0;
		for (int j = 1; j < i; j++) {
			checksum ^= (data[j] & 0x7F);
		}
		data[i++] = (byte) (checksum & 0x7F);
		data[i++] = (byte) 0xF7;
		return data;
	}

	public String toString() {
		return "MidiTuning[" + EDO + "-EDO]";
	}
}
