package com.qlink.ar;

import java.util.Locale;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class UserSettingActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// add the xml resource
		addPreferencesFromResource(R.xml.user_settings);

		ListPreference langSelect = (ListPreference) findPreference("languageLocale");
		langSelect
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference arg0,
							Object arg1) {
						Locale l = new Locale(arg1.toString());

						Configuration c = new Configuration(getResources()
								.getConfiguration());
						c.locale = l;
						getResources().updateConfiguration(c,
								getResources().getDisplayMetrics());

						Locale.setDefault(l);
						return true;
					}
				});

		CharSequence[] langs = { "Español", "English" };
		CharSequence[] langsVals = { "es", "en" };
		langSelect.setEntries(langs);
		langSelect.setEntryValues(langsVals);
		langSelect.setDefaultValue("en");

		Preference button = (Preference) findPreference("buttonAbout");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				openQlinkWeb();
				return true;
			}
		});

		button = (Preference) findPreference("buttonPrivacy");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				openPrivacy();
				return true;
			}
		});
	}

	private void openQlinkWeb() {
		String locale = Locale.getDefault().getLanguage();
		String url = "https://qlink.it/main?lang=" + locale;
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	private void openPrivacy() {
		String url = "https://qlink.it/corp/docs/privacy-policy.pdf";
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}
}
