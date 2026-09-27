package app.tuxguitar.player.base;

public interface MidiSynthesizer {

	public MidiChannel openChannel(int channelId) throws MidiPlayerException;

	public void closeChannel(MidiChannel midiChannel) throws MidiPlayerException;

	public boolean isChannelOpen(MidiChannel midiChannel) throws MidiPlayerException;

	public boolean isBusy() throws MidiPlayerException;

	/** Raw System Exclusive message (with the leading F0 and trailing F7). Ports that cannot deliver it ignore it. */
	default void sendSysex(byte[] data) throws MidiPlayerException {
	}
}
