CREATE TABLE product_roles (
  org_id VARCHAR(128) NOT NULL,
  role_id VARCHAR(64) NOT NULL,
  title_ru VARCHAR(256) NOT NULL,
  features TEXT NOT NULL,
  PRIMARY KEY (org_id, role_id)
);
