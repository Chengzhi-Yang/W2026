import { Request, Response } from 'express';

export const getUser = async (req: Request, res: Response) => {
  res.json({
    firstName: "Chengzhi",
    lastName: "Yang"
  });
};

export const login = async (req: Request, res: Response) => {
  res.json({ status: "ok" });
};