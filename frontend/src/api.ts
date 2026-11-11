const BASE = import.meta.env.VITE_API_BASE_URL || '/api';

const FETCH_OPTS = { credentials: "include" as RequestCredentials };

/** Fetches all messages from the backend. Raw response is returned and handled by normalizeList in the component. */
export async function getMessages() {
  const res = await fetch(`${BASE}/messages`, FETCH_OPTS);
  if (!res.ok) throw new Error(`GET /messages failed: ${res.status}`);
  return res.json(); // return raw data and normalizeList will handle the mapping
}

/**
 * Creates a new message.
 * @param subject - The title of the message (max 512 chars)
 * @param details - The body of the message (max 1024 chars)
 */
export async function createMessage(subject: string, details: string) {
  const res = await fetch(`${BASE}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ subject, details }),
  });

  if (!res.ok) throw new Error(`POST /messages failed: ${res.status}`);
  return res.json();
}

/**
 * Updates an existing message. Only the owner can update their message.
 * @param id - The message ID to update
 * @param subject - The new subject
 * @param details - The new details
 */
export async function updateMessage(id: number, subject: string, details: string) {
  const res = await fetch(`${BASE}/messages/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ subject, details }),
  });

  if (!res.ok) throw new Error(`PUT /messages/${id} failed: ${res.status}`);
  return res.json();
}
 
/**
 * Deletes a message by ID. Only the owner can delete their message.
 * Note: call deleteFile and deleteLink for all attachments first.
 * @param id - The message ID to delete
 */
export async function deleteMessage(id: number) {
  const res = await fetch(`${BASE}/messages/${id}`, {
    method: "DELETE",
    credentials: "include",
  });

  if (!res.ok) throw new Error(`DELETE /messages/${id} failed: ${res.status}`);
  return res.json();
}

/**
 * Likes a message. Has no effect if the user has already liked it.
 * @param messageId - The message to like
 */
export async function likeMessage(messageId: number) {
  const res = await fetch(`${BASE}/messages/${messageId}/like`, {
    method: "POST",
    credentials: "include",
  });

  if (!res.ok) throw new Error(`POST /messages/${messageId}/like failed: ${res.status}`);
  const result = await res.json();
  return result.data;
}

/**
 * Unlikes a message.
 * @param messageId - The message to unlike
 */
export async function unlikeMessage(messageId: number) {
  const res = await fetch(`${BASE}/messages/${messageId}/like`, {
    method: "DELETE",
    credentials: "include",
  });

  if (!res.ok) throw new Error(`DELETE /messages/${messageId}/like failed: ${res.status}`);
  const result = await res.json();
  return result.data;
}
 
/**
 * Fetches all replies for a given message. Maps backend fields to frontend shape.
 * @param messageId - The message whose replies to fetch
 */
export async function getReplies(messageId: number) {
  const res = await fetch(`${BASE}/messages/${messageId}/replies`, FETCH_OPTS);

  if (!res.ok) throw new Error(`GET /messages/${messageId}/replies failed: ${res.status}`);

  const result = await res.json();
  const replies = result.data || [];

  return replies.map((r: any) => ({
    id: r.id,
    content: r.details || r.content || '',
    userId: r.creatorId,
    userName: r.name || 'Unknown', // ← replies still OK to keep
    createdAt: r.as_of || new Date().toISOString(),
    likeCount: r.likeCount || 0,
    userLiked: r.userLiked || false,
    parentReplyId: null,
    messageId: r.messageId || messageId
  }));
}

/**
 * Creates a new reply on a message.
 * @param messageId - The message to reply to
 * @param content - The reply text
 * @param parentReplyId - If replying to another reply, its ID; otherwise null
 */
export async function createReply(
  messageId: number,
  content: string,
  parentReplyId: number | null = null
) {
  const res = await fetch(`${BASE}/messages/${messageId}/replies`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ content, parentReplyId }),
  });

  if (!res.ok) throw new Error(`POST /messages/${messageId}/replies failed: ${res.status}`);

  const result = await res.json();

  return {
    id: parseInt(result.data),
    messageId,
    content,
    userId: 1,
    userName: "You",
    createdAt: new Date().toISOString(),
    likeCount: 0,
    userLiked: false,
    parentReplyId
  };
}

/**
 * Likes a reply.
 * @param replyId - The reply to like
 */
export async function likeReply(replyId: number) {
  const res = await fetch(`${BASE}/replies/${replyId}/like`, {
    method: "POST",
    credentials: "include",
  });

  if (!res.ok) throw new Error(`POST /replies/${replyId}/like failed: ${res.status}`);
  const result = await res.json();
  return result.data;
}

/**
 * Unlikes a reply.
 * @param replyId - The reply to unlike
 */
export async function unlikeReply(replyId: number) {
  const res = await fetch(`${BASE}/replies/${replyId}/like`, {
    method: "DELETE",
    credentials: "include",
  });

  if (!res.ok) throw new Error(`DELETE /replies/${replyId}/like failed: ${res.status}`);
  const result = await res.json();
  return result.data;
}

/**
 * Uploads a file attachment to a message. Converts the file to base64 before sending.
 * @param messageId - The message to attach the file to
 * @param file - The File object to upload
 */
export async function uploadFileToMessage(messageId: number, file: File) {
  const base64 = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve((reader.result as string)?.split(",")[1] ?? "");
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

  const res = await fetch(`${BASE}/messages/${messageId}/files`,{
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({
      fileName: file.name,
      mimeType: file.type,
      fileData: base64,
    }),
  });
  if(!res.ok) throw new Error(`POST /messages/${messageId}/files failed: ${res.status}`);
  return res.json();
};

/**
 * Adds a link to a message.
 * @param messageId - The message to add the link to
 * @param url - The URL to attach
 */
export async function addLinkToMessage(messageId: number, url: string) {
  const res = await fetch(`${BASE}/messages/${messageId}/links`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ url }),
  });
  if (!res.ok) throw new Error(`POST /messages/${messageId}/links failed: ${res.status}`);
  return res.json();
};

/**
 * Uploads a file attachment to a reply. Converts the file to base64 before sending.
 * @param replyId - The reply to attach the file to
 * @param file - The File object to upload
 */
export async function uploadFileToReply(replyId: number, file: File) {
  const base64 = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve((reader.result as string).split(",")[1]?? "");
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

  const res = await fetch(`${BASE}/replies/${replyId}/files`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({
      fileName: file.name,
      mimeType: file.type,
      fileData: base64,
    }),
  });
  if (!res.ok) throw new Error(`POST /replies/${replyId}/files failed: ${res.status}`);
  return res.json();
}

/**
 * Adds a link to a reply.
 * @param replyId - The reply to add the link to
 * @param url - The URL to attach
 */
export async function addLinkToReply(replyId: number, url: string) {
  const res = await fetch(`${BASE}/replies/${replyId}/links`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ url }),
  });
  if (!res.ok) throw new Error(`POST /replies/${replyId}/links failed: ${res.status}`);
  return res.json();
}

/**
 * Fetches all files attached to a message. Maps backend snake_case fields to camelCase.
 * @param messageId - The message whose files to fetch
 */
export async function getMessageFiles(messageId: number) {
  const res = await fetch(`${BASE}/messages/${messageId}/files`, FETCH_OPTS);
  if (!res.ok) throw new Error(`GET /messages/${messageId}/files failed: ${res.status}`);
  const result = await res.json();
  return (result.data || []).map((f: any) => ({
    id: f.id,
    fileName: f.file_name || f.fileName,
    fileUrl: f.file_url || f.fileUrl,
    mimeType: f.mime_type || f.mimeType,

  }));
};

/**
 * Fetches all links attached to a message.
 * @param messageId - The message whose links to fetch
 */
export async function getMessageLinks(messageId: number) {
  const res = await fetch(`${BASE}/messages/${messageId}/links`, FETCH_OPTS);
  if (!res.ok) throw new Error(`GET /messages/${messageId}/links failed: ${res.status}`);
  const result = await res.json();
  return (result.data || []).map((l: any) => ({
    id: l.id,
    url: l.url,
  }));
}

/**
 * Fetches all files attached to a reply. Maps backend snake_case fields to camelCase.
 * @param replyId - The reply whose files to fetch
 */
export async function getReplyFiles(replyId: number) {
  const res = await fetch(`${BASE}/replies/${replyId}/files`, FETCH_OPTS);
  if (!res.ok) throw new Error(`GET /replies/${replyId}/files failed`);
  const result = await res.json();

  return (result.data || []).map((f: any) => ({
    id: f.id,
    fileName: f.file_name || f.fileName,
    fileUrl: f.file_url || f.fileUrl,
    mimeType: f.mime_type || f.mimeType,
  }));
}

/**
 * Fetches all links attached to a reply.
 * @param replyId - The reply whose links to fetch
 */
export async function getReplyLinks(replyId: number) {
  const res = await fetch(`${BASE}/replies/${replyId}/links`, FETCH_OPTS);
  if (!res.ok) throw new Error(`GET /replies/${replyId}/links failed`);
  const result = await res.json();

  return (result.data || []).map((l: any) => ({
    id: l.id,
    url: l.url,
  }));
}

/**
 * Deletes a file by ID. Also triggers cleanup of the file in Google Cloud Storage on the backend.
 * @param fileID - The file ID to delete
 */
export async function deleteFile(fileID: number){
  const res = await fetch(`${BASE}/files/${fileID}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) throw new Error(`DELETE /files/${fileID} failed: ${res.status}`);
  return res.json();
}

/**
 * Deletes a link by ID.
 * @param linkID - The link ID to delete
 */
export async function deleteLink(linkID: number){
  const res = await fetch(`${BASE}/links/${linkID}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) throw new Error(`DELETE /link/${linkID} failed: ${res.status}`);
  return res.json();
}  

/** Gets the current user's ID from the auth cookie set at login. */
export function getCurrentUser(): number {
  const raw = document.cookie
    .split('; ')
    .find(row => row.startsWith('auth.id='))
    ?.split('=')[1];
  return raw ? parseInt(raw) : 0;
}