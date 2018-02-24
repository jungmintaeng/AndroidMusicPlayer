package com.jungmin.mymusicplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

/**
 * Created by 신정민 on 2016-11-23.
 */
public class PausedState implements State {
    private static PlayerService service = null;
    private static MediaPlayer myPlayer = null;
    private static volatile PausedState pausedInstance = null;
    private int currentTime = 0;
    private PausedState(PlayerService service){this.service = service;}
    public static State getInstance(PlayerService newService){
        if(service != newService)
            service = newService;
        if(pausedInstance == null)
            synchronized (PausedState.class){
                if(pausedInstance == null)
                    pausedInstance = new PausedState(newService);
            }
        return pausedInstance;
    }

    @Override
    public void start() {
        myPlayer = service.getMyPlayer();
        myPlayer.seekTo(0);
        service.updateNotifyPauseIcon(true);
        service.updateSmallIcon("play");
        service.setState(PlayingState.getInstance(service));
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
        service.updateNotifyPauseIcon(true);
        service.updateSmallIcon("play");
        service.setState(PlayingState.getInstance(service));
    }

    @Override
    public void pause() {
        myPlayer = service.getMyPlayer();
        myPlayer.start();
        service.setState(PlayingState.getInstance(service));
        service.updateSmallIcon("play");
        Log.d("jungmin", "Player Pause --> Resume. Current Time : " + String.valueOf(currentTime));
        service.updateNotifyPauseIcon(true);
    }

    @Override
    public void stop() {
        myPlayer.release();
        service.setState(NoInstanceState.getInstance(service));
        service.updateSmallIcon("stop");
        service.updateNotifyPauseIcon(true);
        service.sendBroadcast(new Intent("NOTI_STOP"));
    }

    @Override
    public void rewind() {
        myPlayer = service.getMyPlayer();
        if(currentTime >= 10000)
            myPlayer.seekTo(currentTime - 10000);
        else
            this.start();
        service.updateNotifyPauseIcon(true);
        service.setState(PlayingState.getInstance(service));
    }

    public void setPausedTime(int position){
        this.currentTime = position;
    }

    @Override
    public void seek(int position) {
        myPlayer = service.getMyPlayer();
        myPlayer.seekTo(position);
        service.updateNotifyPauseIcon(true);
        service.setState(PlayingState.getInstance(service));
    }

}
