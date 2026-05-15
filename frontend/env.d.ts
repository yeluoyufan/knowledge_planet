/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '@/utils/request' {
  const request: any
  export default request
}

declare module '@/api/auth' {
  export const login: any
}

declare module '@kangc/v-md-editor' {
  const VMdEditor: any
  export default VMdEditor
}

declare module '@kangc/v-md-editor/lib/preview' {
  const VMdPreview: any
  export default VMdPreview
}

declare module '@kangc/v-md-editor/lib/theme/github.js' {
  const githubTheme: any
  export default githubTheme
}
