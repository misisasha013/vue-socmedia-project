<template>
 <!-- Descided to wrap everything in one root div since was broken ealrier -->
  <div class="reply-form-wrapper">
    <div class="reply-input-row">
      <textarea 
        v-model="replyText" 
        placeholder="Write a reply..."
        rows="2"
        class="reply-textarea"
      ></textarea>
    </div>

    <!-- Attachment label, I copied code from MessageCreate to match -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;">
      ATTACHMENT <span style="font-weight: 400; text-transform: none; letter-spacing: 0;">(optional)</span>
    </label>

    <!-- drop zone when no files are attached yet -->
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
      <input ref="fileInput" type="file" style="display: none;" @change="handleFileChange" />
      <div style="font-size: 0.9rem; color: #8A96A3;">
        Drag & drop a file, or <span style="color: #0F9E82; font-weight: 600;">click to browse</span>
      </div>
      <div style="font-size: 0.75rem; color: #8A96A3; margin-top: 4px;">
        Images, PDFs, slides, video — up to 50 MB
      </div>
    </div>

   <!-- File preview card shown once a file is chosen -->
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
          {{ formatFileSize(attachedFile.size) }} · {{ attachedFile.type || 'unknown' }}
        </div>
      </div>
      <span
        @click="attachedFile = null"
        style="color: #8A96A3; cursor: pointer; font-size: 1rem; flex-shrink: 0;"
      >✕ Remove</span>
    </div>

    <!-- Link section -->
    <label style="font-size: 0.7rem; font-weight: 700; color: #8A96A3; letter-spacing: 0.08em;">
      OR ADD A LINK <span style="font-weight: 400; text-transform: none; letter-spacing: 0;">(optional)</span>
    </label>
    <div style="display: flex; gap: 8px; margin: 6px 0 10px;">
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
        type="button"
        @click="addLink"
        style="
          padding: 10px 16px;
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
      <a :href="link" target="_blank" rel="noopener noreferrer" style="
        font-size: 0.85rem;
        color: #0F9E82;
        text-decoration: none;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      ">{{ link }}</a>
      <span
        @click="links.splice(i, 1)"
        style="color: #8A96A3; cursor: pointer; flex-shrink: 0; font-size: 0.85rem;"
      >✕</span>
    </div>
 
    <!-- post button that only shows when there is contnet in the reply now -->
    <div v-if="replyText.trim() || attachedFile || links.length" class="submit-row">
      <button
        @click="submitReply"
        :disabled="!replyText.trim() || loading"
        class="post-btn"
        type="button"
      >
        {{ loading ? 'Posting...' : 'Post Reply' }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { createReply, uploadFileToReply, addLinkToReply } from '../api';

export default {
  name: 'ReplyForm',
  props: {
    ideaId: { type: Number, required: true },
    parentReplyId: { type: Number, default: null }
  },
  data() {
    return {
      replyText: '',
      loading: false,
      attachedFile: null as File | null,
      isDragging: false,
      links: [] as string[],
      linkInput: '',
    };
  },
  methods: {
    /**
    * Submits the reply, then uploads any attached files and links.
    * Emits reply-added on success and resets the form.
    */
    async submitReply() {
      if (!this.replyText.trim()) return;
      
      this.loading = true;
      try {
        const newReply = await createReply(
          this.ideaId, 
          this.replyText, 
          this.parentReplyId
        );

        // Upload files if attached
        if (this.attachedFile) {
          await uploadFileToReply(newReply.id, this.attachedFile);
        }

        // Upload links
        for (const url of this.links) {
            await addLinkToReply(newReply.id, url);
        }

        this.$emit('reply-added', newReply);

        // Reset
        this.replyText = '';
        this.attachedFile = null;
        this.links = [];
      } catch (error) {
        console.error('Error posting reply:', error);
      } finally {
        this.loading = false;
      }
    },

    /** Returns a short file type label (PDF, IMG, VID, AUD, FILE) based on the file's type - copied from MessageCreate.vue
    * @param file - The File object to check
    */
    getFileTypeBadge(file:File): string {
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
    },

    /** Formats a file size in bytes to a human-readable KB or MB string.
    * @param bytes - File size in bytes
    */
    formatFileSize(bytes: number): string{
        if (bytes < 1024 * 1024) return `${(bytes /1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    },

    /**
    * Handles the file input change event and stores the selected files.
    * @param event - The input change event
    */
    handleFileChange(event: Event) {
      const input = event.target as HTMLInputElement;
      if (input.files?.[0]) {
        this.attachedFile = input.files[0];
      }
    },
    
    /** Handles a file being dropped onto the drop zone and sets it as the attached file.
    * @param e - The drag drop event
    */
    onDrop (e: DragEvent){
        this.isDragging = false;
        const file = e.dataTransfer?.files?.[0];
        if(file){
            this.attachedFile = file;
        }
    },

    /** Adds the current link input value to the links list and clears the input. */
    addLink() {
      if (this.linkInput.trim()) {
        this.links.push(this.linkInput.trim());
        this.linkInput = '';
      }
    }
  },
};
</script>

<style scoped>
.reply-form-wrapper {
  margin: 1rem 0;
}
 
.reply-input-row {
  display: flex;
  gap: 0.5rem;
}

.reply-textarea {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #0F9E82;
  border-radius: 4px;
  font-family: inherit;
  resize: vertical;
  background: white; 
  width: 100%;
  box-sizing: border-box;
  color: #1A2533;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
 
.post-btn {
  padding: 6px 16px;
  background: #0F9E82;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-family: Inter, system-ui, sans-serif;
  font-weight: 600;
  font-size: 0.85rem;
}
 
.post-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
 
.post-btn:hover:not(:disabled) {
  background: #0F6E56;
}

</style>