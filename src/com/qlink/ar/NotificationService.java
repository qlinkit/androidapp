package com.qlink.ar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.StrictMode;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class NotificationService extends Service {

	NotificationManager NM;

	private String host = "qlink";
	private int port = 443;
	private String protocol = "https";

	private static boolean isRunning = false;
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_SET_INT_VALUE = 3;
	static final int MSG_SET_STRING_VALUE = 4;
	static final int MSG_SET_SERIAL_VALUE = 5;

	private Resources resources;
	ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	int mValue = 0;
	int sdk = android.os.Build.VERSION.SDK_INT;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	private Boolean first = true;
	static ArrayList<String> tnNumbers = new ArrayList<String>();
	static HashMap<String, String> qlInits = new HashMap<String, String>();
	static ArrayList<String> tnNumbersAux = new ArrayList<String>();

	private Context context;
	private Properties pfile;

	@Override
	public void onCreate() {
		if (sdk > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		first = true;

		// Read property file
		if (readProperties() == false) {
			return;
		}

		resources = getResources();

		timer = new Timer();
		try {
			timer.schedule(new RemindTask(), 60 * 1000);
		} catch (IllegalArgumentException e) {

		} catch (IllegalStateException es) {

		}

		resetTimer = new Timer();
		try {
			resetTimer.schedule(new ResetTask(), 60 * 1000 * 4); // 20 min
		} catch (IllegalArgumentException e) {

		} catch (IllegalStateException es) {

		}

		if (tnNumbers.isEmpty()) {
			try {
				recreateState("tnStates.dat");
			} catch (IOException e) {
			}
			try {
				recreateStateQL("qlStates.dat");
			} catch (IOException e) {
			}
		}
		isRunning = true;
	}

	private boolean readProperties() {
		AssetManager assetManager = context.getAssets();
		InputStream inputStream;
		try {
			inputStream = assetManager.open("qlink.properties");
			pfile.load(inputStream);
			this.host = pfile.getProperty("host");
			this.port = Integer.parseInt(pfile.getProperty("port"));
			this.protocol = pfile.getProperty("protocol");
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static boolean isRunning() {
		return isRunning;
	}

	private static int counter = 0;
	Timer timer;
	Timer resetTimer;

	class RemindTask extends TimerTask {
		public void run() {
			runCheck();
			if (counter > 2) {
				counter = 0;
				if (resetTimer != null) {
					resetTimer.cancel();
					resetTimer.purge();
				}
				resetTimer = new Timer();
				try {
					resetTimer.schedule(new ResetTask(), 60 * 1000); // 20 min
				} catch (IllegalArgumentException e) {

				} catch (IllegalStateException es) {

				}
			}
		}
	}

	class ResetTask extends TimerTask {
		public void run() {
			counter++;

			ArrayList<String> tnNumbersX = new ArrayList<String>();
			HashMap<String, String> qlInitsX = new HashMap<String, String>();
			for (String string : tnNumbers) {
				tnNumbersX.add(string);
				if (!string.equals("XSTART")) {
					qlInitsX.put(string, qlInits.get(string));
				}
			}
			qlInits = new HashMap<String, String>();
			tnNumbers = new ArrayList<String>();

			for (String string : tnNumbersX) {
				if (!tnNumbers.contains(string))
					tnNumbers.add(string);

				if (!string.equals("XSTART") && !qlInits.containsKey(string))
					qlInits.put(string, qlInitsX.get(string));
			}

			if (resetTimer != null) {
				resetTimer.cancel();
				resetTimer.purge();
			}
			resetTimer = new Timer();
			try {
				resetTimer.schedule(new ResetTask(), 60 * 1000 * 4); // 20 min
			} catch (IllegalArgumentException e) {

			} catch (IllegalStateException es) {

			}

			if (timer != null) {
				timer.cancel();
				timer.purge();
			}
			timer = new Timer();
			try {
				timer.schedule(new RemindTask(), 60 * 1000);
			} catch (IllegalArgumentException e) {

			} catch (IllegalStateException es) {

			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	@SuppressWarnings("deprecation")
	public void mnotify(String tn) {
		if (resources == null)
			resources = getResources();

		NM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notify = new Notification(
				android.R.drawable.stat_notify_more,
				resources.getString(R.string.notification_notice),
				System.currentTimeMillis());
		notify.icon = R.drawable.ic_launcher;

		PendingIntent pending = PendingIntent.getActivity(
				getApplicationContext(), 0, new Intent(), 0);

		notify.setLatestEventInfo(getApplicationContext(), "DN " + tn + ": "
				+ resources.getString(R.string.notification_status),
				resources.getString(R.string.notification_descr) + " "
						+ qlInits.get(tn) + "...", pending);

		NM.notify(0, notify);
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				try {
					recreateState("tnStates.dat");
				} catch (IOException e) {
				}
				try {
					recreateStateQL("qlStates.dat");
				} catch (IOException e) {
				}

				if (timer != null) {
					timer.cancel();
					timer.purge();
				}
				timer = new Timer();
				try {
					timer.schedule(new RemindTask(), 60 * 1000);
				} catch (IllegalArgumentException e) {

				} catch (IllegalStateException es) {

				}
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_INT_VALUE:
				break;
			case MSG_SET_STRING_VALUE:
				Bundle bnd = msg.getData();
				qlInits.put(msg.obj.toString(), bnd.getString("qlInit"));
				tnNumbers.add(msg.obj.toString());
				if (timer != null) {
					timer.cancel();
					timer.purge();
				}
				timer = new Timer();
				try {
					timer.schedule(new RemindTask(), 60 * 1000);
				} catch (IllegalArgumentException e) {

				} catch (IllegalStateException es) {

				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public void onDestroy() {
		if (timer != null) {
			timer.cancel();
		}
		if (resetTimer != null) {
			resetTimer.cancel();
		}
		isRunning = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	private String getStatus(String tn) {
		HttpPost httpget = new HttpPost(protocol + "://" + host + "/gtrkstatus");

		HttpParams httpParameters = new BasicHttpParams();

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
		nameValuePairs.add(new BasicNameValuePair("trk", tn));
		nameValuePairs.add(new BasicNameValuePair("lang", "en"));

		try {
			httpget.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			if (httpget != null)
				httpget.abort();
			return null;
		} finally {

		}

		int timeoutConnection = 7500;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
		int timeoutSocket = 7500;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = null;

		try {
			response = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e1) {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
			return null;
		} catch (IOException e2) {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
			return null;
		} catch (Exception e3) {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
			return null;
		} finally {

		}

		JSONObject jsonr = null;
		try {
			jsonr = new JSONObject(response);
		} catch (JSONException e4) {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
			return null;
		} finally {

		}
		try {
			String st = (String) jsonr.getString("trkStatus");
			if (st == null || st.equals(""))
				return "UTK";
			else
				return st;
		} catch (JSONException e5) {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
			return null;
		} finally {
			if (httpget != null)
				httpget.abort();
			if (httpclient != null)
				httpclient.getConnectionManager().shutdown();
		}
	}

	private void runCheck() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		timer = new Timer();
		try {
			timer.schedule(new RemindTask(), 60 * 1000);
		} catch (IllegalArgumentException e) {

		} catch (IllegalStateException es) {

		}
		tnNumbersAux.clear();

		if (!tnNumbers.isEmpty()) {
			for (String tn : tnNumbers) {
				if (tn.equals("XSTART")) {
					tnNumbersAux.add(tn);
					continue;
				}
				String status = getStatus(tn);
				if (status != null && status.equals("UTK")) {
					qlInits.remove(tn);
				} else {
					if (status != null && status != "null"
							&& status.equals("1")) {
						mnotify(tn);
						qlInits.remove(tn);
					} else {
						tnNumbersAux.add(tn);
					}
				}
			}

			tnNumbers.clear();
			tnNumbers.addAll(tnNumbersAux);

			if (first == false) {
				saveStateInternalStorage("tnStates.dat");
				saveStateInternalStorageQL("qlStates.dat");
			}
			first = false;
		}
	}

	private void saveStateInternalStorage(String nameFile) {
		try {
			FileOutputStream output = openFileOutput(nameFile,
					Context.MODE_PRIVATE);
			DataOutputStream dout = new DataOutputStream(output);
			dout.writeInt(tnNumbers.size());
			for (String line : tnNumbers)
				dout.writeUTF(line);
			dout.flush();
			dout.close();
		} catch (IOException exc) {
		}
	}

	private void recreateState(String nameFile) throws IOException {
		FileInputStream input = openFileInput(nameFile);
		DataInputStream din = new DataInputStream(input);
		int sz = din.readInt();
		for (int i = 0; i < sz; i++) {
			String line = din.readUTF();
			if (!tnNumbers.contains(line))
				tnNumbers.add(line);
		}
		din.close();
	}

	private void saveStateInternalStorageQL(String nameFile) {
		try {
			FileOutputStream output = openFileOutput(nameFile,
					Context.MODE_PRIVATE);
			DataOutputStream dout = new DataOutputStream(output);
			dout.writeInt(qlInits.size());
			for (String line : qlInits.keySet())
				dout.writeUTF(line + " " + qlInits.get(line));
			dout.flush();
			dout.close();
		} catch (IOException exc) {
		}
	}

	private void recreateStateQL(String nameFile) throws IOException {
		FileInputStream input = openFileInput(nameFile);
		DataInputStream din = new DataInputStream(input);
		int sz = din.readInt();
		for (int i = 0; i < sz; i++) {
			String line = din.readUTF();
			String tn = line.split(" ")[0];
			String ql = line.split(" ")[1];
			if (!qlInits.containsKey(tn))
				qlInits.put(tn, ql);
		}
		din.close();
	}
}
