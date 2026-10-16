# 빌드 단계: 정적 파일 생성
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# full 프로필은 개발 전용이다. 개발용 운영자 계정(HTTP Basic)이 번들에 포함되므로 운영 배포에는 쓰지 않는다.
ARG VITE_DEV_USERNAME
ARG VITE_DEV_PASSWORD
RUN npm run build

# 서빙 단계: 정적 파일 제공과 /api 프록시
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
