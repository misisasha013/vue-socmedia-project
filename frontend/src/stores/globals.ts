import { defineStore } from "pinia";
import { ref, type Ref } from "vue";

export const globals = defineStore('globals', () => {
  const popup = ref({ msg: "", header: "", element: undefined as undefined | Ref });

  function showPopup(header: string, message: string) {
    popup.value.msg = message;
    popup.value.header = header;
    (popup.value.element as any)?.value?.showModal?.();
  }

  function clearPopup() {
    popup.value.msg = "";
    popup.value.header = "";
    (popup.value.element as any)?.value?.close?.();
  }

  return { popup, showPopup, clearPopup }
});

