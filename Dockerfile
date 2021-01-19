#
# Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
##########################################################
#            Build Docker Image
##########################################################
FROM maven:3-adoptopenjdk-11 as mvnbuild-jdk11

# Install any additional packages that a typical developer in the team needs.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git vim \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/src/autotune

# Copy the pom.xml only and download the dependencies
COPY pom.xml /opt/src/autotune/
RUN mvn -f pom.xml install dependency:copy-dependencies

# Now copy the sources and compile and package them
COPY src /opt/src/autotune/src/
RUN mvn -f pom.xml clean package

# Create a jlinked JRE specific to the App
RUN jlink --strip-debug --compress 2 --no-header-files --no-man-pages --module-path /opt/java/openjdk/jmods --add-modules java.base,java.compiler,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.sql,java.xml,jdk.compiler,jdk.httpserver,jdk.unsupported,jdk.crypto.ec --exclude-files=**java_**.properties,**J9TraceFormat**.dat,**OMRTraceFormat**.dat,**j9ddr**.dat,**public_suffix_list**.dat --output jre

##########################################################
#            Runtime Docker Image
##########################################################
# Use ubi-minimal as the base image
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.3

ARG AUTOTUNE_VERSION

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

# Install packages needed for Java to function correctly
RUN microdnf install -y tzdata openssl curl ca-certificates fontconfig glibc-langpack-en gzip tar \
    && microdnf update -y; microdnf clean all

LABEL name="Kruize Autotune" \
      vendor="Red Hat" \
      version=${AUTOTUNE_VERSION} \
      release=${AUTOTUNE_VERSION} \
      run="docker run --rm -it -p 8080:8080 <image_name:tag>" \
      summary="Docker Image for Autotune with ubi-minimal" \
      description="For more information on this image please see https://github.com/kruize/autotune/blob/master/README.md"

WORKDIR /opt/app

# Create the non root user, same as the one used in the build phase.
RUN microdnf -y install shadow-utils \
    && useradd -u 1001 -r -g 0 -s /usr/sbin/nologin default \
    && chown -R 1001:0 /opt/app \
    && chmod -R g+rw /opt/app \
    && microdnf -y remove shadow-utils \
    && microdnf clean all

# Switch to the non root user
USER 1001

# Copy the jlinked JRE
COPY --chown=1001:0 --from=mvnbuild-jdk11 /opt/src/autotune/jre/ /opt/app/jre/
# Copy the app binaries
COPY --chown=1001:0 --from=mvnbuild-jdk11 /opt/src/autotune/target/ /opt/app/target/

EXPOSE 8080

ENV JAVA_HOME=/opt/app/jre \
    PATH="/opt/app/jre/bin:$PATH"

ENTRYPOINT bash target/bin/Autotune
