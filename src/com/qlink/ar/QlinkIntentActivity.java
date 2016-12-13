package com.qlink.ar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
public class QlinkIntentActivity extends ActionBarActivity {

	int sdk = android.os.Build.VERSION.SDK_INT;
	private Boolean isInternetConnected = false;
	private Boolean isHostConnected = false;
	private List<String> decFiles = new ArrayList<String>();
	private JSONArray namesFiles = new JSONArray();
	private RelativeLayout progressDecrypting;
	private Typeface fontTypeFace;
	private Handler clockHandler2 = new Handler();
	private Handler clockHandler3 = new Handler();
	private Resources resources;
	private String imprint = null;
	private Boolean blurText = true;

	protected String host = "qlink";
	protected int port = 443;
	protected String protocol = "https";
	private String currentAppWebVersion = null;
	private String currentWebVersion = "0";
	private Boolean forceUpdateFlag = false;
	private String forceText = "";
	protected String appVersion = "0";
	private ActionBar actionBar;

	private Integer currentViewId = R.layout.read_qlink;

	private Boolean conversationFlag = false;
	private List<EditText> listMsgsConversation = new ArrayList<EditText>();

	protected String lastQl = "";
	private ScrollView smsgE;

	private Context context;
	private Properties pfile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (android.os.Build.VERSION.SDK_INT < 11) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		super.onCreate(savedInstanceState);

		// Read properties
		if ( readProperties() == false ) {
			return;
		}
		
		// Read preferences
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPrefs.getBoolean("prefLockScreen", false))
			blurText = true;
		else
			blurText = false;

		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			appVersion = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}

		actionBar = getSupportActionBar();
		actionBar.show();
		actionBar.setTitle(" Qlink.it");
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setLogo(R.drawable.ic_launcher);

		resources = getResources();
		if (sdk > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		fontTypeFace = Typeface.createFromAsset(this.getAssets(),
				"font/Roboto.ttf");

		preCheck();
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
			setContentView(R.layout.read_qlink);
			TextView warning = (TextView) findViewById(R.id.warning2text);
			warning.setText(resources.getString(R.string.host_disconnected));
			warning.setVisibility(View.VISIBLE);

			Button btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.GONE);

			btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.VISIBLE);

			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);
			return false;
		}
		return true;
	}
	
	private void preCheck() {
		checkInternetConnection();
		checkHostConnection();

		if (!isInternetConnected) {
			setContentView(R.layout.read_qlink);
			TextView warning = (TextView) findViewById(R.id.warning2text);
			warning.setText(resources.getString(R.string.inet_disconnected));
			warning.setVisibility(View.VISIBLE);

			Button btWar = (Button) findViewById(R.id.closeWar);
			btWar.setVisibility(View.GONE);

			btWar = (Button) findViewById(R.id.reinWar);
			btWar.setVisibility(View.VISIBLE);

			RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
			layWar.setOnClickListener(null);
			layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			layWar.setVisibility(View.VISIBLE);
		} else {
			if (!isHostConnected) {
				setContentView(R.layout.read_qlink);
				TextView warning = (TextView) findViewById(R.id.warning2text);
				warning.setText(resources.getString(R.string.host_disconnected));
				warning.setVisibility(View.VISIBLE);

				Button btWar = (Button) findViewById(R.id.closeWar);
				btWar.setVisibility(View.GONE);

				btWar = (Button) findViewById(R.id.reinWar);
				btWar.setVisibility(View.VISIBLE);

				RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
				layWar.setOnClickListener(null);
				layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
				layWar.setVisibility(View.VISIBLE);
			} else {
				Boolean retrieveVersion = getCurrentAppVersion();
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
					setContentView(R.layout.read_qlink);
					TextView warning = (TextView) findViewById(R.id.warning2text);
					warning.setText(resources
							.getString(R.string.inet_disconnected));
					warning.setVisibility(View.VISIBLE);

					Button btWar = (Button) findViewById(R.id.closeWar);
					btWar.setVisibility(View.GONE);

					btWar = (Button) findViewById(R.id.reinWar);
					btWar.setVisibility(View.VISIBLE);

					RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
					layWar.setOnClickListener(null);
					layWar.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
					layWar.setVisibility(View.VISIBLE);

					return;
				}

				setContentView(R.layout.read_qlink);
				progressDecrypting = (RelativeLayout) findViewById(R.id.decripting);
				TextView txw = (TextView) findViewById(R.id.decriptingText);
				txw.setText(resources.getString(R.string.searching));

				txw.setTypeface(fontTypeFace);
				progressDecrypting.setVisibility(View.VISIBLE);
				ProgressBar t = (ProgressBar) findViewById(R.id.decriptingImg);
				t.startAnimation(AnimationUtils.loadAnimation(this,
						R.anim.splash));

				clockHandler2.postDelayed(clockTask2, 500);
			}
		}
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

	public Boolean getCurrentAppVersion() {
		String locale = Locale.getDefault().getLanguage();

		HttpGet httpget = new HttpGet(protocol + "://" + host
				+ "/appversion?lang=" + locale);

		HttpParams httpParameters = new BasicHttpParams();
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

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		FontChangeHelper fontChanger = new FontChangeHelper(getAssets(),
				"font/Roboto.ttf");
		fontChanger.replaceFonts((ViewGroup) this
				.findViewById(android.R.id.content));
	}

	public void closeWarning(View v) {
		RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
		layWar.setVisibility(View.GONE);
	}

	public void reintentWarning(View v) {
		RelativeLayout layWar = (RelativeLayout) findViewById(R.id.warning2);
		layWar.setVisibility(View.GONE);

		preCheck();
	}

	private Runnable clockTask2 = new Runnable() {

		@Override
		public void run() {
			initDecrypt();
		}

	};

	JSONObject json = null;
	JSONObject jsonr = null;
	String fragment = null;

	public void initDecrypt() {
		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			final List<String> segments = intent.getData().getPathSegments();
			String hash = null;
			String servnum = null;
			if (segments.size() > 0) {
				servnum = segments.get(0);
			}
			if (segments.size() > 1) {
				hash = segments.get(1);
			}

			fragment = intent.getData().getFragment();
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(protocol + "://" + host
					+ "/readmessage");

			ResponseHandler<String> responseHandler = new BasicResponseHandler();

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						4);
				nameValuePairs.add(new BasicNameValuePair("from", "app_android"
						+ appVersion));
				nameValuePairs.add(new BasicNameValuePair("servnum", servnum));
				nameValuePairs.add(new BasicNameValuePair("hash", hash));
				String locale = Locale.getDefault().getLanguage();
				nameValuePairs.add(new BasicNameValuePair("lang", locale));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				String response = httpclient.execute(httppost, responseHandler);

				json = null;
				jsonr = null;
				String msgEncripted = null;
				Boolean msgRead = false;
				try {
					jsonr = new JSONObject(response);
					msgEncripted = (String) jsonr.get("message");
					msgRead = (Boolean) jsonr.getBoolean("read");
					imprint = (String) jsonr.get("imprint");
					json = new JSONObject(msgEncripted.substring(1,
							msgEncripted.length() - 1));

					TextView txw = (TextView) findViewById(R.id.decriptingText);
					txw.setText(resources.getString(R.string.decrypting));

					clockHandler3.postDelayed(clockTask3, 1500);
				} catch (JSONException e) {
					try {
						json = new JSONObject(msgEncripted);
						TextView txw = (TextView) findViewById(R.id.decriptingText);
						txw.setText(resources.getString(R.string.decrypting));

						clockHandler3.postDelayed(clockTask3, 1500);
					} catch (JSONException e1) {
						progressDecrypting.setVisibility(View.GONE);

						TextView msgT = (TextView) findViewById(R.id.warning1);
						if (msgRead == true) {
							msgT.setText(resources.getString(R.string.ops_read));
						} else {
							msgT.setText(resources.getString(R.string.ops));
						}

						msgT.setVisibility(View.VISIBLE);

						Button btnE = (Button) findViewById(R.id.newLink2);
						btnE.setVisibility(View.VISIBLE);

						return;
					}
				}
			} catch (ClientProtocolException e) {
			} catch (IOException e) {
			}
		}
	}

	public void finalDecrypt() {
		decFiles.clear();
		System.gc();

		Integer iter = 500;

		try {
			iter = Integer.valueOf(json.getString("iter"));
		} catch (NumberFormatException e1) {
			iter = 500;
		} catch (JSONException e1) {
			iter = 500;
		}

		AesUtil util = new AesUtil(256, iter);
		String textDecrypt = null;
		try {
			textDecrypt = util.decrypt(json.getString("salt"),
					json.getString("iv"), fragment, json.getString("data"));
		} catch (JSONException e) {
		}

		String doDecompress = "true";

		try {
			doDecompress = json.getString("decom");
		} catch (NumberFormatException e1) {
			doDecompress = "true";
		} catch (JSONException e1) {
			doDecompress = "true";
		}

		if (doDecompress.equals("true")) {
			textDecrypt = StringEscapeUtils.unescapeHtml(LZW
					.decompress(textDecrypt));
		} else {
			textDecrypt = StringEscapeUtils.unescapeHtml(textDecrypt);
		}
		textDecrypt = textDecrypt.replace("<br />", "\n");
		textDecrypt = EscapeUtil.unEscapeHtmlEntities(textDecrypt);

		try {
			namesFiles = (JSONArray) jsonr.get("namesFiles");
		} catch (JSONException e) {
		}

		JSONArray encFiles = null;
		try {
			encFiles = (JSONArray) jsonr.get("encFiles");

			for (int i = 0; i < encFiles.length(); i++) {
				json = new JSONObject(encFiles.getString(0)
						.replace("\\\"", "\"").replace("\"{", "{")
						.replace("}\"", "}"));
				try {
					decFiles.add(util.decrypt(json.getString("salt"),
							json.getString("iv"), fragment,
							json.getString("data")));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (imprint != null && !imprint.equals("")) {
			TextView tx = (TextView) findViewById(R.id.imprint);
			tx.setText("qlinked from: " + imprint);
		} else {
			TextView tx = (TextView) findViewById(R.id.imprint);
			tx.setText(R.string.imprint_not_used);
		}

		smsgE = (ScrollView) findViewById(R.id.scroller);
		smsgE.setOnLongClickListener(lcl);
		smsgE.setOnTouchListener(tl);

		Button btTapE = (Button) findViewById(R.id.button_tar_to_read);
		btTapE.setOnLongClickListener(null);
		btTapE.setOnTouchListener(tl);

		EditText msgE = (EditText) findViewById(R.id.msg);
		msgE.setKeyListener(null);
		msgE.setOnTouchListener(tl);

		int versionMessager = textDecrypt.indexOf("%%A%%");
		listMsgsConversation.clear();

		String[] msgsArray = textDecrypt.split("%%A%%");

		if (versionMessager == -1 || msgsArray.length < 3) {
			if (versionMessager == -1) {
				lastQl = "%%A%%" + textDecrypt + "%%C%%";
			} else {
				String[] msgOneArray = msgsArray[1].split("%%C%%");
				textDecrypt = msgOneArray[0];
				lastQl = "%%A%%" + textDecrypt + "%%C%%";
			}
			msgE.setVisibility(View.VISIBLE);
			msgE.setText(textDecrypt);

			if (blurText) {
				RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
				tap.setVisibility(View.VISIBLE);
				if (android.os.Build.VERSION.SDK_INT < 11) {
					msgE.setTextColor(0x00000000);
				} else {
					msgE.setTextColor(0xffc3c3c3);
					msgE.setAlpha(0.7f);
				}
				msgE.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
			} else {
				if (android.os.Build.VERSION.SDK_INT < 11) {
					msgE.setTextColor(0xff000000);
				} else {
					msgE.setTextColor(0xff000000);
					msgE.setAlpha(1.0f);
				}
				msgE.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
			}
		} else {
			lastQl = textDecrypt;
			this.conversationFlag = true;
			msgE.setVisibility(View.GONE);
			msgE.setText("");
			
			textDecrypt = "";
			LinearLayout textContainer = (LinearLayout) findViewById(R.id.textContainer);
			int istart = 2;
			int color = Color.argb(255, 166, 236, 238);
			for (String msA : msgsArray) {
				if (msA.trim().equals(""))
					continue;
				String[] msgOneArray = msA.split("%%C%%");
				textDecrypt = msgOneArray[0];

				EditText mt = new EditText(this);
				mt.setText(textDecrypt);
				mt.setTextSize(15);
				mt.setPadding(25, 25, 25, 35);

				mt.setKeyListener(null);
				mt.setOnTouchListener(tl);

				LayoutParams par = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				int marginDef = 10;
				int marginMax = 40;
				if (istart % 2 == 0) {
					color = Color.argb(255, 166, 236, 238);
					mt.setGravity(Gravity.LEFT);
					par.setMargins(getDp(marginDef, this),
							getDp(marginDef, this), getDp(marginMax, this),
							getDp(marginDef, this));
					mt.setBackgroundResource(R.drawable.edit_text_globe2);
				} else {
					color = Color.argb(255, 248, 248, 248);
					mt.setGravity(Gravity.LEFT);
					par.setMargins(getDp(marginMax, this),
							getDp(marginDef, this), getDp(marginDef, this),
							getDp(marginDef, this));
					mt.setBackgroundResource(R.drawable.edit_text_globe);
				}
				mt.setLayoutParams(par);
				mt.setScrollBarStyle(View.SCROLL_AXIS_VERTICAL);

				if (blurText) {
					RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
					tap.setVisibility(View.VISIBLE);
					if (android.os.Build.VERSION.SDK_INT < 11) {
						mt.setTextColor(0x00000000);
					} else {
						mt.setTextColor(0xffc3c3c3);
						mt.setAlpha(0.7f);
					}
					mt.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
				} else {
					if (android.os.Build.VERSION.SDK_INT < 11) {
						mt.setTextColor(0xff000000);
					} else {
						mt.setTextColor(0xff000000);
						mt.setAlpha(1.0f);
					}
					mt.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
				}

				listMsgsConversation.add(mt);

				textContainer.addView(mt, istart);
				istart++;
			}
		}

		TextView tx = (TextView) findViewById(R.id.notice1);
		tx.setText(Html.fromHtml(resources.getString(R.string.notice1)));
		tx.setVisibility(View.VISIBLE);

		LinearLayout ll = (LinearLayout) findViewById(R.id.reply_button_container);
		ll.setVisibility(View.VISIBLE);

		ll = (LinearLayout) findViewById(R.id.imp_container);
		ll.setVisibility(View.VISIBLE);

		Button btnE = null;
		if (Float.valueOf(currentWebVersion) >= 2.0f) {
			btnE = (Button) findViewById(R.id.newLink);
			btnE.setVisibility(View.VISIBLE);

			btnE = (Button) findViewById(R.id.helpLink);
			btnE.setVisibility(View.VISIBLE);

			btnE = (Button) findViewById(R.id.forwardLink);
			btnE.setVisibility(View.VISIBLE);
		} else {
			btnE = (Button) findViewById(R.id.helpLink);
			btnE.setVisibility(View.GONE);

			btnE = (Button) findViewById(R.id.forwardLink);
			btnE.setVisibility(View.GONE);

			btnE = (Button) findViewById(R.id.newLink);
			btnE.setVisibility(View.VISIBLE);

			ViewGroup.LayoutParams params = btnE.getLayoutParams();
			params.width = ViewGroup.LayoutParams.FILL_PARENT;
			btnE.setLayoutParams(params);
		}

		MenuItem item = appMenu.findItem(R.id.btSave);
		item.setVisible(false); // cambiar a true para activar esta opción

		json = null;
		jsonr = null;
		System.gc();
		if (namesFiles.length() > 0) {
			btnE = (Button) findViewById(R.id.btFile);
			try {
				btnE.setText(namesFiles.getString(0));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			btnE.setVisibility(View.VISIBLE);
			String[] imgS = decFiles.get(0).split(",");
			String[] mimeType = imgS[0].split(";")[0].split(":");

			if (mimeType.length > 1) {
				if (mimeType[1].equals("image/jpeg")
						|| mimeType[1].equals("image/png")) {
					RelativeLayout relImage = (RelativeLayout) findViewById(R.id.attachImageContainer);
					relImage.setVisibility(View.VISIBLE);

					byte[] decodedString = Base64.decode(imgS[1],
							Base64.DEFAULT);
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPurgeable = true;

					Bitmap bitmap = BitmapFactory.decodeByteArray(
							decodedString, 0, decodedString.length, options);
					ImageView attachImage = (ImageView) findViewById(R.id.attachImage);
					attachImage.setImageBitmap(bitmap);

					tx = (TextView) findViewById(R.id.attachImageTitle);
					try {
						tx.setText(namesFiles.getString(0));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					ll = (LinearLayout) findViewById(R.id.actionAttach);
					ll.setVisibility(View.VISIBLE);
				}
			}

		}

		ll = (LinearLayout) findViewById(R.id.reply_button_container);
		ll.setVisibility(View.VISIBLE);

		ll = (LinearLayout) findViewById(R.id.imp_container);
		ll.setVisibility(View.VISIBLE);

		smsgE = (ScrollView) findViewById(R.id.scroller);
		smsgE.setVisibility(View.VISIBLE);
		smsgE.post(new Runnable() {
			@Override
			public void run() {
				smsgE.scrollTo(0, smsgE.getBottom());
			}
		});

	}

	OnLongClickListener lcl = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			return true;
		}
	};

	OnTouchListener tl = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (conversationFlag == true) {
				if (blurText) {
					for (EditText msgE : listMsgsConversation) {
						if (event.getAction() == MotionEvent.ACTION_DOWN
								|| event.getAction() == MotionEvent.ACTION_SCROLL
								|| event.getAction() == MotionEvent.ACTION_MOVE) {
							if (android.os.Build.VERSION.SDK_INT < 11) {
								msgE.setTextColor(0xff000000);
							} else {
								msgE.setTextColor(0xff000000);
								msgE.setAlpha(1.0f);
							}
							msgE.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
							RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
							tap.setVisibility(View.GONE);
						} else {
							if (android.os.Build.VERSION.SDK_INT < 11) {
								msgE.setTextColor(0x00000000);
							} else {
								msgE.setTextColor(0xffc3c3c3);
								msgE.setAlpha(0.7f);
							}
							msgE.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
							RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
							tap.setVisibility(View.VISIBLE);
						}
					}
				}
			} else {
				if (blurText) {
					EditText msgE = (EditText) findViewById(R.id.msg);
					if (event.getAction() == MotionEvent.ACTION_DOWN
							|| event.getAction() == MotionEvent.ACTION_SCROLL
							|| event.getAction() == MotionEvent.ACTION_MOVE) {
						if (android.os.Build.VERSION.SDK_INT < 11) {
							msgE.setTextColor(0xff000000);
						} else {
							msgE.setTextColor(0xff000000);
							msgE.setAlpha(1.0f);
						}
						msgE.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
						RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
						tap.setVisibility(View.GONE);
					} else {
						if (android.os.Build.VERSION.SDK_INT < 11) {
							msgE.setTextColor(0x00000000);
						} else {
							msgE.setTextColor(0xffc3c3c3);
							msgE.setAlpha(0.7f);
						}
						msgE.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
						RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
						tap.setVisibility(View.VISIBLE);
					}
				}
			}
			return false;
		}
	};

	OnTouchListener tls = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (conversationFlag == true) {
				if (blurText) {
					for (EditText msgE : listMsgsConversation) {
						if (event.getAction() == MotionEvent.ACTION_DOWN
								|| event.getAction() == MotionEvent.ACTION_SCROLL
								|| event.getAction() == MotionEvent.ACTION_MOVE) {
							if (android.os.Build.VERSION.SDK_INT < 11) {
								msgE.setTextColor(0xff000000);
							} else {
								msgE.setTextColor(0xff000000);
								msgE.setAlpha(1.0f);
							}
							msgE.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
							RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
							tap.setVisibility(View.GONE);
						} else {
							if (android.os.Build.VERSION.SDK_INT < 11) {
								msgE.setTextColor(0x00000000);
							} else {
								msgE.setTextColor(0xffc3c3c3);
								msgE.setAlpha(0.7f);
							}
							msgE.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
							RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
							tap.setVisibility(View.VISIBLE);
						}
					}
				}
			} else {
				if (blurText) {
					EditText msgE = (EditText) v;
					if (event.getAction() == MotionEvent.ACTION_DOWN
							|| event.getAction() == MotionEvent.ACTION_SCROLL
							|| event.getAction() == MotionEvent.ACTION_MOVE) {
						if (android.os.Build.VERSION.SDK_INT < 11) {
							msgE.setTextColor(0xff000000);
						} else {
							msgE.setTextColor(0xff000000);
							msgE.setAlpha(1.0f);
						}
						msgE.setShadowLayer(0.0f, 0.0f, 0.0f, 0x00000000);
						RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
						tap.setVisibility(View.GONE);
					} else {
						if (android.os.Build.VERSION.SDK_INT < 11) {
							msgE.setTextColor(0x00000000);
						} else {
							msgE.setTextColor(0xffc3c3c3);
							msgE.setAlpha(0.7f);
						}
						msgE.setShadowLayer(8.0f, 0.0f, 0.0f, 0xff000000);
						RelativeLayout tap = (RelativeLayout) findViewById(R.id.tar_to_read);
						tap.setVisibility(View.VISIBLE);
					}
				}
			}
			return false;
		}
	};

	private Runnable clockTask3 = new Runnable() {

		@Override
		public void run() {
			finalDecrypt();
			progressDecrypting.setVisibility(View.GONE);
		}

	};

	private String fileTextName = "";
	private String defaultTextName = "qlinkText.txt";

	public void downloadText(View v) {
		/*
		 * currentViewId = R.id.namefile; RelativeLayout relSave =
		 * (RelativeLayout) findViewById(R.id.namefile);
		 * relSave.setVisibility(View.VISIBLE);
		 */
		downloadText2(v);
	}

	public void downloadText2(View v) {
		currentViewId = R.layout.read_qlink;
		/*
		 * EditText fileName = (EditText) findViewById(R.id.filename);
		 * fileTextName = fileName.getText().toString(); if
		 * (fileTextName.equals("")) fileTextName = defaultTextName; else
		 * fileTextName = fileTextName + ".txt";
		 * 
		 * RelativeLayout relSave = (RelativeLayout)
		 * findViewById(R.id.namefile); relSave.setVisibility(View.GONE);
		 */

		EditText textDec = (EditText) findViewById(R.id.msg);

		byte[] textBytes = textDec.getText().toString().getBytes();

		InputStream inputStream = new ByteArrayInputStream(textBytes);

		File sdDir = Environment.getExternalStorageDirectory();
		String packageName = this.getPackageName();
		File dir = new File(sdDir, "/Android/data/" + packageName + "/cache/");
		Boolean exist = createDirectory(dir);
		if (!exist)
			return;

		File cacheDir = this.getExternalCacheDir();
		File cacheFile = null;
		try {
			cacheFile = File.createTempFile("qlfl", ".txt", cacheDir);
		} catch (IOException e1) {
		}

		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(cacheFile);
		} catch (FileNotFoundException e) {
		}

		byte buffer[] = new byte[1024];
		int dataSize;
		try {
			while ((dataSize = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, dataSize);
			}
		} catch (IOException e) {
		}
		try {
			outputStream.close();
		} catch (IOException e) {
		}

		openFile(cacheFile);
		/*
		 * Toast.makeText( getApplicationContext(),
		 * resources.getString(R.string.text_download_in) + " " +
		 * cacheFile.getAbsolutePath(), Toast.LENGTH_LONG) .show();
		 */
	}

	private Boolean createDirectory(File dir) {
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				return false;
			}
		}
		return true;
	}

	public void openFile(File f) {
		Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);

		String mimeType = getMimeType(f);
		newIntent.setDataAndType(Uri.fromFile(f), mimeType);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			this.startActivity(newIntent);
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(this, "No handler for this type of file.",
					Toast.LENGTH_LONG).show();
		}
	}

	public String getMimeType(File f) {
		return URLConnection.guessContentTypeFromName(f.getName());
	}

	public void downloadFile(View v) {
		ProgressBar pgbar = (ProgressBar) findViewById(R.id.progressBar1);
		pgbar.setVisibility(View.VISIBLE);

		int indexComa = decFiles.get(0).indexOf(",");
		decFiles.set(0, decFiles.get(0).substring(indexComa + 1));
		byte[] fileBytes = Base64.decode(decFiles.get(0), Base64.DEFAULT);
		InputStream inputStream = new ByteArrayInputStream(fileBytes);
		File cacheDir = Environment.getExternalStorageDirectory();
		File cacheFile = null;
		try {
			cacheFile = new File(cacheDir, namesFiles.getString(0));
		} catch (JSONException e) {
		}
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(cacheFile);
		} catch (FileNotFoundException e) {
		}

		byte buffer[] = new byte[1024];
		int dataSize;
		long loadedSize = 0;
		long totalSize = fileBytes.length;
		try {
			while ((dataSize = inputStream.read(buffer)) != -1) {
				loadedSize += dataSize;
				pgbar.setProgress((int) (loadedSize * 100 / totalSize));
				outputStream.write(buffer, 0, dataSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		openFile(cacheFile);
		Toast.makeText(
				getApplicationContext(),
				resources.getString(R.string.file_download_in) + " "
						+ cacheFile.getAbsolutePath(), Toast.LENGTH_LONG)
				.show();
	}

	private void checkHostConnection() {
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
			socket.connect(new InetSocketAddress(host, port), 5000);
			socket.close();
			isHostConnected = true;
			return;
		} catch (Exception ex) {
			isHostConnected = false;
		}
	}

	private void checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		isInternetConnected = activeNetwork != null
				&& activeNetwork.isConnectedOrConnecting();
	}

	public void replyQlink(View v) {
		Intent intent = new Intent(this, QlinkActivity.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("welcomed", true);
		intent.putExtras(bundle);

		startActivity(intent);
		finish();
	}

	public void forwardQlink(View v) {
		Intent intent = new Intent(this, QlinkActivity.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("welcomed", true);
		bundle.putString("lql", lastQl);
		intent.putExtras(bundle);

		startActivity(intent);
		finish();
	}

	private View helpView;

	public void helpQlink(View v) {
		LayoutInflater inflater = getLayoutInflater();
		helpView = inflater.inflate(R.layout.help_forward, null);
		currentViewId = R.layout.help_forward;
		addContentView(helpView, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT));
	}

	public void closeHelpLink(View v) {
		ViewGroup vg = (ViewGroup) (helpView.getParent());
		vg.removeView(helpView);
		currentViewId = R.layout.read_qlink;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public void onBackPressed() {
		if (currentViewId != null) {
			if (currentViewId == R.id.namefile) {
				RelativeLayout relSave = (RelativeLayout) findViewById(R.id.namefile);
				relSave.setVisibility(View.GONE);
			} else if (currentViewId == R.layout.help_forward) {

			} else {
				super.onBackPressed();
				finish();
			}
		} else {
			super.onBackPressed();
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		System.gc();
	}

	@Override
	protected void onStop() {
		super.onStop();
		System.gc();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.gc();
		AppUtil.trimCache(this);
	}

	private Menu appMenu = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		appMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bar_read, menu);

		MenuItem item = appMenu.findItem(R.id.btSave);
		item.setVisible(false);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.btSave:
			downloadText(null);
			break;
		default:
			break;
		}

		return true;
	}

	public static int convertPixelsToDp(float px, Context context) {
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		int dp = (int) (px / (metrics.densityDpi / 160.0f));
		return dp;
	}

	public static int getDp(float px, Context context) {
		return (int) px;
	}
}
