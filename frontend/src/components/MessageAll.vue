<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getMessages, updateMessage, deleteMessage, getMessageFiles, getMessageLinks, deleteFile, deleteLink, getCurrentUser } from "../api";
import LikeButton from "./LikeButton.vue";
import ReplyList from "./ReplyList.vue";
import { router, Routes } from "../router";

type Msg = { 
  id: number; 
  subject: string;
  details: string;
  likeCount: number; 
  replyCount: number;
  userLiked: boolean;
  authorId: number;       
  authorName: string;
  files: { id: number; fileName: string; fileUrl: string; mimeType: string; }[];
  links: { id: number; url: string; }[];
};

const messages = ref<Msg[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const editingId = ref<number | null>(null); // To track which message is cur being edited
const editSubject = ref(""); // Tracking draft subject
const editDetails = ref(""); // And trackign draft text
const expandedReplies = ref<Set<number>>(new Set()); // // To track which message cards have replies expanded
// Now track the logged-in user's id so we can make sure they cant edit/delete others messages
const currentUserId = ref<number>(getCurrentUser());

/** Returns up to 2 uppercase initials from a full name for the avatar display. */
function getInitials(name: string): string {
  return name
    .split(" ")
    .map(w => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

/** Returns a short file type label (PDF, IMG, VID, AUD, FILE) based on the mime type. */
function getMimeBadge(mime: string): string {
  if (!mime) return "FILE";
  if (mime.includes("pdf"))   return "PDF";
  if (mime.includes("image")) return "IMG";
  if (mime.includes("video")) return "VID";
  if (mime.includes("audio")) return "AUD";
  return "FILE";
}

/**
 * Toggles the reply section open or closed for a message card.
 * Reassigns the Set to trigger Vue reactivity.
 * @param id - The message ID to toggle
 */
function toggleReplies(id: number) {
  if (expandedReplies.value.has(id)) {
    expandedReplies.value.delete(id);
  } else {
    expandedReplies.value.add(id);
  }
  // trigger reactivity
  expandedReplies.value = new Set(expandedReplies.value);
}

/**
 * Maps raw API response data to the Msg type used by this component.
 * Handles both wrapped ({ data: [...] }) and unwrapped array responses.
 * @param data - Raw response from getMessages()
 */
function normalizeList(data: any): Msg[] {
  const unwrapped = data?.data ?? data;
  const list = Array.isArray(unwrapped) ? unwrapped : [];
  return list.map((m: any) => ({
    id: m.id,
    subject: m.subject || "",
    details: m.details || "",
    likeCount: m.likeCount || 0,
    replyCount: m.replyCount || 0,
    userLiked: m.userLiked || false,
    authorId: m.creatorId ?? m.authorId ?? 0,
    authorName: m.name || m.authorName || `User ${m.creatorId ?? m.authorId ?? "?"}`,
    files: [],
    links: [],
  })).sort((a, b) => b.id - a.id); // to sort message by lastest ID
}

/**
 * Fetches all messages and their files and links, then updates the feed.
 * File and link fetches run in parallel per message. If they fail for a
 * single message, that message still renders without attachments.
 */
async function refresh() {
  loading.value = true;
  error.value = null;
  try {
    const data = await getMessages();
    const base = normalizeList(data);
    // Fetch files and links for each message at same time
    messages.value = await Promise.all(
      base.map(async (m) => {
        try {
          const [files, links] = await Promise.all([
            getMessageFiles(m.id),
            getMessageLinks(m.id),
          ]);

          // Dont think i need these since they are already unwarpped and maped in api.ts
          // const files = filesRes?.data ?? filesRes ?? [];
          //const links = linksRes?.data ?? linksRes ?? [];

          return { ...m, files, links };
        } catch {
          // If files/links fail, don't break the whole feed
          return { ...m, files: [], links: [] };
        }
      })
    );
  } catch (e: any) {
    error.value = e?.message ?? String(e);
  } finally {
    loading.value = false;
  }
}

/**
 * Puts a message card into edit mode and populates the draft fields.
 * @param m - The message to edit
 */
function startEdit(m: Msg){
    editingId.value = m.id;
    editSubject.value = m.subject;
    editDetails.value = m.details;
}

/** Exits edit mode without saving. */
function cancelEdit(){
    editingId.value = null;
}

/**
 * Saves edits to a message and refreshes the feed.
 * Does nothing if the subject field is empty.
 * @param m - The message being edited
 */
async function saveMsg(m: Msg){
    const subject = editSubject.value.trim();
    if(!subject) return;
    loading.value = true;
    try{
        await updateMessage(m.id, subject, editDetails.value.trim());
        editingId.value = null;
        await refresh();
    } catch (e: any){
         error.value = e?.message ?? String(e);
    } finally {
        loading.value = false;
    }
}

/**
 * Deletes a message and all its attached files and links.
 * Files are deleted first to trigger GCS cleanup on the backend.
 * Prompts the user to confirm before deleting.
 * @param id - The message ID to delete
 */
async function removeMsg(id: number) {
  if (!window.confirm("Delete this message? This will also remove any attached files and links.")) return;
  loading.value = true;
  try {
    // Delte fiels firs that will trigger GCS file on backend
    const files = await getMessageFiles(id);
    for (const f of files) await deleteFile(f.id);

    // and delte links
    const links = await getMessageLinks(id);
    for (const l of links) await deleteLink(l.id);

    // now delte message
    await deleteMessage(id);
    await refresh();
  } catch (e: any) {
    error.value = e?.message ?? String(e);
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
</script>


<template>
  <div style="max-width: 860px; margin: 0 auto;">

    <p style="
      font-family: Inter, system-ui, sans-serif;
      font-weight: 700;
      font-size: 0.75rem;
      letter-spacing: 0.08em;
      color: #8A96A3;
      margin-bottom: 16px;
    ">LATEST IDEAS</p>
    <p v-if="error" style="color: #A32D2D;">{{ error }}</p>

    <p v-if="loading" style="color: #8A96A3; font-family: Inter, system-ui, sans-serif;"> Loading… </p>

    <!-- Actual message cards -->
    <div
      v-for="m in messages"
      :key="m.id"
      style="
        background: white;
        border: 1.5px solid #E4E8EC;
        border-radius: 10px;
        margin-bottom: 16px;
        overflow: hidden;
      "
    >
      <!-- Top of card, like header so avatoar and auther name and time-->
      <div style="padding: 18px 20px 10px; display: flex; justify-content: space-between; align-items: flex-start;">
        <div style="display: flex; align-items: center; gap: 12px;">
          <!-- Avatar of initials -->
          <div style="
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: rgba(44, 69, 99, 0.69);
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
          ">
            <span style="
              font-family: Inter, system-ui, sans-serif;
              font-weight: 700;
              font-size: 0.85rem;
              color: #2C4563;
            ">{{ getInitials(m.authorName) }}</span>
          </div>
          <div>
            <div style="
              font-family: Inter, system-ui, sans-serif;
              font-weight: 700;
              font-size: 1rem;
              color: #1A2533;
            ">{{ m.authorName }}</div>
            <div style="
              font-family: Inter, system-ui, sans-serif;
              font-size: 0.8rem;
              color: #8A96A3;
            ">id: {{ m.id }}</div>
          </div>
        </div>

        <!-- Buttons: View is always shown and Edit/Delete only for creator -->
        <div style="display: flex; gap: 8px; align-items: center;">
          <button
            @click="router.push(`/messages/${m.id}`)"
            style="
              padding: 6px 16px;
              background: white;
              color: #0F9E82;
              border: 1px solid #0F9E82;
              border-radius: 6px;
              cursor: pointer;
              font-family: Inter, system-ui, sans-serif;
              font-size: 0.85rem;
              font-weight: 600;
            "
          >View</button>
          <!-- Edit/Delete buttons -->
          <template v-if="editingId !== m.id && m.authorId === currentUserId">
            <button
              @click="startEdit(m)"
              :disabled="loading"
              style="
                padding: 6px 16px;
                background: white;
                color: #293B52;
                border: 1px solid #2C4563;
                border-radius: 6px;
                cursor: pointer;
                font-family: Inter, system-ui, sans-serif;
                font-size: 0.85rem;
              "
            >Edit</button>
            <button
              @click="removeMsg(m.id)"
              :disabled="loading"
              style="
                padding: 6px 16px;
                background: white;
                color: #A32D2D;
                border: 1px solid #F09595;
                border-radius: 6px;
                cursor: pointer;
                font-family: Inter, system-ui, sans-serif;
                font-size: 0.85rem;
              "
            >Delete</button>
          </template>
        </div>
      </div>

      <!-- Actual info on card: view mode -->
      <div v-if="editingId !== m.id" style="padding: 0 20px 14px;">
        <div style="
          font-family: Inter, system-ui, sans-serif;
          font-weight: 700;
          font-size: 1rem;
          color: #1A2533;
          margin-bottom: 6px;
        ">{{ m.subject }}</div>
        <div style="
          font-family: Inter, system-ui, sans-serif;
          font-size: 0.9rem;
          color: #4A5568;
          line-height: 1.5;
        ">{{ m.details }}</div>
      </div>

      <!-- Actual info on card: edit mode -->
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
          background: #F7F8F9;
          font-size: 0.95rem;
        ">{{ editSubject }}</div>
        <textarea
          v-model="editDetails"
          placeholder="Details"
          rows="2"
          style="
            width: 100%;
            padding: 8px;
            border: 1.5px solid #0F9E82;
            border-radius: 6px;
            font-family: Inter, system-ui, sans-serif;
            box-sizing: border-box;
            resize: vertical;
            color: #1A2533;
            background: white;
          "
          @keyup.escape="cancelEdit"
        />
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px;">
          <button @click="cancelEdit" style="
            padding: 6px 14px;
            background: white;
            color: #2C4563;
            border: 1.5px solid #E4E8EC;
            border-radius: 6px;
            cursor: pointer;
            font-family: Inter, system-ui, sans-serif;
          ">Cancel</button>
          <button @click="saveMsg(m)" :disabled="loading" style="
            padding: 6px 14px;
            background: #0F9E82;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-family: Inter, system-ui, sans-serif;
            font-weight: 700;
          ">Save</button>
        </div>
      </div>

      <!-- File attachments -->
      <div v-if="m.files && m.files.length" style="padding: 0 20px 10px;">
        <div
          v-for="f in m.files"
          :key="f.id"
          style="
            border: 1.5px solid #E4E8EC;
            border-radius: 8px;
            padding: 10px 14px;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 12px;
            background: #F7F8F9;
          "
        >
          <div style="
            background: #E1F5EE;
            color: #0F6E56;
            font-weight: 700;
            font-size: 0.7rem;
            padding: 4px 8px;
            border-radius: 4px;
            flex-shrink: 0;
          ">
            {{ getMimeBadge(f.mimeType) }}
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="
              font-size: 0.85rem;
              color: #1A2533;
              font-weight: 500;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            ">{{ f.fileName }}</div>
            <div style="font-size: 0.75rem; color: #8A96A3;">{{ f.mimeType }}</div>
          </div>
          <a
            :href="f.fileUrl"
            target="_blank"
            rel="noopener noreferrer"
            style="
              padding: 6px 14px;
              background: white;
              color: #2C4563;
              border: 1.5px solid #E4E8EC;
              border-radius: 6px;
              font-size: 0.8rem;
              text-decoration: none;
              flex-shrink: 0;
              font-family: Inter, system-ui, sans-serif;
              font-weight: 600;
            "
          >Download</a>
        </div>
      </div>

      <!-- Links -->
      <div v-if="m.links && m.links.length" style="padding: 0 20px 10px;">
        <div
          v-for="l in m.links"
          :key="l.id"
          style="
            border-left: 3px solid #0F9E82;
            background: #F0FDF9;
            padding: 8px 12px;
            margin-bottom: 8px;
            border-radius: 0 6px 6px 0;
          "
        > 
        <a
            :href="l.url"
            target="_blank"
            rel="noopener noreferrer"
            style="
              font-size: 0.85rem;
              color: #0F9E82;
              text-decoration: none;
              word-break: break-all;
            "
          >{{ l.url }}</a>
        </div>
      </div>

      <!-- Likes and replies bar -->
      <div style="
        background: #F0F2F5;
        padding: 10px 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #E4E8EC;
      ">
        <LikeButton
          :ideaId="m.id"
          :initialLikeCount="m.likeCount"
          :initialUserLiked="m.userLiked"
        />
        <span
          @click="toggleReplies(m.id)"
          style="
            font-family: Inter, system-ui, sans-serif;
            font-size: 0.85rem;
            color: #0F9E82;
            cursor: pointer;
            font-weight: 600;
            user-select: none;
          "
          >
          {{ expandedReplies.has(m.id) ? '▾' : '›' }}
          {{ m.replyCount }} {{ m.replyCount === 1 ? 'reply' : 'replies' }}
        </span>
      </div>

      <!-- Replies -->
      <div v-if="expandedReplies.has(m.id)" style="background: #F7F8F9; padding: 16px 20px;">
        <ReplyList :ideaId="m.id" />
      </div>
    </div>

    <p v-if="!loading && !messages.length" style="
      color: #8A96A3;
      font-family: Inter, system-ui, sans-serif;
      text-align: center;
      margin-top: 40px;
    ">No ideas yet. Be the first!</p>
  </div>
</template>