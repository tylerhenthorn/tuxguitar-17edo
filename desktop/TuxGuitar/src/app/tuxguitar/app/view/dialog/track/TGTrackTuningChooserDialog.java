package app.tuxguitar.app.view.dialog.track;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.*;
import app.tuxguitar.app.util.TGEdoNoteNames;
import app.tuxguitar.util.TGMusicKeyUtils;

public class TGTrackTuningChooserDialog {

	private TGTrackTuningDialog tuningDialog;

	private UIButton buttonOK;

	public TGTrackTuningChooserDialog(TGTrackTuningDialog tuningDialog){
		this.tuningDialog = tuningDialog;
	}

	public void select(final TGTrackTuningChooserHandler handler) {
		this.select(handler, null);
	}

	public void select(final TGTrackTuningChooserHandler handler, TGTrackTuningModel model) {
		final UIFactory uiFactory = this.tuningDialog.getUIFactory();
		final UITableLayout dialogLayout = new UITableLayout();
		final UIWindow dialog = uiFactory.createWindow(this.tuningDialog.getDialog(), true, false);

		dialog.setLayout(dialogLayout);
		dialog.setText(TuxGuitar.getProperty("tuning"));

		//-------------MAIN PANEL-----------------------------------------------
		UITableLayout panelLayout = new UITableLayout();
		UILegendPanel panel = uiFactory.createLegendPanel(dialog);
		panel.setLayout(panelLayout);
		panel.setText(TuxGuitar.getProperty("tuning"));
		dialogLayout.set(panel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		// value
		UILabel tuningValueLabel = uiFactory.createLabel(panel);
		tuningValueLabel.setText(TuxGuitar.getProperty("tuning.value") + ":");
		panelLayout.set(tuningValueLabel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_RIGHT, false, true);

		// the list offers the note names, the spinner below edits the step value
		final UIDropDownSelect<Integer> tuningValueControl = uiFactory.createDropDownSelect(panel);
		tuningValueControl.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("tuning.value.select")));

		for(int value = TGMusicKeyUtils.MIN_MIDI_NOTE ; value <= TGMusicKeyUtils.MAX_MIDI_NOTE ; value ++) {
			tuningValueControl.addItem(new UISelectItem<Integer>(TGEdoNoteNames.fullName(TuxGuitar.getInstance().getContext(), value), value));
		}

		tuningValueControl.setSelectedValue(model != null ? model.getValue() : null);
		panelLayout.set(tuningValueControl, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 150f, null, null);

		// value spinner
		UILabel tuningSpinnerLabel = uiFactory.createLabel(panel);
		tuningSpinnerLabel.setText(TuxGuitar.getProperty("tuning.step-value") + ":");
		panelLayout.set(tuningSpinnerLabel, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_RIGHT, false, true);

		final UISpinner tuningValueSpinner = uiFactory.createSpinner(panel);
		tuningValueSpinner.setMinimum(TGMusicKeyUtils.MIN_MIDI_NOTE);
		tuningValueSpinner.setMaximum(TGMusicKeyUtils.MAX_MIDI_NOTE);
		tuningValueSpinner.setValue(model != null ? model.getValue() : 0);
		final int[] currentValue = new int[] { (model != null ? model.getValue() : -1) };
		panelLayout.set(tuningValueSpinner, 2, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 150f, null, null);

		// label
		UILabel tuningLabelLabel = uiFactory.createLabel(panel);
		tuningLabelLabel.setText(TuxGuitar.getProperty("tuning.label") + ":");
		panelLayout.set(tuningLabelLabel, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_RIGHT, false, true);

		final UIReadOnlyTextField tuningLabelControl = uiFactory.createReadOnlyTextField(panel);
		if( model != null ) {
			tuningLabelControl.setText(TGEdoNoteNames.fullName(TuxGuitar.getInstance().getContext(), model.getValue()));
		}
		panelLayout.set(tuningLabelControl, 3, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 150f, null, null);

		tuningValueControl.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				int noteValue = -1;
				if( tuningValueControl.getSelectedValue() == null ) {
					// 1st item of list selected ("tuning.value.select")
					buttonOK.setEnabled(false);
				} else {
					noteValue = tuningValueControl.getSelectedValue();
					buttonOK.setEnabled(true);
				}
				String noteName = TGEdoNoteNames.fullName(TuxGuitar.getInstance().getContext(), noteValue);
				if (noteName != null) {
					tuningLabelControl.setText(noteName);
					tuningValueSpinner.setValue(tuningValueControl.getSelectedValue());
					currentValue[0] = noteValue;
				}
			}
		});
		tuningValueSpinner.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				currentValue[0] = tuningValueSpinner.getValue();
				tuningValueControl.setSelectedValue(currentValue[0]);
				tuningLabelControl.setText(TGEdoNoteNames.fullName(TuxGuitar.getInstance().getContext(), currentValue[0]));
				buttonOK.setEnabled(true);
			}
		});

		//------------------BUTTONS--------------------------
		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttons = uiFactory.createPanel(dialog, false);
		buttons.setLayout(buttonsLayout);
		dialogLayout.set(buttons, 3, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true);

		this.buttonOK = uiFactory.createButton(buttons);
		this.buttonOK.setText(TuxGuitar.getProperty("ok"));
		this.buttonOK.setEnabled(tuningValueControl.getSelectedValue() == null ? false : true);
		this.buttonOK.setDefaultButton();
		this.buttonOK.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGTrackTuningModel model = new TGTrackTuningModel();
				model.setValue(currentValue[0] >= 0 ? currentValue[0] : tuningValueControl.getSelectedValue());
				handler.handleSelection(model);
				dialog.dispose();
			}
		});
		buttonsLayout.set(this.buttonOK, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);

		UIButton buttonCancel = uiFactory.createButton(buttons);
		buttonCancel.setText(TuxGuitar.getProperty("cancel"));
		buttonCancel.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				dialog.dispose();
			}
		});
		buttonsLayout.set(buttonCancel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);
		buttonsLayout.set(buttonCancel, UITableLayout.MARGIN_RIGHT, 0f);

		TGDialogUtil.openDialog(dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}

}
