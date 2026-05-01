"""
JARVIS System Tray Icon
Affiche une icône dans la barre des tâches avec menu contextuel
"""
import threading
import time
import webbrowser
import sys

try:
    import pystray
    from pystray import MenuItem as item
    from PIL import Image, ImageDraw
    HAS_PYSTRAY = True
except ImportError:
    HAS_PYSTRAY = False


def create_icon_image():
    """Crée une icône simple pour la barre des tâches"""
    size = 64
    img = Image.new('RGB', (size, size), color=(0, 102, 204))
    draw = ImageDraw.Draw(img)

    # Dessiner un cercle
    draw.ellipse([10, 10, 54, 54], fill=(0, 150, 255))

    # Dessiner un "J"
    draw.text((22, 18), "J", fill=(255, 255, 255))

    return img


class TrayIcon:
    def __init__(self, port=5000):
        self.port = port
        self.icon = None
        self.running = False

    def on_open(self, tray_icon=None):
        """Ouvre JARVIS dans le navigateur"""
        webbrowser.open(f'http://localhost:{self.port}')

    def on_quit(self, tray_icon=None):
        """Quitte l'application"""
        self.running = False
        if tray_icon:
            tray_icon.stop()
        sys.exit(0)

    def run(self):
        """Lance l'icône tray"""
        if not HAS_PYSTRAY:
            print("pystray non installé. L'icône tray sera désactivée.")
            print("Installez: pip install pystray Pillow")
            return

        image = create_icon_image()

        menu = [
            item('Ouvrir JARVIS', self.on_open),
            item('---', None),  # Séparateur
            item('Quitter', self.on_quit),
        ]

        self.icon = pystray.Icon(
            'jarvis',
            image,
            'JARVIS - Assistant IA',
            menu
        )

        self.running = True
        self.icon.run()


def start_tray_icon(port=5000):
    """Lance l'icône tray dans un thread séparé"""
    tray = TrayIcon(port)
    tray_thread = threading.Thread(target=tray.run, daemon=True)
    tray_thread.start()
    return tray


if __name__ == '__main__':
    print("Lancement de l'icône JARVIS...")
    tray = TrayIcon()
    tray.run()
