package io.github.kaisubr.miditiles;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayMidi extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private File file;
    private MidiFile midiFile;
    private RelativeLayout rel1, rel2, rel3, rel4;
    private int width, height;
    private Composition composition; //the translated score
    private float tempo = -1f; //in bpm
    private boolean firstClicked;

    private int totalDropped;
    private int numberClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_play_midi);
        if (getSupportActionBar()!=null) getSupportActionBar().hide();

        String filePath = getIntent().getStringExtra("EXTRA_FILE");
        Log.d("play", filePath);

        file = new File(filePath);
        Toast.makeText(PlayMidi.this,"Playing " + file.getName() + " [" + file.length()/1000. + " kb]", Toast.LENGTH_LONG).show();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        rel1 = (RelativeLayout) findViewById(R.id.rl1);
        rel2 = (RelativeLayout) findViewById(R.id.rl2);
        rel3 = (RelativeLayout) findViewById(R.id.rl3);
        rel4 = (RelativeLayout) findViewById(R.id.rl4);

        try {
            midiFile = new MidiFile(file);
            playFile(midiFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void playFile(MidiFile midiFile) throws IOException {
        List<MidiTrack> tracks = midiFile.getTracks();

//        File tempFile = File.createTempFile(file.getName() + "temp", ".mid", getExternalCacheDir());
//
//        midiFile.writeToFile(tempFile);
//        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setDataSource(new FileInputStream(tempFile).getFD());
//        mediaPlayer.prepare();

        prepareMediaPlayer();

        if(!mediaPlayer.isPlaying()) {
            //wait for first tile to be clicked.
//            final Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (firstClicked) {
//                        mediaPlayer.start();
//                        Toast.makeText(PlayMidi.this, "Playing", Toast.LENGTH_SHORT).show();
//                        Log.d("main", "playing! ");
//                    } else handler.postDelayed(this, 1);
//                }
//            }, 0);

        } else {
            mediaPlayer.pause();
            Toast.makeText(PlayMidi.this, "Paused", Toast.LENGTH_SHORT).show();
        }

        MidiTrack tr = tracks.get(0);

        List<MidiEvent> events = new ArrayList<>();
        Iterator<MidiEvent> it = tr.getEvents().iterator();
        while (it.hasNext()) {
            MidiEvent ev = it.next();
            Log.d("mid", ev.toString() + " with tick " + ev.getTick() + " and size " + ev.getSize() + " and delta " + ev.getDelta());
            events.add(ev);
            if (ev instanceof Tempo) {
                Log.d("mid", "tempo " + ((Tempo)ev).getBpm());
                tempo = Math.max(tempo, ((Tempo)ev).getBpm());
            }
        }

        List<NoteEvent> noteEvents = new ArrayList<>();
        for (int n = 0; n < events.size(); n++) {
            if (events.get(n).getSize() == 4 && events.get(n) instanceof NoteOn) {
                long dtn = events.get(n+1).getDelta();  //the last note is instanceof EndOfTrack
                noteEvents.add(new NoteEvent(events.get(n).getTick(), events.get(n).getDelta(), dtn));
            }
        }

        for (NoteEvent nev : noteEvents) {
            Log.d("mid", "nevnt: start " + nev.getTick() + " | length " + nev.getDeltaToNext());
        }

        composition = new Composition(noteEvents);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                new AlertDialog.Builder(PlayMidi.this)
                        .setTitle("Congrats! Accuracy: " + String.format("%.2f", (100.*numberClicked/totalDropped)))
                        .setMessage("You got to the end! Try and beat your accuracy.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        readScore();
    }

    /**
     * Note that 480 ticks = 1 quarter-note (a beat) regardless of bpm.
     */
    private void readScore() {
        if (tempo == -1f) tempo = 120; //default

        final int[] cur = {0};

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Chord c = composition.getScore().get(cur[0]++);
                    dropView((int) (Math.random()*5), (int) ticksToMilliseconds(c.getTick()), ticksToMilliseconds(c.getDeltaToNext()));

                    if (cur[0] < composition.getScore().size())
                        handler.postDelayed(this, ticksToMilliseconds(c.getDeltaToNext()));
                } catch (Exception e) {
                    new AlertDialog.Builder(PlayMidi.this)
                            .setTitle("MIDI couldn't be read. ")
                            .setMessage("Unrecognized type or events found in file. Unfortunately, this app has been designed to work at a resolution of 480 ticks, typical of MuseScore exports, so this might be the problem.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }

            }
        }, 0);
    }

    private long ticksToMilliseconds(long tick) {
        //Sample: 480 tick = 1 beat. At 120 bpm --> 1 beat = 60/120 = 0.5 sec. Thus 480 tick = 0.5 sec = 500 ms.
        return (long) (1000 * ((tick*(60./tempo))/480.));
    }

    private void dropView(int col, final int startMsec, final long timeLength) { //height based on time length (in ms)
        View v = new View(PlayMidi.this);
        v.setBackgroundColor(Color.BLACK);
        totalDropped++;

        //let ticksToMs(480) = 1/4 of the screen
        int vHeight = (int) ((timeLength*(0.25 * height))/ticksToMilliseconds(480));
        int vWidth = width/4;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width/4, vHeight);
        params.leftMargin = 0;
        params.topMargin = -vHeight;

        switch (col) {
            case 1: rel1.addView(v, params); break;
            case 2: rel2.addView(v, params); break;
            case 3: rel3.addView(v, params); break;
            case 4: rel4.addView(v, params); break;
            default: rel1.addView(v, params); break;
        }

        ObjectAnimator animation = ObjectAnimator.ofFloat(v, "translationY", vHeight + height);
        //let 500 ms note = 2000 ms screen duration
        //int duration = (int) ((timeLength*2000)/500);
        animation.setDuration((long) (1*ticksToMilliseconds(480)));
        animation.setInterpolator(new LinearInterpolator());
        animation.start();

        v.setPivotX(vWidth/2);
        v.setPivotY(vHeight);
        //ObjectAnimator dis1 = ObjectAnimator.ofFloat(v, "scaleX", 0);
        ObjectAnimator dis2 = ObjectAnimator.ofFloat(v, "scaleY", 0);

        final AnimatorSet animationDisappear = new AnimatorSet();
        animationDisappear.setDuration(timeLength/4);
        animationDisappear.playTogether(/*dis1, */dis2);
        animationDisappear.setInterpolator(new LinearInterpolator());

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                try {
                    playPauseMedia(startMsec, timeLength);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!firstClicked) firstClicked = true;
                view.setBackgroundColor(Color.BLUE);
                numberClicked++;
                animationDisappear.start();
                //v.animate().scaleX(0).scaleY(0).setInterpolator(new LinearInterpolator()).setDuration(100).start();
                return false;
            }
        });

    }

    private void playPauseMedia(int startMsec, long timeLength) throws IOException {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        mediaPlayer.seekTo(startMsec - 50);
        mediaPlayer.start();

        if (timeLength > 480) {
//            final Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mediaPlayer.pause();
////
////                try {
////                    prepareMediaPlayer();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//                }
//            }, timeLength);
        }

    }

    private void prepareMediaPlayer() throws IOException {
        File tempFile = File.createTempFile(file.getName() + "temp", ".mid", getExternalCacheDir());

        midiFile.writeToFile(tempFile);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(new FileInputStream(tempFile).getFD());
        mediaPlayer.prepare();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}