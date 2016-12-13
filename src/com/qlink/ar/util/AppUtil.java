package com.qlink.ar.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Context;
/*
 * Author: Ricardo Bianchi
 */
import android.content.pm.PackageManager;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class AppUtil {

	public static void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			File dir = context.getExternalCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}

	public static void clearCacheStorage(Context context) {
		PackageManager pm = context.getPackageManager();
		Method[] methods = pm.getClass().getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().equals("freeStorageAndNotify")) {
				try {
					long desiredFreeStorage = 64 * 1024 * 1024 * 1024;
					m.invoke(pm, desiredFreeStorage, null);
				} catch (Exception e) {
				}
				break;
			}
		}
	}

	public static ArrayList<String> getDNSServers() {
		Class<?> SystemProperties = null;
		try {
			SystemProperties = Class.forName("android.os.SystemProperties");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Method method = null;
		try {
			method = SystemProperties.getMethod("get",
					new Class[] { String.class });
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<String> servers = new ArrayList<String>();
		for (String name : new String[] { "net.dns1", "net.dns2", "net.dns3",
				"net.dns4", }) {
			String value = null;
			try {
				value = (String) method.invoke(null, name);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (value != null && !"".equals(value) && !servers.contains(value))
				servers.add(value);
		}
		return servers;
	}
}
