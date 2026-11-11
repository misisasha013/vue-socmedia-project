import { createRouter, createWebHistory } from 'vue-router'
import PersonAll from '@/components/PersonAll.vue'
import PersonOne from '@/components/PersonOne.vue'
import MessageAll from '@/components/MessageAll.vue'
import MessageOne from '@/components/MessageOne.vue'
import MessageCreate from '@/components/MessageCreate.vue'

export const Routes = {
  home: "/",
  readPersonAll: "/pa",
  readPersonOne: "/p1/:id",
  messages: "/messages",
  messageNew: "/messages/new",
  readMessageOne: "/messages/:id",
};

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: Routes.home, redirect: Routes.messages },
    { path: Routes.readPersonAll, name: "ReadPersonAll", component: PersonAll },
    { path: Routes.readPersonOne, name: "ReadPersonOne", component: PersonOne, props: true },
    { path: Routes.messages, name: "ReadMessageAll", component: MessageAll },
    { path: Routes.readMessageOne, name: "ReadMessageOne", component: MessageOne, props: true },
    { path: "/messages/new", name: "MessageCreate", component: MessageCreate }
  ]
});