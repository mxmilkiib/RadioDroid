'use strict';

const loadingIndicator = document.getElementById('loadingIndicator');
const bufferingOverlay = document.getElementById('bufferingOverlay');

let visualizer = null;
let visualizerActive = false;
let audioContext = null;
let analyser = null;
let mediaElement = null;
let renderLoopId = null;
let presetIndex = 0;
let presetTimerId = null;
let presets = null;

const PRESET_INTERVAL_MS = 15000;

function showBuffering() {
    if (bufferingOverlay) bufferingOverlay.classList.remove('hidden');
}

function hideBuffering() {
    if (bufferingOverlay) bufferingOverlay.classList.add('hidden');
}

function loadVisualizerLibs() {
    return new Promise((resolve, reject) => {
        if (window.butterchurn) {
            resolve();
            return;
        }

        const isSupportedScript = document.createElement('script');
        isSupportedScript.src = 'https://cdn.jsdelivr.net/npm/butterchurn@2.6.7/lib/isSupported.min.js';
        isSupportedScript.onload = () => {
            if (!window.isButterchurnSupported || !window.isButterchurnSupported()) {
                reject(new Error('Butterchurn (WebGL2) not supported on this device'));
                return;
            }

            const mainScript = document.createElement('script');
            mainScript.src = 'https://cdn.jsdelivr.net/npm/butterchurn@2.6.7/lib/butterchurn.min.js';
            mainScript.onload = () => {
                const presetsScript = document.createElement('script');
                presetsScript.src = 'https://cdn.jsdelivr.net/npm/butterchurn-presets@2.4.7/lib/butterchurn-presets.min.js';
                presetsScript.onload = () => {
                    if (window.butterchurn && window.butterchurnPresets) {
                        presets = window.butterchurnPresets.getPresets();
                        resolve();
                    } else {
                        reject(new Error('Butterchurn presets failed to load'));
                    }
                };
                presetsScript.onerror = () => reject(new Error('Failed to load butterchurn-presets'));
                document.head.appendChild(presetsScript);
            };
            mainScript.onerror = () => reject(new Error('Failed to load butterchurn'));
            document.head.appendChild(mainScript);
        };
        isSupportedScript.onerror = () => reject(new Error('Failed to load isSupported'));
        document.head.appendChild(isSupportedScript);
    });
}

function getMediaElement() {
    if (!mediaElement) {
        mediaElement = document.querySelector('video, audio');
        if (!mediaElement) {
            const player = document.querySelector('cast-media-player');
            if (player && player.shadowRoot) {
                mediaElement = player.shadowRoot.querySelector('video, audio');
            }
        }
    }
    return mediaElement;
}

function initVisualizer() {
    if (visualizer) return;

    const canvas = document.getElementById('visualizerCanvas');
    const el = getMediaElement();
    if (!el || !canvas) return;

    try {
        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const source = audioContext.createMediaElementSource(el);
            analyser = audioContext.createAnalyser();
            analyser.fftSize = 2048;
            source.connect(analyser);
            analyser.connect(audioContext.destination);
        }

        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        visualizer = window.butterchurn.createVisualizer(audioContext, canvas, {
            width: canvas.width,
            height: canvas.height
        });
        visualizer.connectAudio(analyser);

        const presetNames = Object.keys(presets);
        if (presetNames.length > 0) {
            visualizer.loadPreset(presets[presetNames[presetIndex]], 2.0);
        }

        startPresetCycling();
        startRenderLoop();
    } catch (e) {
        console.error('Visualizer init error:', e);
        visualizer = null;
    }
}

function startPresetCycling() {
    if (presetTimerId) clearInterval(presetTimerId);
    presetTimerId = setInterval(() => {
        if (!visualizer || !presets) return;
        const presetNames = Object.keys(presets);
        presetIndex = (presetIndex + 1) % presetNames.length;
        visualizer.loadPreset(presets[presetNames[presetIndex]], 2.0);
    }, PRESET_INTERVAL_MS);
}

function startRenderLoop() {
    if (renderLoopId) cancelAnimationFrame(renderLoopId);
    const render = () => {
        if (visualizer && visualizerActive) {
            visualizer.render();
        }
        renderLoopId = requestAnimationFrame(render);
    };
    render();
}

function stopVisualizer() {
    visualizerActive = false;
    if (presetTimerId) {
        clearInterval(presetTimerId);
        presetTimerId = null;
    }
    if (renderLoopId) {
        cancelAnimationFrame(renderLoopId);
        renderLoopId = null;
    }
    const canvas = document.getElementById('visualizerCanvas');
    if (canvas) {
        const ctx = canvas.getContext('webgl2') || canvas.getContext('webgl');
        if (ctx) ctx.clear(ctx.COLOR_BUFFER_BIT);
    }
}

function startVisualizer() {
    if (visualizer) {
        visualizerActive = true;
        startPresetCycling();
        startRenderLoop();
        return;
    }

    loadVisualizerLibs().then(() => {
        initVisualizer();
        visualizerActive = true;
    }).catch((e) => {
        console.warn('Visualizer not available:', e.message);
    });
}

function startReceiver() {
    const context = cast.framework.CastReceiverContext.getInstance();
    const playerManager = context.getPlayerManager();

    const options = new cast.framework.CastReceiverOptions();
    options.maxInactivity = 3600;
    context.start(options);

    if (loadingIndicator) {
        loadingIndicator.classList.add('hidden');
    }

    try {
        playerManager.addEventListener(cast.framework.events.EventType.BUFFERING, (event) => {
            if (event.isBuffering) {
                showBuffering();
            } else {
                hideBuffering();
            }
        });
    } catch(e) { console.error('Failed to add BUFFERING listener:', e); }

    try {
        playerManager.addEventListener(cast.framework.events.EventType.PLAYING, () => {
            hideBuffering();
        });
    } catch(e) { console.error('Failed to add PLAYING listener:', e); }

    try {
        playerManager.addEventListener(cast.framework.events.EventType.PAUSED, () => {
            hideBuffering();
        });
    } catch(e) { console.error('Failed to add PAUSED listener:', e); }

    try {
        playerManager.addEventListener(cast.framework.events.EventType.ENDED, () => {
            hideBuffering();
            stopVisualizer();
        });
    } catch(e) { console.error('Failed to add ENDED listener:', e); }

    try {
        playerManager.setMessageInterceptor(
            cast.framework.messages.MessageType.LOAD,
            (loadRequestData) => {
                const customData = loadRequestData.media && loadRequestData.media.customData;
                const shouldVisualize = customData && customData.visualize === true;

                if (shouldVisualize) {
                    startVisualizer();
                } else {
                    stopVisualizer();
                }

                if (loadRequestData.media && loadRequestData.media.contentId) {
                    var url = loadRequestData.media.contentId;
                    if (url.indexOf('http://') === 0) {
                        loadRequestData.media.contentId = 'https://' + url.substring(7);
                        console.log('Upgraded stream URL to HTTPS: ' + loadRequestData.media.contentId);
                    }
                }

                return loadRequestData;
            }
        );
    } catch(e) { console.error('Failed to set LOAD interceptor:', e); }

    window.addEventListener('resize', () => {
        if (visualizer) {
            const canvas = document.getElementById('visualizerCanvas');
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
            visualizer.setRendererSize(canvas.width, canvas.height);
        }
    });
}

if (typeof cast !== 'undefined' && cast.framework && cast.framework.CastReceiverContext) {
    startReceiver();
} else {
    let attempts = 0;
    const interval = setInterval(() => {
        attempts++;
        if (typeof cast !== 'undefined' && cast.framework && cast.framework.CastReceiverContext) {
            clearInterval(interval);
            startReceiver();
        } else if (attempts > 25) {
            clearInterval(interval);
            if (loadingIndicator) {
                loadingIndicator.textContent = 'Error: Cast SDK not loaded';
                loadingIndicator.style.color = '#ff4444';
            }
        }
    }, 200);
}
