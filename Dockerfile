# ui
FROM eclipse-temurin:17-jre
WORKDIR /opt
ENV PORT=8080
EXPOSE 8080
COPY target/*.war /opt/app.war
ENTRYPOINT ["java","-jar","app.war"]
