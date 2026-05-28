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
	private static final String START_BUTTON_ID = "shattered-pixel-dungeon-start";

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
				"  './scripts/howler.js',",
				"  './icons/icon-192.png',",
				"  './icons/icon-512.png'",
				"];",
				"const APP_SHELL_URLS = APP_SHELL.map(path => new URL(path, self.location.href).href);",
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
				"  if (request.mode === 'navigate' || request.destination === 'document') {",
				"    event.respondWith(",
				"      fetch(request).then(response => {",
				"        if (response && response.ok) {",
				"          const copy = response.clone();",
				"          caches.open(CACHE_NAME).then(cache => cache.put('./index.html', copy));",
				"        }",
				"        return response;",
				"      }).catch(() => caches.match(request).then(cached => cached || caches.match('./index.html')))",
				"    );",
				"    return;",
				"  }",
				"",
				"  const fetchAndCache = fetch(request).then(response => {",
				"        if (response && response.ok) {",
				"          const copy = response.clone();",
				"          caches.open(CACHE_NAME).then(cache => cache.put(request, copy));",
				"        }",
				"        return response;",
				"      });",
				"",
				"  if (APP_SHELL_URLS.includes(request.url)) {",
				"    event.respondWith(fetchAndCache.catch(() => caches.match(request)));",
				"    return;",
				"  }",
				"",
				"  event.respondWith(",
				"    caches.match(request).then(cached => {",
				"      if (cached) {",
				"        fetchAndCache.catch(() => {});",
				"        return cached;",
				"      }",
				"      return fetchAndCache;",
				"    })",
				"  );",
				"});",
				"");
		Files.write(new File(webappDir, "service-worker.js").toPath(), serviceWorker.getBytes(StandardCharsets.UTF_8));
	}

	private static void injectPwaTags(File index) throws IOException {
		String html = new String(Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8);
		html = html.replace("window.addEventListener(\"load\", start);", "window.shatteredPixelDungeonStartGame = start;");
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
					"                width: 100vw;",
					"                width: var(--spd-viewport-width, 100vw);",
					"                height: 100vh;",
					"                height: var(--spd-viewport-height, 100dvh);",
					"                min-width: 0;",
					"                min-height: 0;",
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
					"            body {",
					"                display: block !important;",
					"                background: #000;",
					"            }",
					"            body > div {",
					"                width: 100vw;",
					"                width: var(--spd-viewport-width, 100vw);",
					"                height: 100vh;",
					"                height: var(--spd-viewport-height, 100dvh);",
					"                overflow: hidden;",
					"                position: fixed;",
					"                inset: 0;",
					"            }",
					"            #canvas {",
					"                display: block;",
					"                width: 100%;",
					"                height: 100%;",
					"                max-width: none;",
					"                max-height: none;",
					"                touch-action: none;",
					"                outline: none;",
					"            }",
					"            #" + START_BUTTON_ID + " {",
					"                display: none;",
					"                position: fixed;",
					"                inset: 0;",
					"                z-index: 10;",
					"                width: 100vw;",
					"                width: var(--spd-viewport-width, 100vw);",
					"                height: 100vh;",
					"                height: var(--spd-viewport-height, 100dvh);",
					"                border: 0;",
					"                background: #000;",
					"                color: #f2f2f2;",
					"                font: 700 20px system-ui, sans-serif;",
					"                align-items: center;",
					"                justify-content: center;",
					"                touch-action: manipulation;",
					"            }",
					"            #" + START_BUTTON_ID + "[data-visible=\"true\"] {",
					"                display: flex;",
					"            }",
					"        </style>",
					"        <script>",
					"            (function () {",
					"                var contexts = [];",
					"                var activationSeen = false;",
					"                var retryTimer = 0;",
					"                var eventNames = ['click', 'contextmenu', 'auxclick', 'dblclick', 'mousedown', 'mouseup', 'pointerdown', 'pointerup', 'touchstart', 'touchend', 'keydown', 'keyup'];",
					"",
					"                function patchAudioContext(name) {",
					"                    var Context = window[name];",
					"                    if (!Context || Context.__shatteredPixelDungeonPatched) {",
					"                        return;",
					"                    }",
					"                    var PatchedContext = new Proxy(Context, {",
					"                        construct: function (target, args) {",
					"                            var context = Reflect.construct(target, args);",
					"                            contexts.push(context);",
					"                            if (activationSeen) {",
					"                                window.setTimeout(resumeAudio, 0);",
					"                            }",
					"                            return context;",
					"                        }",
					"                    });",
					"                    PatchedContext.__shatteredPixelDungeonPatched = true;",
					"                    window[name] = PatchedContext;",
					"                }",
					"",
					"                function startSilentBuffer(context) {",
					"                    try {",
					"                        var source = context.createBufferSource();",
					"                        source.buffer = context.createBuffer(1, 1, 22050);",
					"                        source.connect(context.destination);",
					"                        source.onended = function () {",
					"                            source.disconnect(0);",
					"                        };",
					"                        source.start(0);",
					"                        context.__shatteredPixelDungeonUnlocked = true;",
					"                    } catch (e) {",
					"                    }",
					"                }",
					"",
					"                function resumeContext(context) {",
					"                    if (!context) {",
					"                        return;",
					"                    }",
					"                    if (context.state !== 'running' && typeof context.resume === 'function') {",
					"                        var resume = context.resume();",
					"                        if (resume && typeof resume.catch === 'function') {",
					"                            resume.catch(function () {});",
					"                        }",
					"                    }",
					"                    if (!context.__shatteredPixelDungeonUnlocked) {",
					"                        startSilentBuffer(context);",
					"                    }",
					"                }",
					"",
					"                function resumeAudio() {",
					"                    activationSeen = true;",
					"                    contexts.forEach(resumeContext);",
					"                    if (window.Howler) {",
					"                        window.Howler.autoUnlock = true;",
					"                        resumeContext(window.Howler.ctx);",
					"                        if (window.Howler.ctx && window.Howler.ctx.state !== 'running' && typeof window.Howler._autoResume === 'function') {",
					"                            window.Howler._autoResume();",
					"                        }",
					"                    }",
					"                    if (!retryTimer) {",
					"                        retryTimer = window.setInterval(function () {",
					"                            var running = window.Howler && window.Howler.ctx && window.Howler.ctx.state === 'running';",
					"                            contexts.forEach(resumeContext);",
					"                            if (window.Howler) {",
					"                                resumeContext(window.Howler.ctx);",
					"                            }",
					"                            if (running) {",
					"                                window.clearInterval(retryTimer);",
					"                                retryTimer = 0;",
					"                            }",
					"                        }, 250);",
					"                        window.setTimeout(function () {",
					"                            if (retryTimer) {",
					"                                window.clearInterval(retryTimer);",
					"                                retryTimer = 0;",
					"                            }",
					"                        }, 5000);",
					"                    }",
					"                }",
					"",
					"                patchAudioContext('AudioContext');",
					"                patchAudioContext('webkitAudioContext');",
					"                window.shatteredPixelDungeonResumeAudio = resumeAudio;",
					"                eventNames.forEach(function (eventName) {",
					"                    document.addEventListener(eventName, resumeAudio, { capture: true, passive: true });",
					"                });",
					"                document.addEventListener('visibilitychange', function () {",
					"                    if (document.visibilityState === 'visible') {",
					"                        resumeAudio();",
					"                    }",
					"                });",
					"            }());",
					"        </script>",
					"        <script src=\"scripts/howler.js\"></script>");
			html = html.replace("    </head>", meta + "\n    </head>");
		}

		if (!html.contains("id=\"" + START_BUTTON_ID + "\"")) {
			html = html.replace("        <script>\n            async function start()", "        <button id=\"" + START_BUTTON_ID + "\" type=\"button\" aria-label=\"Start game\">Start</button>\n        <script>\n            async function start()");
		}

		if (!html.contains(PWA_SCRIPT_MARKER)) {
			String script = String.join("\n",
					"        " + PWA_SCRIPT_MARKER,
					"        <script>",
					"            (function () {",
					"                function viewportSize() {",
					"                    var doc = document.documentElement;",
					"                    var viewport = window.visualViewport;",
					"                    var width = viewport && viewport.width ? viewport.width : window.innerWidth || doc.clientWidth || screen.width;",
					"                    var height = viewport && viewport.height ? viewport.height : window.innerHeight || doc.clientHeight || screen.height;",
					"                    var nav = navigator || {};",
					"                    var ua = nav.userAgent || '';",
					"                    var platform = nav.platform || '';",
					"                    var maxTouch = nav.maxTouchPoints || nav.msMaxTouchPoints || 0;",
					"                    var mobileLike = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua) || (platform === 'MacIntel' && maxTouch > 1);",
					"                    var type = screen.orientation && screen.orientation.type ? screen.orientation.type : '';",
					"                    var angle = typeof window.orientation === 'number' ? window.orientation : (screen.orientation && typeof screen.orientation.angle === 'number' ? screen.orientation.angle : 0);",
					"                    var normalizedAngle = Math.abs(angle) % 180;",
					"                    var physicalLandscape = mobileLike && (type.indexOf('landscape') >= 0 || normalizedAngle === 90);",
					"                    var physicalPortrait = mobileLike && (type.indexOf('portrait') >= 0 || normalizedAngle === 0);",
					"                    if (physicalLandscape && height > width) {",
					"                        var landWidth = Math.max(width, height, screen.width || 0, screen.height || 0);",
					"                        var landHeight = Math.min(width, height, screen.width || width, screen.height || height);",
					"                        width = landWidth;",
					"                        height = landHeight;",
					"                    } else if (physicalPortrait && width > height) {",
					"                        var portWidth = Math.min(width, height, screen.width || width, screen.height || height);",
					"                        var portHeight = Math.max(width, height, screen.width || 0, screen.height || 0);",
					"                        width = portWidth;",
					"                        height = portHeight;",
					"                    }",
					"                    return {",
					"                        width: Math.max(1, Math.round(width)),",
					"                        height: Math.max(1, Math.round(height))",
					"                    };",
					"                }",
					"",
					"                function applyPixelSize(element, size) {",
					"                    if (!element) {",
					"                        return;",
					"                    }",
					"                    element.style.width = size.width + 'px';",
					"                    element.style.height = size.height + 'px';",
					"                }",
					"",
					"                function syncCanvasToViewport() {",
					"                    var size = viewportSize();",
					"                    var rootStyle = document.documentElement.style;",
					"                    rootStyle.setProperty('--spd-viewport-width', size.width + 'px');",
					"                    rootStyle.setProperty('--spd-viewport-height', size.height + 'px');",
					"                    var canvas = document.getElementById('canvas');",
					"                    var container = canvas && canvas.parentElement ? canvas.parentElement : document.body.firstElementChild;",
					"                    applyPixelSize(document.body, size);",
					"                    applyPixelSize(container, size);",
					"                    if (canvas) {",
					"                        if (canvas.width !== size.width) {",
					"                            canvas.width = size.width;",
					"                        }",
					"                        if (canvas.height !== size.height) {",
					"                            canvas.height = size.height;",
					"                        }",
					"                        applyPixelSize(canvas, size);",
					"                    }",
					"                    return size;",
					"                }",
					"",
					"                function resizeLoop() {",
					"                    syncCanvasToViewport();",
					"                    window.requestAnimationFrame(resizeLoop);",
					"                }",
					"",
					"                function settleCanvasResize() {",
					"                    syncCanvasToViewport();",
					"                    window.setTimeout(syncCanvasToViewport, 60);",
					"                    window.setTimeout(syncCanvasToViewport, 250);",
					"                    window.setTimeout(syncCanvasToViewport, 600);",
					"                }",
					"",
					"                window.shatteredPixelDungeonResizeCanvas = settleCanvasResize;",
					"                window.shatteredPixelDungeonSyncViewport = syncCanvasToViewport;",
					"                window.addEventListener('resize', settleCanvasResize);",
					"                window.addEventListener('orientationchange', settleCanvasResize);",
					"                window.addEventListener('pageshow', settleCanvasResize);",
					"                if (window.visualViewport) {",
					"                    window.visualViewport.addEventListener('resize', settleCanvasResize);",
					"                    window.visualViewport.addEventListener('scroll', settleCanvasResize);",
					"                }",
					"                if (screen.orientation && screen.orientation.addEventListener) {",
					"                    screen.orientation.addEventListener('change', settleCanvasResize);",
					"                }",
					"",
					"                if (document.readyState === 'loading') {",
					"                    document.addEventListener('DOMContentLoaded', function () {",
					"                        settleCanvasResize();",
					"                        window.requestAnimationFrame(resizeLoop);",
					"                    });",
					"                } else {",
					"                    settleCanvasResize();",
					"                    window.requestAnimationFrame(resizeLoop);",
					"                }",
					"",
					"                var started = false;",
					"                var startButton = document.getElementById('" + START_BUTTON_ID + "');",
					"                function shouldWaitForGesture() {",
					"                    var standalone = !!(navigator.standalone || (window.matchMedia && window.matchMedia('(display-mode: standalone)').matches));",
					"                    var coarse = !!(window.matchMedia && window.matchMedia('(any-pointer: coarse)').matches);",
					"                    var ua = navigator.userAgent || '';",
					"                    return standalone || coarse || /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(ua);",
					"                }",
					"                function startGame() {",
					"                    if (started) {",
					"                        return;",
					"                    }",
					"                    started = true;",
					"                    if (window.shatteredPixelDungeonResumeAudio) {",
					"                        window.shatteredPixelDungeonResumeAudio();",
					"                    }",
					"                    settleCanvasResize();",
					"                    if (startButton) {",
					"                        startButton.removeAttribute('data-visible');",
					"                        startButton.hidden = true;",
					"                    }",
					"                    var start = window.shatteredPixelDungeonStartGame || window.start;",
					"                    if (typeof start === 'function') {",
					"                        start();",
					"                    }",
					"                }",
					"                if (shouldWaitForGesture()) {",
					"                    if (startButton) {",
					"                        startButton.hidden = false;",
					"                        startButton.setAttribute('data-visible', 'true');",
					"                        startButton.addEventListener('pointerup', startGame, { once: true });",
					"                        startButton.addEventListener('touchend', startGame, { once: true });",
					"                        startButton.addEventListener('click', startGame, { once: true });",
					"                    }",
					"                    document.addEventListener('pointerup', startGame, { once: true });",
					"                    document.addEventListener('touchend', startGame, { once: true });",
					"                    document.addEventListener('keydown', startGame, { once: true });",
					"                } else if (document.readyState === 'complete') {",
					"                    window.setTimeout(startGame, 0);",
					"                } else {",
					"                    window.addEventListener('load', startGame, { once: true });",
					"                }",
					"            }());",
					"",
					"            if ('serviceWorker' in navigator) {",
					"                window.addEventListener('load', function () {",
					"                    var refreshing = false;",
					"                    navigator.serviceWorker.addEventListener('controllerchange', function () {",
					"                        if (refreshing) {",
					"                            return;",
					"                        }",
					"                        refreshing = true;",
					"                        window.location.reload();",
					"                    });",
					"                    navigator.serviceWorker.register('service-worker.js').then(function (registration) {",
					"                        registration.update();",
					"                    });",
					"                });",
					"            }",
					"        </script>");
			html = html.replace("    </body>", script + "\n    </body>");
		}

		Files.write(index.toPath(), html.getBytes(StandardCharsets.UTF_8));
	}
}
