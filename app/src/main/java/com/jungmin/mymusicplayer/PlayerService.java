package com.jungmin.mymusicplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FilenameFilter;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener{
    private static final int REQ_START = 1001;
    private static final int REQ_FOREGROUND = 1002;
    private static final int REQ_STOP = 1003;
    private static final int REQ_SEEK = 1004;
    private static final int REQ_STOPFOREGROUND = 1005;
    private static final int ERR_CODE = -1;
    private static final int REQ_LIST_START = 1006;
    private static final int NOTI_ID = 9281;

    private static final String PLAY = "com.jungmin.action.play";
    private static final String PAUSE = "com.jungmin.action.pause";
    private static final String STOP = "com.jungmin.action.stop";
    private static final String REWIND = "com.jungmin.action.rewind";

    private Thread progressbarUpdateThread;
    private NotificationCompat.Builder myBuilder;
    private RemoteViews myRemoteView;
    private String sdPath;
    private int currentMedia;
    private String[] mediaList;
    private State state = null;
    private MediaPlayer myPlayer = null;
    private NotificationManager notificationManager;
    private BroadcastReceiver receiver;
    private IPlayerService.Stub myBinder = new IPlayerService.Stub() {
        @Override
        public void Pause() throws RemoteException {
            state.pause();
        }

        @Override
        public void Rewind() throws RemoteException {
            state.rewind();
        }

        @Override
        public int getMax() throws RemoteException {
            return maxPosition();
        }

        @Override
        public int getPosition() throws RemoteException {
            return currentPosition();
        }

        @Override
        public String getFileName() throws RemoteException {
            return mediaList[currentMedia];
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            try{
                if(myPlayer.isPlaying())
                    return true;
            }catch (Exception e){}
            return false;
        }
    };

    public int maxPosition() {
        try {
            if (myPlayer.isPlaying())
                return myPlayer.getDuration();
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    public int currentPosition() {
        try {
            if (myPlayer.isPlaying())
                return myPlayer.getCurrentPosition();
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentMedia = 0;
        state = NoInstanceState.getInstance(this);
        sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File sdRoot = new File(sdPath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        };
        mediaList = sdRoot.list(filter);
        Log.i("jungmin", "PlayList : " + String.valueOf(mediaList.length));
        if (mediaList.length <= 0) {
            Toast.makeText(getApplicationContext(), "재생할 파일이 없습니다.", Toast.LENGTH_LONG).show();
        }
        progressbarUpdateThread = new Thread("update thread"){
            @Override
            public void run(){
                while(true){
                    int max = maxPosition();
                    int position = currentPosition();
                    //Log.i("progressbar", String.valueOf(max) + " " +String.valueOf(position));
                    if(max >= 0 && position >= 0)
                        updateProgress(max, position);
                    try{Thread.sleep(500);}catch (InterruptedException e){break;}
                }
            }
        };
        progressbarUpdateThread.start();
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(PLAY)){Log.v("jungmin", "PLAY_BROADCAST");
                    if (mediaList.length <= 0) {
                        Toast.makeText(getApplicationContext(), "There's no file to Play", Toast.LENGTH_LONG).show();
                        return;
                    }
                    state.start();
                }else if(action.equals(PAUSE)){
                    Log.v("jungmin", "PAUSE_BROADCAST");
                    state.pause();
                }else if(action.equals(STOP)){
                    Log.v("jungmin", "STOP_BROADCAST");
                    state.stop();
                    myRemoteView.setTextViewText(R.id.noti_text, "");
                    myBuilder.setContentText("No Music....");
                    notificationManager.notify(NOTI_ID, myBuilder.build());
                    updateProgress(100, 0);
                }else if(action.equals(REWIND)){
                    Log.v("jungmin", "REWIND_BROADCAST");
                    state.rewind();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY);
        intentFilter.addAction(PAUSE);
        intentFilter.addAction(STOP);
        intentFilter.addAction(REWIND);
        registerReceiver(receiver, intentFilter);
        setupNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        if (currentMedia >= mediaList.length) currentMedia = 0;
        switch (intent.getIntExtra("request", ERR_CODE)) {
            case REQ_START: {
                Log.i("jungmin", "REQ_START, Current Media Index : " + String.valueOf(currentMedia));
                if (mediaList.length <= 0) {
                    Toast.makeText(getApplicationContext(), "There's no file to Play", Toast.LENGTH_LONG).show();
                    return START_STICKY;
                }
                state.start();
                break;
            }
            case REQ_LIST_START:{
                currentMedia = findSongIndex(intent.getStringExtra("fileName"));
                Log.i("jungmin", "REQ_START, Current Media Index : " + String.valueOf(currentMedia));
                if (mediaList.length <= 0) {
                    Toast.makeText(getApplicationContext(), "There's no file to Play", Toast.LENGTH_LONG).show();
                    return START_STICKY;
                }
                state.start(intent.getStringExtra("fileName"));
                break;
            }
            case REQ_STOP: {
                Log.i("jungmin", "REQ_STOP");
                state.stop();
                myRemoteView.setTextViewText(R.id.noti_text, "");
                myBuilder.setContentText("No Music....");
                notificationManager.notify(NOTI_ID, myBuilder.build());
                updateProgress(100, 0);
                break;
            }
            case REQ_SEEK: {
                Log.i("jungmin", "REQ_SEEK");
                state.seek(intent.getIntExtra("position", 0));
                break;
            }
            case REQ_FOREGROUND: {
                startForeground(NOTI_ID, myBuilder.build());

                try{
                    if(!myPlayer.isPlaying())
                        updateSmallIcon("stop");
                    else
                        updateSmallIcon("play");
                }catch (Exception e){}
                break;
            }
            case REQ_STOPFOREGROUND: {
                Log.i("jungmin", "REQ_STOPFOREGROUND");
                try {stopForeground(true);}catch (Exception e){}
                try{unregisterReceiver(receiver);}catch (Exception e){}
                try {myPlayer.release();} catch (Exception e) {}
                try {progressbarUpdateThread.interrupt();}catch (Exception e){}
                //stopSelf();
                System.exit(0);
                break;
            }
            case ERR_CODE: {
                Log.e("jungmin", "RECEIVED ERRCODE");
                break;
            }

        }

        return START_STICKY;
    }

    private void setupNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

        Intent playIntent = new Intent(PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, NOTI_ID , playIntent ,0);

        Intent pauseIntent = new Intent(PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this, NOTI_ID , pauseIntent ,0);

        Intent rewindIntent = new Intent(REWIND);
        PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(this, NOTI_ID , rewindIntent ,0);

        Intent stopIntent = new Intent(STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, NOTI_ID , stopIntent ,0);

        myRemoteView = new RemoteViews(this.getPackageName(), R.layout.notification);
        myRemoteView.setImageViewResource(R.id.imageView, R.drawable.musicplayer);

        myRemoteView.setImageViewResource(R.id.noti_play_Button, R.drawable.play);
        myRemoteView.setOnClickPendingIntent(R.id.noti_play_Button, playPendingIntent);

        myRemoteView.setImageViewResource(R.id.noti_pause_Button, R.drawable.pause);
        myRemoteView.setOnClickPendingIntent(R.id.noti_pause_Button, pausePendingIntent);

        myRemoteView.setImageViewResource(R.id.noti_rewind_Button, R.drawable.rewind);
        myRemoteView.setOnClickPendingIntent(R.id.noti_rewind_Button, rewindPendingIntent);

        myRemoteView.setImageViewResource(R.id.noti_stop_Button, R.drawable.stop);
        myRemoteView.setOnClickPendingIntent(R.id.noti_stop_Button, stopPendingIntent);

        myBuilder = new NotificationCompat.Builder(this);
        if(playing())
            myBuilder.setSmallIcon(R.drawable.play);
        else
            myBuilder.setSmallIcon(R.drawable.stop);
        myBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.musicplayer));
        myBuilder.setContentTitle("My Music Player");
        myBuilder.setContentText("My Music");
        myBuilder.setContent(myRemoteView);
        myBuilder.setContentIntent(mainPendingIntent);
    }

    @Override
    public void onDestroy() {
        try {stopForeground(true);}catch (Exception e){}
        try {unregisterReceiver(receiver);}catch (Exception e){}
        try {myPlayer.release();} catch (Exception e) {}
        try {progressbarUpdateThread.interrupt();}catch (Exception e){}
        Log.i("jungmin", "Service onDestroy()");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("jungmin", "onBind()");
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("jungmin", "onUnBind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.i("jungmin", "onPrepared(), Music will start");
        myBuilder.setContentText(getCurrentMediaPath());
        myRemoteView.setTextViewText(R.id.noti_text, getCurrentMediaPath());
        notificationManager.notify(NOTI_ID, myBuilder.build());
        mediaPlayer.start();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d("jungmin", "Next Song");
        state.stop();
        this.currentMedia++;
        if (currentMedia >= mediaList.length)
            currentMedia = 0;
        state.start();
    }

    public MediaPlayer getMyPlayer() {
        return myPlayer;
    }

    public String getCurrentMediaPath() {
        return this.mediaList[currentMedia];
    }

    public void setState(State state) {
        this.state = state;
    }

    public void InitializePlayer() {
        try{myPlayer.release();}catch (Exception e){}
        myPlayer = new MediaPlayer();
        myPlayer.setOnCompletionListener(this);
        myPlayer.setOnPreparedListener(this);
        myPlayer.setOnSeekCompleteListener(this);
    }

    public void updateSmallIcon(String what) {
        if (what.equals("play")) {
            myBuilder.setSmallIcon(R.drawable.play);
        } else if (what.equals("pause")) {
            myBuilder.setSmallIcon(R.drawable.pause);
        } else if (what.equals("stop")) {
            myBuilder.setSmallIcon(R.drawable.stop);
        }
        notificationManager.notify(NOTI_ID, myBuilder.build());
    }

    public void updateProgress(int max, int position){
        myRemoteView.setProgressBar(R.id.progressbar, max, position, false);
        notificationManager.notify(NOTI_ID, myBuilder.build());
    }

    private int findSongIndex(String string){
        for(int i = 0 ; i< mediaList.length; i++){
            if(mediaList[i].equals(string))
                return i;
        }
        return -1;
    }

    private boolean playing(){
        try{
            if(myPlayer.isPlaying())
                return true;
        }catch (Exception e){return false;}
        return  false;
    }

    public void updateNotifyPauseIcon(boolean bool){
        if(bool) {
            myRemoteView.setImageViewResource(R.id.noti_pause_Button, R.drawable.pause);
            sendBroadcast(new Intent("NOTI_RESUME"));
        }
        else{
            myRemoteView.setImageViewResource(R.id.noti_pause_Button, R.drawable.play);
            sendBroadcast(new Intent("NOTI_PAUSE"));
        }
        notificationManager.notify(NOTI_ID, myBuilder.build());
    }
}