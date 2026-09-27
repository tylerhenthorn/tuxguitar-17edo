package app.tuxguitar.editor.action.file;

import java.util.Iterator;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiTuning;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;

public class TGNewSongAction extends TGActionBase{

	public static final String NAME = "action.song.new-default";

	public TGNewSongAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context){
		TGSong tgSong = getSongManager(context).newSong();
		convertDefaultTunings(getContext(), tgSong);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, tgSong);

		TGActionManager tgActionManager = TGActionManager.getInstance(getContext());
		tgActionManager.execute(TGLoadSongAction.NAME, context);
	}

	/** New songs and templates are stored in 12-EDO; retune them when the EDO retune is on */
	public static void convertDefaultTunings(TGContext context, TGSong song) {
		MidiTuning tuning = MidiTuning.getInstance(context);
		Iterator<TGTrack> it = song.getTracks();
		while (it.hasNext()) {
			tuning.convertDefaultTuning(it.next());
		}
	}
}
