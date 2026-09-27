package app.tuxguitar.graphics.control;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.graphics.control.TGFretLabelFormatter.Dot;

public class TestFretLabelFormatter {

	private static final String[] EXPECTED_17EDO = new String[] {
		"0",
		"0.1", "0.2", "0.3",
		"3", "3.1", "3.2",
		"5", "5.1", "5.2",
		"7", "7.1", "7.2",
		"9", "9.1", "9.2", "9.3",
		"12", "12.1", "12.2", "12.3",
		"15", "15.1", "15.2",
		"17", "17.1", "17.2",
		"19", "19.1", "19.2",
		"21", "21.1", "21.2", "21.3",
		"24", "24.1", "24.2", "24.3", "24.4", "24.5", "24.6"
	};

	@Test
	public void testDefaultDots17Edo() {
		TGFretLabelFormatter f = new TGFretLabelFormatter(null);
		assertArrayEquals(new int[] {4, 7, 10, 13, 17, 21, 24, 27, 30, 34}, f.getDotFrets());
		assertEquals("4:3, 7:5, 10:7, 13:9, 17:12, 21:15, 24:17, 27:19, 30:21, 34:24", f.getDotsAsString());
	}

	@Test
	public void testFormat17EdoFrets0To40() {
		TGFretLabelFormatter f = new TGFretLabelFormatter(null);
		for (int fret = 0; fret <= 40; fret++) {
			assertEquals(EXPECTED_17EDO[fret], f.format(fret), "fret " + fret);
		}
		// spot checks from the plan
		assertEquals("0.1", f.format(1));
		assertEquals("3", f.format(4));
		assertEquals("3.1", f.format(5));
		assertEquals("12", f.format(17));
		assertEquals("12.1", f.format(18));
	}

	@Test
	public void testCMajorExample17Edo() {
		TGFretLabelFormatter f = new TGFretLabelFormatter(null);
		// e B G D A : frets 0 1 0 3 4
		assertEquals("0", f.format(0));
		assertEquals("0.1", f.format(1));
		assertEquals("0", f.format(0));
		assertEquals("0.3", f.format(3));
		assertEquals("3", f.format(4));
	}

	@Test
	public void testCustomDotList() {
		List<Dot> dots = new ArrayList<Dot>();
		dots.add(new Dot(5, "A"));
		dots.add(new Dot(2, "B"));
		TGFretLabelFormatter f = new TGFretLabelFormatter(dots);
		assertArrayEquals(new int[] {2, 5}, f.getDotFrets());
		assertEquals("0.1", f.format(1));
		assertEquals("B", f.format(2));
		assertEquals("B.1", f.format(3));
		assertEquals("B.2", f.format(4));
		assertEquals("A", f.format(5));
		assertEquals("A.30", f.format(35));
	}

	@Test
	public void testDuplicateFretKeepsFirst() {
		List<Dot> dots = new ArrayList<Dot>();
		dots.add(new Dot(4, "3"));
		dots.add(new Dot(4, "x"));
		dots.add(new Dot(7, "5"));
		TGFretLabelFormatter f = new TGFretLabelFormatter(dots);
		assertEquals(2, f.getDots().size());
		assertEquals("3", f.format(4));
		assertEquals("3.2", f.format(6));
		assertEquals("5", f.format(7));
	}

	@Test
	public void testParseDots() {
		List<Dot> dots = TGFretLabelFormatter.parseDots(" 4:3, 7:5;10:7  13:9 ");
		assertEquals(4, dots.size());
		assertEquals(4, dots.get(0).getFret());
		assertEquals("3", dots.get(0).getName());
		assertEquals(13, dots.get(3).getFret());

		// malformed entries are skipped, not fatal
		List<Dot> partial = TGFretLabelFormatter.parseDots("4:3, junk, :5, 7:, x:y, 0:z, 10:7");
		assertEquals(2, partial.size());
		assertEquals(4, partial.get(0).getFret());
		assertEquals(10, partial.get(1).getFret());

		assertNull(TGFretLabelFormatter.parseDots(""));
		assertNull(TGFretLabelFormatter.parseDots("   "));
		assertNull(TGFretLabelFormatter.parseDots(null));
	}

	@Test
	public void testRoundTripThroughString() {
		TGFretLabelFormatter a = new TGFretLabelFormatter(null);
		TGFretLabelFormatter b = new TGFretLabelFormatter(TGFretLabelFormatter.parseDots(a.getDotsAsString()));
		assertArrayEquals(a.getDotFrets(), b.getDotFrets());
		for (int fret = 0; fret <= 45; fret++) {
			assertEquals(a.format(fret), b.format(fret));
		}
	}

	@Test
	public void testDefaultInstance() {
		assertEquals("12.2", TGFretLabelFormatter.DEFAULT.format(19));
		assertEquals("12.2", TGFretLabelFormatter.getInstance(null).format(19));
	}

	@Test
	public void testNegativeFretPassesThrough() {
		TGFretLabelFormatter f = new TGFretLabelFormatter(null);
		assertEquals("-1", f.format(-1));
	}
}
