import express, { Router } from 'express';
import { healthCheck } from '../controllers/util.controller';

export const utilRouter = Router();

utilRouter.get('/health', healthCheck);
