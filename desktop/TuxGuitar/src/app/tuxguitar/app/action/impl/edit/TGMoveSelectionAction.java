package app.tuxguitar.app.action.impl.edit;

import java.util.List;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.action.impl.caret.TGMoveToAction;
import app.tuxguitar.app.view.component.tab.Selector;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.song.helpers.TGStoredBeatList;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;

/**
 * Drops the selected beats at the mouse position (Ctrl + drag): the selection is
 * emptied (its beats become rests) and its content replaces the beats found at the
 * destination, exactly as cut + paste would, without touching the clipboard.
 */
public class TGMoveSelectionAction extends TGPasteAction {

	public static final String NAME = "action.edit.move-selection";

	public TGMoveSelectionAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context){
		Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		Selector selector = tablature.getSelector();

		if (selector.isActive() && tablature.getEditorKit().fillSelection(context, true)) {
			List<TGBeat> beats = selector.getBeatRange().getBeats();
			TGBeat destinationBeat = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT);
			TGTrack destinationTrack = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK);

			if (!beats.isEmpty() && destinationBeat != null && destinationBeat != beats.get(0)) {
				TGTrack sourceTrack = beats.get(0).getMeasure().getTrack();
				if (sourceTrack.isPercussion() == destinationTrack.isPercussion()) {
					TGSongManager songManager = getSongManager(context);
					songManager.updatePreciseStart(sourceTrack);
					songManager.updatePreciseStart(destinationTrack);

					TGStoredBeatList beatList = new TGStoredBeatList(beats, sourceTrack.getStrings(), sourceTrack.isPercussion(), songManager.getFactory());
					long preciseStart = destinationBeat.getPreciseStart();

					for (TGBeat beat : beats) {
						this.clearBeat(beat);
					}
					this.pasteBeats(context, beatList, destinationTrack, preciseStart);

					// leave the caret on the first moved beat
					TGBeat first = selector.getStartBeat();
					if (selector.isActive() && first != null) {
						context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, first.getMeasure().getTrack());
						context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, first.getMeasure());
						context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT, first);
						context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_KEEP_SELECTION, true);
						TGActionManager.getInstance(getContext()).execute(TGMoveToAction.NAME, context);
					}
				}
			}
		}
	}

	private void clearBeat(TGBeat beat) {
		for (int i = 0; i < beat.countVoices(); i++) {
			beat.getVoice(i).clearNotes();
		}
		beat.removeChord();
		beat.removeText();
	}
}
