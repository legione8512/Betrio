ALTER TABLE prediction_evaluation
    ADD COLUMN market_home_odd DOUBLE PRECISION,
    ADD COLUMN market_draw_odd DOUBLE PRECISION,
    ADD COLUMN market_away_odd DOUBLE PRECISION;