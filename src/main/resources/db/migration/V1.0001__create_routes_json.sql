BEGIN TRANSACTION;

    CREATE TABLE routes_json(
        route_id VARCHAR(64) PRIMARY KEY,
        created_by VARCHAR(255),
        creation_time TIMESTAMP NOT NULL DEFAULT NOW(),
        route_definition VARCHAR(65535),
        creation_date DATE NOT NULL DEFAULT CURRENT_DATE
    );

COMMIT;
