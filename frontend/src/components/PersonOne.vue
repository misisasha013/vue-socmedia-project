<script setup lang="ts">
import { reactive, onBeforeMount } from "vue";
import { useRoute } from "vue-router";
import { globals } from "@/stores/globals";
import { router, Routes } from "@/router";

const route = useRoute();
const id = route.params.id as string;

// Reactive state
const localState = reactive({
  loaded: false,
  email: "",
  name: "",
  gender_identity: "",
  sexual_orientation: "",
  note: "",
  buttonOff: true,
});

// Make currentUser reactive
const currentUser = reactive({ value: false });

/**
 * Reads a cookie value by name from document.cookie.
 * @param name - The cookie name to look up
 */
function getCookie(name: string) {
  const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
  return match ? match[2] : null;
}

/** Returns up to 2 uppercase initials from a full name for the avatar display.
 * @param name - The full name to get initials from
 */
function getInitials(name: string): string {
  return name
    .split(" ")
    .map(w => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

/** Returns up to 2 uppercase initials from a full name for the avatar display.
 * @param name - The full name to get initials from
 */
async function loadPerson() {
  try {
    const res = await fetch(`/api/users/${id}`, {
      method: "GET",
      headers: { "Content-Type": "application/json; charset=UTF-8" },
      credentials: "include",
    });

    if (!res.ok) {
      globals().showPopup(
        "Error",
        `Server responded with: ${res.status} ${res.statusText}`
      );
      return;
    }

    const json = await res.json();
    if (json.status === "ok") {
      const person = json.data;

      localState.email = person.email;
      localState.name = person.name;
      localState.gender_identity = person.gender_identity || "";
      localState.sexual_orientation = person.sexual_orientation || "";
      localState.note = person.note || "";
      
      currentUser.value = `${person.id}` === getCookie("auth.id");
      localState.loaded = true;
      localState.buttonOff = false;
    } else {
      globals().showPopup("Error", json.message || "Failed to load user");
    }
  } catch (err: any) {
    globals().showPopup("Error", err.message || String(err));
  }
}

/**
 * Saves changes to the user's own profile.
 * Only the profile owner can call this — the backend enforces this too.
 * Does nothing if the profile hasn't loaded yet or the name is blank.
 */
async function update() {
  if (!localState.loaded) return;

  if (!localState.name.trim()) {
    globals().showPopup("Error", "Name cannot be blank");
    return;
  }

  localState.buttonOff = true;

  try {
    const res = await fetch(`/api/users/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json; charset=UTF-8" },
      credentials: "include",
      body: JSON.stringify({
        name: localState.name,
        gender_identity: localState.gender_identity || null,
        sexual_orientation: localState.sexual_orientation || null,
        note: localState.note || null,
      }),
    });

    if (!res.ok) {
      globals().showPopup(
        "Error",
        `Server responded with: ${res.status} ${res.statusText}`
      );
      return;
    }

    const json = await res.json();
    if (json.status === "ok") {
      globals().showPopup("Info", "Profile successfully updated");
    } else {
      globals().showPopup("Error", json.message || "Update failed");
    }
  } catch (err: any) {
    globals().showPopup("Error", err.message || String(err));
  } finally {
    localState.buttonOff = false;
  }
}

onBeforeMount(loadPerson);
</script>

<template>
  <div style="max-width: 620px; margin: 0 auto; font-family: Inter, system-ui, sans-serif;">

    <button
      @click="router.push(Routes.messages)"
      style="
        margin-bottom: 16px;
        padding: 8px 16px;
        background: white;
        color: #2C4563;
        border: 1.5px solid #E4E8EC;
        border-radius: 6px;
        cursor: pointer;
        font-family: Inter, system-ui, sans-serif;
      "
    >← Back</button>

    <!-- Profile card -->
    <div style="background: white; border: 1.5px solid #E4E8EC; border-radius: 10px; overflow: hidden;">

      <!-- Header banner - teal for owner, navy for viewer -->
      <div :style="`
        background: ${currentUser.value ? '#0F9E82' : '#2C4563'};
        padding: 20px 24px;
        display: flex;
        align-items: center;
        gap: 20px;
      `">
        <!-- Avatar inital circle -->
        <div style="
          width: 64px;
          height: 64px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        ">
          <span :style="`
            font-weight: 700;
            font-size: 1.4rem;
            color: ${currentUser.value ? '#0F9E82' : '#2C4563'};
            font-family: Inter, system-ui, sans-serif;
          `">
            {{ localState.name ? getInitials(localState.name) : '?' }}
          </span>
        </div>

        <!-- Name and email -->
        <div>
          <div style="font-weight: 700; font-size: 1.3rem; color: white; font-family: Inter, system-ui, sans-serif;">
            {{ localState.name || 'Loading...' }}
          </div>
          <div style="font-size: 0.9rem; color: rgba(255,255,255,0.72); font-family: Inter, system-ui, sans-serif; margin-top: 2px;">
            {{ localState.email || '' }}
          </div>
        </div>
      </div>

      <!-- Form body -->
      <div style="padding: 24px;">

        <!-- email - always read only -->
        <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em; display: block; margin-bottom: 6px;">EMAIL</label>
        <div style="
          width: 100%;
          padding: 10px 12px;
          margin-bottom: 18px;
          background: #F7F8F9;
          border: 1.5px solid #E4E8EC;
          border-radius: 6px;
          font-size: 0.9rem;
          color: #1A2533;
          box-sizing: border-box;
        ">{{ localState.email || 'Loading...' }}</div>

        <!-- Name -->
        <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em; display: block; margin-bottom: 6px;">NAME</label>

        <!-- Owner: editable -->
        <input
          v-if="currentUser.value"
          v-model="localState.name"
          placeholder="Your name"
          style="
            width: 100%;
            padding: 10px 12px;
            margin-bottom: 18px;
            border: 1.5px solid #0F9E82;
            border-radius: 6px;
            font-size: 0.9rem;
            color: #1A2533;
            background: white; 
            box-sizing: border-box;
            font-family: Inter, system-ui, sans-serif;
          "
        />
        <!-- Viewer: read only -->
        <div
          v-else
          style="
            width: 100%;
            padding: 10px 12px;
            margin-bottom: 18px;
            background: #F7F8F9;
            border: 1.5px solid #E4E8EC;
            border-radius: 6px;
            font-size: 0.9rem;
            color: #1A2533;
            box-sizing: border-box;
          "
        >{{ localState.name || 'Loading...' }}</div>

        <!-- Gender Identity - owner only -->
        <template v-if="currentUser.value">
          <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em; display: block; margin-bottom: 6px;">GENDER IDENTITY</label>
          <select
            v-model="localState.gender_identity"
            style="
              width: 100%;
              padding: 10px 12px;
              margin-bottom: 18px;
              border: 1.5px solid #0F9E82;
              border-radius: 6px;
              font-size: 0.9rem;
              color: #2C4563;
              background: white;
              box-sizing: border-box;
              font-family: Inter, system-ui, sans-serif;
            "
          >
            <option value="">-- Select --</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
            <option value="Nonbinary">Nonbinary</option>
            <option value="Other">Other</option>
            <option value="Choose not to disclose">Choose not to disclose</option>
          </select>

          <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em; display: block; margin-bottom: 6px;">SEXUAL ORIENTATION</label>
          <select
            v-model="localState.sexual_orientation"
            style="
              width: 100%;
              padding: 10px 12px;
              margin-bottom: 18px;
              border: 1.5px solid #0F9E82;
              border-radius: 6px;
              font-size: 0.9rem;
              color: #2C4563;
              background: white;
              box-sizing: border-box;
              font-family: Inter, system-ui, sans-serif;
            "
          >
            <option value="">-- Select --</option>
            <option value="Heterosexual">Heterosexual</option>
            <option value="Homosexual">Homosexual</option>
            <option value="Bisexual">Bisexual</option>
            <option value="Other">Other</option>
            <option value="Questioning">Questioning</option>
            <option value="Choose not to disclose">Choose not to disclose</option>
          </select>
        </template>

        <!-- note -->
        <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em; display: block; margin-bottom: 6px;">NOTE</label>

        <!-- Owner: editable text area -->
        <textarea
          v-if="currentUser.value"
          v-model="localState.note"
          placeholder="Add a personal note… (200 char limit)"
          maxlength="200"
          rows="4"
          style="
            width: 100%;
            padding: 10px 12px;
            border: 1.5px solid #0F9E82;
            border-radius: 6px;
            font-size: 0.9rem;
            color: #1A2533;
            background: white; 
            box-sizing: border-box;
            resize: vertical;
            font-family: Inter, system-ui, sans-serif;
          "
        />
        <!-- Viewer: read only -->
        <div
          v-else
          style="
            width: 100%;
            padding: 10px 12px;
            background: #F7F8F9;
            border: 1.5px solid #E4E8EC;
            border-radius: 6px;
            font-size: 0.9rem;
            color: #1A2533;
            box-sizing: border-box;
            min-height: 80px;
          "
        >{{ localState.note || 'No note added.' }}</div>

        <!-- Footer bar with Save button (owner only) -->
        <div v-if="currentUser.value" style="
          margin-top: 20px;
          padding-top: 16px;
          border-top: 1px solid #F0F2F5;
          display: flex;
          justify-content: flex-end;
        ">
          <button
            @click="update"
            :disabled="localState.buttonOff"
            style="
              padding: 10px 24px;
              background: #F06030;
              color: white;
              border: none;
              border-radius: 6px;
              font-weight: 700;
              cursor: pointer;
              font-family: Inter, system-ui, sans-serif;
              font-size: 0.9rem;
            "
          >Save Changes</button>
        </div>
      </div>
    </div>
  </div>
</template>