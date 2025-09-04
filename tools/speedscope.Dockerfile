FROM debian:13

WORKDIR /app

RUN apt-get update
RUN apt-get -y install npm git
RUN git clone https://github.com/jlfwong/speedscope .
RUN npm i
RUN scripts/prepack.sh --outdir dist --protocol http

ENTRYPOINT ["npm", "exec", "tsx", "scripts/dev-server.ts"]
