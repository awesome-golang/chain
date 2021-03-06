import { combineReducers } from 'redux'
import { routerReducer as routing} from 'react-router-redux'
import { reducer as form } from 'redux-form'
import { reducers as access_token } from 'features/accessTokens'
import { reducers as account } from 'features/accounts'
import { reducers as app } from 'features/app'
import { reducers as asset } from 'features/assets'
import { reducers as balance } from 'features/balances'
import { reducers as core } from 'features/core'
import { reducers as mockhsm } from 'features/mockhsm'
import { reducers as testnet } from 'features/testnet'
import { reducers as transaction } from 'features/transactions'
import { reducers as transactionFeed } from 'features/transactionFeeds'
import { reducers as unspent } from 'features/unspents'
import { clear as clearStorage } from 'utility/localStorage'

const makeRootReducer = () => (state, action) => {
  if (action.type == 'UPDATE_CORE_INFO' &&
      !action.param.is_configured) {
    const newState = {
      form: state.form,
      routing: state.routing,
    }

    if (state.core.blockchainId == (action.param.blockchain_id || 0)) {
      newState.core = state.core
    }

    state = newState
  } else if (action.type == 'USER_LOG_OUT') {
    // TODO: see if we can't move this outside of a reducer..

    // Actions still may fire after the location redirect, so make sure they
    // fire against blank state, and the local storage listener doesn't
    // persist state.
    state = undefined

    // Clear tokens and other state from local storage.
    clearStorage()

    // Finally, reboot the entire dashboard app via a hard redirect.
    window.location.href = '/'
  }

  return combineReducers({
    ...access_token,
    account,
    app,
    asset,
    balance,
    core,
    form,
    mockhsm,
    routing,
    testnet,
    transaction,
    transactionFeed,
    unspent,
  })(state, action)
}
export default makeRootReducer
