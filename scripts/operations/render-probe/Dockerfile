# Temporary operation probe only: no application, assets, secrets or database.
FROM node:22.23.4-bookworm-slim@sha256:4b150b48b4116e2a22f2c39cc27025fbf4f153e2078fd3b14d159fdb5c7279da
WORKDIR /probe
COPY --chown=node:node probe.mjs ./
USER node
EXPOSE 10000
ENTRYPOINT ["node", "/probe/probe.mjs"]
