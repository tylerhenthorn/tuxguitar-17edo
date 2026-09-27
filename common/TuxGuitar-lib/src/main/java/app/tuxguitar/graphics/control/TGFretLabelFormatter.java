package app.tuxguitar.graphics.control;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.util.TGContext;

/**
 * Formats a raw fret number (an integer number of 17-EDO steps above the nut) as a
 * "dot-relative" label.
 *
 * Every fret that carries a fretboard dot is labelled with the name that dot has
 * on a 12-EDO neck (3, 5, 7, 9, 12, ...). Frets past a dot are labelled
 * "&lt;dot&gt;.&lt;k&gt;" where k is the number of steps past that dot. Frets past the
 * open string are "0.1", "0.2", ... and the open string is "0".
 *
 * Only display is affected: the note model keeps a plain integer fret.
 */
public class TGFretLabelFormatter {

	public static final int EDO = 17;

	/** 12-EDO fret positions that carry a dot on a conventional neck. */
	public static final int[] DOT_NAMES_12EDO = new int[] {3, 5, 7, 9, 12, 15, 17, 19, 21, 24};

	/** Formatter with the default dots. */
	public static final TGFretLabelFormatter DEFAULT = new TGFretLabelFormatter(null);

	private static final String CONTEXT_KEY = TGFretLabelFormatter.class.getName();

	/** A dot on the fretboard: which EDO fret it sits on, and what it is called. */
	public static class Dot {
		private final int fret;
		private final String name;

		public Dot(int fret, String name) {
			this.fret = fret;
			this.name = name;
		}

		public int getFret() {
			return this.fret;
		}

		public String getName() {
			return this.name;
		}

		public String toString() {
			return this.fret + ":" + this.name;
		}
	}

	private final List<Dot> dots;

	/**
	 * @param dots    explicit dot list, or null to use {@link #defaultDots()}
	 */
	public TGFretLabelFormatter(List<Dot> dots) {
		this.dots = normalize(dots != null ? dots : defaultDots());
	}

	/**
	 * The formatter published on the application context (the desktop config does this),
	 * so exporters that have no access to the configuration can still label frets.
	 * DEFAULT when nothing was published.
	 */
	public static TGFretLabelFormatter getInstance(TGContext context) {
		if (context != null && context.hasAttribute(CONTEXT_KEY)) {
			Object value = context.getAttribute(CONTEXT_KEY);
			if (value instanceof TGFretLabelFormatter) {
				return (TGFretLabelFormatter) value;
			}
		}
		return DEFAULT;
	}

	public static void setInstance(TGContext context, TGFretLabelFormatter formatter) {
		if (context != null) {
			context.setAttribute(CONTEXT_KEY, (formatter != null ? formatter : DEFAULT));
		}
	}

	/** Sorted by fret, duplicates removed. Never contains the nut. */
	public List<Dot> getDots() {
		return new ArrayList<Dot>(this.dots);
	}

	/** Frets (in EDO steps) that carry a dot, in ascending order. Useful for drawing a fretboard. */
	public int[] getDotFrets() {
		int[] frets = new int[this.dots.size()];
		for (int i = 0; i < frets.length; i++) {
			frets[i] = this.dots.get(i).getFret();
		}
		return frets;
	}

	/**
	 * The label for a fret. For negative frets, this is the plain integer.
	 */
	public String format(int fret) {
		if (fret < 0) {
			return Integer.toString(fret);
		}
		if (fret == 0) {
			return "0";
		}
		// the nut acts as a dot named "0" at fret 0
		int dotFret = 0;
		String dotName = "0";
		for (Dot dot : this.dots) {
			if (dot.getFret() <= fret) {
				dotFret = dot.getFret();
				dotName = dot.getName();
			} else {
				break;
			}
		}
		int k = fret - dotFret;
		return (k == 0 ? dotName : dotName + "." + k);
	}

	/** Serializes the dot list as "fret:name, fret:name, ...", the format accepted by {@link #parseDots(String)}. */
	public String getDotsAsString() {
		return dotsToString(this.dots);
	}

	/**
	 * Default dot map: each 12-EDO dot c lands on fret round(c * 17 / 12).
	 */
	public static List<Dot> defaultDots() {
		List<Dot> dots = new ArrayList<Dot>();
		for (int c : DOT_NAMES_12EDO) {
			int fret = Math.round((float) c * (float) EDO / 12f);
			dots.add(new Dot(fret, Integer.toString(c)));
		}
		return dots;
	}

	/**
	 * Parses "fret:name, fret:name, ..." (separators: comma, semicolon or whitespace).
	 * Malformed entries are skipped with a warning. Returns null for a blank string,
	 * which callers should treat as "use the formula".
	 */
	public static List<Dot> parseDots(String text) {
		if (text == null || text.trim().length() == 0) {
			return null;
		}
		List<Dot> dots = new ArrayList<Dot>();
		String[] entries = text.trim().split("[,;\\s]+");
		for (String entry : entries) {
			if (entry.length() == 0) {
				continue;
			}
			int sep = entry.indexOf(':');
			if (sep <= 0 || sep == entry.length() - 1) {
				System.err.println("EDO fret labels: ignoring malformed dot entry '" + entry + "' (expected fret:name)");
				continue;
			}
			try {
				int fret = Integer.parseInt(entry.substring(0, sep).trim());
				String name = entry.substring(sep + 1).trim();
				if (fret <= 0) {
					System.err.println("EDO fret labels: ignoring dot entry '" + entry + "' (fret must be > 0)");
					continue;
				}
				dots.add(new Dot(fret, name));
			} catch (NumberFormatException e) {
				System.err.println("EDO fret labels: ignoring malformed dot entry '" + entry + "' (fret is not a number)");
			}
		}
		return dots;
	}

	public static String dotsToString(List<Dot> dots) {
		StringBuilder sb = new StringBuilder();
		for (Dot dot : dots) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(dot.toString());
		}
		return sb.toString();
	}

	/** Sorts by fret; when two names land on the same fret, keeps the first and logs a warning. */
	private static List<Dot> normalize(List<Dot> input) {
		List<Dot> sorted = new ArrayList<Dot>();
		for (Dot dot : input) {
			if (dot == null || dot.getFret() <= 0) {
				continue;
			}
			boolean duplicate = false;
			for (Dot existing : sorted) {
				if (existing.getFret() == dot.getFret()) {
					System.err.println("EDO fret labels: dot '" + dot.getName() + "' and dot '" + existing.getName()
							+ "' both land on fret " + dot.getFret() + "; keeping '" + existing.getName() + "'");
					duplicate = true;
					break;
				}
			}
			if (duplicate) {
				continue;
			}
			int index = 0;
			while (index < sorted.size() && sorted.get(index).getFret() < dot.getFret()) {
				index++;
			}
			sorted.add(index, dot);
		}
		return sorted;
	}
}
