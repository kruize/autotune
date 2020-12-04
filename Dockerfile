#
# Copyright (c) 2019, 2019 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM adoptopenjdk/maven-openjdk11-openj9:latest as mvnbuild-openj9

RUN apt-get update \
    && apt-get install -y --no-install-recommends git vim \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/app

COPY src /opt/app/src
COPY pom.xml /opt/app/

RUN mvn install dependency:copy-dependencies

ARG AUTOTUNE_VERSION

RUN mvn clean package

RUN jlink --strip-debug --compress 2 --no-header-files --no-man-pages --module-path /opt/java/openjdk/jmods --add-modules java.base,java.compiler,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.sql,java.xml,jdk.compiler,jdk.httpserver,jdk.unsupported,jdk.crypto.ec --exclude-files=**java_**.properties,**J9TraceFormat**.dat,**OMRTraceFormat**.dat,**j9ddr**.dat,**public_suffix_list**.dat --output jre

#####################################################################

FROM dinogun/alpine:3.10-glibc

ARG AUTOTUNE_VERSION

WORKDIR /opt/app

RUN adduser -u 1001 -S -G root -s /usr/sbin/nologin autotune \
    && chown -R 1001:0 /opt/app \
    && chmod -R g+rw /opt/app

USER 1001

COPY --chown=1001:0 --from=mvnbuild-openj9 /opt/app/jre /opt/app/jre
COPY --chown=1001:0 --from=mvnbuild-openj9 /opt/app/target/autotune-monitoring-0.0.1-NA-jar-with-dependencies.jar /opt/app/autotune-monitoring-with-dependencies.jar

ENV JAVA_HOME=/opt/app/jre \
    PATH="/opt/app/jre/bin:$PATH"

CMD ["java", "-jar", "/opt/app/autotune-monitoring-with-dependencies.jar"]
