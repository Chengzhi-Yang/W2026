import os from 'os';
import { Request, Response } from 'express';

export const healthCheck = (req: Request, res: Response) => {
    res.json({ status: 'ok' });
}


/** First non-internal IPv4 address on this machine. */
function getServerIp(): string {
  for (const addresses of Object.values(os.networkInterfaces())) {
    for (const address of addresses ?? []) {
      if (address.family === 'IPv4' && !address.internal) {
        return address.address;
      }
    }
  }
  return '127.0.0.1';
}

export const getIp = (req: Request, res: Response) => {
  const clientIp = (req.ip ?? 'unknown').replace(/^::ffff:/, '');

  res.json({
    serverIp: getServerIp(),
    clientIp,
  });
};

export const getTime = (req: Request, res: Response) => {
    res.json({
        time: new Date().toISOString(),
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    })
}