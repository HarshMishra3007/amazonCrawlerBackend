-- Remove demo price history that was inserted by the retired V2 migration
DELETE FROM crawl_snapshots
WHERE crawled_at IN (
    '2026-04-28 10:00:00+00',
    '2026-05-05 10:00:00+00',
    '2026-05-12 10:00:00+00',
    '2026-05-19 10:00:00+00',
    '2026-05-26 10:00:00+00',
    '2026-06-02 10:00:00+00',
    '2026-06-09 10:00:00+00',
    '2026-06-16 10:00:00+00'
);

-- Re-sync current_price from remaining real crawl snapshots only
UPDATE products p
SET current_price = NULL
WHERE NOT EXISTS (
    SELECT 1 FROM crawl_snapshots cs WHERE cs.product_id = p.id
);

UPDATE products p
SET current_price = latest.price,
    currency = COALESCE(NULLIF(p.currency, ''), latest.currency, 'USD')
FROM (
    SELECT DISTINCT ON (product_id) product_id, price, currency
    FROM crawl_snapshots
    ORDER BY product_id, crawled_at DESC
) latest
WHERE p.id = latest.product_id;
