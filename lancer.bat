@echo off
title J.A.R.V.I.S - Just A Rather Very Intelligent System
color 0B

echo.
echo ============================================================
echo              J.A.R.V.I.S v7.4.1 - Lancement
echo         Just A Rather Very Intelligent System
echo ============================================================
echo.

cd /d "%~dp0backend"

echo [1/3] Verification de Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Python n'est pas installe !
    echo Telechargez Python sur https://python.org
    pause
    exit /b 1
)
echo Python OK

echo.
echo [2/3] Installation des dependances...
pip install -r requirements.txt -q
if errorlevel 1 (
    echo ERREUR: Echec de l'installation des dependances
    pause
    exit /b 1
)
echo Dependances installees

echo.
echo [3/3] Lancement du serveur JARVIS...
echo.
echo ============================================================
echo  Serveur disponible sur: http://localhost:5000
echo.
echo  COMMANDES VOCALES DISPONIBLES :
echo    - "Bonjour JARVIS" - Salutation
echo    - "Quelle heure est-il" - Heure actuelle
echo    - "Quel jour sommes-nous" - Date
echo    - "Statut systeme" - Performances
echo    - "Calcule 25 fois 4" - Calculatrice
echo    - "Raconte-moi une blague" - Divertissement
echo    - "Qui es-tu" - Identite de JARVIS
echo.
echo  Pour une experience complete :
echo    1. Configurez votre cle API Mistral dans backend/.env
echo    2. Utilisez Google Chrome ou Edge pour la reconnaissance vocale
echo    3. Autorisez l'acces au microphone quand demande
echo.
echo  Appuyez sur Ctrl+C pour arreter le serveur
echo ============================================================
echo.

python app.py

echo.
echo Serveur arrete.
pause
