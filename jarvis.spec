# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller spec file for JARVIS Desktop
Build: pyinstaller jarvis.spec
"""
import os
import sys

block_cipher = None

# Collect all data files
datas = [
    ('static', 'static'),
]

# Add .env if it exists
if os.path.exists('.env'):
    datas.append(('.env', '.'))

# Hidden imports - all modules used by JARVIS
hiddenimports = [
    'app',
    'system_actions',
    'system_monitor',
    'mistral_ai',
    'tts',
    'flask',
    'flask_cors',
    'dotenv',
    'psutil',
    'requests',
    'jinja2',
    'werkzeug',
]

a = Analysis(
    ['jarvis_app.py'],
    pathex=[],
    binaries=[],
    datas=datas,
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        'tkinter',
        'unittest',
        'email',
        'xml',
        'pydoc',
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='JARVIS',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,  # True pour voir les logs, False pour mode silencieux
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,  # Ajouter un fichier .ico si disponible
)
