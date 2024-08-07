FROM gradle:8.7.0-jdk-21-and-22-graal-jammy AS build_image

WORKDIR /usr/build/

COPY . .
RUN gradle clean build -x test

FROM sapmachine:22

RUN apt-get update && \
    apt-get install -y libstdc++6 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /usr/app/

COPY genesis /usr/app/genesis
COPY --from=build_image /usr/build/build/libs/*.jar /usr/app/app.jar

ENTRYPOINT ["java","-jar","/usr/app/app.jar"]