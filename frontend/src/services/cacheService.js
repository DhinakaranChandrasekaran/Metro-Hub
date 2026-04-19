// Simple request cache with TTL (Time To Live)
class RequestCache {
  constructor() {
    this.cache = new Map()
    this.timers = new Map()
  }

  // Set cache with TTL (default 5 minutes)
  set(key, value, ttl = 5 * 60 * 1000) {
    // Clear existing timer if any
    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key))
    }

    // Set cache value
    this.cache.set(key, value)

    // Set auto-expiry timer
    const timer = setTimeout(() => {
      this.cache.delete(key)
      this.timers.delete(key)
    }, ttl)

    this.timers.set(key, timer)
  }

  get(key) {
    return this.cache.get(key)
  }

  has(key) {
    return this.cache.has(key)
  }

  clear(key) {
    if (this.timers.has(key)) {
      clearTimeout(this.timers.get(key))
      this.timers.delete(key)
    }
    this.cache.delete(key)
  }

  clearAll() {
    this.timers.forEach(timer => clearTimeout(timer))
    this.timers.clear()
    this.cache.clear()
  }
}

export const requestCache = new RequestCache()
