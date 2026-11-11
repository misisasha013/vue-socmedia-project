// Mock API for Phase 2 Likes & Replies
// NEED TO-DO: Replace with real backend API when routes get implemented
// Next Required backend routes:
//- POST /ideas/{id}/like
//- DELETE /ideas/{id}/like
//- GET /ideas/{id}/replies
//- POST /ideas/{id}/replies
//- POST /replies/{id}/like
//- DELETE /replies/{id}/like


//network delay mock


//storage demo purposes
const mockLikes = new Map<number, { count: number; userLiked: boolean }>();
const mockReplies = new Map<number, any[]>();
let replyIdCounter = 100;

export async function likeIdea(ideaId: number) {
  console.log('[MOCK API] POST /ideas/' + ideaId + '/like');
  
  
  const current = mockLikes.get(ideaId) || { count: 0, userLiked: false };
  const updated = { count: current.count + 1, userLiked: true };
  mockLikes.set(ideaId, updated);
  
  return { 
    success: true,
    likeCount: updated.count,
    userLiked: true 
  };
}

//unliking an idea
export async function unlikeIdea(ideaId: number) {
  console.log('[MOCK API] DELETE /ideas/' + ideaId + '/like');
  
  
  const current = mockLikes.get(ideaId) || { count: 1, userLiked: true };
  const updated = { count: Math.max(0, current.count - 1), userLiked: false };
  mockLikes.set(ideaId, updated);
  
  return { 
    success: true,
    likeCount: updated.count,
    userLiked: false 
  };
}

//geting replies
export async function getReplies(ideaId: number) {
  console.log('[MOCK API] GET /ideas/' + ideaId + '/replies');

  
  //return all stored replies or empty array
  return mockReplies.get(ideaId) || [];
}

//creating a reply
export async function createReply(ideaId: number, content: string, parentReplyId: number | null = null) {
  console.log('[MOCK API] POST /ideas/' + ideaId + '/replies', { content, parentReplyId });
  
  
  const newReply = {
    id: replyIdCounter++,
    ideaId,
    content,
    userId: 1,
    userName: "Mock User",
    createdAt: new Date().toISOString(),
    likeCount: 0,
    userLiked: false,
    parentReplyId
  };
  
  const replies = mockReplies.get(ideaId) || [];
  replies.push(newReply);
  mockReplies.set(ideaId, replies);
  //return the reply
  return newReply;
}

//liking a replied post
export async function likeReply(replyId: number) {
  console.log('[MOCK API] POST /replies/' + replyId + '/like');

  
  return { 
    success: true,
    likeCount: Math.floor(Math.random() * 10) + 1,
    userLiked: true 
  };
}

//unliking a replied post
export async function unlikeReply(replyId: number) {
  console.log('[MOCK API] DELETE /replies/' + replyId + '/like');
  
  
  return { 
    success: true,
    likeCount: Math.floor(Math.random() * 10),
    userLiked: false 
  };
}
