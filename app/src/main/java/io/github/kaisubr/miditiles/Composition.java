package io.github.kaisubr.miditiles;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Composition {
    private List<Chord> score;
    private List<NoteEvent> notes;

    /**
     * Makes a Composition. It is not currently configured to build a score by adding in NoteEvents one at a time, but
     * rather all of them at once. It's more efficient this way.
     * @param notes all the NoteEvents in the score.
     */
    public Composition(List<NoteEvent> notes) {
        this.notes = notes;
        this.score = new ArrayList<>();

        for (int n = 0; n < notes.size(); n++) {
            int num = 1, cur = n;
            if (cur+1 < notes.size()) Log.d("comp", " should be true for chord: " + notes.get(cur+1).equals(notes.get(n)));
            while (cur+1 < notes.size() && notes.get(cur+1).equals(notes.get(n))) {
                num++; cur++;
                Log.d("comp", "comp [" + num + "] " + ": start " + notes.get(n).getTick() + " | length " + notes.get(cur).getDeltaToNext());
            }

            if (notes.get(cur).getDeltaToNext() > 120) //16th note. Can't play grace notes on piano tiles!
                score.add(new Chord(num, notes.get(n).getTick(), notes.get(n).getDelta(), notes.get(cur).getDeltaToNext()));
            //skip next num notes
            n += num - 1; //num had started at 1
        }
    }

    public List<Chord> getScore() {
        return score;
    }

    public List<NoteEvent> getNotes() {
        return notes;
    }

}
