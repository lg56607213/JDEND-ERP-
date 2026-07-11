/* JDEND 렌터카 ERP 홈페이지 - 공용 스크립트 (의존성 없음) */
(function () {
  "use strict";

  // 모바일 내비 토글
  var toggle = document.querySelector(".nav-toggle");
  var links = document.querySelector(".nav-links");
  if (toggle && links) {
    toggle.addEventListener("click", function () {
      var open = links.classList.toggle("open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
    });
  }

  // 푸터 연도 자동
  document.querySelectorAll("[data-year]").forEach(function (el) {
    el.textContent = new Date().getFullYear();
  });

  // 문의/데모 신청 폼 (프론트 전용 - 실제 전송은 추후 백엔드/폼서비스 연동 예정)
  document.querySelectorAll("form[data-demo-form]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }
      var msg = form.querySelector(".form-msg");
      // TODO: 실제 전송 연동 (예: 이메일 API / 스프레드시트 / ERP 리드 DB).
      // 현재는 placeholder — 입력값을 콘솔에만 남기고 완료 안내만 표시.
      var data = {};
      new FormData(form).forEach(function (v, k) { data[k] = v; });
      console.log("[데모 신청 - 미연동 placeholder]", data);
      if (msg) {
        msg.classList.add("ok");
        msg.textContent = "신청이 접수되었습니다. 담당자가 1영업일 이내에 연락드리겠습니다. (현재 데모 환경 — 실제 전송은 준비 중)";
        msg.scrollIntoView({ behavior: "smooth", block: "center" });
      }
      form.reset();
    });
  });

  // 게시판 탭 필터 (support.html)
  var tabs = document.querySelectorAll(".tab[data-filter]");
  if (tabs.length) {
    tabs.forEach(function (tab) {
      tab.addEventListener("click", function () {
        tabs.forEach(function (t) { t.classList.remove("active"); });
        tab.classList.add("active");
        var f = tab.getAttribute("data-filter");
        document.querySelectorAll("tr[data-status]").forEach(function (row) {
          row.style.display = (f === "all" || row.getAttribute("data-status") === f) ? "" : "none";
        });
      });
    });
  }
})();
