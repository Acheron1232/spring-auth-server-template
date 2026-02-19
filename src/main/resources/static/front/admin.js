document.addEventListener("DOMContentLoaded", () => {
    // ── Register Client Form ─────────────────────────────────────────────────
    const registerForm = document.getElementById("registerClientForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const btn = registerForm.querySelector("[data-submit]");
            const msg = document.getElementById("clientMsg");
            btn.classList.add("is-loading");

            const csrfToken = document.cookie
                .split("; ")
                .find(r => r.startsWith("XSRF-TOKEN="))
                ?.split("=")[1];

            const payload = {
                clientId: document.getElementById("clientId").value.trim(),
                clientSecret: document.getElementById("clientSecret").value.trim() || null,
                redirectUris: document.getElementById("redirectUris").value.split(",").map(s => s.trim()).filter(Boolean),
                scopes: document.getElementById("scopes").value.split(",").map(s => s.trim()).filter(Boolean),
                requirePkce: document.getElementById("requirePkce").checked,
                requireConsent: document.getElementById("requireConsent").checked,
                accessTokenTtlMinutes: parseInt(document.getElementById("accessTokenTtl").value) || 5,
                refreshTokenTtlDays: parseInt(document.getElementById("refreshTokenTtl").value) || 20,
            };

            try {
                const res = await fetch("/admin/api/clients", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "X-XSRF-TOKEN": decodeURIComponent(csrfToken || ""),
                    },
                    body: JSON.stringify(payload),
                });

                if (res.ok) {
                    msg.textContent = "Client registered successfully!";
                    msg.className = "message is-success";
                    registerForm.reset();
                } else {
                    const err = await res.json().catch(() => ({ message: "Unknown error" }));
                    msg.textContent = err.message || "Registration failed";
                    msg.className = "message is-error";
                }
            } catch (err) {
                msg.textContent = "Network error: " + err.message;
                msg.className = "message is-error";
            } finally {
                btn.classList.remove("is-loading");
            }
        });
    }
});