// Original diagnostic fixture. No Dixit imports, data, third-party assets or secrets.
import http from 'node:http';
import {randomUUID} from 'node:crypto';
const bootId = randomUUID();
const startedAt = new Date().toISOString();
const instanceId = process.env.RENDER_INSTANCE_ID || 'local';
let stopping = false;
const server = http.createServer((request, response) => {
  response.writeHead(stopping ? 503 : 200, {'Content-Type':'application/json', 'Cache-Control':'no-store'});
  response.end(JSON.stringify({status:stopping?'STOPPING':'UP', purpose:'Dixit operation probe; no game data', bootId, instanceId, startedAt, pid:process.pid}));
});
server.listen(Number(process.env.PORT || 10000), '0.0.0.0', () => {
  console.log(JSON.stringify({event:'PROBE_START',bootId,instanceId,startedAt,pid:process.pid}));
});
process.on('SIGTERM', () => {
  if (stopping) return;
  stopping = true;
  server.close(() => {
    console.log(JSON.stringify({event:'PROBE_STOP',bootId,instanceId,stoppedAt:new Date().toISOString()}));
    process.exit(0);
  });
  server.closeAllConnections();
});
