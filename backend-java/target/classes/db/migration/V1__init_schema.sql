-- Roles
CREATE TABLE IF NOT EXISTS "role" (
    roleid   INTEGER PRIMARY KEY,
    rolename VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO "role" (roleid, rolename) VALUES (1, 'admin'), (2, 'user')
ON CONFLICT DO NOTHING;

-- Groups
CREATE TABLE IF NOT EXISTS "groups" (
    "groupId"     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    "groupName"   VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) UNIQUE,
    "phoneNumber" VARCHAR(50)  UNIQUE,
    "createdAt"   TIMESTAMP   NOT NULL DEFAULT NOW(),
    "updatedAt"   TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Users
CREATE TABLE IF NOT EXISTS "users" (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    roleid       INTEGER     NOT NULL DEFAULT 2 REFERENCES "role"(roleid),
    "groupId"    UUID        REFERENCES "groups"("groupId"),
    username     VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    "clientType" VARCHAR(20)  NOT NULL DEFAULT 'human' CHECK ("clientType" IN ('human','robot')),
    "createdAt"  TIMESTAMP   NOT NULL DEFAULT NOW(),
    "updatedAt"  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Prompts (one per group)
CREATE TABLE IF NOT EXISTS prompts (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    "groupId"    UUID        NOT NULL UNIQUE REFERENCES "groups"("groupId"),
    "promptName" VARCHAR(255) NOT NULL,
    content      TEXT        NOT NULL,
    "createdAt"  TIMESTAMP   NOT NULL DEFAULT NOW(),
    "updatedAt"  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Summary Prompts (one per prompt)
CREATE TABLE IF NOT EXISTS "summaryprompts" (
    id             UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
    "promptId"     UUID  NOT NULL UNIQUE REFERENCES prompts(id),
    summary_prompt TEXT  NOT NULL,
    "createdAt"    TIMESTAMP NOT NULL DEFAULT NOW(),
    "updatedAt"    TIMESTAMP NOT NULL DEFAULT NOW()
);
