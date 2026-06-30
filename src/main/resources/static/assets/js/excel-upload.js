// 공통 엑셀 일괄 업로드 모달. 계약/차량/고객/전표 등록 화면에서 공통으로 사용한다.
// 사용법: openExcelUploadModal({ title, templateUrl, uploadUrl, onSuccess })
(function () {
  let stylesInjected = false;

  function injectStyles() {
    if (stylesInjected) return;
    stylesInjected = true;
    const style = document.createElement("style");
    style.textContent = `
      .xu-backdrop { display:none; position:fixed; inset:0; background:rgba(0,0,0,.45); z-index:10050; align-items:center; justify-content:center; }
      .xu-backdrop.show { display:flex; }
      .xu-modal { width:min(640px, calc(100vw - 24px)); max-height:calc(100vh - 40px); overflow:auto; background:#fff; border-radius:14px; box-shadow:0 18px 50px rgba(0,0,0,.25); }
      .xu-header { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid #eee; }
      .xu-title { font-size:17px; font-weight:700; }
      .xu-close { border:0; background:transparent; font-size:22px; cursor:pointer; line-height:1; }
      .xu-body { padding:18px 20px; }
      .xu-row { margin-bottom:14px; }
      .xu-row label { display:block; font-size:13px; font-weight:700; margin-bottom:6px; color:#374151; }
      .xu-link { color:#2f6fed; text-decoration:underline; font-size:13px; cursor:pointer; }
      .xu-file { width:100%; border:1px solid #ddd; border-radius:10px; padding:8px 10px; box-sizing:border-box; }
      .xu-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:6px; }
      .xu-btn { height:38px; padding:0 16px; border-radius:10px; border:1px solid #ddd; background:#fff; cursor:pointer; }
      .xu-btn-primary { background:#2f6fed; border-color:#2f6fed; color:#fff; }
      .xu-btn:disabled { opacity:.6; cursor:not-allowed; }
      .xu-summary { margin-top:14px; font-size:14px; font-weight:600; }
      .xu-summary.ok { color:#059669; }
      .xu-summary.warn { color:#d97706; }
      .xu-error-table { width:100%; border-collapse:collapse; margin-top:10px; font-size:13px; }
      .xu-error-table th, .xu-error-table td { border:1px solid #eee; padding:8px; text-align:left; }
      .xu-error-table th { background:#fafafa; }
    `;
    document.head.appendChild(style);
  }

  function escapeHtml(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function apiBase() {
    return (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? String(API_BASE_URL).replace(/\/+$/, "") : "";
  }

  window.openExcelUploadModal = function (opts) {
    injectStyles();

    const { title, templateUrl, uploadUrl, onSuccess } = opts || {};

    const backdrop = document.createElement("div");
    backdrop.className = "xu-backdrop";
    backdrop.innerHTML = `
      <div class="xu-modal">
        <div class="xu-header">
          <div class="xu-title">${escapeHtml(title || "엑셀 일괄 업로드")}</div>
          <button type="button" class="xu-close">&times;</button>
        </div>
        <div class="xu-body">
          <div class="xu-row">
            <a class="xu-link" data-action="template">양식 다운로드</a>
          </div>
          <div class="xu-row">
            <label>엑셀 파일 선택 (.xlsx)</label>
            <input type="file" class="xu-file" accept=".xlsx" />
          </div>
          <div class="xu-actions">
            <button type="button" class="xu-btn" data-action="close">취소</button>
            <button type="button" class="xu-btn xu-btn-primary" data-action="upload">업로드</button>
          </div>
          <div class="xu-result"></div>
        </div>
      </div>
    `;
    document.body.appendChild(backdrop);

    function close() {
      backdrop.classList.remove("show");
      backdrop.remove();
    }

    backdrop.querySelector(".xu-close").addEventListener("click", close);
    backdrop.querySelector('[data-action="close"]').addEventListener("click", close);
    backdrop.addEventListener("click", (e) => { if (e.target === backdrop) close(); });
    document.addEventListener("keydown", function escHandler(e) {
      if (e.key === "Escape") { close(); document.removeEventListener("keydown", escHandler); }
    });

    backdrop.querySelector('[data-action="template"]').addEventListener("click", (e) => {
      e.preventDefault();
      window.open(apiBase() + templateUrl, "_blank");
    });

    const uploadBtn = backdrop.querySelector('[data-action="upload"]');
    const fileInput = backdrop.querySelector(".xu-file");
    const resultBox = backdrop.querySelector(".xu-result");

    function renderResult(data) {
      const cls = data.failCount > 0 ? "warn" : "ok";
      let html = `<div class="xu-summary ${cls}">전체 ${data.totalRows}건 중 성공 ${data.successCount}건, 실패 ${data.failCount}건</div>`;

      if (data.errors && data.errors.length > 0) {
        html += `
          <table class="xu-error-table">
            <thead><tr><th style="width:70px;">행번호</th><th>실패 이유</th></tr></thead>
            <tbody>
              ${data.errors.map(err => `<tr><td>${escapeHtml(err.rowNumber)}</td><td>${escapeHtml(err.message)}</td></tr>`).join("")}
            </tbody>
          </table>
        `;
      }
      resultBox.innerHTML = html;
    }

    uploadBtn.addEventListener("click", async () => {
      const file = fileInput.files[0];
      if (!file) { alert("엑셀 파일을 선택해주세요."); return; }

      uploadBtn.disabled = true;
      uploadBtn.textContent = "업로드 중...";
      resultBox.innerHTML = "";

      try {
        const formData = new FormData();
        formData.append("file", file);

        const res = await fetch(apiBase() + uploadUrl, { method: "POST", body: formData });
        if (!res.ok) {
          const text = await res.text().catch(() => "");
          throw new Error(`HTTP ${res.status} ${text}`.trim());
        }
        const data = await res.json();
        renderResult(data);

        if (data.successCount > 0 && typeof onSuccess === "function") {
          onSuccess(data);
        }
      } catch (e) {
        resultBox.innerHTML = `<div class="xu-summary warn">업로드 실패: ${escapeHtml(e.message)}</div>`;
      } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = "업로드";
      }
    });

    requestAnimationFrame(() => backdrop.classList.add("show"));
  };
})();
