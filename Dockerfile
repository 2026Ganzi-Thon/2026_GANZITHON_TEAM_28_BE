# 배포용 이미지: 미리 빌드한 jar 를 실행만 합니다.
# 빌드:  ./gradlew bootJar -x test
#        docker build --platform linux/amd64 -t <도커허브ID>/team28-backend:latest .
# (Mac M칩은 EC2(x86) 에서 돌리려면 --platform linux/amd64 필수)
FROM eclipse-temurin:21-jre

WORKDIR /app
ENV TZ=Asia/Seoul

COPY build/libs/*.jar app.jar

EXPOSE 8080

# t2/t3.micro(메모리 1GB) 기준. 메모리가 더 큰 인스턴스면 JAVA_OPTS 로 늘리세요.
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
