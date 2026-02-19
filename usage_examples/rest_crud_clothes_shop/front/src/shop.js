export class Shop {
  #api;
  #auth;
  #cart = JSON.parse(localStorage.getItem("shop_cart") || "[]");
  #currentPage = "home";
  #products = [];
  #categories = [];

  constructor({ apiBase, auth }) {
    this.#api = apiBase;
    this.#auth = auth;
  }

  async render(root) {
    await this.#loadData();
    root.innerHTML = this.#buildLayout();
    this.#attachEvents(root);
    this.#renderPage(root, "home");
  }

  async #loadData() {
    const [products, categories] = await Promise.all([
      fetch(`${this.#api}/products?size=50`).then(r => r.json()),
      fetch(`${this.#api}/categories`).then(r => r.json()),
    ]);
    this.#products = products.content || [];
    this.#categories = categories || [];
  }

  #buildLayout() {
    return `
      <div class="app">
        <nav class="nav">
          <a class="nav-brand" href="#" data-page="home">VOID MARKET</a>
          <div class="nav-links">
            <a href="#" data-page="home">Shop</a>
            <a href="#" data-page="cart">Cart <span class="cart-count">${this.#cart.length}</span></a>
            ${this.#auth.isLoggedIn()
              ? `<a href="#" data-page="orders">Orders</a>
                 <button class="nav-btn" id="logoutBtn">Logout</button>`
              : `<button class="nav-btn" id="loginBtn">Login</button>`
            }
          </div>
        </nav>
        <main class="main" id="pageContent"></main>
        <footer class="footer">
          <p>VOID MARKET &copy; ${new Date().getFullYear()} — Powered by Spring Auth Server</p>
        </footer>
      </div>
    `;
  }

  #attachEvents(root) {
    root.addEventListener("click", async (e) => {
      const page = e.target.closest("[data-page]")?.dataset.page;
      if (page) { e.preventDefault(); this.#renderPage(root, page); }

      if (e.target.id === "loginBtn") this.#auth.login();
      if (e.target.id === "logoutBtn") this.#auth.logout();

      if (e.target.closest(".add-to-cart")) {
        const id = e.target.closest(".add-to-cart").dataset.id;
        this.#addToCart(id);
        root.querySelector(".cart-count").textContent = this.#cart.length;
      }

      if (e.target.id === "checkoutBtn") await this.#checkout(root);
    });

    root.addEventListener("change", (e) => {
      if (e.target.id === "categoryFilter" || e.target.id === "searchInput") {
        this.#renderProductGrid(root);
      }
    });

    root.addEventListener("input", (e) => {
      if (e.target.id === "searchInput") this.#renderProductGrid(root);
    });
  }

  #renderPage(root, page) {
    this.#currentPage = page;
    const content = root.querySelector("#pageContent");
    if (page === "home") content.innerHTML = this.#buildHomePage();
    if (page === "cart") content.innerHTML = this.#buildCartPage();
    if (page === "orders") this.#renderOrdersPage(content);
  }

  #buildHomePage() {
    const categoryOptions = this.#categories
      .map(c => `<option value="${c.id}">${c.name}</option>`)
      .join("");

    const productCards = this.#products.map(p => this.#buildProductCard(p)).join("");

    return `
      <section class="hero">
        <div class="hero-content">
          <h1 class="hero-title">WEAR THE <span class="accent">VOID</span></h1>
          <p class="hero-sub">Alt fashion for those who refuse to conform.</p>
        </div>
      </section>
      <section class="catalog">
        <div class="filters">
          <input type="text" id="searchInput" class="filter-input" placeholder="Search brands, styles..."/>
          <select id="categoryFilter" class="filter-select">
            <option value="">All Categories</option>
            ${categoryOptions}
          </select>
        </div>
        <div class="product-grid" id="productGrid">
          ${productCards}
        </div>
      </section>
    `;
  }

  #renderProductGrid(root) {
    const search = root.querySelector("#searchInput")?.value.toLowerCase() || "";
    const catId = root.querySelector("#categoryFilter")?.value || "";
    const filtered = this.#products.filter(p =>
      (!search || p.name.toLowerCase().includes(search) || (p.brand || "").toLowerCase().includes(search)) &&
      (!catId || p.categoryName)
    );
    const grid = root.querySelector("#productGrid");
    if (grid) grid.innerHTML = filtered.map(p => this.#buildProductCard(p)).join("");
  }

  #buildProductCard(p) {
    return `
      <div class="product-card">
        <div class="product-img" style="background:linear-gradient(135deg,#1a1a2e,#16213e)">
          ${p.imageUrl ? `<img src="${p.imageUrl}" alt="${p.name}"/>` : `<div class="product-placeholder">&#128084;</div>`}
          ${p.stock === 0 ? '<div class="out-of-stock">SOLD OUT</div>' : ""}
        </div>
        <div class="product-info">
          <div class="product-brand">${p.brand || ""}</div>
          <div class="product-name">${p.name}</div>
          <div class="product-meta">
            ${p.size ? `<span class="tag">${p.size}</span>` : ""}
            ${p.color ? `<span class="tag">${p.color}</span>` : ""}
          </div>
          <div class="product-footer">
            <span class="product-price">€${Number(p.price).toFixed(2)}</span>
            <button class="add-to-cart" data-id="${p.id}" ${p.stock === 0 ? "disabled" : ""}>
              ${p.stock === 0 ? "Sold Out" : "+ Cart"}
            </button>
          </div>
        </div>
      </div>
    `;
  }

  #buildCartPage() {
    if (this.#cart.length === 0) {
      return `<div class="empty-state"><div class="empty-icon">&#128084;</div><p>Your cart is empty.</p><a href="#" data-page="home" class="btn-primary">Browse Shop</a></div>`;
    }

    const items = this.#cart.map(item => {
      const product = this.#products.find(p => p.id === item.id);
      if (!product) return "";
      return `
        <div class="cart-item">
          <div class="cart-item-name">${product.name}</div>
          <div class="cart-item-brand">${product.brand || ""}</div>
          <div class="cart-item-price">€${Number(product.price).toFixed(2)} × ${item.qty}</div>
        </div>
      `;
    }).join("");

    const total = this.#cart.reduce((sum, item) => {
      const p = this.#products.find(p => p.id === item.id);
      return sum + (p ? Number(p.price) * item.qty : 0);
    }, 0);

    return `
      <section class="cart-section">
        <h2 class="section-title">Your Cart</h2>
        <div class="cart-items">${items}</div>
        <div class="cart-total">Total: <strong>€${total.toFixed(2)}</strong></div>
        ${this.#auth.isLoggedIn()
          ? `<button class="btn-primary" id="checkoutBtn">Place Order</button>`
          : `<p class="cart-login-hint">Please <button class="link-btn" id="loginBtn">login</button> to checkout.</p>`
        }
        <div id="checkoutMsg"></div>
      </section>
    `;
  }

  async #renderOrdersPage(content) {
    if (!this.#auth.isLoggedIn()) {
      content.innerHTML = `<div class="empty-state"><p>Please login to view orders.</p></div>`;
      return;
    }
    content.innerHTML = `<div class="loading">Loading orders...</div>`;
    try {
      const res = await fetch(`${this.#api}/orders`, {
        headers: this.#auth.authHeaders(),
      });
      const data = await res.json();
      const orders = data.content || [];
      if (orders.length === 0) {
        content.innerHTML = `<div class="empty-state"><p>No orders yet.</p></div>`;
        return;
      }
      content.innerHTML = `
        <section class="orders-section">
          <h2 class="section-title">Your Orders</h2>
          ${orders.map(o => `
            <div class="order-card">
              <div class="order-header">
                <span class="order-id">#${o.id.substring(0, 8)}</span>
                <span class="order-status status-${o.status.toLowerCase()}">${o.status}</span>
                <span class="order-total">€${Number(o.totalAmount).toFixed(2)}</span>
              </div>
              <div class="order-items">
                ${o.items.map(i => `<span class="order-item-name">${i.productName} ×${i.quantity}</span>`).join(", ")}
              </div>
            </div>
          `).join("")}
        </section>
      `;
    } catch {
      content.innerHTML = `<div class="empty-state"><p>Failed to load orders.</p></div>`;
    }
  }

  #addToCart(productId) {
    const existing = this.#cart.find(i => i.id === productId);
    if (existing) {
      existing.qty++;
    } else {
      this.#cart.push({ id: productId, qty: 1 });
    }
    localStorage.setItem("shop_cart", JSON.stringify(this.#cart));
  }

  async #checkout(root) {
    const btn = root.querySelector("#checkoutBtn");
    const msg = root.querySelector("#checkoutMsg");
    btn.disabled = true;
    btn.textContent = "Placing order...";

    const items = this.#cart.map(i => ({ productId: i.id, quantity: i.qty }));
    try {
      const res = await fetch(`${this.#api}/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...this.#auth.authHeaders() },
        body: JSON.stringify({ items }),
      });

      if (res.ok) {
        this.#cart = [];
        localStorage.removeItem("shop_cart");
        root.querySelector(".cart-count").textContent = "0";
        msg.innerHTML = `<div class="success-msg">&#10003; Order placed! <a href="#" data-page="orders">View orders</a></div>`;
        root.querySelector("#pageContent").innerHTML = this.#buildCartPage();
      } else {
        msg.innerHTML = `<div class="error-msg">Order failed. Please try again.</div>`;
        btn.disabled = false;
        btn.textContent = "Place Order";
      }
    } catch {
      msg.innerHTML = `<div class="error-msg">Network error.</div>`;
      btn.disabled = false;
      btn.textContent = "Place Order";
    }
  }
}