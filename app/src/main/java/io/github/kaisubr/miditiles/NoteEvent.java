package io.github.kaisubr.miditiles;

import android.support.annotation.NonNull;
import com.leff.midi.event.MidiEvent;

public class NoteEvent extends MidiEvent {

    private long deltaToNext;

    /**
     * Creates a NoteEvent. NoteEvents are recommended to have size=4 and should be an instanceof NoteOn.
     *
     * Considering this event to be the nth 'note' (either an actual note or a rest) in the composition:
     * @param tick starting tick of this note.
     * @param deltaBefore number of ticks between (n-1) and (n). Usually very small, unless a rest is present.
     * @param deltaToNext number of ticks between (n) and (n+1). Depicts length of the note.
     */
    public NoteEvent(long tick, long deltaBefore, long deltaToNext) {
        super(tick, deltaBefore);
        this.deltaToNext = deltaToNext;
    }

    public long getDeltaToNext() {
        return deltaToNext;
    }

    public void setDeltaToNext(long deltaToNext) {
        this.deltaToNext = deltaToNext;
    }

    @Override
    protected int getEventSize() {
        return getSize();
    }

    @Override
    public int compareTo(@NonNull MidiEvent midiEvent) {
        return (int) (midiEvent.getTick() - getTick());
    }

    @Override
    public boolean equals(Object o) {
        return ((NoteEvent)o).getTick() - getTick() == 0;
    }
}
