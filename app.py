"""
J.A.R.V.I.S - Backend Flask
Just A Rather Very Intelligent System
"""
from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS
from system_monitor import SystemMonitor
from mistral_ai import MistralAI
from tts import TextToSpeech
import os
import platform
import subprocess
from datetime import datetime
import re
from dotenv import set_key

app = Flask(__name__, static_folder='static', static_url_path='')
CORS(app)

# Initialisation des modules
monitor = SystemMonitor()
ai = MistralAI()
tts = TextToSpeech()

# Variables globales
request_count = 0
start_time = datetime.now()

# ============================================================
# COMMANDES SYSTÈME
# ============================================================

def execute_command(command):
    """Exécute une commande système et retourne le résultat"""
    command = command.lower().strip()

    # Obtenir l'heure
    if any(word in command for word in ['heure', 'quel temps', 'time']):
        now = datetime.now()
        return f"Il est {now.strftime('%H heures %M minutes et %S secondes')}."

    # Obtenir la date
    if any(word in command for word in ['date', 'quel jour', 'jour']):
        now = datetime.now()
        jours = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche']
        mois = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin',
                'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']
        jour = jours[now.weekday()]
        return f"Nous sommes le {jour} {now.day} {mois[now.month-1]} {now.year}."

    # État du système
    if any(word in command for word in ['statut', 'status', 'performance', 'systeme']):
        cpu = monitor.get_cpu_usage()
        mem = monitor.get_memory_usage()
        return (
            f"Système nominal. CPU à {cpu}%, mémoire à {mem['percent']}%. "
            f"Tous les systèmes sont opérationnels."
        )

    # Arrêter le système
    if any(word in command for word in ['éteins', 'eteins', 'shutdown', 'ferme']):
        return "Je ne peux pas éteindre le système pour des raisons de sécurité. Faites-le manuellement."

    # Calculatrice simple
    calc_match = re.search(r'calcul[e]*\s*(.+)', command)
    if calc_match:
        try:
            expression = calc_match.group(1)
            # Remplacer les symboles
            expression = expression.replace('fois', '*').replace('x', '*').replace('×', '*')
            expression = expression.replace('divisé par', '/').replace('sur', '/')
            expression = expression.replace('plus', '+').replace('moins', '-')
            expression = re.sub(r'[^\d+\-*/().%\s]', '', expression)
            result = eval(expression)
            return f"Le résultat est {result}."
        except Exception:
            return "Je n'ai pas pu calculer cette expression."

    # Information sur le système
    if any(word in command for word in ['info systeme', 'machine', 'ordinateur']):
        return (
            f"Système: {platform.system()} {platform.release()}. "
            f"Processeur: {platform.processor() or 'Non détecté'}. "
            f"Architecture: {platform.machine()}."
        )

    # Météo (simulée)
    if any(word in command for word in ['météo', 'meteo', 'température', 'temperature']):
        return "La météo actuelle est de 28°C à Cotonou avec un ciel partiellement nuageux."

    # Blague
    if any(word in command for word in ['blague', 'rire', 'drôle', 'amuse']):
        import random
        blagues = [
            "Pourquoi les plongeurs plongent-ils toujours en arrière ? Parce que sinon ils tomberaient dans le bateau.",
            "Qu'est-ce qu'un canif ? Un petit fien.",
            "Que dit une imprimante dans l'eau ? Je patafouille !",
            "Pourquoi le livre est-il tombé ? Parce qu'il n'avait pas de couvert.",
        ]
        return random.choice(blagues)

    return None

# ============================================================
# ROUTES PRINCIPALES
# ============================================================

@app.route('/')
def index():
    """Sert le fichier HTML principal"""
    return send_from_directory(app.static_folder, 'index.html')

@app.route('/api/status')
def get_status():
    """Retourne le statut général du système"""
    global request_count
    request_count += 1

    sys_info = monitor.get_system_info()
    return jsonify({
        'status': 'online',
        'system': {
            'cpu': sys_info['cpu'],
            'memory': sys_info['memory']['percent'],
            'network': {
                'upload_speed': sys_info['network']['upload_speed'],
                'download_speed': sys_info['network']['download_speed']
            },
            'disk': sys_info['disk']['percent']
        },
        'uptime': monitor.get_uptime_formatted(),
        'request_count': request_count,
        'timestamp': sys_info['timestamp'],
        'version': '7.4.1',
        'engine': 'Mistral AI'
    })

@app.route('/api/system')
def get_system_info():
    """Retourne les informations détaillées du système"""
    global request_count
    request_count += 1

    sys_info = monitor.get_system_info()
    return jsonify({
        'cpu': sys_info['cpu'],
        'memory': sys_info['memory'],
        'network': sys_info['network'],
        'disk': sys_info['disk'],
        'os': platform.system(),
        'os_version': platform.version(),
        'machine': platform.machine(),
        'processor': platform.processor(),
        'timestamp': sys_info['timestamp'],
        'uptime': monitor.get_uptime_formatted()
    })

# ============================================================
# CHAT ET IA
# ============================================================

@app.route('/api/chat', methods=['POST'])
def chat():
    """Endpoint pour le dialogue avec l'IA"""
    global request_count
    request_count += 1

    data = request.get_json()
    if not data or 'message' not in data:
        return jsonify({'error': 'Message requis'}), 400

    user_message = data['message']
    use_history = data.get('history', True)
    use_tts = data.get('speak', False)

    # Vérifier si c'est une commande système
    command_result = execute_command(user_message)
    if command_result:
        response = command_result
        if use_tts:
            tts.speak(response)
        return jsonify({
            'response': response,
            'request_count': request_count,
            'type': 'command'
        })

    # Sinon, utiliser l'IA Mistral
    response = ai.chat(user_message, use_history)

    # Synthèse vocale si demandée
    if use_tts:
        tts.speak(response)

    return jsonify({
        'response': response,
        'request_count': request_count,
        'type': 'ai'
    })

@app.route('/api/chat/history', methods=['GET'])
def get_chat_history():
    """Retourne l'historique de conversation"""
    return jsonify({
        'history': ai.get_history()
    })

@app.route('/api/chat/clear', methods=['POST'])
def clear_chat_history():
    """Efface l'historique de conversation"""
    ai.clear_history()
    return jsonify({'status': 'history cleared'})

# ============================================================
# COMMANDES VOCALES
# ============================================================

@app.route('/api/voice/command', methods=['POST'])
def voice_command():
    """Reçoit une commande vocale transcrite et l'exécute"""
    global request_count
    request_count += 1

    data = request.get_json()
    if not data or 'text' not in data:
        return jsonify({'error': 'Texte requis'}), 400

    text = data['text']
    response_text = process_voice_command(text)

    # Synthèse vocale automatique
    tts.speak(response_text)

    return jsonify({
        'command': text,
        'response': response_text,
        'request_count': request_count
    })

def process_voice_command(text):
    """Traite une commande vocale et retourne une réponse"""
    text = text.lower().strip()

    # Salutations
    if any(word in text for word in ['bonjour', 'salut', 'hello', 'hey', 'jarvis']):
        hour = datetime.now().hour
        if hour < 12:
            return "Bonjour, monsieur. Comment puis-je vous aider ce matin ?"
        elif hour < 18:
            return "Bonjour, monsieur. Que puis-je faire pour vous cet après-midi ?"
        else:
            return "Bonsoir, monsieur. Comment puis-je vous assister ce soir ?"

    # Comment vas-tu
    if any(word in text for word in ['comment vas', 'ça va', 'ca va', 'comment tu']):
        return "Je fonctionne parfaitement, tous mes systèmes sont nominaux. Merci de demander."

    # Qui es-tu
    if any(word in text for word in ['qui es-tu', 'qui est tu', 'ton nom', 'tu es qui']):
        return "Je suis J.A.R.V.I.S, Just A Rather Very Intelligent System. Votre assistant personnel."

    # Merci
    if any(word in text for word in ['merci', 'thanks', 'super', 'parfait']):
        return "Avec plaisir, monsieur. Je reste à votre disposition."

    # Au revoir
    if any(word in text for word in ['au revoir', 'bye', 'adieu', 'à bientôt']):
        return "À votre service, monsieur. Bonne journée."

    # Heure
    if any(word in text for word in ['heure', 'quelle heure']):
        now = datetime.now()
        return f"Il est {now.strftime('%H heures et %M minutes')}."

    # Date
    if any(word in text for word in ['date', 'quel jour', 'quelle date']):
        now = datetime.now()
        mois = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin',
                'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']
        return f"Nous sommes le {now.day} {mois[now.month-1]} {now.year}."

    # Statut système
    if any(word in text for word in ['statut', 'status', 'performance']):
        cpu = monitor.get_cpu_usage()
        mem = monitor.get_memory_usage()
        return f"Système nominal. CPU à {cpu}%, mémoire à {mem['percent']}%."

    # Calcul
    calc_match = re.search(r'calcul[e]*\s*(.+)', text)
    if calc_match:
        try:
            expression = calc_match.group(1)
            expression = expression.replace('fois', '*').replace('x', '*')
            expression = expression.replace('divisé par', '/').replace('sur', '/')
            expression = expression.replace('plus', '+').replace('moins', '-')
            expression = re.sub(r'[^\d+\-*/().%\s]', '', expression)
            result = eval(expression)
            return f"Le résultat est {result}."
        except Exception:
            return "Je n'ai pas pu effectuer ce calcul."

    # Silence / tais-toi
    if any(word in text for word in ['tais-toi', 'tais toi', 'silence', 'ferme-la']):
        return "Comme vous voudrez, monsieur."

    # Parle / voix
    if any(word in text for word in ['parle', 'active la voix', 'parle-moi', 'parle moi']):
        tts.voice_enabled = True
        return "Voix activée. Je peux maintenant parler."

    # Arrêter la voix
    if any(word in text for word in ['coupe la voix', 'silence voix', 'desactive voix']):
        tts.voice_enabled = False
        return "Voix désactivée."

    # Si aucune commande spécifique ne correspond, envoyer à l'IA
    return ai.chat(text, history=True)

# ============================================================
# UTILITAIRES
# ============================================================

@app.route('/api/time')
def get_time():
    """Retourne l'heure actuelle"""
    now = datetime.now()
    return jsonify({
        'time': now.strftime('%H:%M:%S'),
        'date': now.strftime('%d/%m/%Y'),
        'timestamp': now.isoformat(),
        'formatted': f"Il est {now.strftime('%H heures %M minutes')}"
    })

@app.route('/api/location')
def get_location():
    """Retourne les informations de localisation"""
    return jsonify({
        'city': 'COTONOU',
        'country': 'BÉNIN',
        'latitude': 6.3654,
        'longitude': 2.4183,
        'altitude': 12,
        'gps_signal': 'FORT'
    })

@app.route('/api/power')
def get_power_stats():
    """Retourne les statistiques de puissance"""
    import random
    return jsonify({
        'power': round(2.8 + random.random() * 0.8, 1),
        'autonomy': '∞',
        'efficiency': round(97 + random.random() * 2.5, 1),
        'temperature': round(2.2 + random.random() * 0.5, 1)
    })

@app.route('/api/tts', methods=['POST'])
def text_to_speech():
    """Convertit du texte en parole"""
    data = request.get_json()
    if not data or 'text' not in data:
        return jsonify({'error': 'Texte requis'}), 400

    success = tts.speak(data['text'])
    return jsonify({
        'status': 'speaking' if success else 'failed',
        'text': data['text']
    })

@app.route('/api/tts/enable', methods=['POST'])
def enable_tts():
    """Active/désactive la synthèse vocale"""
    data = request.get_json() or {}
    if data.get('enabled') is not None:
        tts.voice_enabled = data['enabled']
    else:
        tts.toggle()

    return jsonify({
        'enabled': tts.voice_enabled,
        'volume': tts.volume
    })

@app.route('/api/config', methods=['POST'])
def set_config():
    """Configure la clé API Mistral"""
    data = request.get_json()
    if not data or 'api_key' not in data:
        return jsonify({'error': 'Clé API requise'}), 400

    ai.set_api_key(data['api_key'])

    # Sauvegarder dans le fichier .env
    env_path = os.path.join(os.path.dirname(__file__), '.env')
    # Using set_key from dotenv to manage .env file
    set_key(env_path, "MISTRAL_API_KEY", data['api_key'])

    return jsonify({'status': 'API key configured'})

@app.route('/api/health')
def health_check():
    """Vérification de l'état du serveur"""
    return jsonify({
        'status': 'healthy',
        'version': '7.4.1',
        'engine': 'Mistral AI',
        'voice_enabled': tts.voice_enabled,
        'uptime': monitor.get_uptime_formatted()
    })

# ============================================================
# LANCEMENT
# ============================================================

if __name__ == '__main__':
    print("=" * 60)
    print("J.A.R.V.I.S v7.4.1 - Backend Server")
    print("Just A Rather Very Intelligent System")
    print("=" * 60)
    print("Serveur démarre sur: http://localhost:5000")
    print("Appuyez sur Ctrl+C pour arreter")
    print("=" * 60)

    app.run(
        host='0.0.0.0',
        port=5000,
        debug=True,
        threaded=True
    )
