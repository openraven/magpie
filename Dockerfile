FROM   maven:3.9.6-amazoncorretto-21-debian-bookworm as builder
RUN mkdir /build/
COPY ./ /build/ 
RUN ls -al /build/
RUN cd /build/ && mvn clean  install -DskipTests && mvn --projects magpie-cli assembly:single -DskipTests

FROM openjdk:17.0.2-slim-bullseye
COPY --from=builder /build/magpie-cli/target/*.tar.gz /tmp/
RUN mkdir /magpie
RUN tar -zxvf /tmp/*.tar.gz --strip-components=1 -C magpie
RUN apt update && apt install -y git && apt clean all
WORKDIR /magpie
CMD []
ENTRYPOINT ["./magpie.sh"]


