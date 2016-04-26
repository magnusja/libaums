/*
 * (C) Copyright 2014-2016 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.mjdev.libaums.usbfileman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.github.mjdev.libaums.server.http.UsbFileHttpServer;
import com.github.mjdev.libaums.server.http.UsbFileHttpServerService;
import com.github.mjdev.libaums.test.LibAumsTest;

/**
 * MainActivity of the demo application which shows the contents of the first
 * partition.
 * 
 * @author mjahnen
 * 
 */
public class MainActivity extends AppCompatActivity implements OnItemClickListener {

	/**
	 * Action string to request the permission to communicate with an UsbDevice.
	 */
	private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";
	private static final String TAG = MainActivity.class.getSimpleName();

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {

				UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

					if (device != null) {
						setupDevice();
					}
				}

			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				Log.d(TAG, "USB device attached");

				// determine if connected device is a mass storage devuce
				if (device != null) {
					discoverDevice();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				Log.d(TAG, "USB device detached");

				// determine if connected device is a mass storage devuce
				if (device != null) {
					if (MainActivity.this.device != null) {
						MainActivity.this.device.close();
					}
					// check if there are other devices or set action bar title
					// to no device if not
					discoverDevice();
				}
			}

		}
	};

    /**
	 * Dialog to create new directories.
	 * 
	 * @author mjahnen
	 * 
	 */
	public static class NewDirDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final MainActivity activity = (MainActivity) getActivity();
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle("New Directory");
			builder.setMessage("Please enter a name for the new directory");
			final EditText input = new EditText(activity);
			builder.setView(input);

			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {

					UsbFile dir = activity.adapter.getCurrentDir();
					try {
						dir.createDirectory(input.getText().toString());
						activity.adapter.refresh();
					} catch (Exception e) {
						Log.e(TAG, "error creating dir!", e);
					}

				}

			});

			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			});

			builder.setCancelable(false);
			return builder.create();
		}

	}

	/**
	 * Dialog to create new files.
	 * 
	 * @author mjahnen
	 * 
	 */
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
			textView.setText(R.string.name);
			layout.addView(textView);
			layout.addView(input);
			textView = new TextView(activity);
			textView.setText(R.string.content);
			layout.addView(textView);
			layout.addView(content);

			builder.setView(layout);

			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {

					UsbFile dir = activity.adapter.getCurrentDir();
					try {
						UsbFile file = dir.createFile(input.getText().toString());
						file.write(0, ByteBuffer.wrap(content.getText().toString().getBytes()));
						file.close();
						activity.adapter.refresh();
					} catch (Exception e) {
						Log.e(TAG, "error creating file!", e);
					}

				}

			});

			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			});

			builder.setCancelable(false);
			return builder.create();
		}

	}

	/**
	 * Class to hold the files for a copy task. Holds the source and the
	 * destination file.
	 * 
	 * @author mjahnen
	 * 
	 */
	private static class CopyTaskParam {
		/* package */UsbFile from;
		/* package */File to;
	}

	/**
	 * Asynchronous task to copy a file from the mass storage device connected
	 * via USB to the internal storage.
	 * 
	 * @author mjahnen
	 * 
	 */
	private class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {

		private ProgressDialog dialog;
		private CopyTaskParam param;

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
				for (long i = 0; i < length; i += buffer.limit()) {
					buffer.limit((int) Math.min(buffer.capacity(), length - i));
					params[0].from.read(i, buffer);
					out.write(buffer.array(), 0, buffer.limit());
					publishProgress((int) i);
					buffer.clear();
				}
				out.close();
			} catch (IOException e) {
				Log.e(TAG, "error copying!", e);
			}
			Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();

			Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
			File file = new File(param.to.getAbsolutePath());
			String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri
					.fromFile(file).toString());
			String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					extension);
			myIntent.setDataAndType(Uri.fromFile(file), mimetype);
			try {
				startActivity(myIntent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(MainActivity.this, "Could no find an app for that file!",
						Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			dialog.setMax((int) param.from.getLength());
			dialog.setProgress(values[0]);
		}

	}

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "on service connected " + name);
            UsbFileHttpServerService.ServiceBinder binder = (UsbFileHttpServerService.ServiceBinder) service;
            serverService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "on service disconnected " + name);
            serverService = null;
        }
    };

	private ListView listView;
	private UsbMassStorageDevice device;
	/* package */UsbFileListAdapter adapter;
	private Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();

    private Intent serviceIntent = null;
    private UsbFileHttpServerService serverService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        serviceIntent = new Intent(this, UsbFileHttpServerService.class);

		setContentView(R.layout.activity_main);

		listView = (ListView) findViewById(R.id.listview);

		listView.setOnItemClickListener(this);
		registerForContextMenu(listView);

		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(usbReceiver, filter);
		discoverDevice();
    }

    @Override
    protected void onStart() {
        super.onStart();

        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(serviceConnection);
    }

    /**
	 * Searches for connected mass storage devices, and initializes them if it
	 * could find some.
	 */
	private void discoverDevice() {
		UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);

		if (devices.length == 0) {
			Log.w(TAG, "no device found!");
			android.support.v7.app.ActionBar actionBar = getSupportActionBar();
			actionBar.setTitle("No device");
			listView.setAdapter(null);
			return;
		}

		// we only use the first device
		device = devices[0];

		UsbDevice usbDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

		if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
			Log.d(TAG, "received usb device via intent");
			// requesting permission is not needed in this case
			setupDevice();
		} else {
			// first request permission from user to communicate with the
			// underlying
			// UsbDevice
			PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
					ACTION_USB_PERMISSION), 0);
			usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
		}
	}

	/**
	 * Sets the device up and shows the contents of the root directory.
	 */
	private void setupDevice() {
		try {
			device.init();

			// we always use the first partition of the device
			FileSystem fs = device.getPartitions().get(0).getFileSystem();
			Log.d(TAG, "Capacity: " + fs.getCapacity());
			Log.d(TAG, "Occupied Space: " + fs.getOccupiedSpace());
			Log.d(TAG, "Free Space: " + fs.getFreeSpace());
			UsbFile root = fs.getRootDirectory();

			ActionBar actionBar = getSupportActionBar();
			actionBar.setTitle(fs.getVolumeLabel());

			listView.setAdapter(adapter = new UsbFileListAdapter(this, root));
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		MoveClipboard cl = MoveClipboard.getInstance();
		menu.findItem(R.id.paste).setEnabled(cl.getFile() != null);
		menu.findItem(R.id.stop_http_server).setEnabled(serverService != null && serverService.isServerRunning());
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
		case R.id.create_big_file:
			createBigFile();
			return true;
		case R.id.paste:
			move();
			return true;
        case R.id.stop_http_server:
            if(serverService != null) {
                serverService.stopServer();
            }
            return true;
		case R.id.run_tests:
			startActivity(new Intent(this, LibAumsTest.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final UsbFile entry = adapter.getItem((int) info.id);
		switch (item.getItemId()) {
		case R.id.delete_item:
			try {
				entry.delete();
				adapter.refresh();
			} catch (IOException e) {
				Log.e(TAG, "error deleting!", e);
			}
			return true;
		case R.id.rename_item:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Rename");
			builder.setMessage("Please enter a name for renaming");
			final EditText input = new EditText(this);
			input.setText(entry.getName());
			builder.setView(input);

			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						entry.setName(input.getText().toString());
						adapter.refresh();
					} catch (IOException e) {
						Log.e(TAG, "error renaming!", e);
					}
				}

			});

			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			});
			builder.setCancelable(false);
			builder.create().show();
			return true;
		case R.id.move_item:
			MoveClipboard cl = MoveClipboard.getInstance();
			cl.setFile(entry);
			return true;
        case R.id.start_http_server:
            startHttpServer(entry);
            return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
		UsbFile entry = adapter.getItem(position);
		try {
			if (entry.isDirectory()) {
				dirs.push(adapter.getCurrentDir());
				listView.setAdapter(adapter = new UsbFileListAdapter(this, entry));

			} else {
				CopyTaskParam param = new CopyTaskParam();
				param.from = entry;
				File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/usbfileman/cache");
				f.mkdirs();
				int index = entry.getName().lastIndexOf(".");
				String prefix = entry.getName().substring(0, index);
				String ext = entry.getName().substring(index);
				// prefix must be at least 3 characters
				if(prefix.length() < 3) {
					prefix += "pad";
				}
				param.to = File.createTempFile(prefix, ext, f);
				new CopyTask().execute(param);
			}
		} catch (IOException e) {
			Log.e(TAG, "error staring to copy!", e);
		}
	}

    private void startHttpServer(final UsbFile file) {

        Log.d(TAG, "starting HTTP server");

        if(serverService == null) {
            Toast.makeText(MainActivity.this, "serverService == null!", Toast.LENGTH_LONG).show();
            return;
        }

        if(serverService.isServerRunning()) {
            Log.d(TAG, "Stopping existing server service");
            serverService.stopServer();
        }

        // now start the server
        try {
            serverService.startServer(file);
            Toast.makeText(MainActivity.this, "HTTP server up and running", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error starting HTTP server", e);
            Toast.makeText(MainActivity.this, "Could not start HTTP server", Toast.LENGTH_LONG).show();
        }

        if(file.isDirectory()) {
            // only open activity when serving a file
            return;
        }

        Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
        myIntent.setData(Uri.parse(serverService.getServer().getBaseUrl() + file.getName()));
        try {
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, "Could no find an app for that file!",
                    Toast.LENGTH_LONG).show();
        }
    }

	/**
	 * This methods creates a very big file for testing purposes. It writes only
	 * a small chunk of bytes in every loop iteration, so the offset where the
	 * write starts will not always be a multiple of the cluster or block size
	 * of the file system or block device. As a plus the file has to be grown
	 * after every loop iteration which tests for example on FAT32 the dynamic
	 * growth of a cluster chain.
	 */
	private void createBigFile() {
		UsbFile dir = adapter.getCurrentDir();
		UsbFile file;
		try {
			file = dir.createFile("big_file_test.txt");
			file.write(0, ByteBuffer.wrap("START\n".getBytes()));
			int i;

			for (i = 6; i < 9000; i += 5) {
				file.write(i, ByteBuffer.wrap("TEST\n".getBytes()));
			}

			file.write(i, ByteBuffer.wrap("END\n".getBytes()));

			file.close();

			adapter.refresh();
		} catch (IOException e) {
			Log.e(TAG, "error creating big file!", e);
		}
	}

	/**
	 * This method moves the file located in the {@link MoveClipboard} into the
	 * current shown directory.
	 */
	private void move() {
		MoveClipboard cl = MoveClipboard.getInstance();
		UsbFile file = cl.getFile();
		try {
			file.moveTo(adapter.getCurrentDir());
			adapter.refresh();
		} catch (IOException e) {
			Log.e(TAG, "error moving!", e);
		}
		cl.setFile(null);
	}

	@Override
	public void onBackPressed() {
		try {
			UsbFile dir = dirs.pop();
			listView.setAdapter(adapter = new UsbFileListAdapter(this, dir));
		} catch (NoSuchElementException e) {
			super.onBackPressed();
		} catch (IOException e) {
			Log.e(TAG, "error initializing adapter!", e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(usbReceiver);

        if(!serverService.isServerRunning()) {
            Log.d(TAG, "Stopping service");
            stopService(serviceIntent);

            if (device != null) {
                Log.d(TAG, "Closing device");

                device.close();
            }
        }
	}
}
