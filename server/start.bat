@echo off
title Music Player LAN Cast Server
cd /d "%~dp0"
echo Starting LAN Audio Cast Server on port 8080...
node server.js
pause

