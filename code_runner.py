"""
JARVIS Code Runner
Exécution sécurisée de code Python (sandbox)
"""
import sys
import io
import traceback
import re
from contextlib import redirect_stdout, redirect_stderr

def execute_python_code(code: str, timeout: int = 10) -> dict:
    """
    Exécute du code Python de manière sécurisée et retourne le résultat

    Args:
        code: Le code Python à exécuter
        timeout: Timeout en secondes (non implémenté nativement)

    Returns:
        dict: {'success': bool, 'output': str, 'error': str|None}
    """
    # Nettoyage du code
    code = code.strip()

    # Vérifications de sécurité - blocage des opérations dangereuses
    dangerous_patterns = [
        r'__import__', r'importlib', r'subprocess', r'os\.system',
        r'os\.popen', r'os\.exec', r'os\.fork', r'shutil',
        r'sys\.exit', r'quit\(\)', r'exit\(\)',
        r'open\(.*["\'](/|C:\\|D:\\)',  # Pas d'accès système direct
        r'eval\(', r'exec\(', r'compile\(',
        r'globals\(\)', r'locals\(\)', r'vars\(\)',
        r'getattr\(', r'setattr\(', r'delattr\(',
        r'input\(', r'raw_input\(',
    ]

    for pattern in dangerous_patterns:
        if re.search(pattern, code):
            return {
                'success': False,
                'output': '',
                'error': f"Exécution bloquée: opération non autorisée détectée ({pattern})"
            }

    # Capture stdout et stderr
    stdout_buffer = io.StringIO()
    stderr_buffer = io.StringIO()

    # Namespace limité
    safe_globals = {
        '__builtins__': {
            # Fonctions autorisées
            'abs': abs, 'all': all, 'any': any, 'bin': bin, 'bool': bool,
            'bytearray': bytearray, 'bytes': bytes, 'callable': callable,
            'chr': chr, 'complex': complex, 'dict': dict, 'dir': dir,
            'divmod': divmod, 'enumerate': enumerate, 'filter': filter,
            'float': float, 'format': format, 'frozenset': frozenset,
            'hash': hash, 'hex': hex, 'id': id, 'int': int, 'isinstance': isinstance,
            'issubclass': issubclass, 'iter': iter, 'len': len, 'list': list,
            'map': map, 'max': max, 'min': min, 'next': next, 'object': object,
            'oct': oct, 'ord': ord, 'pow': pow, 'print': print, 'range': range,
            'repr': repr, 'reversed': reversed, 'round': round, 'set': set,
            'slice': slice, 'sorted': sorted, 'str': str, 'sum': sum,
            'super': super, 'tuple': tuple, 'type': type, 'zip': zip,
            # Modules mathématiques
            'True': True, 'False': False, 'None': None,
            'ValueError': ValueError, 'TypeError': TypeError,
            'KeyError': KeyError, 'IndexError': IndexError,
        }
    }

    try:
        # Essayer d'importer des modules utiles
        allowed_imports = ['math', 'random', 'datetime', 'collections', 'itertools', 'functools', 'statistics', 'json', 're', 'string', 'time']

        # Pré-charger les imports autorisés
        for mod_name in allowed_imports:
            try:
                mod = __import__(mod_name)
                safe_globals['__builtins__'][mod_name] = mod
            except ImportError:
                pass

        with redirect_stdout(stdout_buffer), redirect_stderr(stderr_buffer):
            exec(code, safe_globals)

        stdout_output = stdout_buffer.getvalue()
        stderr_output = stderr_buffer.getvalue()

        if stderr_output:
            return {
                'success': False,
                'output': stdout_output,
                'error': stderr_output
            }

        return {
            'success': True,
            'output': stdout_output if stdout_output else "Code exécuté avec succès (aucune sortie).",
            'error': None
        }

    except Exception as e:
        error_msg = stderr_buffer.getvalue() or traceback.format_exc() or str(e)
        return {
            'success': False,
            'output': stdout_buffer.getvalue(),
            'error': error_msg
        }
