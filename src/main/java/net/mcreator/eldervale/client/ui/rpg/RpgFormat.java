package net.mcreator.eldervale.client.ui.rpg;

import java.util.Locale;

/** Number formatting for stats (same rules as AttributesScreen; TODO: AttributesScreen can use this one when restyled). */
final class RpgFormat {

	private RpgFormat() {
	}

	/** dec = max decimals, minDec = decimals always kept. */
	static String num(double v, int dec, int minDec) {
		String s = String.format(Locale.ROOT, "%." + dec + "f", v);
		int dot = s.indexOf('.');
		if (dot >= 0) {
			int min = dot + 1 + minDec;
			int end = s.length();
			while (end > min && s.charAt(end - 1) == '0')
				end--;
			s = s.substring(0, end);
			if (s.endsWith("."))
				s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	static String signed(double v, int dec) {
		return (v < 0 ? "-" : "+") + num(Math.abs(v), dec, 0);
	}
}
