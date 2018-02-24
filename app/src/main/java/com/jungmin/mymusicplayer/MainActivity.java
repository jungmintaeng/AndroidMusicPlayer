package com.jungmin.mymusicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, ListView.OnItemClickListener{
    private static final int REQ_START = 1001;
    private static final int REQ_FOREGROUND = 1002;
    private static final int REQ_STOP = 1003;
    private static final int REQ_SEEK = 1004;
    private static final int REQ_STOPFOREGROUND = 1005;
    private static final int REQ_LIST_START = 1006;
    private IPlayerService myBinder;
    private static SeekBar seekBar;
    private Thread seekRequestThread;
    private ImageButton button;
    private TextView songName;
    private BroadcastReceiver receiver = null;
    ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i("jungmin", "Bind 완료!");
            myBinder = IPlayerService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i("jungmin", "unBind 완료!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
        serviceIntent.setPackage("com.jungmin.mymusicplayer");
        bindService(serviceIntent, myConnection, BIND_AUTO_CREATE);
        MyAdapter adapter = new MyAdapter(this);
        ListView song_list = (ListView)findViewById(R.id.song_list);
        song_list.setAdapter(adapter);
        song_list.setOnItemClickListener(this);
        song_list.setDivider(new ColorDrawable(Color.BLACK));
        song_list.setDividerHeight(1);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ImageButton pauseButton = (ImageButton)findViewById(R.id.pause_Button);
                if(intent.getAction().equals("NOTI_PAUSE")){
                    pauseButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
                }else if(intent.getAction().equals("NOTI_RESUME")){
                    pauseButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                }else if(intent.getAction().equals("NOTI_STOP")){
                    button = (ImageButton)findViewById(R.id.pause_Button);
                    songName = (TextView) findViewById(R.id.fileName_TextView);
                    songName.setText("");
                    seekBar = (SeekBar)findViewById(R.id.seekbar);
                    updateSeekBar(100, 0);
                    button.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("NOTI_PAUSE");
        filter.addAction("NOTI_RESUME");
        filter.addAction("NOTI_STOP");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        unbindService(myConnection);
        try {
            seekRequestThread.interrupt();
        } catch (Exception e) {
            Log.i("jungmin", "THREAD INTURRUPT");
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        button = (ImageButton) findViewById(R.id.play_Button);
        button.setOnTouchListener(this);
        button = (ImageButton) findViewById(R.id.stop_Button);
        button.setOnTouchListener(this);
        button = (ImageButton) findViewById(R.id.exit_Button);
        button.setOnTouchListener(this);
        button = (ImageButton) findViewById(R.id.rewind_Button);
        button.setOnTouchListener(this);
        button = (ImageButton) findViewById(R.id.pause_Button);
        button.setOnTouchListener(this);
        songName = (TextView) findViewById(R.id.fileName_TextView);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    Intent serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
                    serviceIntent.setPackage("com.jungmin.mymusicplayer");
                    serviceIntent.putExtra("request", REQ_SEEK);
                    serviceIntent.putExtra("position", i);
                    startService(serviceIntent);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        Intent foregroundIntent = new Intent("com.jungmin.service.MusicPlayer");
        foregroundIntent.setPackage("com.jungmin.mymusicplayer");
        foregroundIntent.putExtra("request", REQ_FOREGROUND);
        startService(foregroundIntent);
        startThread();
    }

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                v.setBackgroundColor(Color.GRAY);
                break;
            }
            case MotionEvent.ACTION_UP: {
                Intent serviceIntent;
                switch (v.getId()) {
                    case R.id.play_Button: {//시작버튼
                        serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
                        serviceIntent.setPackage("com.jungmin.mymusicplayer");
                        serviceIntent.putExtra("request", REQ_START);
                        startService(serviceIntent);
                        v.setBackgroundColor(Color.parseColor("#FFE082"));
                        break;
                    }
                    case R.id.stop_Button: {//정지버튼
                        serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
                        serviceIntent.setPackage("com.jungmin.mymusicplayer");
                        serviceIntent.putExtra("request", REQ_STOP);
                        startService(serviceIntent);
                        v.setBackgroundColor(Color.parseColor("#FFE082"));
                        break;
                    }
                    case R.id.pause_Button: {
/*                        try {
                            if (myBinder.isPlaying()) {
                                button.setImageDrawable(getResources().getDrawable(R.drawable.play));
                            } else {
                                button.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                            }
                        }catch (RemoteException e){e.printStackTrace();}*/
                        try {
                            myBinder.Pause();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        v.setBackgroundColor(Color.parseColor("#FFE082"));
                        break;
                    }
                    case R.id.rewind_Button: {
                        try {
                            myBinder.Rewind();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        v.setBackgroundColor(Color.parseColor("#FFE082"));
                        break;
                    }
                    case R.id.exit_Button: {
                        serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
                        serviceIntent.setPackage("com.jungmin.mymusicplayer");
                        serviceIntent.putExtra("request", REQ_STOPFOREGROUND);
                        startService(serviceIntent);
                        try {
                            seekRequestThread.interrupt();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        finish();
                        break;
                    }
                }
            }
        }
        return true;
    }

    private void updateSeekBar(int max, int position) {
        seekBar.setMax(max);
        seekBar.setProgress(position);
    }

    private void startThread() {
        try {
            seekRequestThread.interrupt();
        } catch (Exception e) {
            Log.i("jungmin", "START_THREAD");
        }
        seekRequestThread = new Thread("seek Thread") {
            @Override
            public void run() {
                while (true) {
                    try {
                        final int max = myBinder.getMax();
                        final int position = myBinder.getPosition();
                        if (max >= 0 && position >= 0)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateSeekBar(max, position);
                                    try {
                                        songName.setText(myBinder.getFileName());
                                    } catch (RemoteException e) {}
                                }
                            });
                        //Log.i("updating", String.valueOf(max) + " " + String.valueOf(position));
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {}
                }
            }
        };
        seekRequestThread.start();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        MyAdapter.ViewHolder holder = (MyAdapter.ViewHolder)view.getTag();
        String songName = holder.songNameTextView.getText().toString();
        Intent serviceIntent = new Intent("com.jungmin.service.MusicPlayer");
        serviceIntent.setPackage("com.jungmin.mymusicplayer");
        serviceIntent.putExtra("request", REQ_LIST_START);
        serviceIntent.putExtra("fileName", songName);
        startService(serviceIntent);
    }
}
