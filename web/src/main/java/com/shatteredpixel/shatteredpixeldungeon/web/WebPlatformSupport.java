/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.web;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.watabou.utils.PlatformSupport;
import org.teavm.jso.JSBody;

import java.util.ArrayList;
import java.util.HashMap;

class WebPlatformSupport extends PlatformSupport {

	private static FreeTypeFontGenerator basicFontGenerator;
	private static FreeTypeFontGenerator asianFontGenerator;

	@Override
	public void updateDisplaySize() {
		// The browser backend tracks the canvas size directly.
	}

	@Override
	public boolean supportsFullScreen() {
		return false;
	}

	@Override
	public boolean isDesktopDevice() {
		return !isMobileBrowser();
	}

	@Override
	public boolean isMobileDevice() {
		return isMobileBrowser();
	}

	@Override
	public boolean hasHardKeyboard() {
		return !isMobileBrowser();
	}

	@Override
	public void updateSystemUI() {
		// Browser chrome and canvas sizing are controlled by the generated web shell.
	}

	@Override
	public boolean connectedToUnmeteredNetwork() {
		return true;
	}

	@Override
	public boolean supportsVibration() {
		return Gdx.input.isPeripheralAvailable(Input.Peripheral.Vibrator);
	}

	@Override
	public void setupFontGenerators(int pageSize, boolean systemfont) {
		if (fonts != null && this.pageSize == pageSize && this.systemfont == systemfont) {
			return;
		}
		this.pageSize = pageSize;
		this.systemfont = systemfont;

		resetGenerators(false);
		fonts = new HashMap<>();

		if (systemfont) {
			basicFontGenerator = asianFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/droid_sans.ttf"));
		} else {
			basicFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel_font.ttf"));
			asianFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/droid_sans.ttf"));
		}

		fonts.put(basicFontGenerator, new HashMap<>());
		fonts.put(asianFontGenerator, new HashMap<>());

		packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 1, false);
	}

	@Override
	protected FreeTypeFontGenerator getGeneratorForString(String input) {
		if (containsAsianChar(input)) {
			return asianFontGenerator;
		} else {
			return basicFontGenerator;
		}
	}

	@Override
	public String[] splitforTextBlock(String text, boolean multiline) {
		ArrayList<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
				flushToken(result, current);
				result.add("**");
				i++;
				continue;
			}

			char c = text.charAt(i);
			if (c == '\n' || c == '_' || (multiline && c == ' ') || isAsianChar(c)) {
				flushToken(result, current);
				result.add(String.valueOf(c));
			} else {
				current.append(c);
			}
		}

		flushToken(result, current);
		return result.toArray(new String[0]);
	}

	private static boolean containsAsianChar(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (isAsianChar(input.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAsianChar(char c) {
		return (c >= 0xAC00 && c <= 0xD7AF)
				|| (c >= 0x4E00 && c <= 0x9FFF)
				|| (c >= 0x3000 && c <= 0x303F)
				|| (c >= 0xFF00 && c <= 0xFFEF)
				|| (c >= 0x3040 && c <= 0x309F)
				|| (c >= 0x30A0 && c <= 0x30FF);
	}

	private static void flushToken(ArrayList<String> result, StringBuilder current) {
		if (current.length() > 0) {
			result.add(current.toString());
			current.setLength(0);
		}
	}

	@JSBody(script =
			"var nav = window.navigator || {};\n" +
			"var ua = nav.userAgent || '';\n" +
			"var platform = nav.platform || '';\n" +
			"var maxTouch = nav.maxTouchPoints || nav.msMaxTouchPoints || 0;\n" +
			"var mobileUA = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua);\n" +
			"var iPadOS = platform === 'MacIntel' && maxTouch > 1;\n" +
			"var coarse = !!(window.matchMedia && window.matchMedia('(any-pointer: coarse)').matches);\n" +
			"var noHover = !!(window.matchMedia && window.matchMedia('(hover: none)').matches);\n" +
			"var shortSide = Math.min(window.innerWidth || screen.width || 0, window.innerHeight || screen.height || 0);\n" +
			"return mobileUA || iPadOS || ((maxTouch > 0 || coarse) && noHover && shortSide <= 900);")
	private static native boolean isMobileBrowser();
}
