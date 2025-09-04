FROM debian:13

WORKDIR /app

RUN apt-get update
RUN apt-get -y install ninja-build git python3 curl python3-venv build-essential libc6-dev
RUN git clone --depth=1 https://github.com/google/perfetto .
RUN python3 tools/install-build-deps --ui
RUN python3 tools/gn gen out/ui
RUN python3 tools/ninja -C out/ui perfetto traced trace_processor_shell
RUN ui/build

ENTRYPOINT ["ui/run-dev-server" , "--serve-host", "0.0.0.0"]
