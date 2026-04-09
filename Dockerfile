# ========== 1. Build stage ==========
# 1-1. Amazon Corretto 17 이미지를 build 베이스 이미지로 설정
FROM amazoncorretto:17 AS builder

# 1-2. 작업 디렉터리 설정
WORKDIR /app

# 1-3. 레이어 캐시를 고려해, 자주 변경되지 않는 Gradle 관련 파일 먼저 복사
# COPY 원본_경로 컨테이너_안의_목적지_경로
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# 1-4. `gradlew` 파일에 실행 권한 부여
# `chmod` : 파일의 권한을 변경하는 리눅스 명령어
# `+x` : `chmod`와 함께 쓰이며, 실행 권한을 추가하는 명령어
RUN chmod +x ./gradlew

# 1-5. Gradle 의존성 관련 레이어를 먼저 생성하여 캐시 활용
# `--no-daemon` : 백그라운드 상주 프로세스(daemon) 없이 한 번만 실행하고 끝내라는 명령어
RUN ./gradlew dependencies --no-daemon
# 즉, 소스코드만 바뀌고, Gradle 관련 설정이 안 바뀌면, 이 앞의 레이어 재사용 가능해짐

# 1-6. 실제 소스 코드 복사
COPY src ./src

# 1-7. Gradle Wrapper 사용하여 실행 가능한 `JAR` 파일 생성
RUN ./gradlew clean bootJar --no-daemon

# ========== 2. Runtime stage ==========
# 2-1. Amazon Corretto 17 이미지를 런타임 베이스 이미지로 설정
FROM amazoncorretto:17

# 2-2. 작업 디렉터리 설정
WORKDIR /app

# 2-3. 서비스 포트 노출
EXPOSE 80

# 2-4. 프로젝트 정보를 환경 변수로 설정 -> 실행할 jar 파일의 이름을 추론하는데 활용
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8

# 2-5. JVM 옵션을 환경 변수로 설정 (기본값은 빈 문자열)
ENV JVM_OPTS=""

# 2-6. Build stage에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar ./

# 2-7. 컨테이너가 실행될 때 실행할 명령어
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar ${PROJECT_NAME}-${PROJECT_VERSION}.jar"]