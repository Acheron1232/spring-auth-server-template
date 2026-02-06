import { useState } from 'react'
import './App.css'

function App() {
    const [data, setData] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleLogin = () => {
        window.location.href = 'http://localhost:5173/oauth2/authorization/messaging-client-oidc';
    };

    // 2. Функція отримання даних
    const fetchData = async () => {
        try {
            const response = await fetch('/api/test');

            if (response.status === 401) {
                setError("Не авторизовано! Будь ласка, увійдіть.");
                setData(null);
                return;
            }

            if (!response.ok) {
                throw new Error('Помилка мережі');
            }

            const text = await response.text();
            setData(text);
            setError(null);
        } catch (e) {
            setError("Помилка запиту");
            console.error(e);
        }
    };

    const handleLogout = () => {
        window.location.href = 'http://localhost:5173/logout';
    }

    return (
        <div className="card">
            <h1>Auth Server Test</h1>

            <div className="button-group">
                <button onClick={handleLogin} style={{backgroundColor: '#4CAF50'}}>
                    Увійти (Login)
                </button>

                <button onClick={fetchData} style={{backgroundColor: '#2196F3'}}>
                    Отримати Дані (Get Resource)
                </button>

                <button onClick={handleLogout} style={{backgroundColor: '#f44336'}}>
                    Вийти (Logout)
                </button>
            </div>

            <div className="status-area">
                <h3>Результат:</h3>
                {error && <p style={{color: 'red'}}>{error}</p>}
                {data && <pre style={{textAlign: 'left', background: '#333', padding: '10px', borderRadius: '5px'}}>{data}</pre>}
                {!data && !error && <p>Натисніть "Отримати Дані" щоб перевірити доступ.</p>}
            </div>
        </div>
    )
}

export default App