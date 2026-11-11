<!-- Changeing App.vue to just handle the nav bar & logout and handling messges in sperate components -->

<script setup lang="ts">
import { router, Routes } from "./router";

/**
 * Logs the current user out by calling the backend logout endpoint, then redirects to the home page to clear the session.
 */
async function logout() {
  try{
    await fetch (`${import.meta.env.VITE_API_BASE_URL || ''}/logout`, {
      credentials: "include",
    });
  } catch (e) {
    console.error("Logout failed:", e);
  }
  window.location.href = "/";
}
</script>

<template>
  <nav style="display: flex; align-items: center; justify-content: space-between; padding: 12px 32px; background: #FFFFFF; border-bottom: 1px solid #E4E8EC; margin-bottom: 0;">
    
    <div style="display: flex; align-items: baseline; gap: 0;">
      <span style="
        font-family: Inter, system-ui, sans-serif;
        font-weight: 700;
        font-size: 1.6rem;
        color: #2C4563;
      ">The </span>
      <span style="
        font-family: Inter, system-ui, sans-serif;
        font-weight: 700;
        font-size: 1.6rem;
        color: #0F9E82;
      ">Buzz</span>
    </div>

<!-- Changed button tempaltes to include a hover so moved them out of inline style to a scoped style with transistions-->
    <div style="display: flex; gap: 10px; align-items: center;">
      <button
       @click="router.push('/messages/new')"
        class="nav-btn nav-btn--primary">+ New Idea</button>
      <button
        @click="router.push(Routes.readPersonAll)"
        class="nav-btn nav-btn--outline">People</button>
      <button
        @click="router.push(Routes.messages)"
        class="nav-btn nav-btn--outline">Refresh</button>
      <button
        @click="logout"
        class="nav-btn nav-btn--danger">Logout</button>
    </div>
  </nav>
  <div style="background: #F7F8F9; min-height: 100vh; padding: 32px;">
    <router-view />
  </div>
</template>
 
<style scoped>
.nav-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-family: Inter, system-ui, sans-serif;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
 
.nav-btn--primary {
  background: #0F9E82;
  color: white;
  border: none;
}
 
.nav-btn--primary:hover {
  background: #0F6E56;
}
 
.nav-btn--outline {
  background: white;
  color: #2C4563;
  border: 2px solid #2C4563;
}
 
.nav-btn--outline:hover {
  background: #2C4563;
  color: white;
}
 
.nav-btn--danger {
  background: #F06030;
  color: white;
  border: none;
}
 
.nav-btn--danger:hover {
  background: #c44c22;
}
</style>