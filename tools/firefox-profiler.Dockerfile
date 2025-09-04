FROM node:lts-trixie

WORKDIR /app

RUN apt-get update
RUN apt-get -y install git
RUN git clone https://github.com/firefox-devtools/profiler .
RUN npm exec yarn install

ENTRYPOINT ["npm", "exec", "yarn", "start"]
