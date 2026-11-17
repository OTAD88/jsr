const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_buyer_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validThemePayload(uid, overrides = {}) {
  return {
    creatorId: uid,
    creatorName: "مصمم جسر",
    title: "واجهة الغروب",
    description: "ثيم دافئ بألوان رملية وإضاءة ناعمة",
    backgroundHex: 0xFFD5C3B0,
    iconHex: 0xFF131312,
    lightHex: 0xFFFFF5E4,
    lightEnabled: true,
    priceUsd: 1,
    isPublished: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller cannot read or list store_themes or accounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("store_themes").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("accounts").doc("JESR-AAAA-BBBB").get());
});

test("Authenticated creator can publish a $1 or Free theme listing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("store_themes").doc("theme_sunset_1").set(validThemePayload(ALICE_UID, { priceUsd: 1 }))
  );
  await assertSucceeds(
    aliceDb.collection("store_themes").doc("theme_free_1").set(validThemePayload(ALICE_UID, { priceUsd: 0 }))
  );
});

test("Reject invalid price (only $0 Free or $1 allowed) and reject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("store_themes").doc("theme_bad_price").set(validThemePayload(ALICE_UID, { priceUsd: 5 }))
  );
  await assertFails(
    aliceDb.collection("store_themes").doc("theme_ghost").set(validThemePayload(ALICE_UID, { isVerified: true }))
  );
});

test("Cross-user update or delete on store_themes is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("store_themes").doc("alice_theme").set(validThemePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("store_themes").where("isPublished", "==", true).get()
  );
  await assertFails(bobDb.collection("store_themes").get());
  await assertFails(
    bobDb.collection("store_themes").doc("alice_theme").update({ priceUsd: 0, updatedAt: new Date() })
  );
  await assertFails(bobDb.collection("store_themes").doc("alice_theme").delete());
});

test("User acquired_themes isolation: owner can read/write, other user cannot", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("acquired_themes").doc("theme_sunset_1").set({
      themeId: "theme_sunset_1",
      title: "واجهة الغروب",
      priceUsd: 1,
      backgroundHex: 0xFFD5C3B0,
      iconHex: 0xFF131312,
      lightHex: 0xFFFFF5E4,
      lightEnabled: true,
      acquiredAt: new Date(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("acquired_themes").get()
  );
});

test("Accounts exact-ID lookup succeeds for signed-in users, list denied, and cross-owner write rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceId = "JESR-ALIC-1111";
  await assertSucceeds(
    aliceDb.collection("accounts").doc(aliceId).set({
      uid: ALICE_UID,
      jesrId: aliceId,
      displayName: "Alice",
      ecdhPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890",
      signingPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0987654321",
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("accounts").doc(aliceId).get());
  await assertFails(bobDb.collection("accounts").get());
  await assertFails(
    bobDb.collection("accounts").doc(aliceId).set({
      uid: BOB_UID,
      jesrId: aliceId,
      displayName: "Hijack",
      ecdhPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890",
      signingPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0987654321",
      updatedAt: new Date(),
    })
  );
});

test("Owner-only private identity, friends, conversations, and messages isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("identity").set({
      uid: ALICE_UID,
      jesrId: "JESR-ALIC-1111",
      encryptedKeyBundle: "{\"v\":1,\"salt\":\"a\",\"iv\":\"b\",\"ct\":\"c\"}",
      ecdhPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1234567890",
      signingPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0987654321",
      displayName: "Alice",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc("JESR-BOB2-2222").set({
      jesrId: "JESR-BOB2-2222",
      displayName: "Bob",
      publicKeyBase64: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE",
      status: "FRIEND",
      addedAt: 1700000000000,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      messageId: "msg_1",
      conversationId: "JESR-BOB2-2222",
      senderId: "JESR-ALIC-1111",
      recipientId: "JESR-BOB2-2222",
      text: "مرحبا من السحابة",
      timestamp: 1700000000000,
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("identity").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("messages").get());
});
