package app.tuxguitar.app.view.dialog.settings.items;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.view.dialog.settings.TGSettingsEditor;
import app.tuxguitar.graphics.control.TGFretLabelFormatter;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.toolbar.UIToolBar;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UITextField;
import app.tuxguitar.util.TGSynchronizer;

/**
 * Preferences page for the dot-relative fret labels: the list of fretboard dots.
 */
public class EdoLabelsOption extends TGSettingsOption {

	private static final int PREVIEW_LAST_FRET = (TGFretLabelFormatter.EDO + 2);

	private boolean initialized;
	private UITextField dots;
	private UIButton resetDots;
	private UILabel preview;

	public EdoLabelsOption(TGSettingsEditor configEditor, UIToolBar toolBar, UILayoutContainer parent){
		super(configEditor, toolBar, parent, TuxGuitar.getProperty("settings.config.edo"));
		this.initialized = false;
	}

	public void createOption() {
		UIFactory uiFactory = this.getUIFactory();

		getToolItem().setText(TuxGuitar.getProperty("settings.config.edo"));
		getToolItem().setImage(TuxGuitar.getInstance().getIconManager().getOptionStyle());
		getToolItem().addSelectionListener(this);

		showLabel(getPanel(), TuxGuitar.getProperty("settings.config.edo.title"), true, 1, 1);

		UITableLayout optionsLayout = new UITableLayout();
		UIPanel options = uiFactory.createPanel(getPanel(), false);
		options.setLayout(optionsLayout);
		this.indent(options, 2, 1);

		UILabel dotsLabel = uiFactory.createLabel(options);
		dotsLabel.setText(TuxGuitar.getProperty("settings.config.edo.dots") + ":");
		optionsLayout.set(dotsLabel, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, true);

		this.dots = uiFactory.createTextField(options);
		this.dots.setTextLimit(400);
		optionsLayout.set(this.dots, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);

		UILabel dotsHelp = uiFactory.createLabel(options);
		dotsHelp.setText(TuxGuitar.getProperty("settings.config.edo.dots.help"));
		optionsLayout.set(dotsHelp, 2, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true);

		this.resetDots = uiFactory.createButton(options);
		this.resetDots.setText(TuxGuitar.getProperty("settings.config.edo.dots.reset"));
		optionsLayout.set(this.resetDots, 3, 2, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, true);

		this.preview = uiFactory.createLabel(options);
		optionsLayout.set(this.preview, 4, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true, 1, 2);

		this.resetDots.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				EdoLabelsOption.this.dots.setText(TGFretLabelFormatter.dotsToString(TGFretLabelFormatter.defaultDots()));
				EdoLabelsOption.this.updatePreview();
			}
		});
		this.dots.addModifyListener(new app.tuxguitar.ui.event.UIModifyListener() {
			public void onModify(app.tuxguitar.ui.event.UIModifyEvent event) {
				EdoLabelsOption.this.updatePreview();
			}
		});

		this.loadConfig();
	}

	protected void loadConfig(){
		new Thread(new Runnable() {
			public void run() {
				final String dots = getConfig().getStringValue(TGConfigKeys.EDO_LABELS_DOTS);
				TGSynchronizer.getInstance(getViewContext().getContext()).executeLater(new Runnable() {
					public void run() {
						if(!isDisposed()){
							EdoLabelsOption.this.dots.setText(dots != null ? dots : "");
							EdoLabelsOption.this.updatePreview();
							EdoLabelsOption.this.initialized = true;
							EdoLabelsOption.this.pack();
						}
					}
				});
			}
		}).start();
	}

	private TGFretLabelFormatter createFormatter() {
		return new TGFretLabelFormatter(TGFretLabelFormatter.parseDots(this.dots.getText()));
	}

	private void updatePreview() {
		TGFretLabelFormatter formatter = this.createFormatter();
		StringBuilder sb = new StringBuilder();
		sb.append(TuxGuitar.getProperty("settings.config.edo.preview")).append(": ");
		for (int fret = 0; fret <= PREVIEW_LAST_FRET; fret++) {
			if (fret > 0) {
				sb.append("  ");
			}
			sb.append(formatter.format(fret));
		}
		this.preview.setText(sb.toString());
	}

	public void updateConfig(){
		if(this.initialized){
			// blank means "use the default dots"; store the normalized list otherwise
			String text = this.dots.getText();
			String value = (TGFretLabelFormatter.parseDots(text) == null ? "" : this.createFormatter().getDotsAsString());
			getConfig().setValue(TGConfigKeys.EDO_LABELS_DOTS, value);
		}
	}

	public void updateDefaults(){
		if(this.initialized){
			getConfig().setValue(TGConfigKeys.EDO_LABELS_DOTS, getDefaults().getValue(TGConfigKeys.EDO_LABELS_DOTS));
		}
	}
}
