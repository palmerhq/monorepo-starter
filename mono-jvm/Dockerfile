FROM gradle:4.10.1-jdk8-alpine

# Install imagemagick
USER root
RUN apk --update add imagemagick

# Change user to gradle 
# @see https://discuss.gradle.org/t/how-to-run-a-gradle-java-project-in-docker/24838
ADD --chown=gradle . .
EXPOSE 8080
RUN gradle test
RUN gradle shadowJar
CMD java -jar mono-api/build/libs/mono-api-1.0-SNAPSHOT.jar server mono-api/config/prod.yml
