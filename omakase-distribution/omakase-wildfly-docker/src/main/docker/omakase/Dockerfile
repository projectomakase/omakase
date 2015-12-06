FROM projectomakase/wildfly-extras
MAINTAINER Richard Lucas <feedback@projectomakase.org>
EXPOSE 8080 9990 7600
COPY maven /maven/
USER root
RUN chown -R omakase:omakase /maven/ && cp -r /maven/* /opt/
USER omakase
CMD /opt/wildfly/bin/standalone-omakase.sh -c standalone-omakase-$OMAKASE_CONFIG.xml
