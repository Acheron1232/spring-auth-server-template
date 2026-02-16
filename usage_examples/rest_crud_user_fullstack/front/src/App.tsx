import { useEffect, useMemo, useState } from 'react'
import './App.css'

function App() {
    const [error, setError] = useState<string | null>(null);
    const [me, setMe] = useState<{ id: string; email: string; displayName: string } | null>(null);
    const [products, setProducts] = useState<Array<{ id: string; sku: string; name: string; priceCents: number }>>([]);
    const [orders, setOrders] = useState<Array<{ id: string; userId: string; productId: string; quantity: number; status: string }>>([]);

    const [newProductSku, setNewProductSku] = useState('SKU-001');
    const [newProductName, setNewProductName] = useState('Test Product');
    const [newProductPriceCents, setNewProductPriceCents] = useState(1999);

    const [orderProductId, setOrderProductId] = useState<string>('');
    const [orderQuantity, setOrderQuantity] = useState<number>(1);

    const canOrder = useMemo(() => orderProductId.trim().length > 0 && orderQuantity > 0, [orderProductId, orderQuantity]);

    const handleLogin = () => {
        window.location.href = '/oauth2/authorization/messaging-client-oidc';
    };

    const handleLogout = () => {
        window.location.href = '/logout';
    };

    const fetchMe = async () => {
        setError(null);
        try {
            const res = await fetch('/api/me');
            if (res.status === 401) {
                setMe(null);
                return;
            }
            if (!res.ok) {
                throw new Error('Failed to fetch /api/me');
            }
            const json = await res.json();
            setMe(json);
        } catch (e) {
            setError('Помилка запиту /api/me');
            console.error(e);
        }
    };

    const fetchProducts = async () => {
        setError(null);
        try {
            const res = await fetch('/api/products');
            if (res.status === 401) {
                setError('Не авторизовано! Будь ласка, увійдіть.');
                return;
            }
            if (!res.ok) {
                throw new Error('Failed to fetch /api/products');
            }
            const json = await res.json();
            setProducts(json);
            if (json.length > 0 && !orderProductId) {
                setOrderProductId(json[0].id);
            }
        } catch (e) {
            setError('Помилка запиту /api/products');
            console.error(e);
        }
    };

    const createProduct = async () => {
        setError(null);
        try {
            const res = await fetch('/api/products', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sku: newProductSku,
                    name: newProductName,
                    priceCents: newProductPriceCents,
                }),
            });
            if (res.status === 401) {
                setError('Не авторизовано! Будь ласка, увійдіть.');
                return;
            }
            if (!res.ok) {
                setError('Не вдалося створити продукт (можливо SKU вже існує)');
                return;
            }
            await fetchProducts();
        } catch (e) {
            setError('Помилка запиту POST /api/products');
            console.error(e);
        }
    };

    const fetchOrders = async () => {
        setError(null);
        try {
            const res = await fetch('/api/orders');
            if (res.status === 401) {
                setError('Не авторизовано! Будь ласка, увійдіть.');
                return;
            }
            if (!res.ok) {
                throw new Error('Failed to fetch /api/orders');
            }
            const json = await res.json();
            setOrders(json);
        } catch (e) {
            setError('Помилка запиту /api/orders');
            console.error(e);
        }
    };

    const createOrder = async () => {
        if (!canOrder) return;

        setError(null);
        try {
            const res = await fetch('/api/orders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    productId: orderProductId,
                    quantity: orderQuantity,
                }),
            });
            if (res.status === 401) {
                setError('Не авторизовано! Будь ласка, увійдіть.');
                return;
            }
            if (!res.ok) {
                setError('Не вдалося створити замовлення');
                return;
            }
            await fetchOrders();
        } catch (e) {
            setError('Помилка запиту POST /api/orders');
            console.error(e);
        }
    };

    useEffect(() => {
        void fetchMe();
    }, []);

    return (
        <div className="card">
            <h1>Shop CRUD (Auth Server Real Case)</h1>

            <div className="button-group">
                <button onClick={handleLogin} style={{backgroundColor: '#4CAF50'}}>
                    Увійти (Login)
                </button>

                <button onClick={fetchMe} style={{backgroundColor: '#607D8B'}}>
                    /api/me
                </button>

                <button onClick={fetchProducts} style={{backgroundColor: '#2196F3'}}>
                    Products
                </button>

                <button onClick={fetchOrders} style={{backgroundColor: '#3F51B5'}}>
                    My Orders
                </button>

                <button onClick={handleLogout} style={{backgroundColor: '#f44336'}}>
                    Вийти (Logout)
                </button>
            </div>

            <div className="status-area">
                {error && <p style={{color: 'red'}}>{error}</p>}

                <h3>Session</h3>
                {me ? (
                    <pre style={{textAlign: 'left', background: '#333', padding: '10px', borderRadius: '5px'}}>
                        {JSON.stringify(me, null, 2)}
                    </pre>
                ) : (
                    <p>Не авторизовано (або сесія не створена). Натисни Login і потім /api/me.</p>
                )}

                <h3>Create Product</h3>
                <div style={{display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center'}}>
                    <input value={newProductSku} onChange={(e) => setNewProductSku(e.target.value)} placeholder="SKU" />
                    <input value={newProductName} onChange={(e) => setNewProductName(e.target.value)} placeholder="Name" />
                    <input
                        type="number"
                        value={newProductPriceCents}
                        onChange={(e) => setNewProductPriceCents(Number(e.target.value))}
                        placeholder="Price cents"
                        min={0}
                    />
                    <button onClick={createProduct}>Create</button>
                </div>

                <h3>Products</h3>
                <pre style={{textAlign: 'left', background: '#333', padding: '10px', borderRadius: '5px'}}>
                    {JSON.stringify(products, null, 2)}
                </pre>

                <h3>Create Order</h3>
                <div style={{display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center'}}>
                    <select value={orderProductId} onChange={(e) => setOrderProductId(e.target.value)}>
                        <option value="" disabled>Choose product</option>
                        {products.map((p) => (
                            <option key={p.id} value={p.id}>
                                {p.sku} - {p.name}
                            </option>
                        ))}
                    </select>
                    <input
                        type="number"
                        value={orderQuantity}
                        onChange={(e) => setOrderQuantity(Number(e.target.value))}
                        min={1}
                    />
                    <button onClick={createOrder} disabled={!canOrder}>Create order</button>
                </div>

                <h3>My Orders</h3>
                <pre style={{textAlign: 'left', background: '#333', padding: '10px', borderRadius: '5px'}}>
                    {JSON.stringify(orders, null, 2)}
                </pre>
            </div>
        </div>
    )
}

export default App