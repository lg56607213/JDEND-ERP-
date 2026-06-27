// 로그인한 사용자의 권한(ADMIN/MANAGER/STAFF)을 한 번 조회해서 캐시해두고,
// 등록 외(수정/삭제/승인) 버튼을 권한에 따라 숨기는 데 쓰는 공통 헬퍼.
(function () {
  let cachedRole = null;
  let fetchPromise = null;

  function apiBase() {
    return (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? String(API_BASE_URL).replace(/\/+$/, "") : "";
  }

  async function fetchRole() {
    try {
      const res = await fetch(apiBase() + "/api/auth/me", { credentials: "include" });
      const data = await res.json();
      cachedRole = data && data.success ? data.role : null;
    } catch (e) {
      console.error("권한 조회 실패", e);
      cachedRole = null;
    }
    return cachedRole;
  }

  window.getCurrentRole = function () {
    if (!fetchPromise) fetchPromise = fetchRole();
    return fetchPromise;
  };

  window.isManager = async function () {
    const role = await window.getCurrentRole();
    return role === "ADMIN" || role === "COMPANY_ADMIN" || role === "MANAGER";
  };

  // 회사관리자(또는 운영자) 여부 — "사용자관리" 화면/메뉴 노출 판단용.
  window.isCompanyAdmin = async function () {
    const role = await window.getCurrentRole();
    return role === "ADMIN" || role === "COMPANY_ADMIN";
  };

  // 실무자(STAFF)일 때 지정한 엘리먼트들을 비활성화 처리(숨기지 않고 disabled + 안내).
  window.disableForStaff = async function (elements, title) {
    if (await window.isManager()) return;

    const list = elements instanceof NodeList || Array.isArray(elements) ? elements : [elements];
    list.forEach((el) => {
      if (!el) return;
      el.disabled = true;
      el.title = title || "책임자 권한이 필요합니다.";
      el.style.opacity = "0.5";
      el.style.cursor = "not-allowed";
    });
  };
})();
