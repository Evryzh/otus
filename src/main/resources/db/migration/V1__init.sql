CREATE TABLE customer_profile (
                                  id                  BIGSERIAL PRIMARY KEY,
                                  keycloak_user_id    VARCHAR(255) NOT NULL UNIQUE,
                                  username            VARCHAR(256) NOT NULL UNIQUE,
                                  email               VARCHAR(255) NOT NULL UNIQUE,
                                  phone               VARCHAR(20) UNIQUE,
                                  first_name          VARCHAR(100) NOT NULL,
                                  last_name           VARCHAR(100) NOT NULL,
                                  middle_name         VARCHAR(100),
                                  birth_date          DATE,
                                  preferred_language  VARCHAR(10) NOT NULL DEFAULT 'ru',
                                  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_customer_profile_keycloak_id ON customer_profile (keycloak_user_id);
CREATE UNIQUE INDEX idx_customer_profile_email ON customer_profile (email);
CREATE UNIQUE INDEX idx_customer_profile_phone ON customer_profile (phone);