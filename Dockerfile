FROM openjdk:11.0.11-jre
COPY magpie-cli/target/magpie*.tar.gz /
RUN mkdir /magpie
RUN tar -zxvf *.tar.gz --strip-components=1 -C magpie
WORKDIR /magpie
CMD []
ENTRYPOINT ["./magpie.sh"]


