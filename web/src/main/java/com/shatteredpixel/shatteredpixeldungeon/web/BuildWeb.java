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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BuildWeb {

	private static final String WEBAPP_FOLDER = "webapp";
	private static final String PWA_META_MARKER = "<!-- shattered-pixel-dungeon-pwa-meta -->";
	private static final String PWA_SCRIPT_MARKER = "<!-- shattered-pixel-dungeon-pwa-script -->";

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
				.setWebappFolderName(WEBAPP_FOLDER)
				.setStartJettyAfterBuild(false);

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

		File webappDir = new File(outputDir, WEBAPP_FOLDER);
		enhanceWebApp(webappDir, desktopAssets);

		if ("run".equals(action)) {
			backend.startJetty(port, webappDir.getAbsolutePath());
		}
	}

	private static void enhanceWebApp(File webappDir, String desktopAssets) {
		try {
			Files.createDirectories(webappDir.toPath());
			writePwaIcons(webappDir, desktopAssets);
			writeManifest(webappDir);
			writeServiceWorker(webappDir);
			injectPwaTags(new File(webappDir, "index.html"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to prepare PWA web app output", e);
		}
	}

	private static void writePwaIcons(File webappDir, String desktopAssets) throws IOException {
		File iconSource = findIconSource(desktopAssets);
		if (iconSource == null) {
			throw new IOException("No source icon found for PWA output");
		}

		File iconsDir = new File(webappDir, "icons");
		Files.createDirectories(iconsDir.toPath());
		writeScaledPng(iconSource, new File(iconsDir, "icon-192.png"), 192);
		writeScaledPng(iconSource, new File(iconsDir, "icon-512.png"), 512);
	}

	private static File findIconSource(String desktopAssets) {
		String[] candidates = {
				"../ios/assets/Assets.xcassets/AppIcon.appiconset/Icon-1024.png",
				"ios/assets/Assets.xcassets/AppIcon.appiconset/Icon-1024.png",
				desktopAssets + "/icons/icon_256.png"
		};

		for (String candidate : candidates) {
			File file = new File(candidate);
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}

	private static void writeScaledPng(File source, File target, int size) throws IOException {
		BufferedImage src = ImageIO.read(source);
		if (src == null) {
			throw new IOException("Unsupported icon format: " + source);
		}

		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = out.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(src, 0, 0, size, size, null);
		} finally {
			graphics.dispose();
		}
		ImageIO.write(out, "png", target);
	}

	private static void writeManifest(File webappDir) throws IOException {
		String manifest = String.join("\n",
				"{",
				"  \"name\": \"Shattered Pixel Dungeon\",",
				"  \"short_name\": \"Shattered PD\",",
				"  \"description\": \"A roguelike dungeon crawler for the web.\",",
				"  \"start_url\": \"./\",",
				"  \"scope\": \"./\",",
				"  \"display\": \"standalone\",",
				"  \"display_override\": [\"fullscreen\", \"standalone\"],",
				"  \"orientation\": \"any\",",
				"  \"background_color\": \"#000000\",",
				"  \"theme_color\": \"#000000\",",
				"  \"icons\": [",
				"    {",
				"      \"src\": \"icons/icon-192.png\",",
				"      \"sizes\": \"192x192\",",
				"      \"type\": \"image/png\",",
				"      \"purpose\": \"any maskable\"",
				"    },",
				"    {",
				"      \"src\": \"icons/icon-512.png\",",
				"      \"sizes\": \"512x512\",",
				"      \"type\": \"image/png\",",
				"      \"purpose\": \"any maskable\"",
				"    }",
				"  ]",
				"}",
				"");
		Files.write(new File(webappDir, "manifest.webmanifest").toPath(), manifest.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeServiceWorker(File webappDir) throws IOException {
		String cacheName = "shattered-pixel-dungeon-" + WebBuildConfig.VERSION_CODE + "-" + System.currentTimeMillis();
		String serviceWorker = String.join("\n",
				"const CACHE_PREFIX = 'shattered-pixel-dungeon-';",
				"const CACHE_NAME = '" + cacheName + "';",
				"const APP_SHELL = [",
				"  './',",
				"  './index.html',",
				"  './manifest.webmanifest',",
				"  './shattered-pixel-dungeon.js',",
				"  './icons/icon-192.png',",
				"  './icons/icon-512.png'",
				"];",
				"",
				"self.addEventListener('install', event => {",
				"  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL)));",
				"  self.skipWaiting();",
				"});",
				"",
				"self.addEventListener('activate', event => {",
				"  event.waitUntil(",
				"    caches.keys().then(keys => Promise.all(",
				"      keys.filter(key => key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME)",
				"        .map(key => caches.delete(key))",
				"    ))",
				"  );",
				"  self.clients.claim();",
				"});",
				"",
				"self.addEventListener('fetch', event => {",
				"  const request = event.request;",
				"  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) {",
				"    return;",
				"  }",
				"",
				"  event.respondWith(",
				"    caches.match(request).then(cached => {",
				"      const fetchAndCache = fetch(request).then(response => {",
				"        if (response && response.ok) {",
				"          const copy = response.clone();",
				"          caches.open(CACHE_NAME).then(cache => cache.put(request, copy));",
				"        }",
				"        return response;",
				"      });",
				"      return cached || fetchAndCache;",
				"    })",
				"  );",
				"});",
				"");
		Files.write(new File(webappDir, "service-worker.js").toPath(), serviceWorker.getBytes(StandardCharsets.UTF_8));
	}

	private static void injectPwaTags(File index) throws IOException {
		String html = new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8);
		if (!html.contains(PWA_META_MARKER)) {
			String meta = String.join("\n",
					"        " + PWA_META_MARKER,
					"        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no\">",
					"        <meta name=\"theme-color\" content=\"#000000\">",
					"        <meta name=\"mobile-web-app-capable\" content=\"yes\">",
					"        <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">",
					"        <meta name=\"apple-mobile-web-app-title\" content=\"Shattered PD\">",
					"        <meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black-translucent\">",
					"        <link rel=\"manifest\" href=\"manifest.webmanifest\">",
					"        <link rel=\"icon\" type=\"image/png\" sizes=\"192x192\" href=\"icons/icon-192.png\">",
					"        <link rel=\"apple-touch-icon\" href=\"icons/icon-192.png\">",
					"        <style>",
					"            html, body {",
					"                width: 100%;",
					"                height: 100%;",
					"                min-width: 100%;",
					"                min-height: 100%;",
					"                margin: 0;",
					"                padding: 0;",
					"                overflow: hidden;",
					"                overscroll-behavior: none;",
					"                position: fixed;",
					"                inset: 0;",
					"                touch-action: none;",
					"                -webkit-user-select: none;",
					"                user-select: none;",
					"            }",
					"            body > div {",
					"                width: 100vw;",
					"                height: 100vh;",
					"                height: 100dvh;",
					"                overflow: hidden;",
					"                position: fixed;",
					"                inset: 0;",
					"            }",
					"            #canvas {",
					"                display: block;",
					"                width: 100vw;",
					"                height: 100vh;",
					"                height: 100dvh;",
					"                max-width: none;",
					"                max-height: none;",
					"                touch-action: none;",
					"                outline: none;",
					"            }",
					"        </style>");
			html = html.replace("    </head>", meta + "\n    </head>");
		}

		if (!html.contains(PWA_SCRIPT_MARKER)) {
			String script = String.join("\n",
					"        " + PWA_SCRIPT_MARKER,
					"        <script>",
					"            (function () {",
					"                var resizeFrame = 0;",
					"                var lastWidth = 0;",
					"                var lastHeight = 0;",
					"",
					"                function viewportSize() {",
					"                    var doc = document.documentElement;",
					"                    var viewport = window.visualViewport;",
					"                    var width = viewport && viewport.width ? viewport.width : window.innerWidth || doc.clientWidth || screen.width;",
					"                    var height = viewport && viewport.height ? viewport.height : window.innerHeight || doc.clientHeight || screen.height;",
					"                    return {",
					"                        width: Math.max(1, Math.round(width)),",
					"                        height: Math.max(1, Math.round(height))",
					"                    };",
					"                }",
					"",
					"                function resizeCanvas() {",
					"                    resizeFrame = 0;",
					"                    var canvas = document.getElementById('canvas');",
					"                    if (!canvas) {",
					"                        return;",
					"                    }",
					"",
					"                    var size = viewportSize();",
					"                    if (canvas.width !== size.width) {",
					"                        canvas.width = size.width;",
					"                    }",
					"                    if (canvas.height !== size.height) {",
					"                        canvas.height = size.height;",
					"                    }",
					"",
					"                    canvas.style.width = size.width + 'px';",
					"                    canvas.style.height = size.height + 'px';",
					"",
					"                    if (canvas.parentElement) {",
					"                        canvas.parentElement.style.width = size.width + 'px';",
					"                        canvas.parentElement.style.height = size.height + 'px';",
					"                    }",
					"",
					"                    lastWidth = size.width;",
					"                    lastHeight = size.height;",
					"                }",
					"",
					"                function scheduleCanvasResize() {",
					"                    if (!resizeFrame) {",
					"                        resizeFrame = window.requestAnimationFrame(resizeCanvas);",
					"                    }",
					"                }",
					"",
					"                function settleCanvasResize() {",
					"                    scheduleCanvasResize();",
					"                    window.setTimeout(scheduleCanvasResize, 60);",
					"                    window.setTimeout(scheduleCanvasResize, 250);",
					"                    window.setTimeout(scheduleCanvasResize, 600);",
					"                }",
					"",
					"                function unlockAudio() {",
					"                    var howler = window.Howler;",
					"                    if (!howler) {",
					"                        return;",
					"                    }",
					"                    howler.autoUnlock = true;",
					"                    if (howler.ctx && howler.ctx.state !== 'running') {",
					"                        var resume = howler.ctx.resume();",
					"                        if (resume && typeof resume.catch === 'function') {",
					"                            resume.catch(function () {});",
					"                        }",
					"                    }",
					"                    if (howler.ctx && !howler._audioUnlocked) {",
					"                        try {",
					"                            var buffer = howler._scratchBuffer || howler.ctx.createBuffer(1, 1, 22050);",
					"                            var source = howler.ctx.createBufferSource();",
					"                            howler._scratchBuffer = buffer;",
					"                            source.buffer = buffer;",
					"                            source.connect(howler.ctx.destination);",
					"                            source.onended = function () {",
					"                                source.disconnect(0);",
					"                                howler._audioUnlocked = true;",
					"                            };",
					"                            if (typeof source.start === 'function') {",
					"                                source.start(0);",
					"                            } else if (typeof source.noteOn === 'function') {",
					"                                source.noteOn(0);",
					"                            }",
					"                        } catch (e) {",
					"                            // Some browsers only permit resume(), which is attempted above.",
					"                        }",
					"                    }",
					"                    if (typeof howler._autoResume === 'function') {",
					"                        howler._autoResume();",
					"                    }",
					"                }",
					"",
					"                function wireAudioUnlock() {",
					"                    ['pointerdown', 'pointerup', 'touchstart', 'touchend', 'mousedown', 'click', 'keydown'].forEach(function (eventName) {",
					"                        window.addEventListener(eventName, unlockAudio, { capture: true, passive: true });",
					"                    });",
					"                    window.addEventListener('pageshow', unlockAudio);",
					"                    document.addEventListener('visibilitychange', unlockAudio);",
					"                }",
					"",
					"                window.shatteredPixelDungeonResizeCanvas = settleCanvasResize;",
					"                window.addEventListener('resize', settleCanvasResize);",
					"                window.addEventListener('orientationchange', settleCanvasResize);",
					"                if (window.visualViewport) {",
					"                    window.visualViewport.addEventListener('resize', settleCanvasResize);",
					"                    window.visualViewport.addEventListener('scroll', settleCanvasResize);",
					"                }",
					"",
					"                settleCanvasResize();",
					"                wireAudioUnlock();",
					"                window.setInterval(function () {",
					"                    var size = viewportSize();",
					"                    if (size.width !== lastWidth || size.height !== lastHeight) {",
					"                        settleCanvasResize();",
					"                    }",
					"                }, 1000);",
					"            }());",
					"",
					"            if ('serviceWorker' in navigator) {",
					"                window.addEventListener('load', function () {",
					"                    navigator.serviceWorker.register('service-worker.js');",
					"                });",
					"            }",
					"        </script>");
			html = html.replace("    </body>", script + "\n    </body>");
		}

		Files.write(index.toPath(), html.getBytes(StandardCharsets.UTF_8));
	}
}
