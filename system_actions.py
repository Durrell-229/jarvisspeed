"""
Module d'actions système pour JARVIS
Permet le contrôle réel du système via des commandes structurées
"""
import os
import sys
import subprocess
import webbrowser
import json
import platform
from datetime import datetime
from pathlib import Path


# ============================================================
# REGISTRY D'APPLICATIONS (Windows)
# ============================================================

APP_REGISTRY = {
    # Navigateurs
    "chrome": r"chrome.exe",
    "google chrome": r"chrome.exe",
    "edge": r"msedge.exe",
    "firefox": r"firefox.exe",
    "brave": r"brave.exe",

    # Musique / Média
    "spotify": r"spotify.exe",
    "spotify music": r"spotify.exe",
    "vlc": r"vlc.exe",
    "windows media player": r"wmplayer.exe",
    "groove": r"wmplayer.exe",

    # Productivité
    "notepad": r"notepad.exe",
    "bloc-notes": r"notepad.exe",
    "calculatrice": r"calc.exe",
    "calculator": r"calc.exe",
    "word": r"WINWORD.EXE",
    "excel": r"EXCEL.EXE",
    "powerpoint": r"POWERPNT.EXE",

    # Terminal / Dev
    "terminal": r"wt.exe",
    "windows terminal": r"wt.exe",
    "powershell": r"powershell.exe",
    "cmd": r"cmd.exe",
    "invite de commandes": r"cmd.exe",
    "vscode": r"Code.exe",
    "visual studio code": r"Code.exe",
    "code": r"Code.exe",

    # Explorateur
    "explorer": r"explorer.exe",
    "explorateur": r"explorer.exe",
    "fichiers": r"explorer.exe",

    # Communication
    "discord": r"Discord.exe",
    "teams": r"Teams.exe",
    "slack": r"Slack.exe",
    "whatsapp": r"WhatsApp.exe",

    # Paramètres
    "paramètres": "ms-settings:",
    "settings": "ms-settings:",
    "gestionnaire de tâches": r"taskmgr.exe",
    "task manager": r"taskmgr.exe",
}

# Liste des apps autorisées (pour validation)
ALLOWED_APPS = set(APP_REGISTRY.keys())


# ============================================================
# LANCER UNE APPLICATION
# ============================================================

def launch_app(app_name):
    """Lance une application par son nom"""
    app_name = app_name.lower().strip()

    if app_name not in APP_REGISTRY:
        close_apps = [a for a in ALLOWED_APPS if app_name in a]
        if close_apps:
            app_name = close_apps[0]
        else:
            return {
                "success": False,
                "message": f"Application '{app_name}' non reconnue. Applications disponibles: {', '.join(sorted(ALLOWED_APPS)[:15])}..."
            }

    try:
        target = APP_REGISTRY[app_name]

        # URI scheme (ms-settings:)
        if target.startswith("ms-"):
            os.startfile(target)
            return {"success": True, "message": f"Ouverture des {app_name}."}

        # Chercher dans PATH d'abord
        if subprocess.run(["where", target], capture_output=True, shell=True).returncode == 0:
            subprocess.Popen(target, shell=True)
            return {"success": True, "message": f"Application '{app_name}' lancée."}

        # Chercher dans les chemins courants Windows
        search_paths = [
            os.environ.get("ProgramFiles", r"C:\Program Files"),
            os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)"),
            os.path.join(os.environ.get("LOCALAPPDATA", ""), "Microsoft", "WindowsApps"),
            os.path.join(os.environ.get("LOCALAPPDATA", "")),
        ]

        for base in search_paths:
            for root, dirs, files in os.walk(base, topdown=True):
                # Limiter la profondeur
                if root.count(os.sep) - base.count(os.sep) > 4:
                    continue
                if target.lower() in [f.lower() for f in files]:
                    full_path = os.path.join(root, target)
                    subprocess.Popen(full_path)
                    return {"success": True, "message": f"Application '{app_name}' lancée depuis {full_path}."}

        # Fallback: essayer directement
        subprocess.Popen(target, shell=True)
        return {"success": True, "message": f"Tentative de lancement de '{app_name}'."}

    except FileNotFoundError:
        return {"success": False, "message": f"Impossible de trouver '{app_name}' sur ce système."}
    except Exception as e:
        return {"success": False, "message": f"Erreur lors du lancement de '{app_name}': {str(e)}"}


# ============================================================
# CONTRÔLE DU VOLUME (Windows PowerShell)
# ============================================================

def volume_control(action, level=None):
    """Contrôle le volume système via PowerShell"""
    action = action.lower().strip()

    try:
        if action == "up" or action == "monter" or action == "augmenter":
            # Augmente de 10%
            script = """
            Add-Type -TypeDefinition @'
using System.Runtime.InteropServices;
[Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IAudioEndpointVolume {
    int f(); int g(); int n(); int m();
    [PreserveSig] int SetMasterVolumeLevelScalar(float fLevel, System.Guid pguidEventContext);
    [PreserveSig] int GetMasterVolumeLevelScalar(out float pfLevelCurrent);
}
[DllImport("mmdeviceapi.dll")] static extern int CoCreateInstance(ref Guid clsid, object pUnkOuter, uint dwClsCtx, ref Guid riid, out IAudioEndpointVolume ppv);
'@
$clsid = [Guid]"{5CDF2C82-841E-4546-9722-0CF74078229A}"
$iid = [Guid]"{5CDF2C82-841E-4546-9722-0CF74078229A}"
# Simple approach via SendKeys
$shell = New-Object -ComObject WScript.Shell
for ($i=0; $i -lt 2; $i++) { $shell.SendKeys([char]175) }
"""
            subprocess.run(["powershell", "-Command", script], capture_output=True, timeout=5)
            return {"success": True, "message": "Volume augmenté."}

        elif action == "down" or action == "descendre" or action == "réduire" or action == "baisser":
            script = """
            $shell = New-Object -ComObject WScript.Shell
            for ($i=0; $i -lt 2; $i++) { $shell.SendKeys([char]174) }
            """
            subprocess.run(["powershell", "-Command", script], capture_output=True, timeout=5)
            return {"success": True, "message": "Volume réduit."}

        elif action == "mute" or action == "couper":
            script = """
            $shell = New-Object -ComObject WScript.Shell
            $shell.SendKeys([char]173)
            """
            subprocess.run(["powershell", "-Command", script], capture_output=True, timeout=5)
            return {"success": True, "message": "Volume coupé."}

        elif action == "set" or action == "définir" or action == "mettre":
            if level is None:
                return {"success": False, "message": "Niveau de volume requis pour l'action 'set'."}
            level = max(0, min(100, int(level)))
            # Utilise PowerShell avec NirCmd ou approche alternative
            script = f"""
            $shell = New-Object -ComObject WScript.Shell
            # Mute d'abord puis ajuste
            $shell.SendKeys([char]173)
            Start-Sleep -Milliseconds 200
            $shell.SendKeys([char]173)
            Start-Sleep -Milliseconds 200
            # Ajuste par pas de 2%
            $steps = [math]::Round({level} / 2)
            for ($i=0; $i -lt $steps; $i++) {{ $shell.SendKeys([char]175) }}
            """
            subprocess.run(["powershell", "-Command", script], capture_output=True, timeout=15)
            return {"success": True, "message": f"Volume défini à {level}%."}

        else:
            return {"success": False, "message": f"Action de volume non reconnue: {action}. Utilisez: up, down, mute, set."}

    except Exception as e:
        return {"success": False, "message": f"Erreur de contrôle du volume: {str(e)}"}


# ============================================================
# CONTRÔLE MÉDIA (Windows)
# ============================================================

def media_control(action):
    """Contrôle la lecture média via touches clavier virtuelles"""
    action = action.lower().strip()

    try:
        script_map = {
            "play": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]179)",
            "pause": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]179)",
            "toggle": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]179)",
            "next": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]176)",
            "suivant": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]176)",
            "previous": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]177)",
            "précédent": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]177)",
            "stop": "$shell = New-Object -ComObject WScript.Shell; $shell.SendKeys([char]178)",
        }

        if action in script_map:
            subprocess.run(["powershell", "-Command", script_map[action]], capture_output=True, timeout=5)
            labels = {
                "play": "Lecture", "pause": "Pause", "toggle": "Lecture/Pause",
                "next": "Média suivant", "suivant": "Média suivant",
                "previous": "Média précédent", "précédent": "Média précédent",
                "stop": "Lecture arrêtée"
            }
            return {"success": True, "message": f"{labels.get(action, action)}."}
        else:
            return {"success": False, "message": f"Action média non reconnue: {action}. Utilisez: play, pause, next, previous, stop."}

    except Exception as e:
        return {"success": False, "message": f"Erreur de contrôle média: {str(e)}"}


# ============================================================
# GESTION DE FICHIERS
# ============================================================

def file_manager(action, path=None):
    """Gère les fichiers et dossiers"""
    action = action.lower().strip()

    try:
        if action == "list" or action == "lister":
            target = path or os.path.expanduser("~\\Desktop")
            target = os.path.normpath(target)
            # Sécurité: empêcher le traversal
            if ".." in target:
                return {"success": False, "message": "Chemin non autorisé."}

            items = []
            for item in os.listdir(target)[:20]:  # Limite à 20
                full = os.path.join(target, item)
                prefix = "[DIR]" if os.path.isdir(full) else "[FILE]"
                items.append(f"{prefix} {item}")

            return {
                "success": True,
                "message": f"Contenu de {target}:\n" + "\n".join(items),
                "count": len(items)
            }

        elif action == "create" or action == "créer":
            if not path:
                return {"success": False, "message": "Nom du fichier/dossier requis."}
            full_path = os.path.expanduser(f"~\\Desktop\\{path}")
            full_path = os.path.normpath(full_path)
            if ".." in full_path:
                return {"success": False, "message": "Chemin non autorisé."}

            if path.endswith((".txt", ".md", ".py", ".js", ".html")):
                with open(full_path, "w", encoding="utf-8") as f:
                    f.write("")
                return {"success": True, "message": f"Fichier '{path}' créé sur le bureau."}
            else:
                os.makedirs(full_path, exist_ok=True)
                return {"success": True, "message": f"Dossier '{path}' créé sur le bureau."}

        elif action == "open" or action == "ouvrir":
            if not path:
                return {"success": False, "message": "Chemin du fichier requis."}
            # Chercher sur le bureau d'abord
            full_path = os.path.expanduser(f"~\\Desktop\\{path}")
            if not os.path.exists(full_path):
                full_path = os.path.expanduser(f"~\\Documents\\{path}")
            if not os.path.exists(full_path):
                full_path = os.path.normpath(path)
            if not os.path.exists(full_path):
                return {"success": False, "message": f"Fichier '{path}' non trouvé."}

            os.startfile(full_path)
            return {"success": True, "message": f"Ouverture de '{path}'."}

        elif action == "search" or action == "chercher":
            if not path:
                return {"success": False, "message": "Terme de recherche requis."}
            # Cherche sur le bureau
            desktop = os.path.expanduser("~\\Desktop")
            results = []
            for root, dirs, files in os.walk(desktop):
                if root.count(os.sep) - desktop.count(os.sep) > 3:
                    continue
                for f in files:
                    if path.lower() in f.lower():
                        results.append(os.path.join(root, f))
                        if len(results) >= 10:
                            break
                if len(results) >= 10:
                    break

            if results:
                return {
                    "success": True,
                    "message": f"Fichiers trouvés:\n" + "\n".join(results),
                    "count": len(results)
                }
            return {"success": True, "message": f"Aucun fichier trouvé pour '{path}' sur le bureau."}

        else:
            return {"success": False, "message": f"Action de fichier non reconnue: {action}. Utilisez: list, create, open, search."}

    except Exception as e:
        return {"success": False, "message": f"Erreur de gestion de fichiers: {str(e)}"}


# ============================================================
# OUVRIR UN SITE WEB
# ============================================================

def open_website(url):
    """Ouvre un site web dans le navigateur par défaut"""
    if not url:
        return {"success": False, "message": "URL requise."}

    # Ajouter https:// si manquant
    if not url.startswith(("http://", "https://")):
        url = "https://" + url

    try:
        webbrowser.open(url)
        return {"success": True, "message": f"Ouverture de {url}."}
    except Exception as e:
        return {"success": False, "message": f"Impossible d'ouvrir {url}: {str(e)}"}


# ============================================================
# RECHERCHE WEB
# ============================================================

def web_search(query, engine="google"):
    """Effectue une recherche web"""
    if not query:
        return {"success": False, "message": "Terme de recherche requis."}

    import urllib.parse
    encoded = urllib.parse.quote(query)

    urls = {
        "google": f"https://www.google.com/search?q={encoded}",
        "bing": f"https://www.bing.com/search?q={encoded}",
        "wikipedia": f"https://fr.wikipedia.org/wiki/{encoded}",
        "youtube": f"https://www.youtube.com/results?search_query={encoded}",
        "duckduckgo": f"https://duckduckgo.com/?q={encoded}",
    }

    url = urls.get(engine.lower(), urls["google"])

    try:
        webbrowser.open(url)
        return {"success": True, "message": f"Recherche '{query}' sur {engine}."}
    except Exception as e:
        return {"success": False, "message": f"Erreur de recherche: {str(e)}"}


# ============================================================
# CONTRÔLE SYSTÈME
# ============================================================

def system_control(action):
    """Contrôle le système (shutdown, restart, sleep, lock)"""
    action = action.lower().strip()

    actions = {
        "shutdown": "shutdown /s /t 60 /c 'Arrêt initié par JARVIS'",
        "restart": "shutdown /r /t 60 /c 'Redémarrage initié par JARVIS'",
        "sleep": "powershell -Command '(Add-Type -MemberDefinition \\\"[DllImport(\\\\\\\"powrprof.dll\\\\\\\",SetLastError=true)]\\\" -Name \\\"SetSuspendState\\\" -Namespace \\\"Win32\\\" -PassThru)::SetSuspendState($false,$true,$true)'",
        "lock": "rundll32.exe user32.dll,LockWorkStation",
        "hibernate": "shutdown /h",
    }

    if action not in actions:
        return {"success": False, "message": f"Action système non reconnue: {action}. Utilisez: shutdown, restart, sleep, lock, hibernate."}

    # Pour shutdown/restart, on retourne juste l'info sans exécuter (sécurité)
    if action in ("shutdown", "restart", "hibernate"):
        return {
            "success": True,
            "message": f"Action '{action}' prête. Pour des raisons de sécurité, confirmez manuellement.",
            "command": actions[action],
            "requires_confirmation": True
        }

    try:
        subprocess.Popen(actions[action], shell=True)
        return {"success": True, "message": f"Action '{action}' exécutée."}
    except Exception as e:
        return {"success": False, "message": f"Erreur: {str(e)}"}


# ============================================================
# INFOS SYSTÈME
# ============================================================

def get_system_info(metric="all", monitor=None):
    """Retourne les infos système"""
    if monitor:
        info = monitor.get_system_info()
        if metric == "cpu":
            return {"success": True, "message": f"CPU à {info['cpu']}%."}
        elif metric == "memory":
            return {"success": True, "message": f"Mémoire à {info['memory']['percent']}%."}
        elif metric == "disk":
            return {"success": True, "message": f"Disque à {info['disk']['percent']}%."}
        elif metric == "network":
            net = info['network']
            return {
                "success": True,
                "message": f"Réseau: upload {net['upload_speed']:.0f} B/s, download {net['download_speed']:.0f} B/s."
            }
        else:
            cpu = info['cpu']
            mem = info['memory']['percent']
            disk = info['disk']['percent']
            return {
                "success": True,
                "message": f"CPU {cpu}%, Mémoire {mem}%, Disque {disk}%. Tous les systèmes sont opérationnels."
            }
    else:
        return {"success": True, "message": "Informations système non disponibles."}


# ============================================================
# REGISTRY DES ACTIONS (pour exécution dynamique)
# ============================================================

ACTION_REGISTRY = {
    "launch_app": launch_app,
    "volume_control": volume_control,
    "media_control": media_control,
    "file_manager": file_manager,
    "open_website": open_website,
    "web_search": web_search,
    "system_control": system_control,
}


def execute_action(action_name, **kwargs):
    """Exécute une action par son nom avec les arguments fournis"""
    if action_name not in ACTION_REGISTRY:
        return {"success": False, "message": f"Action '{action_name}' non reconnue."}

    try:
        result = ACTION_REGISTRY[action_name](**kwargs)
        return result
    except Exception as e:
        return {"success": False, "message": f"Erreur lors de l'exécution de '{action_name}': {str(e)}"}


def get_available_actions():
    """Retourne la liste des actions disponibles avec descriptions"""
    return {
        "launch_app": "Ouvre une application (chrome, spotify, vscode, notepad, calc, discord, etc.)",
        "volume_control": "Contrôle le volume (up, down, mute, set avec level)",
        "media_control": "Contrôle les médias (play, pause, next, previous, stop)",
        "file_manager": "Gère les fichiers (list, create, open, search)",
        "open_website": "Ouvre un site web (url)",
        "web_search": "Recherche sur le web (query, engine: google/bing/wikipedia/youtube)",
        "system_control": "Contrôle système (shutdown, restart, sleep, lock, hibernate)",
    }
