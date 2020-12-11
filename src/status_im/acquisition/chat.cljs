(ns status-im.acquisition.chat
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.utils.fx :as fx]
            [status-im.ethereum.tokens :as tokens]
            [status-im.ui.components.react :as react]
            [status-im.acquisition.claim :as claim]
            [status-im.ethereum.core :as ethereum]
            [status-im.acquisition.persistance :as persistence]
            [status-im.notifications.core :as notifications]
            [status-im.acquisition.gateway :as gateway]
            [status-im.chat.models :as chat]
            [status-im.navigation :as navigation]))

(def public-chat "public-chat")

(fx/defn start-acquisition
  [{:keys [db]} {:keys [key id] :as referrer} public]
  {:db                             (assoc-in db [:acquisition :chat-referrer (or key id)] referrer)
   ::persistence/chat-initialized? (fn [state]
                                     (when-not (= "initialized" state)
                                       (if public
                                         (re-frame/dispatch [::start-public-chat referrer public])
                                         (re-frame/dispatch [::start-chat referrer public]))))})

(fx/defn start-chat
  {:events [::start-chat]}
  [cofx {:keys [key id]} public]
  (fx/merge cofx
            {::persistence/chat-initalized! true}
            (chat/start-chat key)))

(fx/defn start-public-chat
  {:events [::start-public-chat]}
  [cofx referrer chat-name]
  (fx/merge cofx
            {::persistence/chat-initalized! true}
            (navigation/navigate-to :tabs {:screen :chat-stack
                                           :params {:screen :referral-enclav}})))

(fx/defn join-public-chat
  [cofx chat-name]
  (chat/start-public-chat cofx chat-name nil))

(fx/defn accept-pack
  {:events [::accept-pack]}
  [{:keys [db] :as cofx}]
  (let [referral          (get-in db [:acquisition :referrer])
        {:keys [type id]} (get-in db [:acquisition :metadata])
        payload           {:chat_key    (get-in db [:multiaccount :public-key])
                           :address     (ethereum/default-address db)
                           :invite_code referral}]
    (fx/merge cofx
              {:db (update db :acquisition dissoc :chat-referrer)
               ::notifications/local-notifications
               {:title   (i18n/label :t/starter-pack-coming)
                :message (i18n/label :t/starter-pack-coming-description)
                :icon    (:uri (react/resolve-asset-source tokens/snt-icon-source))}}
              (when (= type public-chat)
                (join-public-chat id))
              (gateway/handle-acquisition {:message    payload
                                           :method     "PATCH"
                                           :url        [:clicks referral]
                                           :on-success [::claim/success-starter-pack-claim]}))))
