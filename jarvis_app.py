"""
JARVIS Desktop Application
Lance le serveur Flask et ouvre l'interface dans le navigateur
"""
import webbrowser
import threading
import time
import sys
import os
import socket
from app import app


def get_local_ip():
    """Obtient l'IP locale pour l'affichage"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return '127.0.0.1'


def open_browser(port):
    """Ouvre le navigateur après un délai"""
    time.sleep(2.5)
    webbrowser.open(f'http://localhost:{port}')


if __name__ == '__main__':
    # Configuration
    port = int(os.getenv('PORT', 5000))
    host = '127.0.0.1'
    local_ip = get_local_ip()

    print("=" * 60)
    print("  J.A.R.V.I.S - Desktop Application")
    print("  Just A Rather Very Intelligent System")
    print("=" * 60)
    print()
    print(f"  Serving at: http://{host}:{port}")
    print(f"  Local network: http://{local_ip}:{port}")
    print()
    print("  Opening browser automatically...")
    print("  Press Ctrl+C to quit")
    print("=" * 60)
    print()

    # Ouvrir le navigateur automatiquement
    threading.Thread(target=open_browser, args=(port,), daemon=True).start()

    # Lancer le serveur Flask
    try:
        app.run(host=host, port=port, debug=False, threaded=True)
    except KeyboardInterrupt:
        print("\n\n  JARVIS shutting down...")
        sys.exit(0)
