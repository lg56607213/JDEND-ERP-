# 운영 서버 배포 가이드 (rentcarerp.com)

> 비밀번호·키 값은 이 문서에 적지 않는다. 값이 있는 위치만 적는다.

## 1. 서버 구성

| 항목 | 내용 |
|---|---|
| 서버 | AWS EC2 (Ubuntu), 리전 `ap-northeast-2`, 인스턴스 `i-0af9141df61da056e`, 공인 IP `3.36.181.58` |
| 접속 | `ssh -i <jdenderp-server.pem> ubuntu@3.36.181.58` (비밀번호 로그인 꺼짐, 키 로그인만 가능) |
| 보안그룹 | `launch-wizard-1`, `ec2-rds-1` |
| IAM 역할 | 없음 (그래서 AWS SSM 은 현재 쓸 수 없음) |
| 자바 | OpenJDK 17 (`/usr/bin/java`) |
| DB | AWS RDS MySQL (`jdend-erp-db...rds.amazonaws.com`). 인증 DB `erp` + 회사별 DB `erp_company_*` |
| 웹서버 | nginx → `http://localhost:8080` 프록시. 설정 파일 `/etc/nginx/sites-enabled/erp` |
| HTTPS | Let's Encrypt (certbot, nginx 방식). `certbot.timer` 가 하루 두 번 자동 갱신 |

## 2. ERP 실행 방식

- **systemd 서비스 `erp`** 로 실행한다. 정의 파일: `/etc/systemd/system/erp.service`
  - 작업 디렉터리: `/home/ubuntu/app`
  - 실행: `java -Xms512m -Xmx1024m ... -jar /home/ubuntu/app/erp.jar --spring.profiles.active=prod --spring.config.additional-location=file:/home/ubuntu/app/application-prod.yml`
  - `Restart=always` — 죽으면 10초 뒤 자동 재시작
- **설정값(DB 접속, 메일 계정, 키움페이)은 환경변수가 아니라 `/home/ubuntu/app/application-prod.yml` 에 있다.**
  저장소의 `application.yml` 에 있는 `${DB_URL_AUTH}` 등은 이 파일 값으로 덮어써진다.
  - CODEF 키(`CODEF_CLIENT_ID` 등)는 비어 있어도 된다(기본값 빈 문자열, CODEF 중단 상태).
- 서버 디렉터리는 **git 저장소가 아니다**. 빌드된 jar 파일만 올려서 교체한다.
- DB 테이블은 `ddl-auto: update` 로 앱 시작 시 자동 반영된다(모든 `erp_company_*` 포함). 따로 실행할 SQL 은 없다.

## 3. 절대 건드리지 말 것

- **`/home/ubuntu/app/uploads/`** — 첨부파일(계약서·보험증권 스캔 등) 실제 저장소. 삭제·덮어쓰기 금지.
- `/home/ubuntu/app/application-prod.yml` — 운영 비밀값이 들어 있음. 저장소에 올리지 말 것.

## 4. 자동 배포 (기본 방법)

**`main` 브랜치에 push 하면 GitHub Actions 가 자동으로 배포한다.** 어느 PC에서 push 해도 된다.
설정 파일: `.github/workflows/deploy.yml`

1. JDK 17 로 jar 빌드
2. 서버에 `erp.jar.new` 로 업로드
3. 현재 jar 백업(`erp.jar.bak-auto-<시각>-before-<커밋>`) → 교체 → `systemctl restart erp`
4. 서버 안에서 기동 로그와 `localhost:8080` 응답 확인, 이어서 `https://rentcarerp.com/login.html` 200 확인
5. **3·4번이 실패하면 백업 jar 로 자동 원복**하고 Actions 실행이 빨간색(실패)으로 끝난다
6. 자동 배포 백업은 최근 5개만 남긴다

- 진행 상황: GitHub 저장소 → **Actions** 탭. 실패하면 GitHub 가 메일로 알려준다.
- 같은 커밋을 다시 배포하려면 Actions 탭 → Deploy to production → **Run workflow**.
- 배포 중 약 30초~1분 접속이 끊긴다. 영업시간 중 push 는 피할 것.
- 필요한 GitHub Secrets (값은 GitHub 에만 저장): `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`(pem 내용), `EC2_KNOWN_HOSTS`(서버 호스트 키)

## 4-1. 수동 배포 절차 (Actions 를 못 쓸 때)

PC에서 (저장소 루트):

```bash
./mvnw -DskipTests package                      # target/erp-0.0.1-SNAPSHOT.jar 생성
ssh -i <pem> ubuntu@3.36.181.58 'cat > /home/ubuntu/app/erp.jar.new' < target/erp-0.0.1-SNAPSHOT.jar
```

서버에서:

```bash
cd /home/ubuntu/app
cp -p erp.jar erp.jar.bak-$(date +%Y%m%d-%H%M)  # 1) 현재 jar 백업
mv erp.jar.new erp.jar                          # 2) 교체
sudo systemctl restart erp                      # 3) 재시작 (약 30초~1분 접속 끊김)
sudo journalctl -u erp -f                        # 4) "Started ErpApplication" 나오면 성공
curl -sI https://rentcarerp.com/login.html      # 5) 200 확인
```

### 원복 (문제 생겼을 때)

```bash
cd /home/ubuntu/app
cp -p erp.jar.bak-<날짜> erp.jar
sudo systemctl restart erp
```

## 5. 로그 보기

```bash
sudo journalctl -u erp --since "10 min ago" --no-pager
```

## 6. 장애 이력

- 2026-10-13: HTTPS 인증서 만료로 로그인 불가. 원인은 iptables 의 `80 → 8080` 우회 규칙이
  인증서 갱신 요청을 nginx 대신 Spring 으로 보낸 것. 규칙 삭제(백업 `/etc/iptables/rules.v4.bak-20261013`),
  certbot 을 nginx 방식으로 변경.
