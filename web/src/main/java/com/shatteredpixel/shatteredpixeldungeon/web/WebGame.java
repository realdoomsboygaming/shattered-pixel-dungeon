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

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

class WebGame extends ShatteredPixelDungeon {

	private int pendingResizeWidth;
	private int pendingResizeHeight;
	private boolean pendingResize;

	WebGame() {
		super(new WebPlatformSupport());
	}

	@Override
	public void create() {
		WebPlatformSupport.syncCanvasSize();
		SPDSettings.set(Gdx.app.getPreferences(SPDSettings.DEFAULT_PREFS_FILE));
		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
		// TeaVM's Deflater can fail in the browser; Bundle.read still accepts old gzip saves.
		Bundle.setCompressByDefault(false);
		super.create();
	}

	@Override
	public void resize( int width, int height ) {
		if (width == 0 || height == 0) {
			return;
		}

		if (scene instanceof GameScene && Actor.processing()) {
			pendingResizeWidth = width;
			pendingResizeHeight = height;
			pendingResize = true;
			return;
		}

		super.resize(width, height);
	}

	@Override
	public void render() {
		WebPlatformSupport.syncCanvasSize();

		if (pendingResize && !(scene instanceof GameScene && Actor.processing())) {
			pendingResize = false;
			super.resize(pendingResizeWidth, pendingResizeHeight);
		}

		super.render();
	}

	@Override
	protected void step() {
		if (requestedReset && scene instanceof GameScene && Actor.processing() && sceneClass != InterlevelScene.class) {
			update();
			return;
		}

		super.step();
	}
}
