# AWS Lightsail 내부 테스트 배포

개발 프로필(`dev`)을 그대로 쓰는 **내부 테스트용** 배포다. 정식 공개에는 쓰지 않는다.
보호 방식: nginx가 사이트 전체에 Basic 인증을 걸고, 백엔드 운영자 계정은 nginx가 서버에서 주입한다(브라우저·번들에 노출되지 않음).

## 1. 인스턴스
- Lightsail > 인스턴스 생성 > Linux/Unix > OS 전용 Ubuntu 22.04/24.04, **2GB RAM 이상**
- 고정 IP 연결, 방화벽은 **22(SSH), 80(HTTP)만** 허용 (DB·백엔드 포트는 열지 않는다)

## 2. Docker 설치 (SSH 접속 후)
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
```

## 3. 코드 반입과 설정
```bash
git clone <저장소 주소> sample && cd sample      # 또는 scp 로 복사
cp .env.example .env && nano .env                # 아래 값을 채운다
```
`.env`에 반드시 채울 값: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`(강한 값),
`DEV_OPERATOR_USERNAME`, `DEV_OPERATOR_PASSWORD`(강한 값). 나머지는 비워도 된다.

사이트 접속용 Basic 인증 계정 생성:
```bash
mkdir -p deploy
docker run --rm httpd:2.4-alpine htpasswd -nbB 접속아이디 접속비밀번호 > deploy/.htpasswd
```

## 4. 실행
```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker compose -f docker-compose.prod.yml ps
```
접속: `http://<고정 IP>` (브라우저가 묻는 Basic 인증 계정 입력)

## 5. 운영
- 로그: `docker compose -f docker-compose.prod.yml logs -f backend`
- 갱신: `git pull && docker compose -f docker-compose.prod.yml --env-file .env up -d --build`
- 백업: Lightsail 인스턴스 스냅샷(DB `pgdata`, 이미지 `imagedata` 볼륨 포함)
- HTTP 는 평문이다. Basic 인증 정보가 노출되므로 도메인을 연결하면 HTTPS(Let's Encrypt)를 적용한다.
