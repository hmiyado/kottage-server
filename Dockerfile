FROM openjdk:20-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/kottage/ /app/
WORKDIR /app/bin
CMD ["./kottage"]
