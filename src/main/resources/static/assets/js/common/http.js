// TEST/www/assets/js/common/http.js
// window.http.getJson / postJson / putJson / del 제공

(function () {
  function baseUrl() {
    // ✅ 1순위: window.API_BASE_URL (window로 선언한 경우)
    // ✅ 2순위: 전역 const API_BASE_URL (config.js에 const로 선언한 경우)
    const v =
      (typeof window.API_BASE_URL === "string" && window.API_BASE_URL) ||
      (typeof API_BASE_URL === "string" && API_BASE_URL) ||
      "";

    return String(v).replace(/\/$/, "");
  }

  function buildUrl(path) {
    if (!path) return baseUrl();
    if (/^https?:\/\//i.test(path)) return path;

    const p = path.startsWith("/") ? path : "/" + path;
    return baseUrl() + p;
  }

  async function mustOk(res) {
    if (res.ok) return res;

    let msg = `HTTP ${res.status}`;
    try {
      const ct = res.headers.get("content-type") || "";
      if (ct.includes("application/json")) {
        const j = await res.json();
        msg = j?.message || j?.error || JSON.stringify(j);
      } else {
        const t = await res.text();
        if (t) msg = t;
      }
    } catch (_) {}
    throw new Error(msg);
  }

  function defaultHeaders(extra) {
    return Object.assign({ "Content-Type": "application/json" }, extra || {});
  }

  window.http = {
    async getJson(path) {
      const url = buildUrl(path);
      console.log("[http.js] GET", url);
      const res = await fetch(url, { method: "GET", headers: defaultHeaders() });
      await mustOk(res);
      return res.json();
    },

    async postJson(path, body) {
      const url = buildUrl(path);
      console.log("[http.js] POST", url);
      const res = await fetch(url, {
        method: "POST",
        headers: defaultHeaders(),
        body: JSON.stringify(body ?? {})
      });
      await mustOk(res);
      return res.headers.get("content-type")?.includes("application/json")
        ? res.json()
        : res.text();
    },

    async putJson(path, body) {
      const url = buildUrl(path);
      console.log("[http.js] PUT", url);
      const res = await fetch(url, {
        method: "PUT",
        headers: defaultHeaders(),
        body: JSON.stringify(body ?? {})
      });
      await mustOk(res);
      return res.headers.get("content-type")?.includes("application/json")
        ? res.json()
        : res.text();
    },

    async del(path) {
      const url = buildUrl(path);
      console.log("[http.js] DELETE", url);
      const res = await fetch(url, { method: "DELETE", headers: defaultHeaders() });
      await mustOk(res);
      return res.headers.get("content-type")?.includes("application/json")
        ? res.json()
        : res.text();
    }
  };
})();