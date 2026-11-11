import { mount } from '@vue/test-utils';
import LikeButton from './components/LikeButton.vue';

describe('LikeButton Component', () => {
  it('renders with initial props', () => {
    const wrapper = mount(LikeButton, {
      props: { 
        ideaId: 1, 
        initialLikeCount: 5,
        initialUserLiked: false 
      }
    });
    expect(wrapper.text()).toContain('5 likes');
    expect(wrapper.text()).toContain('🤍');
  });

  it('shows liked state', () => {
    const wrapper = mount(LikeButton, {
      props: { 
        ideaId: 1,
        initialUserLiked: true 
      }
    });
    expect(wrapper.text()).toContain('♥️');
  });

  it('button exists and is clickable', () => {
    const wrapper = mount(LikeButton, {
      props: { ideaId: 1 }
    });
    const button = wrapper.find('button');
    expect(button.exists()).toBe(true);
  });
});