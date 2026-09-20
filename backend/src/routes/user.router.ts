import express, { Router } from 'express';
import { getUser, login } from '../controllers/user.controller';

export const userRouter = Router();

userRouter.get('/user', getUser);
userRouter.post('/login', login);