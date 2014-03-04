package com.github.mjdev.libaums.usbfileman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.mjdev.libaums.fs.UsbFile;

public class MyListAdapter extends ArrayAdapter<UsbFile> {
	
	Comparator<UsbFile> comparator = new Comparator<UsbFile>() {

		@Override
		public int compare(UsbFile lhs, UsbFile rhs) {
			
			if(lhs.isDirectory() && !rhs.isDirectory()) {
				return -1;
			} 
			
			if(rhs.isDirectory() && !lhs.isDirectory()) {
				return 1;
			}
			
			return lhs.getName().compareToIgnoreCase(rhs.getName());
		}
	};
	
	private List<UsbFile> files;
	private UsbFile currentDir;
	
	private LayoutInflater inflater;
	
	public MyListAdapter(Context context, UsbFile dir) throws IOException {
		super(context, R.layout.list_item);
		currentDir = dir;
		files = new ArrayList<UsbFile>();
		
		inflater = (LayoutInflater) context
			        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		files = Arrays.asList(dir.listFiles());
		
		Collections.sort(files, comparator);
	}

	@Override
	public int getCount() {
		return files.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if(convertView != null) {
			view = convertView;
		} else {
			view = inflater.inflate(R.layout.list_item, parent, false);
		}
		
		TextView typeText = (TextView) view.findViewById(R.id.type_text_view);
		TextView  nameText = (TextView) view.findViewById(R.id.name_text_view);
		UsbFile file = files.get(position);
		if(file.isDirectory()) {
			typeText.setText("Directory");
		} else {
			typeText.setText("File");
		}
		nameText.setText(file.getName());
		
		return view;
	}

	@Override
	public UsbFile getItem(int position) {
		return files.get(position);
	}

	public UsbFile getCurrentDir() {
		return currentDir;
	}
	
}