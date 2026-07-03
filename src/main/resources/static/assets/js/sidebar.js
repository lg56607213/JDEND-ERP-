

function loadSidebar() {
  // 현재 페이지의 경로 깊이를 감지하여 base path 설정
  const path = window.location.pathname;
  const isInSubfolder = path.includes('/pages/');
  const basePath = isInSubfolder ? '../../' : '';

  const sidebarHTML = `
    <aside class="sidebar" id="sidebar">
      <div class="sidebar-header">
        <a href="${basePath}index.html" class="sidebar-title">JDEND <span class="erp-logo">ERP</span></a>
        <button class="sidebar-toggle" id="sidebarToggle">
          <span class="toggle-icon"></span>
          <span class="toggle-icon"></span>
          <span class="toggle-icon"></span>
        </button>
      </div>
      <nav>
        <ul class="sidebar-menu">
          <li class="has-sub">
            <span class="menu-label">고객관리</span>
            <ul>
              <li><a href="${basePath}pages/customer/customer_register.html">고객등록</a></li>
              <li><a href="${basePath}pages/customer/account_register.html">계좌등록</a></li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">차량등록</span>
            <ul>
              <li class="has-sub">
                <span class="menu-label">차량현황</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/vehicle_order.html">발주</a></li>
                  <li><a href="${basePath}pages/vehicle/vehicle_advance.html">선급</a></li>
                  <li><a href="${basePath}pages/vehicle/vehicle_register.html">등록</a></li>
                  <li><a href="${basePath}pages/vehicle/vehicle_delivery.html">실행</a></li>
                </ul>
              </li>
<li class="has-sub">
  <span class="menu-label">차량 차입금관리</span>
  <ul>
    <li><a href="${basePath}pages/payment/loan_schedule_register.html">차입금스케줄등록</a></li>
    <li><a href="${basePath}pages/payment/loan_schedule_adjust.html">차입금스케줄조정</a></li>
    <li><a href="${basePath}pages/payment/loan_status.html">차입금현황</a></li>
  </ul>
</li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">계약관리</span>
            <ul>
              <li class="has-sub">
                <span class="menu-label">계약등록</span>
                <ul>
                  <li><a href="${basePath}pages/contract/contract_register.html">등록</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">중도상환</span>
                <ul>
                  <li><a href="${basePath}pages/contract/early_termination.html">반납</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">만기관리</span>
                <ul>
                  <li><a href="${basePath}pages/contract/maturity_management.html">재계약</a></li>
                  <li><a href="${basePath}pages/contract/maturity_termination.html">종료</a></li>
                </ul>
              </li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">차량관리</span>
            <ul>
              <li class="has-sub">
                <span class="menu-label">정비</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/maintenance_register.html">정비등록</a></li>
                  <li><a href="${basePath}pages/vehicle/maintenance_status.html">정비현황</a></li>
                  <li><a href="${basePath}pages/vehicle/mt_status.html">MT현황</a></li>
                  <li><a href="${basePath}pages/vehicle/mt_register.html">MT등록</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">검사관리</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/periodic_inspection.html">검사현황</a></li>
                  <li><a href="${basePath}pages/vehicle/inspection_register.html">검사등록</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">배차등록</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/dispatch_register.html">납품&회수&기타</a></li>
                  <li><a href="${basePath}pages/vehicle/dispatch_status.html">배차현황</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">매각관리</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/vehicle_sale_register.html">매각등록</a></li>
                  <li><a href="${basePath}pages/vehicle/vehicle_sale_status.html">매각현황</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">보험관리</span>
                <ul>
                  <li><a href="${basePath}pages/vehicle/insurance_register.html">등록</a></li>
                  <li><a href="${basePath}pages/vehicle/insurance_status.html">보험현황</a></li>
                </ul>
              </li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">수납관리</span>
            <ul>
              <li class="has-sub">
                <span class="menu-label">스케줄관리</span>
                <ul>
                  <li><a href="${basePath}pages/payment/schedule_management.html">스케줄변경</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">청구관리</span>
                <ul>
                  <li><a href="${basePath}pages/payment/billing_create.html">청구생성</a></li>
                  <li><a href="${basePath}pages/payment/billing_issue.html">청구서발행</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">수납관리</span>
                <ul>
                  <li><a href="${basePath}pages/payment/payment_register.html">수납등록</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">미수관리</span>
                <ul>
                  <li><a href="${basePath}pages/payment/receivable_register.html">미수금등록</a></li>
                  <li><a href="${basePath}pages/payment/receivable_status.html">미수현황</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">상담관리</span>
                <ul>
                  <li><a href="${basePath}pages/payment/consultation_register.html">상담등록</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">은행내역업로드</span>
                <ul>
                  <li><a href="${basePath}pages/payment/bank_transaction_upload.html">은행내역업로드</a></li>
                </ul>
              </li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">회계관리</span>
            <ul>
              <li class="has-sub">
                <span class="menu-label">전표등록</span>
                <ul>
                  <li><a href="${basePath}pages/accounting/daily_voucher.html">일일전표등록</a></li>
                  <li><a href="${basePath}pages/accounting/sales_voucher.html">매출전표등록</a></li>
                  <li><a href="${basePath}pages/accounting/monthly_voucher.html">월 전표등록</a></li>
                  <li><a href="${basePath}pages/accounting/depreciation_register.html">감가상각등록</a></li>
                  <li><a href="${basePath}pages/accounting/depreciation_schedule_change.html">감가상각스케줄변경</a></li>
                  <li><a href="${basePath}pages/accounting/voucher_approval.html">전표승인</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">재무제표관리</span>
                <ul>
                  <li><a href="${basePath}pages/accounting/balance_sheet.html">재무상태표</a></li>
                  <li><a href="${basePath}pages/accounting/income_statement.html">손익계산서</a></li>
                   <li><a href="${basePath}pages/management/financial_statement_management.html">재무제표관리</a></li>
                </ul>
              </li>
              <li class="has-sub">
                <span class="menu-label">일일자금</span>
                <ul>
                  <li><a href="${basePath}pages/accounting/daily_cash_flow.html">일자별 입출금현황</a></li>
                  <li><a href="${basePath}pages/accounting/daily_fund_report.html">일일자금일보</a></li>
                </ul>
              </li>
            </ul>
          </li>
          <li class="has-sub">
            <span class="menu-label">총괄관리</span>
            <ul>
              <li><a href="${basePath}pages/management/contract_status.html">계약현황</a></li>
              <li class="has-sub">
                <span class="menu-label">회계계정관리</span>
                <ul>
                  <li><a href="${basePath}pages/accounting/account_management.html">기타계정관리</a></li>
                </ul>
              </li>
              <li id="companyUsersMenuItem" style="display:none;">
                <a href="${basePath}pages/management/company_users.html">사용자관리</a>
              </li>
            </ul>
          </li>
        </ul>
      </nav>
      <div class="sidebar-footer">
        <button class="sidebar-logout-btn" id="sidebarLogoutBtn">&#x2192; 로그아웃</button>
      </div>
    </aside>
  `;
  document.getElementById('sidebar-container').innerHTML = sidebarHTML + '<div class="sidebar-overlay" id="sidebarOverlay"></div>';

  // 로그아웃 버튼
  const logoutBtn = document.getElementById('sidebarLogoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', function() {
      const base = (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? String(API_BASE_URL).replace(/\/+$/, '') : '';
      fetch(base + '/api/auth/logout', { method: 'POST', credentials: 'include' })
        .finally(function() {
          window.location.href = base + '/login.html';
        });
    });
  }

  // 회사관리자(또는 운영자)일 때만 "사용자관리" 메뉴 노출. 페이지별로 role.js를 따로 안 넣어도
  // 항상 동작하게 sidebar.js 자체에서 직접 /api/auth/me를 조회한다.
  (function () {
    const base = (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? String(API_BASE_URL).replace(/\/+$/, '') : '';
    fetch(base + '/api/auth/me', { credentials: 'include' })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        const role = data && data.success ? data.role : null;
        const item = document.getElementById('companyUsersMenuItem');
        if (item && (role === 'ADMIN' || role === 'COMPANY_ADMIN')) item.style.display = '';
      })
      .catch(function (e) { console.error('권한 조회 실패', e); });
  })();

  // 서브메뉴 접기/펼치기 (아코디언 방식)
  document.querySelectorAll('.sidebar .has-sub > .menu-label').forEach(function(label) {
    label.addEventListener('click', function(e) {
      e.stopPropagation();
      const parent = label.parentElement;
      const isOpen = parent.classList.contains('open');

      // 같은 레벨의 모든 메뉴 닫기
      const siblings = parent.parentElement.children;
      Array.from(siblings).forEach(function(sibling) {
        if(sibling.classList.contains('has-sub')) {
          sibling.classList.remove('open');
        }
      });

      // 클릭한 메뉴가 닫혀있었으면 열기
      if(!isOpen) {
        parent.classList.add('open');
      }
    });
  });

  // 현재 페이지에 해당하는 링크에 active 클래스 추가 및 부모 메뉴 열기
  const currentPage = window.location.pathname.split('/').pop();
  const currentLink = document.querySelector(`a[href$="${currentPage}"]`);

  if (currentLink) {
    currentLink.classList.add('active');

    // 현재 링크의 부모 메뉴들을 찾아서 open 클래스 추가
    let parent = currentLink.closest('.has-sub');
    while (parent) {
      parent.classList.add('open');
      parent = parent.parentElement.closest('.has-sub');
    }
  }

  // 모든 메뉴 링크의 파일 존재 여부를 자동 체크 (웹서버 환경에서만)
  // 로컬 파일(file://) 환경에서는 fetch가 작동하지 않으므로 건너뜀
  if (window.location.protocol !== 'file:') {
    document.querySelectorAll('.sidebar-menu a[href$=".html"]').forEach(function(link) {
      const href = link.getAttribute('href');
      if (!href || href === '#') return;

      fetch(href, { method: 'HEAD' })
        .then(function(response) {
          if (!response.ok) {
            // 파일이 없으면 미연결 표시
            link.style.color = '#e53e3e';
            link.style.fontWeight = '600';
            link.innerHTML = link.textContent + ' <span style="font-size:10px;background:#e53e3e;color:#fff;padding:1px 4px;border-radius:3px;margin-left:4px;">미연결</span>';
            link.addEventListener('click', function(e) {
              e.preventDefault();
              alert('아직 준비 중인 페이지입니다.');
            });
          }
        })
        .catch(function() {
          // 에러 발생 시에도 미연결 표시
          link.style.color = '#e53e3e';
          link.style.fontWeight = '600';
          link.innerHTML = link.textContent + ' <span style="font-size:10px;background:#e53e3e;color:#fff;padding:1px 4px;border-radius:3px;margin-left:4px;">미연결</span>';
          link.addEventListener('click', function(e) {
            e.preventDefault();
            alert('아직 준비 중인 페이지입니다.');
          });
        });
    });
  }

  // 사이드바 토글 기능
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebar = document.getElementById('sidebar');
  const sidebarOverlay = document.getElementById('sidebarOverlay');
  const contentWrapper = document.querySelector('.content-wrapper');

  // 모바일 체크 함수
  function isMobile() {
    return window.innerWidth <= 768;
  }

  if (sidebarToggle) {
    sidebarToggle.addEventListener('click', function(e) {
      e.stopPropagation();

      if (isMobile()) {
        // 모바일: open 클래스 토글
        sidebar.classList.toggle('open');

        if (sidebar.classList.contains('open')) {
          sidebarOverlay.style.display = 'block';
        } else {
          sidebarOverlay.style.display = 'none';
        }
      } else {
        // 데스크톱: collapsed 클래스 토글
        sidebar.classList.toggle('collapsed');

        if (contentWrapper) {
          if (sidebar.classList.contains('collapsed')) {
            contentWrapper.style.marginLeft = '0';
          } else {
            contentWrapper.style.marginLeft = '220px';
          }
        }
      }
    });
  }

  // 오버레이 클릭 시 사이드바 닫기
  if (sidebarOverlay) {
    sidebarOverlay.addEventListener('click', function() {
      sidebar.classList.remove('open');
      sidebarOverlay.style.display = 'none';
    });
  }

  // 윈도우 리사이즈 시 처리
  window.addEventListener('resize', function() {
    if (!isMobile()) {
      sidebar.classList.remove('open');
      sidebarOverlay.style.display = 'none';

      // 데스크톱에서 collapsed 상태가 아니면 margin 복원
      if (contentWrapper && !sidebar.classList.contains('collapsed')) {
        contentWrapper.style.marginLeft = '220px';
      }
    } else {
      // 모바일에서는 margin 제거
      if (contentWrapper) {
        contentWrapper.style.marginLeft = '0';
      }
    }
  });
}
window.addEventListener('DOMContentLoaded', loadSidebar);
