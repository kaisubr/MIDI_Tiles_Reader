package io.github.kaisubr.miditiles;

import android.support.annotation.NonNull;

public class Chord implements Comparable{
    private final long tick;
    private final long deltaBefore;
    private final long deltaToNext;
    private final int numNotes;

    /**
     * A Chord is basically a NoteEvent with a few distinctions:
     *  > a chord is a combination of several MIDI events
     *  > even if numNotes = 1, there should be no association with its NoteEvent
     *  > there is no MIDI message associated with a Chord
     *
     * @param numNotes number of NoteEvents associated with the chord
     * @param tick this is the start tick.
     * @param deltaBefore number of ticks between (n-1) and (n). Usually very small, unless a rest is present.
     * @param deltaToNext number of ticks between (n) and (n+1). Depicts length of the note.
     */
    public Chord(int numNotes, long tick, long deltaBefore, long deltaToNext) {
        this.numNotes = numNotes;
        this.tick = tick;
        this.deltaBefore = deltaBefore;
        this.deltaToNext = deltaToNext;
    }

    public long getTick() {
        return tick;
    }

    public long getDeltaBefore() {
        return deltaBefore;
    }

    public long getDeltaToNext() {
        return deltaToNext;
    }

    public int getNumNotes() {
        return numNotes;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return (int) (((NoteEvent)o).getTick() - getTick());
    }
}
