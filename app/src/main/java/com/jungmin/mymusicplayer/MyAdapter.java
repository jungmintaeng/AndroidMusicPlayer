package com.jungmin.mymusicplayer;

import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

public class MyAdapter extends BaseAdapter {
    String[] musicList;
    Context mContext;
    LayoutInflater myInflater;

    public MyAdapter(Context mContext){
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File sdRoot = new File(sdPath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        };
        String[] mediaList = sdRoot.list(filter);
        musicList = mediaList;
        this.mContext = mContext;
        myInflater = LayoutInflater.from(mContext);
    }

    public class ViewHolder{
        TextView songNameTextView;
    }

    @Override
    public int getCount() {
        return musicList.length;
    }

    @Override
    public Object getItem(int i) {
        return musicList[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View songLayout = convertView;
        ViewHolder holder;
        if(songLayout == null){
            songLayout = myInflater.inflate(R.layout.song_list, null);
            holder = new ViewHolder();
            holder.songNameTextView = (TextView)songLayout.findViewById(R.id.songNameTextView);
            songLayout.setTag(holder);
        }else
            holder = (ViewHolder)songLayout.getTag();

        holder.songNameTextView.setText(musicList[position]);
        return songLayout;
    }
}
