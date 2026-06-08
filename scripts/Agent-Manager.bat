@echo off
title Agent Manager
powershell.exe -NoLogo -NoExit -ExecutionPolicy Bypass -Command "Set-Location -LiteralPath '%~dp0'; & '.\Agent-Manager.ps1'"
