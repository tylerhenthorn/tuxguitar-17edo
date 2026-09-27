package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestMidiTuning {

	private static int semitone(byte[] dump, int key) {
		return dump[22 + key * 3] & 0xFF;
	}

	private static int fraction(byte[] dump, int key) {
		return ((dump[22 + key * 3 + 1] & 0x7F) << 7) | (dump[22 + key * 3 + 2] & 0x7F);
	}

	@Test
	public void testDumpLayout() {
		byte[] dump = new MidiTuning().createBulkTuningDump();
		assertEquals(8 + 16 + 128 * 3, dump.length);
		assertEquals(0xF0, dump[0] & 0xFF);
		assertEquals(0x7E, dump[1] & 0xFF);
		assertEquals(0x7F, dump[2] & 0xFF);
		assertEquals(0x08, dump[3] & 0xFF);
		assertEquals(0x01, dump[4] & 0xFF);
		assertEquals(0x00, dump[5] & 0xFF);
		assertEquals(0xF7, dump[dump.length - 1] & 0xFF);
		// every payload byte is 7-bit
		for (int i = 1; i < dump.length - 1; i++) {
			assertTrue((dump[i] & 0x80) == 0, "byte " + i);
		}
		// checksum: XOR of everything between F0 and the checksum itself
		int checksum = 0;
		for (int i = 1; i < dump.length - 2; i++) {
			checksum ^= (dump[i] & 0x7F);
		}
		assertEquals(checksum & 0x7F, dump[dump.length - 2] & 0xFF);
	}

	@Test
	public void test17EdoAnchoredAtA4() {
		MidiTuning tuning = new MidiTuning();
		byte[] dump = tuning.createBulkTuningDump();
		// the reference key plays A4 exactly
		assertEquals(69, semitone(dump, 69));
		assertEquals(0, fraction(dump, 69));
		// 17 steps = one octave
		assertEquals(81, semitone(dump, 86));
		assertEquals(0, fraction(dump, 86));
		assertEquals(57, semitone(dump, 52));
		assertEquals(0, fraction(dump, 52));
		// one step = 12/17 semitone = 70.588 cents
		assertEquals(69, semitone(dump, 70));
		assertEquals(Math.round(12.0 / 17.0 * 16384.0), fraction(dump, 70));
		// 7 steps below A4 (a 17-EDO fourth) = 64.06 semitones
		assertEquals(64, semitone(dump, 62));
		assertEquals(Math.round((5 - 7 * 12.0 / 17.0) * 16384.0), fraction(dump, 62));
		assertEquals(440.0, tuning.getFrequency(69, 440.0), 0.001);
		assertEquals(220.0, tuning.getFrequency(52, 440.0), 0.001);
		assertEquals(110.0, tuning.getFrequency(35, 440.0), 0.001);
	}

	@Test
	public void testToStepValueReproduces17EdoPresets() {
		MidiTuning tuning = new MidiTuning();
		int[] eStandard = {40, 45, 50, 55, 59, 64};
		int[] expectedE = {28, 35, 42, 49, 55, 62};
		int[] dStandard = {38, 43, 48, 53, 57, 62};
		int[] expectedD = {25, 32, 39, 46, 52, 59};
		for (int i = 0; i < 6; i++) {
			assertEquals(expectedE[i], tuning.toStepValue(eStandard[i]), "E string " + i);
			assertEquals(expectedD[i], tuning.toStepValue(dStandard[i]), "D string " + i);
		}
	}

	@Test
	public void testStepRange() {
		MidiTuning tuning = new MidiTuning();
		// every A is exact, bass low E and a 40-fret guitar fit the 128 keys
		assertEquals(35, tuning.toStepValue(45));
		assertEquals(11, tuning.toStepValueUnclamped(28));
		assertTrue(tuning.toStepValueUnclamped(64) + 40 <= 127);
		assertTrue(MidiTuning.getInstance(null) != null);
	}

	@Test
	public void testConvertDefaultTuning() {
		app.tuxguitar.song.managers.TGSongManager songManager = new app.tuxguitar.song.managers.TGSongManager();
		app.tuxguitar.song.models.TGTrack track = songManager.newSong().getTrack(0);

		new MidiTuning().convertDefaultTuning(track);
		int[] expected = {62, 55, 49, 42, 35, 28};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], track.getString(i + 1).getValue());
		}
		assertEquals(34, track.getMaxFret());
	}
}
