FROM openjdk:11
COPY magpie-cli/target/magpie*.tar.gz /
RUN mkdir /magpie
RUN tar -zxvf *.tar.gz --strip-components=1 -C magpie
WORKDIR /magpie
ENTRYPOINT ["./magpie"]


