"""
Module Mistral AI pour JARVIS
Gère l'intégration avec l'API Mistral AI pour le dialogue intelligent
"""
import os
import requests
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

class MistralAI:
    def __init__(self):
        self.api_key = os.getenv('MISTRAL_API_KEY', '')
        self.base_url = 'https://api.mistral.ai/v1'
        self.model = 'mistral-small-latest'
        self.conversation_history = []
        self._init_system_message()

    def _init_system_message(self):
        """Initialise le message système pour JARVIS"""
        now = datetime.now()
        self.conversation_history = [{
            'role': 'system',
            'content': (
                "Tu es J.A.R.V.I.S (Just A Rather Very Intelligent System), "
                "l'assistant personnel intelligent de Tony Stark. "
                "Tu parles français couramment avec un ton professionnel et élégant. "
                "Tu es efficace, poli, et toujours prêt à aider. "
                "Tu donnes des réponses concises mais complètes (2-4 phrases max). "
                "Tu peux aider avec: informations générales, calculs, programmation, "
                "conseils, analyses, traductions, résumé de textes, etc. "
                "Si on te demande quelque chose que tu ne peux pas faire, "
                "dis-le poliment et propose une alternative. "
                f"Nous sommes le {now.strftime('%d/%m/%Y')} et il est {now.strftime('%H:%M')}."
            )
        }]

    def set_api_key(self, api_key):
        """Définit la clé API Mistral"""
        self.api_key = api_key

    def chat(self, user_message, history=True):
        """
        Envoie un message à Mistral AI et retourne la réponse
        
        Args:
            user_message: Le message de l'utilisateur
            history: Si True, utilise l'historique de conversation
            
        Returns:
            str: La réponse de l'IA
        """
        if not self.api_key or self.api_key == 'YOUR_MISTRAL_API_KEY_HERE':
            return (
                "Bonjour. Je suis JARVIS. Ma clé API Mistral n'est pas encore configurée. "
                "Veuillez la définir dans le fichier .env pour activer mes capacités d'IA complètes. "
                "En attendant, je peux exécuter les commandes système."
            )

        try:
            # Ajouter le message utilisateur à l'historique
            if history:
                self.conversation_history.append({
                    'role': 'user',
                    'content': user_message
                })
                messages = self.conversation_history.copy()
            else:
                messages = [
                    self.conversation_history[0],  # System message
                    {'role': 'user', 'content': user_message}
                ]

            # Appeler l'API Mistral
            headers = {
                'Authorization': f'Bearer {self.api_key}',
                'Content-Type': 'application/json'
            }

            payload = {
                'model': self.model,
                'messages': messages,
                'max_tokens': 512,
                'temperature': 0.7,
                'top_p': 0.9
            }

            response = requests.post(
                f'{self.base_url}/chat/completions',
                headers=headers,
                json=payload,
                timeout=30
            )

            if response.status_code == 200:
                data = response.json()
                assistant_message = data['choices'][0]['message']['content']

                # Ajouter la réponse à l'historique
                if history:
                    self.conversation_history.append({
                        'role': 'assistant',
                        'content': assistant_message
                    })

                # Limiter l'historique à 20 messages
                if len(self.conversation_history) > 21:
                    self.conversation_history = [self.conversation_history[0]] + self.conversation_history[-20:]

                return assistant_message
            elif response.status_code == 401:
                return "Erreur: Clé API Mistral invalide. Vérifiez votre configuration."
            elif response.status_code == 429:
                return "Erreur: Quota d'API dépassé. Réessayez dans quelques instants."
            else:
                return f"Erreur API Mistral: {response.status_code} - {response.text}"

        except requests.exceptions.Timeout:
            return "Délai d'attente dépassé. Veuillez réessayer."
        except requests.exceptions.RequestException as e:
            return f"Erreur de connexion: {str(e)}"
        except Exception as e:
            return f"Erreur inattendue: {str(e)}"

    def clear_history(self):
        """Efface l'historique de conversation"""
        self._init_system_message()

    def get_history(self):
        """Retourne l'historique de conversation"""
        return self.conversation_history[1:]  # Exclure le message système
