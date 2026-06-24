const API_BASE_URL = "";

(function () {
  const originalFetch = window.fetch;

  window.fetch = function (input, init = {}) {
    const newInit = {
      ...init,
      credentials: "include",
      headers: {
        ...(init.headers || {})
      }
    };

    return originalFetch(input, newInit);
  };
})();