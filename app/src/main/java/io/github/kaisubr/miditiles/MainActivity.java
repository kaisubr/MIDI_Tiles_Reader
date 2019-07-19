package io.github.kaisubr.miditiles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final int PRQ_WRITE_EXTERNAL_STORAGE = 113;
    public static final String DIR_EXT = "~DIR";
    public static final int DRQ = 120;
    private boolean granted;
    private Button bSearch, bLaunch;
    private ListView lView;
    private String selectedDir;
    private String selectedName;
    private ArrayList<File> midiInDir;
    private TextView tvDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("test", String.format("%.2f", (100.*11/12)));
        requestFilePermission();
        bSearch = (Button) findViewById(R.id.b_search);
        bSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.putExtra("android.content.extra.SHOW_ADVANCED",true);
                startActivityForResult(Intent.createChooser(i, "Choose directory"), DRQ);
            }
        });

        bLaunch = (Button) findViewById(R.id.b_launch);
        bLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), PlayMidi.class);
                intent.putExtra("EXTRA_FILE", selectedDir + "/" + selectedName);
                Log.d("main", " sending " + selectedDir +"/" + selectedName);
                startActivity(intent);

            }
        });

        lView = (ListView) findViewById(R.id.listView);
        lView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lView.setEmptyView(findViewById(R.id.empty));

        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                for(int c = 0; c < adapterView.getChildCount(); c++) {
                    adapterView.getChildAt(c).setBackgroundColor(Color.TRANSPARENT);
                }

                view.setBackgroundColor(Color.LTGRAY);
                selectedName = ((TextView)view).getText().toString();
                tvDisplay.setText("[" + selectedName + "]");
                bLaunch.setEnabled(true);
            }
        });

        lView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                for(int c = 0; c < absListView.getChildCount(); c++) {
                    absListView.getChildAt(c).setBackgroundColor(Color.TRANSPARENT);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });

        if (granted) {
            drawList("/");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case DRQ:
                Log.i("Test", "Result URI " + data.getData());
                Uri uri = data.getData();
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                        DocumentsContract.getTreeDocumentId(uri));
                String path = docUri.getPath();
                path = path.substring(path.lastIndexOf(":") + 1);
                Log.i("main ","Converted to " + path);
                drawList("/" + path);
                break;
        }
    }

    private void drawList(String pt) {
        String path = Environment.getExternalStorageDirectory().toString() + pt;
        Log.d("main", "Path: " + path);

        bSearch.setText(path);
        selectedDir = path;

        File directory = new File(path);
        File[] files = directory.listFiles();
        midiInDir = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            Log.d("main", "" + files[i].getName());

            String apth = files[i].getAbsolutePath(), ext = files[i].isFile()? apth.substring(apth.lastIndexOf(".") + 1).toUpperCase() : DIR_EXT;
            if (ext.equals("MID") || ext.equals("MIDI")) {
                Log.d("main", "\t\t --> it's midiInDir");
                midiInDir.add(files[i]);
            } else Log.d("main", "\t\t --> not midiInDir (ext " + ext);
        }

        //File[] midiFiles = new File[midiInDir.size()]; midiInDir.toArray(midiFiles);
        String[] stringMidiFiles = new String[midiInDir.size()];

        for (int i = 0; i < midiInDir.size(); i++) stringMidiFiles[i] = midiInDir.get(i).getName();
        Log.d("main", Arrays.toString(stringMidiFiles));

        if (midiInDir.size() == 0) bLaunch.setEnabled(false);

        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.activity_listview, stringMidiFiles);

        lView.setAdapter(adapter);

    }

    private void requestFilePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PRQ_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            granted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PRQ_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                } else {
                    granted = false;
                    Toast.makeText(MainActivity.this, "MIDI Tiles can't read MIDI files without the 'READ'/'WRITE_EXTERNAL_STORAGE' permission!",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

}
