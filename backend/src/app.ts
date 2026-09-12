import express, { type Express } from 'express';
import { userRouter } from './routes/user.router';
import { utilRouter } from './routes/util.router';


export function createApp(): Express {
  const app = express();
  app.use(express.json());

  app.use('/user', userRouter);
  app.use('/', utilRouter);

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
