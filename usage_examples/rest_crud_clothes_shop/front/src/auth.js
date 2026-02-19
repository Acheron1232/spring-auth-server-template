export class Auth {
  #config;
  #tokenKey = "shop_access_token";

  constructor(config) {
    this.#config = config;
  }

  get token() {
    return sessionStorage.getItem(this.#tokenKey);
  }

  isLoggedIn() {
    return !!this.token;
  }

  async login() {
    const verifier = this.#generateCodeVerifier();
    const challenge = await this.#generateCodeChallenge(verifier);
    sessionStorage.setItem("pkce_verifier", verifier);

    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.#config.clientId,
      redirect_uri: this.#config.redirectUri,
      scope: this.#config.scopes,
      code_challenge: challenge,
      code_challenge_method: "S256",
    });

    window.location.href = `${this.#config.authServerUrl}/oauth2/authorize?${params}`;
  }

  async handleCallback() {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    if (!code) return;

    const verifier = sessionStorage.getItem("pkce_verifier");
    const body = new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri: this.#config.redirectUri,
      client_id: this.#config.clientId,
      code_verifier: verifier,
    });

    const res = await fetch(`${this.#config.authServerUrl}/oauth2/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
    });

    if (res.ok) {
      const data = await res.json();
      sessionStorage.setItem(this.#tokenKey, data.access_token);
      sessionStorage.removeItem("pkce_verifier");
    }
  }

  logout() {
    sessionStorage.removeItem(this.#tokenKey);
    window.location.href = `${this.#config.authServerUrl}/connect/logout?post_logout_redirect_uri=${encodeURIComponent(window.location.origin)}`;
  }

  authHeaders() {
    return this.token ? { Authorization: `Bearer ${this.token}` } : {};
  }

  #generateCodeVerifier() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return btoa(String.fromCharCode(...array)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
  }

  async #generateCodeChallenge(verifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const digest = await crypto.subtle.digest("SHA-256", data);
    return btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
  }
}