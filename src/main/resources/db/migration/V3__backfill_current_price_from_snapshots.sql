-- Backfill latest snapshot price on products that have history but no current_price
UPDATE products p
SET current_price = latest.price,
    currency = COALESCE(NULLIF(p.currency, ''), latest.currency, 'USD')
FROM (
    SELECT DISTINCT ON (product_id) product_id, price, currency
    FROM crawl_snapshots
    ORDER BY product_id, crawled_at DESC
) latest
WHERE p.id = latest.product_id
  AND p.current_price IS NULL;
