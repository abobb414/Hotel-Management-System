<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const today = new Date()
const dateString = (value) => value.toISOString().slice(0, 10)
const plusDays = (value, days) => {
  const next = new Date(value)
  next.setDate(next.getDate() + days)
  return dateString(next)
}

const createPager = () => ({ pageNo: 1, pageSize: 8, total: 0, totalPages: 1, records: [] })
const hasText = (value) => value !== null && value !== undefined && String(value).trim() !== ''
const isPhoneLike = (value) => /^1\d{10}$/.test(String(value || '').trim())

const defaultReservationForm = () => ({
  guestName: '',
  phone: '',
  idCard: '',
  roomId: '',
  checkInDate: dateString(today),
  checkOutDate: plusDays(today, 1),
  guestCount: 2,
  breakfastFee: 0,
  extraBedFee: 0,
  depositAmount: 300,
  couponAmount: 0,
  channel: 'DIRECT',
  specialRequest: '',
})

const defaultRoomTypeForm = () => ({
  name: '',
  basePrice: 498,
  maxGuests: 2,
  bedType: '1.8m Queen Bed',
  area: 32,
  description: '',
  amenities: '',
  coverImage: '',
})

const defaultRoomForm = () => ({
  roomNumber: '',
  roomTypeId: '',
  floor: 8,
  status: 'AVAILABLE',
  cleanStatus: 'READY',
})

const defaultGuestForm = () => ({
  fullName: '',
  phone: '',
  idCard: '',
  memberLevel: 'REGULAR',
  remark: '',
})

const defaultRegisterForm = () => ({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})

const defaultCustomerLoginForm = () => ({
  phone: '',
  password: '',
})

const defaultCustomerRegisterForm = () => ({
  displayName: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const defaultCustomerReservationForm = () => ({
  idCard: '',
  roomId: '',
  checkInDate: dateString(today),
  checkOutDate: plusDays(today, 1),
  guestCount: 2,
  specialRequest: '',
})

const defaultPasswordForm = () => ({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const defaultUserForm = () => ({
  username: '',
  displayName: '',
  role: 'FRONT_DESK',
  status: 'ACTIVE',
  password: '',
})

const loginForm = ref({
  username: 'admin',
  password: 'admin123',
})

const registerForm = ref(defaultRegisterForm())
const customerLoginForm = ref(defaultCustomerLoginForm())
const customerRegisterForm = ref(defaultCustomerRegisterForm())
const customerReservationForm = ref(defaultCustomerReservationForm())
const passwordForm = ref(defaultPasswordForm())
const userForm = ref(defaultUserForm())

const token = ref(localStorage.getItem('hotel_access_token') || localStorage.getItem('hotel_admin_token') || '')
const currentUser = ref(null)
const loading = ref(false)
const authLoading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref('dashboard')
const authMode = ref('login')
const authAudience = ref('staff')
const customerTab = ref('home')

const summary = ref({
  availableRooms: 0,
  inHouseReservations: 0,
  todayArrivals: 0,
  lastThirtyDaysRevenue: 0,
})

const trends = ref({
  totalRevenue: 0,
  averageDailyRevenue: 0,
  totalArrivals: 0,
  totalDepartures: 0,
  points: [],
})

const roomTypeList = ref([])
const roomCatalog = ref([])
const roomTypePage = ref(createPager())
const roomPage = ref(createPager())
const reservationPage = ref(createPager())
const guestPage = ref(createPager())
const userPage = ref(createPager())
const financePage = ref(createPager())
const logPage = ref(createPager())
const notificationPage = ref(createPager())
const selectedGuestProfile = ref(null)
const selectedReservationOps = ref(null)
const customerReservationPage = ref(createPager())
const customerRoomTypes = ref([])
// 指定入住/离店日期下真正可预订的房间，来源为 /api/v1/rooms/available
const customerAvailableRooms = ref([])
const customerAvailabilityLoading = ref(false)
const customerAvailabilityError = ref('')
const customerProfile = ref(null)
const roomCalendar = ref([])
const unreadNotificationCount = ref(0)
const financeSummary = ref({
  transactionCount: 0,
  chargeTotal: 0,
  discountTotal: 0,
  refundTotal: 0,
  netTotal: 0,
})

const roomTypeFilters = ref({ keyword: '' })
const roomFilters = ref({ keyword: '', roomTypeId: '', status: '', cleanStatus: '' })
const reservationFilters = ref({ keyword: '', status: '', channel: '', roomNumber: '' })
const guestFilters = ref({ keyword: '', memberLevel: '' })
const userFilters = ref({ keyword: '', role: '', status: '' })
const financeFilters = ref({ reservationId: '', transactionType: '', direction: '' })
const logFilters = ref({ reservationId: '', actionType: '', operatorUsername: '' })
const notificationFilters = ref({ status: '', category: '' })
const calendarFilters = ref({ startDate: dateString(today), days: 7, floor: '' })
const reservationOpsForm = ref({
  extendCheckOutDate: plusDays(today, 2),
  changeRoomId: '',
  changeReason: '',
})

const roomTypeForm = ref(defaultRoomTypeForm())
const roomForm = ref(defaultRoomForm())
const reservationForm = ref(defaultReservationForm())
const guestForm = ref(defaultGuestForm())

const editingRoomTypeId = ref(null)
const editingRoomId = ref(null)
const editingReservationId = ref(null)
const editingGuestId = ref(null)
const editingUserId = ref(null)

const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const isCustomer = computed(() => currentUser.value?.role === 'CUSTOMER')
const roleLabel = computed(() => {
  if (isCustomer.value) return '住客用户'
  return isAdmin.value ? '管理员角色' : '前台角色'
})
const visibleTabs = computed(() =>
  isAdmin.value
    ? ['dashboard', 'calendar', 'reservations', 'finance', 'logs', 'messages', 'room-types', 'rooms', 'guests', 'users']
    : ['dashboard', 'calendar', 'reservations', 'finance', 'logs', 'messages', 'guests', 'rooms']
)

const stats = computed(() => [
  { label: '可用房', value: summary.value.availableRooms, note: '当前可售库存' },
  { label: '在住订单', value: summary.value.inHouseReservations, note: '前台接待节奏' },
  { label: '今日到店', value: summary.value.todayArrivals, note: '待处理入住' },
  { label: '近 30 天营收', value: currency(summary.value.lastThirtyDaysRevenue), note: '经营收入总览' },
])

const selectedRoomRate = computed(() => {
  const room = roomCatalog.value.find((item) => item.id === Number(reservationForm.value.roomId))
  if (!room) return 0
  const roomType = roomTypeList.value.find((item) => item.id === room.roomTypeId)
  return Number(roomType?.basePrice || 0)
})

const stayNights = computed(() => {
  if (!reservationForm.value.checkInDate || !reservationForm.value.checkOutDate) return 0
  const start = new Date(reservationForm.value.checkInDate)
  const end = new Date(reservationForm.value.checkOutDate)
  const diff = Math.round((end - start) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
})

const estimatedAmount = computed(() => selectedRoomRate.value * stayNights.value)
const totalChargePreview = computed(
  () =>
    estimatedAmount.value +
    Number(reservationForm.value.breakfastFee || 0) +
    Number(reservationForm.value.extraBedFee || 0) +
    Number(reservationForm.value.depositAmount || 0) -
    Number(reservationForm.value.couponAmount || 0)
)

const customerSelectedRoom = computed(() =>
  customerAvailableRooms.value.find((item) => item.id === Number(customerReservationForm.value.roomId))
)

const customerSelectedRoomType = computed(() =>
  customerRoomTypes.value.find((item) => item.id === customerSelectedRoom.value?.roomTypeId)
)

const customerRoomRate = computed(() =>
  Number(customerSelectedRoom.value?.basePrice || customerSelectedRoomType.value?.basePrice || 0)
)

const customerStayNights = computed(() => {
  if (!customerReservationForm.value.checkInDate || !customerReservationForm.value.checkOutDate) return 0
  const start = new Date(customerReservationForm.value.checkInDate)
  const end = new Date(customerReservationForm.value.checkOutDate)
  const diff = Math.round((end - start) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
})

const customerEstimatedAmount = computed(() => customerRoomRate.value * customerStayNights.value)

const calendarDays = computed(() => {
  const start = new Date(calendarFilters.value.startDate || dateString(today))
  const days = Number(calendarFilters.value.days || 7)
  return Array.from({ length: days }, (_, index) => {
    const next = new Date(start)
    next.setDate(start.getDate() + index)
    return dateString(next)
  })
})

const availableTargetRooms = computed(() => {
  if (!selectedReservationOps.value) return []
  return roomCatalog.value.filter((room) => room.id !== selectedReservationOps.value.roomId && room.status !== 'MAINTENANCE')
})

const financeSummaryCards = computed(() => [
  { label: '流水笔数', value: financeSummary.value.transactionCount || 0, note: '当前筛选范围' },
  { label: '入账金额', value: currency(financeSummary.value.chargeTotal), note: '房费 / 押金 / 结算' },
  { label: '优惠抵扣', value: currency(financeSummary.value.discountTotal), note: '优惠券与折扣' },
  { label: '净流水', value: currency(financeSummary.value.netTotal), note: '入账 - 抵扣 - 退款' },
])

const unreadNotifications = computed(() =>
  notificationPage.value.records.filter((item) => item.status === 'UNREAD').slice(0, 3)
)

const trendMax = computed(() => Math.max(...trends.value.points.map((item) => Number(item.revenue || 0)), 1))
const trendPolyline = computed(() => {
  if (!trends.value.points.length) return ''
  return trends.value.points
    .map((point, index) => {
      const x = trends.value.points.length === 1 ? 0 : (index / (trends.value.points.length - 1)) * 100
      const y = 100 - (Number(point.revenue || 0) / trendMax.value) * 100
      return `${x},${Math.max(4, y)}`
    })
    .join(' ')
})

const currency = (value) =>
  new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))

const statusText = (status) =>
  ({
    BOOKED: '已预订',
    CHECKED_IN: '已入住',
    CHECKED_OUT: '已退房',
    CANCELLED: '已取消',
    AVAILABLE: '空房',
    OCCUPIED: '在住',
    MAINTENANCE: '维修停用',
    UNREAD: '未读',
    READ: '已读',
    READY: '已清洁',
    CLEANING: '清洁中',
    BLOCKED: '锁房',
    ACTIVE: '启用',
    DISABLED: '停用',
  }[status] || status || '—')

const channelText = (channel) =>
  ({
    DIRECT: '前台直订',
    OTA: '线上平台',
    PHONE: '电话预订',
  }[channel] || channel || '—')

const roleText = (role) =>
  ({
    ADMIN: '系统管理员',
    FRONT_DESK: '前台专员',
    CUSTOMER: '住客',
    STAFF: '员工',
  }[role] || role || '—')

const memberLevelText = (level) =>
  ({
    REGULAR: '普通会员',
    GOLD: '金卡会员',
    PLATINUM: '白金会员',
  }[level] || level || '普通会员')

const actionText = (action) =>
  ({
    CREATE_RESERVATION: '创建订单',
    UPDATE_RESERVATION: '更新订单',
    DELETE_RESERVATION: '删除订单',
    STATUS_CHANGE: '状态变更',
    EXTEND_STAY: '办理续住',
    ROOM_CHANGE: '办理换房',
  }[action] || action || '—')

const transactionText = (type) =>
  ({
    ROOM_FEE: '房费入账',
    BREAKFAST_FEE: '早餐费用',
    EXTRA_BED_FEE: '加床费用',
    DEPOSIT: '押金',
    COUPON: '优惠券抵扣',
    CHECK_IN_CONFIRMED: '入住确认',
    CHECKOUT_SETTLEMENT: '退房结算',
    BOOKING_CANCELLED: '订单取消',
    ROOM_FEE_ADJUSTMENT: '房费调整',
    BREAKFAST_ADJUSTMENT: '早餐调整',
    EXTRA_BED_ADJUSTMENT: '加床调整',
    DEPOSIT_ADJUSTMENT: '押金调整',
    COUPON_ADJUSTMENT: '优惠调整',
  }[type] || type || '—')

const directionText = (direction) =>
  ({
    CHARGE: '入账',
    DISCOUNT: '抵扣',
    REFUND: '退款',
  }[direction] || direction || '—')

const notificationCategoryText = (category) =>
  ({
    BOOKING_SUCCESS: '预订成功',
    UPCOMING_CHECKIN: '即将入住',
    UPCOMING_CHECKOUT: '即将退房',
  }[category] || category || '—')

const formatDateTime = (value) => (value ? value.replace('T', ' ').slice(0, 16) : '—')

function snapshotText(snapshot) {
  if (!snapshot) return '—'
  const parts = Object.fromEntries(
    snapshot.split(',').map((part) => {
      const [key, ...rest] = part.split('=')
      return [key, rest.join('=')]
    })
  )
  const items = [
    parts.reservationNo && `订单 ${parts.reservationNo}`,
    parts.status && `状态 ${statusText(parts.status)}`,
    parts.roomId && `房间ID ${parts.roomId}`,
    parts.checkIn && `入住 ${parts.checkIn}`,
    parts.checkOut && `离店 ${parts.checkOut}`,
    parts.total && `金额 ${currency(parts.total)}`,
  ].filter(Boolean)
  return items.length ? items.join(' · ') : snapshot
}

async function requestJson(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }

  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`
  }

  const response = await fetch(url, { ...options, headers })
  const result = await response.json().catch(() => ({ success: false, message: `HTTP ${response.status}` }))

  if (response.status === 401) {
    logout()
    throw new Error(result.message || '登录已失效，请重新登录')
  }

  if (response.status === 403) {
    throw new Error(result.message || '当前账号没有权限执行这个操作')
  }

  if (!response.ok || !result.success) {
    throw new Error(result.message || `请求失败：${response.status}`)
  }

  return result.data
}

async function requestBlob(url) {
  const headers = {}
  if (token.value) headers.Authorization = `Bearer ${token.value}`
  const response = await fetch(url, { headers })
  if (!response.ok) {
    throw new Error(`下载失败：${response.status}`)
  }
  return response.blob()
}

function queryString(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (hasText(value)) search.set(key, value)
  })
  return search.toString()
}

async function loadDashboard() {
  const [overviewData, trendData, roomTypeData, roomData] = await Promise.all([
    requestJson('/api/v1/dashboard/overview'),
    requestJson('/api/v1/dashboard/trends'),
    requestJson('/api/v1/room-types'),
    requestJson('/api/v1/rooms'),
  ])
  summary.value = overviewData
  trends.value = trendData
  roomTypeList.value = roomTypeData
  roomCatalog.value = roomData
}

async function loadCurrentUser() {
  currentUser.value = await requestJson('/api/v1/auth/me')
}

async function loadRoomTypePage(pageNo = roomTypePage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: roomTypePage.value.pageSize, ...roomTypeFilters.value })
  roomTypePage.value = await requestJson(`/api/v1/room-types/page?${query}`)
}

async function loadRoomPage(pageNo = roomPage.value.pageNo) {
  const query = queryString({
    pageNo,
    pageSize: roomPage.value.pageSize,
    keyword: roomFilters.value.keyword,
    roomTypeId: roomFilters.value.roomTypeId,
    status: roomFilters.value.status,
    cleanStatus: roomFilters.value.cleanStatus,
  })
  roomPage.value = await requestJson(`/api/v1/rooms/page?${query}`)
}

async function loadReservationPage(pageNo = reservationPage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: reservationPage.value.pageSize, ...reservationFilters.value })
  reservationPage.value = await requestJson(`/api/v1/reservations/page?${query}`)
}

async function loadGuestPage(pageNo = guestPage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: guestPage.value.pageSize, ...guestFilters.value })
  guestPage.value = await requestJson(`/api/v1/guests?${query}`)
}

async function loadUserPage(pageNo = userPage.value.pageNo) {
  if (!isAdmin.value) return
  const query = queryString({ pageNo, pageSize: userPage.value.pageSize, ...userFilters.value })
  userPage.value = await requestJson(`/api/v1/users?${query}`)
}

async function loadGuestProfile(id) {
  selectedGuestProfile.value = await requestJson(`/api/v1/guests/${id}/profile`)
}

async function loadRoomCalendar() {
  const query = queryString({
    startDate: calendarFilters.value.startDate,
    days: calendarFilters.value.days,
    floor: calendarFilters.value.floor,
  })
  roomCalendar.value = await requestJson(`/api/v1/operations/room-calendar?${query}`)
}

async function loadFinancePage(pageNo = financePage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: financePage.value.pageSize, ...financeFilters.value })
  const [pageData, summaryData] = await Promise.all([
    requestJson(`/api/v1/operations/financial-transactions?${query}`),
    requestJson(`/api/v1/operations/financial-summary?${queryString(financeFilters.value)}`),
  ])
  financePage.value = pageData
  financeSummary.value = summaryData
}

async function loadLogPage(pageNo = logPage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: logPage.value.pageSize, ...logFilters.value })
  logPage.value = await requestJson(`/api/v1/operations/logs?${query}`)
}

async function loadNotificationPage(pageNo = notificationPage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: notificationPage.value.pageSize, ...notificationFilters.value })
  const [pageData, unreadData] = await Promise.all([
    requestJson(`/api/v1/operations/notifications?${query}`),
    requestJson('/api/v1/operations/notifications?pageNo=1&pageSize=1&status=UNREAD'),
  ])
  notificationPage.value = pageData
  unreadNotificationCount.value = unreadData.total
}

async function loadCustomerProfile() {
  customerProfile.value = await requestJson('/api/v1/customer/auth/me')
}

async function loadCustomerRoomTypes() {
  customerRoomTypes.value = await requestJson('/api/v1/customer/room-types')
}

/**
 * 按当前选择的入住/离店日期查询可预订房间。
 * 房间能否预订取决于「日期区间是否与已有预约重叠」，不能只看房间的静态状态，
 * 否则用户会选到已被占用的房间、提交后才被拒绝。
 */
async function loadCustomerAvailableRooms() {
  const { checkInDate, checkOutDate } = customerReservationForm.value

  if (!checkInDate || !checkOutDate) {
    customerAvailableRooms.value = []
    customerAvailabilityError.value = '请先选择入住日期与离店日期'
    return
  }

  if (checkOutDate <= checkInDate) {
    customerAvailableRooms.value = []
    customerReservationForm.value.roomId = ''
    customerAvailabilityError.value = '离店日期需晚于入住日期'
    return
  }

  customerAvailabilityLoading.value = true
  customerAvailabilityError.value = ''
  try {
    const query = queryString({ checkIn: checkInDate, checkOut: checkOutDate })
    const rooms = await requestJson(`/api/v1/rooms/available?${query}`)
    customerAvailableRooms.value = Array.isArray(rooms) ? rooms : []
    // 日期变化后，原先选中的房间可能已不再可订，清空选择避免提交时被后端拒绝
    const stillBookable = customerAvailableRooms.value.some(
      (item) => item.id === Number(customerReservationForm.value.roomId)
    )
    if (!stillBookable) {
      customerReservationForm.value.roomId = ''
    }
  } catch (error) {
    customerAvailableRooms.value = []
    customerReservationForm.value.roomId = ''
    customerAvailabilityError.value = error.message
  } finally {
    customerAvailabilityLoading.value = false
  }
}

// 进入「在线预订」页或改动日期时，重新查询可订房间
watch(
  () => [customerTab.value, customerReservationForm.value.checkInDate, customerReservationForm.value.checkOutDate],
  () => {
    if (!isCustomer.value || customerTab.value !== 'booking') return
    loadCustomerAvailableRooms()
  },
  { immediate: true }
)

async function loadCustomerReservationPage(pageNo = customerReservationPage.value.pageNo) {
  const query = queryString({ pageNo, pageSize: customerReservationPage.value.pageSize })
  customerReservationPage.value = await requestJson(`/api/v1/customer/reservations?${query}`)
}

async function loadAllData() {
  loading.value = true
  errorMessage.value = ''
  try {
    await loadCurrentUser()
    if (isCustomer.value) {
      await Promise.all([loadCustomerProfile(), loadCustomerRoomTypes(), loadCustomerReservationPage(1)])
      customerTab.value = customerTab.value || 'home'
    } else {
      await loadDashboard()
      await Promise.all([
        loadRoomTypePage(1),
        loadRoomPage(1),
        loadReservationPage(1),
        loadRoomCalendar(),
        loadFinancePage(1),
        loadLogPage(1),
        loadNotificationPage(1),
        loadGuestPage(1),
        isAdmin.value ? loadUserPage(1) : Promise.resolve(),
      ])
      if (guestPage.value.records.length > 0) {
        await loadGuestProfile(guestPage.value.records[0].id)
      } else {
        selectedGuestProfile.value = null
      }
      if (!isAdmin.value && activeTab.value === 'users') {
        activeTab.value = 'dashboard'
      }
      selectedReservationOps.value = reservationPage.value.records[0] || null
      if (selectedReservationOps.value) {
        reservationOpsForm.value.extendCheckOutDate = plusDays(new Date(selectedReservationOps.value.checkOutDate), 1)
        reservationOpsForm.value.changeRoomId = ''
        reservationOpsForm.value.changeReason = ''
      }
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function login() {
  authLoading.value = true
  clearMessages()
  try {
    let data
    if (authAudience.value === 'customer') {
      data = await requestJson('/api/v1/customer/auth/login', {
        method: 'POST',
        headers: {},
        body: JSON.stringify(customerLoginForm.value),
      })
    } else {
      try {
        data = await requestJson('/api/v1/auth/login', {
          method: 'POST',
          headers: {},
          body: JSON.stringify(loginForm.value),
        })
      } catch (error) {
        if (isPhoneLike(loginForm.value.username)) {
          customerLoginForm.value = {
            phone: loginForm.value.username,
            password: loginForm.value.password,
          }
          data = await requestJson('/api/v1/customer/auth/login', {
            method: 'POST',
            headers: {},
            body: JSON.stringify(customerLoginForm.value),
          })
          authAudience.value = 'customer'
        } else {
          throw error
        }
      }
    }
    applyAuthSuccess(data, data.role === 'CUSTOMER' ? `欢迎回来，${data.displayName}` : `欢迎回来，${data.displayName}`)
    await loadAllData()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    authLoading.value = false
  }
}

async function register() {
  authLoading.value = true
  clearMessages()
  try {
    const endpoint = authAudience.value === 'customer' ? '/api/v1/customer/auth/register' : '/api/v1/auth/register'
    const payload = authAudience.value === 'customer' ? customerRegisterForm.value : registerForm.value
    const data = await requestJson(endpoint, {
      method: 'POST',
      headers: {},
      body: JSON.stringify(payload),
    })
    registerForm.value = defaultRegisterForm()
    customerRegisterForm.value = defaultCustomerRegisterForm()
    applyAuthSuccess(data, authAudience.value === 'customer' ? `注册成功，已进入住客中心：${data.displayName}` : `注册成功，已进入工作台：${data.displayName}`)
    await loadAllData()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    authLoading.value = false
  }
}

async function changePassword() {
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson('/api/v1/auth/change-password', {
      method: 'PUT',
      body: JSON.stringify(passwordForm.value),
    })
    passwordForm.value = defaultPasswordForm()
    successMessage.value = '密码已更新'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

function applyAuthSuccess(data, message) {
  token.value = data.token
  currentUser.value = data
  localStorage.setItem('hotel_access_token', data.token)
  localStorage.setItem('hotel_admin_token', data.token)
  successMessage.value = message
}

function logout() {
  token.value = ''
  currentUser.value = null
  customerProfile.value = null
  customerRoomTypes.value = []
  customerAvailableRooms.value = []
  customerReservationPage.value = createPager()
  localStorage.removeItem('hotel_admin_token')
  localStorage.removeItem('hotel_access_token')
  activeTab.value = 'dashboard'
  customerTab.value = 'home'
  authMode.value = 'login'
}

function clearMessages() {
  errorMessage.value = ''
  successMessage.value = ''
}

function resetRoomTypeForm() {
  roomTypeForm.value = defaultRoomTypeForm()
  editingRoomTypeId.value = null
}

function resetRoomForm() {
  roomForm.value = defaultRoomForm()
  editingRoomId.value = null
}

function resetReservationForm() {
  reservationForm.value = defaultReservationForm()
  editingReservationId.value = null
}

function resetGuestForm() {
  guestForm.value = defaultGuestForm()
  editingGuestId.value = null
}

function resetUserForm() {
  userForm.value = defaultUserForm()
  editingUserId.value = null
}

function resetCustomerReservationForm() {
  customerReservationForm.value = defaultCustomerReservationForm()
  // 重置后日期回到默认值，watch 可能因值未变化而不触发，这里主动刷新一次可订房间
  if (isCustomer.value && customerTab.value === 'booking') {
    loadCustomerAvailableRooms()
  }
}

function startEditRoomType(item) {
  roomTypeForm.value = { ...item }
  editingRoomTypeId.value = item.id
  activeTab.value = 'room-types'
}

function startEditRoom(item) {
  roomForm.value = {
    roomNumber: item.roomNumber,
    roomTypeId: item.roomTypeId,
    floor: item.floor,
    status: item.status,
    cleanStatus: item.cleanStatus,
  }
  editingRoomId.value = item.id
  activeTab.value = 'rooms'
}

function startEditReservation(item) {
  const targetRoom = roomCatalog.value.find((room) => room.id === Number(item.roomId))
  reservationForm.value = {
    guestName: item.guestName,
    phone: item.phone,
    idCard: '',
    roomId: targetRoom?.id || '',
    checkInDate: item.checkInDate,
    checkOutDate: item.checkOutDate,
    guestCount: item.guestCount,
    breakfastFee: item.breakfastFee || 0,
    extraBedFee: item.extraBedFee || 0,
    depositAmount: item.depositAmount || 0,
    couponAmount: item.couponAmount || 0,
    channel: item.channel,
    specialRequest: item.specialRequest || '',
  }
  editingReservationId.value = item.id
  activeTab.value = 'reservations'
}

function selectReservationOps(item) {
  selectedReservationOps.value = item
  reservationOpsForm.value.extendCheckOutDate = plusDays(new Date(item.checkOutDate), 1)
  reservationOpsForm.value.changeRoomId = ''
  reservationOpsForm.value.changeReason = ''
}

function startEditGuest(item) {
  guestForm.value = { ...item }
  editingGuestId.value = item.id
  activeTab.value = 'guests'
}

function startEditUser(item) {
  userForm.value = {
    username: item.username,
    displayName: item.displayName,
    role: item.role,
    status: item.status,
    password: '',
  }
  editingUserId.value = item.id
  activeTab.value = 'users'
}

async function saveRoomType() {
  actionLoading.value = true
  clearMessages()
  try {
    const url = editingRoomTypeId.value ? `/api/v1/room-types/${editingRoomTypeId.value}` : '/api/v1/room-types'
    const method = editingRoomTypeId.value ? 'PUT' : 'POST'
    await requestJson(url, {
      method,
      body: JSON.stringify({
        ...roomTypeForm.value,
        basePrice: Number(roomTypeForm.value.basePrice),
        maxGuests: Number(roomTypeForm.value.maxGuests),
        area: Number(roomTypeForm.value.area),
      }),
    })
    successMessage.value = editingRoomTypeId.value ? '房型已更新' : '房型已创建'
    resetRoomTypeForm()
    await Promise.all([loadDashboard(), loadRoomTypePage(1)])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function saveRoom() {
  actionLoading.value = true
  clearMessages()
  try {
    const url = editingRoomId.value ? `/api/v1/rooms/${editingRoomId.value}` : '/api/v1/rooms'
    const method = editingRoomId.value ? 'PUT' : 'POST'
    await requestJson(url, {
      method,
      body: JSON.stringify({
        ...roomForm.value,
        roomTypeId: Number(roomForm.value.roomTypeId),
        floor: Number(roomForm.value.floor),
      }),
    })
    successMessage.value = editingRoomId.value ? '房间已更新' : '房间已创建'
    resetRoomForm()
    await Promise.all([loadDashboard(), loadRoomPage(1)])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function saveReservation() {
  actionLoading.value = true
  clearMessages()
  try {
    const url = editingReservationId.value
      ? `/api/v1/reservations/${editingReservationId.value}`
      : '/api/v1/reservations'
    const method = editingReservationId.value ? 'PUT' : 'POST'
    await requestJson(url, {
      method,
      body: JSON.stringify({
        ...reservationForm.value,
        roomId: Number(reservationForm.value.roomId),
        guestCount: Number(reservationForm.value.guestCount),
        breakfastFee: Number(reservationForm.value.breakfastFee || 0),
        extraBedFee: Number(reservationForm.value.extraBedFee || 0),
        depositAmount: Number(reservationForm.value.depositAmount || 0),
        couponAmount: Number(reservationForm.value.couponAmount || 0),
        idCard: reservationForm.value.idCard || '330102199901011234',
      }),
    })
    successMessage.value = editingReservationId.value ? '订单已更新' : '订单已创建'
    resetReservationForm()
    await Promise.all([loadDashboard(), loadReservationPage(1), loadGuestPage(1), loadFinancePage(1), loadLogPage(1), loadNotificationPage(1), loadRoomCalendar()])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function saveGuest() {
  actionLoading.value = true
  clearMessages()
  try {
    const url = editingGuestId.value ? `/api/v1/guests/${editingGuestId.value}` : '/api/v1/guests'
    const method = editingGuestId.value ? 'PUT' : 'POST'
    await requestJson(url, {
      method,
      body: JSON.stringify(guestForm.value),
    })
    successMessage.value = editingGuestId.value ? '住客已更新' : '住客已创建'
    resetGuestForm()
    await loadGuestPage(1)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function saveUser() {
  actionLoading.value = true
  clearMessages()
  try {
    const payload = {
      ...userForm.value,
      password: hasText(userForm.value.password) ? userForm.value.password : null,
    }
    const url = editingUserId.value ? `/api/v1/users/${editingUserId.value}` : '/api/v1/users'
    const method = editingUserId.value ? 'PUT' : 'POST'
    await requestJson(url, {
      method,
      body: JSON.stringify(payload),
    })
    successMessage.value = editingUserId.value ? '账户已更新' : '账户已创建'
    resetUserForm()
    await loadUserPage(1)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function saveCustomerReservation() {
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson('/api/v1/customer/reservations', {
      method: 'POST',
      body: JSON.stringify({
        ...customerReservationForm.value,
        roomId: Number(customerReservationForm.value.roomId),
        guestCount: Number(customerReservationForm.value.guestCount),
      }),
    })
    successMessage.value = '预订已提交'
    resetCustomerReservationForm()
    await Promise.all([loadCustomerReservationPage(1), loadCustomerRoomTypes()])
    customerTab.value = 'reservations'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function removeItem(type, id) {
  actionLoading.value = true
  clearMessages()
  try {
    const urlMap = {
      roomType: `/api/v1/room-types/${id}`,
      room: `/api/v1/rooms/${id}`,
      reservation: `/api/v1/reservations/${id}`,
      guest: `/api/v1/guests/${id}`,
      user: `/api/v1/users/${id}`,
    }
    await requestJson(urlMap[type], { method: 'DELETE' })
    successMessage.value = '数据已删除'
    await Promise.all([
      loadDashboard(),
      loadRoomTypePage(),
      loadRoomPage(),
      loadReservationPage(),
      loadFinancePage(),
      loadLogPage(),
      loadNotificationPage(),
      loadRoomCalendar(),
      loadGuestPage(),
      isAdmin.value ? loadUserPage() : Promise.resolve(),
    ])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function changeReservationStatus(item, status) {
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson(`/api/v1/reservations/${item.id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    })
    successMessage.value = `订单状态已更新为 ${status}`
    await Promise.all([
      loadDashboard(),
      loadReservationPage(reservationPage.value.pageNo),
      loadRoomPage(roomPage.value.pageNo),
      loadRoomCalendar(),
      loadFinancePage(1),
      loadLogPage(1),
      loadNotificationPage(1),
    ])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function extendReservation() {
  if (!selectedReservationOps.value) return
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson(`/api/v1/reservations/${selectedReservationOps.value.id}/extend`, {
      method: 'PUT',
      body: JSON.stringify({ checkOutDate: reservationOpsForm.value.extendCheckOutDate }),
    })
    successMessage.value = '续住已处理'
    await Promise.all([
      loadDashboard(),
      loadReservationPage(reservationPage.value.pageNo),
      loadRoomCalendar(),
      loadFinancePage(1),
      loadLogPage(1),
      loadNotificationPage(1),
    ])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function changeReservationRoom() {
  if (!selectedReservationOps.value || !reservationOpsForm.value.changeRoomId) return
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson(`/api/v1/reservations/${selectedReservationOps.value.id}/change-room`, {
      method: 'PUT',
      body: JSON.stringify({
        roomId: Number(reservationOpsForm.value.changeRoomId),
        reason: reservationOpsForm.value.changeReason,
      }),
    })
    successMessage.value = '换房已处理'
    await Promise.all([
      loadDashboard(),
      loadReservationPage(reservationPage.value.pageNo),
      loadRoomPage(roomPage.value.pageNo),
      loadRoomCalendar(),
      loadFinancePage(1),
      loadLogPage(1),
      loadNotificationPage(1),
    ])
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function markNotificationRead(item) {
  actionLoading.value = true
  clearMessages()
  try {
    await requestJson(`/api/v1/operations/notifications/${item.id}/read`, { method: 'PUT' })
    successMessage.value = '消息已标记为已读'
    await loadNotificationPage(notificationPage.value.pageNo)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function exportExcel() {
  actionLoading.value = true
  clearMessages()
  try {
    const blob = await requestBlob('/api/v1/reports/operations/export')
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `hotel-operations-${dateString(new Date())}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    successMessage.value = 'Excel 报表已导出'
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

async function openPrintDocument(item, type) {
  actionLoading.value = true
  clearMessages()
  try {
    const endpointMap = {
      reservation: `/api/v1/reservations/${item.id}/print`,
      checkin: `/api/v1/reservations/${item.id}/check-in-slip`,
      settlement: `/api/v1/reservations/${item.id}/checkout-settlement`,
    }
    const data = await requestJson(endpointMap[type])
    const popup = window.open('', '_blank', 'width=900,height=800')
    if (!popup) throw new Error('浏览器拦截了打印窗口')
    popup.document.write(`
      <html>
        <head>
          <title>${data.documentType}</title>
          <style>
            body{font-family:Arial,sans-serif;padding:32px;color:#1d1a16}
            h1{font-size:28px;margin:0 0 20px}
            .grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-bottom:24px}
            .card{border:1px solid #ddd;padding:12px;border-radius:8px}
            table{width:100%;border-collapse:collapse;margin-top:12px}
            td,th{border:1px solid #ddd;padding:10px;text-align:left}
          </style>
        </head>
        <body>
          <h1>${data.documentType}</h1>
          <div class="grid">
            <div class="card">订单号：${data.reservationNo}</div>
            <div class="card">住客：${data.guestName}</div>
            <div class="card">手机号：${data.phone}</div>
            <div class="card">房号：${data.roomNumber}</div>
            <div class="card">房型：${data.roomTypeName}</div>
            <div class="card">入住日期：${data.checkInDate}</div>
          </div>
          <table>
            <tr><th>项目</th><th>金额</th></tr>
            <tr><td>房费</td><td>${currency(data.charges.roomFee)}</td></tr>
            <tr><td>早餐</td><td>${currency(data.charges.breakfastFee)}</td></tr>
            <tr><td>加床</td><td>${currency(data.charges.extraBedFee)}</td></tr>
            <tr><td>押金</td><td>${currency(data.charges.depositAmount)}</td></tr>
            <tr><td>优惠券</td><td>${currency(data.charges.couponAmount)}</td></tr>
            <tr><th>合计</th><th>${currency(data.charges.totalAmount)}</th></tr>
          </table>
        </body>
      </html>
    `)
    popup.document.close()
    popup.focus()
    popup.print()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    actionLoading.value = false
  }
}

function roomTypeName(roomTypeId) {
  const source = isCustomer.value ? customerRoomTypes.value : roomTypeList.value
  return source.find((item) => item.id === roomTypeId)?.name || 'Unknown'
}

function roomTypeCover(roomTypeId) {
  const source = isCustomer.value ? customerRoomTypes.value : roomTypeList.value
  return source.find((item) => item.id === roomTypeId)?.coverImage || ''
}

function goPage(loader, pagerRef, nextPage) {
  if (nextPage < 1 || nextPage > pagerRef.value.totalPages) return
  loader(nextPage)
}

function tabLabel(tab) {
  return {
    dashboard: '概览',
    calendar: '房态',
    'room-types': '房型',
    rooms: '房间',
    reservations: '订单',
    finance: '流水',
    logs: '日志',
    messages: '提醒',
    guests: '住客',
    users: '账户',
  }[tab]
}

function customerTabLabel(tab) {
  return {
    home: '房型',
    booking: '预订',
    reservations: '我的订单',
    profile: '我的资料',
  }[tab]
}

onMounted(async () => {
  if (token.value) {
    await loadAllData()
  }
})
</script>

<template>
  <div class="shell">
    <template v-if="!token">
      <section class="login-layout">
        <div class="login-copy">
          <div class="poster-shell">
            <div class="brand-stage poster-stage">
              <div class="brand-lockup brand-lockup-title">
                <span class="brand-mark brand-mark-login" role="img" aria-label="五星级酒店"></span>
                <div class="brand-copy">
                  <h1 class="login-title">轻量酒店工作台</h1>
                </div>
              </div>
            </div>

            <div class="brand-notes poster-notes">
              <article class="brand-note">
                <span class="brand-note-index">01</span>
                <div>
                  <p class="list-title">前台接待</p>
                  <p class="list-subtitle">入住、离店、打印单据与住客登记集中处理。</p>
                </div>
              </article>
              <article class="brand-note">
                <span class="brand-note-index">02</span>
                <div>
                  <p class="list-title">订单流转</p>
                  <p class="list-subtitle">预订、入住、退房、取消状态按日常流程推进。</p>
                </div>
              </article>
              <article class="brand-note">
                <span class="brand-note-index">03</span>
                <div>
                  <p class="list-title">经营分析</p>
                  <p class="list-subtitle">趋势、报表、消费统计与房态数据保持同一视角。</p>
                </div>
              </article>
            </div>
          </div>
        </div>

        <section class="login-panel">
          <div class="window-bar">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="panel-body">
            <div class="auth-audience">
              <button :class="['tab', authAudience === 'staff' && 'is-active']" @click="authAudience = 'staff'">后台入口</button>
              <button :class="['tab', authAudience === 'customer' && 'is-active']" @click="authAudience = 'customer'">住客端</button>
            </div>

            <form v-if="authMode === 'login'" class="auth-form" @submit.prevent="login">
              <div class="section-head compact">
                <div>
                  <p class="section-label">{{ authAudience === 'customer' ? 'Guest Sign In' : 'Sign In' }}</p>
                  <h2>{{ authAudience === 'customer' ? '进入住客中心' : '进入工作台' }}</h2>
                </div>
              </div>
              <template v-if="authAudience === 'customer'">
                <label>手机号<input v-model="customerLoginForm.phone" type="text" autocomplete="tel" /></label>
                <label>密码<input v-model="customerLoginForm.password" type="password" autocomplete="current-password" /></label>
                <button class="primary-button stretch" type="submit">{{ authLoading ? '登录中...' : '进入住客中心' }}</button>
              </template>
              <template v-else>
                <label>用户名<input v-model="loginForm.username" type="text" autocomplete="username" /></label>
                <label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" /></label>
                <button class="primary-button stretch" type="submit">{{ authLoading ? '登录中...' : '进入工作台' }}</button>
              </template>
            </form>

            <form v-else class="auth-form" @submit.prevent="register">
              <div class="section-head compact">
                <div>
                  <p class="section-label">{{ authAudience === 'customer' ? 'Guest Register' : 'Register' }}</p>
                  <h2>{{ authAudience === 'customer' ? '注册住客账号' : '注册账号' }}</h2>
                </div>
              </div>
              <template v-if="authAudience === 'customer'">
                <label>显示名称<input v-model="customerRegisterForm.displayName" type="text" /></label>
                <label>手机号<input v-model="customerRegisterForm.phone" type="text" autocomplete="tel" /></label>
                <label>密码<input v-model="customerRegisterForm.password" type="password" autocomplete="new-password" /></label>
                <label>确认密码<input v-model="customerRegisterForm.confirmPassword" type="password" autocomplete="new-password" /></label>
                <button class="primary-button stretch" type="submit">{{ authLoading ? '注册中...' : '注册并进入住客中心' }}</button>
              </template>
              <template v-else>
                <label>用户名<input v-model="registerForm.username" type="text" autocomplete="username" /></label>
                <label>显示名称<input v-model="registerForm.displayName" type="text" /></label>
                <label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" /></label>
                <label>确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" /></label>
                <button class="primary-button stretch" type="submit">{{ authLoading ? '注册中...' : '注册并登录' }}</button>
              </template>
            </form>

            <div class="auth-switch auth-switch-bottom">
              <button :class="['tab', authMode === 'login' && 'is-active']" @click="authMode = 'login'">登录</button>
              <button :class="['tab', authMode === 'register' && 'is-active']" @click="authMode = 'register'">注册</button>
            </div>

            <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
          </div>
        </section>
      </section>
    </template>

    <template v-else-if="isCustomer">
      <header class="hero">
        <div class="hero-head">
          <div class="brand-lockup brand-lockup-header">
            <span class="brand-mark brand-mark-header" role="img" aria-label="五星级酒店"></span>
            <div class="hero-copy compact-copy">
              <p class="eyebrow">Guest Portal</p>
              <h1>住客中心</h1>
              <p class="copy-text">欢迎回来，{{ customerProfile?.displayName || currentUser?.displayName }} · 会员等级 {{ memberLevelText(customerProfile?.memberLevel) }}</p>
            </div>
          </div>

          <div class="toolbar-actions hero-actions">
            <button class="secondary-button" @click="loadAllData">刷新数据</button>
            <button class="secondary-button" @click="logout">退出登录</button>
          </div>
        </div>
      </header>

      <section class="toolbar">
        <nav class="tabs">
          <button v-for="tab in ['home', 'booking', 'reservations', 'profile']" :key="tab" :class="['tab', customerTab === tab && 'is-active']" @click="customerTab = tab">
            {{ customerTabLabel(tab) }}
          </button>
        </nav>
      </section>

      <section v-if="successMessage || errorMessage" class="status-strip">
        <span :class="['pill', errorMessage ? 'pill-red' : 'pill-green']">
          {{ errorMessage ? 'Operation Failed' : 'Operation Success' }}
        </span>
        <p>{{ errorMessage || successMessage }}</p>
      </section>

      <main v-if="!loading" class="content">
        <section v-if="customerTab === 'home'" class="dashboard-stack">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Rooms</p>
                <h2>可订房型</h2>
              </div>
              <p class="section-note">浏览房型与基础价格</p>
            </div>
            <div class="room-gallery">
              <article v-for="item in customerRoomTypes" :key="item.id" class="room-card">
                <figure>
                  <img v-if="item.coverImage" :src="item.coverImage" :alt="item.name" />
                  <span v-else>暂无图片</span>
                </figure>
                <div class="room-card-body">
                  <p class="list-title">{{ item.name }}</p>
                  <p class="list-subtitle">{{ currency(item.basePrice) }} / 晚 · {{ item.maxGuests }} 位 · {{ item.area }}㎡</p>
                  <p class="list-subtitle">{{ item.bedType }} · {{ item.amenities }}</p>
                </div>
              </article>
            </div>
            <p v-if="!customerRoomTypes.length" class="empty-note">暂无可订房型。</p>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Member</p>
                <h2>住客账户</h2>
              </div>
              <button class="secondary-button small" @click="customerTab = 'booking'">去预订</button>
            </div>
            <div class="table-list">
              <article class="summary-row">
                <div>
                  <p class="list-title">当前账户</p>
                  <p class="list-subtitle">{{ customerProfile?.displayName || currentUser?.displayName }}</p>
                </div>
                <strong>{{ memberLevelText(customerProfile?.memberLevel) }}</strong>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">手机号</p>
                  <p class="list-subtitle">用于订单通知与入住联系</p>
                </div>
                <strong>{{ customerProfile?.phone || currentUser?.username }}</strong>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">我的订单</p>
                  <p class="list-subtitle">查看历史预订和当前状态</p>
                </div>
                <strong>{{ customerReservationPage.total }}</strong>
              </article>
            </div>
          </section>
        </section>

        <section v-if="customerTab === 'booking'" class="split-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Create Booking</p>
                <h2>在线预订</h2>
              </div>
            </div>
            <form class="editor-form" @submit.prevent="saveCustomerReservation">
              <label>手机号<input :value="customerProfile?.phone || currentUser?.username" disabled /></label>
              <label>入住人<input :value="customerProfile?.displayName || currentUser?.displayName" disabled /></label>
              <label>身份证<input v-model="customerReservationForm.idCard" type="text" /></label>
              <label>
                房间
                <select
                  v-model="customerReservationForm.roomId"
                  :disabled="customerAvailabilityLoading || !customerAvailableRooms.length"
                >
                  <option value="">{{ customerAvailabilityLoading ? '查询中…' : '请选择' }}</option>
                  <option v-for="item in customerAvailableRooms" :key="item.id" :value="item.id">
                    {{ item.roomNumber }} · {{ item.roomTypeName }} · {{ currency(item.basePrice) }}
                  </option>
                </select>
              </label>
              <p v-if="customerAvailabilityLoading" class="field-hint full">正在查询该日期区间内的可订房间…</p>
              <p v-else-if="customerAvailabilityError" class="field-hint is-warn full">{{ customerAvailabilityError }}</p>
              <p v-else-if="!customerAvailableRooms.length" class="field-hint is-warn full">
                该日期区间内没有可预订的房间，请调整入住或离店日期。
              </p>
              <p v-else class="field-hint full">该日期区间内可预订 {{ customerAvailableRooms.length }} 间房</p>
              <label>入住日期<input v-model="customerReservationForm.checkInDate" type="date" /></label>
              <label>离店日期<input v-model="customerReservationForm.checkOutDate" type="date" /></label>
              <label>入住人数<input v-model="customerReservationForm.guestCount" type="number" min="1" /></label>
              <label>房价/晚<input :value="currency(customerRoomRate)" disabled /></label>
              <label>入住晚数<input :value="customerStayNights" disabled /></label>
              <label>预估房费<input :value="currency(customerEstimatedAmount)" disabled /></label>
              <div v-if="customerSelectedRoomType?.coverImage" class="cover-preview full">
                <img :src="customerSelectedRoomType.coverImage" :alt="customerSelectedRoomType.name" />
                <p class="list-subtitle">{{ customerSelectedRoomType.name }} · {{ customerSelectedRoomType.bedType }}</p>
              </div>
              <label class="full">特殊需求<textarea v-model="customerReservationForm.specialRequest" rows="3"></textarea></label>
              <div class="form-actions full">
                <button class="primary-button" type="submit">{{ actionLoading ? '提交中...' : '提交预订' }}</button>
                <button class="secondary-button" type="button" @click="resetCustomerReservationForm">重置</button>
              </div>
            </form>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Booking Tips</p>
                <h2>预订说明</h2>
              </div>
            </div>
            <div class="table-list">
              <article class="summary-row">
                <div>
                  <p class="list-title">当前预估</p>
                  <p class="list-subtitle">系统会按房型单价与入住晚数自动计算订单金额。</p>
                </div>
                <strong>{{ currency(customerEstimatedAmount) }}</strong>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">入住流程</p>
                <p class="list-subtitle">提交后订单状态为“已预订”，到店后由前台办理入住。</p>
                </div>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">证件信息</p>
                  <p class="list-subtitle">请确保身份证件与实际入住人一致。</p>
                </div>
              </article>
            </div>
          </section>
        </section>

        <section v-if="customerTab === 'reservations'" class="panel">
          <div class="section-head">
            <div>
              <p class="section-label">My Reservations</p>
              <h2>我的订单</h2>
            </div>
            <p class="section-note">共 {{ customerReservationPage.total }} 条</p>
          </div>
          <div class="table-list">
            <article v-for="item in customerReservationPage.records" :key="item.id" class="table-row">
              <div>
                <p class="list-title">{{ item.reservationNo }} · {{ item.roomNumber }}</p>
                <p class="list-subtitle">{{ item.checkInDate }} 至 {{ item.checkOutDate }} · {{ statusText(item.status) }}</p>
                <p class="list-subtitle">房费 {{ currency(item.roomFee) }} · 合计 {{ currency(item.totalAmount) }}</p>
              </div>
              <span :class="['pill', item.status === 'CHECKED_IN' ? 'pill-green' : item.status === 'CHECKED_OUT' ? 'pill-blue' : item.status === 'CANCELLED' ? 'pill-red' : 'pill-yellow']">
                {{ statusText(item.status) }}
              </span>
            </article>
            <p v-if="!customerReservationPage.records.length" class="empty-note">还没有订单，去「预订」页提交第一条吧。</p>
          </div>
          <div class="pager">
            <button class="secondary-button small" @click="goPage(loadCustomerReservationPage, customerReservationPage, customerReservationPage.pageNo - 1)">上一页</button>
            <span>第 {{ customerReservationPage.pageNo }} / {{ customerReservationPage.totalPages || 1 }} 页</span>
            <button class="secondary-button small" @click="goPage(loadCustomerReservationPage, customerReservationPage, customerReservationPage.pageNo + 1)">下一页</button>
          </div>
        </section>

        <section v-if="customerTab === 'profile'" class="panel-grid">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Profile</p>
                <h2>账户资料</h2>
              </div>
            </div>
            <div class="table-list">
              <article class="summary-row">
                <div>
                  <p class="list-title">显示名称</p>
                </div>
                <strong>{{ customerProfile?.displayName || currentUser?.displayName }}</strong>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">手机号</p>
                </div>
                <strong>{{ customerProfile?.phone || currentUser?.username }}</strong>
              </article>
              <article class="summary-row">
                <div>
                  <p class="list-title">会员等级</p>
                </div>
                <strong>{{ memberLevelText(customerProfile?.memberLevel) }}</strong>
              </article>
            </div>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Security</p>
                <h2>修改密码</h2>
              </div>
            </div>
            <form class="editor-form single-column" @submit.prevent="changePassword">
              <label>当前密码<input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" /></label>
              <label>新密码<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" /></label>
              <label>确认新密码<input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" /></label>
              <div class="form-actions">
                <button class="primary-button" type="submit">{{ actionLoading ? '提交中...' : '更新密码' }}</button>
              </div>
            </form>
          </section>
        </section>
      </main>

      <section v-else class="panel loading-panel">
        <p>正在加载住客中心...</p>
      </section>
    </template>

    <template v-else>
      <header class="hero">
        <div class="hero-head">
          <div class="brand-lockup brand-lockup-header">
            <span class="brand-mark brand-mark-header" role="img" aria-label="五星级酒店"></span>
            <div class="hero-copy compact-copy">
              <p class="eyebrow">Hotel Management System</p>
              <h1>日常工作台</h1>
            </div>
          </div>

          <div class="toolbar-actions hero-actions">
            <button class="secondary-button" @click="loadAllData">刷新数据</button>
            <button class="secondary-button" @click="exportExcel">导出 Excel</button>
            <button class="secondary-button" @click="logout">退出登录</button>
          </div>
        </div>
      </header>

      <section class="toolbar">
        <nav class="tabs">
          <button v-for="tab in visibleTabs" :key="tab" :class="['tab', activeTab === tab && 'is-active']" @click="activeTab = tab">
            {{ tabLabel(tab) }}
            <span v-if="tab === 'messages' && unreadNotificationCount > 0" class="tab-badge">{{ unreadNotificationCount }}</span>
          </button>
        </nav>
      </section>

      <section v-if="successMessage || errorMessage" class="status-strip">
        <span :class="['pill', errorMessage ? 'pill-red' : 'pill-green']">
          {{ errorMessage ? 'Operation Failed' : 'Operation Success' }}
        </span>
        <p>{{ errorMessage || successMessage }}</p>
      </section>

      <main v-if="!loading" class="content">
        <section v-if="activeTab === 'dashboard'" class="dashboard-stack">
          <section class="panel-grid">
            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Overview</p>
                  <h2>运营总览</h2>
                </div>
                <p class="section-note">房态、订单与收入核心指标</p>
              </div>
              <div class="table-list">
                <article v-for="item in stats" :key="item.label" class="summary-row">
                  <div>
                    <p class="list-title">{{ item.label }}</p>
                    <p class="list-subtitle">{{ item.note }}</p>
                  </div>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Trend</p>
                  <h2>近 7 日营收趋势</h2>
                </div>
              </div>
              <div class="trend-strip">
                <div class="trend-kpi">
                  <span>7日营收</span>
                  <strong>{{ currency(trends.totalRevenue) }}</strong>
                </div>
                <div class="trend-kpi">
                  <span>日均营收</span>
                  <strong>{{ currency(trends.averageDailyRevenue) }}</strong>
                </div>
                <div class="trend-kpi">
                  <span>到店</span>
                  <strong>{{ trends.totalArrivals }}</strong>
                </div>
                <div class="trend-kpi">
                  <span>离店</span>
                  <strong>{{ trends.totalDepartures }}</strong>
                </div>
              </div>
              <div class="chart-panel">
                <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="line-chart">
                  <polyline :points="trendPolyline" fill="none" stroke="#171411" stroke-width="2.4" />
                </svg>
                <div class="chart-grid">
                  <article v-for="point in trends.points" :key="point.date" class="chart-bar-card">
                    <div class="chart-bar">
                      <span :style="{ height: `${Math.max(10, (Number(point.revenue || 0) / trendMax) * 100)}%` }"></span>
                    </div>
                    <strong>{{ currency(point.revenue) }}</strong>
                    <small>{{ point.date }}</small>
                  </article>
                </div>
              </div>
            </section>
          </section>

          <section class="panel-grid">
            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Daily Focus</p>
                  <h2>日常处理建议</h2>
                </div>
              </div>
              <div class="table-list">
                <article class="summary-row">
                  <div>
                    <p class="list-title">到店接待</p>
                    <p class="list-subtitle">先处理今日“已预订”订单，确认证件、押金与房态。</p>
                  </div>
                </article>
                <article class="summary-row">
                  <div>
                    <p class="list-title">在住跟进</p>
                    <p class="list-subtitle">重点关注“已入住”订单的续住、加床与早餐加购。</p>
                  </div>
                </article>
                <article class="summary-row">
                  <div>
                    <p class="list-title">离店结算</p>
                    <p class="list-subtitle">退房前打印结算单，核对房费明细、押金与优惠券抵扣。</p>
                  </div>
                </article>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Message Focus</p>
                  <h2>未读提醒</h2>
                </div>
                <button class="secondary-button small" @click="activeTab = 'messages'">查看全部</button>
              </div>
              <div v-if="unreadNotifications.length" class="table-list">
                <article v-for="item in unreadNotifications" :key="item.id" class="summary-row">
                  <div>
                    <p class="list-title">{{ item.title }}</p>
                    <p class="list-subtitle">{{ item.content }}</p>
                  </div>
                  <span class="pill pill-yellow">{{ notificationCategoryText(item.category) }}</span>
                </article>
              </div>
              <p v-else class="empty-note">暂无未读提醒，前台节奏很稳。</p>
            </section>
          </section>

          <section class="panel-grid">
            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Finance Brief</p>
                  <h2>流水概览</h2>
                </div>
                <button class="secondary-button small" @click="activeTab = 'finance'">查看流水</button>
              </div>
              <div class="trend-strip">
                <div v-for="item in financeSummaryCards" :key="item.label" class="trend-kpi">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                  <small>{{ item.note }}</small>
                </div>
              </div>
            </section>

            <section class="panel">
              <div class="section-head">
                <div>
                  <p class="section-label">Account Security</p>
                  <h2>修改密码</h2>
                </div>
              </div>
              <form class="editor-form single-column" @submit.prevent="changePassword">
                <label>当前密码<input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" /></label>
                <label>新密码<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" /></label>
                <label>确认新密码<input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" /></label>
                <div class="form-actions">
                  <button class="primary-button" type="submit">{{ actionLoading ? '提交中...' : '更新密码' }}</button>
                </div>
              </form>
            </section>
          </section>
        </section>

        <section v-if="activeTab === 'calendar'" class="panel">
          <div class="section-head">
            <div>
              <p class="section-label">Room Calendar</p>
              <h2>房态日历</h2>
            </div>
            <p class="section-note">按日期查看每个房间的预订、入住与空房状态</p>
          </div>
          <div class="filter-grid">
            <label>开始日期<input v-model="calendarFilters.startDate" type="date" /></label>
            <label>天数
              <select v-model="calendarFilters.days">
                <option :value="7">7 天</option>
                <option :value="10">10 天</option>
                <option :value="14">14 天</option>
              </select>
            </label>
            <label>楼层<input v-model="calendarFilters.floor" type="number" min="1" placeholder="全部楼层" /></label>
            <button class="secondary-button" @click="loadRoomCalendar">刷新日历</button>
          </div>
          <div class="calendar-legend">
            <span><i class="legend-dot legend-available"></i> 空房</span>
            <span><i class="legend-dot legend-booked"></i> 已预订</span>
            <span><i class="legend-dot legend-checked-in"></i> 在住</span>
            <span><i class="legend-dot legend-maintenance"></i> 停用/维修</span>
          </div>
          <div class="calendar-table">
            <div class="calendar-row calendar-head" :style="{ gridTemplateColumns: `minmax(220px, 1.3fr) repeat(${calendarDays.length}, minmax(94px, 1fr))` }">
              <div class="calendar-room-meta">房间</div>
              <div v-for="day in calendarDays" :key="day" class="calendar-day">{{ day.slice(5) }}</div>
            </div>
            <div
              v-for="row in roomCalendar"
              :key="row.roomId"
              class="calendar-row"
              :style="{ gridTemplateColumns: `minmax(220px, 1.3fr) repeat(${calendarDays.length}, minmax(94px, 1fr))` }"
            >
              <div class="calendar-room-meta">
                <p class="list-title">{{ row.roomNumber }}</p>
                <p class="list-subtitle">{{ row.roomTypeName }} · {{ row.floor }}F · {{ statusText(row.cleanStatus) }}</p>
              </div>
              <div
                v-for="day in row.days"
                :key="`${row.roomId}-${day.date}`"
                :class="['calendar-cell', `calendar-${day.status.toLowerCase()}`]"
              >
                <strong>{{ day.status === 'CHECKED_IN' ? '在住' : day.status === 'BOOKED' ? '预订' : day.status === 'MAINTENANCE' ? '停用' : '空房' }}</strong>
                <small v-if="day.guestName">{{ day.guestName }}</small>
                <small v-else>{{ row.roomNumber }}</small>
                <div class="calendar-tooltip">
                  <p>{{ row.roomNumber }} · {{ row.roomTypeName }}</p>
                  <p>日期：{{ day.date }}</p>
                  <p>状态：{{ statusText(day.status) }}</p>
                  <p v-if="day.reservationNo">订单：{{ day.reservationNo }}</p>
                  <p v-if="day.guestName">住客：{{ day.guestName }}</p>
                  <p>清洁：{{ statusText(row.cleanStatus) }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'room-types'" class="split-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Room Type Config</p>
                <h2>{{ editingRoomTypeId ? '编辑房型' : '新增房型' }}</h2>
              </div>
            </div>
            <template v-if="isAdmin">
              <form class="editor-form" @submit.prevent="saveRoomType">
                <label>房型名称<input v-model="roomTypeForm.name" type="text" /></label>
                <label>基础价格<input v-model="roomTypeForm.basePrice" type="number" min="0" /></label>
                <label>最大入住<input v-model="roomTypeForm.maxGuests" type="number" min="1" /></label>
                <label>床型<input v-model="roomTypeForm.bedType" type="text" /></label>
                <label>面积<input v-model="roomTypeForm.area" type="number" min="1" /></label>
                <label class="full">描述<textarea v-model="roomTypeForm.description" rows="3"></textarea></label>
                <label class="full">设施<input v-model="roomTypeForm.amenities" type="text" /></label>
                <label class="full">
                  封面图路径
                  <input v-model="roomTypeForm.coverImage" type="text" placeholder="如 /room-types/urban-queen.jpg" />
                </label>
                <div v-if="roomTypeForm.coverImage" class="cover-preview full">
                  <img :src="roomTypeForm.coverImage" :alt="roomTypeForm.name || '房型封面'" />
                </div>
                <div class="form-actions full">
                  <button class="primary-button" type="submit">{{ actionLoading ? '保存中...' : '保存房型' }}</button>
                  <button class="secondary-button" type="button" @click="resetRoomTypeForm">重置</button>
                </div>
              </form>
            </template>
            <p v-else class="empty-note">前台角色仅可查看房型配置，不能修改。</p>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Query</p>
                <h2>房型列表</h2>
              </div>
              <p class="section-note">共 {{ roomTypePage.total }} 个房型</p>
            </div>
            <div class="filter-bar">
              <input v-model="roomTypeFilters.keyword" placeholder="搜索房型名称" @keyup.enter="loadRoomTypePage(1)" />
              <button class="secondary-button" @click="loadRoomTypePage(1)">筛选</button>
            </div>
            <div class="table-list">
              <article v-for="item in roomTypePage.records" :key="item.id" class="table-row media-row">
                <img v-if="item.coverImage" class="media-thumb" :src="item.coverImage" :alt="item.name" />
                <span v-else class="media-thumb is-empty">暂无图片</span>
                <div class="table-row-body">
                  <p class="list-title">{{ item.name }}</p>
                  <p class="list-subtitle">{{ currency(item.basePrice) }} · {{ item.maxGuests }} 位 · {{ item.area }}㎡ · {{ item.bedType }}</p>
                  <p class="list-subtitle">{{ item.amenities }}</p>
                  <div v-if="isAdmin" class="inline-actions row-actions">
                    <button class="secondary-button small" @click="startEditRoomType(item)">编辑</button>
                    <button class="secondary-button small danger" @click="removeItem('roomType', item.id)">删除</button>
                  </div>
                </div>
              </article>
              <p v-if="!roomTypePage.records.length" class="empty-note">没有符合条件的房型。</p>
            </div>
            <div class="pager">
              <button class="secondary-button small" @click="goPage(loadRoomTypePage, roomTypePage, roomTypePage.pageNo - 1)">上一页</button>
              <span>第 {{ roomTypePage.pageNo }} / {{ roomTypePage.totalPages || 1 }} 页</span>
              <button class="secondary-button small" @click="goPage(loadRoomTypePage, roomTypePage, roomTypePage.pageNo + 1)">下一页</button>
            </div>
          </section>
        </section>

        <section v-if="activeTab === 'rooms'" class="split-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Room Config</p>
                <h2>{{ editingRoomId ? '编辑房间' : '新增房间' }}</h2>
              </div>
            </div>
            <template v-if="isAdmin">
              <form class="editor-form" @submit.prevent="saveRoom">
                <label>房号<input v-model="roomForm.roomNumber" type="text" /></label>
                <label>
                  房型
                  <select v-model="roomForm.roomTypeId">
                    <option value="">请选择</option>
                    <option v-for="item in roomTypeList" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </label>
                <label>楼层<input v-model="roomForm.floor" type="number" min="1" /></label>
                <label>
                  销售状态
                  <select v-model="roomForm.status">
                    <option value="AVAILABLE">空房可售</option>
                    <option value="OCCUPIED">在住占用</option>
                    <option value="MAINTENANCE">维修停用</option>
                  </select>
                </label>
                <label>
                  清洁状态
                  <select v-model="roomForm.cleanStatus">
                    <option value="READY">已清洁</option>
                    <option value="CLEANING">清洁中</option>
                    <option value="BLOCKED">锁房</option>
                  </select>
                </label>
                <div class="form-actions full">
                  <button class="primary-button" type="submit">{{ actionLoading ? '保存中...' : '保存房间' }}</button>
                  <button class="secondary-button" type="button" @click="resetRoomForm">重置</button>
                </div>
              </form>
            </template>
            <p v-else class="empty-note">前台角色仅可查看房间配置，不能修改。</p>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Filters</p>
                <h2>房间列表</h2>
              </div>
              <p class="section-note">共 {{ roomPage.total }} 间</p>
            </div>
            <div class="filter-grid">
              <input v-model="roomFilters.keyword" placeholder="房号搜索" @keyup.enter="loadRoomPage(1)" />
              <select v-model="roomFilters.roomTypeId">
                <option value="">全部房型</option>
                <option v-for="item in roomTypeList" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
              <select v-model="roomFilters.status">
                <option value="">全部销售状态</option>
                <option value="AVAILABLE">空房可售</option>
                <option value="OCCUPIED">在住占用</option>
                <option value="MAINTENANCE">维修停用</option>
              </select>
              <select v-model="roomFilters.cleanStatus">
                <option value="">全部清洁状态</option>
                <option value="READY">已清洁</option>
                <option value="CLEANING">清洁中</option>
                <option value="BLOCKED">锁房</option>
              </select>
              <button class="secondary-button" @click="loadRoomPage(1)">筛选</button>
            </div>
            <div class="table-list">
              <article v-for="item in roomPage.records" :key="item.id" class="table-row media-row">
                <img v-if="roomTypeCover(item.roomTypeId)" class="media-thumb" :src="roomTypeCover(item.roomTypeId)" :alt="roomTypeName(item.roomTypeId)" />
                <span v-else class="media-thumb is-empty">暂无图片</span>
                <div class="table-row-body">
                  <p class="list-title">Room {{ item.roomNumber }} · {{ roomTypeName(item.roomTypeId) }}</p>
                  <p class="list-subtitle">楼层 {{ item.floor }}F · {{ statusText(item.status) }} · {{ statusText(item.cleanStatus) }}</p>
                  <div v-if="isAdmin" class="inline-actions row-actions">
                    <button class="secondary-button small" @click="startEditRoom(item)">编辑</button>
                    <button class="secondary-button small danger" @click="removeItem('room', item.id)">删除</button>
                  </div>
                </div>
              </article>
              <p v-if="!roomPage.records.length" class="empty-note">没有符合条件的房间。</p>
            </div>
            <div class="pager">
              <button class="secondary-button small" @click="goPage(loadRoomPage, roomPage, roomPage.pageNo - 1)">上一页</button>
              <span>第 {{ roomPage.pageNo }} / {{ roomPage.totalPages || 1 }} 页</span>
              <button class="secondary-button small" @click="goPage(loadRoomPage, roomPage, roomPage.pageNo + 1)">下一页</button>
            </div>
          </section>
        </section>

        <section v-if="activeTab === 'reservations'" class="split-layout reservations-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Reservation Operation</p>
                <h2>{{ editingReservationId ? '编辑订单' : '新增订单' }}</h2>
              </div>
            </div>
            <form class="editor-form" @submit.prevent="saveReservation">
              <label>住客姓名<input v-model="reservationForm.guestName" type="text" /></label>
              <label>手机号<input v-model="reservationForm.phone" type="text" /></label>
              <label>身份证<input v-model="reservationForm.idCard" type="text" /></label>
              <label>
                入住房间
                <select v-model="reservationForm.roomId">
                  <option value="">请选择</option>
                  <option v-for="item in roomCatalog" :key="item.id" :value="item.id">
                    {{ item.roomNumber }} · {{ statusText(item.status) }}
                  </option>
                </select>
              </label>
              <label>入住日期<input v-model="reservationForm.checkInDate" type="date" /></label>
              <label>离店日期<input v-model="reservationForm.checkOutDate" type="date" /></label>
              <label>入住人数<input v-model="reservationForm.guestCount" type="number" min="1" /></label>
              <label>渠道
                <select v-model="reservationForm.channel">
                  <option value="DIRECT">前台直订</option>
                  <option value="OTA">线上平台</option>
                  <option value="PHONE">电话预订</option>
                </select>
              </label>
              <label>房价/晚<input :value="currency(selectedRoomRate)" disabled /></label>
              <label>入住晚数<input :value="stayNights" disabled /></label>
              <label>房费<input :value="currency(estimatedAmount)" disabled /></label>
              <label>早餐<input v-model="reservationForm.breakfastFee" type="number" min="0" /></label>
              <label>加床<input v-model="reservationForm.extraBedFee" type="number" min="0" /></label>
              <label>押金<input v-model="reservationForm.depositAmount" type="number" min="0" /></label>
              <label>优惠券<input v-model="reservationForm.couponAmount" type="number" min="0" /></label>
              <label>合计<input :value="currency(totalChargePreview)" disabled /></label>
              <label class="full">特殊需求<textarea v-model="reservationForm.specialRequest" rows="3"></textarea></label>
              <div class="form-actions full">
                <button class="primary-button" type="submit">{{ actionLoading ? '保存中...' : '保存订单' }}</button>
                <button class="secondary-button" type="button" @click="resetReservationForm">重置</button>
              </div>
            </form>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Filters</p>
                <h2>订单列表</h2>
              </div>
            </div>
            <div class="filter-grid">
              <input v-model="reservationFilters.keyword" placeholder="订单号 / 住客 / 手机号" />
              <select v-model="reservationFilters.status">
                <option value="">全部状态</option>
                <option value="BOOKED">已预订</option>
                <option value="CHECKED_IN">已入住</option>
                <option value="CHECKED_OUT">已退房</option>
                <option value="CANCELLED">已取消</option>
              </select>
              <select v-model="reservationFilters.channel">
                <option value="">全部渠道</option>
                <option value="DIRECT">前台直订</option>
                <option value="OTA">线上平台</option>
                <option value="PHONE">电话预订</option>
              </select>
              <input v-model="reservationFilters.roomNumber" placeholder="房号" />
              <button class="secondary-button" @click="loadReservationPage(1)">筛选</button>
            </div>
            <section v-if="selectedReservationOps" class="ops-card">
              <div class="section-head compact">
                <div>
                  <p class="section-label">Stay Operations</p>
                  <h2>入住操作</h2>
                </div>
              </div>
              <div class="table-list">
                <article class="summary-row">
                  <div>
                    <p class="list-title">{{ selectedReservationOps.guestName }} · {{ selectedReservationOps.roomNumber }}</p>
                    <p class="list-subtitle">{{ selectedReservationOps.checkInDate }} 至 {{ selectedReservationOps.checkOutDate }} · {{ statusText(selectedReservationOps.status) }}</p>
                  </div>
                  <strong>{{ currency(selectedReservationOps.totalAmount) }}</strong>
                </article>
              </div>
              <div class="ops-grid">
                <label>续住到<input v-model="reservationOpsForm.extendCheckOutDate" type="date" /></label>
                <button class="secondary-button" @click="extendReservation">办理续住</button>
                <label>
                  更换房间
                  <select v-model="reservationOpsForm.changeRoomId">
                    <option value="">请选择目标房间</option>
                    <option v-for="room in availableTargetRooms" :key="room.id" :value="room.id">
                      {{ room.roomNumber }} · {{ roomTypeName(room.roomTypeId) }} · {{ statusText(room.status) }}
                    </option>
                  </select>
                </label>
                <label>换房原因<input v-model="reservationOpsForm.changeReason" type="text" placeholder="如：住客偏好 / 设备问题" /></label>
              </div>
              <div class="form-actions">
                <button class="primary-button" type="button" @click="changeReservationRoom">确认换房</button>
              </div>
            </section>
            <div class="table-list">
              <article
                v-for="item in reservationPage.records"
                :key="item.id"
                :class="['table-row', selectedReservationOps?.id === item.id && 'table-row-active']"
              >
                <div>
                  <p class="list-title">{{ item.guestName }} · {{ item.roomNumber }} · {{ statusText(item.status) }}</p>
                  <p class="list-subtitle">
                    {{ item.checkInDate }} 至 {{ item.checkOutDate }} · {{ channelText(item.channel) }} · {{ currency(item.totalAmount) }}
                  </p>
                  <p class="list-subtitle">
                    房费 {{ currency(item.roomFee) }} / 早餐 {{ currency(item.breakfastFee) }} / 加床 {{ currency(item.extraBedFee) }}
                  </p>
                  <div class="inline-actions row-actions">
                    <button v-if="item.status === 'BOOKED'" class="secondary-button small" @click="changeReservationStatus(item, 'CHECKED_IN')">入住登记</button>
                    <button v-if="item.status === 'BOOKED'" class="secondary-button small" @click="changeReservationStatus(item, 'CANCELLED')">取消订单</button>
                    <button v-if="item.status === 'CHECKED_IN'" class="secondary-button small" @click="changeReservationStatus(item, 'CHECKED_OUT')">办理离店</button>
                    <button class="secondary-button small" @click="openPrintDocument(item, 'reservation')">打印单</button>
                    <button class="secondary-button small" @click="openPrintDocument(item, 'checkin')">入住单</button>
                    <button class="secondary-button small" @click="openPrintDocument(item, 'settlement')">结算单</button>
                  </div>
                </div>
                <div class="inline-actions vertical">
                  <span :class="['pill', item.status === 'CHECKED_IN' ? 'pill-green' : item.status === 'CHECKED_OUT' ? 'pill-blue' : item.status === 'CANCELLED' ? 'pill-red' : 'pill-yellow']">
                    {{ statusText(item.status) }}
                  </span>
                  <div class="inline-actions">
                    <button class="secondary-button small" @click="selectReservationOps(item)">选中</button>
                    <button class="secondary-button small" @click="startEditReservation(item)">编辑</button>
                    <button v-if="isAdmin" class="secondary-button small danger" @click="removeItem('reservation', item.id)">删除</button>
                  </div>
                </div>
              </article>
              <p v-if="!reservationPage.records.length" class="empty-note">没有符合条件的订单。</p>
            </div>
            <div class="pager">
              <button class="secondary-button small" @click="goPage(loadReservationPage, reservationPage, reservationPage.pageNo - 1)">上一页</button>
              <span>第 {{ reservationPage.pageNo }} / {{ reservationPage.totalPages || 1 }} 页</span>
              <button class="secondary-button small" @click="goPage(loadReservationPage, reservationPage, reservationPage.pageNo + 1)">下一页</button>
            </div>
          </section>
        </section>

        <section v-if="activeTab === 'finance'" class="panel">
          <div class="section-head">
            <div>
              <p class="section-label">Finance Ledger</p>
              <h2>财务流水</h2>
            </div>
            <p class="section-note">共 {{ financePage.total }} 笔</p>
          </div>
          <div class="filter-grid">
            <input v-model="financeFilters.reservationId" placeholder="订单 ID" @keyup.enter="loadFinancePage(1)" />
            <select v-model="financeFilters.transactionType">
              <option value="">全部类型</option>
              <option value="ROOM_FEE">房费入账</option>
              <option value="DEPOSIT">押金</option>
              <option value="CHECKOUT_SETTLEMENT">退房结算</option>
              <option value="ROOM_FEE_ADJUSTMENT">房费调整</option>
              <option value="COUPON">优惠券抵扣</option>
            </select>
            <select v-model="financeFilters.direction">
              <option value="">全部方向</option>
              <option value="CHARGE">入账</option>
              <option value="DISCOUNT">抵扣</option>
              <option value="REFUND">退款</option>
            </select>
            <button class="secondary-button" @click="loadFinancePage(1)">筛选</button>
          </div>
          <div class="stats-grid finance-summary-grid">
            <article v-for="item in financeSummaryCards" :key="item.label" class="stat-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.note }}</small>
            </article>
          </div>
          <div class="table-list">
            <article v-for="item in financePage.records" :key="item.id" class="table-row">
              <div>
                <p class="list-title">{{ transactionText(item.transactionType) }} · {{ item.reservationNo }}</p>
                <p class="list-subtitle">{{ item.guestName }} · {{ item.roomNumber }} · {{ item.remark }}</p>
              </div>
              <div class="inline-actions vertical">
                <span :class="['pill', item.direction === 'CHARGE' ? 'pill-yellow' : item.direction === 'DISCOUNT' ? 'pill-green' : 'pill-blue']">{{ directionText(item.direction) }}</span>
                <strong>{{ currency(item.amount) }}</strong>
              </div>
            </article>
            <p v-if="!financePage.records.length" class="empty-note">当前筛选范围内没有财务流水。</p>
          </div>
          <div class="pager">
            <button class="secondary-button small" @click="goPage(loadFinancePage, financePage, financePage.pageNo - 1)">上一页</button>
            <span>第 {{ financePage.pageNo }} / {{ financePage.totalPages || 1 }} 页</span>
            <button class="secondary-button small" @click="goPage(loadFinancePage, financePage, financePage.pageNo + 1)">下一页</button>
          </div>
        </section>

        <section v-if="activeTab === 'logs'" class="panel">
          <div class="section-head">
            <div>
              <p class="section-label">Audit Trail</p>
              <h2>操作日志</h2>
            </div>
            <p class="section-note">共 {{ logPage.total }} 条</p>
          </div>
          <div class="filter-grid">
            <input v-model="logFilters.reservationId" placeholder="订单 ID" @keyup.enter="loadLogPage(1)" />
            <input v-model="logFilters.operatorUsername" placeholder="操作人" @keyup.enter="loadLogPage(1)" />
            <select v-model="logFilters.actionType">
              <option value="">全部动作</option>
              <option value="CREATE_RESERVATION">创建订单</option>
              <option value="STATUS_CHANGE">状态变更</option>
              <option value="EXTEND_STAY">办理续住</option>
              <option value="ROOM_CHANGE">办理换房</option>
            </select>
            <button class="secondary-button" @click="loadLogPage(1)">筛选</button>
          </div>
          <div class="table-list">
            <article v-for="item in logPage.records" :key="item.id" class="table-row">
              <div>
                <p class="list-title">{{ actionText(item.actionType) }} · {{ item.reservationNo || '系统记录' }}</p>
                <p class="list-subtitle">操作人：{{ item.operatorUsername }} · {{ roleText(item.operatorRole) }} · {{ item.description }}</p>
                <p class="list-subtitle">变更前：{{ snapshotText(item.beforeSnapshot) }}</p>
                <p class="list-subtitle">变更后：{{ snapshotText(item.afterSnapshot) }}</p>
              </div>
              <span class="log-time">{{ formatDateTime(item.createdAt) }}</span>
            </article>
            <p v-if="!logPage.records.length" class="empty-note">当前筛选范围内没有操作日志。</p>
          </div>
          <div class="pager">
            <button class="secondary-button small" @click="goPage(loadLogPage, logPage, logPage.pageNo - 1)">上一页</button>
            <span>第 {{ logPage.pageNo }} / {{ logPage.totalPages || 1 }} 页</span>
            <button class="secondary-button small" @click="goPage(loadLogPage, logPage, logPage.pageNo + 1)">下一页</button>
          </div>
        </section>

        <section v-if="activeTab === 'messages'" class="panel">
          <div class="section-head">
            <div>
              <p class="section-label">Notifications</p>
              <h2>消息提醒</h2>
            </div>
            <p class="section-note">未读 {{ unreadNotificationCount }} 条 / 共 {{ notificationPage.total }} 条</p>
          </div>
          <div class="filter-grid">
            <select v-model="notificationFilters.status">
              <option value="">全部状态</option>
              <option value="UNREAD">未读</option>
              <option value="READ">已读</option>
            </select>
            <select v-model="notificationFilters.category">
              <option value="">全部类别</option>
              <option value="BOOKING_SUCCESS">预订成功</option>
              <option value="UPCOMING_CHECKIN">即将入住</option>
              <option value="UPCOMING_CHECKOUT">即将退房</option>
            </select>
            <button class="secondary-button" @click="loadNotificationPage(1)">筛选</button>
          </div>
          <div class="table-list">
            <article v-for="item in notificationPage.records" :key="item.id" class="table-row">
              <div>
                <p class="list-title">{{ item.title }}</p>
                <p class="list-subtitle">{{ item.content }}</p>
                <p class="list-subtitle">{{ notificationCategoryText(item.category) }} · {{ formatDateTime(item.scheduledAt) }}</p>
              </div>
              <div class="inline-actions vertical">
                <span :class="['pill', item.status === 'UNREAD' ? 'pill-yellow' : 'pill-green']">{{ statusText(item.status) }}</span>
                <button v-if="item.status === 'UNREAD'" class="secondary-button small" @click="markNotificationRead(item)">标记已读</button>
              </div>
            </article>
            <p v-if="!notificationPage.records.length" class="empty-note">当前筛选范围内没有提醒消息。</p>
          </div>
          <div class="pager">
            <button class="secondary-button small" @click="goPage(loadNotificationPage, notificationPage, notificationPage.pageNo - 1)">上一页</button>
            <span>第 {{ notificationPage.pageNo }} / {{ notificationPage.totalPages || 1 }} 页</span>
            <button class="secondary-button small" @click="goPage(loadNotificationPage, notificationPage, notificationPage.pageNo + 1)">下一页</button>
          </div>
        </section>

        <section v-if="activeTab === 'guests'" class="triple-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Guest CRUD</p>
                <h2>{{ editingGuestId ? '编辑住客' : '新增住客' }}</h2>
              </div>
            </div>
            <form class="editor-form single-column" @submit.prevent="saveGuest">
              <label>姓名<input v-model="guestForm.fullName" type="text" /></label>
              <label>手机号<input v-model="guestForm.phone" type="text" /></label>
              <label>身份证<input v-model="guestForm.idCard" type="text" /></label>
              <label>
                会员等级
                <select v-model="guestForm.memberLevel">
                  <option value="REGULAR">普通会员</option>
                  <option value="GOLD">金卡会员</option>
                  <option value="PLATINUM">白金会员</option>
                </select>
              </label>
              <label>备注<textarea v-model="guestForm.remark" rows="3"></textarea></label>
              <div class="form-actions">
                <button class="primary-button" type="submit">{{ actionLoading ? '保存中...' : '保存住客' }}</button>
                <button class="secondary-button" type="button" @click="resetGuestForm">重置</button>
              </div>
            </form>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Filters</p>
                <h2>住客列表</h2>
              </div>
            </div>
            <div class="filter-grid">
              <input v-model="guestFilters.keyword" placeholder="姓名 / 手机 / 身份证" @keyup.enter="loadGuestPage(1)" />
              <select v-model="guestFilters.memberLevel">
                <option value="">全部会员等级</option>
                <option value="REGULAR">普通会员</option>
                <option value="GOLD">金卡会员</option>
                <option value="PLATINUM">白金会员</option>
              </select>
              <button class="secondary-button" @click="loadGuestPage(1)">筛选</button>
            </div>
            <div class="table-list">
              <article v-for="item in guestPage.records" :key="item.id" class="table-row">
                <div>
                  <p class="list-title">{{ item.fullName }} · {{ memberLevelText(item.memberLevel) }}</p>
                  <p class="list-subtitle">{{ item.phone }} · {{ item.idCard }}</p>
                </div>
                <div class="inline-actions">
                  <button class="secondary-button small" @click="loadGuestProfile(item.id)">画像</button>
                  <button class="secondary-button small" @click="startEditGuest(item)">编辑</button>
                  <button v-if="isAdmin" class="secondary-button small danger" @click="removeItem('guest', item.id)">删除</button>
                </div>
              </article>
              <p v-if="!guestPage.records.length" class="empty-note">没有符合条件的住客。</p>
            </div>
            <div class="pager">
              <button class="secondary-button small" @click="goPage(loadGuestPage, guestPage, guestPage.pageNo - 1)">上一页</button>
              <span>第 {{ guestPage.pageNo }} / {{ guestPage.totalPages || 1 }} 页</span>
              <button class="secondary-button small" @click="goPage(loadGuestPage, guestPage, guestPage.pageNo + 1)">下一页</button>
            </div>
          </section>

          <section class="panel" v-if="selectedGuestProfile">
            <div class="section-head">
              <div>
                <p class="section-label">Guest Profile</p>
                <h2>{{ selectedGuestProfile.guest.fullName }}</h2>
              </div>
            </div>
            <div class="profile-grid">
              <article class="stat-card compact-card">
                <span>总订单</span>
                <strong>{{ selectedGuestProfile.stats.totalReservations }}</strong>
              </article>
              <article class="stat-card compact-card">
                <span>完成入住</span>
                <strong>{{ selectedGuestProfile.stats.completedStays }}</strong>
              </article>
              <article class="stat-card compact-card">
                <span>累计消费</span>
                <strong>{{ currency(selectedGuestProfile.stats.totalSpent) }}</strong>
              </article>
              <article class="stat-card compact-card">
                <span>平均消费</span>
                <strong>{{ currency(selectedGuestProfile.stats.averageSpent) }}</strong>
              </article>
            </div>
            <div class="history-list">
              <article v-for="item in selectedGuestProfile.history" :key="item.reservationId" class="history-row">
                <div>
                  <p class="list-title">{{ item.reservationNo }} · {{ item.roomNumber }}</p>
                  <p class="list-subtitle">{{ item.checkInDate }} 至 {{ item.checkOutDate }} · {{ statusText(item.status) }}</p>
                </div>
                <strong>{{ currency(item.totalAmount) }}</strong>
              </article>
            </div>
          </section>
        </section>

        <section v-if="activeTab === 'users' && isAdmin" class="split-layout">
          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Account & Role</p>
                <h2>{{ editingUserId ? '编辑账户' : '新增账户' }}</h2>
              </div>
            </div>
            <form class="editor-form single-column" @submit.prevent="saveUser">
              <label>用户名<input v-model="userForm.username" type="text" /></label>
              <label>显示名称<input v-model="userForm.displayName" type="text" /></label>
              <label>
                用户角色
                <select v-model="userForm.role">
                  <option value="ADMIN">系统管理员</option>
                  <option value="FRONT_DESK">前台专员</option>
                </select>
              </label>
              <label>
                账户状态
                <select v-model="userForm.status">
                  <option value="ACTIVE">启用</option>
                  <option value="DISABLED">停用</option>
                </select>
              </label>
              <label>
                {{ editingUserId ? '重置密码（留空则不修改）' : '登录密码' }}
                <input v-model="userForm.password" type="password" autocomplete="new-password" />
              </label>
              <div class="form-actions">
                <button class="primary-button" type="submit">{{ actionLoading ? '保存中...' : '保存账户' }}</button>
                <button class="secondary-button" type="button" @click="resetUserForm">重置</button>
              </div>
            </form>
          </section>

          <section class="panel">
            <div class="section-head">
              <div>
                <p class="section-label">Access Control</p>
                <h2>用户与角色列表</h2>
              </div>
            </div>
            <div class="filter-grid">
              <input v-model="userFilters.keyword" placeholder="用户名 / 显示名称" @keyup.enter="loadUserPage(1)" />
              <select v-model="userFilters.role">
                <option value="">全部角色</option>
                <option value="ADMIN">系统管理员</option>
                <option value="FRONT_DESK">前台专员</option>
              </select>
              <select v-model="userFilters.status">
                <option value="">全部状态</option>
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">停用</option>
              </select>
              <button class="secondary-button" @click="loadUserPage(1)">筛选</button>
            </div>
            <div class="table-list">
              <article v-for="item in userPage.records" :key="item.id" class="table-row">
                <div>
                  <p class="list-title">{{ item.displayName }} · {{ item.username }}</p>
                  <p class="list-subtitle">{{ roleText(item.role) }} · {{ statusText(item.status) }}</p>
                </div>
                <div class="inline-actions">
                  <button class="secondary-button small" @click="startEditUser(item)">编辑</button>
                  <button class="secondary-button small danger" @click="removeItem('user', item.id)">删除</button>
                </div>
              </article>
              <p v-if="!userPage.records.length" class="empty-note">没有符合条件的账户。</p>
            </div>
            <div class="pager">
              <button class="secondary-button small" @click="goPage(loadUserPage, userPage, userPage.pageNo - 1)">上一页</button>
              <span>第 {{ userPage.pageNo }} / {{ userPage.totalPages || 1 }} 页</span>
              <button class="secondary-button small" @click="goPage(loadUserPage, userPage, userPage.pageNo + 1)">下一页</button>
            </div>
          </section>
        </section>
      </main>

      <section v-else class="panel loading-panel">
        <p>正在加载后台数据...</p>
      </section>
    </template>
  </div>
</template>
