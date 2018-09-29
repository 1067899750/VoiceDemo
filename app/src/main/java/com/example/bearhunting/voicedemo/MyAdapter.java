package com.example.bearhunting.voicedemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by bearhunting on 2018/02/02.
 */

public class MyAdapter extends BaseAdapter{

    private Context context;
    private LayoutInflater inflater;
    public ArrayList<Map<String,String>> arr;

    public MyAdapter(Context context, ArrayList<Map<String,String>> array) {
        super();
        this.context = context;
        inflater = LayoutInflater.from(context);
        arr = array;
    }

    @Override
    public int getCount() {
        if (arr != null)
            return arr.size();
        else
            return 0;
    }

    @Override
    public Object getItem(int arg0) {
        if (arr.get(arg0) != null) {
            return arr.get(arg0);
        }
        return arg0;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    public void setArray(ArrayList<Map<String,String>> list) {
        this.arr = list;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.item_group, null);
        }
        final TextView edit = (TextView) view.findViewById(R.id.group_item_content);
        Map<String,String> map2 = arr.get(arr.size() - position - 1);
        edit.setText(map2.get("group_id")+"("+map2.get("group_name")+")"); // 在重构adapter的时候不至于数据错乱
        return view;
    }

}
