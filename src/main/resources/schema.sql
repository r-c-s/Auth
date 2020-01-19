CREATE TABLE user_credentials (
  username    VARCHAR(32) PRIMARY KEY,
  password    VARCHAR(128) NOT NULL,
  authority   INTEGER NOT NULL
);