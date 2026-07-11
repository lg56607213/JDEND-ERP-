# JDEND 렌터카 ERP - 홍보 홈페이지 (1차, 로컬)

렌터카 사업자 대상 마케팅 홈페이지. **정적 HTML/CSS/JS(빌드 불필요, self-contained)**.
ERP 소스(`../src/`)와 분리되어 있으며 이 폴더만으로 동작/배포 가능합니다.

## 로컬에서 열기
- 파일 더블클릭: `index.html`을 브라우저로 열면 됩니다(외부 의존성 없음).
- 로컬 서버(선택): `python -m http.server 5500` 후 http://localhost:5500

## 폴더 구조
```
homepage/
├─ index.html        # 랜딩(히어로·문제공감·핵심기능·차별점·도입절차·FAQ·CTA)
├─ features.html     # 기능 소개(차량/계약/청구수납/자동회계/재무제표/멀티회사)
├─ pricing.html      # 요금제(스타트·그로스·엔터프라이즈, 잠정)
├─ contact.html      # 무료 데모/상담 신청 폼(프론트 only, 전송 미연동)
├─ support.html      # 고객지원·1:1 문의 게시판(골격, CS 정책 미확정)
├─ robots.txt        # SEO(네이버 Yeti 포함)
├─ sitemap.xml       # SEO 사이트맵
└─ assets/
   ├─ css/style.css  # 공용 스타일(모바일 우선)
   └─ js/main.js     # 모바일 메뉴·폼 처리·게시판 탭
```

## 미연동/추후 작업 (중요)
- **폼 전송 미연동**: contact/support 폼은 프론트 검증·완료 안내만. 실제 저장/이메일은 백엔드·폼서비스 연동 필요(`main.js`의 TODO).
- **요금 잠정**: pricing.html 금액은 잠정안. 기획팀·총괄과 확정 필요.
- **CS 게시판 골격**: 실제 게시판(저장·상태·비밀글·SLA)은 CS 담당자와 정책 확정 후 연동.
- **도메인/연락처 placeholder**: `www.rentcarerp.com`·전화·이메일은 확정 시 반영.
- **OG 이미지**: og:image 미지정(대표 이미지 제작 후 추가 권장).
