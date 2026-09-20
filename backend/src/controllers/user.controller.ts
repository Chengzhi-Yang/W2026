import { Request, Response } from 'express';
import { OAuth2Client } from 'google-auth-library';
import { env } from '../config/env';

const client = new OAuth2Client();

// 1. Storage for the logged-in user
let currentUser = {
  firstName: "Guest",
  lastName: "User"
};

export const getUser = async (req: Request, res: Response) => {
  res.json(currentUser);
};

export const login = async (req: Request, res: Response) => {
  const { idToken } = req.body;

  try {
    const ticket = await client.verifyIdToken({
      idToken,
      audience: env.googleClientId,
    });
    const payload = ticket.getPayload();

    if (!payload) {
      return res.status(401).json({ error: 'Invalid token' });
    }

    // 2. Update the current user
    currentUser = {
      firstName: payload.given_name || "Google",
      lastName: payload.family_name || "User",
    };

    console.log("Successfully logged in:", currentUser.firstName);
    res.json(currentUser);

  } catch (error: any) {
    // 3. 🔍 Check your BACKEND terminal/console for this output!
    console.error("Google Token Verification Failed:", error.message);
    res.status(401).json({ error: 'Authentication failed', details: error.message });
  }
};