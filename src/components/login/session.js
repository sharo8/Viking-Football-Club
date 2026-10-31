export const TOKEN_KEY = "vikingfc.accessToken";
export const REFRESH_KEY = "vikingfc.refreshToken";
export const USER_KEY = "vikingfc.user";

export function writeSession({ accessToken, refreshToken, user }, remember) {
  const primary = remember ? window.localStorage : window.sessionStorage;
  const other = remember ? window.sessionStorage : window.localStorage;

  for (const key of [TOKEN_KEY, REFRESH_KEY, USER_KEY]) {
    other.removeItem(key);
  }

  primary.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) primary.setItem(REFRESH_KEY, refreshToken);
  else primary.removeItem(REFRESH_KEY);
  primary.setItem(USER_KEY, JSON.stringify(user));
  window.dispatchEvent(new Event("vikingfc-session"));
}

export function readSession() {
  const stores = [window.localStorage, window.sessionStorage];
  for (const store of stores) {
    const accessToken = store.getItem(TOKEN_KEY);
    if (!accessToken) continue;
    let user = null;
    try {
      user = JSON.parse(store.getItem(USER_KEY) || "null");
    } catch {
      user = null;
    }
    return {
      accessToken,
      refreshToken: store.getItem(REFRESH_KEY),
      user,
    };
  }
  return null;
}
