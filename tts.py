"""
Module de synthèse vocale pour JARVIS
Permet à JARVIS de parler à haute voix
"""
import os
import sys
import subprocess
import tempfile
from datetime import datetime

class TextToSpeech:
    def __init__(self):
        self.voice_enabled = True
        self.volume = 100
        self._platform = sys.platform

    def speak(self, text):
        """
        Fait parler JARVIS avec le texte fourni
        Utilise différentes méthodes selon la plateforme
        """
        if not self.voice_enabled:
            return False

        # Nettoyer le texte pour la synthèse vocale
        text = self._clean_text(text)

        try:
            if self._platform == 'win32':
                return self._speak_windows(text)
            elif self._platform == 'darwin':
                return self._speak_macos(text)
            else:
                return self._speak_linux(text)
        except Exception as e:
            print(f"Erreur TTS: {e}")
            return False

    def _clean_text(self, text):
        """Nettoie le texte pour une meilleure synthèse vocale"""
        # Supprimer les caractères spéciaux problématiques
        text = text.replace('*', '').replace('_', '').replace('#', '')
        # Limiter la longueur
        if len(text) > 400:
            text = text[:397] + '...'
        return text

    def _speak_windows(self, text):
        """Synthèse vocale sur Windows avec PowerShell"""
        # Échapper les caractères spéciaux pour PowerShell
        escaped_text = text.replace("'", "''").replace('"', '`"')

        script = f"""
        Add-Type -AssemblyName System.Speech
        $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
        $synth.Rate = 0
        $synth.Volume = {self.volume}
        $synth.Speak('{escaped_text}')
        $synth.Dispose()
        """

        try:
            subprocess.run(
                ['powershell', '-Command', script],
                capture_output=True,
                timeout=30
            )
            return True
        except subprocess.TimeoutExpired:
            return False
        except Exception:
            return False

    def _speak_macos(self, text):
        """Synthèse vocale sur macOS avec say"""
        try:
            subprocess.run(
                ['say', '-v', 'Alex', text],
                timeout=30
            )
            return True
        except subprocess.TimeoutExpired:
            return False
        except Exception:
            return False

    def _speak_linux(self, text):
        """Synthèse vocale sur Linux avec espeak ou festival"""
        try:
            # Essayer espeak d'abord
            subprocess.run(
                ['espeak', '-v', 'fr', '-s', '150', text],
                timeout=30
            )
            return True
        except FileNotFoundError:
            try:
                # Essayer festival
                subprocess.run(
                    ['festival', '--tts'],
                    input=text,
                    text=True,
                    timeout=30
                )
                return True
            except FileNotFoundError:
                print("Aucun TTS Linux disponible (installez espeak)")
                return False
        except subprocess.TimeoutExpired:
            return False
        except Exception:
            return False

    def set_volume(self, volume):
        """Définit le volume (0-100)"""
        self.volume = max(0, min(100, volume))

    def toggle(self):
        """Active/désactive la voix"""
        self.voice_enabled = not self.voice_enabled
        return self.voice_enabled
