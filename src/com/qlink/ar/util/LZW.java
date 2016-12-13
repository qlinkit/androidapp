package com.qlink.ar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LZW {

	public static String compress(String uncompressed) {
		// Build the dictionary.
		int dictSize = 30000;
		Map<String, Integer> dictionary = new HashMap<String, Integer>();
		for (int i = 0; i < 30000; i++)
			dictionary.put("" + (char) i, i);

		String w = "";
		List<Integer> result = new ArrayList<Integer>();
		for (char c : uncompressed.toCharArray()) {
			String wc = w + c;
			if (dictionary.containsKey(wc))
				w = wc;
			else {
				result.add(dictionary.get(w));
				dictionary.put(wc, dictSize++);
				w = "" + c;
			}
		}

		if (!w.equals(""))
			result.add(dictionary.get(w));

		String resSt = "";
		for (Integer integer : result) {
			char[] resArr = Character.toChars(integer);
			for (int i = 0; i < resArr.length; i++) {
				resSt += resArr[i];
			}
		}
		return resSt;
	}

	/** Decompress a list of output ks to a string. */
	public static String decompress(String compressedStr) {

		List<Integer> compressed = new ArrayList<Integer>();
		for (char chr : compressedStr.toCharArray()) {
			compressed.add((int) chr);
		}

		int dictSize = 30000;
		Map<Integer, String> dictionary = new HashMap<Integer, String>();
		for (int i = 0; i < 30000; i++)
			dictionary.put(i, "" + (char) i);

		String w = "" + (char) (int) compressed.remove(0);
		StringBuffer result = new StringBuffer(w);
		for (int k : compressed) {
			String entry;
			if (dictionary.containsKey(k))
				entry = dictionary.get(k);
			else if (k == dictSize)
				entry = w + w.charAt(0);
			else
				throw new IllegalArgumentException("Bad compressed k: " + k);

			result.append(entry);

			dictionary.put(dictSize++, w + entry.charAt(0));

			w = entry;
		}
		return result.toString();
	}
}
