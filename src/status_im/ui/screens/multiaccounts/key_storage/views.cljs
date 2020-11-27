(ns status-im.ui.screens.multiaccounts.key-storage.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [quo.core :as quo]
            [re-frame.core :as re-frame]
            [status-im.ui.components.react :as react]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.components.chat-icon.screen :as chat-icon.screen]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.components.toolbar :as toolbar]
            [status-im.ui.components.accordion :as accordion]))

;; Component to render Key and Storage management screen
(defview key-storage []
  (letsubs [{:keys [name] :as multiaccount} [:multiaccounts/login]
            move-keystore-checked? [:multiaccount/move-keystore-checked?]]
    [react/view {:flex 1}
     ;; TODO: i18n the title and subtitle
     [topbar/topbar {:title    "Key management"
                     :subtitle "Choose actions"}]
     [accordion/section {:title   name
                         :icon    [chat-icon.screen/contact-icon-contacts-tab
                                   (multiaccounts/displayed-photo multiaccount)]
                         :count   0
                         :content [react/text :acc-text]}]
     [react/view {:flex 1
                  :flex-direction :column}
      [quo/list-header "Actions"]
      [quo/list-item {:title "Move keystore file"
                      :subtitle "Select a new location to save your private key(s)"
                      :subtitle-max-lines 4
                      :accessory :checkbox
                      :active move-keystore-checked?
                      :on-press #(re-frame/dispatch [:multiaccounts.ui/key-management-move-keystore-checked? (not move-keystore-checked?)])}]
      [quo/list-item {:title "Reset database"
                      :subtitle "Delete chats, contacts and settings. Required when you’ve lost your password"
                      :subtitle-max-lines 4
                      :disabled true
                      :active move-keystore-checked?
                      :accessory :checkbox}]

      [react/view {:flex 1}]
      [toolbar/toolbar {:show-border? true
                        :right [quo/button {:type :secondary
                                            :disabled (not move-keystore-checked?)
                                            :after :main-icons/next}
                                "Enter seed phrase"]}]]]))

(comment
  (-> re-frame.db/app-db deref keys)
  (-> re-frame.db/app-db deref
      :multiaccounts/login)

  )
