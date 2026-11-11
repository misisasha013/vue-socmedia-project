<script setup lang="ts">
import { createMessage, uploadFileToMessage, addLinkToMessage } from "../api";
import { router, Routes } from "../router";
import { ref } from "vue";

const subject = ref("");
const details = ref("");
const loading = ref(false);
const error = ref<string | null>(null);

//Fiel attachemnt state
const attachedFile = ref<File | null>(null);
const isDragging = ref(false);

//Link state
const linkInput = ref("");
const links = ref<string[]>([]);

/** Returns a short file type label (PDF, IMG, VID, AUD, FILE) based on the file's mime type.
 * @param file - The File object to check
 */
function getFileTypeBadge(file:File): string {
    const type = file.type;
    if(type.includes("pdf")){
        return "PDF";
    } 
    if(type.includes("image")){
        return "IMG";
    } 
    if(type.includes("video")){
        return "VID";
    } 
    if(type.includes("audio")){
        return "AUD";
    } 
    return "FILE";
}

/** Formats a file size in bytes to a human-readable KB or MB string.
 * @param bytes - File size in bytes
 */
function formatFileSize(bytes: number): string{
    if (bytes < 1024 * 1024) return `${(bytes /1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Handles the file input change event and sets the selected file.
 * @param e - The input change event
 */
function onFileChange(e: Event){
    const input = e.target as HTMLInputElement;
    if(input.files?.[0]){
        attachedFile.value = input.files[0];
    } 
}

/** Handles a file being dropped onto the drop zone and sets it as the attached file.
 * @param e - The drag drop event
 */
function onDrop (e: DragEvent){
    isDragging.value = false;
    const file = e.dataTransfer?.files?.[0];
    if(file){
        attachedFile.value = file;
    }
}

/** Clears the currently attached file. */
function removeFile(){
    attachedFile.value = null;
}

/**
 * Validates and adds a URL to the links list.
 * Shows an error if the URL doesn't start with http:// or https://.
 */
function addLink(){
    const url = linkInput.value.trim();
    if(!url) return;
    // Simple URL validation (hopfully is enought)
    if(!url.startsWith("http://") && !url.startsWith("https://")){
        error.value = "Link must start with http:// or https://";
        return; 
    }
    links.value.push(url);
    linkInput.value = "";
    error.value = null;
}

/**
 * Removes a link from the links list by index.
 * @param index - The index of the link to remove
 */
function removeLink (index: number){
    links.value.splice(index, 1);
}

/**
 * Submits the new message form. Creates the message first, then uploads
 * the file and links if present. Redirects to the feed on success.
 * Shows an error if subject is empty.
 */
async function submit(){
    const s = subject.value.trim();
    if(!s){
        error.value = "Subject is required.";
        return;
    }
    loading.value = true;
    error.value = null;
    try{ 
        // First, create the message
        const res = await createMessage(s, details.value.trim());
        const newMessageId = res?.data?.id ?? res?.data;

        // Now upload file if attached
        if (attachedFile.value && newMessageId) {
            await uploadFileToMessage(newMessageId, attachedFile.value);
        }

        // And then uplaod each link
        if (newMessageId) {
          for (const url of links.value) {
            await addLinkToMessage(newMessageId, url);
          }
        }
        subject.value = "";
        details.value = "";
        attachedFile.value = null;
        links.value = [];
        router.push(Routes.messages);
    }
    catch(e: any){
        error.value = e?.message ?? String(e);
    }
    finally{
        loading.value = false;
    }
}
</script>

<template>
    <div style="
        background: white;
        border: 1.5px solid #E4E8EC;
        border-radius: 10px;
        padding: 24px;
        margin-bottom: 20px;
        font-family: Inter, system-ui, sans-serif;
    ">
    <h2 style="
        font-size: 1rem;
        font-weight: 700;
        color: #1A2533;
        margin: 0 0 18px 0;
    ">Create a new idea</h2>

    <p v-if="error" style="color: #A32D2D; font-size: 0.85rem; margin-bottom: 10px;">{{ error }}</p>

    <!-- Subject area -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;"> SUBJECT</label>
    <input
        v-model="subject"
        placeholder="Give your idea a title…"
        maxlength="512"
        style="
            width: 100%;
            padding: 10px;
            margin: 6px 0 14px;
            border: 1.5px solid #0F9E82;
            border-radius: 6px;
            font-family: Inter, system-ui, sans-serif;
            font-size: 0.95rem;
            box-sizing: border-box;
            color: #1A2533;
            background: white; 
        "
    />

    <!-- Details -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;">DETAILS</label>
    <textarea
        v-model="details"
        placeholder="Describe your idea in detail…"
        maxlength="1024"
        rows="4"
        style="
            width: 100%;
            padding: 10px;
            margin: 6px 0 14px;
            border: 1.5px solid #0F9E82;
            border-radius: 6px;
            font-family: Inter, system-ui, sans-serif;
            font-size: 0.95rem;
            box-sizing: border-box;
            resize: vertical;
            color: #1A2533;
            background: white; 
        "
    />

    <!-- File drop area -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;">
      ATTACHMENT <span style="font-weight: 400; text-transform: none; letter-spacing: 0;">(optional)</span>
    </label>

    <div
      v-if="!attachedFile"
      @dragover.prevent="isDragging = true"
      @dragleave="isDragging = false"
      @drop.prevent="onDrop"
      @click="($refs.fileInput as HTMLInputElement).click()"
      style="
        margin: 6px 0 14px;
        border: 1.5px dashed #E4E8EC;
        border-radius: 8px;
        padding: 24px;
        text-align: center;
        cursor: pointer;
        background: #F7F8F9;
        transition: border-color 0.2s;
      "
      :style="isDragging ? 'border-color: #0F9E82; background: #E1F5EE;' : ''"
    >
      <input
        ref="fileInput"
        type="file"
        style="display: none;"
        @change="onFileChange"
      />
      <div style="font-size: 0.9rem; color: #8A96A3;">
        Drag & drop a file, or <span style="color: #0F9E82; font-weight: 600;">click to browse</span>
      </div>
      <div style="font-size: 0.75rem; color: #8A96A3; margin-top: 4px;">
        Images, PDFs, slides, video — up to 50 MB
      </div>
    </div>

    <!-- File preview -->
    <div
      v-else
      style="
        margin: 6px 0 14px;
        border: 1.5px solid #E4E8EC;
        border-radius: 8px;
        padding: 12px 16px;
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
      ">{{ getFileTypeBadge(attachedFile) }}</div>
      <div style="flex: 1; min-width: 0;">
        <div style="font-size: 0.85rem; color: #1A2533; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
          {{ attachedFile.name }}
        </div>
        <div style="font-size: 0.75rem; color: #8A96A3;">
          {{ formatFileSize(attachedFile.size) }} · {{ attachedFile.type || "unknown" }}
        </div>
      </div>
      <span
        @click="removeFile"
        style="color: #8A96A3; cursor: pointer; font-size: 1rem; flex-shrink: 0;"
      >✕ Remove</span>
    </div>

    <!-- Link input -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;">
      OR ADD A LINK <span style="font-weight: 400; text-transform: none; letter-spacing: 0;">(optional)</span>
    </label>
    <div style="display: flex; gap: 8px; margin: 6px 0 10px; align-items: stretch ">
      <input
        v-model="linkInput"
        placeholder="https://…"
        @keyup.enter="addLink"
        style="
          flex: 1;
          padding: 10px;
          border: 1.5px solid #0F9E82;
          border-radius: 6px;
          font-family: Inter, system-ui, sans-serif;
          font-size: 0.9rem;
          box-sizing: border-box;
          color: #1A2533;
          background: white; 
        "
      />
      <button
        @click="addLink"
        style="
          height: 60px;
          padding: 0 20px;
          background: #E1F5EE;
          color: #0F6E56;
          border: none;
          border-radius: 6px;
          font-weight: 600;
          cursor: pointer;
          font-family: Inter, system-ui, sans-serif;
          white-space: nowrap;
        "
      >+ Add</button>
    </div>

    <!-- Added links list -->
    <div
    v-for="(link, i) in links"
    :key="i"
    style="
        border-left: 3px solid #0F9E82;
        background: #F0FDF9;
        padding: 8px 12px;
        margin-bottom: 8px;
        border-radius: 0 6px 6px 0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
    "
    >
    <a :href="link"
        target="_blank"
        rel="noopener noreferrer"
        style="
        font-size: 0.85rem;
        color: #0F9E82;
        text-decoration: none;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        "
    >{{ link }}</a>
    <span
        @click="removeLink(i)"
        style="color: #8A96A3; cursor: pointer; flex-shrink: 0; font-size: 0.85rem;"
    >✕</span>
    </div>

    <!-- Buttons -->
    <div style="
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 18px;
      padding-top: 16px;
      border-top: 1px solid #F0F2F5;
    ">
      <button
        @click="router.push(Routes.messages)"
        style="
          padding: 9px 20px;
          background: white;
          color: #2C4563;
          border: 1.5px solid #E4E8EC;
          border-radius: 6px;
          cursor: pointer;
          font-family: Inter, system-ui, sans-serif;
          font-size: 0.9rem;
        "
      >Cancel</button>
      <button
        @click="submit"
        :disabled="loading"
        style="
          padding: 9px 20px;
          background: #0F9E82;
          color: white;
          border: none;
          border-radius: 6px;
          font-weight: 700;
          cursor: pointer;
          font-family: Inter, system-ui, sans-serif;
          font-size: 0.9rem;
        "
      >POST IDEA</button>
    </div>
  </div>
</template>