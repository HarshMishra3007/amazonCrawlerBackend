UPDATE products
SET currency = 'INR'
WHERE currency IS NULL OR currency = 'USD';

UPDATE crawl_snapshots
SET currency = 'INR'
WHERE currency = 'USD';
