// JARVIS - Code Editor & Execution
const API_BASE = window.location.origin;

function renderCodeBlocks(message) {
    // Detect code blocks: ```language\ncode\n```
    const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g;
    let result = message;
    let match;
    let codeBlocks = [];

    while ((match = codeBlockRegex.exec(message)) !== null) {
        const lang = match[1] || 'text';
        const code = match[2].trim();
        const blockId = 'code-' + Math.random().toString(36).substr(2, 9);

        const html = createCodeBlockHTML(blockId, lang, code);
        result = result.replace(match[0], `<div id="${blockId}">${html}</div>`);
    }

    return result;
}

function createCodeBlockHTML(blockId, lang, code) {
    const escapedCode = escapeHTML(code);
    const showExecute = ['python', 'python3'].includes(lang.toLowerCase());
    const showPreview = ['html', 'htm'].includes(lang.toLowerCase());

    return `
        <div class="code-block">
            <div class="code-header">
                <span class="code-language">${lang}</span>
                <div class="code-actions">
                    <button class="code-btn copy" onclick="copyCode('${blockId}')">Copier</button>
                    ${showExecute ? `<button class="code-btn execute" onclick="executeCode('${blockId}')">Exécuter</button>` : ''}
                    ${showPreview ? `<button class="code-btn preview" onclick="previewHTML('${blockId}')">Aperçu</button>` : ''}
                </div>
            </div>
            <div class="code-content" id="${blockId}-code">${escapedCode}</div>
            <div class="code-output" id="${blockId}-output" style="display:none;"></div>
            <iframe class="code-preview" id="${blockId}-preview" style="display:none;"></iframe>
        </div>
    `;
}

function escapeHTML(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function copyCode(blockId) {
    const codeEl = document.getElementById(`${blockId}-code`);
    const text = codeEl.textContent || codeEl.innerText;

    try {
        await navigator.clipboard.writeText(text);
        const btn = document.querySelector(`#${blockId} .code-btn.copy`);
        btn.textContent = 'Copié!';
        setTimeout(() => btn.textContent = 'Copier', 2000);
    } catch (err) {
        // Fallback
        const textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }
}

async function executeCode(blockId) {
    const codeEl = document.getElementById(`${blockId}-code`);
    const code = codeEl.textContent || codeEl.innerText;
    const outputEl = document.getElementById(`${blockId}-output`);

    outputEl.style.display = 'block';
    outputEl.textContent = 'Exécution en cours...';
    outputEl.className = 'code-output';

    try {
        const response = await fetch(`${API_BASE}/api/code/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code })
        });

        const result = await response.json();

        if (result.success) {
            outputEl.textContent = result.output;
        } else {
            outputEl.textContent = `Erreur:\n${result.error}`;
            outputEl.className = 'code-output error';
        }
    } catch (err) {
        outputEl.textContent = `Erreur de connexion: ${err.message}`;
        outputEl.className = 'code-output error';
    }
}

function previewHTML(blockId) {
    const codeEl = document.getElementById(`${blockId}-code`);
    const code = codeEl.textContent || codeEl.innerText;
    const previewEl = document.getElementById(`${blockId}-preview`);

    previewEl.style.display = 'block';
    previewEl.srcdoc = code;
}
