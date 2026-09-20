import express, { type Express, type Request, type Response, type NextFunction } from 'express';
import { userRouter } from './routes/user.router';
import { utilRouter } from './routes/util.router';


export function createApp(): Express {
  const app = express();
  app.use(express.json());

  app.use('/', userRouter);
  app.use('/', utilRouter);

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  app.use((err: any, req: Request, res: Response, next: NextFunction) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Internal Server Error' });
  });

  return app;
}
