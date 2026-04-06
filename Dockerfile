# 1. Amazon Corretto 17 이미지를 베이스 이미지로 설정
FROM amazoncorretto:17

# 2. 작업 디렉터리 설정
WORKDIR /app

# 3. 프로젝트 파일을 컨테이너 작업 디렉터리(/app)로 복사
COPY . .

# 4. `gradlew` 파일에 실행 권한 부여
RUN chmod +x ./gradlew
# 5. Gradle Wrapper 사용하여 build
RUN ./gradlew build

# 6. 서비스 포트 노출
EXPOSE 80

# 7. 프로젝트 정보를 환경 변수로 설정 -> 실행할 jar 파일의 이름을 추론하는데 활용
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8
# 8. JVM 옵션을 환경 변수로 설정 (기본값은 빈 문자열)
ENV JVM_OPTS=""

# 9. 컨테이너가 실행될 때 실행할 명령어
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar"]