"""
Module Mistral AI pour JARVIS
Gère l'intégration avec l'API Mistral AI pour le dialogue intelligent
"""
import os
import json
import requests
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

class MistralAI:
    def __init__(self):
        self.api_key = os.getenv('MISTRAL_API_KEY', '')
        self.base_url = 'https://api.mistral.ai/v1'
        self.model = 'mistral-large-latest'
        self.conversation_history = []
        self._init_system_message()

    def _init_system_message(self):
        """Initialise le message système pour JARVIS"""
        now = datetime.now()
        self.conversation_history = [{
            'role': 'system',
            'content': (
                "Tu es J.A.R.V.I.S (Just A Rather Very Intelligent System), "
                "l'assistant personnel intelligent. "
                "Tu parles français couramment avec un ton professionnel et élégant. "
                "Tu es efficace, poli, et toujours prêt à aider. "
                "Tu donnes des réponses concises mais complètes (2-4 phrases max). "
                "Tu as accès à des outils pour contrôler le système de l'utilisateur: "
                "lancer des applications, contrôler le volume et les médias, gérer des fichiers, "
                "ouvrir des sites web, faire des recherches web, et contrôler le système. "
                "Quand l'utilisateur demande une action, utilise l'outil approprié. "
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

    # ============================================================
    # TOOL CALLING (Fonction calling)
    # ============================================================

    # Définition des outils pour l'API Mistral
    TOOLS = [
        {
            "type": "function",
            "function": {
                "name": "launch_app",
                "description": "Ouvre une application installée sur l'ordinateur. Utilise le nom exact de l'application.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "app_name": {
                            "type": "string",
                            "description": "Nom de l'application: chrome, spotify, vscode, notepad, calculator, calc, explorer, discord, teams, slack, firefox, edge, vlc, word, excel, powershell, cmd, terminal, whatsapp, paramètres, task manager",
                            "enum": ["chrome", "spotify", "vscode", "visual studio code", "notepad", "calculator", "calc", "explorer", "discord", "teams", "slack", "firefox", "edge", "vlc", "word", "excel", "powershell", "cmd", "terminal", "whatsapp", "paramètres", "task manager", "bloc-notes", "calculatrice", "explorateur", "windows terminal", "invite de commandes", "google chrome", "brave", "groove", "windows media player", "spotify music", "code", "fichiers"]
                        }
                    },
                    "required": ["app_name"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "volume_control",
                "description": "Contrôle le volume sonore du système. Permet de monter, descendre, couper ou définir le volume.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "action": {
                            "type": "string",
                            "description": "Action à effectuer: up (monter), down (descendre/baisser/réduire), mute (couper), set (définir un niveau)",
                            "enum": ["up", "down", "mute", "set", "monter", "descendre", "baisser", "réduire", "couper", "définir", "mettre"]
                        },
                        "level": {
                            "type": "integer",
                            "description": "Niveau de volume en pourcentage (0-100). Requis uniquement pour l'action 'set'."
                        }
                    },
                    "required": ["action"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "media_control",
                "description": "Contrôle la lecture des médias (musique, vidéo). Play, pause, suivant, précédent, stop.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "action": {
                            "type": "string",
                            "description": "Action média: play (jouer), pause, next/suivant (prochain), previous/précédent (précédent), stop",
                            "enum": ["play", "pause", "toggle", "next", "suivant", "previous", "précédent", "stop"]
                        }
                    },
                    "required": ["action"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "file_manager",
                "description": "Gère les fichiers et dossiers: lister le contenu, créer des fichiers/dossiers, ouvrir un fichier, chercher un fichier.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "action": {
                            "type": "string",
                            "description": "Action: list (lister le bureau), create (créer un fichier/dossier), open (ouvrir un fichier), search (chercher un fichier)",
                            "enum": ["list", "create", "open", "search", "lister", "créer", "ouvrir", "chercher"]
                        },
                        "path": {
                            "type": "string",
                            "description": "Nom du fichier, dossier ou terme de recherche. Pour 'list', peut être omis pour lister le bureau."
                        }
                    },
                    "required": ["action"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "open_website",
                "description": "Ouvre un site web dans le navigateur par défaut.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "url": {
                            "type": "string",
                            "description": "URL du site web à ouvrir (ex: google.com, youtube.com, wikipedia.org)"
                        }
                    },
                    "required": ["url"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "web_search",
                "description": "Effectue une recherche sur le web (Google, Bing, Wikipedia, YouTube).",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "Terme de recherche"
                        },
                        "engine": {
                            "type": "string",
                            "description": "Moteur de recherche: google, bing, wikipedia, youtube, duckduckgo",
                            "enum": ["google", "bing", "wikipedia", "youtube", "duckduckgo"]
                        }
                    },
                    "required": ["query"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "generate_code",
                "description": "Génère du code dans un langage spécifique (Python, JavaScript, HTML, CSS, etc.). Retourne le code formaté.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "language": {
                            "type": "string",
                            "description": "Langage de programmation: python, javascript, html, css, json, markdown, typescript, java, c, cpp, rust, go, php, sql, bash, powershell",
                            "enum": ["python", "javascript", "html", "css", "json", "markdown", "typescript", "java", "c", "cpp", "rust", "go", "php", "sql", "bash", "powershell"]
                        },
                        "description": {
                            "type": "string",
                            "description": "Description de ce que le code doit faire"
                        }
                    },
                    "required": ["language", "description"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "analyze_file",
                "description": "Analyse le contenu d'un fichier (code, données, texte). Peut lire, expliquer et suggérer des améliorations.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "file_type": {
                            "type": "string",
                            "description": "Type du fichier: python, javascript, csv, json, text, html, css, markdown, image"
                        },
                        "content": {
                            "type": "string",
                            "description": "Contenu du fichier ou description"
                        },
                        "task": {
                            "type": "string",
                            "description": "Tâche à effectuer: explain, review, improve, debug, summarize, translate"
                        }
                    },
                    "required": ["file_type", "content", "task"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "execute_python",
                "description": "Exécute du code Python et retourne le résultat. Utile pour les calculs complexes, l'analyse de données, etc.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "code": {
                            "type": "string",
                            "description": "Le code Python à exécuter"
                        }
                    },
                    "required": ["code"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "create_chart",
                "description": "Crée un graphique à partir de données. Supporte bar, line, pie, scatter, area.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "chart_type": {
                            "type": "string",
                            "description": "Type de graphique: bar, line, pie, scatter, area",
                            "enum": ["bar", "line", "pie", "scatter", "area"]
                        },
                        "title": {
                            "type": "string",
                            "description": "Titre du graphique"
                        },
                        "labels": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "Labels pour l'axe X ou les catégories"
                        },
                        "values": {
                            "type": "array",
                            "items": {"type": "number"},
                            "description": "Valeurs numériques"
                        }
                    },
                    "required": ["chart_type", "title", "labels", "values"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "read_document",
                "description": "Lit et analyse un document (PDF, texte long). Peut résumer, extraire des informations, ou répondre à des questions.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "content": {
                            "type": "string",
                            "description": "Contenu du document"
                        },
                        "task": {
                            "type": "string",
                            "description": "Tâche: summarize, extract, answer, analyze"
                        },
                        "question": {
                            "type": "string",
                            "description": "Question spécifique sur le document (optionnel)"
                        }
                    },
                    "required": ["content", "task"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "summarize_text",
                "description": "Résume un texte long en points clés concis.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "text": {
                            "type": "string",
                            "description": "Le texte à résumer"
                        },
                        "max_length": {
                            "type": "string",
                            "description": "Longueur maximale du résumé: short, medium, long"
                        }
                    },
                    "required": ["text"]
                }
            }
        },
    ]

    def chat_with_tools(self, user_message, history=True, execute_fn=None):
        """
        Appelle Mistral AI avec le tool calling et exécute les actions retournées.

        Args:
            user_message: Le message de l'utilisateur
            history: Si True, utilise l'historique de conversation
            execute_fn: Fonction pour exécuter les actions. Signature: execute_fn(action_name, **kwargs) -> dict

        Returns:
            dict: {"response": str, "action_executed": str|None, "action_result": dict|None, "type": str}
        """
        if not self.api_key or self.api_key == 'YOUR_MISTRAL_API_KEY_HERE':
            return {
                "response": (
                    "Bonjour. Je suis JARVIS. Ma clé API Mistral n'est pas encore configurée. "
                    "Veuillez la définir via la commande /config pour activer mes capacités complètes. "
                    "En attendant, je peux exécuter les commandes système de base: heure, date, calcul, statut système."
                ),
                "action_executed": None,
                "action_result": None,
                "type": "no_api"
            }

        try:
            # Préparer les messages
            if history:
                self.conversation_history.append({
                    'role': 'user',
                    'content': user_message
                })
                messages = self.conversation_history.copy()
            else:
                messages = [
                    self.conversation_history[0],
                    {'role': 'user', 'content': user_message}
                ]

            # Appeler l'API avec les outils
            headers = {
                'Authorization': f'Bearer {self.api_key}',
                'Content-Type': 'application/json'
            }

            payload = {
                'model': self.model,
                'messages': messages,
                'tools': self.TOOLS,
                'tool_choice': 'auto',
                'max_tokens': 1024,
                'temperature': 0.7,
                'top_p': 0.9
            }

            response = requests.post(
                f'{self.base_url}/chat/completions',
                headers=headers,
                json=payload,
                timeout=30
            )

            if response.status_code != 200:
                # Fallback: réponse textuelle sans outils
                return self._fallback_chat(user_message, history)

            data = response.json()
            assistant_msg = data['choices'][0]['message']

            # Vérifier s'il y a des tool_calls
            tool_calls = assistant_msg.get('tool_calls')

            if tool_calls and execute_fn:
                # Exécuter les tool calls
                results = []
                for tc in tool_calls:
                    func_name = tc['function']['name']
                    try:
                        args = json.loads(tc['function']['arguments'])
                    except json.JSONDecodeError:
                        args = {}

                    # Mapper les noms de fonctions
                    result = execute_fn(func_name, **args)
                    results.append({
                        "action": func_name,
                        "args": args,
                        "result": result
                    })

                # Ajouter le message assistant et les résultats à l'historique
                if history:
                    self.conversation_history.append(assistant_msg)

                    # Ajouter les résultats des tool calls
                    for i, tc in enumerate(tool_calls):
                        result_text = results[i]['result'].get('message', 'Action exécutée.')
                        self.conversation_history.append({
                            'role': 'tool',
                            'tool_call_id': tc['id'],
                            'content': result_text
                        })

                # Construire la réponse finale
                response_text = ""
                action_executed = None
                for r in results:
                    msg = r['result'].get('message', '')
                    response_text += msg + " "
                    action_executed = r['action']

                # Si execute_fn est fourni, on peut demander à Mistral de formater une réponse naturelle
                # Mais pour garder simple, on retourne directement le message de l'action
                if history:
                    self.conversation_history.append({
                        'role': 'assistant',
                        'content': response_text.strip()
                    })

                return {
                    "response": response_text.strip(),
                    "action_executed": action_executed,
                    "action_result": results,
                    "type": "tool_call"
                }

            # Pas de tool_call, réponse texte normale
            assistant_content = assistant_msg.get('content', '')
            if history:
                self.conversation_history.append(assistant_msg)

            # Limiter l'historique
            if len(self.conversation_history) > 21:
                self.conversation_history = [self.conversation_history[0]] + self.conversation_history[-20:]

            return {
                "response": assistant_content,
                "action_executed": None,
                "action_result": None,
                "type": "ai"
            }

        except requests.exceptions.Timeout:
            return self._fallback_chat(user_message, history)
        except requests.exceptions.RequestException:
            return self._fallback_chat(user_message, history)
        except Exception as e:
            return self._fallback_chat(user_message, history)

    def _fallback_chat(self, user_message, history):
        """Fallback vers le chat normal sans outils en cas d'erreur"""
        return {
            "response": self.chat(user_message, history),
            "action_executed": None,
            "action_result": None,
            "type": "ai"
        }
