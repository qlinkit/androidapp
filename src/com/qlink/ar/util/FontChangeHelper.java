package com.qlink.ar.util;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class FontChangeHelper {
	private Typeface typeface;

	public FontChangeHelper(Typeface typeface) {
		this.typeface = typeface;
	}

	public FontChangeHelper(AssetManager assets, String assetsFontFileName) {
		typeface = Typeface.createFromAsset(assets, assetsFontFileName);
	}

	public void replaceFonts(ViewGroup viewTree) {
		View child;
		for (int i = 0; i < viewTree.getChildCount(); ++i) {
			child = viewTree.getChildAt(i);
			if (child instanceof ViewGroup) {
				replaceFonts((ViewGroup) child);
			} else if (child instanceof EditText) {
				((EditText) child).setTypeface(typeface);
			}

		}
	}
}