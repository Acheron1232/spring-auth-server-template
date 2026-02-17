document.addEventListener("DOMContentLoaded", () => {
    const forms = document.querySelectorAll("form[data-auth-form]");

    const setFieldError = (form, name, text) => {
        const hint = form.querySelector(`[data-field-error="${name}"]`);
        const input = form.querySelector(`[name="${name}"]`);

        if (hint) {
            hint.textContent = text || "";
            hint.classList.toggle("is-error", Boolean(text));
        }
        if (input) {
            input.setAttribute("aria-invalid", text ? "true" : "false");
        }
    };

    const setMessage = (form, text, kind) => {
        const message = form.querySelector(".message");
        if (!message) return;
        message.textContent = text || "";
        message.classList.remove("is-error", "is-success");
        if (kind === "error") message.classList.add("is-error");
        if (kind === "success") message.classList.add("is-success");
    };

    const shake = (el) => {
        if (!el) return;
        el.classList.remove("shake");
        void el.offsetWidth;
        el.classList.add("shake");
    };

    const validateEmail = (value) => {
        const v = (value || "").trim();
        if (!v) return "Email is required.";
        const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
        return ok ? "" : "Enter a valid email.";
    };

    const validateUsername = (value) => {
        const v = (value || "").trim();
        if (!v) return "Username is required.";
        if (v.length < 3) return "Username must be at least 3 characters.";
        if (v.length > 32) return "Username must be at most 32 characters.";
        return "";
    };

    const validatePassword = (value) => {
        const v = value || "";
        if (!v) return "Password is required.";
        if (v.length < 8) return "Password must be at least 8 characters.";
        return "";
    };

    const normalize2fa = (value) => (value || "").replace(/\s+/g, "").trim();

    const validate2fa = (value) => {
        const v = normalize2fa(value);
        if (!v) return "2FA code is required.";
        if (!/^\d{6}$/.test(v)) return "2FA code must be 6 digits.";
        return "";
    };

    const passwordStrength = (value) => {
        const v = value || "";
        let score = 0;
        if (v.length >= 8) score += 1;
        if (v.length >= 12) score += 1;
        if (/[A-Z]/.test(v)) score += 1;
        if (/[0-9]/.test(v)) score += 1;
        if (/[^A-Za-z0-9]/.test(v)) score += 1;
        return Math.min(score, 4);
    };

    const strengthLabel = (score) => {
        if (score <= 1) return "Weak";
        if (score === 2) return "Fair";
        if (score === 3) return "Good";
        return "Strong";
    };

    document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const wrap = btn.closest(".field-with-action");
            const input = wrap ? wrap.querySelector("input") : null;
            if (!input) return;
            const isPwd = input.type === "password";
            input.type = isPwd ? "text" : "password";
            btn.textContent = isPwd ? "Hide" : "Show";
            btn.setAttribute("aria-label", isPwd ? "Hide password" : "Show password");
        });
    });

    const mfaToggle = document.querySelector("[data-mfa-toggle]");
    const mfaPanel = document.querySelector("[data-mfa-panel]");
    if (mfaToggle && mfaPanel) {
        const sync = () => {
            const on = Boolean(mfaToggle.checked);
            mfaPanel.hidden = !on;
        };
        mfaToggle.addEventListener("change", sync);
        sync();
    }

    const regPassword = document.querySelector("#reg_password");
    const bar = document.querySelector("[data-strength] .strength-bar");
    const label = document.querySelector("[data-strength-label]");
    if (regPassword && bar && label) {
        const update = () => {
            const score = passwordStrength(regPassword.value);
            const widths = [0, 28, 52, 76, 100];
            bar.style.width = widths[score] + "%";
            label.textContent = regPassword.value ? strengthLabel(score) : "";
        };
        regPassword.addEventListener("input", update);
        update();
    }

    forms.forEach((form) => {
        const box = form.closest(".auth-box") || form;
        const submit = form.querySelector("[data-submit]");

        const validate = () => {
            let ok = true;
            setMessage(form, "", null);

            const hasUsername = Boolean(form.querySelector('[name="username"]'));
            const hasEmail = Boolean(form.querySelector('[name="email"]'));
            const hasPassword = Boolean(form.querySelector('[name="password"]'));
            const has2fa = Boolean(form.querySelector('[name="2facode"]'));

            if (hasUsername) {
                const err = validateUsername(form.username.value);
                setFieldError(form, "username", err);
                ok = ok && !err;
            }
            if (hasEmail) {
                const err = validateEmail(form.email.value);
                setFieldError(form, "email", err);
                ok = ok && !err;
            }
            if (hasPassword) {
                const err = validatePassword(form.password.value);
                setFieldError(form, "password", err);
                ok = ok && !err;
            }
            if (has2fa) {
                const err = validate2fa(form["2facode"].value);
                setFieldError(form, "2facode", err);
                ok = ok && !err;

                if (ok) {
                    form["2facode"].value = normalize2fa(form["2facode"].value);
                }
            }
            return ok;
        };

        form.addEventListener("input", (e) => {
            const t = e.target;
            if (!(t instanceof HTMLInputElement)) return;
            if (!t.name) return;
            if (["username", "email", "password", "2facode"].includes(t.name)) {
                validate();
            }
        });

        form.addEventListener("submit", (e) => {
            const ok = validate();
            if (!ok) {
                e.preventDefault();
                setMessage(form, "Please fix the highlighted fields.", "error");
                shake(box);
                return;
            }
            if (submit) {
                submit.classList.add("is-loading");
                submit.setAttribute("aria-busy", "true");
            }
        });
    });
});
