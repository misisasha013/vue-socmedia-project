// Used AI to create tests. Specifically used Claude.ai and provided most of my current compoenents to create proper unit test

import { mount, flushPromises } from '@vue/test-utils';
import { vi } from 'vitest';

// Mock router
vi.mock('../router', () => ({
  router: { push: vi.fn() },
  Routes: { messages: '/messages', readPersonAll: '/pa', messageNew: '/messages/new' },
}));

// Mock all API calls
vi.mock('./api', () => ({
  likeMessage:         vi.fn().mockResolvedValue({ likeCount: 1, userLiked: true }),
  unlikeMessage:       vi.fn().mockResolvedValue({ likeCount: 0, userLiked: false }),
  likeReply:           vi.fn().mockResolvedValue({ likeCount: 1, userLiked: true }),
  unlikeReply:         vi.fn().mockResolvedValue({ likeCount: 0, userLiked: false }),
  getMessages:         vi.fn().mockResolvedValue({ data: [
    { id: 1, subject: 'Test Idea', details: 'Some details', likeCount: 2, replyCount: 3, userLiked: false, creatorId: 1, name: 'Jack Lamb' }
  ]}),
  getMessageFiles:     vi.fn().mockResolvedValue([]),
  getMessageLinks:     vi.fn().mockResolvedValue([]),
  createMessage:       vi.fn().mockResolvedValue({ data: '42' }),
  updateMessage:       vi.fn().mockResolvedValue({}),
  deleteMessage:       vi.fn().mockResolvedValue({}),
  deleteFile:          vi.fn().mockResolvedValue({}),
  deleteLink:          vi.fn().mockResolvedValue({}),
  uploadFileToMessage: vi.fn().mockResolvedValue({}),
  addLinkToMessage:    vi.fn().mockResolvedValue({}),
  createReply:         vi.fn().mockResolvedValue({ id: 99, content: 'reply', parentReplyId: null }),
  getReplies:          vi.fn().mockResolvedValue([
    { id: 10, content: 'A reply', userName: 'Julia', createdAt: new Date().toISOString(), likeCount: 0, userLiked: false, parentReplyId: null }
  ]),
  uploadFileToReply:   vi.fn().mockResolvedValue({}),
  addLinkToReply:      vi.fn().mockResolvedValue({}),
  getReplyFiles:       vi.fn().mockResolvedValue([]),
  getReplyLinks:       vi.fn().mockResolvedValue([]),
}));

import LikeButton    from './components/LikeButton.vue';
import MessageCreate from './components/MessageCreate.vue';
import MessageAll    from './components/MessageAll.vue';
import ReplyForm     from './components/ReplyForm.vue';
import Reply         from './components/Reply.vue';
import ReplyList     from './components/ReplyList.vue';

// ── LikeButton ────────────────────────────────────────────────────────────────
describe('LikeButton', () => {
  it('renders like count', () => {
    const wrapper = mount(LikeButton, {
      props: { ideaId: 1, initialLikeCount: 5, initialUserLiked: false }
    });
    expect(wrapper.text()).toContain('5 likes');
  });

  it('shows unliked heart when not liked', () => {
    const wrapper = mount(LikeButton, {
      props: { ideaId: 1, initialUserLiked: false }
    });
    expect(wrapper.text()).toContain('🤍');
  });

  it('shows liked heart when liked', () => {
    const wrapper = mount(LikeButton, {
      props: { ideaId: 1, initialUserLiked: true }
    });
    expect(wrapper.text()).toContain('♥️');
  });

  it('button exists', () => {
    const wrapper = mount(LikeButton, { props: { ideaId: 1 } });
    expect(wrapper.find('button').exists()).toBe(true);
  });
});

// ── MessageCreate ─────────────────────────────────────────────────────────────
describe('MessageCreate', () => {
  it('renders the form heading', () => {
    const wrapper = mount(MessageCreate);
    expect(wrapper.text()).toContain('Create a new idea');
  });

  it('shows error when submitting with no subject', async () => {
    const wrapper = mount(MessageCreate);
    // find the POST IDEA button by text
    const btn = wrapper.findAll('button').find(b => b.text() === 'POST IDEA');
    await btn!.trigger('click');
    expect(wrapper.text()).toContain('Subject is required');
  });

  it('rejects a link that does not start with http', async () => {
    const wrapper = mount(MessageCreate);
    const inputs = wrapper.findAll('input');
    await inputs[inputs.length - 1].setValue('not-a-url');
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('Add'));
    await addBtn!.trigger('click');
    expect(wrapper.text()).toContain('Link must start with http');
  });

  it('adds a valid link to the list', async () => {
    const wrapper = mount(MessageCreate);
    const inputs = wrapper.findAll('input');
    await inputs[inputs.length - 1].setValue('https://example.com');
    const addBtn = wrapper.findAll('button').find(b => b.text().includes('Add'));
    await addBtn!.trigger('click');
    expect(wrapper.text()).toContain('https://example.com');
  });
});

// ── MessageAll ────────────────────────────────────────────────────────────────
describe('MessageAll', () => {
  it('renders the LATEST IDEAS heading', () => {
    const wrapper = mount(MessageAll);
    expect(wrapper.text()).toContain('LATEST IDEAS');
  });

  it('loads and shows messages after mount', async () => {
    const wrapper = mount(MessageAll);
    await flushPromises();
    expect(wrapper.text()).toContain('Test Idea');
    expect(wrapper.text()).toContain('Jack Lamb');
  });

  it('shows reply count on card', async () => {
    const wrapper = mount(MessageAll);
    await flushPromises();
    expect(wrapper.text()).toContain('3 replies');
  });

  it('does not show replies until toggle is clicked', async () => {
    const wrapper = mount(MessageAll);
    await flushPromises();
    expect(wrapper.findComponent({ name: 'ReplyList' }).exists()).toBe(false);
  });
});

// ── ReplyForm ─────────────────────────────────────────────────────────────────
describe('ReplyForm', () => {
  it('renders textarea', () => {
    const wrapper = mount(ReplyForm, { props: { ideaId: 1 } });
    expect(wrapper.find('textarea').exists()).toBe(true);
  });

  it('hides Post Reply button when textarea is empty', () => {
    const wrapper = mount(ReplyForm, { props: { ideaId: 1 } });
    expect(wrapper.text()).not.toContain('Post Reply');
  });

  it('shows Post Reply button when textarea has content', async () => {
    const wrapper = mount(ReplyForm, { props: { ideaId: 1 } });
    await wrapper.find('textarea').setValue('hello');
    expect(wrapper.text()).toContain('Post Reply');
  });

  it('shows link input when + Link is clicked', async () => {
    const wrapper = mount(ReplyForm, { props: { ideaId: 1 } });
    const linkBtn = wrapper.findAll('button').find(b => b.text().includes('Link'));
    await linkBtn!.trigger('click');
    expect(wrapper.find('input.link-input').exists()).toBe(true);
  });

  it('emits reply-added after submit', async () => {
    const wrapper = mount(ReplyForm, { props: { ideaId: 1 } });
    await wrapper.find('textarea').setValue('my reply');
    await wrapper.find('button.post-btn').trigger('click');
    await flushPromises();
    expect(wrapper.emitted('reply-added')).toBeTruthy();
  });
});

// ── Reply ─────────────────────────────────────────────────────────────────────
describe('Reply', () => {
  const reply = {
    id: 10,
    content: 'This is a reply',
    userName: 'Julia',
    createdAt: new Date().toISOString(),
    likeCount: 2,
    userLiked: false,
  };

  it('renders reply content', () => {
    const wrapper = mount(Reply, { props: { reply, ideaId: 1, allReplies: [] } });
    expect(wrapper.text()).toContain('This is a reply');
  });

  it('renders author name', () => {
    const wrapper = mount(Reply, { props: { reply, ideaId: 1, allReplies: [] } });
    expect(wrapper.text()).toContain('Julia');
  });

  it('shows ReplyForm when Reply button is clicked', async () => {
    const wrapper = mount(Reply, { props: { reply, ideaId: 1, allReplies: [] } });
    await wrapper.find('button.reply-btn').trigger('click');
    expect(wrapper.findComponent({ name: 'ReplyForm' }).exists()).toBe(true);
  });

  it('adds nested class when isNested is true', () => {
    const wrapper = mount(Reply, { props: { reply, ideaId: 1, allReplies: [], isNested: true } });
    expect(wrapper.find('.reply').classes()).toContain('nested');
  });
});

// ── ReplyList ─────────────────────────────────────────────────────────────────
describe('ReplyList', () => {
  it('shows reply content after load', async () => {
    const wrapper = mount(ReplyList, { props: { ideaId: 1 } });
    await flushPromises();
    expect(wrapper.text()).toContain('A reply');
  });

  it('renders ReplyForm', async () => {
    const wrapper = mount(ReplyList, { props: { ideaId: 1 } });
    await flushPromises();
    expect(wrapper.findComponent({ name: 'ReplyForm' }).exists()).toBe(true);
  });

  it('shows no replies message when list is empty', async () => {
    const { getReplies } = await import('./api');
    vi.mocked(getReplies).mockResolvedValueOnce([]);
    const wrapper = mount(ReplyList, { props: { ideaId: 1 } });
    await flushPromises();
    expect(wrapper.text()).toContain('No replies yet');
  });
});