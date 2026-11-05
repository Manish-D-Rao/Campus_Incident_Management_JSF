const API_BASE = '/api';

function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
}

function clearToken() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}

function getAuthHeaders() {
    const token = getToken();
    return token ? { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
}

// Convert technical errors into friendly user messages
function friendlyError(status, backendMessage) {
    if (!backendMessage) backendMessage = '';

    const msg = backendMessage.toLowerCase();

    // Network / connection issues (handled in catch blocks)
    if (status === 0) {
        return 'Unable to connect. Please check your internet and try again.';
    }

    // Authentication
    if (status === 401) {
        return 'Your session has expired. Please log in again.';
    }
    if (status === 403) {
        return backendMessage || 'You do not have permission to do this.';
    }

    // Registration errors
    if (msg.includes('email already registered') || msg.includes('email already exists')) {
        return 'This email is already registered. Please use a different email or log in.';
    }
    if (msg.includes('user not found')) {
        return 'Account not found. Please check your email or register.';
    }
    if (msg.includes('invalid') || msg.includes('credentials') || msg.includes('password')) {
        return 'Invalid email or password. Please try again.';
    }

    // Not found
    if (status === 404) {
        return 'The item you are looking for could not be found.';
    }

    // Server error
    if (status >= 500) {
        return 'Something went wrong on our end. Please try again later.';
    }

    // Generic fallback
    return 'Something went wrong. Please try again.';
}

async function apiRequest(endpoint, options = {}) {
    const headers = { ...getAuthHeaders(), ...options.headers };
    const res = await fetch(API_BASE + endpoint, { ...options, headers });

    // A 401 means the session is missing or expired. A 403 means the user is
    // signed in but lacks permission, so return it to the form for feedback.
    const isLoginRequest = endpoint === '/auth/login';
    if (!isLoginRequest && res.status === 401) {
        clearToken();
        window.location.href = '/login.html';
        throw new Error('Session expired');
    }

    const text = await res.text();
    try {
        return { status: res.status, data: text ? JSON.parse(text) : null };
    } catch (e) {
        return { status: res.status, data: text };
    }
}

async function handleResponse(res) {
    if (res.status === 401 || res.status === 403) {
        clearToken();
        window.location.href = '/login.html';
        throw new Error('Unauthorized');
    }
    const text = await res.text();
    try {
        return { status: res.status, data: text ? JSON.parse(text) : null };
    } catch (e) {
        return { status: res.status, data: text };
    }
}

const alertTimers = new Map();

function showAlert(containerId, message, type) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const existingTimer = alertTimers.get(containerId);
    if (existingTimer) {
        clearTimeout(existingTimer);
        alertTimers.delete(containerId);
    }
    container.innerHTML = '<div class="alert alert-' + type + '">' + escapeHtml(message) + '</div>';
    if (type !== 'error') {
        const timer = setTimeout(() => {
            container.innerHTML = '';
            alertTimers.delete(containerId);
        }, 5000);
        alertTimers.set(containerId, timer);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(isoString) {
    if (!isoString) return '-';
    return new Date(isoString).toLocaleString();
}

function getBadgeClass(severity, status) {
    let s = 'badge-' + (severity || '').toLowerCase();
    let st = 'badge-' + (status || '').toLowerCase().replace(' ', '_');
    return s + ' ' + st;
}
