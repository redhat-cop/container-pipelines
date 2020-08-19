FROM registry.access.redhat.com/ubi8/openjdk-17-runtime

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/home/jboss/app.jar"]
