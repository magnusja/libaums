package com.github.mjdev.libaums.usbfileman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;


public class MainActivity extends Activity implements OnItemClickListener {
	
	private static final String ACTION_USB_PERMISSION =
			"com.github.mjdev.libaums.USB_PERMISSION";
	private static final String TAG = MainActivity.class.getSimpleName();

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					
					if(device != null) {
						setupDevice();
					}
				}
			}
		}
	};
	
	public static class NewDirDialog extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final MainActivity activity = (MainActivity) getActivity();
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle("New Directory");
			builder.setMessage("Please enter a name for the new directory");
			final EditText input = new EditText(activity);
			builder.setView(input);

			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							UsbFile dir = activity.adapter.getCurrentDir();
							try {
								dir.createDirectory(input.getText().toString());
								activity.adapter.refresh();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					});

			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
						}
					});
			builder.setCancelable(false);
			return builder.create();
		}
		
	}
	
	public static class NewFileDialog extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final MainActivity activity = (MainActivity) getActivity();
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle("New File");
			builder.setMessage("Please enter a name for the new file and some input");
			final EditText input = new EditText(activity);
			final EditText content = new EditText(activity);
			LinearLayout layout = new LinearLayout(activity);
			layout.setOrientation(LinearLayout.VERTICAL);
			TextView textView = new TextView(activity);
			textView.setText("Name:");
			layout.addView(textView);
			layout.addView(input);
			textView = new TextView(activity);
			textView.setText("Content:");
			layout.addView(textView);
			layout.addView(content);
			
			builder.setView(layout);

			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							UsbFile dir = activity.adapter.getCurrentDir();
							try {
								UsbFile file = dir.createFile(input.getText().toString());
								file.write(0, ByteBuffer.wrap(content.getText().toString().getBytes()));
								file.close();
								activity.adapter.refresh();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					});

			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
						}
					});
			builder.setCancelable(false);
			return builder.create();
		}
		
	}
	
	private static class CopyTaskParam {
		UsbFile from;
		File to;
	}
	
	private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {
		
		ProgressDialog dialog;
		CopyTaskParam param;
		
		public CopyTask() {
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setTitle("Copying file");
			dialog.setMessage("Copying a file to the internal storage, this can take some time!");
			dialog.setIndeterminate(false);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected void onPreExecute() {
			dialog.show();
		}
		
		@Override
		protected Void doInBackground(CopyTaskParam... params) {
			long time = System.currentTimeMillis();
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			param = params[0];
			long length = params[0].from.getLength();
			try {
				FileOutputStream out = new FileOutputStream(params[0].to);
				for(long i = 0; i < length; i += buffer.limit()) {
					buffer.limit((int) Math.min(buffer.capacity(), length - i));
					params[0].from.read(i, buffer);
					out.write(buffer.array(), 0, buffer.limit());
					publishProgress((int)i);
					buffer.clear();
				}
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			
			Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
			File file = new File(param.to.getAbsolutePath());
			String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
			String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			myIntent.setDataAndType(Uri.fromFile(file), mimetype);
			try {
				startActivity(myIntent);
			} catch(ActivityNotFoundException e) {
				Toast.makeText(MainActivity.this, "Could no find an app for that file!", Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			dialog.setMax((int) param.from.getLength());
			dialog.setProgress(values[0]);
		}
		
	}
	
	ListView listView;
	UsbMassStorageDevice device;
	MyListAdapter adapter;
	Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		listView = (ListView) findViewById(R.id.listview);
		
		listView.setOnItemClickListener(this);
		registerForContextMenu(listView);
		
		UsbManager usbManager  = (UsbManager) getSystemService(Context.USB_SERVICE);
		UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
		
		if(devices.length == 0) {
			Log.w(TAG, "no device found!");
			ActionBar actionBar = getActionBar();
			actionBar.setTitle("No device");
			return;
		}

		device = devices[0];

		PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(usbReceiver, filter);
		usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
	}
	
	private void setupDevice() {
		try {
			device.init();
			
			FileSystem fs = device.getPartitions().get(0).getFileSystem();
			UsbFile root = fs.getRootDirectory();
			
			ActionBar actionBar = getActionBar();
			actionBar.setTitle(fs.getVolumeLabel());
			
			listView.setAdapter(adapter = new MyListAdapter(this, root));
		} catch (IOException e) {
			Log.e(TAG, "error setting up device", e);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
	    switch (item.getItemId()) {
	        case R.id.create_file:
	        	new NewFileDialog().show(getFragmentManager(), "NEW_FILE");
	            return true;
	        case R.id.create_dir:
	        	new NewDirDialog().show(getFragmentManager(), "NEW_DIR");
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		UsbFile entry = adapter.getItem((int)info.id);
	    switch (item.getItemId()) {
	        case R.id.delete_item:
			try {
				entry.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	            return true;
	        default:
	            return super.onContextItemSelected(item);
	    }
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
		UsbFile entry = adapter.getItem(position);
		try {
			if(entry.isDirectory()) {
				dirs.push(adapter.getCurrentDir());
				listView.setAdapter(adapter = new MyListAdapter(this, entry));

			} else {
				CopyTaskParam param = new CopyTaskParam();
				param.from = entry;
				File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/usbfileman/cache");
				f.mkdirs();
				int index = entry.getName().lastIndexOf(".");
				String prefix = entry.getName().substring(0, index);
				String ext = entry.getName().substring(index);
				param.to = File.createTempFile(prefix, ext, f);
				new CopyTask().execute(param);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onBackPressed() {
		try {
			UsbFile dir = dirs.pop();
			listView.setAdapter(adapter = new MyListAdapter(this, dir));
		} catch(NoSuchElementException e) {
			super.onBackPressed();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(device != null) {
			device.close();
			unregisterReceiver(usbReceiver);
		}
	}
}
