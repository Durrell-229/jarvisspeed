@echo off
echo ============================================================
echo   J.A.R.V.I.S - Desktop Application Builder
echo ============================================================
echo.

REM Verifier Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python n'est pas installe ou n'est pas dans le PATH
    echo Installe Python 3.12+ depuis python.org
    pause
    exit /b 1
)

echo [1/4] Installation des dependances...
pip install -r requirements.txt --quiet
pip install pyinstaller --quiet

echo.
echo [2/4] Verification des fichiers...
if not exist "app.py" (
    echo ERROR: app.py non trouve
    pause
    exit /b 1
)
if not exist "jarvis_app.py" (
    echo ERROR: jarvis_app.py non trouve
    pause
    exit /b 1
)
if not exist "jarvis.spec" (
    echo ERROR: jarvis.spec non trouve
    pause
    exit /b 1
)

echo.
echo [3/4] Construction de l'executable...
pyinstaller --clean jarvis.spec

echo.
echo [4/4] Verification du build...
if exist "dist\JARVIS.exe" (
    echo.
    echo ============================================================
    echo   Build termine avec succes!
    echo ============================================================
    echo.
    echo   Executable: dist\JARVIS.exe
    echo.
    echo   Double-clique sur JARVIS.exe pour lancer l'application.
    echo   Le navigateur s'ouvrira automatiquement sur http://localhost:5000
    echo.
    echo   Pour distribuer:
    echo   - Copie le dossier dist/ sur une cle USB
    echo   - Ou cree un installeur avec Inno Setup ou NSIS
    echo ============================================================
) else (
    echo.
    echo ============================================================
    echo   ERREUR: Le build a echoue!
    echo   Verifie les messages d'erreur ci-dessus.
    echo ============================================================
)

echo.
pause
