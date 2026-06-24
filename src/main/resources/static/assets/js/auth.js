async function authCheck() {
  try {
    const res = await fetch('/api/auth/me', {
      method: 'GET',
      credentials: 'include'
    });

    const data = await res.json();

    if (!data.success) {
      location.href = '/login.html';
      return;
    }

    window.LOGIN_USER = data;
  } catch (e) {
    location.href = '/login.html';
  }
}

async function logout() {
  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include'
    });
  } catch (e) {
  }
  location.href = '/login.html';
}