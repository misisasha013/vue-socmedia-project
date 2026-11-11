<template>
  <div class="like-container">
    <button 
      @click="toggleLike" 
      :class="{ liked: userLiked }"
      class="like-btn"
      :disabled="loading"
    >
      {{ userLiked ? '♥️' : '🤍' }}
    </button>
    <span class="like-count">{{ likeCount }} {{ likeCount === 1 ? 'like' : 'likes' }}</span>
  </div>
</template>

<script lang="ts">
import { likeMessage, unlikeMessage, likeReply, unlikeReply } from '../api';

export default {
  name: 'LikeButton',
  props: {
    ideaId: { 
      type: Number, 
      required: true 
    },
    initialLikeCount: { 
      type: Number, 
      default: 0 
    },
    initialUserLiked: { 
      type: Boolean, 
      default: false 
    },
    type: {
      type: String,
      default: 'message',
      validator: (value: string) => ['message', 'reply'].includes(value)
    }
  },
  data() {
    return {
      likeCount: this.initialLikeCount,
      userLiked: this.initialUserLiked,
      loading: false
    };
  },
  methods: {
    /**
    * Toggles the like state for a message or reply.
    * Does nothing if ideaId is invalid. Calls the appropriate
    * like or unlike API function based on the type prop.
    */
    async toggleLike() {
      // ADD THESE 4 LINES AT THE TOP
      if (!this.ideaId || isNaN(this.ideaId)) {
        console.warn('Invalid ideaId:', this.ideaId);
        return;
      }
      
      this.loading = true;
      try {
        let data;
        
        if (this.type === 'reply') {
          //reply like/unlike functions
          data = this.userLiked 
            ? await unlikeReply(this.ideaId)
            : await likeReply(this.ideaId);
        } else {
          //message like/unlike functions
          data = this.userLiked 
            ? await unlikeMessage(this.ideaId)
            : await likeMessage(this.ideaId);
        }
        
        console.log('Like response:', data);
        
        //handling response
        this.likeCount = data?.likeCount ?? data?.data?.likeCount ?? 0;
        this.userLiked = data?.userLiked ?? data?.data?.userLiked ?? !this.userLiked;
      } catch (error) {
        console.error('Error toggling like:', error);
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.like-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.like-btn {
  border: none;
  background: none;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0.25rem;
  transition: transform 0.1s;
}

.like-btn:hover:not(:disabled) {
  transform: scale(1.1);
}

.like-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.like-btn.liked {
  animation: pulse 0.3s ease;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.like-count {
  font-size: 0.9rem;
  color: #666;
}
</style>