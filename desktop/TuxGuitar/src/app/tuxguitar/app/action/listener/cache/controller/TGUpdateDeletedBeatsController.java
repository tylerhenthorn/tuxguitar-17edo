package app.tuxguitar.app.action.listener.cache.controller;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.component.tab.Caret;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.note.TGDeleteNoteOrRestAction;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGBeatRange;
import app.tuxguitar.util.TGContext;

/**
 * Delete usually changes a few beats, but it removes whole measures when they are empty:
 * then the song is updated, the selection dropped and the caret kept inside the song.
 */
public class TGUpdateDeletedBeatsController extends TGUpdateItemsController {

	@Override
	public void update(final TGContext context, TGActionContext actionContext) {
		if( Boolean.TRUE.equals(actionContext.getAttribute(TGDeleteNoteOrRestAction.ATTRIBUTE_MEASURES_REMOVED)) ) {
			final TGSong tgSong = (TGSong) actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
			final TGSongManager tgSongManager = (TGSongManager) actionContext.getAttribute(TGSongManager.class.getName());

			this.findUpdateBuffer(context, actionContext).requestUpdateSong();
			this.findUpdateBuffer(context, actionContext).doPostUpdate(new Runnable() {
				public void run() {
					int measureCount = tgSong.countMeasureHeaders();
					Tablature tablature = TuxGuitar.getInstance().getTablatureEditor().getTablature();
					Caret caret = tablature.getCaret();
					tablature.getSelector().clearSelection();
					if( caret.getMeasure().getNumber() > measureCount ){
						TGTrack track = tgSongManager.getTrack(tgSong, caret.getTrack().getNumber());
						TGMeasure measure = tgSongManager.getTrackManager().getMeasure(track, measureCount);
						caret.update(track.getNumber(), measure.getStart(), caret.getStringNumber());
					}
				}
			});
		} else {
			TGBeatRange beats = actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT_RANGE);
			for (TGBeat beat : beats.getBeats()) {
				this.findUpdateBuffer(context, actionContext).requestUpdateMeasure(beat.getMeasure().getNumber());
			}
		}
		super.update(context, actionContext);
	}
}
