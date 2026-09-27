package app.tuxguitar.io.ascii;

import app.tuxguitar.graphics.control.TGFretLabelFormatter;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.io.base.TGFileFormat;
import app.tuxguitar.io.base.TGFileFormatException;
import app.tuxguitar.io.base.TGSongWriter;
import app.tuxguitar.io.base.TGSongWriterHandle;

public class ASCIISongWriter implements TGSongWriter {

	private TGContext context;

	public ASCIISongWriter() {
		this(null);
	}

	public ASCIISongWriter(TGContext context) {
		super();
		this.context = context;
	}

	public TGFileFormat getFileFormat() {
		return new TGFileFormat("ASCII", "text/x-tab", new String[]{"tab"});
	}

	public void write(TGSongWriterHandle handle) throws TGFileFormatException {
		try{
			ASCIITabOutputStream stream = new ASCIITabOutputStream(handle.getOutputStream());
			stream.setFretLabelFormatter(TGFretLabelFormatter.getInstance(this.context));
			stream.writeSong(handle.getSong());
		}catch(Throwable throwable){
			throw new TGFileFormatException(throwable);
		}
	}
}