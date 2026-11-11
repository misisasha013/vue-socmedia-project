<script setup lang="ts">
import { onBeforeMount, reactive } from "vue";
import { globals } from "@/stores/globals";
import { Routes, router } from "@/router";

const BASE = import.meta.env.VITE_API_BASE_URL || '';

interface Person {
  id: number;
  name: string;
}

const localState = reactive({
  when: "(loading...)",
  data: [] as Person[],
  loading: false
});

/** Returns up to 2 uppercase initials from a full name for the avatar display.
 * @param name - The full name to get initials from
 */
function getInitials(name: string): string {
  return name.split(" ").map(w => w[0]).join("").toUpperCase().slice(0, 2);
}

/**
 * Navigates to the profile page for a given user.
 * Does nothing if the id is falsy.
 * @param id - The user ID to navigate to
 */
function goToPerson(id: number) {
  if (!id) return;
  router.push(Routes.readPersonOne.replace(":id", id.toString()));
}

/** Fetches all users from the backend and populates the people list. */
async function fetchAllPeople() {
  localState.loading = true;

  try {
    const res = await fetch(`/api/people`, {
      method: "GET",
      credentials: "include",
    });

    if (!res.ok) {
      globals().showPopup(
        "Error",
        `Server replied: ${res.status} ${res.statusText}`
      );
      return;
    }

    const json = await res.json();

    if (json.status === "ok") {
      localState.data = (Array.isArray(json.data) ? json.data : []).map((p: any) => ({
        id: p.id,
        name: p.name
      }));
      localState.when = new Date().toLocaleString();
    } else {
      globals().showPopup("Error", json.message || "Unknown error");
    }

  } catch (err: any) {
    globals().showPopup("Error", err?.message || String(err));
  } finally {
    localState.loading = false;
  }
}

onBeforeMount(fetchAllPeople);
</script>

<template>
    <div style="max-width: 860px; margin: 0 auto; font-family: Inter, system-ui, sans-serif;">

    <p style="
      font-weight: 700;
      font-size: 0.75rem;
      letter-spacing: 0.08em;
      color: #8A96A3;
      margin-bottom: 16px;
    ">PEOPLE</p>

    <p v-if="localState.loading" style="color: #8A96A3;">Loading…</p>

    <div
      v-for="person in localState.data"
      :key="person.id"
      @click="goToPerson(person.id)"
      style="
        background: white;
        border: 1.5px solid #E4E8EC;
        border-radius: 10px;
        padding: 14px 20px;
        margin-bottom: 10px;
        display: flex;
        align-items: center;
        gap: 14px;
        cursor: pointer;
      "
    >
      <!-- Avatar initials  -->
      <div style="
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: rgba(44, 69, 99, 0.69);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      ">
        <span style="font-weight: 700; font-size: 0.8rem; color: white;">
          {{ getInitials(person.name) }}
        </span>
      </div>

      <div style="font-weight: 600; font-size: 0.95rem; color: #1A2533;">
        {{ person.name }}
      </div>
    </div>

    <p v-if="!localState.loading && !localState.data.length" style="color: #8A96A3; text-align: center; margin-top: 40px;">
      No users found.
    </p>

    <div style="margin-top: 16px; font-size: 0.75rem; color: #8A96A3;">
      As of {{ localState.when }}
    </div>
  </div>
</template>