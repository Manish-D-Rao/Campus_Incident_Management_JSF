document.getElementById('login-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    try {
        const res = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        if (res.status === 200 && res.data.token) {
            setToken(res.data.token);
            localStorage.setItem('user', JSON.stringify({ email: res.data.email, role: res.data.role, name: res.data.name }));
            window.location.href = '/dashboard.html';
        } else {
            showAlert('alert', 'Invalid email or password. Please try again.', 'error');
        }
    } catch (err) {
        showAlert('alert', 'Unable to connect. Please check your internet and try again.', 'error');
    }
});

document.getElementById('logout-btn')?.addEventListener('click', () => {
    clearToken();
    window.location.href = '/login.html';
});
