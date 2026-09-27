package app.tuxguitar.app.system.config;

import app.tuxguitar.graphics.control.TGFretLabelFormatter;

/**
 * Builds the fret label formatter from the user configuration.
 */
public class TGFretLabelConfig {

	public static TGFretLabelFormatter createFormatter(TGConfigManager config) {
		String dots = config.getStringValue(TGConfigKeys.EDO_LABELS_DOTS);
		TGFretLabelFormatter formatter = new TGFretLabelFormatter(TGFretLabelFormatter.parseDots(dots));
		// publish for exporters (SVG, ASCII, headless PDF) that cannot read the config
		TGFretLabelFormatter.setInstance(config.getContext(), formatter);
		return formatter;
	}
}
