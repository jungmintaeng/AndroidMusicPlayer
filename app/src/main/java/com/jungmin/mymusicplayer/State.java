package com.jungmin.mymusicplayer;

import android.media.MediaPlayer;

/**
 * Created by 신정민 on 2016-11-23.
 */
public interface State {
    public void start(String fileName);
    public void start();
    public void stop();
    public void pause();
    public void rewind();
    public void seek(int position);
}
