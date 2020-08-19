FROM registry.access.redhat.com/ubi8/openjdk-11

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/home/jboss/app.jar"]
