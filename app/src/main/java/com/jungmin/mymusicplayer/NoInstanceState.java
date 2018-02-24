package com.jungmin.mymusicplayer;

import android.app.Service;
import android.media.MediaPlayer;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 신정민 on 2016-11-23.
 */
public class NoInstanceState implements State {
    private static PlayerService service = null;
    private static MediaPlayer player = null;
    private static volatile NoInstanceState noInstance = null;

    private NoInstanceState(PlayerService service){this.service = service;}

    public static State getInstance(PlayerService newService){
        if(service != newService)
            service = newService;
        if(noInstance == null)
            synchronized (NoInstanceState.class){
                if(noInstance == null)
                    noInstance = new NoInstanceState(newService);
            }
        return noInstance;
    }

    @Override
    public void start() {
        service.InitializePlayer();
        player = service.getMyPlayer();
        service.setState(PlayingState.getInstance(service));
        service.updateSmallIcon("play");
        try{
            Log.d("jungmin", "NoInstanceState...play : " + service.getCurrentMediaPath());
            player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + service.getCurrentMediaPath());
            player.prepare();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void start(String fileName) {
        service.InitializePlayer();
        player = service.getMyPlayer();
        service.setState(PlayingState.getInstance(service));
        service.updateSmallIcon("play");
        try{
            Log.d("jungmin", "NoInstanceState...play : " + service.getCurrentMediaPath());
            player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName);
            player.prepare();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void pause() {
        Log.e("jungmin", "Pause is Called in NoInstanceState");
    }

    @Override
    public void stop(){
        Log.e("jungmin", "Stop is Called in NoInstanceState");
    }

    @Override
    public void rewind() {
        Log.e("jungmin", "rewind is Called in NoInstanceState");
    }

    @Override
    public void seek(int position) {Log.e("jungmin", "seek is Called in NoInstanceState");}
}
