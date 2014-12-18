CREATE TABLE IF NOT EXISTS PERMISSIONS(
  frameHost               VARCHAR NOT NULL,
  requestHost             VARCHAR NOT NULL,
  permissions             INT4,
  PRIMARY KEY (frameHost, requestHost)
);

CREATE TABLE IF NOT EXISTS COOKIES(
  hostName                VARCHAR NOT NULL,
  name                    VARCHAR NOT NULL,
  value                   VARCHAR NOT NULL,
  path                    VARCHAR NOT NULL,
  secure                  BOOLEAN,
  httpOnly                BOOLEAN,
  creationTime            INT8,
  expirationTime          INT8,
  PRIMARY KEY (hostName, name)
);

CREATE TABLE IF NOT EXISTS GLOBALS(
  notacolumn              CHAR(0) NOT NULL,
  schemaVersion           INT4 NOT NULL,
  permissionsInitialized  BOOLEAN NOT NULL DEFAULT(FALSE),
  PRIMARY KEY (notacolumn)
);

INSERT INTO GLOBALS VALUES ('', 0, FALSE);
