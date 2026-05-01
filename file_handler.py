"""
JARVIS File Handler
Gère l'upload, le stockage, l'extraction et l'analyse de fichiers
"""
import os
import json
import uuid
from datetime import datetime
from werkzeug.utils import secure_filename

UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

ALLOWED_EXTENSIONS = {
    'txt', 'pdf', 'csv', 'json', 'md', 'py', 'js', 'html', 'css',
    'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp',
    'doc', 'docx', 'xls', 'xlsx'
}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def save_file(file):
    """Sauvegarde un fichier uploadé"""
    if not file or not file.filename:
        return None, "Aucun fichier fourni"

    if not allowed_file(file.filename):
        return None, f"Type de fichier non autorisé. Types acceptés: {', '.join(ALLOWED_EXTENSIONS)}"

    original_name = secure_filename(file.filename)
    file_id = str(uuid.uuid4())[:8]
    ext = original_name.rsplit('.', 1)[1].lower()
    stored_name = f"{file_id}_{original_name}"

    filepath = os.path.join(UPLOAD_FOLDER, stored_name)
    file.save(filepath)

    file_info = {
        'id': file_id,
        'original_name': original_name,
        'stored_name': stored_name,
        'extension': ext,
        'size': os.path.getsize(filepath),
        'uploaded_at': datetime.now().isoformat(),
        'path': filepath
    }

    return file_info, None

def extract_text_from_file(file_info):
    """Extrait le texte d'un fichier selon son type"""
    filepath = file_info['path']
    ext = file_info['extension']

    try:
        if ext in ('txt', 'py', 'js', 'html', 'css', 'md', 'json'):
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                return f.read(), None

        elif ext == 'csv':
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
                header = lines[0].strip().split(',') if lines else []
                rows = [line.strip().split(',') for line in lines[1:10]]
                return {
                    'type': 'csv',
                    'header': header,
                    'rows': rows,
                    'total_rows': len(lines) - 1,
                    'columns': len(header),
                    'preview': '\n'.join([','.join(r) for r in rows])
                }, None

        elif ext == 'pdf':
            try:
                import PyPDF2
                text = ""
                with open(filepath, 'rb') as f:
                    reader = PyPDF2.PdfReader(f)
                    for page in reader.pages[:10]:  # Max 10 pages
                        text += page.extract_text() or ""
                return text, None
            except ImportError:
                return None, "PyPDF2 non installé. pip install PyPDF2"

        elif ext in ('png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'):
            return {
                'type': 'image',
                'format': ext,
                'size': file_info['size'],
                'path': filepath,
                'url': f"/api/files/{file_info['id']}/download"
            }, None

        else:
            return None, f"Extraction non supportée pour le type .{ext}"

    except Exception as e:
        return None, f"Erreur lors de l'extraction: {str(e)}"

def analyze_csv_data(file_info):
    """Analyse un fichier CSV et retourne des statistiques"""
    filepath = file_info['path']

    try:
        import pandas as pd
        df = pd.read_csv(filepath)

        stats = {
            'type': 'csv_analysis',
            'rows': len(df),
            'columns': len(df.columns),
            'column_names': list(df.columns),
            'dtypes': {col: str(df[col].dtype) for col in df.columns},
            'numeric_stats': {},
            'missing_values': {col: int(df[col].isna().sum()) for col in df.columns}
        }

        # Stats pour colonnes numériques
        for col in df.select_dtypes(include=['number']).columns:
            stats['numeric_stats'][col] = {
                'mean': round(float(df[col].mean()), 2),
                'median': round(float(df[col].median()), 2),
                'min': round(float(df[col].min()), 2),
                'max': round(float(df[col].max()), 2),
                'std': round(float(df[col].std()), 2)
            }

        return stats, None

    except ImportError:
        return None, "pandas non installé. pip install pandas"
    except Exception as e:
        return None, f"Erreur analyse CSV: {str(e)}"

def get_file_info(file_id):
    """Retourne les infos d'un fichier par son ID"""
    for filename in os.listdir(UPLOAD_FOLDER):
        if filename.startswith(file_id):
            filepath = os.path.join(UPLOAD_FOLDER, filename)
            original_name = filename.split('_', 1)[1] if '_' in filename else filename
            ext = original_name.rsplit('.', 1)[1].lower() if '.' in original_name else ''
            return {
                'id': file_id,
                'original_name': original_name,
                'stored_name': filename,
                'extension': ext,
                'size': os.path.getsize(filepath),
                'path': filepath
            }
    return None

def list_files():
    """Liste tous les fichiers uploadés"""
    files = []
    for filename in os.listdir(UPLOAD_FOLDER):
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        if os.path.isfile(filepath):
            parts = filename.split('_', 1)
            file_id = parts[0]
            original_name = parts[1] if len(parts) > 1 else filename
            ext = original_name.rsplit('.', 1)[1].lower() if '.' in original_name else ''
            files.append({
                'id': file_id,
                'original_name': original_name,
                'extension': ext,
                'size': os.path.getsize(filepath),
                'uploaded_at': datetime.fromtimestamp(os.path.getctime(filepath)).isoformat()
            })
    return sorted(files, key=lambda x: x['uploaded_at'], reverse=True)

def delete_file(file_id):
    """Supprime un fichier"""
    for filename in os.listdir(UPLOAD_FOLDER):
        if filename.startswith(file_id):
            filepath = os.path.join(UPLOAD_FOLDER, filename)
            os.remove(filepath)
            return True
    return False
