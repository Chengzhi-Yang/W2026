import express, { Router } from 'express';
import { healthCheck, getTime, getIp } from '../controllers/util.controller';

export const utilRouter = Router();

utilRouter.get('/health', healthCheck);
utilRouter.get('/time', getTime);
utilRouter.get('/ip', getIp);

