import express, { Router } from 'express';
import { getUser } from '../controllers/user.controller';

export const userRouter = Router();

userRouter.get('/', getUser);