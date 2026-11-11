<template>
  <div class="reply" :class="{ nested: isNested }">
    <div class="reply-header">
      <strong>{{ reply.userName }}</strong>
    </div>
    <p class="reply-content">{{ reply.content }}</p>

    <!-- Change files and links to styled cards that match feed -->
    <div v-if="files.length" class="attachments">
      <div v-for="file in files" :key="file.id" class="file-card">
        <div class="file-badge">
          {{ file.mimeType?.includes('pdf') ? 'PDF' :
             file.mimeType?.includes('image') ? 'IMG' :
             file.mimeType?.includes('video') ? 'VID' : 'FILE' }}
        </div>
        <div class="file-info">
          <div class="file-name">{{ file.fileName }}</div>
          <div class="file-meta">{{ file.mimeType }}</div>
        </div>
        <a :href="file.fileUrl" target="_blank" rel="noopener noreferrer" class="download-btn">
          Download
        </a>
      </div>
    </div>

    <div v-if="links.length" class="links">
      <div v-for="link in links" :key="link.id" class="link-bar">
        <a :href="link.url" target="_blank" rel="noopener noreferrer" class="link-url">
          {{ link.url }}
        </a>
      </div>
    </div>
    
    <div class="reply-actions">
      <LikeButton 
        type="reply"
        :ideaId="reply.id" 
        :initialLikeCount="reply.likeCount || 0"
        :initialUserLiked="reply.userLiked || false"
      />
      <button @click="showReplyForm = !showReplyForm" class="reply-btn">
        {{ showReplyForm ? 'Cancel' : 'Reply' }}
      </button>
    </div>
    
    <ReplyForm 
      v-if="showReplyForm"
      :ideaId="ideaId"
      :parentReplyId="reply.id"
      @reply-added="handleNestedReply"
    />
    
    <!--Nested replies-->
    <div v-if="nestedReplies.length" class="nested-replies">
      <div 
        v-for="nested in nestedReplies"
        :key="nested.id"
        class="nested-reply"
      >
        <div class="reply-header">
          <strong>{{ nested.userName }}</strong>
        </div>
        <p class="reply-content">{{ nested.content }}</p>
        
        <div class="reply-actions">
          <LikeButton 
            type="reply"
            :ideaId="nested.id" 
            :initialLikeCount="nested.likeCount || 0"
            :initialUserLiked="nested.userLiked || false"
          />
        </div>
      </div>
    </div>
    
  </div>
</template>

<script lang="ts">
import LikeButton from './LikeButton.vue';
import ReplyForm from './ReplyForm.vue';
import { getReplyFiles, getReplyLinks } from '../api';

export default {
  name: 'Reply',
  components: { LikeButton, ReplyForm },
  props: {
    reply: { type: Object, required: true },
    ideaId: { type: Number, required: true },
    allReplies: { type: Array as () => any[], default: () => [] },
    isNested: { type: Boolean, default: false }
  },
  data() {
    return {
      showReplyForm: false,
      files: [] as { id: number; fileName: string; fileUrl: string; mimeType: string }[],
      links: [] as { id: number; url: string }[]
    };
  },
  computed: {
    nestedReplies() {
      return this.allReplies.filter((r: any) => r.parentReplyId === this.reply.id);
    }
  },
  
  mounted() {
    this.loadAttachments();
  },

  methods: {
    /**
    * Formats an ISO date string into a localized date and time string.
    * @param date - ISO date string to format
    */
    formatDate(date: string) {
      return new Date(date).toLocaleString();
    },

    /**
    * Handles a nested reply being added. Emits reply-added to the parent
    * and closes the reply form.
    * @param newReply - The newly created reply object
    */
    handleNestedReply(newReply: any) {
      this.$emit('reply-added', newReply);
      this.showReplyForm = false;
    },

    /** Fetches files and links attached to this reply and stores them locally. */
    async loadAttachments() {
      try {
        this.files = await getReplyFiles(this.reply.id);
        this.links = await getReplyLinks(this.reply.id);
      } catch (e) {
        console.error('Error loading attachments:', e);
      }
    }
  }
};
</script>

<style scoped>
.reply {
  border-left: 2px solid #e0e0e0;
  padding-left: 1rem;
  margin: 0.75rem 0;
}

.reply.nested {
  margin-left: 1.5rem;
  border-color: #bbb;
  color: #1A2533;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  color: #1A2533;
}

.reply-content {
  margin: 0.5rem 0;
  color: #1A2533;
}

.reply-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 0.5rem;
}

.reply-btn {
  background: #0F9E82;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 0.85rem;
  cursor: pointer;
  font-family: Inter, system-ui, sans-serif;
  font-weight: 600;
}

.reply-btn:hover {
  background: #0F6E56;
  text-decoration: none;
}

.nested-replies {
  margin-top: 0.5rem;
}

.nested-reply {
  margin-left: 2.5 rem;
  margin-top: 0.75rem;
  padding-left: 1rem;
  border-left: 2px solid #ccc;
}

/* New styles for file cards */
.attachments {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 8px 0;
}
 
.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1.5px solid #E4E8EC;
  border-radius: 8px;
  padding: 8px 12px;
  background: #F7F8F9;
}
 
.file-badge {
  background: #E1F5EE;
  color: #0F6E56;
  font-weight: 700;
  font-size: 0.65rem;
  padding: 3px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
 
.file-info {
  flex: 1;
  min-width: 0;
}
 
.file-name {
  font-size: 0.82rem;
  color: #1A2533;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
 
.file-meta {
  font-size: 0.72rem;
  color: #8A96A3;
}
 
.download-btn {
  padding: 4px 12px;
  border: 1.5px solid #E4E8EC;
  border-radius: 6px;
  background: white;
  color: #2C4563;
  font-size: 0.78rem;
  font-weight: 600;
  text-decoration: none;
  flex-shrink: 0;
}
 
.download-btn:hover {
  border-color: #0F9E82;
  color: #0F6E56;
}
 
/* new styles for link bar */
.links {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin: 8px 0;
}
 
.link-bar {
  border-left: 3px solid #0F9E82;
  background: #F0FDF9;
  padding: 6px 10px;
  border-radius: 0 6px 6px 0;
}
 
.link-url {
  font-size: 0.82rem;
  color: #0F9E82;
  text-decoration: none;
  word-break: break-all;
}
 
.link-url:hover {
  text-decoration: underline;
}

</style>