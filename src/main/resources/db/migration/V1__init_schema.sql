CREATE TYPE product_type AS ENUM ('OWN', 'COMPETITOR');
CREATE TYPE crawl_status AS ENUM ('SUCCESS', 'FAILED', 'PENDING');

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    asin            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(500),
    description     TEXT,
    current_price   NUMERIC(12, 2),
    currency        VARCHAR(3) DEFAULT 'USD',
    seller          VARCHAR(255),
    images          JSONB DEFAULT '[]',
    type            product_type NOT NULL,
    last_crawl_at   TIMESTAMPTZ,
    last_crawl_status crawl_status,
    last_crawl_error TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE crawl_snapshots (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price       NUMERIC(12, 2) NOT NULL,
    currency    VARCHAR(3) NOT NULL DEFAULT 'USD',
    seller      VARCHAR(255),
    images      JSONB DEFAULT '[]',
    crawled_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crawl_snapshots_product_id ON crawl_snapshots(product_id);
CREATE INDEX idx_crawl_snapshots_crawled_at ON crawl_snapshots(crawled_at);

CREATE TABLE competitor_links (
    id                      BIGSERIAL PRIMARY KEY,
    own_product_id          BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    competitor_product_id   BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (own_product_id, competitor_product_id)
);

CREATE INDEX idx_competitor_links_own_product ON competitor_links(own_product_id);
