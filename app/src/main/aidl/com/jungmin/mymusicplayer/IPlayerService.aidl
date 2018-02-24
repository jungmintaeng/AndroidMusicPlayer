// IPlayerService.aidl
package com.jungmin.mymusicplayer;

// Declare any non-default types here with import statements

interface IPlayerService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void Pause();
    void Rewind();
    int getMax();
    int getPosition();
    String getFileName();
    boolean isPlaying();
}
