package com.qlink.ar.util;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class EscapeUtil {

	public static String escapeHtmlEntities(String html) {
		html = html.replaceAll("<", "&lt;");
		html = html.replaceAll(">", "&gt;");
		html = html.replaceAll("&", "&amp;");
		html = html.replaceAll("¢", "&cent;");
		html = html.replaceAll("£", "&pound;");
		html = html.replaceAll("¥", "&yen;");
		html = html.replaceAll("€", "&euro;");
		html = html.replaceAll("§", "&sect;");
		html = html.replaceAll("©", "&copy;");
		html = html.replaceAll("®", "&reg;");
		html = html.replaceAll("™", "&trade;");
		html = html.replaceAll("\"", "&quot;");
		html = html.replaceAll("'", "&apos;");

		return html;
	}

	public static String unEscapeHtmlEntities(String html) {
		html = html.replaceAll("&amp;", "&");
		html = html.replaceAll("&lt;", "<");
		html = html.replaceAll("&gt;", ">");
		html = html.replaceAll("&cent;", "¢");
		html = html.replaceAll("&pound;", "£");
		html = html.replaceAll("&yen;", "¥");
		html = html.replaceAll("&euro;", "€");
		html = html.replaceAll("&sect;", "§");
		html = html.replaceAll("&copy;", "©");
		html = html.replaceAll("&reg;", "®");
		html = html.replaceAll("&trade;", "™");
		html = html.replaceAll("&quot;", "\"");
		html = html.replaceAll("&apos;", "'");

		return html;
	}

}
