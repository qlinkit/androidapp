package com.qlink.ar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.qlink.ar.util.AesChiperData;
import com.qlink.ar.util.AesUtil;
import com.qlink.ar.util.AppUtil;
import com.qlink.ar.util.DNSResolver;
import com.qlink.ar.util.EscapeUtil;
import com.qlink.ar.util.FontChangeHelper;
import com.qlink.ar.util.LZW;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class QlinkActivity extends ActionBarActivity {

	int sdk = android.os.Build.VERSION.SDK_INT;

	private File file = null;

	private static final int SETTINGS_RESULT = 1;
	private RelativeLayout progressEncripting;
	private Handler clockHandler = new Handler();
	private Handler clockHandler2 = new Handler();
	private Boolean welcomed = false;
	private Boolean acceptedDiscraimer = false;

	private Boolean finishWelcome = false;
	private Boolean isInternetConnected = false;
	private Boolean isHostConnected = false;
	private String imprint = "false";
	private String selectedLanguage = null;
	private List<String> encFiles = new ArrayList<String>();
	private List<String> namesFiles = new ArrayList<String>();
	private Typeface fontTypeFace;
	private Resources resources;

	private String xToken = null;
	private String currentAppWebVersion = null;
	private String currentWebVersion = null;
	private Boolean forceUpdateFlag = false;
	private String forceText = "";
	private String host = "qlink";
	private int port = 443;
	private String protocol = "https";
	private String shareText = "";
	private ActionBar actionBar;

	protected String appVersion = "0";
	protected String sharedExternalText = "";

	private Integer currentViewId = null;

	protected String password = "";
	protected String lastQl = "";
	protected String pUrlHash = "";

	private Boolean forwardMsg = false;

	private String tn = null;
	private String tnlk = null;

	private Context context;
	private Properties pfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (android.os.Build.VERSION.SDK_INT < 11) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		super.onCreate(savedInstanceState);

		// Read property file
		if (readProperties() == false) {
			return;
		}

		// Read preferences
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPrefs.getBoolean("imprintOption", false))
			imprint = "true";
		else
			imprint = "false";

		selectedLanguage = sharedPrefs.getString("languageLocale", null);
		if (selectedLanguage != null) {
			Locale l = new Locale(selectedLanguage);

			Configuration c = new Configuration(getResources()
					.getConfiguration());
			c.locale = l;
			getResources().updateConfiguration(c,
					getResources().getDisplayMetrics());

			Locale.setDefault(l);
		}

		if (sharedPrefs.getBoolean("acceptedDiscraimer", false))
			acceptedDiscraimer = true;
		else
			acceptedDiscraimer = false;

		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			appVersion = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}

		actionBar = getSupportActionBar();
		actionBar.hide();

		resources = getResources();
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			Boolean wcAct = bundle.getBoolean("welcomed");
			if (wcAct != null)
				welcomed = wcAct;

			String lQl = bundle.getString("lql");
			if (lQl != null) {
				forwardMsg = true;
				lastQl = lQl;
			}
		}

		if (sdk > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		fontTypeFace = Typeface.createFromAsset(this.getAssets(),
				"font/Roboto.ttf");

		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if ("text/plain".equals(type)) {
				handleSendText(intent);
				welcomed = true;
			}
		}

		if (!welcomed) {
			welcomed = true;
			setContentView(R.layout.welcome_qlink);
			TextView txw = (TextView) findViewById(R.id.isoLogo);
			txw.setTypeface(fontTypeFace);
			txw.setVisibility(View.VISIBLE);

			txw = (TextView) findViewById(R.id.isoLogoExtra);
			txw.setTypeface(fontTypeFace);
			txw.setVisibility(View.VISIBLE);

			((ViewFlipper) findViewById(R.id.view_flipper))
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							clockHandler.removeCallbacks(clockTask);
							viewMain();
						}
					});

			clockHandler.postDelayed(clockTask, 200);
			return;
		} else {
			viewMain();
		}
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
			progressEncripting.setVisibility(View.GONE);
			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);

			TextView warning = (TextView) findViewById(R.id.warning1text);
			warning.setText(resources.getString(R.string.host_disconnected));

			Button btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.VISIBLE);

			btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.GONE);
			generatingQlink = false;
			return false;
		}
		return true;
	}

	private void checkIfServiceIsRunning() {
		if (NotificationService.isRunning()) {
			doBindService();
		} else {
			Intent intentService = new Intent(this, NotificationService.class);
			startService(intentService);
			doBindService();
		}
	}

	Messenger mService = null;
	boolean mIsBound;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NotificationService.MSG_SET_INT_VALUE:
				break;
			case NotificationService.MSG_SET_STRING_VALUE:
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null,
						NotificationService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			checkIfServiceIsRunning();
		}
	};

	void doBindService() {
		mIsBound = bindService(new Intent(this, NotificationService.class),
				mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							NotificationService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
				}
			}
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	void handleSendText(Intent intent) {
		if (intent.getStringExtra(Intent.EXTRA_TEXT).length() <= 2000) {
			sharedExternalText = intent.getStringExtra(Intent.EXTRA_TEXT);
		} else {
			Toast.makeText(getApplicationContext(),
					resources.getString(R.string.share_max_size),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void sendMessageToService(int intvaluetosend, String tn, String ql) {
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							NotificationService.MSG_SET_STRING_VALUE,
							intvaluetosend, 0, tn);
					Bundle bnd = new Bundle();
					bnd.putString("qlInit", ql);
					msg.setData(bnd);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
				}
			}
		}
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		FontChangeHelper fontChanger = new FontChangeHelper(getAssets(),
				"font/Roboto.ttf");
		fontChanger.replaceFonts((ViewGroup) this
				.findViewById(android.R.id.content));
	}

	private void viewMain() {
		if (!acceptedDiscraimer) {
			setContentView(R.layout.discraimer_app);
			return;
		}

		Boolean retrieveVersion = false;
		if (checkResolvConnection())
			retrieveVersion = getCurrentAppVersion();

		if (retrieveVersion == true) {
			if (forceUpdateFlag == true
					&& (Float.valueOf(appVersion) < Float
							.valueOf(currentAppWebVersion))) {
				setContentView(R.layout.update_app);
				TextView tx = (TextView) findViewById(R.id.updateText);
				tx.setText(forceText);
				return;
			}
		} else {
			setContentView(R.layout.activity2_qlink);
			LinearLayout llayText = (LinearLayout) findViewById(R.id.textContainer);
			llayText.setVisibility(View.GONE);

			llayText = (LinearLayout) findViewById(R.id.my_toolbar);
			llayText.setVisibility(View.GONE);

			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);

			TextView warning = (TextView) findViewById(R.id.warning1text);
			warning.setText(resources.getString(R.string.inet_disconnected));

			Button btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.VISIBLE);

			btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.GONE);
			generatingQlink = false;
			return;
		}

		setContentView(R.layout.activity2_qlink);

		if (forwardMsg) {
			TextView titleMsg = (TextView) findViewById(R.id.titleMsg);
			titleMsg.setVisibility(View.VISIBLE);
		}

		EditText msg = (EditText) findViewById(R.id.msg);
		msg.setText(sharedExternalText);
		msg.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {

			}

			@Override
			public void afterTextChanged(Editable arg0) {
				Long curDate = new Date().getTime();
				password = generateRandomString(32, curDate.toString()
						+ password);
				password = Base64.encodeToString(password.getBytes(),
						Base64.DEFAULT).substring(0, 32);
			}
		});
		sharedExternalText = "";

		actionBar.show();
		actionBar.setTitle(" Qlink.it");
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setLogo(R.drawable.ic_launcher);
	}

	public void updateApp(View v) {
		final String appPackageName = getPackageName();
		try {
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=" + appPackageName)));
		} catch (android.content.ActivityNotFoundException anfe) {
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/apps/details?id="
							+ appPackageName)));
		}
	}

	public void closeWarning(View v) {
		RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
		layWar.setVisibility(View.GONE);
	}

	public void acceptDiscraimer(View v) {
		acceptedDiscraimer = true;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putBoolean("acceptedDiscraimer", true);
		editor.commit();

		viewMain();
	}

	public void reintentWarning(View v) {
		RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
		layWar.setVisibility(View.GONE);

		viewMain();
	}

	private void checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		isInternetConnected = activeNetwork != null
				&& activeNetwork.isConnectedOrConnecting();
	}

	private void checkHostConnection(int timeout) {
		ArrayList<String> dnsServers = AppUtil.getDNSServers();
		if (dnsServers.isEmpty()) {
			isHostConnected = false;
			return;
		}

		DNSResolver dnsRes = new DNSResolver(host);
		Thread t = new Thread(dnsRes);
		t.start();
		try {
			t.join(3000);
		} catch (InterruptedException e1) {
			isHostConnected = false;
			return;
		}

		if (dnsRes.get() == null) {
			isHostConnected = false;
			return;
		}

		try {
			Socket socket = new Socket();
			socket.setSoTimeout(timeout);
			socket.connect(new InetSocketAddress(host, port), timeout);
			socket.close();
			isHostConnected = true;
			return;
		} catch (Exception ex) {
			isHostConnected = false;
		}
		isHostConnected = false;
	}

	public Boolean checkResolvConnection() {
		ArrayList<String> dnsServers = AppUtil.getDNSServers();
		if (dnsServers.isEmpty())
			return false;

		DNSResolver dnsRes = new DNSResolver(host);
		Thread t = new Thread(dnsRes);
		t.start();
		try {
			t.join(3000);
		} catch (InterruptedException e1) {
			Toast.makeText(this,
					resources.getString(R.string.inet_disconnected),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		if (dnsRes.get() == null) {
			Toast.makeText(this,
					resources.getString(R.string.inet_disconnected),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		HttpGet httpget = new HttpGet(protocol + "://" + host
				+ "/images/sprites.png");

		HttpParams httpParameters = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = 3000;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);

		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 3000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = null;
		try {
			response = httpclient.execute(httpget, responseHandler);
			return true;
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private Runnable clockTask3 = new Runnable() {

		@Override
		public void run() {
			createQlinkSt1();
		}

	};

	private Runnable clockTask2 = new Runnable() {

		@Override
		public void run() {
			createQlinkSt2();
		}

	};

	private Runnable clockTask = new Runnable() {

		@Override
		public void run() {
			finishWelcome = true;
			viewMain();
		}

	};

	public Boolean getToken() {
		HttpGet httpget = new HttpGet(protocol + "://" + host + "/tokenizer");

		HttpParams httpParameters = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = 7500;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 7500;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = null;
		try {
			response = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			return false;
		}

		JSONObject jsonr = null;
		try {
			jsonr = new JSONObject(response);
		} catch (JSONException e) {
			return false;
		}
		try {
			xToken = (String) jsonr.getString("x_token");
		} catch (JSONException e) {
			return false;
		}
		return true;
	}

	public Boolean getCurrentAppVersion() {
		String locale = Locale.getDefault().getLanguage();

		HttpGet httpget = new HttpGet(protocol + "://" + host
				+ "/appversion?lang=" + locale);

		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = 3500;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 3500;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = null;
		try {
			response = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			return false;
		}

		JSONObject jsonr = null;
		try {
			jsonr = new JSONObject(response);
		} catch (JSONException e) {
			return false;
		}
		try {
			currentAppWebVersion = (String) jsonr.getString("current_version");
			forceUpdateFlag = (Boolean) jsonr.getBoolean("fup");
			forceText = (String) jsonr.getString("fup_text");
		} catch (JSONException e) {
			return false;
		}

		try {
			currentWebVersion = (String) jsonr.getString("web_current_version");
		} catch (JSONException e) {
		}

		return true;
	}

	private Boolean generatingQlink = false;

	public void createQlink(View v) {
		progressEncripting = (RelativeLayout) findViewById(R.id.encripting);
		TextView txw = (TextView) findViewById(R.id.encriptingText);
		txw.setText(resources.getString(R.string.connectivity));
		txw.setTypeface(fontTypeFace);
		progressEncripting.setVisibility(View.VISIBLE);
		ProgressBar t = (ProgressBar) findViewById(R.id.encriptingImg);
		t.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash));

		clockHandler2.postDelayed(clockTask3, 100);
	}

	public void createQlinkSt1() {
		if (generatingQlink) // Evitamos multiples clicks
			return;

		generatingQlink = true;
		EditText msg = (EditText) findViewById(R.id.msg);
		if (msg.length() == 0) {
			progressEncripting.setVisibility(View.GONE);
			Toast.makeText(getApplicationContext(),
					resources.getString(R.string.type_message),
					Toast.LENGTH_SHORT).show();
			generatingQlink = false;
			return;
		}

		checkInternetConnection();
		if (!isInternetConnected) {
			progressEncripting.setVisibility(View.GONE);
			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);

			TextView warning = (TextView) findViewById(R.id.warning1text);
			warning.setText(resources.getString(R.string.inet_disconnected));

			Button btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.VISIBLE);

			btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.GONE);
			generatingQlink = false;
			return;
		}

		checkHostConnection(5000);
		if (!isHostConnected) {
			progressEncripting.setVisibility(View.GONE);
			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);

			TextView warning = (TextView) findViewById(R.id.warning1text);
			warning.setText(resources.getString(R.string.host_disconnected));

			Button btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.VISIBLE);

			btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.GONE);
			generatingQlink = false;
			return;
		} else {
			Boolean validToken = getToken();
			if (!validToken) {
				progressEncripting.setVisibility(View.GONE);
				RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
				layWar.setOnClickListener(null);
				layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				layWar.setVisibility(View.VISIBLE);

				TextView warning = (TextView) findViewById(R.id.warning1text);
				warning.setText(resources.getString(R.string.host_disconnected));

				Button btWar = (Button) findViewById(R.id.closeWar);
				btWar.setVisibility(View.VISIBLE);

				btWar = (Button) findViewById(R.id.reinWar);
				btWar.setVisibility(View.GONE);
				generatingQlink = false;
				return;
			}
		}

		progressEncripting = (RelativeLayout) findViewById(R.id.encripting);
		TextView txw = (TextView) findViewById(R.id.encriptingText);
		txw.setText(resources.getString(R.string.encrypting));
		txw.setTypeface(fontTypeFace);
		progressEncripting.setVisibility(View.VISIBLE);
		ProgressBar t = (ProgressBar) findViewById(R.id.encriptingImg);
		t.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash));

		clockHandler2.postDelayed(clockTask2, 200);
	}

	@SuppressWarnings("deprecation")
	public void createQlinkSt2() {
		EditText msg = (EditText) findViewById(R.id.msg);
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(protocol + "://" + host + "/inject");

		ResponseHandler<String> responseHandler = new BasicResponseHandler();

		Integer iter = 100;
		AesUtil util = new AesUtil(256, iter);

		Long curDate = new Date().getTime();
		password = generateRandomString(32, curDate.toString() + password);
		Random rand = new Random();
		int rnd = rand.nextInt(5) + 16; // 16 a 20
		String fpassword = Base64.encodeToString(password.getBytes(),
				Base64.DEFAULT).substring(0, rnd);

		String salt = getRandomHexString(8);
		String iv = getRandomHexString(32);

		// Sanity
		String strReplaced = EscapeUtil.escapeHtmlEntities(msg.getText()
				.toString());
		strReplaced = strReplaced.replaceAll("[\n\r]", "<br />");
		strReplaced = strReplaced.replaceAll("[\r]", "<br />");
		strReplaced = strReplaced.replaceAll("\n", "<br />");

		String sanity = Jsoup.clean(StringEscapeUtils.escapeHtml(strReplaced),
				Whitelist.basicWithImages());

		sanity = lastQl + "%%A%%" + sanity + "%%C%%";
		Boolean decomEnabled = false;
		String dataCompress = null;
		String decom = "true";

		// Compress
		if (decomEnabled) {
			dataCompress = LZW.compress(sanity);
			decom = "true";
		} else {
			dataCompress = sanity;
			decom = "false";
		}

		// Encrypt
		String encrypt = util.encrypt(salt, iv, fpassword, dataCompress);

		AesChiperData aesData = new AesChiperData();
		aesData.setIv(iv);
		aesData.setSalt(salt);
		aesData.setData(encrypt);

		JSONObject jo = new JSONObject();
		try {
			jo.put("data", encrypt);
			jo.put("iv", iv);
			jo.put("salt", salt);
			jo.put("iter", iter);
			jo.put("decom", decom);
		} catch (JSONException e1) {
			generatingQlink = false;
		}

		try {
			String[] array = namesFiles.toArray(new String[namesFiles.size()]);
			JSONArray mJSONArray = new JSONArray(Arrays.asList(array));

			String[] arrayEnc = encFiles.toArray(new String[encFiles.size()]);
			for (int i = 0; i < arrayEnc.length; i++) {
				JSONObject jof = new JSONObject();
				try {
					jof.put("data",
							util.encrypt(salt, iv, fpassword, arrayEnc[i]));
					jof.put("iv", iv);
					jof.put("salt", salt);
					jof.put("iter", iter);
				} catch (JSONException e1) {
					generatingQlink = false;
				}
				arrayEnc[i] = jof.toString();
			}
			JSONArray mJSONArrayEnc = new JSONArray(Arrays.asList(arrayEnc));

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
			nameValuePairs.add(new BasicNameValuePair("msg", jo.toString()));
			nameValuePairs.add(new BasicNameValuePair("from", "app_android"
					+ appVersion));
			nameValuePairs.add(new BasicNameValuePair("imprint", imprint));
			nameValuePairs.add(new BasicNameValuePair("files", mJSONArrayEnc
					.toString()));
			nameValuePairs.add(new BasicNameValuePair("namesFiles", mJSONArray
					.toString()));
			nameValuePairs.add(new BasicNameValuePair("randomHash", String
					.valueOf(new Date().getTime())));
			nameValuePairs.add(new BasicNameValuePair("x_token", xToken));
			nameValuePairs.add(new BasicNameValuePair("n", String
					.valueOf(new Date().getTimezoneOffset())));
			String locale = Locale.getDefault().getLanguage();
			nameValuePairs.add(new BasicNameValuePair("lang", locale));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			String response = httpclient.execute(httppost, responseHandler);

			String status = null;
			String urlHash = null;
			String expire = null;

			JSONObject jsonr = null;
			try {
				jsonr = new JSONObject(response);
				status = (String) jsonr.get("status");
				urlHash = (String) jsonr.get("hash");
				expire = (String) jsonr.get("expireDate");
			} catch (JSONException e) {
				progressEncripting = (RelativeLayout) findViewById(R.id.encripting);
				progressEncripting.setVisibility(View.GONE);

				RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
				layWar.setOnClickListener(null);
				layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				layWar.setVisibility(View.VISIBLE);

				TextView warning = (TextView) findViewById(R.id.warning1text);
				warning.setText(resources.getString(R.string.exception_error));

				Button btWar = (Button) findViewById(R.id.closeWar);
				btWar.setVisibility(View.VISIBLE);

				btWar = (Button) findViewById(R.id.reinWar);
				btWar.setVisibility(View.GONE);
				generatingQlink = false;
				return;
			} catch (ClassCastException e) {
				progressEncripting = (RelativeLayout) findViewById(R.id.encripting);
				progressEncripting.setVisibility(View.GONE);

				RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
				layWar.setOnClickListener(null);
				layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				layWar.setVisibility(View.VISIBLE);

				TextView warning = (TextView) findViewById(R.id.warning1text);
				warning.setText(resources.getString(R.string.exception_error));

				Button btWar = (Button) findViewById(R.id.closeWar);
				btWar.setVisibility(View.VISIBLE);

				btWar = (Button) findViewById(R.id.reinWar);
				btWar.setVisibility(View.GONE);
				generatingQlink = false;
				return;
			} catch (Exception e) {
				progressEncripting = (RelativeLayout) findViewById(R.id.encripting);
				progressEncripting.setVisibility(View.GONE);

				RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning1);
				layWar.setOnClickListener(null);
				layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				layWar.setVisibility(View.VISIBLE);

				TextView warning = (TextView) findViewById(R.id.warning1text);
				warning.setText(resources.getString(R.string.exception_error));

				Button btWar = (Button) findViewById(R.id.closeWar);
				btWar.setVisibility(View.VISIBLE);

				btWar = (Button) findViewById(R.id.reinWar);
				btWar.setVisibility(View.GONE);
				generatingQlink = false;
				return;
			}

			try {
				if (jsonr != null) {
					tn = (String) jsonr.get("tn");
					tnlk = (String) jsonr.get("tnlk");
				}
				final TextView descriptionText = (TextView) findViewById(R.id.detail_description_content);
				final TextView descriptionTextDN = (TextView) findViewById(R.id.detail_description_content_DN);
				final Button showAll = (Button) findViewById(R.id.detail_read_all);
				final CheckBox detailReadCheck = (CheckBox) findViewById(R.id.detail_read_check);
				showAll.setVisibility(View.VISIBLE);
				showAll.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showAll.setVisibility(View.GONE);
						descriptionTextDN.setText(tn);
						descriptionText.setText(resources
								.getString(R.string.dn_description));
						descriptionText.setVisibility(View.VISIBLE);
						descriptionTextDN.setVisibility(View.VISIBLE);
						detailReadCheck.setVisibility(View.VISIBLE);
					}
				});
			} catch (JSONException e) {
				Button btDN = (Button) findViewById(R.id.detail_read_all);
				btDN.setVisibility(View.GONE);
			}

			LinearLayout ll = (LinearLayout) findViewById(R.id.my_toolbar);
			ll.setVisibility(View.GONE);

			FrameLayout fl = (FrameLayout) findViewById(R.id.my_content);
			fl.setVisibility(View.GONE);

			MenuItem settingsItem = appMenu.findItem(R.id.action_settings);
			settingsItem.setVisible(false);

			MenuItem item = appMenu.findItem(R.id.btFileBar);

			item = appMenu.findItem(R.id.btFileMenu);
			item.setVisible(false);

			item = appMenu.findItem(R.id.btDraw);

			item = appMenu.findItem(R.id.action_share);
			item.setVisible(true);

			item = appMenu.findItem(R.id.btClip);
			item.setVisible(true);

			RelativeLayout rl = (RelativeLayout) findViewById(R.id.tap_to_init);
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			if (!sharedPrefs.getBoolean("helpShareViewed", false)) {
				rl.setVisibility(View.VISIBLE);
			}

			rl = (RelativeLayout) findViewById(R.id.resultData);
			rl.setVisibility(View.VISIBLE);

			EditText etlink = (EditText) findViewById(R.id.link);
			etlink.setText(urlHash + "#" + fpassword);
			etlink.setVisibility(View.VISIBLE);
			etlink.requestFocus();
			etlink.setKeyListener(null);

			TextView expireEdit = (TextView) findViewById(R.id.expire);
			expireEdit.setText("This qlink expire on: " + expire);
			expireEdit.setVisibility(View.VISIBLE);

			msg.setVisibility(View.GONE);
			msg.setText("");

			/*
			 * CheckBox imprTx = (CheckBox) findViewById(R.id.checkImprint);
			 * imprTx.setChecked(false); imprint = "false";
			 * imprTx.setVisibility(View.GONE);
			 */

			Button bt = (Button) findViewById(R.id.btCreate);
			bt.setVisibility(View.GONE);

			bt = (Button) findViewById(R.id.btDelete);
			bt.setVisibility(View.GONE);

			bt = (Button) findViewById(R.id.newLink);
			bt.setVisibility(View.VISIBLE);

			progressEncripting.setVisibility(View.GONE);

			compartir(null);

			if (getCurrentFocus() != null) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(getCurrentFocus()
						.getWindowToken(), 0);
			}
			generatingQlink = false;
			pUrlHash = urlHash;
		} catch (ClientProtocolException e) {
			generatingQlink = false;
		} catch (IOException e) {
			generatingQlink = false;
		}
	}

	public void navigateToStatusPage(View v) {
		sendMessageToService(0, tn.split(" ")[1].trim(),
				pUrlHash.split("/")[4].substring(0, 5));
		CheckBox readCheck = (CheckBox) findViewById(R.id.detail_read_check);
		readCheck.setVisibility(View.GONE);
		readCheck.setChecked(false);
	}

	public void doNothing(View v) {

	}

	public void newQlink(View v) {
		encFiles.clear();
		namesFiles.clear();

		TextView descriptionText = (TextView) findViewById(R.id.detail_description_content);
		TextView descriptionTextDN = (TextView) findViewById(R.id.detail_description_content_DN);
		CheckBox detailReadCheck = (CheckBox) findViewById(R.id.detail_read_check);
		descriptionText.setVisibility(View.GONE);
		descriptionTextDN.setVisibility(View.GONE);
		detailReadCheck.setSelected(false);
		detailReadCheck.setVisibility(View.GONE);

		RelativeLayout rl = (RelativeLayout) findViewById(R.id.resultData);
		rl.setVisibility(View.GONE);

		rl = (RelativeLayout) findViewById(R.id.tap_to_init);
		rl.setVisibility(View.GONE);

		LinearLayout ll = (LinearLayout) findViewById(R.id.my_toolbar);
		ll.setVisibility(View.VISIBLE);

		FrameLayout fl = (FrameLayout) findViewById(R.id.my_content);
		fl.setVisibility(View.VISIBLE);

		MenuItem settingsItem = appMenu.findItem(R.id.action_settings);
		settingsItem.setVisible(true);

		MenuItem item = appMenu.findItem(R.id.btFileBar);
		item.setVisible(true);

		item = appMenu.findItem(R.id.btFileMenu);
		item.setVisible(true);

		item = appMenu.findItem(R.id.btDraw);
		item.setVisible(true);

		item = appMenu.findItem(R.id.action_share);
		item.setVisible(false);

		item = appMenu.findItem(R.id.btClip);
		item.setVisible(false);

		/*
		 * item = appMenu.findItem(R.id.btAbout); item.setVisible(true);
		 */

		EditText msgE = (EditText) findViewById(R.id.msg);
		msgE.setVisibility(View.VISIBLE);
		msgE.setText("");

		/*
		 * CheckBox imprTx = (CheckBox) findViewById(R.id.checkImprint);
		 * imprTx.setChecked(false); imprint = "false";
		 * imprTx.setVisibility(View.VISIBLE);
		 */

		Button bt = (Button) findViewById(R.id.btCreate);
		bt.setVisibility(View.VISIBLE);

		bt = (Button) findViewById(R.id.newLink);
		bt.setVisibility(View.GONE);

		bt = (Button) findViewById(R.id.btDelete);
		bt.setVisibility(View.GONE);

	}

	public void compartir(View v) {
		EditText etlink = (EditText) findViewById(R.id.link);
		shareText = "" + etlink.getText().toString();

		MenuItem shareItem = appMenu.findItem(R.id.action_share);
		shareItem.setVisible(true);
		mShareActionProvider = (ShareActionProvider) MenuItemCompat
				.getActionProvider(shareItem);
		mShareActionProvider.setShareIntent(getDefaultIntentDespl());
		MenuItemCompat.setOnActionExpandListener(shareItem,
				new OnActionExpandListener() {

					@Override
					public boolean onMenuItemActionExpand(MenuItem arg0) {
						return true;
					}

					@Override
					public boolean onMenuItemActionCollapse(MenuItem arg0) {
						return true;
					}
				});
	}

	@SuppressWarnings("deprecation")
	public void clipToCl(View v) {
		EditText etlink = (EditText) findViewById(R.id.link);
		String textCp = etlink.getText().toString();
		if (textCp.length() != 0) {
			if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(textCp);
				Toast.makeText(getApplicationContext(),
						resources.getString(R.string.cp_to_clipboard),
						Toast.LENGTH_LONG).show();
			} else {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData
						.newPlainText("Clip", textCp);
				Toast.makeText(getApplicationContext(),
						resources.getString(R.string.cp_to_clipboard),
						Toast.LENGTH_LONG).show();
				clipboard.setPrimaryClip(clip);
			}
		} else {
			Toast.makeText(getApplicationContext(), "Nothing to Copy",
					Toast.LENGTH_SHORT).show();
		}
	}

	public void clipDN(View v) {
		TextView etlink = (TextView) findViewById(R.id.detail_description_content_DN);
		String textCp = etlink.getText().toString();
		textCp = textCp.split(" ")[1];
		if (textCp.length() != 0) {
			if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(textCp);
				Toast.makeText(getApplicationContext(),
						resources.getString(R.string.dn_to_clipboard),
						Toast.LENGTH_LONG).show();
			} else {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData
						.newPlainText("Clip", textCp);
				Toast.makeText(getApplicationContext(),
						resources.getString(R.string.dn_to_clipboard),
						Toast.LENGTH_LONG).show();
				clipboard.setPrimaryClip(clip);
			}
		} else {
			Toast.makeText(getApplicationContext(), "Nothing to Copy",
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		checkIfServiceIsRunning();
		System.gc();
	}

	@Override
	public void onPause() {
		try {
			doUnbindService();
		} catch (Throwable t) {
		}
		super.onPause();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	private String getRandomHexString(int numchars) {
		SecureRandom r = new SecureRandom();
		StringBuffer sb = new StringBuffer();
		while (sb.length() < numchars) {
			sb.append(Integer.toHexString(r.nextInt()));
		}

		return sb.toString().substring(0, numchars);
	}

	private String generateRandomString(int length, String seeded) {
		SecureRandom r = new SecureRandom(seeded.getBytes());

		String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String randomString = "";
		for (int i = 0; i < length; i++) {
			int jind = r.nextInt(characters.length() - 1);
			randomString = randomString + characters.charAt(jind);
		}
		return randomString;
	}

	private static final int FILE_SELECT_CODE = 0;

	public void showFileChooser(View v) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		// special intent for Samsung file manager
		Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
		sIntent.putExtra("CONTENT_TYPE", "*/*");
		sIntent.addCategory(Intent.CATEGORY_DEFAULT);

		Intent chooserIntent;
		if (getPackageManager().resolveActivity(sIntent, 0) != null) {
			// it is device with samsung file manager
			chooserIntent = Intent.createChooser(sIntent,
					resources.getString(R.string.select_a_file));
			chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					new Intent[] { intent });
		} else {
			chooserIntent = Intent.createChooser(intent,
					resources.getString(R.string.select_a_file));
		}

		try {
			startActivityForResult(chooserIntent, FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void displayUserSettings() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPrefs.getBoolean("imprintOption", false))
			imprint = "true";
		else
			imprint = "false";
	}

	public String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null,
					null, null);
			int column_index = 0;
			if (cursor != null) {
				column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
			}

			if (cursor.getString(column_index) == null || cursor == null) {
				proj[0] = MediaStore.Video.Media.DATA;
				cursor = context.getContentResolver().query(contentUri, proj,
						null, null, null);
				column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
				cursor.moveToFirst();
			}

			if (cursor.getString(column_index) == null || cursor == null) {
				proj[0] = MediaStore.Audio.Media.DATA;
				cursor = context.getContentResolver().query(contentUri, proj,
						null, null, null);
				column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
				cursor.moveToFirst();
			}

			if (cursor != null) {
				return cursor.getString(column_index);
			}
		} catch (Exception e) {

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		String sdata = "";
		Boolean compressedFlag = false;
		byte fileContent[];
		switch (requestCode) {
		case SETTINGS_RESULT:
			displayUserSettings();
			break;
		case FILE_SELECT_CODE:
			if (resultCode == RESULT_OK) {
				TextView sizeAlert = (TextView) findViewById(R.id.sizeAlert);
				sizeAlert.setVisibility(View.GONE);

				Uri uri = data.getData();
				if (uri == null) {
					Toast.makeText(this,
							resources.getString(R.string.file_unreach),
							Toast.LENGTH_SHORT).show();
					return;
				}

				String path = null;

				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
					path = getPath5(this, uri);
				} else {
					try {
						path = getPath(this, uri);
					} catch (URISyntaxException e) {
					}
				}

				if (path == null) {
					path = getRealPathFromURI(this, uri);
				}

				// Get the file instance
				Boolean localFile = true;

				FileInputStream fin = null;
				InputStream iin = null;
				if (path != null && isLocal(path)) {
					file = new File(path);
					try {
						fin = new FileInputStream(file);
					} catch (FileNotFoundException e) {
						Toast.makeText(this,
								resources.getString(R.string.file_notfound),
								Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					localFile = false;
					try {
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
							if (path != null) {
								iin = new URL(path).openStream();
							} else {
								Toast.makeText(
										this,
										resources
												.getString(R.string.file_unreach),
										Toast.LENGTH_SHORT).show();
								return;
							}
						} else {
							iin = new URL(uri.getLastPathSegment())
									.openStream();
						}
					} catch (MalformedURLException e) {
						Toast.makeText(this,
								resources.getString(R.string.file_unreach),
								Toast.LENGTH_SHORT).show();
						return;

					} catch (IOException e) {
						Toast.makeText(this,
								resources.getString(R.string.file_unreach),
								Toast.LENGTH_SHORT).show();
						return;
					}

					if (iin == null) {
						Toast.makeText(this,
								resources.getString(R.string.file_unreach),
								Toast.LENGTH_SHORT).show();
						return;
					}
				}

				try {
					String mimeType = null;
					String extension = null;
					// Reads up to certain bytes of data from this input stream
					// into an array of bytes.
					if (localFile) {
						mimeType = URLConnection.guessContentTypeFromName(file
								.getName());

						if (mimeType.equals("image/jpeg")
								|| mimeType.equals("image/png")) {
							fileContent = new byte[(int) file.length()];
							fin.read(fileContent);
							fin.close();
							if (file.length() > 900000) {
								fileContent = resizeImage(fileContent, mimeType);
								compressedFlag = true;
							}

							if (fileContent.length > 1048576) {
								fin.close();
								sizeAlert.setVisibility(View.VISIBLE);
								Toast.makeText(
										this,
										resources
												.getString(R.string.max_file_size),
										Toast.LENGTH_SHORT).show();
								return;
							}
						} else {
							// MAX SIZE OF ATTACH
							if (file.length() > 1048576) {
								fin.close();
								sizeAlert.setVisibility(View.VISIBLE);
								Toast.makeText(
										this,
										resources
												.getString(R.string.max_file_size),
										Toast.LENGTH_SHORT).show();
								return;
							}
							fileContent = new byte[(int) file.length()];
							fin.read(fileContent);
						}
					} else {
						ContentResolver cR = this.getContentResolver();
						MimeTypeMap mime = MimeTypeMap.getSingleton();
						mimeType = cR.getType(uri);

						fileContent = readBytes(iin, mimeType);
						if (fileContent == null) {
							iin.close();
							sizeAlert.setVisibility(View.VISIBLE);
							Toast.makeText(
									this,
									resources.getString(R.string.max_file_size),
									Toast.LENGTH_SHORT).show();
							return;
						}

						if (mimeType.equals("image/jpeg")
								|| mimeType.equals("image/png")) {
							if (fileContent.length > 900000) {
								fileContent = resizeImage(fileContent, mimeType);
								compressedFlag = true;
							}
						}

						extension = mime.getExtensionFromMimeType(cR
								.getType(uri));
					}

					// MAX SIZE OF ATTACH
					if (fileContent.length > 1048576) {
						sizeAlert.setVisibility(View.VISIBLE);
						Toast.makeText(this,
								resources.getString(R.string.max_file_size),
								Toast.LENGTH_SHORT).show();
						return;
					}

					// create string from byte array
					sdata = "data:"
							+ mimeType
							+ ";base64,"
							+ Base64.encodeToString(fileContent, Base64.DEFAULT);
					encFiles.add(sdata);

					if (localFile) {
						namesFiles.add(file.getName());
					} else {
						namesFiles.add("ifile." + extension);
					}

					if (compressedFlag) {
						Toast.makeText(this,
								resources.getString(R.string.compressed),
								Toast.LENGTH_LONG).show();
					}

					MenuItem attachItem = appMenu.findItem(R.id.btFileBar);
					attachItem.setVisible(false);

					attachItem = appMenu.findItem(R.id.btFileMenu);
					attachItem.setVisible(false);

					attachItem = appMenu.findItem(R.id.btDraw);
					attachItem.setVisible(false);

					Button bt = (Button) findViewById(R.id.btDelete);

					if (localFile) {
						bt.setText(file.getName());
					} else {
						bt.setText("ifile." + extension);
					}
					bt.setVisibility(View.VISIBLE);
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				} finally {
					if (localFile) {
						try {
							if (fin != null)
								fin.close();
						} catch (IOException e) {
						}
					} else {
						try {
							if (iin != null)
								iin.close();
						} catch (IOException e) {
						}
					}
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public byte[] readBytes(InputStream inputStream, String mimeType)
			throws IOException {
		int limit = 1048576; // 1MB
		if (mimeType.equals("image/jpeg") || mimeType.equals("image/png")) {
			limit = 10485760; // 10MB
		}
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);

			// MAX SIZE OF ATTACH
			if (byteBuffer.size() > limit) {
				return null;
			}
		}

		return byteBuffer.toByteArray();
	}

	public void deleteFile(View v) {
		encFiles.clear();
		namesFiles.clear();

		MenuItem attachItem = appMenu.findItem(R.id.btFileBar);
		attachItem.setVisible(true);

		attachItem = appMenu.findItem(R.id.btFileMenu);
		attachItem.setVisible(true);

		attachItem = appMenu.findItem(R.id.btDraw);
		attachItem.setVisible(true);

		Button bt = (Button) findViewById(R.id.btDelete);
		bt.setText("");
		bt.setVisibility(View.GONE);
	}

	public String getPath5(Context context, Uri uri) {
		final boolean isKitKat = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/"
							+ split[1];
				}
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			try {
				return getPath(context, uri);
			} catch (URISyntaxException e) {
			}
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public boolean isLocal(String url) {
		if (url != null && !url.startsWith("http://")
				&& !url.startsWith("https://")) {
			return true;
		}
		return false;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @param selection
	 *            (Optional) Filter used in the query.
	 * @param selectionArgs
	 *            (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri,
			String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri
				.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri
				.getAuthority());
	}

	public String getPath(Context context, Uri uri) throws URISyntaxException {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection,
						null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public void toggleImprint(View v) {
		if (((CheckBox) v).isChecked()) {
			imprint = "true";
		} else {
			imprint = "false";
		}
	}

	private Intent getDefaultIntentDespl() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, shareText);
		intent.putExtra(Intent.EXTRA_SUBJECT,
				resources.getString(R.string.share_subject));
		return intent;
	}

	private Intent getDefaultIntent() {
		Intent emailIntent = new Intent();
		emailIntent.setAction(Intent.ACTION_SEND);
		emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(shareText));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT,
				resources.getString(R.string.share_subject));
		emailIntent.setType("message/rfc822");

		PackageManager pm = getPackageManager();
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("text/plain");

		Intent openInChooser = Intent.createChooser(emailIntent,
				resources.getString(R.string.share_chooser_text));

		List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
		List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();
		for (int i = 0; i < resInfo.size(); i++) {
			ResolveInfo ri = resInfo.get(i);
			String packageName = ri.activityInfo.packageName;
			if (packageName.contains("android.email")) {
				emailIntent.setPackage(packageName);
			} else if (packageName.contains("viber")
					|| packageName.contains("mms")
					|| packageName.contains("whatsapp")
					|| packageName.contains("android.apps.plus")
					|| packageName.contains("com.google.android.talk")) {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName(packageName,
						ri.activityInfo.name));
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				if (packageName.contains("mms")) {
					intent.putExtra(Intent.EXTRA_TEXT, shareText);
				} else if (packageName.contains("whatsapp")) {
					intent.putExtra(Intent.EXTRA_TEXT, shareText);
				} else if (packageName.contains("viber")) {
					intent.putExtra(Intent.EXTRA_TEXT, shareText);
				} else {
					intent.putExtra(Intent.EXTRA_TEXT, shareText);
				}

				intentList.add(new LabeledIntent(intent, packageName, ri
						.loadLabel(pm), ri.icon));
			}
		}

		LabeledIntent[] extraIntents = intentList
				.toArray(new LabeledIntent[intentList.size()]);

		openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
		return openInChooser;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}

	@Override
	public void onUserInteraction() {
		Long curDate = new Date().getTime();
		password = generateRandomString(32, curDate.toString() + password);
		password = Base64.encodeToString(password.getBytes(), Base64.DEFAULT)
				.substring(0, 32);
	}

	@Override
	public void onBackPressed() {
		if (currentViewId != null) {
			if (currentViewId == R.layout.drawer) {
				currentViewId = R.layout.activity2_qlink;
				ViewGroup vg = (ViewGroup) (drawView.getParent());
				vg.removeView(drawView);

				MenuItem item = appMenu.findItem(R.id.action_settings);
				item.setVisible(true);

				item = appMenu.findItem(R.id.btDraw);
				item.setVisible(true);

				item = appMenu.findItem(R.id.btFileBar);
				item.setVisible(true);

				item = appMenu.findItem(R.id.btFileMenu);
				item.setVisible(true);

				return;
			}
		}

		super.onBackPressed();
		AppUtil.clearCacheStorage(this);
	}

	@Override
	public void onStop() {
		try {
			doUnbindService();
		} catch (Throwable t) {
		}
		super.onStop();
		System.gc();
		AppUtil.clearCacheStorage(this);
		AppUtil.trimCache(this);
	}

	@Override
	public void onDestroy() {
		try {
			doUnbindService();
		} catch (Throwable t) {
		}
		super.onDestroy();
		System.gc();
		AppUtil.clearCacheStorage(this);
		AppUtil.trimCache(this);
	}

	private ShareActionProvider mShareActionProvider;
	private Menu appMenu = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		appMenu = menu;

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bar, menu);

		MenuItem item = appMenu.findItem(R.id.btClip);
		item.setVisible(false);

		item = appMenu.findItem(R.id.action_share);
		item.setVisible(false);

		return super.onCreateOptionsMenu(menu);
	}

	private View drawView;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.btDraw:
			LayoutInflater inflater = getLayoutInflater();
			drawView = inflater.inflate(R.layout.drawer, null);
			currentViewId = R.layout.drawer;
			addContentView(drawView, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.FILL_PARENT));

			MenuItem itemx = appMenu.findItem(R.id.action_settings);
			itemx.setVisible(false);

			itemx = appMenu.findItem(R.id.btDraw);
			itemx.setVisible(false);

			itemx = appMenu.findItem(R.id.btFileBar);
			itemx.setVisible(false);

			itemx = appMenu.findItem(R.id.btFileMenu);

			if (getCurrentFocus() != null) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(getCurrentFocus()
						.getWindowToken(), 0);
			}
			break;
		case R.id.btFileBar:
			showFileChooser(null);
			break;
		case R.id.btClip:
			clipToCl(null);
			break;
		case R.id.action_settings:
			Intent i = new Intent(getApplicationContext(),
					UserSettingActivity.class);
			startActivityForResult(i, SETTINGS_RESULT);
		default:
			break;
		}

		return true;
	}

	byte[] resizeImage(byte[] input, String mimeType) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;

		Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length,
				options);
		Integer width = original.getWidth();
		Integer height = original.getHeight();

		float sizeRelation = (new Integer(input.length)).floatValue() / 1048576.0f;
		Integer newWidth = (int) (width.floatValue() / sizeRelation);
		float scale = newWidth.floatValue() / width.floatValue();

		int newHeight = (int) (scale * height);
		Bitmap resized = Bitmap.createScaledBitmap(original, newWidth,
				newHeight, true);

		ByteArrayOutputStream blob = new ByteArrayOutputStream();
		if (mimeType.equals("image/jpeg")) {
			resized.compress(Bitmap.CompressFormat.JPEG, 80, blob);
		}
		if (mimeType.equals("image/png")) {
			resized.compress(Bitmap.CompressFormat.PNG, 80, blob);
		}
		return blob.toByteArray();
	}

	public void initDraw(View view) {
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.tap_to_draw);
		rl.setVisibility(View.GONE);
	}

	public void initShare(View view) {
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.tap_to_init);
		rl.setVisibility(View.GONE);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putBoolean("helpShareViewed", true);
		editor.commit();
	}

	public void setToErase(View view) {
		GestureOverlayView gestureView = (GestureOverlayView) findViewById(R.id.signaturePad);
		gestureView.cancelClearAnimation();
		gestureView.clear(true);
	}

	public void saveSig(View view) {
		try {
			GestureOverlayView gestureView = (GestureOverlayView) findViewById(R.id.signaturePad);
			gestureView.setDrawingCacheEnabled(true);
			Bitmap bm = Bitmap.createBitmap(gestureView.getDrawingCache());
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			bm.compress(Bitmap.CompressFormat.PNG, 80, blob);

			byte[] fileContent = blob.toByteArray();

			if (fileContent.length > 900000) {
				fileContent = resizeImage(fileContent, "image/png");
			}

			String sdata = "data:" + "image/png" + ";base64,"
					+ Base64.encodeToString(fileContent, Base64.DEFAULT);
			encFiles.add(sdata);

			namesFiles.add("qlinkDraw.png");

			Button bt = (Button) findViewById(R.id.btDelete);

			bt.setText("qlinkDraw.png");
			bt.setVisibility(View.VISIBLE);

			ViewGroup vg = (ViewGroup) (drawView.getParent());
			vg.removeView(drawView);
			currentViewId = R.layout.activity2_qlink;

			MenuItem item = appMenu.findItem(R.id.action_settings);
			item.setVisible(true);

			item = appMenu.findItem(R.id.btDraw);
			item.setVisible(false);

			item = appMenu.findItem(R.id.btFileBar);
			item.setVisible(false);

			item = appMenu.findItem(R.id.btFileMenu);
			item.setVisible(false);
		} catch (Exception e) {
		}
	}

}
