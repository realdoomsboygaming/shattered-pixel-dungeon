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

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.news.NewsImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.UpdateImpl;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.watabou.noosa.Game;

public class WebLauncher {

	public static void main(String[] args) {
		Game.version = WebBuildConfig.VERSION_NAME;
		Game.versionCode = WebBuildConfig.VERSION_CODE;

		if (UpdateImpl.supportsUpdates()) {
			Updates.service = UpdateImpl.getUpdateService();
		}
		if (NewsImpl.supportsNews()) {
			News.service = NewsImpl.getNewsService();
		}

		WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
		config.width = 0;
		config.height = 0;
		config.showDownloadLogs = true;
		config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");

		new WebApplication(new WebGame(), config);
	}
}
