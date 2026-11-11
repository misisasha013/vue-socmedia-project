<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getMessages, updateMessage, deleteMessage, getMessageFiles, getMessageLinks, deleteFile, deleteLink, getCurrentUser } from "../api";
import LikeButton from "./LikeButton.vue";
import ReplyList from "./ReplyList.vue";
import { router, Routes } from "../router";

const props = defineProps<{ id: string }>(); 

type Msg = { 
    id: number; 
    subject: string;
    details: string;
    likeCount: number; 
    userLiked: boolean;
    authorId: number;   
    authorName: string;    
};

const message = ref<Msg | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const editingId = ref<number | null>(null);
const editSubject = ref("");
const editDetails = ref("");
// Now track the logged-in user's id so we can make sure they cant edit/delete others messages
const currentUserId = ref<number>(getCurrentUser());

/**
 * Loads the message matching the route id prop from the full messages list.
 * Throws if the message is not found.
 * Now also fetch current user's id from the session.
 */
async function load() {
    loading.value = true;
    error.value = null;
    try{
      const data = await getMessages();
      const list = data?.data ?? data;
      const found = (Array.isArray(list) ? list : []).find((m: any) => m.id === parseInt(props.id));
      if (!found){
          throw new Error("Message not found");
      }
      message.value = {
          id: found.id,
          subject: found.subject || "",
          details: found.details || "",
          likeCount: found.likeCount || 0,
          userLiked: found.userLiked || false,
          authorId: found.creatorId ?? found.authorId ?? 0,
          authorName: found.name || found.authorName || `User ${found.creatorId ?? "?"}`,
      };
  } catch (e: any) {
      error.value = e?.message ?? String(e);
  } finally {
      loading.value = false;
  }
}


/** 
  * Puts the message into edit mode and populates the draft fields. 
  * Show a popup if current user doesnt own the message
  */
function startEdit(){
    if(!message.value) return;
    editingId.value = message.value.id;
    editSubject.value = message.value.subject;
    editDetails.value = message.value.details;
}

/** Exits edit mode without saving. */
function cancelEdit(){
    editingId.value = null;
}

/**
 * Saves edits to the message and reloads it.
 * Does nothing if the subject field is empty.
 */
async function saveMsg(){
    if(!message.value) return;
    const subject = editSubject.value.trim();
    if(!subject) return;
    loading.value = true;
    try{
        await updateMessage(message.value.id, subject, editDetails.value.trim());
        editingId.value = null;
        await load();
    } catch (e: any){
         error.value = e?.message ?? String(e);
    } finally {
        loading.value = false;
    }
}

/**
 * Deletes the message and all its attached files and links.
 * Files are deleted first to trigger GCS cleanup on the backend.
 * Prompts the user to confirm before deleting, then redirects to the feed.
 * Now will also show popup if the current user doesn't own mesage.
 */
async function removeMsg() {
    if(!message.value) return;
    if (!window.confirm("Delete this message? This will also remove any attached files and links.")) return;
    loading.value = true;
    error.value = null;
    try {
      const msgId = message.value.id;
      // Delte fiels firs that will trigger GCS file on backend
      const files = await getMessageFiles(msgId);
      for (const f of files) await deleteFile(f.id);

      // and delte links
      const links = await getMessageLinks(msgId);
      for (const l of links) await deleteLink(l.id);

      // now delte message
      await deleteMessage(msgId);
      router.push(Routes.messages)
    } catch (e: any) {
      error.value = e?.message ?? String(e);
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
  
  <div style="max-width: 860px; margin: 0 auto;">
    <button
      @click="router.push(Routes.messages)"
      style="
        margin-bottom: 20px;
        padding: 8px 16px;
        background: white;
        color: #2C4563;
        border: 1.5px solid #E4E8EC;
        border-radius: 6px;
        cursor: pointer;
        font-family: Inter, system-ui, sans-serif;
      "
    >← Back</button>

    <p v-if="error" style="color: #A32D2D; font-family: Inter, system-ui, sans-serif;">{{ error }}</p>
    <p v-if="loading" style="color: #8A96A3; font-family: Inter, system-ui, sans-serif;">Loading…</p>

    <div v-if="message" style="
      background: white;
      border: 1.5px solid #E4E8EC;
      border-radius: 10px;
      overflow: hidden;
    ">
      <!-- Header -->
      <div style="padding: 18px 20px 10px; display: flex; justify-content: space-between; align-items: flex-start;">
        <div style="display: flex; align-items: center; gap: 12px;">
          <div style="
            width: 44px; height: 44px; border-radius: 50%;
            background: rgba(44, 69, 99, 0.69);
            display: flex; align-items: center; justify-content: center;
          ">
            <span style="font-weight: 700; font-size: 0.85rem; color: white; font-family: Inter, system-ui, sans-serif;">
              {{ message.authorName.split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0,2) }}
            </span>
          </div>

          <div>
            <div style="font-weight: 700; font-size: 1rem; color: #1A2533; font-family: Inter, system-ui, sans-serif;">
              {{ message.authorName }}
            </div>
            <div style="font-size: 0.8rem; color: #8A96A3; font-family: Inter, system-ui, sans-serif;">
              id: {{ message.id }}
            </div>
          </div>
        </div>

        <!-- Changing Edit/Delete buttons so only shown to the message owner, no popup -->
        <div v-if="editingId !== message.id && message.authorId === currentUserId" style="display: flex; gap: 8px;">
          <button @click="startEdit" :disabled="loading" style="
            padding: 6px 16px; background: white; color: #293B52;
            border: 1px solid #2C4563; border-radius: 6px; cursor: pointer;
            font-family: Inter, system-ui, sans-serif; font-size: 0.85rem;
          ">Edit</button>
          <button @click="removeMsg" :disabled="loading" style="
            padding: 6px 16px; background: white; color: #A32D2D;
            border: 1px solid #F09595; border-radius: 6px; cursor: pointer;
            font-family: Inter, system-ui, sans-serif; font-size: 0.85rem;
          ">Delete</button>
        </div>
      </div>

      <!-- Actual message: view mode -->
      <div v-if="editingId !== message.id" style="padding: 0 20px 14px;">
        <div style="font-weight: 700; font-size: 1rem; color: #1A2533; margin-bottom: 6px; font-family: Inter, system-ui, sans-serif;">
          {{ message.subject }}
        </div>
        <div style="font-size: 0.9rem; color: #4A5568; line-height: 1.5; font-family: Inter, system-ui, sans-serif;">
          {{ message.details }}
        </div>
      </div>

      <!-- Actual Message: edit mode -->
      <div v-else style="padding: 0 20px 14px;">
        <div style="
          width: 100%;
          padding: 8px;
          margin-bottom: 8px;
          border: 1.5px solid #E4E8EC;
          border-radius: 6px;
          font-family: Inter, system-ui, sans-serif;
          box-sizing: border-box;
          color: #1A2533;
          background: white;
          font-size: 0.95rem;
        ">{{ editSubject }}</div>
        <textarea
          v-model="editDetails"
          placeholder="Details"
          rows="3"
          style="
            width: 100%; padding: 8px;
            border: 1.5px solid #0F9E82; border-radius: 6px;
            font-family: Inter, system-ui, sans-serif;
            box-sizing: border-box; resize: vertical;
          "
          @keyup.escape="cancelEdit"
        />
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px;">
          <button @click="cancelEdit" style="
            padding: 6px 14px; background: white; color: #2C4563;
            border: 1.5px solid #E4E8EC; border-radius: 6px;
            cursor: pointer; font-family: Inter, system-ui, sans-serif;
          ">Cancel</button>
          <button @click="saveMsg" :disabled="loading" style="
            padding: 6px 14px; background: #0F9E82; color: white;
            border: none; border-radius: 6px; cursor: pointer;
            font-family: Inter, system-ui, sans-serif; font-weight: 700;
          ">Save</button>
        </div>
      </div>

      <!-- Likes -->
      <div style="
        background: #F0F2F5; padding: 10px 20px;
        border-top: 1px solid #E4E8EC;
      ">
        <LikeButton
          :ideaId="message.id"
          :initialLikeCount="message.likeCount"
          :initialUserLiked="message.userLiked"
        />
      </div>

      <!-- Replies -->
      <div style="background: #F7F8F9; padding: 16px 20px;">
        <ReplyList :ideaId="message.id" />
      </div>

    </div>
  </div>
</template>