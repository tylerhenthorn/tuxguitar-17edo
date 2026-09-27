package app.tuxguitar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.note.TGDeleteNoteOrRestAction;
import app.tuxguitar.song.managers.TGMeasureManager;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGBeatRange;
import app.tuxguitar.util.TGContext;

public class TestDeleteRestOrMeasure {

	private TGActionManager actionManager;
	private TGSongManager songManager;
	private TGSong song;
	private TGTrack track;

	public TestDeleteRestOrMeasure() {
		TGContext context = new TGContext();
		this.actionManager = TGActionManager.getInstance(context);
		this.actionManager.mapAction(TGDeleteNoteOrRestAction.NAME, new TGDeleteNoteOrRestAction(context));
		this.songManager = new TGSongManager();
		this.song = this.songManager.newSong();
		for (int i = 0; i < 3; i++) {
			this.songManager.addNewMeasureBeforeEnd(this.song);
		}
		this.track = this.song.getTrack(0);
		for (int i = 0; i < this.track.countMeasures(); i++) {
			this.songManager.getMeasureManager().autoCompleteSilences(this.track.getMeasure(i));
		}
	}

	private void addNote(TGMeasure measure, long start, int fret) {
		TGDuration duration = this.songManager.getFactory().newDuration();
		duration.setValue(TGDuration.QUARTER);
		TGNote note = this.songManager.getFactory().newNote();
		note.setValue(fret);
		note.setString(1);
		this.songManager.getMeasureManager().addNote(measure, start, note, duration, 0);
	}

	private TGActionContext delete(List<TGBeat> beats) {
		TGActionContext context = new TGActionContext() {
		};
		TGBeat beat = beats.get(0);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG_MANAGER, this.songManager);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, this.song);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT, beat);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_VOICE, beat.getVoice(0));
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, this.track.getString(1));
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT_RANGE, new TGBeatRange(beats));
		this.actionManager.execute(TGDeleteNoteOrRestAction.NAME, context);
		return context;
	}

	private List<TGBeat> beatsOf(int... measureIndexes) {
		List<TGBeat> beats = new ArrayList<TGBeat>();
		for (int index : measureIndexes) {
			beats.addAll(this.track.getMeasure(index).getBeats());
		}
		return beats;
	}

	@Test
	public void testDeleteEmptyMeasure() {
		TGMeasure third = this.track.getMeasure(2);
		this.addNote(third, third.getStart(), 5);
		assertEquals(4, this.song.countMeasureHeaders());

		TGActionContext context = this.delete(beatsOf(1));
		assertTrue(Boolean.TRUE.equals(context.getAttribute(TGDeleteNoteOrRestAction.ATTRIBUTE_MEASURES_REMOVED)));
		assertEquals(3, this.song.countMeasureHeaders());
		assertEquals(3, this.track.countMeasures());
		// the measure holding the note moved up
		assertEquals(5, this.track.getMeasure(1).getBeat(0).getVoice(0).getNote(0).getValue());
		assertEquals(2, this.track.getMeasure(1).getNumber());
	}

	@Test
	public void testDeleteSelectedEmptyMeasures() {
		TGMeasure first = this.track.getMeasure(0);
		this.addNote(first, first.getStart(), 7);

		this.delete(beatsOf(1, 2, 3));
		assertEquals(1, this.song.countMeasureHeaders());
		assertEquals(7, this.track.getMeasure(0).getBeat(0).getVoice(0).getNote(0).getValue());
	}

	@Test
	public void testLastMeasureIsKept() {
		this.delete(beatsOf(0, 1, 2, 3));
		assertEquals(1, this.song.countMeasureHeaders());
		this.delete(beatsOf(0));
		assertEquals(1, this.song.countMeasureHeaders());
	}

	@Test
	public void testMeasureUsedByAnotherTrackIsKept() {
		TGTrack other = this.songManager.addTrack(this.song);
		TGMeasure measure = other.getMeasure(1);
		TGMeasureManager measureManager = this.songManager.getMeasureManager();
		measureManager.autoCompleteSilences(measure);
		this.addNote(measure, measure.getStart(), 3);

		TGActionContext context = this.delete(beatsOf(1));
		assertFalse(Boolean.TRUE.equals(context.getAttribute(TGDeleteNoteOrRestAction.ATTRIBUTE_MEASURES_REMOVED)));
		assertEquals(4, this.song.countMeasureHeaders());
	}

	@Test
	public void testDeleteRestPullsNotesLeft() {
		// rest, rest, note, rest: deleting the first rest moves the note one quarter earlier
		TGMeasure first = this.track.getMeasure(0);
		this.addNote(first, first.getStart(), 1);
		this.addNote(first, first.getStart() + (2 * TGDuration.QUARTER_TIME), 9);
		TGMeasureManager measureManager = this.songManager.getMeasureManager();
		measureManager.removeNote(first, first.getStart(), 0, 1);
		this.songManager.updatePreciseStart(this.track);

		TGBeat rest = measureManager.getFirstBeat(first.getBeats());
		assertTrue(rest.isRestBeat());
		List<TGBeat> beats = new ArrayList<TGBeat>();
		beats.add(rest);
		this.delete(beats);

		assertEquals(4, this.song.countMeasureHeaders());
		TGBeat moved = measureManager.getBeat(first, first.getStart() + TGDuration.QUARTER_TIME);
		assertEquals(9, moved.getVoice(0).getNote(0).getValue());
	}
}
