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

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;

public class BuildWeb {

	public static void main(String[] args) {
		String action = args.length > 0 ? args[0] : "build";
		File outputDir = new File(args.length > 1 ? args[1] : "build/dist");
		String coreAssets = args.length > 2 ? args[2] : "../core/src/main/assets";
		String desktopAssets = args.length > 3 ? args[3] : "../desktop/src/main/assets";
		int port = Integer.parseInt(System.getProperty("web.port", args.length > 4 ? args[4] : "8080"));

		WebBackend backend = new WebBackend()
				.setHtmlTitle("Shattered Pixel Dungeon")
				.setHtmlWidth(800)
				.setHtmlHeight(480)
				.setJettyPort(port)
				.setStartJettyAfterBuild("run".equals(action));

		new TeaCompiler(backend)
				.addAssets(new AssetFileHandle(coreAssets))
				.addAssets(new AssetFileHandle(desktopAssets))
				.setMainClass(WebLauncher.class.getName())
				.setOutputName("shattered-pixel-dungeon")
				.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
				.setObfuscated(false)
				.setDebugInformationGenerated(true)
				.addReflectionClass("com.shatteredpixel.shatteredpixeldungeon**")
				.addReflectionClass("com.watabou**")
				.build(outputDir);
	}
}
