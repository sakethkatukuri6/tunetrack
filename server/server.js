/**
 * MusicPlayer LAN Cast & Sync Companion Server
 * 
 * Zero external dependencies - runs directly with: node server.js
 * Default Port: 8080
 */

const http = require('http');
const os = require('os');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 8080;

// Discover Local LAN IP Address (Wi-Fi or Ethernet)
function getLocalLanIp() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const net of interfaces[name]) {
            if (net.family === 'IPv4' && !net.internal) {
                return net.address;
            }
        }
    }
    return '127.0.0.1';
}

const LOCAL_IP = getLocalLanIp();

// In-Memory Playback State
let playbackState = {
    currentTrackIndex: 0,
    isPlaying: false,
    positionMs: 0,
    lastUpdateTimestamp: Date.now(),
    tracks: [
        {
            id: 1001,
            title: "Song 1 (Acoustic Sunrise)",
            artist: "Artist A",
            album: "Morning Echoes",
            durationMs: 225000,
            streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        },
        {
            id: 1002,
            title: "Song 2 (Prototype)",
            artist: "Artist B",
            album: "Neon Horizon",
            durationMs: 214000,
            streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        },
        {
            id: 1003,
            title: "Song 3 (Midnight Drive)",
            artist: "Artist C",
            album: "Cyber Sunset",
            durationMs: 198000,
            streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        }
    ]
};

// SSE (Server-Sent Events) clients for real-time LAN sync
const sseClients = new Set();

function broadcastState() {
    // Update live position if playing
    if (playbackState.isPlaying) {
        const now = Date.now();
        const elapsed = now - playbackState.lastUpdateTimestamp;
        playbackState.positionMs += elapsed;
        playbackState.lastUpdateTimestamp = now;
        const currentTrack = playbackState.tracks[playbackState.currentTrackIndex];
        if (currentTrack && playbackState.positionMs >= currentTrack.durationMs) {
            playbackState.currentTrackIndex = (playbackState.currentTrackIndex + 1) % playbackState.tracks.length;
            playbackState.positionMs = 0;
        }
    }

    const payload = JSON.stringify({
        type: 'STATE_UPDATE',
        state: {
            ...playbackState,
            currentTrack: playbackState.tracks[playbackState.currentTrackIndex]
        }
    });

    for (const client of sseClients) {
        client.write(`data: ${payload}\n\n`);
    }
}

// Minimal ASCII QR code generator for terminal
function renderAsciiBanner(serverUrl) {
    console.log('\n======================================================');
    console.log('       🎵 MUSIC PLAYER - LAN AUDIO CAST SERVER        ');
    console.log('======================================================');
    console.log(` Server active on:   http://${LOCAL_IP}:${PORT}`);
    console.log(` Local loopback:     http://localhost:${PORT}`);
    console.log('------------------------------------------------------');
    console.log(' Scan or visit this URL from any device on Wi-Fi:');
    console.log(`   👉 ${serverUrl}`);
    console.log('======================================================\n');
}

// Web Player HTML page
function getWebPlayerHtml() {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LAN Audio Cast - Music Player</title>
    <style>
        :root {
            --bg: #121212;
            --surface: #1e1e1e;
            --primary: #bb86fc;
            --primary-variant: #3700b3;
            --text: #e0e0e0;
            --text-secondary: #a0a0a0;
        }
        body {
            margin: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 16px;
            box-sizing: border-box;
        }
        .player-card {
            background-color: var(--surface);
            border-radius: 24px;
            padding: 28px;
            width: 100%;
            max-width: 420px;
            box-shadow: 0 12px 32px rgba(0,0,0,0.5);
            text-align: center;
        }
        .header {
            font-size: 13px;
            letter-spacing: 2px;
            font-weight: 600;
            color: var(--primary);
            margin-bottom: 24px;
        }
        .art-box {
            width: 220px;
            height: 220px;
            margin: 0 auto 24px auto;
            border-radius: 20px;
            background: linear-gradient(135deg, #6200ee, #03dac6);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 54px;
            box-shadow: 0 8px 24px rgba(98, 0, 238, 0.3);
        }
        .title {
            font-size: 20px;
            font-weight: bold;
            margin: 0 0 6px 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .artist {
            font-size: 15px;
            color: var(--text-secondary);
            margin: 0 0 20px 0;
        }
        .progress-container {
            margin: 20px 0 12px 0;
        }
        input[type="range"] {
            width: 100%;
            accent-color: var(--primary);
            cursor: pointer;
        }
        .timestamps {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-secondary);
            margin-top: 4px;
        }
        .controls {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 20px;
            margin-top: 16px;
        }
        .btn {
            background: none;
            border: none;
            color: var(--text);
            font-size: 24px;
            cursor: pointer;
            padding: 8px 12px;
            border-radius: 50%;
            transition: all 0.2s;
        }
        .btn:hover {
            background-color: rgba(255,255,255,0.1);
        }
        .btn-play {
            background-color: var(--primary);
            color: #000;
            width: 58px;
            height: 58px;
            font-size: 26px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
        }
        .btn-play:hover {
            transform: scale(1.05);
            background-color: #d7b7fd;
        }
        .status-badge {
            margin-top: 20px;
            font-size: 12px;
            background: rgba(255,255,255,0.05);
            padding: 6px 12px;
            border-radius: 12px;
            display: inline-block;
            color: var(--text-secondary);
        }
        .live-dot {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #03dac6;
            margin-right: 6px;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0% { opacity: 0.4; }
            50% { opacity: 1; }
            100% { opacity: 0.4; }
        }
    </style>
</head>
<body>
    <div class="player-card">
        <div class="header">LAN AUDIO CAST • SYNCED</div>
        <div class="art-box">🎵</div>
        <div class="title" id="trackTitle">Loading...</div>
        <div class="artist" id="trackArtist">Connecting to LAN...</div>

        <div class="progress-container">
            <input type="range" id="seekSlider" min="0" max="100" value="0">
            <div class="timestamps">
                <span id="currentTime">0:00</span>
                <span id="totalTime">0:00</span>
            </div>
        </div>

        <div class="controls">
            <button class="btn" onclick="sendAction('prev')">⏮</button>
            <button class="btn btn-play" id="btnPlay" onclick="sendAction('toggle')">▶</button>
            <button class="btn" onclick="sendAction('next')">⏭</button>
        </div>

        <div class="status-badge">
            <span class="live-dot"></span> <span id="syncStatus">Synced with LAN Server</span>
        </div>
    </div>

    <script>
        let currentState = null;

        function formatTime(ms) {
            if (!ms || ms < 0) ms = 0;
            const sec = Math.floor(ms / 1000);
            const m = Math.floor(sec / 60);
            const s = sec % 60;
            return m + ':' + (s < 10 ? '0' : '') + s;
        }

        function updateUI(state) {
            currentState = state;
            const track = state.currentTrack;
            if (track) {
                document.getElementById('trackTitle').textContent = track.title;
                document.getElementById('trackArtist').textContent = track.artist;
                document.getElementById('totalTime').textContent = formatTime(track.durationMs);
                document.getElementById('seekSlider').max = track.durationMs;
                document.getElementById('seekSlider').value = state.positionMs;
                document.getElementById('currentTime').textContent = formatTime(state.positionMs);
            }
            document.getElementById('btnPlay').textContent = state.isPlaying ? '⏸' : '▶';
        }

        // Connect SSE for Real-time Playback State Sync
        const eventSource = new EventSource('/api/events');
        eventSource.onmessage = (e) => {
            const data = JSON.parse(e.data);
            if (data.type === 'STATE_UPDATE') {
                updateUI(data.state);
            }
        };

        async function sendAction(action, payload = {}) {
            try {
                await fetch('/api/sync', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action, ...payload })
                });
            } catch (err) {
                console.error('Failed to sync action:', err);
            }
        }

        document.getElementById('seekSlider').addEventListener('change', (e) => {
            sendAction('seek', { positionMs: parseInt(e.target.value) });
        });

        // Initial fetch
        fetch('/api/status')
            .then(res => res.json())
            .then(data => updateUI(data.state))
            .catch(console.error);
    </script>
</body>
</html>`;
}

// HTTP Server
const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    // CORS Headers for LAN connectivity
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // 1. Root: Web Player UI
    if (pathname === '/' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(getWebPlayerHtml());
        return;
    }

    // 2. GET /api/status
    if (pathname === '/api/status' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            server: 'MusicPlayer LAN Cast',
            hostIp: LOCAL_IP,
            port: PORT,
            state: {
                ...playbackState,
                currentTrack: playbackState.tracks[playbackState.currentTrackIndex]
            }
        }));
        return;
    }

    // 3. GET /api/tracks
    if (pathname === '/api/tracks' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(playbackState.tracks));
        return;
    }

    // 4. GET /api/events (SSE)
    if (pathname === '/api/events' && req.method === 'GET') {
        res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
        });
        sseClients.add(res);

        // Send initial state immediately
        const initial = JSON.stringify({
            type: 'STATE_UPDATE',
            state: {
                ...playbackState,
                currentTrack: playbackState.tracks[playbackState.currentTrackIndex]
            }
        });
        res.write(`data: ${initial}\n\n`);

        req.on('close', () => {
            sseClients.delete(res);
        });
        return;
    }

    // 5. POST /api/sync
    if (pathname === '/api/sync' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const data = JSON.parse(body || '{}');
                const action = data.action;

                if (action === 'toggle') {
                    playbackState.isPlaying = !playbackState.isPlaying;
                    playbackState.lastUpdateTimestamp = Date.now();
                } else if (action === 'play') {
                    playbackState.isPlaying = true;
                    playbackState.lastUpdateTimestamp = Date.now();
                } else if (action === 'pause') {
                    playbackState.isPlaying = false;
                } else if (action === 'next') {
                    playbackState.currentTrackIndex = (playbackState.currentTrackIndex + 1) % playbackState.tracks.length;
                    playbackState.positionMs = 0;
                    playbackState.lastUpdateTimestamp = Date.now();
                } else if (action === 'prev') {
                    playbackState.currentTrackIndex = (playbackState.currentTrackIndex - 1 + playbackState.tracks.length) % playbackState.tracks.length;
                    playbackState.positionMs = 0;
                    playbackState.lastUpdateTimestamp = Date.now();
                } else if (action === 'seek' && typeof data.positionMs === 'number') {
                    playbackState.positionMs = data.positionMs;
                    playbackState.lastUpdateTimestamp = Date.now();
                } else if (action === 'select' && typeof data.index === 'number') {
                    playbackState.currentTrackIndex = data.index % playbackState.tracks.length;
                    playbackState.positionMs = 0;
                    playbackState.lastUpdateTimestamp = Date.now();
                }

                broadcastState();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true, state: playbackState }));
            } catch (err) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: err.message }));
            }
        });
        return;
    }

    // 404 Fallback
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
});

// Periodic heartbeat broadcast every 1s when playing
setInterval(() => {
    if (playbackState.isPlaying && sseClients.size > 0) {
        broadcastState();
    }
}, 1000);

server.listen(PORT, '0.0.0.0', () => {
    renderAsciiBanner(`http://${LOCAL_IP}:${PORT}`);
});

