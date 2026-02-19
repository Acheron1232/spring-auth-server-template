--liquibase formatted sql

--changeset acheron:1
--comment Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    slug VARCHAR(100)
);

--changeset acheron:2
--comment Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    image_url TEXT,
    size VARCHAR(20),
    color VARCHAR(50),
    brand VARCHAR(100),
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

--changeset acheron:3
--comment Create orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

--changeset acheron:4
--comment Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);

--changeset acheron:5
--comment Seed categories
INSERT INTO categories (name, description, slug) VALUES
    ('Jackets', 'Alt and punk jackets', 'jackets'),
    ('T-Shirts', 'Graphic tees and band shirts', 't-shirts'),
    ('Pants', 'Cargo, ripped, and wide-leg pants', 'pants'),
    ('Accessories', 'Chains, belts, and bags', 'accessories'),
    ('Shoes', 'Platform boots and sneakers', 'shoes');

--changeset acheron:6
--comment Seed sample products
INSERT INTO products (name, description, price, stock, brand, color, size, category_id)
SELECT 'Studded Leather Jacket', 'Classic punk leather jacket with silver studs', 189.99, 15, 'DarkWear', 'Black', 'M',
       (SELECT id FROM categories WHERE slug = 'jackets')
UNION ALL
SELECT 'Void Graphic Tee', 'Oversized tee with abstract void print', 34.99, 50, 'VoidCo', 'Black', 'L',
       (SELECT id FROM categories WHERE slug = 't-shirts')
UNION ALL
SELECT 'Cargo Rave Pants', 'Wide-leg cargo pants with chain detail', 79.99, 30, 'NightForm', 'Olive', 'M',
       (SELECT id FROM categories WHERE slug = 'pants')
UNION ALL
SELECT 'Platform Boots', 'Chunky platform boots, 5cm sole', 149.99, 20, 'GothStep', 'Black', '40',
       (SELECT id FROM categories WHERE slug = 'shoes')
UNION ALL
SELECT 'Spike Collar', 'Genuine leather collar with spikes', 29.99, 100, 'EdgeWear', 'Black', 'One Size',
       (SELECT id FROM categories WHERE slug = 'accessories');