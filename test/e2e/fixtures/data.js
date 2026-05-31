export function uniqueSuffix() {
  return `${Date.now()}-${Math.floor(Math.random() * 10_000)}`
}

export function activityData() {
  const suffix = uniqueSuffix()
  const now = new Date()
  const start = new Date(now.getTime() - 30 * 60 * 1000)
  const end = new Date(now.getTime() + 26 * 60 * 60 * 1000)

  return {
    title: `E2E Cross-day Activity ${suffix}`,
    description: `Automated activity created by Playwright ${suffix}`,
    category: 'Tech',
    capacity: '20',
    price: '0',
    venueName: `E2E Venue ${suffix}`,
    address: '1 Test Street',
    city: 'Paris',
    country: 'France',
    startDate: start.toISOString().slice(0, 10),
    startTime: start.toTimeString().slice(0, 5),
    endDate: end.toISOString().slice(0, 10),
    endTime: end.toTimeString().slice(0, 5)
  }
}

export function postData() {
  const suffix = uniqueSuffix()
  return {
    title: `E2E Post ${suffix}`,
    content: `This is an automated post detail and comment test ${suffix}.`
  }
}


