import './style.css';
import { Auth } from "./auth.js";
import { Shop } from "./shop.js";

const auth = new Auth({
  authServerUrl: "http://localhost:9000",
  clientId: "alt-shop_mobile",
  redirectUri: window.location.origin + "/callback",
  scopes: "openid profile email message.read",
});

const shop = new Shop({ apiBase: "/api", auth });

async function bootstrap() {
  if (window.location.pathname === "/callback") {
    await auth.handleCallback();
    window.history.replaceState({}, "", "/");
  }
  await shop.render(document.getElementById("root"));
}

bootstrap();