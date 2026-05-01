"""
JARVIS Global Hotkey
Raccourci clavier Ctrl+Shift+J pour activer/désactiver le microphone
"""
import threading
import time
import sys

try:
    import keyboard
    HAS_KEYBOARD = True
except ImportError:
    HAS_KEYBOARD = False


class GlobalHotkey:
    def __init__(self, callback=None):
        """
        Initialise le gestionnaire de raccourci clavier global

        Args:
            callback: Fonction appelée quand le raccourci est pressé
        """
        self.callback = callback
        self.is_listening = False
        self.hotkey = 'ctrl+shift+j'

    def on_hotkey(self):
        """Called when the hotkey is pressed"""
        self.is_listening = not self.is_listening

        if self.is_listening:
            print("\n[JARVIS] Microphone activé (Ctrl+Shift+J)")
            if self.callback:
                self.callback(start=True)
        else:
            print("\n[JARVIS] Microphone désactivé (Ctrl+Shift+J)")
            if self.callback:
                self.callback(start=False)

    def start(self):
        """Commence à écouter le raccourci clavier"""
        if not HAS_KEYBOARD:
            print("keyboard non installé. Le raccourci clavier sera désactivé.")
            print("Installez: pip install keyboard")
            print("(Note: nécessite les droits administrateur sur Windows)")
            return False

        try:
            keyboard.add_hotkey(self.hotkey, self.on_hotkey)
            print(f"[JARVIS] Raccourci clavier actif: {self.hotkey.upper()}")
            return True
        except Exception as e:
            print(f"[JARVIS] Erreur lors de l'initialisation du raccourci: {e}")
            return False

    def stop(self):
        """Arrête l'écoute du raccourci clavier"""
        if HAS_KEYBOARD:
            try:
                keyboard.remove_hotkey(self.hotkey)
            except Exception:
                pass

    def is_active(self):
        """Retourne si le microphone est actuellement activé"""
        return self.is_listening


def start_global_hotkey(callback=None):
    """Lance le gestionnaire de raccourci dans un thread séparé"""
    hotkey = GlobalHotkey(callback)

    def listen():
        if hotkey.start():
            # Garder le thread en vie
            try:
                while True:
                    time.sleep(0.1)
            except KeyboardInterrupt:
                hotkey.stop()

    hotkey_thread = threading.Thread(target=listen, daemon=True)
    hotkey_thread.start()

    return hotkey


if __name__ == '__main__':
    def my_callback(start):
        if start:
            print(">> Start listening...")
        else:
            print(">> Stop listening...")

    print("JARVIS Global Hotkey Test")
    print("Press Ctrl+Shift+J to toggle microphone")
    print("Press Ctrl+C to quit")

    hotkey = start_global_hotkey(my_callback)

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nQuitting...")
