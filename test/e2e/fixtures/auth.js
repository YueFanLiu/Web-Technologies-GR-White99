import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const authDir = path.resolve(__dirname, '../auth')

export function authState(roleName) {
  return path.join(authDir, `${roleName}.json`)
}

export { authDir }


