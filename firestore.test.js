const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validConfig(uid = ALICE_UID, ts = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    salt: "c29tZV9zYWx0XzE2Ynl0ZXM=",
    verifierCiphertext: "ZW5jcnlwdGVkX3ZlcmlmaWVyX3Rva2Vu",
    verifierIv: "MTJieXRlaXZkYXRh",
    passwordHint: "First pet name",
    biometricEnabled: true,
    autoLockSeconds: 60,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validItem(uid = ALICE_UID, ts = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    category: "LOGIN",
    encryptedPayload: "ZW5jcnlwdGVkX3ZhdWx0X2l0ZW1fcGF5bG9hZA==",
    iv: "MTJieXRlaXZkYXRh",
    favorite: false,
    strengthScore: 4,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated user cannot read or write vault_config or vault_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("master").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("master").set(validConfig())
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vault_items").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vault_items").doc("item_1").set(validItem())
  );
});

test("Owner can create and update vault_config/master cleanly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const configRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("vault_config")
    .doc("master");

  await assertSucceeds(configRef.set(validConfig()));
  await assertSucceeds(configRef.get());

  // Action 1: update lock settings
  await assertSucceeds(
    configRef.update({
      biometricEnabled: false,
      autoLockSeconds: 300,
      passwordHint: "Updated hint",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action 2: rotate master password
  await assertSucceeds(
    configRef.update({
      salt: "bmV3X3NhbHRfMTZieXRlcw==",
      verifierCiphertext: "bmV3X2VuY3J5cHRlZF92ZXJpZmllcg==",
      verifierIv: "bmV3MTJieXRlaXY=",
      passwordHint: "",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("vault_config rejects wrong docId, ghost fields, future timestamps, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Only 'master' doc ID is allowed
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("other").set(validConfig())
  );

  // Ghost field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_config")
      .doc("master")
      .set({ ...validConfig(), isAdmin: true })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_config")
      .doc("master")
      .set(validConfig(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Valid create then test immutable createdAt & cross-user isolation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("master").set(validConfig())
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_config")
      .doc("master")
      .update({
        createdAt: new Date(Date.now() - 100),
        updatedAt: new Date(Date.now() - 100),
      })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("master").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vault_config").doc("master").delete()
  );
});

test("Owner can list, create, partial-update, edit, and delete vault_items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemsCol = aliceDb.collection("users").doc(ALICE_UID).collection("vault_items");

  // Empty list query succeeds
  await assertSucceeds(itemsCol.get());

  // Create valid item (no 'id' field)
  await assertSucceeds(itemsCol.doc("item_1").set(validItem()));

  // Toggle favorite
  await assertSucceeds(
    itemsCol.doc("item_1").update({
      favorite: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full item update / re-encryption
  await assertSucceeds(
    itemsCol.doc("item_1").update({
      category: "CARD",
      encryptedPayload: "dXBkYXRlZF9jaXBoZXJ0ZXh0X2RhdGE=",
      iv: "dXBkYXRlZGl2MTI=",
      favorite: false,
      strengthScore: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // List populated collection
  await assertSucceeds(itemsCol.get());

  // Delete item
  await assertSucceeds(itemsCol.doc("item_1").delete());
});

test("vault_items rejects @DocumentId 'id' field, invalid category/bounds, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceItems = aliceDb.collection("users").doc(ALICE_UID).collection("vault_items");

  // Extra 'id' field rejected
  await assertFails(
    aliceItems.doc("item_bad_id").set({ ...validItem(), id: "item_bad_id" })
  );

  // Invalid category rejected
  await assertFails(
    aliceItems.doc("item_bad_cat").set({ ...validItem(), category: "INVALID" })
  );

  // Invalid strengthScore rejected
  await assertFails(
    aliceItems.doc("item_bad_score").set({ ...validItem(), strengthScore: 5 })
  );

  // Seed item and verify Bob cannot read, list, update, or delete Alice's items
  await assertSucceeds(aliceItems.doc("item_alice").set(validItem()));

  const bobViewAlice = bobDb.collection("users").doc(ALICE_UID).collection("vault_items");
  await assertFails(bobViewAlice.get());
  await assertFails(bobViewAlice.doc("item_alice").get());
  await assertFails(
    bobViewAlice.doc("item_alice").update({
      favorite: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobViewAlice.doc("item_alice").delete());
});
