import { createApp } from './app';
import { env } from './config/env';
import { WebSocketServer, WebSocket } from 'ws';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}

const wss = new WebSocketServer({ server , path: '/ws/pixels' });
const externalUrl = 'wss://8.229.22.124';
let externalWs = new WebSocket(externalUrl);

function connectToExternal() {
  externalWs = new WebSocket(externalUrl);

  externalWs.on('open', () => console.log('Connected to external pixel source'));

  externalWs.on('message', (data) => {
    const message = data.toString();
    wss.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  });
}

connectToExternal();


