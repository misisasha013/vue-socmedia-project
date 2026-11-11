<template>
  <div class="replies-section">
    <h4>{{ replies.length }} {{ replies.length === 1 ? 'Reply' : 'Replies' }}</h4>
    
    <ReplyForm 
      :ideaId="ideaId" 
      @reply-added="addReply"
    />
    
    <div v-if="loading" class="loading">Loading replies...</div>
    
    <div v-else class="replies-list">
      <Reply 
        v-for="reply in topLevelReplies"
        :key="reply.id"
        :reply="reply"
        :ideaId="ideaId"
        :allReplies="replies"
        @reply-added="addReply"
      />
    </div>
    
    <p v-if="!loading && replies.length === 0" class="no-replies">
      No replies yet. Be the first to reply!
    </p>
  </div>
</template>

<script lang="ts">
import Reply from './Reply.vue';
import ReplyForm from './ReplyForm.vue';
import { getReplies } from '../api';

export default {
  name: 'ReplyList',
  components: { Reply, ReplyForm },
  props: {
    ideaId: { type: Number, required: true }
  },
  data() {
    return {
      replies: [] as any[],
      loading: false
    };
  },
  computed: {
    topLevelReplies() {
      return this.replies.filter((r: any) => !r.parentReplyId);
    }
  },
  async mounted() {
    await this.fetchReplies();
  },
  methods: {
    /** Fetches all replies for this message from the backend and updates the list. */
    async fetchReplies() {
      this.loading = true;
      try {
        this.replies = await getReplies(this.ideaId);
      } catch (error) {
        console.error('Error fetching replies:', error);
      } finally {
        this.loading = false;
      }
    },

    /**
    * Called when a new reply is added anywhere in the thread.
    * Re-fetches all replies to keep the list in sync with the backend.
    * @param newReply - The newly created reply object
    */
    async addReply(newReply: any) {
    await this.fetchReplies();  //re-fetch all replies from the backend
    }
  }
};
</script>

<style scoped>
.replies-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

h4 {
  margin: 0 0 1rem 0;
  color: #555;
  font-size: 1rem;
}

.replies-list {
  margin-top: 1rem;
}

.loading {
  color: #666;
  font-style: italic;
  padding: 1rem 0;
}

.no-replies {
  color: #999;
  font-style: italic;
  margin: 1rem 0;
}
</style>