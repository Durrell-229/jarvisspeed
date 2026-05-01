"""
JARVIS Chart Generator
Génère des graphiques avec matplotlib
"""
import os
import uuid
import base64
import io
import matplotlib
matplotlib.use('Agg')  # Backend non-GUI
import matplotlib.pyplot as plt
import matplotlib

CHART_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads', 'charts')
os.makedirs(CHART_FOLDER, exist_ok=True)

def generate_chart(data: dict, chart_type: str = 'bar', title: str = 'Graphique') -> dict:
    """
    Génère un graphique et retourne l'image en base64

    Args:
        data: {'labels': [...], 'values': [...]}
        chart_type: 'bar', 'line', 'pie', 'scatter', 'area'
        title: Titre du graphique

    Returns:
        dict: {'success': bool, 'image_base64': str, 'error': str|None}
    """
    try:
        labels = data.get('labels', [])
        values = data.get('values', [])

        if not labels or not values:
            return {'success': False, 'image_base64': None, 'error': 'Données manquantes'}

        if len(labels) != len(values):
            return {'success': False, 'image_base64': None, 'error': 'Labels et valeurs doivent avoir la même longueur'}

        # Créer le graphique
        fig, ax = plt.subplots(figsize=(10, 6))

        # Style moderne
        plt.style.use('dark_background')

        colors = ['#10A37A', '#00D4FF', '#FF6B2B', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899']

        if chart_type == 'bar':
            bars = ax.bar(labels, values, color=colors[:len(labels)])
            ax.set_ylabel('Valeurs')
            # Ajouter les valeurs sur les barres
            for bar, val in zip(bars, values):
                ax.text(bar.get_x() + bar.get_width()/2., bar.get_height() + max(values)*0.01,
                       f'{val}', ha='center', va='bottom', fontsize=10, color='white')

        elif chart_type == 'line':
            ax.plot(labels, values, color='#10A37A', marker='o', linewidth=2, markersize=8)
            ax.set_ylabel('Valeurs')
            ax.grid(True, alpha=0.3)

        elif chart_type == 'pie':
            ax.pie(values, labels=labels, autopct='%1.1f%%', colors=colors[:len(labels)],
                   startangle=90, textprops={'color': 'white'})

        elif chart_type == 'scatter':
            ax.scatter(labels, values, color='#10A37A', s=100, alpha=0.7)
            ax.set_ylabel('Valeurs')
            ax.grid(True, alpha=0.3)

        elif chart_type == 'area':
            ax.fill_between(range(len(values)), values, alpha=0.3, color='#10A37A')
            ax.plot(range(len(values)), values, color='#10A37A', linewidth=2)
            ax.set_ylabel('Valeurs')
            ax.grid(True, alpha=0.3)

        ax.set_title(title, fontsize=14, fontweight='bold', color='white', pad=20)
        plt.xticks(rotation=45, ha='right', color='white')
        plt.yticks(color='white')

        # Sauvegarder
        chart_id = str(uuid.uuid4())[:8]
        filepath = os.path.join(CHART_FOLDER, f"{chart_id}.png")

        buf = io.BytesIO()
        plt.savefig(buf, format='png', bbox_inches='tight', dpi=150)
        buf.seek(0)
        image_base64 = base64.b64encode(buf.read()).decode('utf-8')

        # Sauvegarder aussi en fichier
        with open(filepath, 'wb') as f:
            f.write(buf.getvalue())

        plt.close(fig)

        return {
            'success': True,
            'image_base64': image_base64,
            'chart_id': chart_id,
            'filepath': filepath,
            'error': None
        }

    except Exception as e:
        plt.close('all')
        return {'success': False, 'image_base64': None, 'error': f"Erreur génération graphique: {str(e)}"}
