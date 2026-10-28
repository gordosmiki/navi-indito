# Navi indító

Kis segédapp egy K2401 (NWD) androidos fejegységhez (Ford C-MAX).
A gyári beállításokban ezt kell **navigációs programnak** megadni.

## Letöltés

A legfrissebb APK mindig itt:
https://github.com/gordosmiki/navi-indito/releases/latest/download/navi-indito.apk

## Verziók

- **0.1-diag** – diagnosztikai verzió: minden indításkor feljegyzi, hogyan
  indította el a rendszer (Intent: action, flags, extras, referrer, alvási idő),
  és kiírja a képernyőre. Ebből derül ki, miben különbözik a NAVI gombos
  indítás az ébredés (gyújtásráadás) utáni indítástól.

## Terv

- Ébredés után: a háttérben elindítja az Automate „Gombfigyelő” folyamatát,
  majd visszaadja a képernyőt az AGAMA launchernek.
- NAVI gombra: elindítja a kiválasztott navigációs appot (pl. Organic Maps).
