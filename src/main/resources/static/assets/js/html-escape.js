// 사용자 입력값을 innerHTML에 꽂기 전에 이스케이프하는 공용 헬퍼(XSS 방지).
function escHtml(s) {
  return String(s ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
