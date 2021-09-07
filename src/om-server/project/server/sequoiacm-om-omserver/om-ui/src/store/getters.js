const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  user_name: state => state.user.user_name,
  user_id: state => state.user.user_id
}
export default getters
