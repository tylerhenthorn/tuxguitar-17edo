package app.tuxguitar.app.view.toolbar.edit;

import app.tuxguitar.app.action.impl.insert.TGOpenTextDialogAction;
import app.tuxguitar.app.action.impl.note.TGOpenStrokeUpDialogAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.action.impl.note.TGOpenStrokeDownDialogAction;
import app.tuxguitar.editor.action.note.TGChangePickStrokeUpAction;
import app.tuxguitar.editor.action.note.TGChangePickStrokeDownAction;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGPickStroke;
import app.tuxguitar.song.models.TGStroke;
import app.tuxguitar.ui.toolbar.UIToolBar;
import app.tuxguitar.ui.toolbar.UIToolCheckableItem;

public class TGEditToolBarSectionBeat extends TGEditToolBarSection {

	private static final String SECTION_TITLE = "beat";

	private UIToolCheckableItem text;
	private UIToolCheckableItem strokeUp;
	private UIToolCheckableItem strokeDown;
	private UIToolCheckableItem pickStrokeUp;
	private UIToolCheckableItem pickStrokeDown;

	public TGEditToolBarSectionBeat(TGEditToolBar toolBar) {
		super(toolBar, SECTION_TITLE);
	}

	public void createSectionToolBars() {
		UIToolBar toolBar = this.createToolBar();

		this.text = toolBar.createCheckItem();
		this.text.addSelectionListener(this.createActionProcessor(TGOpenTextDialogAction.NAME));

		toolBar.createSeparator();

		this.strokeUp = toolBar.createCheckItem();
		this.strokeUp.addSelectionListener(this.createActionProcessor(TGOpenStrokeUpDialogAction.NAME));

		this.strokeDown = toolBar.createCheckItem();
		this.strokeDown.addSelectionListener(this.createActionProcessor(TGOpenStrokeDownDialogAction.NAME));

		toolBar = this.createToolBar();
		this.pickStrokeDown = toolBar.createCheckItem();
		this.pickStrokeDown.addSelectionListener(this.createActionProcessor(TGChangePickStrokeDownAction.NAME));

		this.pickStrokeUp = toolBar.createCheckItem();
		this.pickStrokeUp.addSelectionListener(this.createActionProcessor(TGChangePickStrokeUpAction.NAME));
	}

	public void loadSectionProperties() {
		this.text.setToolTipText(this.getText("text.insert"));
		this.strokeUp.setToolTipText(this.getText("beat.stroke-up"));
		this.strokeDown.setToolTipText(this.getText("beat.stroke-down"));
		this.pickStrokeUp.setToolTipText(this.getText("beat.pick-stroke-up"));
		this.pickStrokeDown.setToolTipText(this.getText("beat.pick-stroke-down"));
	}

	public void loadSectionIcons() {
		this.text.setImage(this.getIconManager().getImageByName(TGIconManager.TEXT));
		this.strokeUp.setImage(this.getIconManager().getImageByName(TGIconManager.STROKE_UP));
		this.strokeDown.setImage(this.getIconManager().getImageByName(TGIconManager.STROKE_DOWN));
		this.pickStrokeUp.setImage(this.getIconManager().getImageByName(TGIconManager.PICK_STROKE_UP));
		this.pickStrokeDown.setImage(this.getIconManager().getImageByName(TGIconManager.PICK_STROKE_DOWN));
	}

	public void updateSectionItems() {
		TGBeat beat = this.getTablature().getCaret().getSelectedBeat();
		boolean isPercussion = this.getTablature().getCaret().getTrack().isPercussion();

		boolean running = MidiPlayer.getInstance(this.getToolBar().getContext()).isRunning();

		this.text.setEnabled(!running);
		this.text.setChecked(beat.isTextBeat());
		this.strokeUp.setEnabled(!running && !beat.isRestBeat() && !isPercussion);
		this.strokeUp.setChecked( beat != null && beat.getStroke().getDirection() == TGStroke.STROKE_UP );
		this.strokeDown.setEnabled(!running && !beat.isRestBeat() && !isPercussion);
		this.strokeDown.setChecked( beat != null && beat.getStroke().getDirection() == TGStroke.STROKE_DOWN );
		this.pickStrokeUp.setEnabled(!running && !beat.isRestBeat());
		this.pickStrokeUp.setChecked( beat != null && beat.getPickStroke().getDirection() == TGPickStroke.PICK_STROKE_UP );
		this.pickStrokeDown.setEnabled(!running && !beat.isRestBeat());
		this.pickStrokeDown.setChecked( beat != null && beat.getPickStroke().getDirection() == TGPickStroke.PICK_STROKE_DOWN );
	}
}
