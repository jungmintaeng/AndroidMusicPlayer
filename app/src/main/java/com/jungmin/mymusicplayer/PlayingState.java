package com.jungmin.mymusicplayer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import java.util.Stack;

public class PlayingState implements State {
    private static PlayerService service = null;
    private static MediaPlayer myPlayer = null;
    private static volatile PlayingState playingInstance = null;

    private PlayingState(PlayerService service){this.service = service;}

    public static State getInstance(PlayerService newService){
        if(service != newService)
            service = newService;
        if(playingInstance == null)
            synchronized (PlayingState.class){
                if(playingInstance == null)
                    playingInstance = new PlayingState(newService);
            }
        return playingInstance;
    }

    @Override
    public void start() {
        Log.v("jungmin", "PlayingState.start()");
        //Do Nothing
    }

    @Override
    public void start(String fileName) {
        service.InitializePlayer();
        myPlayer = service.getMyPlayer();
        try{
            Log.d("jungmin", "PlayingState...play : " + service.getCurrentMediaPath());
            myPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName);
            myPlayer.prepare();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void pause() {
        Log.v("jungmin", "PlayingState.pause()");
        int currentPosition;
        myPlayer = service.getMyPlayer();
        currentPosition = myPlayer.getCurrentPosition();
        myPlayer.pause();
        PausedState pausedState = (PausedState)PausedState.getInstance(service);
        pausedState.setPausedTime(currentPosition);
        service.setState(pausedState);
        service.updateSmallIcon("pause");
        service.updateNotifyPauseIcon(false);
    }

    @Override
    public void stop() {
        Log.v("jungmin", "PlayingState.stop()");
        myPlayer = service.getMyPlayer();
        myPlayer.release();
        service.setState(NoInstanceState.getInstance(service));
        service.updateSmallIcon("stop");
        service.sendBroadcast(new Intent("NOTI_STOP"));
    }

    @Override
    public void rewind() {
        Log.v("jungmin", "PlayingState.rewind()");
        myPlayer = service.getMyPlayer();
        int currentPosition = myPlayer.getCurrentPosition();
        int newPosition;
        if(currentPosition >= 10000)
            newPosition = currentPosition - 10000;
        else
            newPosition = 0;
        Log.i("jungmin", "Called PlayingState.rewind(), currentPosition is  " + String.valueOf(currentPosition) + "  and newPosition is  " + String.valueOf(newPosition));
        myPlayer.seekTo(newPosition);
        myPlayer.start();
    }

    @Override
    public void seek(int position) {
        myPlayer = service.getMyPlayer();
        myPlayer.seekTo(position);
    }
}
