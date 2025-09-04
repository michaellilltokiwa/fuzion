FROM ubuntu:noble
RUN apt update
RUN apt -y install hotspot

CMD ["hotspot", "/perf.data"]
