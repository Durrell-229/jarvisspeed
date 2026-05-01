// JARVIS - File Upload & Analysis
const API_BASE = window.location.origin;
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

function initFileUpload() {
    const uploadZone = document.getElementById('uploadZone');
    const fileInput = document.getElementById('fileInput');

    if (!uploadZone || !fileInput) return;

    // Click to upload
    uploadZone.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFile(e.target.files[0]);
        }
    });

    // Drag & drop
    uploadZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadZone.classList.add('dragover');
    });

    uploadZone.addEventListener('dragleave', () => {
        uploadZone.classList.remove('dragover');
    });

    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            handleFile(e.dataTransfer.files[0]);
        }
    });
}

async function handleFile(file) {
    if (file.size > MAX_FILE_SIZE) {
        addChatLine('jarvis', `Fichier trop volumineux (${(file.size / 1024 / 1024).toFixed(1)} MB). Maximum: 10 MB.`);
        return;
    }

    // Show upload in chat
    const fileId = 'upload-' + Date.now();
    addChatLine('user', `📎 Fichier: ${file.name} (${formatSize(file.size)})`);

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch(`${API_BASE}/api/upload`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.error) {
            addChatLine('jarvis', `Erreur upload: ${result.error}`);
            return;
        }

        // File uploaded successfully
        const fileData = result.file;
        const content = result.content;

        // Send content to AI for analysis
        let analysisPrompt = '';
        if (typeof content === 'string') {
            analysisPrompt = `J'ai uploadé un fichier "${fileData.name}" (${fileData.type}). Voici le contenu:\n\n${content.substring(0, 3000)}\n\nAnalyse ce contenu et donne-moi un résumé.`;
        } else if (content && content.type === 'csv') {
            analysisPrompt = `J'ai uploadé un fichier CSV "${fileData.name}" avec ${content.total_rows} lignes et ${content.columns} colonnes (${content.header.join(', ')}). Analyse ces données et donne-moi des insights.`;
        } else if (content && content.type === 'image') {
            analysisPrompt = `J'ai uploadé une image "${fileData.name}". Décrivez ce que vous voyez.`;
        }

        if (analysisPrompt) {
            addChatLine('jarvis', `📄 Fichier "${fileData.name}" reçu. Analyse en cours...`);
            sendMessage(analysisPrompt);
        }

    } catch (err) {
        addChatLine('jarvis', `Erreur upload: ${err.message}`);
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
}

async function analyzeFile(fileId) {
    try {
        const response = await fetch(`${API_BASE}/api/analyze`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ file_id: fileId })
        });

        const result = await response.json();
        return result;
    } catch (err) {
        console.error('Analysis error:', err);
        return null;
    }
}
