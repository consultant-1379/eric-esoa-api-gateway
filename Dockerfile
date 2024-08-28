FROM armdocker.rnd.ericsson.se/proj-orchestration-so/so-base-openjdk17:SO_BASE_VERSION

ENV JAVA_OPTS=""
ENV LOADER_PATH ""
# It's needed for self-sign certificate solution.
ENV CACERT_PATH=""
ENV CACERT_NAME=""
ENV DEFAULT_JAVA_CERTS="/usr/lib64/jvm/java-17-openjdk-17/lib/security/cacerts"

VOLUME /tmp

COPY ./target/eric-esoa-api-gateway-0.0.1-SNAPSHOT.jar eric-esoa-api-gateway.jar
COPY entryPoint.sh /entryPoint.sh

RUN echo "118351:x:118351:118351:An Identity for eric-esoa-api-gateway:/nonexistent:/bin/false" >>/etc/passwd
RUN echo "118351:!::0:::::" >>/etc/shadow

RUN chown -R 118351:0 /var/lib/ca-certificates/java-cacerts && \
    chmod -R 660 /var/lib/ca-certificates/java-cacerts && \
    chown -R 118351:0 /entryPoint.sh && \
    chmod +x entryPoint.sh
USER 118351

CMD ["sh", "-c","/entryPoint.sh"]