ALTER TABLE prediction_run
ADD COLUMN IF NOT EXISTS model_version VARCHAR(50);

UPDATE prediction_run
SET model_version = 'poisson-v1'
WHERE model_version IS NULL;

ALTER TABLE prediction_run
ALTER COLUMN model_version SET DEFAULT 'poisson-v1';

ALTER TABLE prediction_run
ALTER COLUMN model_version SET NOT NULL;